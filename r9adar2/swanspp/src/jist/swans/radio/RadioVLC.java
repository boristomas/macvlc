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
import java.awt.geom.Point2D;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import javax.management.RuntimeErrorException;
import javax.swing.JEditorPane;

import org.apache.derby.impl.sql.execute.HashScanResultSet;
import org.omg.CORBA.SystemException;

//import com.sun.xml.internal.ws.api.PropertySet.Property;




import jist.runtime.Controller;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.field.Field;
import jist.swans.field.Mobility.StaticInfo;
import jist.swans.mac.MacInterface;
import jist.swans.mac.MacMessage;
import jist.swans.mac.MacVLCMessage;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.MessageBytes;
import jist.swans.misc.Util;
import jist.swans.net.NetMessage;
import jist.swans.radio.VLCelement.ElementModes;
import jist.swans.radio.VLCelement.ElementStates;
import jist.swans.trans.TransUdp;
import jobs.JobConfigurator;
import driver.GenericDriver;
import driver.JistExperiment;
import driver.spatial;

/** 
 * <code>RadioNoiseIndep</code> implements a VLC radio with an independent noise model.
 *
 * @author boris.tomas@gmail.com
 * @version 0.6
 * @since SWANS1.0
 */

public final class RadioVLC extends RadioNoise
{
	public int NodeID;
	private Location newLocation;//used to store new location
	public float nodeInitialBearing = 0F;
	/**
	 * Automatically set control channel to busy on receive.
	 */
	public static boolean AutoSetControlOnRx =false;

	//////////////////////////////////////////////////
	// locals
	//

	/**
	 * threshold signal-to-noise ratio.
	 */
	protected double thresholdSNR;
	private VLCelement tmpelement;
	public LinkedList<VLCelement> InstalledElementsTx = new LinkedList<VLCelement>();
	public LinkedList<VLCelement> InstalledElementsRx = new LinkedList<VLCelement>();

	private float offsetx;
	private float offsety;
	private Location startLocation; //start location set on ctor.
	public Path2D.Double outlineShape; //vehicle outer shell rectangle
	float tmpx1, tmpy1, tmpx2, tmpy2;
	float Ax,Ay, Bx,By,Cx,Cy,Dx,Dy;
	float NodeBearing =0;
	Location NodeLocation;
	public static boolean isVLC = false;

	//802.11
	protected int signalsRx;
	protected int signalsTx;
	protected Message signalBufferRx;
	protected Message signalBufferTx;
	//////////////////////////////////////////////////
	// initialize
	//

	/**
	 * Create new radio with independent noise model.
	 *
	 * @param id radio identifier
	 * @param sharedInfo shared radio properties
	 * @param thresholdSNR threshold signal-to-noise ratio
	 */
	public RadioVLC(int id, RadioInfo.RadioInfoShared sharedInfo, double thresholdSNR, Location location, float staticBearing, float w, float l, float dw, float dl, float ox, float oy)
	{

		super(id, sharedInfo);
		this.NodeID = id;
		setThresholdSNR(thresholdSNR);


		nodeInitialBearing = staticBearing;
		startLocation = location;
		//offsets are half length from center point to edge of vehicle. example vehicle length 
		//is 5m and width is 2m. xoffset is 2.5 and yoffset is 1. 
		offsetx = ox;
		offsety = oy;

		UpdateNodeShape(true);
		if(JistExperiment.getJistExperiment().getMACProtocol().contains("VLC"))
		{
			isVLC = true;
		}//nije bas elegantno :(
		else
		{
			isVLC = false;
		}
		if(isVLC)
		{
			AutoSetControlOnRx = true;
		}
		else
		{
			//802.11 tako i tako ne koristi kontrolne kanale tako da to ne treba stavljati.
			AutoSetControlOnRx = false;
		}
	}

