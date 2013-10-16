//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioNoiseIndep.java Tue 2004/04/20 09:00:20 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.field.Field;
import jist.swans.mac.MacMessage;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import driver.GenericDriver;
import driver.JistExperiment;

/** 
 * <code>RadioNoiseIndep</code> implements a radio with an independent noise model.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: RadioNoiseIndep.java,v 1.1 2007/04/09 18:49:46 drchoffnes Exp $
 * @since SWANS1.0
 */

public final class RadioVLC extends RadioNoise
{
	//širina: 1,7 +-0.3
	//dužina: 5+-0.5
	private float vehicleDevLength = 0.5F ;
	private float vehicleLength =5.0F;//5

	private float vehicleDevWidth =0.3F;
	private float vehicleWidth = 1.7F;//1,7
	public int NodeID;
	//public Location currentLocation;
	private Location newLocation;//used to store new location

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
		public float sensorBearing = 0;
		public int sensorID = 0;
		public SensorModes mode;
		public RadioVLC node;
		private Location sensorLocation;
		private Location sensorLocation1;//top
		private Location sensorLocation2;//bottom
		private float sensorBearingNotRelative;
		private float stickOut = 0.001F; //1cm


		public VLCsensor(int sensorID, RadioVLC node, float distancelimit, float visionAngle,Location originalLoc, float offsetX, float offsetY, float bearing, SensorModes mode) 
		{
			/*notes:
			 * kod 802.11 vlc radio treba slati i primati sa svih senzora i na taj nacin simulitari omni.
			 * naš mac bi trebao selektivno odabrati pojedini senzor (tx i rx) i samo njega koristiti. 
			 * Što je sa preklapanjima, dogaðati æe se da æe i lijevi i desni primiti signal 
			 * */
			this.node = node;
			this.distanceLimit = distancelimit; 
			this.visionAngle = visionAngle;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			if(this.offsetX >0)
			{
				this.offsetX += stickOut;
			}
			else
			{
				this.offsetX -= stickOut;
			}
			if(this.offsetY >0)
			{
				this.offsetY += stickOut;
			}
			else
			{
				this.offsetY -= stickOut;
			}
			this.sensorBearing = bearing;
			this.sensorID = sensorID;
			this.mode = mode;
			UpdateShape(originalLoc, node.NodeBearing);
		}
		private Polygon poly;
		public void UpdateShape(Location NodeLocation, float NodeBearing)
		{
			sensorBearingNotRelative = NodeBearing + sensorBearing;
			//tmploc = rotatePoint(NodeLocation.getX()+ offsetX, NodeLocation.getY()+ offsetY, NodeLocation, sensorBearingNotRelative);

			//	tmpx= (float) ((NodeLocation.getX()+ offsetX)* Math.cos(NodeBearing) - (NodeLocation.getY() + offsetY) * Math.sin(NodeBearing));
			//	tmpy= (float) ((NodeLocation.getX()+ offsetX)* Math.sin(NodeBearing) + (NodeLocation.getY() + offsetY) * Math.cos(NodeBearing));

			sensorLocation = rotatePoint(NodeLocation.getX()+ offsetX, NodeLocation.getY()+ offsetY, NodeLocation, sensorBearingNotRelative); //new Location.Location2D(tmpx, tmpy);//start.
			sensorLocation1 = getVLCCornerPoint(sensorBearingNotRelative - (visionAngle/2), sensorLocation, distanceLimit, visionAngle);
			sensorLocation2 = getVLCCornerPoint(sensorBearingNotRelative + (visionAngle/2), sensorLocation, distanceLimit, visionAngle);
			if(node.NodeID == nodeidtst)
			{
				poly = new Polygon();
				poly.addPoint((int)sensorLocation.getX(), (int)sensorLocation.getY());
				poly.addPoint((int)sensorLocation1.getX(), (int)sensorLocation1.getY());
				poly.addPoint((int)sensorLocation2.getX(), (int)sensorLocation2.getY());
				GenericDriver.btviz.getGraph().drawPolygon(poly);
			}
		}

		/**
		 * This method will need to be called two times. Each call will detect one corner point of the vlc device's view
		 * @param theta: the angle of the bearing +- visionAngle
		 * @param origin: starting point
		 * @param distanceLimit: the max distance the vlc device can see
		 * @param visionAngle: the angle at which the vlc device can see from the front of the car
		 */
		public Location getVLCCornerPoint(float theta, Location origin, float distLimit, float visionAngle)
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

		/*public LinkedList<Location> getCoverageNodes()
		{

		}*/

	}//VLCsensor
	//////////////////////////////////////////////////
	// locals
	//

	/**
	 * threshold signal-to-noise ratio.
	 */
	public LinkedList<VLCsensor> sensors = new LinkedList<RadioVLC.VLCsensor>();
	public int lineOfSight = 30;
	protected double thresholdSNR;
