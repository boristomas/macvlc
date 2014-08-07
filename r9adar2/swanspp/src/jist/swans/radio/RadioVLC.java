//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioNoiseIndep.java Tue 2004/04/20 09:00:20 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import javax.management.RuntimeErrorException;
import javax.swing.JEditorPane;

import org.apache.derby.impl.sql.execute.HashScanResultSet;
import org.omg.CORBA.SystemException;

import com.sun.xml.internal.ws.api.PropertySet.Property;

import jist.runtime.Controller;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.field.Field;
import jist.swans.field.Mobility.StaticInfo;
import jist.swans.mac.MacInterface;
import jist.swans.mac.MacMessage;
import jist.swans.mac.MacMessageVLC;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.NetMessage;
import jist.swans.radio.VLCsensor.SensorModes;
import jist.swans.radio.VLCsensor.SensorStates;
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
	public int NodeID;
	//public Location currentLocation;
	private Location newLocation;//used to store new location
	public float vehicleStaticBearing = 0F;
	/**
	 * Automatically set control channel to busy on receive.
	 */
	public static boolean AutoSetControlOnRx =true;

	//////////////////////////////////////////////////
	// locals
	//

	/**
	 * threshold signal-to-noise ratio.
	 */
	private VLCsensor tmpsensor;
	public LinkedList<VLCsensor> sensorsTx = new LinkedList<VLCsensor>();
	public LinkedList<VLCsensor> sensorsRx = new LinkedList<VLCsensor>();
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
			offsetx = (float) ((JistExperiment.getJistExperiment().getVehicleLength())/2);
			offsety = (float) ((JistExperiment.getJistExperiment().getVehicleWidth())/2);
		}

		UpdateNodeShape(true);

		float visionTx = JistExperiment.getJistExperiment().getVLCvisionAngleTx();
		float visionRx = JistExperiment.getJistExperiment().getVLCvisionAngleRx();

		//right
		sensorsTx.add(new VLCsensor(1, this, lineOfSight, visionTx, location,    offsetx, offsety, 0, SensorModes.Transmit));//front Tx
		sensorsTx.add(new VLCsensor(2, this, lineOfSight, visionTx, location, -1*offsetx, offsety, 180, SensorModes.Transmit));//back Tx

		//left
		sensorsTx.add(new VLCsensor(3, this, lineOfSight, visionTx, location,    offsetx, -1*offsety, 0, SensorModes.Transmit));//front Tx
		sensorsTx.add(new VLCsensor(4, this, lineOfSight, visionTx, location, -1*offsetx, -1*offsety, 180, SensorModes.Transmit));//back Tx

		sensorsRx.add(new VLCsensor(5, this, lineOfSight, visionRx, location,    offsetx, 0, 0, SensorModes.Receive));//front Rx
		sensorsRx.add(new VLCsensor(6, this, lineOfSight, visionRx, location, -1*offsetx, 0, 180, SensorModes.Receive));//back Rx

		if(JistExperiment.getJistExperiment().getMACProtocol().contains("VLC"))
		{
			isVLC = true;
		}//nije bas elegantno :(
		else
		{
			isVLC = false;
		}
		//	sensorsRx.add(new VLCsensor(6, this, lineOfSight, 70, location, offsetx, -1*offsety, 0, SensorModes.Receive));//front Rx
		//	sensorsRx.add(new VLCsensor(8, this, lineOfSight, 70, location, -1*offsetx, -1*offsety, 180, SensorModes.Receive));//back Rx
		//	checkLocation(true);
		//sensorsRx.add(new VLCsensor(5, this, lineOfSight, 70, location, offsetx, offsety, 0, SensorModes.Receive));//front Rx
		//sensorsRx.add(new VLCsensor(6, this, lineOfSight, 70, location, -1*offsetx, offsety, 180, SensorModes.Receive));//back Rx



		//if((id % 2) == 0)
		//{

		//		this.setControlSignal(2, 7);
		//		this.setControlSignal(2, 3);
		//	}
	}
	public static boolean isVLC = false;
	public float getBearing()
	{

		if( JistExperiment.getJistExperiment().mobility == Constants.MOBILITY_STATIC)
		{
			return vehicleStaticBearing;
		}	
		return Field.getRadioData(NodeID).getMobilityInfo().getBearingAsAngle();
	}
	public VLCsensor GetSensorByID(int id)
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


	/**
	 * sets Control signal for current radio and sensor
	 * 
	 * @param value
	 * @author BorisTomas
	 */

	public void setControlSignal(int sensorID, Integer channelID)
	{
		setControlSignal(GetSensorByID(sensorID), channelID);
	}
	/**
	 * sets control signal, if set for receiving sensor this method will find neared same bearing transmitting sensors and set control signal on them.
	 * @param sensor
	 * @param channelID
	 */
	public void setControlSignal(VLCsensor sensor, Integer channelID)
	{
		if(sensor != null)
		{
			if(sensor.mode != SensorModes.Transmit)
			{
				for (VLCsensor item : getNearestOpositeSensor(sensor)) 
				{
					item.controlSignal.add(channelID);
				}
				//throw new RuntimeException("Signal can not be set to a non sending sensor. change sensor to set signal.");
			}
			else
			{
				sensor.controlSignal.add(channelID);
			}
		}
	}

	public LinkedList<VLCsensor> getNearestOpositeSensor (HashSet<Integer> sensors)
	{
		LinkedList<VLCsensor> returnme = new LinkedList<VLCsensor>();
		for (Integer sensor : sensors) 
		{
			for (VLCsensor item : getNearestOpositeSensor(GetSensorByID(sensor))) 
			{
				if(!returnme.contains(item))
				{
					returnme.add(item);
				}
			}
		}
		return returnme;

	}
	/**
	 * Gets the nearest sensor of the opposite
	 * @param sensor
	 * @param mode
	 * @return
	 */
	public LinkedList<VLCsensor> getNearestOpositeSensors (VLCsensor[] sensors)
	{	
		LinkedList<VLCsensor> returnme = new LinkedList<VLCsensor>();	

		for (VLCsensor sensor : sensors) 
		{
			for (VLCsensor item : getNearestOpositeSensor(sensor)) 
			{
				if(!returnme.contains(item))
				{
					returnme.add(item);
				}
			}
		}
		return returnme;
	}
	public LinkedList<VLCsensor> getNearestOpositeSensor (VLCsensor sensor)
	{
		LinkedList<VLCsensor> returnme = new LinkedList<VLCsensor>();	

		if(sensor.mode == SensorModes.Receive)
		{
			for (VLCsensor item : sensorsTx) 
			{
				if(item.Bearing == sensor.Bearing)
				{
					if(!returnme.contains(item))
					{
						returnme.add(item);
					}
				}
			}
		}
		else
		{//transmit je
			for (VLCsensor item : sensorsRx) 
			{
				if(item.Bearing == sensor.Bearing)
				{
					if(!returnme.contains(item))
					{
						returnme.add(item);
					}
				}
			}

		}

		return returnme;
	}

	public boolean areAllIdle(SensorModes mode)
	{

		switch (mode) {
		case Receive:
		{

			for (VLCsensor item: sensorsRx) 
			{
				if(item.state != SensorStates.Idle)
				{
					return false;
				}
			}	
			break;
		}
		case Transmit:
		{
			for (VLCsensor item: sensorsTx) 
			{
				if(item.state != SensorStates.Idle)
				{
					return false;
				}
			}
			break;
		}
		default:
		{
			throw new RuntimeException("mode should be transmit or receive");

		}
		}
		return true;
	}

	public boolean IsAtLeastOneIdle(SensorModes mode)
	{
		switch (mode) {
		case Receive:
		{
			for (VLCsensor item: sensorsRx) 
			{
				if(item.state == SensorStates.Idle)
				{
					return true;
				}
			}	
			break;
		}
		case Transmit:
		{
			for (VLCsensor item: sensorsTx) 
			{
				if(item.state == SensorStates.Idle)
				{
					return true;
				}
			}
			break;
		}
		default:
		{
			throw new RuntimeException("mode should be transmit or receive");

		}
		}
		return false;
	}
	/**
	 * Gets only the list of sensorIDs.
	 * @param mode
	 * @return
	 * @author BorisTomas
	 */
	public HashSet<Integer> GetSensorsIDs(SensorModes mode)
	{
		HashSet<Integer> returnme = new HashSet<Integer>();

		switch (mode) {
		case Receive:
		{
			for (VLCsensor item: sensorsRx) 
			{
				returnme.add(item.sensorID);
			}	
			break;
		}
		case Transmit:
		{
			for (VLCsensor item: sensorsTx) 
			{
				returnme.add(item.sensorID);
			}
			break;
		}
		default:
		{
			throw new RuntimeException("mode should be transmit or receive");

		}
		}

		return returnme;
	}

	/**
	 * Clears sensor control signal
	 * @param sensorID
	 * @author BorisTomas
	 */
	public void clearControlSignal (int sensorID, int channelID)
	{
		clearControlSignal(GetSensorByID(sensorID), channelID);

	}
	/**
	 * Clears sensor control signal
	 * @param sensor
	 * @param channelID
	 * @author BorisTomas
	 */
	public void clearControlSignal (VLCsensor sensor, int channelID)
	{
		if(sensor != null)
		{
			if(sensor.mode == SensorModes.Transmit)
			{
				sensor.controlSignal.remove(channelID);
			}
			else
			{
				for (VLCsensor item : getNearestOpositeSensor(sensor)) 
				{
					item.controlSignal.remove(channelID);
				}
			}
		}
	}
	/**
	 * gets sensor control signal
	 * @param sensorID
	 * @return
	 */
	public boolean getControlSignal(int sensorID, Integer channelID)
	{
		tmpsensor = GetSensorByID(sensorID);
		if(tmpsensor != null)
		{
			return tmpsensor.controlSignal.contains(channelID);
		}
		return false;
	}

	public boolean queryControlSignal (int sensorID, Integer channelID)
	{
		return queryControlSignal(GetSensorByID(sensorID), channelID);
	}
	public boolean queryControlSignal (VLCsensor sensor, Integer channelID)
	{
		boolean tmpVal =false;

		if(sensor != null)
		{
			if(sensor.mode != SensorModes.Receive)
			{
				throw new RuntimeException("sensor mode is not receiving");
			}
			this.UpdateNodeShape(false);//updating location of the node and sensors and bearings etc
			for (Integer[] node : getRangeAreaNodesAndSensors(this.NodeID,SensorModes.Receive, sensor.sensorID)) 
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
	public void UpdateNodeShape(Boolean isStartCheck)
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
			GenericDriver.btviz.DrawShape(outlineShape, Color.black);
			GenericDriver.btviz.DrawString(NodeID+"", Color.BLUE, NodeLocation.getX(),  NodeLocation.getY());
			//TODO: maknuti ovo nodeidtst jer sluzi samo za testiranje vizualizacije.
			//	
			//GenericDriver.btviz.getGraph().drawRect(, y, width, height);, yPoints, nPoints); fillPolygon(outlineShape);//.drawRect((int)tmpx1, (int)tmpy1, 20 , 20);
			//	GenericDriver.btviz.getGraph().setColor(Color.red);
			//	GenericDriver.btviz.getGraph().drawString(""+NodeID, (int)NodeLocation.getX(), (int)NodeLocation.getY());
			//GenericDriver.btviz.getGraph().setColor(Color.red);
		}
	}
	protected static Location rotatePoint(float ptx, float pty, Location center, double angleDeg)
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

	//	public Dictionary<Sensor, Message> messagesOnAir;
	//////////////////////////////////////////////////
	// reception
	//
	// RadioInterface interface
	/** {@inheritDoc} */
	public void receive(Message msg, Double powerObj_mW, Long durationObj)
	{ 


		((NetMessage.Ip)((MacMessageVLC)msg).getBody()).Times.add(new TimeEntry(250, "radiovlct-rec", null));
		if(mode==Constants.RADIO_MODE_SLEEP) return;

		if(!isVLC && mode == Constants.RADIO_MODE_TRANSMITTING)
		{
			System.out.println("receiving while transmitting is not allowed.");
			//radio ne moze primat i slati, ako pocne ista primati a traje slanje jednostavno cu discardati poruku.

			return;
		}
		final double power_mW = powerObj_mW.doubleValue();
		final long duration = durationObj.longValue();
		// ignore if below sensitivity
		if(power_mW < radioInfo.shared.sensitivity_mW) 
		{
			System.out.println("message is too weak.");
			return;
		}

		UpdateNodeShape(false);
		for (VLCsensor item : this.sensorsRx)
		{
			((MacMessage)msg).addSensorIDRx(item.sensorID, NodeID);
		}//postavio sam sve senzore, u can talk se ce filtrirati samo oni koji mogu vidjeti poruku.



		//u cantalk prosljeðujem id cvora koji je primio poruku.
		//provjeravam jesu li si u piti ili ne.

		if(power_mW < radioInfo.shared.threshold_mW || power_mW < radioInfo.shared.background_mW * thresholdSNR)
		{
			msg = null;
		}
		else
		{
			msg = CheckPhyConditions(((MacMessage)msg).getSrc().hashCode(), NodeID, SensorModes.Receive, (MacMessage)msg);
		}

		if(msg == null)
		{
			//Constants.VLCconstants.DroppedOnReceive++;
			return;
		}





		//Constants.VLCconstants.Received++;

		for (int item : ((MacMessage)msg).getSensorIDRx(NodeID))//.SensorIDRx)
		{
			tmpSensorReceive = GetSensorByID(item);
			((MacMessage)msg).setStartRx(tmpSensorReceive, JistAPI.getTime());
			((MacMessage)msg).setEndRx(tmpSensorReceive, JistAPI.getTime() + duration);
			((MacMessage)msg).setPowerRx(tmpSensorReceive,  power_mW);
			((MacMessage)msg).setInterferedRx(tmpSensorReceive, false);// .InterferedRx = false;
			tmpSensorReceive.Messages.addFirst((MacMessageVLC)msg);
			//	tmpSensorReceive.signalsRx ++;
			setControlSignal(tmpSensorReceive, 1);
			if(tmpSensorReceive.mode == SensorModes.Receive)
			{//ok
				if(tmpSensorReceive.state != SensorStates.Idle)
				{
					if(isVLC)
					{
						(macEntity).notifyReceiveFail(msg, Constants.MacVlcErrorSensorRxIsBusy);
					}
					else
					{//obicni mac
						//TODO: validirati ovo!!! 
						continue;
					}
				}
				else
				{
					tmpSensorReceive.setState(SensorStates.Receiving);
					setMode(Constants.RADIO_MODE_RECEIVING);
					//ok je.
				}
			}
			else
			{
				//nikada se ne bi trebalo desiti
				( macEntity).notifyReceiveFail(msg, Constants.MacVlcErrorSensorIsNotRX);
				return;

			}
		}

		if(msg instanceof MacMessageVLC)
		{
			//mjerenje vremena.
			((NetMessage.Ip)((MacMessageVLC)msg).getBody()).Times.add(new TimeEntry(251, "radiovlct-rec", null));
		}

		///mode set
		if(isVLC)
		{
			this.macEntity.peek(msg);
		}
		else
		{//neki obican mac je.
			switch(mode)
			{
			case Constants.RADIO_MODE_IDLE:
				if(msg!=null) setMode(Constants.RADIO_MODE_RECEIVING);
				lockSignal(msg, power_mW, duration);
				break;
			case Constants.RADIO_MODE_RECEIVING:
				if(power_mW >= radioInfo.shared.threshold_mW &&  power_mW > signalPower_mW * thresholdSNR)
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
		}
		// schedule an endReceive
		JistAPI.sleep(duration); 
		self.endReceive(powerObj_mW, new Long(seqNumber));
	}

	private int msgcounter;
	private double cumpower;
	// RadioInterface interface
	/** {@inheritDoc} */
	public void endReceive(final Double powerObj_mW, Long seqNumber)
	{
		if(mode==Constants.RADIO_MODE_SLEEP) return;

		if(isVLC)
		{
			for (VLCsensor item : sensorsRx)
			{
				if(item.state == SensorStates.Receiving)
				{
					msgcounter = 0;
					cumpower = 0;
					for (MacMessage msg1 : item.Messages) 
					{
						if(msg1.getEndRx(item) > JistAPI.getTime())
						{
							//brojim poruke da znam da postoji transmisija u zraku;
							msgcounter++;
						}
						if(msg1.getEndRx(item) == JistAPI.getTime())
						{
							cumpower = 0;
							//dohvatiti trenutne u zraku.
							for (MacMessage msg2 : item.Messages) 
							{
								//dohvatiti trenutne u zraku.
								if(msg1 != msg2)//&& !msg1.Interfered && !msg2.Interfered)
								{
									//u msg1 imam poruku koja je taman zavrsila, u msg2 imam sve ostale poruke.
									if(msg2.getStartRx(item)< msg1.getEndRx(item) && msg2.getEndRx(item) > msg1.getStartRx(item))
									{
										//ako je bilo koja poruka pocela prije trenutne koju gledam.
										cumpower += msg2.getPowerRx(item);
									}
								}
							}//for msg2;

							if(cumpower == 0 || Util.toDB(msg1.getPowerRx(item)/cumpower) > 16)//16 = tablica 2 iz: http://www.cisco.com/c/en/us/td/docs/wireless/technology/mesh/7-3/design/guide/Mesh/Mesh_chapter_011.pdf
							{
								//ok je poruka se moze primiti
								((NetMessage.Ip)(((MacMessageVLC)msg1).getBody())).Times.add(new TimeEntry(252, "macbtrec", null));
								String aa="";
								for (int item2 : (msg1).getSensorIDTx((msg1).getSrc().hashCode()))
								{
									aa += item2 + " ";
								}
								String bb="";
								for (int item3 : (msg1).getSensorIDRx(NodeID))
								{
									bb += item3 + " ";
								}
					//			System.out.println("r - n: "+NodeID+ " tm: "+JistAPI.getTime()+" s: "+(msg1).getSrc()+ "("+aa+") d: "+(msg1).getDst() +"("+bb+") mhs: " + msg1.hashCode());
								if(msgcounter == 0)
								{
									//znaci da se dogodila interferencija i da su sve poruke koje su kolidirane dosle.
									clearControlSignal(item, (byte)1);
									item.setState(SensorStates.Idle );
								}
								this.macEntity.receive(msg1);
								return;
							}
							else
							{
								//nije ok, msg1 je interferirana
								msg1.setInterferedRx(item, true);// .InterferedRx= true;
								this.macEntity.notifyInterference(msg1, item);
							}
						}
					}//for msg1



					if(msgcounter == 0)
					{
						//znaci da se dogodila interferencija i da su sve poruke koje su kolidirane dosle.
						clearControlSignal(item, (byte)1);
						item.setState(SensorStates.Idle );
					}
				}
			}
		}
		else
		{//neki obican mac je.

			signals--;
			if(mode==Constants.RADIO_MODE_RECEIVING)
			{
				if(signalBuffer!=null && JistAPI.getTime()==signalFinish)
				{
					this.macEntity.receive(signalBuffer);
					unlockSignal();
				}
				else
				{

					//interferencija

					//					System.out.println("interferencija");
				}
				if(signals==0) setMode(Constants.RADIO_MODE_IDLE);
			}
			else
			{
				System.out.println("nevaa3");

			}
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

	VLCsensor tmpSensorTransmit;
	VLCsensor tmpSensorReceive;
	VLCsensor tmpSensorReceiveEnd;
	boolean isAtLeastOneTransmitting = false;
	// RadioInterface interface
	/** {@inheritDoc} */
	public void transmit(Message msg, long delay, long duration)
	{

		/*	if(msg instanceof MacMessageVLC)
		{
			//mjerenje vremena

		}*/
		((NetMessage.Ip)((MacMessageVLC)msg).getBody()).Times.add(new TimeEntry(2, "radiovlct", null));

		// radio in sleep mode
		if(mode == Constants.RADIO_MODE_SLEEP) return;

		///ako poruka nema postavljene senzore postavljam u sve.
		//if sensors are not set, set all of them -> add them to the list of sending sensorids of message.
		if(((MacMessage)msg).getSensorIDTx(NodeID).size() == 0)
		{
			for (VLCsensor item : this.sensorsTx) 
			{
				((MacMessage)msg).addSensorIDTx(item.sensorID, NodeID);
			}
		}
		isAtLeastOneTransmitting = false;
		//System.out.println("radio: "+((MacMessage)msg).getSensorIDTx().toString() );
		for (int item : ((MacMessage)msg).getSensorIDTx(NodeID))
		{
			tmpSensorTransmit= this.GetSensorByID(item);
			if(tmpSensorTransmit.mode == SensorModes.Transmit)
			{
				//ok
				if(tmpSensorTransmit.state == SensorStates.Idle )
				{
					//ok
					tmpSensorTransmit.setState(SensorStates.Transmitting );

					((MacMessage)msg).setEndTx(tmpSensorTransmit, JistAPI.getTime()+duration + delay);
					((MacMessage)msg).setStartTx(tmpSensorTransmit, JistAPI.getTime());
					((MacMessage)msg).setDurationTx(tmpSensorTransmit, duration + delay);
					tmpSensorTransmit.Messages.addFirst((MacMessageVLC)msg);

					isAtLeastOneTransmitting = true;
				}
				else if(tmpSensorTransmit.state == SensorStates.Transmitting)
				{
					if(isVLC)
					{
						(macEntity).notifyTransmitFail(msg, Constants.MacVlcErrorSensorTxIsBusy);
					}
					//ako je dobar mac ovo se ne smjelo desiti.
					//setMode(Constants.RADIO_MODE_TRANSMITTING);
					continue;
				}
				else
				{
					//nikada se ne bi smjelo desiti; jer bi znacilo da je senzor receive i da se koristi za transmit.
					return;
				}
			}
			else
			{
				if(isVLC)
				{
					(macEntity).notifyTransmitFail(msg, Constants.MacVlcErrorSensorIsNotTX);
				}
				//isto se ne bi smjelo desiti
				return;
			}
		}// for all tx sensor defined in message.

		if(isVLC)
		{
			if(!isAtLeastOneTransmitting)
			{
				macEntity.notifyTransmitFail(msg, Constants.MacVlcErrorSensorTxAllBusy);	
			}

		}
		else
		{// obicni neki mac je.
			if(!isAtLeastOneTransmitting)
			{
				//nema tko poslati poruku jer su svi zauzeti.
				return;
			}
			if(mode == Constants.RADIO_MODE_TRANSMITTING) throw new RuntimeException("radio already transmitting");
			if(mode == Constants.RADIO_MODE_RECEIVING) 
			{
				System.out.println("radio receiving, can't transmit at the same time");
				return;//dodao jer mislim da radio ne bi trebao slati poruku ako ju vec prima, ali samo za ne vlc mac
			}
			signalBuffer = null;
		}

		// ensure not currently transmitting
		UpdateNodeShape(false);
		// clear receive buffer
		//assert(signalBuffer==null);

		// use default delay, if necessary
		if(delay==Constants.RADIO_NOUSER_DELAY) delay = Constants.RADIO_PHY_DELAY;
		// set mode to transmitting
		setMode(Constants.RADIO_MODE_TRANSMITTING);

		((NetMessage.Ip)((MacMessageVLC)msg).getBody()).Times.add(new TimeEntry(21, "radiovlct", null));
		// schedule message propagation delay
		JistAPI.sleep(delay);
		String aa="";
		for (int item : ((MacMessage)msg).getSensorIDTx(NodeID))
		{

			aa += item + " ";
		}
		String bb="";
		for (int item : ((MacMessage)msg).getSensorIDRx(NodeID))
		{
			bb += item + " ";
		}
	//	System.out.println("t - n: "+NodeID+ " tm: "+JistAPI.getTime()+" s: "+((MacMessageVLC)msg).getSrc()+ "("+aa+") d: "+((MacMessageVLC)msg).getDst() +"("+bb+") end: "+(duration+getTime()) + " mhs: " + msg.hashCode());
		fieldEntity.transmit(radioInfo, msg, duration);
		// schedule end of transmission
		JistAPI.sleep(duration);
		self.endTransmit();
	}

	private boolean isStillTransmitting = false;
	// RadioInterface interface
	/** {@inheritDoc} */
	public void endTransmit()
	{
		if(isVLC)
		{
			if(mode==Constants.RADIO_MODE_SLEEP) return;


			//if(mode!=Constants.RADIO_MODE_TRANSMITTING) throw new RuntimeException("radio is not transmitting");
			isStillTransmitting = false;
			for (VLCsensor item : sensorsTx)
			{
				if(item.state == SensorStates.Transmitting )
				{
					if (item.Messages.getFirst().getEndTx(item) == JistAPI.getTime()) 
					{
						item.state = SensorStates.Idle;
						//			item.CurrentMessages.getFirst().EndTx = 0;//TODO: provjeriti trebam li ovo ponistiti
					}
					else
					{
						isStillTransmitting = true;
					}
				}
			}
			if(!isStillTransmitting)
			{
				setMode(Constants.RADIO_MODE_IDLE);
			}
		}
		else
		{
			// radio in sleep mode
			if(mode==Constants.RADIO_MODE_SLEEP) return;
			// check that we are currently transmitting
			if(mode!=Constants.RADIO_MODE_TRANSMITTING) throw new RuntimeException("radio is not transmitting");
			// set mode
			setMode(signals>0 ? Constants.RADIO_MODE_RECEIVING : Constants.RADIO_MODE_IDLE);
		}
	}

	//bt
	private HashSet<Integer> possibleNodes;// =  new HashSet<Integer>();
	private HashSet<Integer> tmpNodeList;
	//	private HashSet<Integer> tmpSensorTx = new HashSet<Integer>();
	private HashSet<Integer> tmpSensorRx;

	/**
	 * Checks if two nodes can talk, more specific if sensors can talk according to address in msg.
	 * @param SourceID macaddr of source.
	 * @param DestinationID macaddr of destination.
	 * @param mode sensor mode, if sent or received.
	 * @param msg message to be sent.
	 * @return
	 */
	private MacMessage CheckPhyConditions(int SourceID, int DestinationID, SensorModes mode, MacMessage msg)
	{
		tmpSensorRx = new HashSet<Integer>();
		if(DestinationID != -1)//znaci da nije broadcast poruka, teoretski nikada nece sourceid biti -1 odnosno broadcast adresa
		{
			//			tmpSensorTx.clear();
			//		possibleNodes = new HashSet<Integer>();
			if(mode == SensorModes.Transmit)
			{//znaci sourceid šalje
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
			{//znaci source sluša poruke
				possibleNodes =getRangeAreaNodes(SourceID, SensorModes.Receive,-1);//send mode
				tmpNodeList = getRangeAreaNodes(DestinationID, SensorModes.Transmit,-1);//TODO: test

				if(possibleNodes.contains(DestinationID) && tmpNodeList.contains(SourceID))
				{
					possibleNodes.addAll(tmpNodeList);
					if(/*msg.getSensorIDTx(SourceID).size() != 0 &&*/ msg.getSensorIDRx(DestinationID).size() != 0)
					{
						//u ovom slucaju su poznate stxid i srxid liste.
						for (VLCsensor sensorSrc : Field.getRadioData(SourceID).vlcdevice.sensorsTx)
						{
							if(msg.getSensorIDTx(SourceID).contains(sensorSrc.sensorID))
							{//ako je mac zadao da se šalje sa odreðenog senzora.
								for (VLCsensor sensorDest : Field.getRadioData(DestinationID).vlcdevice.sensorsRx)
								{
									//znaci da listam sve senzore na src i dest koji su zadani u msg sensor listama
									if (IsSensorVisibleToSensor(sensorSrc, sensorDest)) 
									{
										//			tmpSensorTx.add(sensorSrc.sensorID);
										tmpSensorRx.add(sensorDest.sensorID);
									}
								}
							}
						}
						//	msg.SensorIDTx.clear();
						//			msg.setSensorIDTx(tmpSensorTx, SourceID);
						//		msg.SensorIDRx.clear();
						msg.setSensorIDRx(tmpSensorRx, DestinationID);

						if(/*msg.getSensorIDTxSize(SourceID) == 0 ||*/ msg.getSensorIDRxSize(DestinationID) == 0)
						{
							msg = null;//dropped
						}
					}
					else
					{
						if(msg.getSensorIDTxSize(SourceID) == 0)
						{
							System.out.println("neva2 "+ msg.hashCode() + " cnt = "+msg.getSensorIDTxSize(SourceID));
						}
						else
						{
							System.out.println("neva4 "+ msg.hashCode());
						}
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

	public static boolean intersects(Path2D.Double path, Line2D line) {
		double x1 = -1 ,y1 = -1 , x2= -1, y2 = -1,sx=-1,sy=-1;
		/*	Color boja1 = Color.black;
		Color boja2 = Color.orange;
		GenericDriver.btviz.GetFrame().repaint();
		GenericDriver.btviz.getGraph().clearRect(0, 0,GenericDriver.btviz.GetFrame().getSize().width , GenericDriver.btviz.GetFrame().getSize().height);
		GenericDriver.btviz.DrawShape(path, Color.red, 3);*/
		for (PathIterator pi = path.getPathIterator(null); !pi.isDone(); pi.next()) 
		{
			double[] coordinates = new double[6];
			switch (pi.currentSegment(coordinates))
			{
			case PathIterator.SEG_CLOSE:
			{
				coordinates[0]=sx;
				coordinates[1]=sy;
			}
			case PathIterator.SEG_MOVETO:
			{
				if(sx == -1)
				{
					sx= coordinates[0];
					sy= coordinates[1];
				}
			}
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

			default:
			{
				break;
			}
			}
			if(x1 != -1 && y1 != -1 && x2 != -1 && y2 != -1)
			{
				Line2D segment = new Line2D.Double(x1, y1, x2, y2);
				/*	if(boja1 == Color.black)
				{
					boja1 = Color.blue;
					boja2 = Color.green;
				}
				else
				{
					boja1 = Color.black;
					boja2 = Color.orange;
				}
				GenericDriver.btviz.DrawShape(segment, boja1,3);
				GenericDriver.btviz.DrawShape(line, boja2,2);*/
				if (segment.intersectsLine(line)) 
				{
					return true;
				}
				x1 = x2;
				y1 = y2;
				x2 = -1;
				y2 = -1;
			}
		}
		return false;
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
	/*public boolean visibleToVLCdevice(float p1, float p2, float x1, float y1, float x2, float y2, float x3, float y3)		
	{		

		if(tripletOrientation(x1,y1,x2,y2,p1,p2)*tripletOrientation(x1,y1,x2,y2,x3,y3)>0 && tripletOrientation(x2,y2,x3,y3,p1,p2)*tripletOrientation(x2,y2,x3,y3,x1,y1)>0)// && tripletOrientation(x3,y3,x1,y1,p1,p2)*tripletOrientation(x3,y3,x1,y1,x2,y2)>0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}*/
	/**
	 * Checks shape and los and obstacles (other nodes)
	 * @param sensor1
	 * @param sensor2
	 * @return
	 */
	public boolean IsSensorVisibleToSensor(VLCsensor sensor1, VLCsensor sensor2 )
	{
		if(Point.distance(sensor1.sensorLocation.getX(), sensor1.sensorLocation.getY(), sensor2.sensorLocation.getX(), sensor2.sensorLocation.getY()) > Math.min(sensor1.distanceLimit, sensor2.distanceLimit))
		{
			return false;
		}
		else
		{
			if(!sensor1.coverageShape.contains(sensor2.sensorLocation.getX(), sensor2.sensorLocation.getY()))
			{
				return false;
			}
			if(!sensor2.coverageShape.contains(sensor1.sensorLocation.getX(), sensor1.sensorLocation.getY()))
			{
				return false;
			}
			//provjeravam LOS
			for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
			{
				if(intersects(Field.getRadioData(i).vlcdevice.outlineShape, new Line2D.Double(sensor1.sensorLocation.getX(), sensor1.sensorLocation.getY(), sensor2.sensorLocation.getX(), sensor2.sensorLocation.getY())))
				{
					return false;
				}
			}
			return true;
		}

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
		LinkedList<VLCsensor> sensors1 = new LinkedList<VLCsensor>();
		LinkedList<VLCsensor> sensors2 = new LinkedList<VLCsensor>();
		sensors1.add(Field.getRadioData(SourceNodeID).vlcdevice.GetSensorByID(sensorID));
		for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
		{	

			if(SourceNodeID != i)
			{
				Field.getRadioData(i).vlcdevice.UpdateNodeShape(false);
				if(mode == SensorModes.Transmit)
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
						if(IsSensorVisibleToSensor(sensor2,sensor))// sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY()))
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
	 * @param sensorID use sensorID = -1 to check all sensors. setting to specific sensor will check only for that sensor.
	 * @return
	 */
	private HashSet<Integer> getRangeAreaNodes(int SourceNodeID, SensorModes mode, int sensorID)
	{
		//TODO: ne moram sve senzore gledati od send cvora nego samo one koji su u listi, tako se barem malo smanji optereecnje
		HashSet<Integer> returnNodes = new HashSet<Integer>();
		LinkedList<VLCsensor> sourceSensors = new LinkedList<VLCsensor>();
		LinkedList<VLCsensor> destinationSensors = new LinkedList<VLCsensor>();

		if(mode == SensorModes.Transmit)
		{
			if(sensorID == -1)
			{
				sourceSensors = Field.getRadioData(SourceNodeID).vlcdevice.sensorsTx;
			}
		}
		else //receive
		{
			if(sensorID == -1)
			{
				sourceSensors = Field.getRadioData(SourceNodeID).vlcdevice.sensorsRx;
			}
		}
		if(sensorID != -1)
		{
			sourceSensors.add(Field.getRadioData(SourceNodeID).vlcdevice.GetSensorByID(sensorID));
		}


		for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
		{	
			if(SourceNodeID != i)
			{
				boolean stopSearch = false;
				if(mode == SensorModes.Transmit)
				{
					destinationSensors = Field.getRadioData(i).vlcdevice.sensorsRx;
				}
				else //receive
				{
					destinationSensors = Field.getRadioData(i).vlcdevice.sensorsTx;
				}
				for (VLCsensor sensor :sourceSensors)
				{
					if(stopSearch)
						break;
					for (VLCsensor sensor2 :destinationSensors)
					{
						if(IsSensorVisibleToSensor(sensor2,sensor))// sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY()))
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


} // class: RadioVLC