	public float getBearing()
	{
		if( JistExperiment.getJistExperiment().mobility == Constants.MOBILITY_STATIC)
		{
			return nodeInitialBearing;
		}	
		return Field.getRadioData(NodeID).getMobilityInfo().getBearingAsAngle();
	}
	public VLCelement GetElementByID(int id)
	{
		for (VLCelement element: InstalledElementsTx) {

			if(element.elementID == id)
			{
				return element;
			}
		}
		for (VLCelement element: InstalledElementsRx) {

			if(element.elementID == id)
			{
				return element;
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


	/**
	 * sets Control signal for current radio and element
	 * 
	 * @param value
	 * @author BorisTomas
	 */

	public void setControlSignal(int elementID, Integer channelID)
	{
		setControlSignal(GetElementByID(elementID), channelID);
	}
	/**
	 * sets control signal, if set for receiving element this method will find nearest same bearing transmitting sensors and set control signal on them.
	 * @param element
	 * @param channelID
	 */
	public void setControlSignal(VLCelement element, Integer channelID)
	{
		if(element != null)
		{
			if(element.mode != ElementModes.Transmit)
			{
				for (VLCelement item : getNearestOpositeElement(element)) 
				{
					item.controlSignal.add(channelID);
					//CS/		System.out.println("cset: time: "+JistAPI.getTime() +" "+ item.node.NodeID + " - " + item.sensorID+ " c = " + channelID);
				}
			}
			else
			{
				element.controlSignal.add(channelID);
				//CS/		System.out.println("cset: time: "+JistAPI.getTime() +" "+  sensor.node.NodeID + " - " + sensor.sensorID+ " c = " + channelID);
			}
		}
	}

	public LinkedList<VLCelement> getNearestOpositeElement (HashSet<Integer> elements)
	{
		LinkedList<VLCelement> returnme = new LinkedList<VLCelement>();
		for (Integer element : elements) 
		{
			for (VLCelement item : getNearestOpositeElement(GetElementByID(element))) 
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
	 * Gets the nearest element of the opposite
	 * @param element
	 * @param mode
	 * @return
	 */
	public LinkedList<VLCelement> getNearestOpositeElements (VLCelement[] elements)
	{	
		LinkedList<VLCelement> returnme = new LinkedList<VLCelement>();	

		for (VLCelement element : elements) 
		{
			for (VLCelement item : getNearestOpositeElement(element)) 
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
	 * Gets opposite type element list, if parameter is Tx returned will be Rx,
	 * "opposition" is matched by installed bearing matching
	 * @param element
	 * @return
	 */
	public LinkedList<VLCelement> getNearestOpositeElement (VLCelement element)
	{
		LinkedList<VLCelement> returnme = new LinkedList<VLCelement>();	
		LinkedList<VLCelement> source = null;
		if(element.mode == ElementModes.Receive)
		{
			source = InstalledElementsTx;
		}
		else
		{//transmit je
			source = InstalledElementsRx;
		}

		for (VLCelement item : source) 
		{
			if(Math.abs( item.Bearing ) == Math.abs( element.Bearing))
			{
				if(!returnme.contains(item))
				{
					returnme.add(item);
				}
			}
		}

		return returnme;
	}

	public boolean areAllIdle(ElementModes mode)
	{
		switch (mode)
		{
		case Receive:
		{
			for (VLCelement item: InstalledElementsRx) 
			{
				if(item.state != ElementStates.Idle)
				{
					return false;
				}
			}	
			break;
		}
		case Transmit:
		{
			for (VLCelement item: InstalledElementsTx) 
			{
				if(item.state != ElementStates.Idle)
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
		}//switch
		return true;
	}

	public boolean IsAtLeastOneIdle(ElementModes mode)
	{
		switch (mode) {
		case Receive:
		{
			for (VLCelement item: InstalledElementsRx) 
			{
				if(item.state == ElementStates.Idle)
				{
					return true;
				}
			}	
			break;
		}
		case Transmit:
		{
			for (VLCelement item: InstalledElementsTx) 
			{
				if(item.state == ElementStates.Idle)
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
	 * Gets the list of elementIDs.
	 * @param mode
	 * @return
	 * @author BorisTomas
	 */
	public HashSet<Integer> GetElementsIDs(ElementModes mode)
	{
		HashSet<Integer> returnme = new HashSet<Integer>();

		switch (mode) {
		case Receive:
		{
			for (VLCelement item: InstalledElementsRx) 
			{
				returnme.add(item.elementID);
			}	
			break;
		}
		case Transmit:
		{
			for (VLCelement item: InstalledElementsTx) 
			{
				returnme.add(item.elementID);
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
	 * Clears element control signal
	 * @param elementID
	 * @author BorisTomas
	 */
	public void clearControlSignal (int elementID, int channelID)
	{
		clearControlSignal(GetElementByID(elementID), channelID);

	}
	/**
	 * Clears element control signal
	 * @param element
	 * @param channelID
	 * @author BorisTomas
	 */
	public void clearControlSignal (VLCelement element, int channelID)
	{
		if(element != null)
		{
			if(element.mode == ElementModes.Transmit)
			{
				element.controlSignal.remove(channelID);
	//CS/			System.out.println("ccle: time: " + JistAPI.getTime() + " " + element.node.NodeID + " - " + element.elementID + " c = " + channelID);
			}
			else
			{
				for (VLCelement item : getNearestOpositeElement(element)) 
				{
					item.controlSignal.remove(channelID);
					//CS/		System.out.println("ccle: time: " + JistAPI.getTime() + " " + item.node.NodeID + " - " + item.elementID + " c = " + channelID);
				}
			}
		}
	}
	/**
	 * Senses the carrier using single Rx, use id -1 to check all receivers. 
	 * If only one is not idle method returns false.
	 * @param elementID
	 * @return
	 */
	public boolean CarrierSense(int elementID)
	{
		if(elementID != -1)
		{
			tmpelement = GetElementByID(elementID);
			if(tmpelement.mode == ElementModes.Receive)
			{
				if(tmpelement.state == ElementStates.Idle)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			else
			{
				throw new RuntimeException("element mode is not receiving");
			}
		}
		else
		{
			for (VLCelement item : InstalledElementsRx)
			{
				if(item.state != ElementStates.Idle )
				{
					return false;
				}
			}
			return true;
		}
	//	return false;
	}
	
	public boolean CarrierSense(VLCelement element)
	{
			if(element.mode == ElementModes.Receive)
			{
				if(element.state == ElementStates.Idle)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			else
			{
				throw new RuntimeException("element mode is not receiving");
			}
		
	//	return false;
	}
	
	/**
	 * gets element control signal
	 * @param elementID
	 * @return
	 */
	public boolean getControlSignal(int elementID, Integer channelID)
	{
		tmpelement = GetElementByID(elementID);
		if(tmpelement != null)
		{
			return tmpelement.controlSignal.contains(channelID);
		}
		return false;
	}

	/**
	 * Check if there is control signal in the field (visible to node)
	 * @param elementID
	 * @param channelID
	 * @return true if there is signal, else false.
	 */
	public boolean queryControlSignal (int elementID, Integer channelID)
	{
		return queryControlSignal(GetElementByID(elementID), channelID);
	}
	public boolean queryControlSignal (VLCelement element, Integer channelID)
	{
		boolean tmpVal =false;

		if(element != null)
		{
			if(element.mode != ElementModes.Receive)
			{
				throw new RuntimeException("element mode is not receiving");
			}
			this.UpdateNodeShape(false);//updating location of the node and elements and bearings etc
			for (Integer[] node : getRangeAreaNodesAndElements(this.NodeID,ElementModes.Receive, element.elementID)) 
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
			throw new RuntimeException("element not found");
		}
		return false;		
	}


	//public static int nodeidtst = -1;
	Location tmpLoc;
	/****
	 * Checks and updated location of element nodes as well the node itself.
	 * @param isStartCheck use false.
	 */
	public void UpdateNodeShape(Boolean isStartCheck)
	{
		if(!isStartCheck)// .equals(obj)getRadioData(NodeID) != null)
		{
			newLocation =Field.getRadioData(NodeID).getLocation();
			if(nodeInitialBearing != -1)
			{
				NodeBearing = nodeInitialBearing;
			}
			else
			{
				NodeBearing = Field.getRadioData(NodeID).getMobilityInfo().getBearingAsAngle();
			}
		}
		else//startcheck = false
		{
			newLocation = startLocation;
			if(nodeInitialBearing != -1)
			{
				NodeBearing = nodeInitialBearing;
			}
			else
			{
				NodeBearing = 0;
			}
		}
		if(NodeLocation != null)
		{//provjeravam lokaciju jel nova ili ne
			if(NodeLocation.getX() == newLocation.getX())
			{
				if(NodeLocation.getY() == newLocation.getY())
				{
					return;// bearing change not used, it is assumed that vehicle can't rotate in place.
				}
			}
			//ima promjena lokacije
		}
		NodeLocation= newLocation;
		for (VLCelement element : InstalledElementsTx) 
		{
			element.UpdateShape(NodeLocation, NodeBearing);
		}
		for (VLCelement element : InstalledElementsRx) 
		{
			element.UpdateShape(NodeLocation, NodeBearing);
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

		outlineShape = new Path2D.Double();
		outlineShape.moveTo(Ax, Ay);
		outlineShape.lineTo(Bx, By);
		outlineShape.lineTo(Cx, Cy);
		outlineShape.lineTo(Dx, Dy);
		outlineShape.closePath();

		if(isStartCheck)
		{
			if(JistExperiment.getJistExperiment().placement == Constants.PLACEMENT_GRID)
			{
				if(JistExperiment.getJistExperiment().useVisualizer)
				{
					GenericDriver.btviz.DrawShape(outlineShape, Color.black);
					GenericDriver.btviz.DrawString(NodeID+"", Color.BLUE, NodeLocation.getX(),  NodeLocation.getY());
				}
			}
		}
	}
	/*
	 * Rotates the point (ptx, pty) around center location (center) by angle (angleDed) in degrees
	 */
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

	//////////////////////////////////////////////////
	// reception
	//
	// RadioInterface interface
	/** {@inheritDoc} */
	public void receive(Message msg, Double powerObj_mW, Long durationObj)
	{ 
		
		
		if(isVLC)
		{
			((NetMessage.Ip)((MacVLCMessage)msg).getBody()).Times.add(new TimeEntry(250, "radiovlct-rec", null));
		}
		else
		{
			if(msg instanceof MacMessage.Data)
			{
				((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(250, "radiovlct-rec", null));
			}
		}
		if(mode==Constants.RADIO_MODE_SLEEP)
		{
			System.out.println("radio sleeping!!");
			return;
		}

		if(!isVLC && mode == Constants.RADIO_MODE_TRANSMITTING)
		{
			//TODO: 802.11 - teoretski ne moze primati i slati u isto vrijeme?? moze, kaze hsin-mu
			//radio ne moze primat i slati, ako pocne ista primati a traje slanje jednostavno cu discardati poruku.

			//	return;
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
		for (VLCelement item : this.InstalledElementsRx)
		{
			((MacMessage)msg).addElementIDRx(item.elementID, NodeID);
		}//postavio sam sve senzore, u can talk se ce filtrirati samo oni koji mogu vidjeti poruku.

		//u cantalk prosljeðujem id cvora koji je primio poruku.
		//provjeravam jesu li si u piti ili ne.


		if(power_mW < radioInfo.shared.threshold_mW || power_mW < radioInfo.shared.background_mW * thresholdSNR)
		{
			msg = null;
		}
		else
		{
			msg = CheckPhyConditions(((MacMessage)msg).getSrc().hashCode(), NodeID, ElementModes.Receive, (MacMessage)msg);
		}

		if(msg == null || ((MacMessage)msg).isVLCvalid == false)
		{
			for (VLCelement sens : this.InstalledElementsTx)
			{
				if(this.getControlSignal(sens.elementID, 2))
				{
					this.clearControlSignal(sens.elementID, 2);
				}
			}
			return;
		}
		for (VLCelement sens : this.InstalledElementsTx)
		{
			if(this.getControlSignal(sens.elementID, 2))
			{
				this.clearControlSignal(sens.elementID, 2);
				this.setControlSignal(sens.elementID, 1);
			}
		}
		
		if(isVLC)
		{
			//CSM//		System.out.println("rx- " + JistAPI.getTime() + " mid- "+msg.getMessageID() + " n: "+NodeID+ " -> " +((MacVLCMessage)msg).getSrc() + " -> "+((MacVLCMessage)msg).getDst());
			((NetMessage.Ip)((MacVLCMessage)msg).getBody()).Times.add(new TimeEntry(251, "radiovlct-rec", null));
			if( ((NetMessage.Ip)((MacVLCMessage)msg).getBody()).getDst().hashCode() == NodeID)
			{
				((NetMessage.Ip)((MacVLCMessage)msg).getBody()).Times.add(new TimeEntry(70, "radiovlct-rec", null));
			}
		}
		else
		{
			if(msg instanceof MacMessage.Data)
			{
				((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(251, "radiovlct-rec", null));
				if( ((NetMessage.Ip)((MacMessage.Data)msg).getBody()).getDst().hashCode() == NodeID)
				{
					((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(70, "radiovlct-rec", null));
				}
			}
		}

		//Constants.VLCconstants.Received++;

		for (int item : ((MacMessage)msg).getElementIDRx(NodeID))
		{
			tmpElementReceive = GetElementByID(item);
			((MacMessage)msg).setStartRx(tmpElementReceive, JistAPI.getTime());
			((MacMessage)msg).setEndRx(tmpElementReceive, JistAPI.getTime() + duration);
			((MacMessage)msg).setDurationRx(tmpElementReceive,  duration);
			((MacMessage)msg).setPowerRx(tmpElementReceive,  power_mW);
			((MacMessage)msg).setInterferedRx(tmpElementReceive, false);// .InterferedRx = false;
			if(isVLC)
			{
				tmpElementReceive.Messages.addFirst((MacVLCMessage)msg);
			}
			else
			{

			}
			
	//
		
			setControlSignal(tmpElementReceive, 1);
			if(tmpElementReceive.mode == ElementModes.Receive)
			{//ok
				if(tmpElementReceive.state != ElementStates.Idle)
				{
					if(isVLC)
					{
						((MacInterface.VlcMacInterface) this.macEntity).notifyReceiveFail(msg, Constants.MacVlcErrorElementRxIsBusy);
					}
					else
					{//obicni mac
						//TODO: validirati ovo!!! 
						continue;
					}
				}
				else
				{
					tmpElementReceive.setState(ElementStates.Receiving);
					setMode(Constants.RADIO_MODE_RECEIVING);
					//ok je.
				}
			}
			else
			{
				//nikada se ne bi trebalo desiti
				((MacInterface.VlcMacInterface) this.macEntity).notifyReceiveFail(msg, Constants.MacVlcErrorElementIsNotRX);
				return;

			}
		}

		/*	if(msg instanceof MacMessageVLC)
		{
			//mjerenje vremena.
			((NetMessage.Ip)((MacMessageVLC)msg).getBody()).Times.add(new TimeEntry(251, "radiovlct-rec", null));
		}*/

		///mode set
		if(isVLC)
		{
			((MacInterface.VlcMacInterface) this.macEntity).peek(msg);
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
			signalsRx++;
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
			
			
			for (VLCelement item : InstalledElementsRx)
			{
				if(item.state == ElementStates.Receiving)
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
							//CSM//				System.out.println("rxe- " + JistAPI.getTime() + " mid- "+msg1.getMessageID() + " n: "+NodeID+ " -> " +((MacVLCMessage)msg1).getSrc() + " -> "+((MacVLCMessage)msg1).getDst());
							
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
								((NetMessage.Ip)(((MacVLCMessage)msg1).getBody())).Times.add(new TimeEntry(252, "macbtrec", null));
								printMessageTransmissionData(msg1, 0, "r","");

								if(msgcounter == 0)
								{
									//znaci da se dogodila interferencija i da su sve poruke koje su kolidirane dosle.
									clearControlSignal(item, 1);
									clearControlSignal(item, 2);
									item.setState(ElementStates.Idle );
								}
								((MacInterface.VlcMacInterface) this.macEntity).receive(msg1);
								return;
								//continue;
							}
							else
							{
								//nije ok, msg1 je interferirana
								msg1.setInterferedRx(item, true);// .InterferedRx= true;
								((MacInterface.VlcMacInterface) this.macEntity).notifyInterference(msg1, item);
							}
						}
						
					}//for msg1
					if(msgcounter == 0)
					{
						//znaci da se dogodila interferencija i da su sve poruke koje su kolidirane dosle.
						clearControlSignal(item, 1);
						clearControlSignal(item, 2);
						item.setState(ElementStates.Idle );
					}
				}//if receiving
			}//for installed elements
		}
		else
		{//neki obican mac je.

			//TODO: dodao sam da 802.11 moze u isto vrijeme i primati i slati.
			signalsRx--;
			if(mode==Constants.RADIO_MODE_RECEIVING || mode == Constants.RADIO_MODE_TRANSMITTING || mode== Constants.RADIO_MODE_IDLE)
			{
				if(signalBufferRx!=null && JistAPI.getTime()==signalFinish)
				{
					printMessageTransmissionData(signalBufferRx, 0, "r","");
					((MacInterface.Mac802_11)this.macEntity).receive(signalBufferRx);
					unlockSignal();
				}
				else
				{

					//interferencija

					//					System.out.println("interferencija");
				}
				if(signalsRx==0) setMode(Constants.RADIO_MODE_IDLE);
			}
			else
			{
				System.out.println("nevaa3");

			}
		}
	}


	/**
	 * Lock onto current packet signal.
	 *
	 * @param msg packet currently on the air
	 * @param power_mW signal power (units: mW)
	 * @param duration time to EOT (units: simtime)
	 */
	protected void lockSignal(Message msg, double power_mW, long duration)
	{

		seqNumber++;
		signalBufferRx = msg;
		signalPower_mW = power_mW;
		signalFinish = JistAPI.getTime() + duration;
		this.macEntity.peek(msg);
	}

	/**
	 * Unlock from current packet signal.
	 */
	protected void unlockSignal()
	{
		signalBufferRx = null;
		signalPower_mW = 0;
		signalFinish = -1;
	}  

	//bt
	public long getSimulationTime()
	{
		return JistAPI.getTime();
	}

	//////////////////////////////////////////////////
	// transmission
	//

	VLCelement tmpElementTransmit;
	VLCelement tmpElementReceive;
	VLCelement tmpElementReceiveEnd;
	boolean isAtLeastOneTransmitting = false;
	// RadioInterface interface
	/** {@inheritDoc} */
	public void transmit(Message msg, long delay, long duration)
	{
		
		if(isVLC)
		{
//CSM//			System.out.println("tx- " + JistAPI.getTime() + " mid- "+msg.getMessageID() + " n: "+NodeID+ " -> " +((MacVLCMessage)msg).getSrc() + " -> "+((MacVLCMessage)msg).getDst());
			((NetMessage.Ip)((MacVLCMessage)msg).getBody()).Times.add(new TimeEntry(2, "radiovlct", null));
		}
		else
		{
			if(msg instanceof MacMessage.Data)
			{
				((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(2, "radiovlct", null));
			}

			// radio in sleep mode
			if(mode == Constants.RADIO_MODE_SLEEP) return;

		}

		///ako poruka nema postavljene senzore postavljam u sve.
		//if elements are not set, set all of them -> add them to the list of sending elementids of message.
		if(((MacMessage)msg).getElementIDTx(NodeID).size() == 0)
		{
			for (VLCelement item : this.InstalledElementsTx) 
			{
				((MacMessage)msg).addElementIDTx(item.elementID, NodeID);
			}
		}
		if(isVLC)
		{
			
			isAtLeastOneTransmitting = false;
			for (int item : ((MacMessage)msg).getElementIDTx(NodeID))
			{
				tmpElementTransmit= this.GetElementByID(item);
				for (VLCelement ss : getNearestOpositeElement(tmpElementTransmit)) 
				{
					if( queryControlSignal(ss, 1))
					{
		//				System.out.println("tu2");
						TimeEntry.AllMessages.remove(msg);
						
						return;
					}
					
				}
				
				if(tmpElementTransmit.mode == ElementModes.Transmit)
				{
					//ok
				//	if(tmpSensorTransmit.state == SensorStates.Transmitting)
					if(tmpElementTransmit.state  == ElementStates.Idle )//na MAC sloju stavljam vec u transmitting
					{
						//ok
					

						isAtLeastOneTransmitting = true;
					}
					else if(tmpElementTransmit.state == ElementStates.Transmitting)
					{
						if(isVLC)
						{
							((MacInterface.VlcMacInterface) this.macEntity).notifyTransmitFail(msg, Constants.MacVlcErrorElementTxIsBusy);
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
						((MacInterface.VlcMacInterface) this.macEntity).notifyTransmitFail(msg, Constants.MacVlcErrorElementIsNotTX);
					}
					//isto se ne bi smjelo desiti
					return;
				}
			}// for all tx sensor defined in message.
			if(!isAtLeastOneTransmitting)
			{
				((MacInterface.VlcMacInterface) this.macEntity).notifyTransmitFail(msg, Constants.MacVlcErrorElementTxAllBusy);	
			}
			
		//	CheckPhyConditions( NodeID, -2, SensorModes.Receive, (MacMessage)msg);
				
			/*if(res == null || ((MacMessage)res).isVLCvalid == false)
			{
				return;
			}
			else
			{													//ne ide NodeID nego id od receving node.
				for (int item : ((MacMessage)res).getSensorIDRx(NodeID))//.SensorIDRx)
				{
						tmpSensorReceive = GetSensorByID(item);
						setControlSignal(tmpSensorReceive, 1);
				}
			}*/
		}
		else
		{// obicni neki mac je.
			/*if(!isAtLeastOneTransmitting)
			{

				//nema tko poslati poruku jer su svi zauzeti.
				return;
			}*/
			if(mode == Constants.RADIO_MODE_TRANSMITTING) 
				{
					return;
					//throw new RuntimeException("radio already transmitting");
				}
			if(mode == Constants.RADIO_MODE_RECEIVING) 
			{
				//System.out.println("radio receiving, can't transmit at the same time");
				//		return;
				//dodao jer mislim da radio ne bi trebao slati poruku ako ju vec prima, ali samo za ne vlc mac
			}
			signalBufferTx = null;
		}
		UpdateNodeShape(false);
		
		// use default delay, if necessary
		if(delay==Constants.RADIO_NOUSER_DELAY)
			{
			delay = Constants.RADIO_PHY_DELAY;
			}
		// set mode to transmitting
		setMode(Constants.RADIO_MODE_TRANSMITTING);

		if(isVLC)
		{
			((NetMessage.Ip)((MacVLCMessage)msg).getBody()).Times.add(new TimeEntry(21, "radiovlct", null));
		}
		else
		{
			if(msg instanceof MacMessage.Data)
			{
				((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(21, "radiovlct", null));
			}
		}
		// schedule message propagation delay
		JistAPI.sleep(delay);
		printMessageTransmissionData(msg,duration, "t","");
		fieldEntity.transmit(radioInfo, msg, duration);
		// schedule end of transmission
		JistAPI.sleep(duration);
		self.endTransmit();
	}
	public void printMessageTransmissionData(Message msg, long duration, String prefix, String extraData)
	{
		if(JobConfigurator.DoMessageOutput)
		{
			String aa="";
			for (int item : ((MacMessage)msg).getElementIDTx(NodeID))
			{
				aa += item + " ";
			}
			String bb="";
			for (int item : ((MacMessage)msg).getElementIDRx(NodeID))
			{
				bb += item + " ";
			}
			if(isVLC)
			{
				System.out.println(prefix + " - n: "+NodeID+ "\tex:" + extraData + "\tt: "+JistAPI.getTime()+"\ts: "+((MacVLCMessage)msg).getSrc()+ "("+aa+") \t\td: "+((MacVLCMessage)msg).getDst() +"("+bb+") end: "+(duration+getSimulationTime()) + "\tmid: " + msg.getMessageID() + "\tdecoded: "+tryDecodePayload(msg) );
			}
			else
			{
				System.out.println(prefix + " - n: "+NodeID+ "\tex: "+ extraData + "\tt:" +JistAPI.getTime()+"\ts: "+((MacMessage)msg).getSrc()+ "("+aa+") \t\td: "+((MacMessage)msg).getDst() +"("+bb+") end: "+(duration+getSimulationTime()) + "\tmid: " + msg.getMessageID() + "\tdecoded: "+tryDecodePayload(msg));
			}
		}
	}
	  String tryDecodePayload(Message msg)
	{
		String decoded = "empty";
		try {
			if(isVLC)
			{
				decoded = new String(
						((MessageBytes)((TransUdp.UdpMessage)
								(((NetMessage.Ip)((MacVLCMessage)msg).getBody()).getPayload())
								).getPayload()).getBytes()
								, "UTF-8");
			}
			else
			{
				if(msg instanceof MacMessage.Data)
				{
					decoded = new String(
							((MessageBytes)((TransUdp.UdpMessage)
									(((NetMessage.Ip)((MacMessage.Data)msg).getBody()).getPayload())
									).getPayload()).getBytes()
									, "UTF-8");
				}
			}

		} catch (Exception e) 
		{
		//		e.printStackTrace();
		}
		return decoded.replace(' ', '\0').trim();
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
			for (VLCelement item : InstalledElementsTx)
			{
				if(item.state == ElementStates.Transmitting )
				{
					if (item.Messages.getFirst().getEndTx(item) == JistAPI.getTime()) 
					{
						MacMessage msg = item.Messages.getFirst();
						//CSM//			System.out.println("txe- " + JistAPI.getTime() + " mid- "+msg.getMessageID() + " n: "+NodeID+ " -> " +((MacVLCMessage)msg).getSrc() + " -> "+((MacVLCMessage)msg).getDst());
						item.state = ElementStates.Idle;
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
			//TODO: 802.11 fix da moze i primati i slati u isto vrijeme
			if(mode!=Constants.RADIO_MODE_TRANSMITTING && mode != Constants.RADIO_MODE_RECEIVING && mode != Constants.RADIO_MODE_IDLE) 
			{
				throw new RuntimeException("radio is not transmitting");
			}
			// set mode
			setMode(signalsTx>0 ? Constants.RADIO_MODE_RECEIVING : Constants.RADIO_MODE_IDLE);
		}
	}

	//bt
	private HashSet<Integer> NodesThatSourceCanSee;
	private HashSet<Integer> NodesThatDestinationCanSee;
	private HashSet<Integer> tmpElementRx;

	/**
	 * Checks if two nodes can talk, more specific if elements can talk according to address in msg.
	 * @param SourceID macaddr of source.
	 * @param DestinationID macaddr of destination.
	 * @param mode element mode, if sent or received.
	 * @param msg message to be sent.
	 * @return
	 */
	private MacMessage CheckPhyConditions(int SourceID, int DestinationID, ElementModes mode, MacMessage msg)
	{
		//System.out.println("hshs : "+ msg.hashCode() );
		//	NodesThatDestinationCanSee.clear();
		//	NodesThatSourceCanSee.clear();

		tmpElementRx = new HashSet<Integer>();
		msg.isVLCvalid= true;
		if(DestinationID != -1 && DestinationID != -2)//znaci da nije broadcast poruka, teoretski nikada nece sourceid biti -1 odnosno broadcast adresa
		{
			if(mode == ElementModes.Transmit)
			{//znaci sourceid šalje
				//never happens!

				NodesThatSourceCanSee.addAll(getRangeAreaNodes(SourceID, mode,-1));//send mode
				NodesThatDestinationCanSee = getRangeAreaNodes(DestinationID, ElementModes.Receive,-1);

				if(NodesThatSourceCanSee.contains(DestinationID) && NodesThatDestinationCanSee.contains(SourceID))
				{
				}
				else
				{
					//nisu si ni u trokutu
					msg.isVLCvalid= false;
				}
			}
			else if( mode == ElementModes.Receive)
			{//znaci source sluša poruke
				NodesThatSourceCanSee = getRangeAreaNodes(SourceID, ElementModes.Transmit,-1);//send mode
				NodesThatDestinationCanSee = getRangeAreaNodes(DestinationID, ElementModes.Receive,-1);

				if(NodesThatSourceCanSee.contains(DestinationID) && NodesThatDestinationCanSee.contains(SourceID))
				{
					if(msg.getElementIDRx(DestinationID).size() != 0)
					{
						//u ovom slucaju su poznate stxid i srxid liste.
						boolean isVisible = false;
						for (VLCelement elementSrc : Field.getRadioData(SourceID).vlcdevice.InstalledElementsTx)
						{
							if(msg.getElementIDTx(SourceID).contains(elementSrc.elementID))
							{//ako je mac zadao da se šalje sa odreðenog senzora.
								for (VLCelement elementDest : Field.getRadioData(DestinationID).vlcdevice.InstalledElementsRx)
								{
									//znaci da listam sve senzore na src i dest koji su zadani u msg sensor listama
									if (IsElementVisibleToElement(elementSrc, elementDest) && IsElementVisibleToElement(elementDest, elementSrc) ) 
									{
							
										isVisible = true;
										tmpElementRx.add(elementDest.elementID);
									}
								}
							}
						}
						if(false && !isVisible)//Asymmetry evaluation is commented because it is no longer relevant, it is left here for future reference.
						{
							//nije visible onda je problem asimetrije u dizajnu
							if(isVLC)
							{
								((NetMessage.Ip)((MacVLCMessage)msg).getBody()).Times.add(new TimeEntry(84, "drop-asym4", null));
							}
							else
							{
								if(msg instanceof MacMessage.Data)
								{
									((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(84, "drop-asym4", null));
								}
							}
						}
						msg.setElementIDRx(tmpElementRx, DestinationID);

						if(msg.getElementIDRxSize(DestinationID) == 0)
						{
							msg.isVLCvalid =false;// = null;//dropped
						}
					}
					else
					{
						if(msg.getElementIDTxSize(SourceID) == 0)
						{
							System.out.println("neva2 "+ msg.hashCode() + " cnt = "+msg.getElementIDTxSize(SourceID));
						}
						else
						{
							System.out.println("neva4 "+ msg.hashCode());
						}
					}
				}
				else
				{
					//nisu si ni u trokutu , nema LOS.

					msg.isVLCvalid = false;
					if(false)//Asymmetry evaluation is commented because it is no longer relevant, it is left here for future reference.
					{
						if( (NodesThatSourceCanSee.contains(DestinationID) && !NodesThatDestinationCanSee.contains(SourceID) )
								|| (!NodesThatSourceCanSee.contains(DestinationID) && NodesThatDestinationCanSee.contains(SourceID)) )
						{
							//position
							if(isVLC)
							{
								((NetMessage.Ip)((MacVLCMessage)msg).getBody()).Times.add(new TimeEntry(82, "drop-asym2", null));
							}
							else
							{
								if(msg instanceof MacMessage.Data)
								{
									((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(82, "drop-asym2", null));
								}
							}
						}
						else
						{						
							if(isVLC)
							{
								((NetMessage.Ip)((MacVLCMessage)msg).getBody()).Times.add(new TimeEntry(81, "drop-asym1", null));
							}
							else
							{
								if(msg instanceof MacMessage.Data)
								{
									((NetMessage.Ip)((MacMessage.Data)msg).getBody()).Times.add(new TimeEntry(81, "drop-asym1", null));
								}
							}
						}
					}
				}
			}
		
			else
			{
				//should never happen, use send or receive
				msg.isVLCvalid = false;
			}
		}
		else if (DestinationID == -2)
		{
			//mode u kojem svim receiving cvorovima stavim control signal.
			NodesThatSourceCanSee = getRangeAreaNodes(SourceID, ElementModes.Transmit, -1);//send mode

			for (Integer visibleNode : NodesThatSourceCanSee) 
			{
				for (VLCelement elementSrc : Field.getRadioData(SourceID).vlcdevice.InstalledElementsTx)
				{
					if(msg.getElementIDTx(SourceID).contains(elementSrc.elementID))
					{//ako je mac zadao da se šalje sa odreðenog senzora.
						for (VLCelement elementDest : Field.getRadioData(visibleNode).vlcdevice.InstalledElementsRx)
						{
							if (IsElementVisibleToElement(elementSrc, elementDest) && IsElementVisibleToElement(elementDest, elementSrc) ) 
							{
								//			tmpSensorTx.add(sensorSrc.sensorID);
								Field.getRadioData(visibleNode).vlcdevice.setControlSignal(elementDest, 2);
							}
						}
					
					}
					
				}
			}
		}
		else//broadcast poruka je
		{
			//nikada se nece dogoditi jer bcast filteriram na transmit metodi
			msg.isVLCvalid = false;
		}
	/*	if(JobConfigurator.DoRandomDrops)
		{
		//	if(DestinationID != -1)
			{
				//0-1
				if(rnd.nextDouble() <= JobConfigurator.RandomDropRate)
				{
					msg.isVLCvalid = false;
				}
			}
		}*/
		return msg;
	}
//	Random rnd = new Random();
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
	 * @param element1
	 * @param element2
	 * @return
	 */
	public boolean IsElementVisibleToElement(VLCelement element1, VLCelement element2 )
	{
		if(Point.distance(element1.elementLocation.getX(), element1.elementLocation.getY(), element2.elementLocation.getX(), element2.elementLocation.getY()) > Math.min(element1.distanceLimit, element2.distanceLimit))
		{
			return false;
		}
		else
		{
			if(!element1.coverageShape.contains(element2.elementLocation.getX(), element2.elementLocation.getY()))
			{
				return false;
			}
			
			//provjeravam LOS
			for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
			{
				if(intersects(Field.getRadioData(i).vlcdevice.outlineShape, new Line2D.Double(element1.elementLocation.getX(), element1.elementLocation.getY(), element2.elementLocation.getX(), element2.elementLocation.getY())))
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
	 * @param elementID use elementID = -1 to check all elements. setting to specific element will check only for that element.
	 * @return
	 */
	private HashSet<Integer[]> getRangeAreaNodesAndElements(int SourceNodeID, ElementModes mode, int elementID)
	{
		//ne moram sve senzore gledati od send cvora nego samo one koji su u listi, tako se barem malo smanji optereecnje, 
		//teorettski dva
		//puta provjeravam odnose dva senzor cvora!!!!!
		HashSet<Integer[]> returnNodes = new HashSet<Integer[]>();
		LinkedList<VLCelement> elements1 = new LinkedList<VLCelement>();
		LinkedList<VLCelement> elements2 = new LinkedList<VLCelement>();
		elements1.add(Field.getRadioData(SourceNodeID).vlcdevice.GetElementByID(elementID));
		for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
		{	

			if(SourceNodeID != i)
			{
				Field.getRadioData(i).vlcdevice.UpdateNodeShape(false);
				if(mode == ElementModes.Transmit)
				{
					elements2 = Field.getRadioData(i).vlcdevice.InstalledElementsRx;
				}
				else //receive
				{
					elements2 = Field.getRadioData(i).vlcdevice.InstalledElementsTx;
				}


				for (VLCelement element :elements1)
				{
					//	if(stopSearch)
					//	break;
					for (VLCelement element2 :elements2)
					{
						if(IsElementVisibleToElement(element2,element))// sensor.sensorLocation.getX(), sensor.sensorLocation.getY(), sensor.sensorLocation1.getX(), sensor.sensorLocation1.getY(), sensor.sensorLocation2.getX(), sensor.sensorLocation2.getY()))
						{
							//	stopSearch = true;
							Integer[] a =new Integer[]{i,element2.elementID};
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
	 * @param elementID use sensorID = -1 to check all sensors. setting this parameter to a specific sensor will check only for that sensor.
	 * @param mode
	 * @return
	 */
	private HashSet<Integer> getRangeAreaNodes(int SourceNodeID, ElementModes mode, int elementID)
	{

		HashSet<Integer> returnNodes = new HashSet<Integer>();
		LinkedList<VLCelement> sourceElements = null;
		LinkedList<VLCelement> destinationElements =null;

		if(mode == ElementModes.Transmit)
		{
			if(elementID == -1)
			{
				sourceElements = Field.getRadioData(SourceNodeID).vlcdevice.InstalledElementsTx;
			}
		}
		else //receive
		{
			if(elementID == -1)
			{
				sourceElements = Field.getRadioData(SourceNodeID).vlcdevice.InstalledElementsRx;
			}
		}
		if(elementID != -1)
		{
			sourceElements = new LinkedList<VLCelement>();
			sourceElements.add(Field.getRadioData(SourceNodeID).vlcdevice.GetElementByID(elementID));
		}

		boolean stopSearch = false;
		for(int i=1;i<=JistExperiment.getJistExperiment().getNodes(); i++) 
		{	
			if(SourceNodeID != i)
			{
				stopSearch = false;
				if(mode == ElementModes.Transmit)
				{
					destinationElements = Field.getRadioData(i).vlcdevice.InstalledElementsRx;
				}
				else //receive
				{
					destinationElements = Field.getRadioData(i).vlcdevice.InstalledElementsTx;
				}
				for (VLCelement element :sourceElements)
				{
					if(stopSearch)
						break;
					for (VLCelement element2 :destinationElements)
					{
						if(IsElementVisibleToElement(element,element2))
						{
							stopSearch = true;
							returnNodes.add(i);
							break;
						}
					}
				}//for my elements
			}//if not me
		}//for all nodes
		return returnNodes;
	}

	int angcnt= 0;
	double angsum = 0;
	public double GetAngleRx(Location srcLocation) 
	{
		angcnt =0;
		angsum = 0;
		for (VLCelement item : InstalledElementsRx) 
		{
			if(item.coverageShape.contains(new Point2D.Float(srcLocation.getX(), srcLocation.getY()) ))
			{
				angcnt ++;
				angsum += item.visionAngle;
			}
		}
		if(angcnt == 0) return 0;
		return (angsum/angcnt);
	}
	public double GetAngleTx(Location srcLocation) 
	{
		angcnt =0;
		angsum = 0;
		for (VLCelement item : InstalledElementsTx) 
		{
			if(item.coverageShape.contains(new Point2D.Float(srcLocation.getX(), srcLocation.getY()) ))
			{
				angcnt ++;
				angsum += item.visionAngle;
			}
		}
		if(angcnt == 0) return 0;
		return (angsum/angcnt);
	}


} // class: RadioVLC
