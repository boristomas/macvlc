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
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import javax.management.RuntimeErrorException;
import javax.swing.JEditorPane;

import org.apache.derby.impl.sql.execute.HashScanResultSet;

import com.sun.xml.internal.ws.api.PropertySet.Property;

import jist.runtime.Controller;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.field.Field;
import jist.swans.field.Mobility.StaticInfo;
import jist.swans.mac.MacMessage;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.NetMessage;
import driver.GenericDriver;
import driver.JistExperiment;
import driver.spatial;

/** 
 * <code>RadioNoiseIndep</code> implements a radio with an independent noise model.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: RadioNoiseIndep.java,v 1.1 2007/04/09 18:49:46 drchoffnes Exp $
 * @since SWANS1.0
 */

public final class RadioVLC extends RadioNoise
{
	//�irina: 1,7 +-0.3
	//du�ina: 5+-0.5
	//	private float vehicleDevLength =0;// 0.5F ;
	//	private float vehicleLength =50.0F;//5

	//private float vehicleDevWidth =0;//0.3F;
	//	private float vehicleWidth = 50.0F;//1,7
	public int NodeID;
	//public Location currentLocation;
	private Location newLocation;//used to store new location
	public float vehicleStaticBearing =0F; 

	public enum SensorModes
	{
		Receive(0),
		Send(1),
		//SendAndReceive(2),
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

		private HashSet<Integer> controlSignal = new HashSet<Integer>();

