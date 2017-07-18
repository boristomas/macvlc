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
import java.util.UUID;

import com.puppycrawl.tools.checkstyle.api.Utils;

import driver.JistExperiment;
import jist.runtime.JistAPI;
import jist.runtime.Util;
import jist.swans.Constants;
import jist.swans.misc.Message;
import jist.swans.radio.VLCelement;

/**
 * Defines the various message used by the Mac entity.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MacMessage.java,v 1.1 2007/04/09 18:49:45 drchoffnes Exp $
 * @since SWANS1.0
 */

public abstract class MacMessage implements Message
{
	//sensor control
	private HashMap<Integer, HashSet<Integer>> SensorIDTx = new HashMap<Integer, HashSet<Integer>>();
	
	private HashMap<Integer, HashSet<Integer>> SensorIDRx = new HashMap<Integer, HashSet<Integer>>();
	//public HashSet<Integer> SensorIDRx = new HashSet<Integer>();
	
	private HashMap<VLCelement, Long> StartRx = new HashMap<VLCelement, Long>();
	
	/*
	 * Checks if message is valid for vlc, it is false when mesasge is dropped, set at checkphyconditions at receive side. 
	 */
	public boolean isVLCvalid = true;
	public long getStartRx(VLCelement sensor)
	{
		if(!StartRx.containsKey(sensor))
		{
			StartRx.put(sensor, (long)0);
		}
		return StartRx.get(sensor);
	}
	public void setStartRx(VLCelement sensor, long value)
	{
		StartRx.put(sensor, value);
	}
	
	private HashMap<VLCelement, Long> EndRx = new HashMap<VLCelement, Long>();
	public long getEndRx(VLCelement sensor)
	{
		if(!EndRx.containsKey(sensor))
		{
			EndRx.put(sensor, (long)0);
		}
		return EndRx.get(sensor);
	}
	public void setEndRx(VLCelement sensor, long value)
	{
		EndRx.put(sensor, value);
	}
	private HashMap<VLCelement, Long> DurationRx = new HashMap<VLCelement, Long>();
	public long getDurationRx(VLCelement sensor)
	{
		if(!DurationRx.containsKey(sensor))
		{
			DurationRx.put(sensor, (long)0);
		}
		return DurationRx.get(sensor);
	}
	public void setDurationRx(VLCelement sensor, long value)
	{
		DurationRx.put(sensor, value);
	}
	
	private HashMap<VLCelement, Double> PowerRx = new HashMap<VLCelement, Double>();
	public double getPowerRx(VLCelement sensor)
	{
		if(!PowerRx.containsKey(sensor))
		{
			PowerRx.put(sensor, (double)0);
		}
		return PowerRx.get(sensor);
	}
	public void setPowerRx(VLCelement sensor, double value)
	{
		PowerRx.put(sensor, value);
	}
	
	private HashMap<VLCelement, Boolean> InterferedRx = new HashMap<VLCelement, Boolean>();
	public boolean getInterferedRx(VLCelement sensor)
	{
		if(!InterferedRx.containsKey(sensor))
		{
			InterferedRx.put(sensor, false);
		}
		return InterferedRx.get(sensor);
	}
	public void setInterferedRx(VLCelement sensor, boolean value)
	{
		InterferedRx.put(sensor, value);
	}
	
	private HashMap<VLCelement, Long> StartTx = new HashMap<VLCelement, Long>();
	public long getStartTx(VLCelement sensor)
	{
		if(!StartTx.containsKey(sensor))
		{
			StartTx.put(sensor, (long)0);
		}
		return StartTx.get(sensor);
	}
	public void setStartTx(VLCelement sensor, long value)
	{
		StartTx.put(sensor, value);
	}
	
	private HashMap<VLCelement, Long> EndTx = new HashMap<VLCelement, Long>();
	public long getEndTx(VLCelement sensor)
	{
		if(!EndTx.containsKey(sensor))
		{
			EndTx.put(sensor, (long)0);
		}
		return EndTx.get(sensor);
	}
	public void setEndTx(VLCelement sensor, long value)
	{
		EndTx.put(sensor, value);
	}
	
