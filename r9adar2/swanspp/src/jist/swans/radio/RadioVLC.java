//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioNoiseIndep.java Tue 2004/04/20 09:00:20 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import java.awt.Polygon;
import java.util.LinkedList;

import org.omg.CORBA.PRIVATE_MEMBER;

import driver.JistExperiment;
import driver.Visualizer;
import jist.swans.field.Field;
import jist.swans.field.Field.RadioData;
import jist.swans.field.Spatial;
import jist.swans.mac.MacMessage;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.Constants;
import jist.swans.Main;
import jist.runtime.JistAPI;

/** 
 * <code>RadioNoiseIndep</code> implements a radio with an independent noise model.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: RadioNoiseIndep.java,v 1.1 2007/04/09 18:49:46 drchoffnes Exp $
 * @since SWANS1.0
 */

public final class RadioVLC extends RadioNoise
{
	public enum SensorModes
	{
		Receive(0),
		Send(1),
		SendAndReceive(2),
		Unknown(3);
		private int code;

		private SensorModes(int c) {
			code = c;
		}

		public int getCode() {
			return code;
		}
	}
	public class VLCsensor
	{
		public float distanceLimit = 250;
		public float visionAngle = 60;
		public float offsetX =10;
		public float offsetY = 10;
		public float bearing = 0;
		public int sensorID = 0;
		public SensorModes mode;
		
		public VLCsensor(int sensorID, float distancelimit, float visionAngle, float offsetX, float offsetY, float bearing, SensorModes mode) 
		{
			this.distanceLimit = distancelimit; 
			this.visionAngle = visionAngle;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.bearing = bearing;
			this.sensorID = sensorID;
			this.mode = mode;
		}
		
	}
  //////////////////////////////////////////////////
  // locals
  //

  /**
   * threshold signal-to-noise ratio.
   */
public LinkedList<VLCsensor> sensors = new LinkedList<RadioVLC.VLCsensor>();
  protected double thresholdSNR;
	private Location cornerPoint1;
	private Location cornerPoint2;
	float distanceLimit = 250; 	//distance limit the vlc device can see in front of it, def:250
	int visionAngle = 60; 		//The viewing angle the of the vlc device, default is 60 degrees
  
  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new radio with independent noise model.
   *
   * @param id radio identifier
   * @param sharedInfo shared radio properties
   */
  public RadioVLC(int id, RadioInfo.RadioInfoShared sharedInfo)
  {
    this(id, sharedInfo, Constants.SNR_THRESHOLD_DEFAULT);
  }