		public VLCsensor(int sensorID, RadioVLC node, float distancelimit, float visionAngle,Location originalLoc, float offsetX, float offsetY, float bearing, SensorModes mode) 
		{
			/*notes:
			 * kod 802.11 vlc radio treba slati i primati sa svih senzora i na taj nacin simulirati omni.
			 * na� mac bi trebao selektivno odabrati pojedini senzor (tx i rx) i samo njega koristiti. 
			 * �to je sa preklapanjima, doga�ati �e se da �e i lijevi i desni primiti signal 
			 * MAC:
			 * *Bearing(vektor, GPS), sve za topologiju, hardverski razmjestaj senzora na vozilu.
			 * *svaki cvor ce znati kada ec moci nekome poslati poruku i kada ce nekoga moci cuti 
			 * 
			 * LINKS:
			 * http://en.wikipedia.org/wiki/MIMO
			 * http://djw.cs.washington.edu/papers/mimo_for_dummies.pdf
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

			sensorLocation = rotatePoint(NodeLocation.getX()+ offsetX, NodeLocation.getY()+ offsetY, NodeLocation, NodeBearing); //new Location.Location2D(tmpx, tmpy);//start.
			sensorLocation1 = getVLCCornerPoint(sensorBearingNotRelative - (visionAngle/2), sensorLocation, distanceLimit, visionAngle);
			sensorLocation2 = getVLCCornerPoint(sensorBearingNotRelative + (visionAngle/2), sensorLocation, distanceLimit, visionAngle);
			//	if(node.NodeID == nodeidtst)
			{
				poly = new Polygon();
				poly.addPoint((int)sensorLocation.getX(), (int)sensorLocation.getY());
				poly.addPoint((int)sensorLocation1.getX(), (int)sensorLocation1.getY());
				poly.addPoint((int)sensorLocation2.getX(), (int)sensorLocation2.getY());
				if(mode == SensorModes.Receive)
				{

					GenericDriver.btviz.getGraph().setColor(Color.yellow);
				}
				else
				{
					GenericDriver.btviz.getGraph().setColor(Color.red);
				}
				GenericDriver.btviz.getGraph().drawPolygon(poly);
				GenericDriver.btviz.getGraph().setColor(Color.cyan);
				GenericDriver.btviz.getGraph().drawString(""+sensorID, (int)sensorLocation.getX()+5, (int)sensorLocation.getY());
			}
			//	System.out.println("draw bt "+ sensorBearingNotRelative + " - nb= "+NodeBearing+" - "+(int)sensorLocation.getX() + " "+ (int)sensorLocation.getY() + " " +(int)sensorLocation1.getX() + " "+ (int)sensorLocation1.getY() +" " +(int)sensorLocation2.getX() + " "+ (int)sensorLocation2.getY()  );
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
	public LinkedList<VLCsensor> sensorsTx = new LinkedList<RadioVLC.VLCsensor>();
	public LinkedList<VLCsensor> sensorsRx = new LinkedList<RadioVLC.VLCsensor>();
	public int lineOfSight = 30;
	protected double thresholdSNR;
	private float offsetx;
	private float offsety;
	private Location startLocation; //start location set on ctor.
	public Path2D.Double outlineShape;
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
	/*	public RadioVLC(int id, RadioInfo.RadioInfoShared sharedInfo, Location location)
	{
		this(id, sharedInfo, Constants.SNR_THRESHOLD_DEFAULT, location);
	}*/

	/**
	 * Create new radio with independent noise model.
	 *
	 * @param id radio identifier
	 * @param sharedInfo shared radio properties
	 * @param thresholdSNR threshold signal-to-noise ratio
	 */
	public RadioVLC(int id, RadioInfo.RadioInfoShared sharedInfo, double thresholdSNR, Location location, float staticBearing)
	{
		/*90rx
		30tx
		los 30m
		4rx 4tx
		 */
		super(id, sharedInfo);
		this.NodeID = id;

		if(nodeidtst == -1)
		{
			nodeidtst = id;
		}
		setThresholdSNR(thresholdSNR);

		Random rand = new Random();

		vehicleStaticBearing = staticBearing;
		//System.out.println("bt bearing start nid "+NodeID + " - "+ vehicleStaticBearing);
		startLocation = location;
		//offsets are half length from center point to edge of vehicle. example vehicle length is 5m and width is 2m. xoffset is 2.5 and yoffset is 1. if
		//offsetx = (float) ((vehicleLength + (rand.nextFloat()*2*vehicleDevLength)-vehicleDevLength)/2);
		//offsety = (float) ((vehicleWidth + (rand.nextFloat()*2*vehicleDevWidth)-vehicleDevWidth)/2);
		if(!JistExperiment.getJistExperiment().MeasurementMode)
		{
			offsetx = (float) ((JistExperiment.getJistExperiment().getVehicleLength() + (rand.nextFloat()*2*JistExperiment.getJistExperiment().getVehicleLengthDev())-JistExperiment.getJistExperiment().getVehicleLengthDev())/2);
			offsety = (float) ((JistExperiment.getJistExperiment().getVehicleWidth() + (rand.nextFloat()*2*JistExperiment.getJistExperiment().getVehicleWidthDev())-JistExperiment.getJistExperiment().getVehicleWidthDev())/2);
		}
		else
		{
			offsetx = (float) ((JistExperiment.getJistExperiment().getVehicleLength() + (JistExperiment.getJistExperiment().getVehicleLengthDev())-JistExperiment.getJistExperiment().getVehicleLengthDev())/2);
			offsety = (float) ((JistExperiment.getJistExperiment().getVehicleWidth() + (JistExperiment.getJistExperiment().getVehicleWidthDev())-JistExperiment.getJistExperiment().getVehicleWidthDev())/2);
		}

		checkLocation(true);

		float visionTx = JistExperiment.getJistExperiment().getVLCvisionAngleTx();
		float visionRx = JistExperiment.getJistExperiment().getVLCvisionAngleRx();

		//right
		sensorsTx.add(new VLCsensor(1, this, lineOfSight,visionTx , location, offsetx, offsety, 0, SensorModes.Send));//front Tx
		sensorsTx.add(new VLCsensor(2, this, lineOfSight, visionTx, location, -1*offsetx, offsety, 180, SensorModes.Send));//back Tx

		//left
		sensorsTx.add(new VLCsensor(3, this, lineOfSight, visionTx, location, offsetx, -1*offsety, 0, SensorModes.Send));//front Tx
		sensorsTx.add(new VLCsensor(4, this, lineOfSight, visionTx, location, -1*offsetx, -1*offsety, 180, SensorModes.Send));//back Tx

		sensorsRx.add(new VLCsensor(5, this, lineOfSight, visionRx, location, offsetx, 0, 0, SensorModes.Receive));//front Rx
		sensorsRx.add(new VLCsensor(6, this, lineOfSight, visionRx, location, -1*offsetx, 0, 180, SensorModes.Receive));//back Rx

		//	sensorsRx.add(new VLCsensor(6, this, lineOfSight, 70, location, offsetx, -1*offsety, 0, SensorModes.Receive));//front Rx
		//	sensorsRx.add(new VLCsensor(8, this, lineOfSight, 70, location, -1*offsetx, -1*offsety, 180, SensorModes.Receive));//back Rx
		//	checkLocation(true);
		//sensorsRx.add(new VLCsensor(5, this, lineOfSight, 70, location, offsetx, offsety, 0, SensorModes.Receive));//front Rx
		//sensorsRx.add(new VLCsensor(6, this, lineOfSight, 70, location, -1*offsetx, offsety, 180, SensorModes.Receive));//back Rx



		//if((id % 2) == 0)
		{

			//		this.setControlSignal(2, 7);
			//		this.setControlSignal(2, 3);
		}
	}

	public float GetBearing()
	{

		if( JistExperiment.getJistExperiment().mobility == Constants.MOBILITY_STATIC)
		{
			return vehicleStaticBearing;
		}	
		return Field.getRadioData(NodeID).getMobilityInfo().getBearingAsAngle();
	}
	public VLCsensor getSensorByID(int id)
	{
		for (VLCsensor sensor: sensorsTx) {

			if(sensor.sensorID == id)
			{
				return sensor;
			}
		}
		for (VLCsensor sensor: sensorsRx) {

			if(sensor.sensorID == id)
			{
				return sensor;
			}
		}
		return null;
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


	//private ArrayList<ControlSignal> controlSignals = new ArrayList<RadioVLC.ControlSignal>();

	private VLCsensor tmpsensor;
	/**
	 * sets Control signal for current radio and sensor
	 * 
	 * @param value
	 */

	public void setControlSignal(int sensorID, Integer channelID)
	{
		/*if(channelID <= 0 || channelID == 127)
		{
			throw new RuntimeException("invalid values: less than 0 and 127");
		}*/
		tmpsensor = getSensorByID(sensorID);

		if(tmpsensor != null)
		{
			if(tmpsensor.mode != SensorModes.Send)
			{
				throw new RuntimeException("Signal can not be set to a non sending sensor. change sensor to set signal.");
			}
			tmpsensor.controlSignal.add(channelID);
		}
	}

	/**
	 * Clears sensor control signal
	 * @param sensorID
	 */
	public void clearControlSignal (int sensorID, byte channelID)
	{
		tmpsensor = getSensorByID(sensorID);
		if(tmpsensor != null)
		{
			tmpsensor.controlSignal.remove(channelID);
		}
	}
	/**
	 * gets sensor control signal
	 * @param sensorID
	 * @return
	 */
	public boolean getControlSignal(int sensorID, Integer channelID)
	{
		tmpsensor = getSensorByID(sensorID);
		if(tmpsensor != null)
		{
			return tmpsensor.controlSignal.contains(channelID);
		}
		return false;
	}

	public boolean queryControlSignal (int sensorID, Integer channelID)
	{
		byte returnValue = 0;
		boolean tmpVal =false;
		tmpsensor = getSensorByID(sensorID);
		if(tmpsensor != null)
		{
			if(tmpsensor.mode != SensorModes.Receive)
			{
				throw new RuntimeException("sensor mode is not receiving");
			}
			this.checkLocation(false);//updating location of the node and sensors and bearings etc
			for (Integer[] node : getRangeAreaNodesAndSensors(this.NodeID,SensorModes.Receive, sensorID)) 
			{
				tmpVal = Field.getRadioData(node[0]).vlcdevice.getControlSignal(node[1], channelID);	
				if(tmpVal)
				{
					return true;
				}
			}
		}
		else
		{
			throw new RuntimeException("sensor not found");
		}
		return false;		
	}



	public static int nodeidtst = -1;
	Location tmpLoc;
	/****
	 * Checks and updated location of sensor nodes as well the node itself.
	 * @param isStartCheck use false.
	 */
	public void checkLocation(Boolean isStartCheck)
	{
		if( !isStartCheck)// .equals(obj)getRadioData(NodeID) != null)
		{
			newLocation =Field.getRadioData(NodeID).getLocation();
			if(vehicleStaticBearing != -1)
			{
				NodeBearing = vehicleStaticBearing;
			}
			else
			{
				NodeBearing = Field.getRadioData(NodeID).getMobilityInfo().getBearingAsAngle();
			}
		}
		else//startcheck = false
		{
			newLocation = startLocation;
			if(vehicleStaticBearing != -1)
			{
				NodeBearing = vehicleStaticBearing;
			}
			else
			{
				NodeBearing = 0;
			}
		}
		//	System.out.println("bt bearing 1 nid "+NodeID + " - " +NodeBearing);
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
		for (VLCsensor sensor : sensorsTx) 
		{
			sensor.UpdateShape(NodeLocation, NodeBearing);
		}
		for (VLCsensor sensor : sensorsRx) 
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


		/*outlineShape = new Polygon();
		outlineShape.addPoint((int)Ax, (int)Ay);
		outlineShape.addPoint((int)Bx, (int)By);
		outlineShape.addPoint((int)Cx, (int)Cy);
		outlineShape.addPoint((int)Dx, (int)Dy);
		 */

		outlineShape = new Path2D.Double();
		outlineShape.moveTo(Ax, Ay);
		outlineShape.lineTo(Bx, By);
		outlineShape.lineTo(Cx, Cy);
		outlineShape.lineTo(Dx, Dy);
		outlineShape.closePath();

		/*	if(nodeidtst == -1)
		{
			if(!isStartCheck)
			{
				nodeidtst = NodeID;
			}
		}
		 */
		//	if(NodeID == nodeidtst)
		if(isStartCheck)
		{
			//TODO: maknuti ovo nodeidtst jer sluzi samo za testiranje vizualizacije.
			GenericDriver.btviz.getGraph().setColor(Color.black);
			//GenericDriver.btviz.getGraph().drawRect(, y, width, height);, yPoints, nPoints); fillPolygon(outlineShape);//.drawRect((int)tmpx1, (int)tmpy1, 20 , 20);
			GenericDriver.btviz.getGraph().setColor(Color.red);
			GenericDriver.btviz.getGraph().drawString(""+NodeID, (int)NodeLocation.getX(), (int)NodeLocation.getY());
			//GenericDriver.btviz.getGraph().setColor(Color.red);
		}
	}
	public static Location rotatePoint(float ptx, float pty, Location center, double angleDeg)
	{
		double angleRad = (angleDeg/180)*Math.PI;

		double cosAngle = Math.cos(angleRad );
		double sinAngle = Math.sin(angleRad );
		double dx = (ptx-center.getX());
		double dy = (pty-center.getY());

		ptx = center.getX() +  (float)(dx*cosAngle-dy*sinAngle);
		pty = center.getY() +  (float)(dx*sinAngle+dy*cosAngle);
		return new Location.Location2D(ptx, pty);
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
		if(msg instanceof MacMessage.Data)
		{
			((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(25, "radiovlct-rec", null));
		}
		checkLocation(false);

		if(((MacMessage)msg).getSensorIDRx().size() == 0)
		{
			for (VLCsensor item : this.sensorsRx)
			{
				((MacMessage)msg).setSensorIDRx(item.sensorID);
			}
		}

		//if(((MacMessage)msg).getDst().hashCode() == -1)
		//{
		//samo ako je bcast onda na receive provjeravam can talk, inace ne.
		//u cantalk proslje�ujem id cvora koji je primio bcast poruku.
		msg = CanTalk(((MacMessage)msg).getSrc().hashCode(), radioInfo.unique.getID(), SensorModes.Receive, (MacMessage)msg);
		if(msg == null )
		{
			Constants.VLCconstants.DroppedOnReceive++;
			return;
		}
		Constants.VLCconstants.Received++;
		//}
		//else, nije bcast, provjera moze li talk se radi na transmit strani, tako da je cantalk provjera redundantna.


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

	//bt
	public long getTime()
	{
		return JistAPI.getTime();
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
		//System.out.println("nid = " +NodeID+ " lx= "+this.NodeLocation.getX()+" ly= "+this.NodeLocation.getY()+" Csignal  = " + this.queryControlSignal(5,2));

		assert(signalBuffer==null);
		signalBuffer = null;

		//if sensors are not set, set all of them -> add them to the list of sending sensorids of message.
		if(((MacMessage)msg).getSensorIDTx().size() == 0)
		{
			for (VLCsensor item : this.sensorsTx) 
			{
				((MacMessage)msg).setSensorIDTx(item.sensorID);
			}
		}		

		if(((MacMessage)msg).getDst().hashCode() != -1)
		{
			//not bcast

			/*msg = CanTalk(((MacMessage)msg).getSrc().hashCode(), ((MacMessage)msg).getDst().hashCode(), SensorModes.Send, (MacMessage)msg);
			if(msg == null)
			{
				Constants.VLCconstants.DroppedOnSend++;
				return;
			}*/
			Constants.VLCconstants.SentDirect++;
		}
		else
		{
			//bcast je
			Constants.VLCconstants.SentBroadcast++;
		}

		if(msg instanceof MacMessage.Data)
		{
			/*for (TimeEntry item : ((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times) {
				if(item.TimeID == 2)
				{
					System.out.println("aaa");//obrisi ovaj for. 
				}
			}*/
			((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(2, "radiovlct", null));
		}
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
	private HashSet<Integer> tmpSensorTx = new HashSet<Integer>();
	private HashSet<Integer> tmpSensorRx = new HashSet<Integer>();

	/**
	 * Checks if two nodes can talk, more specific if sensors can talk according to address in msg.
	 * @param SourceID macaddr of source.
	 * @param DestinationID macaddr of destination.
	 * @param mode sensor mode, if sent or received.
	 * @param msg message to be sent.
	 * @return
	 */
	private MacMessage CanTalk(int SourceID, int DestinationID, SensorModes mode, MacMessage msg)
	{
		if(DestinationID != -1)//znaci da nije broadcast poruka, teoretski nikada nece sourceid biti -1 odnosno broadcast adresa
		{
			tmpSensorRx.clear();
			tmpSensorTx.clear();
			//		possibleNodes = new HashSet<Integer>();
			if(mode == SensorModes.Send)
			{//znaci sourceid �alje
				//never happens!

				possibleNodes.addAll(getRangeAreaNodes(SourceID, mode,-1));//send mode
				tmpNodeList = getRangeAreaNodes(DestinationID, SensorModes.Receive,-1);

				if(possibleNodes.contains(DestinationID) && tmpNodeList.contains(SourceID))
				{

				}
				else
				{
					//nisu si ni u trokutu
					msg= null;
				}
			}
			else if( mode == SensorModes.Receive)
			{//znaci source slu�a poruke
				possibleNodes =getRangeAreaNodes(SourceID, mode,-1);//send mode
				tmpNodeList = getRangeAreaNodes(DestinationID, SensorModes.Send,-1);//TODO: test

				if(possibleNodes.contains(DestinationID) && tmpNodeList.contains(SourceID))
				{
					possibleNodes.addAll(tmpNodeList);
					if(msg.getSensorIDTx().size() != 0 && msg.getSensorIDRx().size() != 0)
					{
						//u ovom slucaju su poznate stxid i srxid liste.
						for (VLCsensor sensorSrc : Field.getRadioData(SourceID).vlcdevice.sensorsTx) {
							if(msg.getSensorIDTx().contains(sensorSrc.sensorID))
							{
								for (VLCsensor sensorDest : Field.getRadioData(DestinationID).vlcdevice.sensorsRx) {
									if(msg.getSensorIDRx().contains(sensorDest.sensorID))
									{
										//znaci da listam sve senzore na src i dest koji su zadani u msg sensor listama
										for (Integer node : possibleNodes)//listam sve aute koji su mi vidljivi (u trokutu)
										{
											if(!intersects(Field.getRadioData(node).vlcdevice.outlineShape, new Line2D.Float(sensorSrc.sensorLocation.getX(), sensorSrc.sensorLocation.getY(), sensorDest.sensorLocation.getX(), sensorDest.sensorLocation.getY())))
											{
												tmpSensorTx.add(sensorSrc.sensorID);
												tmpSensorRx.add(sensorDest.sensorID);
											}
										}//foreach possiblenodes
									}
								}
							}
						}
						msg.SensorIDTx.clear();
						msg.SensorIDTx.addAll(tmpSensorTx);
						msg.SensorIDRx.clear();
						msg.SensorIDRx.addAll(tmpSensorRx);
						if(msg.SensorIDTx.size() == 0 || msg.SensorIDRx.size() == 0)
						{
							msg = null;//dropped
						}
					}
					else
					{
						System.out.println("neva2");
					}
				}
				else
				{
					msg=  null; //nisu si ni u trokutu.
				}
			}
			else
			{
				//should never happen, use send or receive
				msg=  null;
			}
		}
		else//broadcast poruka je
		{
			//nikada se nece dogoditi jer bcast filteriram na transmit metodi
			msg = null;
		}
		return msg;
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
	 * @param sensorID use sensorID = -1 to check all sensors. setting to specific sensor will check only for that sensor.
	 * @return
	 */
	private HashSet<Integer[]> getRangeAreaNodesAndSensors(int SourceNodeID, SensorModes mode, int sensorID)
	{
		//ne moram sve senzore gledati od send cvora nego samo one koji su u listi, tako se barem malo smanji optereecnje, 
		//teorettski dva
		//puta provjeravam odnose dva senzor cvora!!!!!
		HashSet<Integer[]> returnNodes = new HashSet<Integer[]>();
		LinkedList<RadioVLC.VLCsensor> sensors1 = new LinkedList<RadioVLC.VLCsensor>();
		LinkedList<RadioVLC.VLCsensor> sensors2 = new LinkedList<RadioVLC.VLCsensor>();
		sensors1.add(Field.getRadioData(SourceNodeID).vlcdevice.getSensorByID(sensorID));
		for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
		{	

			//BT todo: treba uzeti samo cvorove koji su blizu, za sada sam checkloc stavio za sve!! a to ne valja :( ***
			if(SourceNodeID != i)
			{
				boolean stopSearch = false;
				Field.getRadioData(i).vlcdevice.checkLocation(false);
				if(mode == SensorModes.Send)
				{
					sensors2 = Field.getRadioData(i).vlcdevice.sensorsRx;
				}
				else //receive
				{
					sensors2 = Field.getRadioData(i).vlcdevice.sensorsTx;
				}


				for (VLCsensor sensor :sensors1)
				{
					//	if(stopSearch)
					//	break;
					for (VLCsensor sensor2 :sensors2)
					{
						if(visibleToVLCdevice(sensor2.sensorLocation.getX(), sensor2.sensorLocation.getY(),sensor))// sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY()))
						{
							//	stopSearch = true;
							Integer[] a =new Integer[]{i,sensor2.sensorID};
							returnNodes.add(a);
							//break;
						}
					}
				}//for my sensors
			}//if not me
		}//for all nodes
		return returnNodes;
	}


	/**
	 * Gets the list of nodeIDs that source can see
	 * @param SourceNodeID
	 * @param sensorID use senorID = -1 to check all sensors. setting to specific sensor will check only for that sensor.
	 * @return
	 */
	private HashSet<Integer> getRangeAreaNodes(int SourceNodeID, SensorModes mode, int sensorID)
	{
		//ne moram sve senzore gledati od send cvora nego samo one koji su u listi, tako se barem malo smanji optereecnje, teorettski dva
		//puta provjeravam odnose dva senzor cvora!!!!!
		HashSet<Integer> returnNodes = new HashSet<Integer>();
		LinkedList<RadioVLC.VLCsensor> sensors1 = new LinkedList<RadioVLC.VLCsensor>();
		LinkedList<RadioVLC.VLCsensor> sensors2 = new LinkedList<RadioVLC.VLCsensor>();
		for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
		{	
			if(SourceNodeID != i)
			{
				boolean stopSearch = false;
				if(mode == SensorModes.Send)
				{
					if(sensorID == -1)
					{
						sensors1 = Field.getRadioData(SourceNodeID).vlcdevice.sensorsTx;
					}
					sensors2 = Field.getRadioData(i).vlcdevice.sensorsRx;
				}
				else //receive
				{
					if(sensorID == -1)
					{
						sensors1 = Field.getRadioData(SourceNodeID).vlcdevice.sensorsRx;
					}
					sensors2 = Field.getRadioData(i).vlcdevice.sensorsTx;
				}
				if(sensorID != -1)
				{
					sensors1.add(Field.getRadioData(SourceNodeID).vlcdevice.getSensorByID(sensorID));
				}
				for (VLCsensor sensor :sensors1)
				{
					if(stopSearch)
						break;
					for (VLCsensor sensor2 :sensors2)
					{
						if(visibleToVLCdevice(sensor2.sensorLocation.getX(), sensor2.sensorLocation.getY(),sensor))// sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY()))
						{
							stopSearch = true;
							returnNodes.add(i);
							break;
						}
					}
				}//for my sensors
			}//if not me
		}//for all nodes
		return returnNodes;
	}

	public static boolean intersects(Path2D.Double path, Line2D line) {
		double x1 = -1 ,y1 = -1 , x2= -1, y2 = -1;
		for (PathIterator pi = path.getPathIterator(null); !pi.isDone(); pi.next()) 
		{
			double[] coordinates = new double[6];
			switch (pi.currentSegment(coordinates))
			{
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
			{
				if(x1 == -1 && y1 == -1 )
				{
					x1= coordinates[0];
					y1= coordinates[1];
					break;
				}				
				if(x2 == -1 && y2 == -1)
				{
					x2= coordinates[0];				
					y2= coordinates[1];
					break;
				}
				break;
			}
			}
			if(x1 != -1 && y1 != -1 && x2 != -1 && y2 != -1)
			{
				Line2D segment = new Line2D.Double(x1, y1, x2, y2);
				if (segment.intersectsLine(line)) 
				{
					return true;
				}
				x1 = -1;
				y1 = -1;
				x2 = -1;
				y2 = -1;
			}
		}
		return false;
	} 

} // class: RadioVLC
