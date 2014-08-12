//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <MacMessage.java Sun 2005/03/13 11:06:45 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.puppycrawl.tools.checkstyle.api.Utils;

import driver.JistExperiment;
import jist.runtime.JistAPI;
import jist.runtime.Util;
import jist.swans.Constants;
import jist.swans.misc.Message;
import jist.swans.radio.VLCsensor;

/**
 * Defines the various message used by the Mac entity.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MacMessage.java,v 1.1 2007/04/09 18:49:45 drchoffnes Exp $
 * @since SWANS1.0
 */

public  class MacMessageVLC extends MacMessage
{
	//sensor control
	private java.util.concurrent.ConcurrentHashMap<Integer, HashSet<Integer>> SensorIDTx = new java.util.concurrent.ConcurrentHashMap<Integer, HashSet<Integer>>();
	//TODO: ove hashmape su mogle bili i liste VLCsensor objekata??
	private java.util.concurrent.ConcurrentHashMap<Integer, HashSet<Integer>> SensorIDRx = new java.util.concurrent.ConcurrentHashMap<Integer, HashSet<Integer>>();
	//public HashSet<Integer> SensorIDRx = new HashSet<Integer>();
	
	private HashMap<VLCsensor, Long> StartRx = new HashMap<VLCsensor, Long>();
	public long getStartRx(VLCsensor sensor)
	{
		if(!StartRx.containsKey(sensor))
		{
			StartRx.put(sensor, (long)0);
		}
		return StartRx.get(sensor);
	}
	public void setStartRx(VLCsensor sensor, long value)
	{
		StartRx.put(sensor, value);
	}
	
	private HashMap<VLCsensor, Long> EndRx = new HashMap<VLCsensor, Long>();
	public long getEndRx(VLCsensor sensor)
	{
		if(!EndRx.containsKey(sensor))
		{
			EndRx.put(sensor, (long)0);
		}
		return EndRx.get(sensor);
	}
	public void setEndRx(VLCsensor sensor, long value)
	{
		EndRx.put(sensor, value);
	}
	private HashMap<VLCsensor, Long> DurationRx = new HashMap<VLCsensor, Long>();
	public long getDurationRx(VLCsensor sensor)
	{
		if(!DurationRx.containsKey(sensor))
		{
			DurationRx.put(sensor, (long)0);
		}
		return DurationRx.get(sensor);
	}
	public void setDurationRx(VLCsensor sensor, long value)
	{
		DurationRx.put(sensor, value);
	}
	
	private HashMap<VLCsensor, Double> PowerRx = new HashMap<VLCsensor, Double>();
	public double getPowerRx(VLCsensor sensor)
	{
		if(!PowerRx.containsKey(sensor))
		{
			PowerRx.put(sensor, (double)0);
		}
		return PowerRx.get(sensor);
	}
	public void setPowerRx(VLCsensor sensor, double value)
	{
		PowerRx.put(sensor, value);
	}
	
	private HashMap<VLCsensor, Boolean> InterferedRx = new HashMap<VLCsensor, Boolean>();
	public boolean getInterferedRx(VLCsensor sensor)
	{
		if(!InterferedRx.containsKey(sensor))
		{
			InterferedRx.put(sensor, false);
		}
		return InterferedRx.get(sensor);
	}
	public void setInterferedRx(VLCsensor sensor, boolean value)
	{
		InterferedRx.put(sensor, value);
	}
	
	private HashMap<VLCsensor, Long> StartTx = new HashMap<VLCsensor, Long>();
	public long getStartTx(VLCsensor sensor)
	{
		if(!StartTx.containsKey(sensor))
		{
			StartTx.put(sensor, (long)0);
		}
		return StartTx.get(sensor);
	}
	public void setStartTx(VLCsensor sensor, long value)
	{
		StartTx.put(sensor, value);
	}
	