  /**
   * Create new radio with independent noise model.
   *
   * @param id radio identifier
   * @param sharedInfo shared radio properties
   * @param thresholdSNR threshold signal-to-noise ratio
   */
  public RadioVLC(int id, RadioInfo.RadioInfoShared sharedInfo, double thresholdSNR)
  {
    super(id, sharedInfo);
    setThresholdSNR(thresholdSNR);
    sensors.add(new VLCsensor(1, 250, 60, 1, 0, 0, SensorModes.Send));//front Tx
    sensors.add(new VLCsensor(1, 250, 80, 1, 0, 0, SensorModes.Receive));//front Rx
    sensors.add(new VLCsensor(1, 250, 60, -1, 0, 180, SensorModes.Send));//back Tx
    sensors.add(new VLCsensor(1, 250, 80, -1, 0, 180, SensorModes.Receive));//front Rx
    
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Set signal-to-noise ratio.
   *
   * @param snrThreshold threshold signal-to-noise ratio
   */
  public void setThresholdSNR(double snrThreshold)
  {
    this.thresholdSNR = snrThreshold;
  }

  //////////////////////////////////////////////////
  // reception
  //

  // RadioInterface interface
  /** {@inheritDoc} */
  public void receive(Message msg, Double powerObj_mW, Long durationObj)
  { 
	  final double power_mW = powerObj_mW.doubleValue();
	  final long duration = durationObj.longValue();
	  // ignore if below sensitivity
	  if(power_mW < radioInfo.shared.sensitivity_mW) 
	  {return;
	  }
	  // discard message if below threshold
	  if(!CanTalk(((MacMessage)msg).getSrc().hashCode(), radioInfo.unique.getID() ))
	  {
		  //System.out.println("TALK rcv :( ");
		  return;
	  }
	  
	  System.out.println("TALK rcv :) msg hc: "+msg.hashCode()+" from: " +((MacMessage)msg).getSrc().hashCode()+ " to: " + radioInfo.unique.getID()  );
	  //System.out.println("TALK rcv :) ");



	  if(power_mW < radioInfo.shared.threshold_mW 
			  || power_mW < radioInfo.shared.background_mW * thresholdSNR) msg = null;
	  switch(mode)
	  {
	  case Constants.RADIO_MODE_IDLE:
		  if(msg!=null) setMode(Constants.RADIO_MODE_RECEIVING);
		  lockSignal(msg, power_mW, duration);
		  break;
	  case Constants.RADIO_MODE_RECEIVING:
		  if(Main.ASSERT) Util.assertion(signals>0);
		  if(power_mW >= radioInfo.shared.threshold_mW
				  &&  power_mW > signalPower_mW*thresholdSNR)
		  {
			  lockSignal(msg, power_mW, duration);
		  }
		  break;
	  case Constants.RADIO_MODE_TRANSMITTING:
		  break;
	  case Constants.RADIO_MODE_SLEEP:
		  break;
	  default:
		  throw new RuntimeException("invalid radio mode: "+mode);
	  }
	  // increment number of incoming signals
	  signals++;
	  // schedule an endReceive
	  JistAPI.sleep(duration); 
	  self.endReceive(powerObj_mW, new Long(seqNumber));
  }

  // RadioInterface interface
  /** {@inheritDoc} */
  public void endReceive(final Double powerObj_mW, Long seqNumber)
  {
	 
    if(mode==Constants.RADIO_MODE_SLEEP) return;
    if(Main.ASSERT) Util.assertion(signals>0);
    signals--;
    if(mode==Constants.RADIO_MODE_RECEIVING)
    {
      if(signalBuffer!=null && JistAPI.getTime()==signalFinish)
      {
        this.macEntity.receive(signalBuffer);
        unlockSignal();
      }
      if(signals==0) setMode(Constants.RADIO_MODE_IDLE);
    }
  }


  //////////////////////////////////////////////////
  // transmission
  //

  // RadioInterface interface
  /** {@inheritDoc} */
  public void transmit(Message msg, long delay, long duration)
  {  
    // radio in sleep mode
    if(mode==Constants.RADIO_MODE_SLEEP) return;
    // ensure not currently transmitting
    if(mode==Constants.RADIO_MODE_TRANSMITTING) throw new RuntimeException("radio already transmitting");
    // clear receive buffer
    assert(signalBuffer==null);
    signalBuffer = null;
    
    if(((MacMessage)msg).getDst().hashCode() != -1)
    {
    	if(!CanTalk(((MacMessage)msg).getSrc().hashCode(), ((MacMessage)msg).getDst().hashCode()))
    	{
    	//	System.out.println("TALK :( ");
    		return;
    	}
    }
    else
    {
    	//bcast je
    }
    System.out.println("TALK snd :)  msg hc: "+msg.hashCode()+"  from: " +((MacMessage)msg).getSrc().hashCode()+ " to: " + ((MacMessage)msg).getDst().hashCode() );
    // use default delay, if necessary
    if(delay==Constants.RADIO_NOUSER_DELAY) delay = Constants.RADIO_PHY_DELAY;
    // set mode to transmitting
    setMode(Constants.RADIO_MODE_TRANSMITTING);
    // schedule message propagation delay
    JistAPI.sleep(delay);
    fieldEntity.transmit(radioInfo, msg, duration);
    // schedule end of transmission
    JistAPI.sleep(duration);
    self.endTransmit();
  }

  // RadioInterface interface
  /** {@inheritDoc} */
  public void endTransmit()
  {
    // radio in sleep mode
    if(mode==Constants.RADIO_MODE_SLEEP) return;
    // check that we are currently transmitting
    if(mode!=Constants.RADIO_MODE_TRANSMITTING) throw new RuntimeException("radio is not transmitting");
    // set mode
    setMode(signals>0 ? Constants.RADIO_MODE_RECEIVING : Constants.RADIO_MODE_IDLE);
  }

  
  
	//bt
  private boolean CanTalk(int SourceID, int DestinationID)
  {
	//  System.out.println("can we talk? me= " +SourceID + " him= "+ DestinationID);
	  if(DestinationID != -1)
	  {
		  if(getQualifiyingNodes(SourceID).contains(DestinationID) && getQualifiyingNodes(DestinationID).contains(SourceID))
		  {
			  return true;
		  }
	  }
	  else
	  {
		  //broadcast poruka je 
		  
	  }
	  return false;
  }
	/**
	 * This method will need to be called two times. Each call will detect one corner point of the vlc device's view
	 * @param theta: the angle of the bearing +- visionAngle
	 * @param origin: starting point
	 * @param distanceLimit: the max distance the vlc device can see
	 * @param visionAngle: the angle at which the vlc device can see from the front of the car
	 */
	public Location getVLCCornerPoint(float theta, Location origin, float distLimit, int visionAngle)
	{
		Location cornerPoint; 
		int quadrant = 0; 
		float hypotenuse = (float)(distanceLimit/Math.cos(((visionAngle/2)*Math.PI)/(180))); 
		
		//first detect what quadrant theta falls, to see how bearing affects corner points
		if(theta >= 0 && theta <= 90)
		{
			quadrant = 1;
			
			cornerPoint = new Location.Location2D(origin.getX()+ (float) (hypotenuse * Math.cos((Math.PI*(theta))/(180))),origin.getY()+ (float) (hypotenuse * Math.sin((Math.PI*(theta))/(180))));			
		}
		else if(theta > 90 && theta <= 180)
		{
			quadrant = 2; 	
			cornerPoint = new Location.Location2D(origin.getX()+(float) (-1 * hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))),origin.getY()+ (float) (hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180)))); 
		}
		else if(theta > 180 && theta <= 270)
		{
			quadrant = 3;
			//x coordinate needs sin function here. the Y axis between the 3rd and 4th quadrants is being used to compute the angle.
			//likewise, cos needs to be used here to find the y coordinate.
			cornerPoint = new Location.Location2D(origin.getX()+(float) (-1 * hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180))),origin.getY()+ (float) (-1 * hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))));					
		}
		else
		{
			quadrant = 4;		
			cornerPoint = new Location.Location2D(origin.getX()+(float) (hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))),origin.getY()+ (float) (-1 * hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180))));
		}
		//cornerPoint = new Location.Location2D(//co, y)
		return cornerPoint; 
	}
	/**
	 * Is the location being checked visible to vlc device?
	 * 
	 * @param p1,p2 are the coordinates to be checked corresponding to the x,y values
	 * @param x1,y1 are the x,y coordinates of the first point on the triangle, probably the origin -- or starting location of the node/car
	 * @param x2,y2 are the x,y coordinates of the second point on the triangle, 1 corner point calculated by the getVLCBounds method
	 * @param x3,y3 are the x,y coordinates of the third point on the triangle, a second point calculated by the getVLCBounds method
	 * @return true/false of whether or not the location falls within our calculated vlc bounds. 
	 */
	public boolean visibleToVLCdevice(float p1, float p2, float x1, float y1, float x2, float y2, float x3, float y3)		
	{		

		if(tripletOrientation(x1,y1,x2,y2,p1,p2)*tripletOrientation(x1,y1,x2,y2,x3,y3)>0 && tripletOrientation(x2,y2,x3,y3,p1,p2)*tripletOrientation(x2,y2,x3,y3,x1,y1)>0 && tripletOrientation(x3,y3,x1,y1,p1,p2)*tripletOrientation(x3,y3,x1,y1,x2,y2)>0)
		{
			return true;
		}
		else
		{
			return false; 
		}
	}

	public float tripletOrientation(float x1, float y1, float x2, float y2, float x3, float y3)
	{
		return x1*(y2 - y3) + x2*(y3 - y1) + x3*(y1 - y2);  
	}


 

	private LinkedList<Integer> getQualifiyingNodes(int SourceNodeID)
	{
//		int SourceNodeID= radioInfo.getUnique().getID();
		LinkedList<Integer> returnNodes = new LinkedList<Integer>();
		
		//float bearingAngle = JistExperiment.getJistExperiment().visualizer.getField().getRadioData(SourceNodeID).getMobilityInfo().getBearingAsAngle();
		float bearingAngle = Field.getRadioData(SourceNodeID).getMobilityInfo().getBearingAsAngle();

		cornerPoint1 = getVLCCornerPoint(bearingAngle - (visionAngle/2), Field.getRadioData(SourceNodeID).getLocation(), distanceLimit, visionAngle);
		cornerPoint2 = getVLCCornerPoint(bearingAngle + (visionAngle/2), Field.getRadioData(SourceNodeID).getLocation(), distanceLimit, visionAngle);

		//go over all nodes, check if they're inside current radar's range
	//	Polygon poly;
		
		for(int i=1;i<JistExperiment.getJistExperiment().getNodes(); i++) 
		{
				
			float ba= Field.getRadioData(i).getMobilityInfo().getBearingAsAngle();
			Location cp1 = getVLCCornerPoint(ba - (visionAngle/2), Field.getRadioData(i).getLocation(), distanceLimit, visionAngle);
			Location cp2 = getVLCCornerPoint(ba + (visionAngle/2), Field.getRadioData(i).getLocation(), distanceLimit, visionAngle);
		/*	poly = new Polygon();
			poly.addPoint((int)Field.getRadioData(i).getLocation().getX(), (int)Field.getRadioData(i).getLocation().getY());
			poly.addPoint((int)cp1.getX(), (int)cp1.getY());
			poly.addPoint((int)cp2.getX(), (int)cp2.getY());
			JistExperiment.getJistExperiment().visualizer.drawPolygon(poly);*/
			if(SourceNodeID != i)
			{
				if(visibleToVLCdevice(Field.getRadioData(i).getLocation().getX(), Field.getRadioData(i).getLocation().getY(),Field.getRadioData(SourceNodeID).getLocation().getX(), Field.getRadioData(SourceNodeID).getLocation().getY(), cornerPoint2.getX(), cornerPoint2.getY(), cornerPoint1.getX(), cornerPoint1.getY()))
				{
					returnNodes.add(i);
				}
			}
		}
//		JistExperiment.getJistExperiment().visualizer.updateVisualizer();
		//JistExperiment.getJistExperiment().visualizer.pause();
		
		/*System.out.print("VLC visible nodes for me(" +SourceNodeID + ") myloc:"+  Field.getRadioData(SourceNodeID).getLocation().toString()+" are : ");
		for (Integer item : returnNodes) {
			System.out.print(item.toString() + ", ");
		}
		System.out.println("");*/
		return returnNodes;
	}
  
} // class: RadioVLC
