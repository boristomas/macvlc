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

public abstract class MacMessage implements Message
{
	//sensor control
	private HashMap<Integer, HashSet<Integer>> SensorIDTx = new HashMap<Integer, HashSet<Integer>>();
	private HashMap<Integer, HashSet<Integer>> SensorIDRx = new HashMap<Integer, HashSet<Integer>>();
	//public HashSet<Integer> SensorIDRx = new HashSet<Integer>();
	
	private HashMap<Integer, Long> StartRx = new HashMap<Integer, Long>();
	public long getStartRx(int nodeID)
	{
		if(!StartRx.containsKey(nodeID))
		{
			StartRx.put(nodeID, (long)0);
		}
		return StartRx.get(nodeID);
	}
	public void setStartRx(int nodeID, long value)
	{
		StartRx.put(nodeID, value);
	}
	
	private HashMap<Integer, Long> EndRx = new HashMap<Integer, Long>();
	public long getEndRx(int nodeID)
	{
		if(!EndRx.containsKey(nodeID))
		{
			EndRx.put(nodeID, (long)0);
		}
		return EndRx.get(nodeID);
	}
	public void setEndRx(int nodeID, long value)
	{
		EndRx.put(nodeID, value);
	}
	private HashMap<Integer, Long> DurationRx = new HashMap<Integer, Long>();
	public long getDurationRx(int nodeID)
	{
		if(!DurationRx.containsKey(nodeID))
		{
			DurationRx.put(nodeID, (long)0);
		}
		return DurationRx.get(nodeID);
	}
	public void setDurationRx(int nodeID, long value)
	{
		DurationRx.put(nodeID, value);
	}
	
	private HashMap<Integer, Double> PowerRx = new HashMap<Integer, Double>();
	public double getPowerRx(int nodeID)
	{
		if(!PowerRx.containsKey(nodeID))
		{
			PowerRx.put(nodeID, (double)0);
		}
		return PowerRx.get(nodeID);
	}
	public void setPowerRx(int nodeID, double value)
	{
		PowerRx.put(nodeID, value);
	}
	
	private HashMap<Integer, Boolean> InterferedRx = new HashMap<Integer, Boolean>();
	public boolean getInterferedRx(int nodeID)
	{
		if(!InterferedRx.containsKey(nodeID))
		{
			InterferedRx.put(nodeID, false);
		}
		return InterferedRx.get(nodeID);
	}
	public void setInterferedRx(int nodeID, boolean value)
	{
		InterferedRx.put(nodeID, value);
	}
	
	private HashMap<Integer, Long> StartTx = new HashMap<Integer, Long>();
	public long getStartTx(int nodeID)
	{
		if(!StartTx.containsKey(nodeID))
		{
			StartTx.put(nodeID, (long)0);
		}
		return StartTx.get(nodeID);
	}
	public void setStartTx(int nodeID, long value)
	{
		StartTx.put(nodeID, value);
	}
	
	private HashMap<Integer, Long> EndTx = new HashMap<Integer, Long>();
	public long getEndTx(int nodeID)
	{
		if(!EndTx.containsKey(nodeID))
		{
			EndTx.put(nodeID, (long)0);
		}
		return EndTx.get(nodeID);
	}
	public void setEndTx(int nodeID, long value)
	{
		EndTx.put(nodeID, value);
	}
	
	private HashMap<Integer, Long> DurationTx = new HashMap<Integer, Long>();
	public long getDurationTx(int nodeID)
	{
		if(!DurationTx.containsKey(nodeID))
		{
			DurationTx.put(nodeID, (long)0);
		}
		return DurationTx.get(nodeID);
	}
	public void setDurationTx(int nodeID, long value)
	{
		DurationTx.put(nodeID, value);
	}
	private HashMap<Integer, Double> PowerTx = new HashMap<Integer,Double>();
	public double getPowerTx(int nodeID)
	{
		if(!PowerTx.containsKey(nodeID))
		{
			PowerTx.put(nodeID, (double)0);
		}
		return PowerTx.get(nodeID);
	}
	public void setPowerTx(int nodeID, double value)
	{
		PowerTx.put(nodeID, value);
	}
	
	private HashMap<Integer, Boolean> InterferedTx = new HashMap<Integer, Boolean>();
	public boolean getInterferedTx(int nodeID)
	{
		if(!InterferedTx.containsKey(nodeID))
		{
			InterferedTx.put(nodeID, false);
		}
		return InterferedTx.get(nodeID);
	}
	public void setInterferedTx(int nodeID, boolean value)
	{
		InterferedTx.put(nodeID, value);
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
		SensorIDRx.put(nodeID, value);
	}	
	public void setSensorIDTx(LinkedList<VLCsensor> value, Integer nodeID)
	{
		HashSet<Integer> hs = new HashSet<Integer>();
		for (VLCsensor sensor : value)
		{
			hs.add(sensor.sensorID);
		}
		SensorIDTx.put(nodeID, hs);
	}
	public void setSensorIDRx(LinkedList<VLCsensor> value, Integer nodeID)
	{
		HashSet<Integer> hs = new HashSet<Integer>();
		for (VLCsensor sensor : value)
		{
			hs.add(sensor.sensorID);
		}
		SensorIDRx.put(nodeID, hs);
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
		}

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