	private HashMap<VLCsensor, Long> EndTx = new HashMap<VLCsensor, Long>();
	public long getEndTx(VLCsensor sensor)
	{
		if(!EndTx.containsKey(sensor))
		{
			EndTx.put(sensor, (long)0);
		}
		return EndTx.get(sensor);
	}
	public void setEndTx(VLCsensor sensor, long value)
	{
		EndTx.put(sensor, value);
	}
	
	private HashMap<VLCsensor, Long> DurationTx = new HashMap<VLCsensor, Long>();
	public long getDurationTx(VLCsensor sensor)
	{
		if(!DurationTx.containsKey(sensor))
		{
			DurationTx.put(sensor, (long)0);
		}
		return DurationTx.get(sensor);
	}
	public void setDurationTx(VLCsensor sensor, long value)
	{
		DurationTx.put(sensor, value);
	}
	private HashMap<VLCsensor, Double> PowerTx = new HashMap<VLCsensor,Double>();
	public double getPowerTx(VLCsensor sensor)
	{
		if(!PowerTx.containsKey(sensor))
		{
			PowerTx.put(sensor, (double)0);
		}
		return PowerTx.get(sensor);
	}
	public void setPowerTx(VLCsensor sensor, double value)
	{
		PowerTx.put(sensor, value);
	}
	
	private HashMap<VLCsensor, Boolean> InterferedTx = new HashMap<VLCsensor, Boolean>();
	public boolean getInterferedTx(VLCsensor sensor)
	{
		if(!InterferedTx.containsKey(sensor))
		{
			InterferedTx.put(sensor, false);
		}
		return InterferedTx.get(sensor);
	}
	public void setInterferedTx(VLCsensor sensor, boolean value)
	{
		InterferedTx.put(sensor, value);
	}
	
	/*public long StartRx1;
	public long EndRx1;
	public long DurationRx1;
	public double PowerRx1;
	public boolean InterferedRx1;
	public long StartTx1;
	public long EndTx1;
	public long DurationTx1;
	public double PowerTx1;
	public boolean InterferedTx1;*/
	
	
	
	
	
	
	public int getSensorIDTxSize(int nodeID)
	{
		return getSensorIDTx(nodeID).size();
	}
	public int getSensorIDRxSize(int nodeID)
	{
		return getSensorIDRx(nodeID).size();
	}
	public void setSensorIDTx(HashSet<Integer> value, Integer nodeID)
	{
		SensorIDTx.put(nodeID, value);
	}
	public void setSensorIDRx(HashSet<Integer> value, Integer nodeID)
	{
		/*for (Integer sensor : value)
		{
			System.out.println("s2: "+nodeID+ " " + sensor + " hsh: " + hashCode());
		}*/
		this.SensorIDRx.put(nodeID, value);
	}	
	public void setSensorIDTx(LinkedList<VLCsensor> value, Integer nodeID)
	{
		HashSet<Integer> hs = new HashSet<Integer>();
		for (VLCsensor sensor : value)
		{
			hs.add(sensor.sensorID);
		}
		this.SensorIDTx.put(nodeID, hs);
	}
	public void setSensorIDRx(LinkedList<VLCsensor> value, Integer nodeID)
	{
		HashSet<Integer> hs = new HashSet<Integer>();
		for (VLCsensor sensor : value)
		{
			hs.add(sensor.sensorID);
	//		System.out.println("set1: "+sensor.sensorID+ " " + nodeID + " hsh: " + hashCode());
		}
		
		this.SensorIDRx.put(nodeID, hs);
	}
	/**
	 * Gets sensor ID this message is assigned to for Tx
	 * @return
	 */
	public HashSet<Integer> getSensorIDTx(Integer nodeID)
	{
		if(!SensorIDTx.containsKey(nodeID))
		{
			SensorIDTx.put(nodeID, new HashSet<Integer>());
		}
		return SensorIDTx.get(nodeID);
	}
	/**
	 * Gets sensor ID this message is assigned to for Rx
	 * @return
	 */
	public HashSet<Integer> getSensorIDRx(Integer nodeID)
	{
		if(!SensorIDRx.containsKey(nodeID))
		{
			SensorIDRx.put(nodeID, new HashSet<Integer>());
		}
		return SensorIDRx.get(nodeID);
	}
	/**
	 * Add Tx sensor ID for this message
	 * @return
	 */
	public void addSensorIDTx(int sensorID, int nodeID)
	{
		getSensorIDTx(nodeID).add(sensorID);
	}
	
