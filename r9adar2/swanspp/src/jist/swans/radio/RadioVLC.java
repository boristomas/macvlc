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

		checkLocation(true);

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
	public boolean isVLC = false;
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


	/**
	 * sets Control signal for current radio and sensor
	 * 
	 * @param value
	 * @author BorisTomas
	 */

	public void setControlSignal(int sensorID, Integer channelID)
	{
		setControlSignal(getSensorByID(sensorID), channelID);
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
	/**
	 * Gets the nearest sensor of the opposite
	 * @param sensor
	 * @param mode
	 * @return
	 */
	public HashSet<VLCsensor> getNearestOpositeSensor (VLCsensor sensor)
	{
		//TODO: ovo bi trebalo biti dinamicno, da gledam bearing i ako je isti ili barem slican tek onda vratiti
		
		HashSet<VLCsensor> returnme = new HashSet<VLCsensor>();	
		//1 3 5 - front 
		//2 4 6 - back
		if(sensor.mode == SensorModes.Receive)
		{
			for (VLCsensor item : sensorsTx) 
			{
				if(item.Bearing == sensor.Bearing)
				{
					returnme.add(item);
				}
			}
			/*
			if(sensor.sensorID == 5)
			{
				returnme.add(getSensorByID(1));
				returnme.add(getSensorByID(3));
				
			}
			if(sensor.sensorID == 6)
			{
				returnme.add(getSensorByID(2));
				returnme.add(getSensorByID(4));
			}*/
		}
		else
		{//transmit je
			for (VLCsensor item : sensorsRx) 
			{
				if(item.Bearing == sensor.Bearing)
				{
					returnme.add(item);
				}
			}
			/*
			if(sensor.sensorID == 1 || sensor.sensorID == 3)
			{
				returnme.add(getSensorByID(5));
				
			}
			if(sensor.sensorID == 2 || sensor.sensorID == 4)
			{
				returnme.add(getSensorByID(6));
			}*/
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
	
	public boolean isAtLeastOneIdle(SensorModes mode)
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
	public HashSet<Integer> GetSensors(SensorModes mode)
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
	public void clearControlSignal (int sensorID, byte channelID)
	{
	 	clearControlSignal(getSensorByID(sensorID), channelID);
		
	}
	/**
	 * Clears sensor control signal
	 * @param sensor
	 * @param channelID
	 * @author BorisTomas
	 */
	public void clearControlSignal (VLCsensor sensor, byte channelID)
	{
		if(sensor != null)
		{
			sensor.controlSignal.remove(channelID);
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
		return queryControlSignal(getSensorByID(sensorID), channelID);
	}
	public boolean queryControlSignal (VLCsensor sensor, Integer channelID)
	{
		byte returnValue = 0;
		boolean tmpVal =false;
		
		if(sensor != null)
		{
			if(sensor.mode != SensorModes.Receive)
			{
				throw new RuntimeException("sensor mode is not receiving");
			}
			this.checkLocation(false);//updating location of the node and sensors and bearings etc
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
			GenericDriver.btviz.DrawShape(outlineShape, Color.black);
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

		if(msg instanceof MacMessage.Data)
		{
			//mjerenje vremena.
			((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(250, "radiovlct-rec", null));
		}

		checkLocation(false);

		

		for (VLCsensor item : this.sensorsRx)
		{
			((MacMessage)msg).setSensorIDRx(item.sensorID);
		}//postavio sam sve senzore, u can talk se ce filtrirati samo oni koji mogu vidjeti poruku.



		//u cantalk prosljeðujem id cvora koji je primio poruku.
		//provjeravam jesu li si u piti ili ne.

		if(power_mW < radioInfo.shared.threshold_mW || power_mW < radioInfo.shared.background_mW * thresholdSNR)
		{
			msg = null;
		}
		else
		{
			msg = CanTalk(((MacMessage)msg).getSrc().hashCode(), radioInfo.unique.getID(), SensorModes.Receive, (MacMessage)msg);
		}

		if(msg == null)
		{
			//Constants.VLCconstants.DroppedOnReceive++;
			return;
		}
		//Constants.VLCconstants.Received++;

		for (int item : ((MacMessage)msg).SensorIDRx)
		{

			tmpSensorReceive = getSensorByID(item);
			tmpSensorReceive.CurrentMessage = msg;
			tmpSensorReceive.CurrentMessageEnd = JistAPI.getTime() + duration;
			tmpSensorReceive.signalsRx ++;
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
						//validirati ovo!!! 
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

		if(msg instanceof MacMessage.Data)
		{
			//mjerenje vremena.
			((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(251, "radiovlct-rec", null));
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

	// RadioInterface interface
	/** {@inheritDoc} */
	public void endReceive(final Double powerObj_mW, Long seqNumber)
	{
		//TODO: treba sve poruke na senzoru staviti u listu i onda treba isto staviti i power i treba vidjeti ako je suma (minus onaj koji gledam) u odnosu na onaj koji gledam, taj omjer treba biti ispod zadanog
		//praga. ako je ispod onda znaci da mogu primiti poruku, za ovo bi trebalo napraviti katalog svih transmisija i gledati koje su u isto vrijeme i sl. pa onda racunati taj omjer...
		
		if(mode==Constants.RADIO_MODE_SLEEP) return;

		if(isVLC)
		{
			for (VLCsensor item : sensorsRx)
			{
					if(item.state == SensorStates.Receiving)
					{
						item.signalsRx --;
						if(item.signalsRx != 0 || item.CurrentMessage == null)
						{
							item.CurrentMessage = null;
							this.macEntity.notifyInterference(item);
						}
						else
						{
							if(item.signalsRx == 0 && item.CurrentMessageEnd == JistAPI.getTime())
							{//poruka je dosla i samo je jedna
								 
								((NetMessage.Ip)(((MacMessage.Data)item.CurrentMessage).getBody())).Times.add(new TimeEntry(252, "macbtrec", null));
								this.macEntity.receive(item.CurrentMessage);
								item.setState(SensorStates.Idle );
							}
							else
							{
								//item.signalsRx = 0;
							}
						}
						if(item.signalsRx == 0 && item.CurrentMessage == null)
						{
							//znaci da se dogodila interferencija i da su sve poruke koje su kolidirane dosle.
							clearControlSignal(item, (byte)1);
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
					if(isVLC)
					{
						(this.macEntity).notifyInterference(null);
					}
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

		if(msg instanceof MacMessage.Data)
		{
			//mjerenje vremena
			((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(2, "radiovlct", null));
		}

		// radio in sleep mode
		if(mode == Constants.RADIO_MODE_SLEEP) return;

		///ako poruka nema postavljene senzore postavljam u sve.
		//if sensors are not set, set all of them -> add them to the list of sending sensorids of message.
		if(((MacMessage)msg).getSensorIDTx().size() == 0)
		{
			for (VLCsensor item : this.sensorsTx) 
			{
				((MacMessage)msg).setSensorIDTx(item.sensorID);
			}
		}
		isAtLeastOneTransmitting = false;
		//System.out.println("radio: "+((MacMessage)msg).getSensorIDTx().toString() );
		for (int item : ((MacMessage)msg).getSensorIDTx())
		{
			tmpSensorTransmit= this.getSensorByID(item);
			if(tmpSensorTransmit.mode == SensorModes.Transmit)
			{
				//ok
				if(tmpSensorTransmit.state == SensorStates.Idle )
				{
					//ok
					tmpSensorTransmit.setState(SensorStates.Transmitting );
					tmpSensorTransmit.CurrentMessage = msg;
					tmpSensorTransmit.CurrentMessageEnd = JistAPI.getTime()+duration + delay;
					tmpSensorTransmit.CurrentMessageDuration = duration + delay;
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
		checkLocation(false);
		// clear receive buffer
		//assert(signalBuffer==null);

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
					if (item.CurrentMessageEnd == JistAPI.getTime()) 
					{
						item.state = SensorStates.Idle;
						item.CurrentMessageEnd = 0;
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
		boolean isSomeNodeIntersecting = true;
		if(DestinationID != -1)//znaci da nije broadcast poruka, teoretski nikada nece sourceid biti -1 odnosno broadcast adresa
		{
			tmpSensorRx.clear();
			tmpSensorTx.clear();
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
					if(msg.getSensorIDTx().size() != 0 && msg.getSensorIDRx().size() != 0)
					{
						//u ovom slucaju su poznate stxid i srxid liste.
						for (VLCsensor sensorSrc : Field.getRadioData(SourceID).vlcdevice.sensorsTx)
						{
							if(msg.getSensorIDTx().contains(sensorSrc.sensorID))
							{//ako je mac zadao da se šalje sa odreðenog senzora.
								for (VLCsensor sensorDest : Field.getRadioData(DestinationID).vlcdevice.sensorsRx) {
									{//znaci da listam sve senzore na src i dest koji su zadani u msg sensor listama
										isSomeNodeIntersecting = false;
										for (Integer node : possibleNodes)//listam sve aute koji su mi vidljivi (u trokutu)
										{
											if(intersects(Field.getRadioData(node).vlcdevice.outlineShape, new Line2D.Float(sensorSrc.sensorLocation.getX(), sensorSrc.sensorLocation.getY(), sensorDest.sensorLocation.getX(), sensorDest.sensorLocation.getY())))
											{
												isSomeNodeIntersecting = true;
												break;
											}
										}//foreach possiblenodes
										if(!isSomeNodeIntersecting)
										{
											tmpSensorTx.add(sensorSrc.sensorID);
											tmpSensorRx.add(sensorDest.sensorID);
										}
									}
								}
							}
						}
					//	msg.SensorIDTx.clear();
						msg.setSensorIDTx(tmpSensorTx);
						msg.SensorIDRx.clear();
						msg.SensorIDRx.addAll(tmpSensorRx);
						if(msg.SensorIDTx.size() == 0 || msg.SensorIDRx.size() == 0)
						{
							msg = null;//dropped
						}
					}
					else
					{
						if(msg.SensorIDTx.size() == 0)
						{
							System.out.println("neva2 "+ msg.hashCode() + " cnt = "+msg.SensorIDTx.size());
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
	public boolean visibleToVLCdevice(double x, double y, VLCsensor sensor )		
	{	

		if(Point.distance(x, y, sensor.sensorLocation.getX(), sensor.sensorLocation.getY()) > sensor.distanceLimit)
		{
			return false;
		}
		else
		{
			return sensor.coverageShape.contains(x, y);
		}
		//TODO: testirati radi li pita
		//return visibleToVLCdevice(p1, p2, sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY());
	}

/*	public float tripletOrientation(float x1, float y1, float x2, float y2, float x3, float y3)
	{
		return x1*(y2 - y3) + x2*(y3 - y1) + x3*(y1 - y2);  
	}
*/

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
		sensors1.add(Field.getRadioData(SourceNodeID).vlcdevice.getSensorByID(sensorID));
		for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
		{	

			if(SourceNodeID != i)
			{
				boolean stopSearch = false;
				Field.getRadioData(i).vlcdevice.checkLocation(false);
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
			sourceSensors.add(Field.getRadioData(SourceNodeID).vlcdevice.getSensorByID(sensorID));
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


} // class: RadioVLC