	private HashMap<VLCelement, Long> DurationTx = new HashMap<VLCelement, Long>();
	public long getDurationTx(VLCelement sensor)
	{
		if(!DurationTx.containsKey(sensor))
		{
			DurationTx.put(sensor, (long)0);
		}
		return DurationTx.get(sensor);
	}
	public void setDurationTx(VLCelement sensor, long value)
	{
		DurationTx.put(sensor, value);
	}
	private HashMap<VLCelement, Double> PowerTx = new HashMap<VLCelement,Double>();
	public double getPowerTx(VLCelement sensor)
	{
		if(!PowerTx.containsKey(sensor))
		{
			PowerTx.put(sensor, (double)0);
		}
		return PowerTx.get(sensor);
	}
	public void setPowerTx(VLCelement sensor, double value)
	{
		PowerTx.put(sensor, value);
	}
	
	private HashMap<VLCelement, Boolean> InterferedTx = new HashMap<VLCelement, Boolean>();
	public boolean getInterferedTx(VLCelement sensor)
	{
		if(!InterferedTx.containsKey(sensor))
		{
			InterferedTx.put(sensor, false);
		}
		return InterferedTx.get(sensor);
	}
	public void setInterferedTx(VLCelement sensor, boolean value)
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
	
	
	
	
	
	
	public int getElementIDTxSize(int nodeID)
	{
		return getElementIDTx(nodeID).size();
	}
	public int getElementIDRxSize(int nodeID)
	{
		return getElementIDRx(nodeID).size();
	}
	public void setElementIDTx(HashSet<Integer> value, Integer nodeID)
	{
		SensorIDTx.put(nodeID, value);
	}
	public void setElementIDRx(HashSet<Integer> value, Integer nodeID)
	{
		for (Integer sensor : value)
		{
			//System.out.println("set2: "+sensor+ " " + nodeID + " hsh: " + hashCode());
		}
		SensorIDRx.put(nodeID, value);
	}	
	public void setElementIDTx(LinkedList<VLCelement> value, Integer nodeID)
	{
		HashSet<Integer> hs = new HashSet<Integer>();
		for (VLCelement sensor : value)
		{
			hs.add(sensor.elementID);
		}
		SensorIDTx.put(nodeID, hs);
	}
	public void setElementIDRx(LinkedList<VLCelement> value, Integer nodeID)
	{
		HashSet<Integer> hs = new HashSet<Integer>();
		for (VLCelement sensor : value)
		{
			hs.add(sensor.elementID);
			//System.out.println("set1: "+sensor.sensorID+ " " + nodeID + " hsh: " + hashCode());
		}
		
		SensorIDRx.put(nodeID, hs);
	}
	/**
	 * Gets sensor ID this message is assigned to for Tx
	 * @return
	 */
	public HashSet<Integer> getElementIDTx(Integer nodeID)
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
	public HashSet<Integer> getElementIDRx(Integer nodeID)
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
	public void addElementIDTx(int sensorID, int nodeID)
	{
		getElementIDTx(nodeID).add(sensorID);
	}
	
	public void removeElementIDTx(int sensorID, int nodeID)
	{
		getElementIDTx(nodeID).remove(sensorID);
	}
	/**
	 * Add Rx sensor ID for this message
	 * @return
	 */
	public void addElementIDRx(int sensorID, int nodeID)
	{
		//System.out.println("add: "+sensorID+ " " + nodeID + " hsh: " + hashCode());
		getElementIDRx(nodeID).add(sensorID);
	}
	