	public void removeSensorIDTx(int sensorID, int nodeID)
	{
		getSensorIDTx(nodeID).remove(sensorID);
	}
	/**
	 * Add Rx sensor ID for this message
	 * @return
	 */
	public void addSensorIDRx(int sensorID, int nodeID)
	{
	//	System.out.println("add: "+sensorID+ " " + nodeID + " hsh: " + hashCode());
		getSensorIDRx(nodeID).add(sensorID);
	}
	
	public void removeSensorIDRx(int sensorID, int nodeID)
	{
		getSensorIDRx(nodeID).remove(sensorID);
	}
	//////////////////////////////////////////////////
	// frame control
	//

	/** RTS packet constant: type = 01, subtype = 1011. */
	public static final byte TYPE_RTS  = 27;

	/** CTS packet constant: type = 01, subtype = 1100. */
	public static final byte TYPE_CTS  = 28;

	/** ACK packet constant: type = 01, subtype = 1101. */
	public static final byte TYPE_ACK  = 29;

	/** DATA packet constant: type = 10, subtype = 0000. */
	public static final byte TYPE_DATA = 32;

	/**
	 * packet type.
	 */
	private byte type;

	/**
	 * packet retry bit.
	 */
	private boolean retry;

	//////////////////////////////////////////////////
	// initialization
	//

	/**
	 * Create a mac packet.
	 * 
	 * @param type packet type
	 * @param retry packet retry bit
	 */
	/*protected MacMessageVLC(byte type, boolean retry)
	{
		//this.TimeCreated = JistAPI.getTime();
		this.type = type;
		this.retry = retry;
		//this.setSensorIDRx(-1);
		//this.setSensorIDTx(-1);
	}*/
	
	//////////////////////////////////////////////////
	// initialization
	//

	/**
	 * Create 802_11 data packet.
	 *
	 * @param dst packet destination address
	 * @param src packet source address
	 * @param duration packet transmission duration
	 * @param seq packet sequence number
	 * @param frag packet fragment number
	 * @param moreFrag packet moreFrag flag
	 * @param retry packet retry bit
	 * @param body packet data payload
	 */
	public MacMessageVLC(MacAddress dst, MacAddress src, int duration, short seq, short frag, 
			boolean moreFrag, boolean retry, Message body)
	{
		super(TYPE_DATA, retry);
		this.dst = dst;
		this.src = src;
		this.duration = duration;
		this.seq = seq;
		this.frag = frag;
		this.body = body;
	}

	/**
	 * Create 802_11 data packet.
	 *
	 * @param dst packet destination address
	 * @param src packet source address
	 * @param duration packet transmission duration
	 * @param body packet data payload
	 */
	public MacMessageVLC(MacAddress dst, MacAddress src, int duration, Message body)
	{
		this(dst, src, duration, (short)-1, (short)-1, false, false, body);
	}

	//////////////////////////////////////////////////
	// accessors
	//

	/**
	 * Return packet type.
	 *
	 * @return packet type
	 */
	public byte getType()
	{
		return type;
	}

	/**
	 * Return retry bit.
	 *
	 * @return retry bit
	 */
	public boolean getRetry()
	{
		return retry;
	}

	
	/**
	 * Packet header size.
	 */
	public static final short HEADER_SIZE = 34;

	/**
	 * Packet sequence number limit.
	 */
	public static final short MAX_SEQ = 4096;