//	float distanceLimit = 250; 	//distance limit the vlc device can see in front of it, def:250
	//int visionAngle = 60; 		//The viewing angle the of the vlc device, default is 60 degrees
	private float offsetx;
	private float offsety;
	private Location startLocation; //start location set on ctor.
	public Polygon outlineShape;
	float tmpx1, tmpy1, tmpx2, tmpy2;
	float Ax,Ay, Bx,By,Cx,Cy,Dx,Dy;
	float NodeBearing =0;
	Location NodeLocation;

	//////////////////////////////////////////////////
	// initialize
	//

	/**
	 * Create new radio with independent noise model.
	 *
	 * @param id radio identifier
	 * @param sharedInfo shared radio properties
	 */
	public RadioVLC(int id, RadioInfo.RadioInfoShared sharedInfo, Location location)
	{
		this(id, sharedInfo, Constants.SNR_THRESHOLD_DEFAULT, location);
	}

	/**
	 * Create new radio with independent noise model.
	 *
	 * @param id radio identifier
	 * @param sharedInfo shared radio properties
	 * @param thresholdSNR threshold signal-to-noise ratio
	 */
	public RadioVLC(int id, RadioInfo.RadioInfoShared sharedInfo, double thresholdSNR, Location location)
	{
		/*90rx
		30tx
		los 30m
		4rx 4tx
		 */
		super(id, sharedInfo);
		this.NodeID = id;
		setThresholdSNR(thresholdSNR);
		//http://en.wikipedia.org/wiki/Rotation_matrix
		Random rand = new Random();
		startLocation = location;
		//offsets are half length from center point to edge of vehicle. example vehicle length is 5m and width is 2m. xoffset is 2.5 and yoffset id 1. if
		offsetx = (float) ((vehicleLength + (rand.nextFloat()*2*vehicleDevLength)-vehicleDevLength)/2);
		offsety = (float) ((vehicleWidth + (rand.nextFloat()*2*vehicleDevWidth)-vehicleDevWidth)/2);
		checkLocation(true);

		
		//left
		sensors.add(new VLCsensor(1, this, lineOfSight, 30, location, offsetx, offsety, 0, SensorModes.Send));//front Tx
		sensors.add(new VLCsensor(2, this, lineOfSight, 90, location, offsetx, offsety, 0, SensorModes.Receive));//front Rx
		sensors.add(new VLCsensor(3, this, lineOfSight, 30, location, -1*offsetx, offsety, 180, SensorModes.Send));//back Tx
		sensors.add(new VLCsensor(4, this, lineOfSight, 90, location, -1*offsetx, offsety, 180, SensorModes.Receive));//front Rx

		//right
		sensors.add(new VLCsensor(5, this, lineOfSight, 30, location, offsetx, -1*offsety, 0, SensorModes.Send));//front Tx
		sensors.add(new VLCsensor(6, this, lineOfSight, 90, location, offsetx, -1*offsety, 0, SensorModes.Receive));//front Rx
		sensors.add(new VLCsensor(7, this, lineOfSight, 30, location, -1*offsetx, -1*offsety, 180, SensorModes.Send));//back Tx
		sensors.add(new VLCsensor(8, this, lineOfSight, 90, location, -1*offsetx, -1*offsety, 180, SensorModes.Receive));//front Rx

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

	public  static Location rotatePoint(float ptx, float pty, Location center, double angleDeg)
	{
		double angleRad = (angleDeg/180)*Math.PI;
		double cosAngle = Math.cos(angleRad );
		double sinAngle = Math.sin(angleRad );
		double dx = (ptx-center.getX());
		double dy = (pty-center.getY());

		ptx = center.getX() + (int) (dx*cosAngle-dy*sinAngle);
		pty = center.getY() + (int) (dx*sinAngle+dy*cosAngle);
		return new Location.Location2D(ptx, pty);
	}
	public static int nodeidtst = -1;
	Location tmpLoc;
	public void checkLocation(Boolean isStartCheck)
	{
		if( !isStartCheck)// .equals(obj)getRadioData(NodeID) != null)
		{
			newLocation =Field.getRadioData(NodeID).getLocation();
			NodeBearing = Field.getRadioData(NodeID).getMobilityInfo().getBearingAsAngle();
		}
		else
		{
			newLocation = startLocation;
			NodeBearing = 0;
		}
		if(NodeLocation != null)
		{//provjeravam lokaciju jel nova ili ne
			if(NodeLocation.getX() == newLocation.getX())
			{
				if(NodeLocation.getY() == newLocation.getY())
				{
					
					return;//nema promjena
					//TODO: bearing change not used, it is assumed that vehicle can't rotate in place.
				}
			}
			//ima promjena lokacije
		}
		NodeLocation= newLocation;
		for (VLCsensor sensor : sensors) 
		{
			sensor.UpdateShape(NodeLocation, NodeBearing);
		}
		//update node shape (rectangle)

		tmpLoc = rotatePoint(NodeLocation.getX() - offsetx, NodeLocation.getY() - offsety, NodeLocation, NodeBearing);
		Ax = tmpLoc.getX();
		Ay = tmpLoc.getY();

		tmpLoc = rotatePoint(NodeLocation.getX() + offsetx, NodeLocation.getY() - offsety, NodeLocation, NodeBearing);
		Bx = tmpLoc.getX();
		By = tmpLoc.getY();

		tmpLoc = rotatePoint(NodeLocation.getX() + offsetx, NodeLocation.getY() + offsety, NodeLocation, NodeBearing);
		Cx = tmpLoc.getX();
		Cy = tmpLoc.getY();

		tmpLoc = rotatePoint(NodeLocation.getX() - offsetx, NodeLocation.getY() + offsety, NodeLocation, NodeBearing);
		Dx = tmpLoc.getX();
		Dy = tmpLoc.getY();

		outlineShape = new Polygon();
		outlineShape.addPoint((int)Ax, (int)Ay);
		outlineShape.addPoint((int)Bx, (int)By);
		outlineShape.addPoint((int)Cx, (int)Cy);
		outlineShape.addPoint((int)Dx, (int)Dy);

		if(nodeidtst == -1)
		{
			if(!isStartCheck)
			{
				nodeidtst = NodeID;
			}
		}

		if(NodeID == nodeidtst)
		{
			//TODO: maknuti ovo nodeidtst jer sluzi samo za testiranje vizualizacije.
			//GenericDriver.btviz.getGraph().setColor(Color.RED);
			GenericDriver.btviz.getGraph().fillPolygon(outlineShape);//.drawRect((int)tmpx1, (int)tmpy1, 20 , 20);
		}
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
		{
			return;
		}
		checkLocation(false);
		// discard message if below threshold
		if(!CanTalk(((MacMessage)msg).getSrc().hashCode(), radioInfo.unique.getID(), SensorModes.Receive))
		{
			//System.out.println("TALK rcv :( ");
			return;
		}

		System.out.println("TALK rcv :) msg hc: " + msg.hashCode() + " from: " +((MacMessage)msg).getSrc().hashCode()+ " to: " + radioInfo.unique.getID()  );
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
		checkLocation(false);
		assert(signalBuffer==null);
		signalBuffer = null;

		if(((MacMessage)msg).getDst().hashCode() != -1)
		{

			if(!CanTalk(((MacMessage)msg).getSrc().hashCode(), ((MacMessage)msg).getDst().hashCode(), SensorModes.Send))
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
	private HashSet<Integer> possibleNodes;// =  new HashSet<Integer>();
	private HashSet<Integer> tmpNodeList;
	private boolean CanTalk(int SourceID, int DestinationID, SensorModes mode)
	{
		if(DestinationID != -1)//znaci da nije broadcast poruka, teoretski nikada nece soruceid biti -1 odnosno broadcast adresa
		{
			possibleNodes = new HashSet<Integer>();
			if(mode == SensorModes.Send)
			{//znaci sourceid šalje
				possibleNodes.addAll(getRangeAreaNodes(SourceID, mode));
				tmpNodeList = getRangeAreaNodes(DestinationID, SensorModes.Receive);

				if(possibleNodes.contains(DestinationID) && tmpNodeList.contains(SourceID))
				{
					possibleNodes.addAll(tmpNodeList);
					//	possibleNodes.remove(SourceID);
					//	possibleNodes.remove(DestinationID);
					for (VLCsensor sensorSrc : Field.getRadioData(SourceID).vlcdevice.sensors)
					{
						for (VLCsensor sensorDest : Field.getRadioData(DestinationID).vlcdevice.sensors)
						{
							for (Integer node : possibleNodes)
							{		
								if(!intersects(Field.getRadioData(node).vlcdevice.outlineShape, new Line2D.Float( sensorSrc.sensorLocation.getX(), sensorSrc.sensorLocation.getY(), sensorDest.sensorLocation.getX(), sensorDest.sensorLocation.getY())))
								{
									//ako pronaðem
									return true;
								}
							}//foreach possiblenodes
						}//foreach sensors in dest
					}//foreach sensor in src
					return false;
				}
				else
				{
					//cak si ni nisu u trokutu.
					return false;
				}
			}
			else if( mode == SensorModes.Receive)
			{//znaci source sluša poruke

				possibleNodes.addAll(getRangeAreaNodes(SourceID, mode));
				tmpNodeList = getRangeAreaNodes(DestinationID, SensorModes.Send);

				if(possibleNodes.contains(DestinationID) && tmpNodeList.contains(SourceID))
				{
					possibleNodes.addAll(tmpNodeList);
					//	possibleNodes.remove(SourceID);
					//	possibleNodes.remove(DestinationID);

					for (VLCsensor sensorSrc : Field.getRadioData(SourceID).vlcdevice.sensors)
					{
						for (VLCsensor sensorDest : Field.getRadioData(DestinationID).vlcdevice.sensors)
						{
							for (Integer node : possibleNodes)
							{
								//								if(!Field.getRadioData(node).vlcdevice.outlineShape.intersectsLine(sensorSrc.sensorLocation.getX(), sensorSrc.sensorLocation.getY(), sensorDest.sensorLocation.getX(), sensorDest.sensorLocation.getY()))
								if(!intersects(Field.getRadioData(node).vlcdevice.outlineShape, new Line2D.Float( sensorSrc.sensorLocation.getX(), sensorSrc.sensorLocation.getY(), sensorDest.sensorLocation.getX(), sensorDest.sensorLocation.getY())))
								{
									return true;
								}
							}//foreach possiblenodes
						}//foreach sensors in dest
					}//foreach sensor in src
					return false;
				}
				else
				{
					//cak si ni nisu u trokutu.
					return false;
				}	
			}
			else
			{
				//should never happen, use send or receive
				return false;
			}
		}
		else//broadcast poruka je
		{
			return false;
		}
		//return false;
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

		if(tripletOrientation(x1,y1,x2,y2,p1,p2)*tripletOrientation(x1,y1,x2,y2,x3,y3)>0 && tripletOrientation(x2,y2,x3,y3,p1,p2)*tripletOrientation(x2,y2,x3,y3,x1,y1)>0)// && tripletOrientation(x3,y3,x1,y1,p1,p2)*tripletOrientation(x3,y3,x1,y1,x2,y2)>0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public boolean visibleToVLCdevice(float p1, float p2, VLCsensor sensor )		
	{	
		if(Math.sqrt(Math.pow((p1-sensor.sensorLocation.getX()),2) + Math.pow((p2-sensor.sensorLocation.getY()),2)) > sensor.distanceLimit)
		{
			return false;
		}
		//TODO: testirati radi li pita
		return visibleToVLCdevice(p1, p2, sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY());
	}

	public float tripletOrientation(float x1, float y1, float x2, float y2, float x3, float y3)
	{
		return x1*(y2 - y3) + x2*(y3 - y1) + x3*(y1 - y2);  
	}



	/**
	 * Gets the list of nodeIDs that source can see
	 * @param SourceNodeID
	 * @return
	 */
	private HashSet<Integer> getRangeAreaNodes(int SourceNodeID, SensorModes mode)
	{
		HashSet<Integer> returnNodes = new HashSet<Integer>();
		for(int i=1;i<JistExperiment.getJistExperiment().getNodes(); i++) 
		{	
			if(SourceNodeID != i)
			{
				boolean stopSearch = false;
				for (VLCsensor sensor : Field.getRadioData(SourceNodeID).vlcdevice.sensors) 
				{
					if(stopSearch)
						break;
					if(sensor.mode == mode)
					{
						for (VLCsensor sensor2 : Field.getRadioData(i).vlcdevice.sensors) 
						{
							if(sensor2.mode != mode)
							{//znaci obrnuto je
//								if(visibleToVLCdevice(sensor2.sensorLocation.getX(), sensor2.sensorLocation.getY(),sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY()))
								if(visibleToVLCdevice(sensor2.sensorLocation.getX(), sensor2.sensorLocation.getY(),sensor))// sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY()))
								{
									stopSearch = true;
									returnNodes.add(i);
									break;
								}
							}
						}
					}
				}//for my sensors
			}//if not me
		}//for all nodes

		return returnNodes;
	}
	public static boolean intersects(Polygon poly, Line2D line) {
		for(int i=0; i < poly.npoints; i++) {
			int j = (i+1) % poly.npoints;
			int x1 = poly.xpoints[i];
			int y1 = poly.ypoints[i];
			int x2 = poly.xpoints[j];
			int y2 = poly.ypoints[j];
			Line2D edge = new Line2D.Float(x1, y1, x2, y2);
			if (edge.intersectsLine(line)) {
				return true;
			}
		}
		return false;
	} 

} // class: RadioVLC