	public void removeElementIDRx(int sensorID, int nodeID)
	{
		getElementIDRx(nodeID).remove(sensorID);
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
	protected MacMessage(byte type, boolean retry)
	{
		//this.TimeCreated = JistAPI.getTime();
		this.type = type;
		this.retry = retry;
		//this.setSensorIDRx(-1);
		//this.setSensorIDTx(-1);
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
	 * Return packet destination address.
	 *
	 * @return packet destination address
	 */
	public abstract MacAddress getDst();
	public abstract MacAddress getSrc();

	/**
	 * Return packet transmission duration.
	 *
	 * @return packet transmission duration
	 */
	public abstract int getDuration();


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

	public static class Rts extends MacMessage
	{
		public String getMessageID()
		  {
			  return "not implemented";
		  }
		
		/**
		 * RTS packet size.
		 */
		public static final int SIZE = 20;

		/**
		 * packet destination address.
		 */
		private MacAddress dst;

		/**
		 * packet source address.
		 */
		private MacAddress src;

		/**
		 * packet transmission duration.
		 */
		private int duration;

		//////////////////////////////////////////////////
		// initialization
		//

		/**
		 * Create an 802_11 RTS packet.
		 *
		 * @param dst packet destination address
		 * @param src packet source address
		 * @param duration packet transmission duration
		 */
		public Rts(MacAddress dst, MacAddress src, int duration)
		{
			super(TYPE_RTS, false);
			this.dst = dst;
			this.src = src;
			this.duration = duration;
		}

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
		 * Return packet transmission duration.
		 *
		 * @return packet transmission duration
		 */
		public int getDuration()
		{
			return duration;
		}

		//////////////////////////////////////////////////
		// message interface 
		//

		// Message interface
		/** {@inheritDoc} */
		public int getSize()
		{
			return SIZE;
		}

		// Message interface
		/** {@inheritDoc} */
		public void getBytes(byte[] msg, int offset)
		{
			throw new RuntimeException("todo: not implemented");
		}

	} // class: RTS


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

	public static class Cts extends MacMessage
	{
		/**
		 * CTS packet size.
		 */
		public static final int SIZE = 14;

		/**
		 * packet destination address.
		 */
		private MacAddress dst;
		private MacAddress src;

		/**
		 * packet transmission duration.
		 */
		private int duration;

		public String getMessageID()
		  {
			  return "not implemented";
		  }
		//////////////////////////////////////////////////
		// initialization
		//

		/**
		 * Create an 802_11 CTS packet.
		 *
		 * @param dst packet destination address
		 * @param duration packet transmission duration
		 */
		public Cts(MacAddress dst, MacAddress src, int duration)
		{
			super(TYPE_CTS, false);
			this.dst = dst;
			this.src = src;
			this.duration = duration;
		}

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
		public MacAddress getSrc()
		{
			return src;
		}

		/**
		 * Return packet transmission duration.
		 *
		 * @return packet transmission duration
		 */
		public int getDuration()
		{
			return duration;
		}

		//////////////////////////////////////////////////
		// message interface 
		//

		// Message interface
		/** {@inheritDoc} */
		public int getSize()
		{
			return SIZE;
		}

		// Message interface
		/** {@inheritDoc} */
		public void getBytes(byte[] msg, int offset)
		{
			throw new RuntimeException("todo: not implemented");
		}

	} // class: CTS


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

	public static class Ack extends MacMessage
	{
		public String getMessageID()
		  {
			  return "not implemented";
		  }
		
		/**
		 * ACK packet size.
		 */
		public static final int SIZE = 14;

		/**
		 * packet destination address.
		 */
		private MacAddress dst;

		/**
		 * packet transmission duration.
		 */
		private int duration;
		private MacAddress src;

		//////////////////////////////////////////////////
		// initialization
		//

		/**
		 * Create 802_11 ACK packet.
		 *
		 * @param dst packet destination address
		 * @param duration packet transmission duration
		 */
		public Ack(MacAddress dst, MacAddress src,int duration)
		{
			super(TYPE_ACK, false);
			this.dst = dst;
			this.src = src;
			this.duration = duration;
		}

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
		public MacAddress getSrc()
		{
			return src;
		}

		/**
		 * Return packet transmission duration.
		 *
		 * @return packet transmission duration
		 */
		public int getDuration()
		{
			return duration;
		}

		//////////////////////////////////////////////////
		// message interface 
		//

		// Message interface
		/** {@inheritDoc} */
		public int getSize()
		{
			return SIZE;
		}

		// Message interface
		/** {@inheritDoc} */
		public void getBytes(byte[] msg, int offset)
		{
			throw new RuntimeException("todo: not implemented");
		}

	} // class: ACK



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

	public static class Data extends MacMessage
	{
		public String getMessageID()
		  {
			  return ID;
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
		public MacAddress dst;

		/**
		 * Packet source address.
		 */
		public MacAddress src;

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
		public Data(MacAddress dst, MacAddress src, int duration, short seq, short frag, 
				boolean moreFrag, boolean retry, Message body)
		{
			super(TYPE_DATA, retry);
			this.dst = dst;
			this.src = src;
			this.duration = duration;
			this.seq = seq;
			this.frag = frag;
			this.body = body;
			this.ID = UUID.randomUUID().toString().replaceAll("-", "");
		}
		String ID= "";

		/**
		 * Create 802_11 data packet.
		 *
		 * @param dst packet destination address
		 * @param src packet source address
		 * @param duration packet transmission duration
		 * @param body packet data payload
		 */
		public Data(MacAddress dst, MacAddress src, int duration, Message body)
		{
			this(dst, src, duration, (short)-1, (short)-1, false, false, body);
		}

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

	} // class: Data

} // class: MacMessage