	/**
	 * Packet destination address.
	 */
	private MacAddress dst;

	/**
	 * Packet source address.
	 */
	private MacAddress src;

//	private MacAddress nextHop;
	/**
	 * get next hop address
	 * @return
	 * @author BorisTomas
	 */
/*		public MacAddress getNextHop()
	{
		return nextHop;
	}*/
	/**
	 * set next hop address
	 * @param value
	 * @author BorisTomas
	 */
/*	public void setNextHop(MacAddress value)
	{
		nextHop = value;
	}*/
	/**
	 * Packet transmission duration.
	 */
	private int duration;

	/**
	 * Packet sequence number.
	 */
	private short seq;

	/**
	 * Packet fragment number.
	 */
	private short frag;

	/**
	 * Packet moreFlag bit.
	 */
	private boolean moreFrag;

	/**
	 * Packet data payload.
	 */
	private Message body;



	//////////////////////////////////////////////////
	// accessors
	//

	/**
	 * Return packet destination address.
	 *
	 * @return packet destination address
	 */
	public MacAddress getDst()
	{
		return dst;
	}

	/**
	 * Return packet source address.
	 *
	 * @return packet source address
	 */
	public MacAddress getSrc()
	{
		return src;
	}

	/**
	 * Return packet transmission time.
	 *
	 * @return packet transmission time
	 */
	public int getDuration()
	{
		return duration;
	}

	/**
	 * Return packet sequence number.
	 *
	 * @return packet sequence number
	 */
	public short getSeq()
	{
		return seq;
	}

	/**
	 * Return packet fragment number.
	 *
	 * @return packet fragment number
	 */
	public short getFrag()
	{
		return frag;
	}

	/**
	 * Return packet data payload.
	 *
	 * @return packet data payload
	 */
	public Message getBody()
	{
		return body;
	}

	//////////////////////////////////////////////////
	// message interface 
	//

	// Message interface
	/** {@inheritDoc} */
	public int getSize()
	{
		int size = body.getSize();
		if(size==Constants.ZERO_WIRE_SIZE)
		{
			return Constants.ZERO_WIRE_SIZE;
		}
		return HEADER_SIZE+size;
	}

	// Message interface
	/** {@inheritDoc} */
	public void getBytes(byte[] msg, int offset)
	{
		throw new RuntimeException("todo: not implemented");
	}	
	
	//////////////////////////////////////////////////
	// RTS frame: (size = 20)
	//   frame control          size: 2
	//   duration               size: 2
	//   address: destination   size: 6
	//   address: source        size: 6
	//   CRC                    size: 4
	//

	/**
	 * An 802_11 Request-To-Send packet.
	 *
	 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
	 * @since SWANS1.0
	 */

	//////////////////////////////////////////////////
	// CTS frame: (size = 14)
	//   frame control          size: 2
	//   duration               size: 2
	//   address: destination   size: 6
	//   CRC                    size: 4

	/**
	 * An 802_11 Clear-To-Send packet.
	 *
	 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
	 * @since SWANS1.0
	 */

	//////////////////////////////////////////////////
	// ACK frame: (size = 14)
	//   frame control          size: 2
	//   duration               size: 2
	//   address: destination   size: 6
	//   CRC                    size: 4

	/**
	 * An 802_11 Acknowlege packet.
	 *
	 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
	 * @since SWANS1.0
	 */




	//////////////////////////////////////////////////
	// DATA frame: (size = 34 + body)
	//   frame control          size: 2
	//   duration / ID          size: 2
	//   address: destination   size: 6
	//   address: source        size: 6
	//   address: #3            size: 6
	//   sequence control       size: 2
	//   address: #4            size: 6
	//   frame body             size: 0 - 2312
	//   CRC                    size: 4

	/**
	 * An 802_11 Data packet.
	 *
	 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
	 * @since SWANS1.0
	 */

/*	public class Data extends MacMessage
	{
		

	} // class: Data
*/
} // class: MacMessage

