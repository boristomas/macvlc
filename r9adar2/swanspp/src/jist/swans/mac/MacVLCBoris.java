//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Mac802_11.java Thu 2005/02/10 11:45:06 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import java.util.HashMap;

import sun.management.Sensor;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.field.StreetMobility;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.NetInterface;
import jist.swans.net.NetMessage;
import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioInterface;
import jist.swans.radio.TimeEntry;
import jist.swans.trans.TransUdp;
import driver.JistExperiment;

/**
 * Implementation of MAC VLC. 
 * @author boris.tomas@foi.hr;
 * @version 1
 */

public class MacVLCBoris implements MacInterface.Mac802_11
{

	////////////////////////////////////////////////////
	// short 802.11 lexicon:
	//   slot - minimum time to sense medium
	//   nav  - network allocation vector (virtual carrier sense)
	//   sifs - short inter frame space
	//   pifs - point coordination inter frame space
	//   difs - distributed inter frame space
	//   eifs - extended inter frame space
	//   cw   - collision (avoidance) window
	//   DSSS - Direct Sequence Spread spectrum
	//   FHSS - Frequency Hopping Spread Spectrum
	//

	//////////////////////////////////////////////////
	// constants
	//

	/** Physical specification constant: 802_11b-1999 Supplement 2_4GHz Direct Sequence. */
	public static final boolean DSSS = true;

	/** Physical specification constant: 802_11b-1999 2_4GHz Frequency Hopping. */
	public static final boolean FHSS = false;
	static
	{
		if(!DSSS ^ FHSS)
		{
			throw new RuntimeException("select either DSSS or FHSS");
		}
	}

	/** Length of PHY preamble. */
	public static final long PREAMBLE = 144;
	/** Length of short PHY preamble.*/
	public static final long PREAMBLE_SHORT = 72;

	/** Length of PLCP Header in bits. */
	public static final long PLCP_HEADER = 48;

	/** PHY synchronization bits. */
	public static final long SYNCHRONIZATION_SHORT = PREAMBLE_SHORT + PLCP_HEADER;

	/** PHY synchronization bits. */
	public static final long SYNCHRONIZATION = PREAMBLE + PLCP_HEADER;
	/** Receive-Transmit turnaround time. */
	public static final long RX_TX_TURNAROUND = 5*Constants.MICRO_SECOND;

	/** Air propagation delay. */
	public static final long PROPAGATION = Constants.MICRO_SECOND;

	/** Minimum time to sense medium. */
	public static final long SLOT_TIME;
	static
	{
		if(DSSS) SLOT_TIME = 20*Constants.MICRO_SECOND;
		if(FHSS) SLOT_TIME = 50*Constants.MICRO_SECOND;
	}

	/**
	 * Short interframe space. Minimum wait time between two frames in the same
	 * communication session.
	 */
	public static final long SIFS;
	static
	{
		if(DSSS) SIFS = 10*Constants.MICRO_SECOND;
		if(FHSS) SIFS = 28*Constants.MICRO_SECOND;
	}

	/**
	 * Point coordination inter frame space. Wait used by the access point (point
	 * coordinator) to gain access to the medium before any of the stations.
	 */
	public static final long PIFS = SIFS + SLOT_TIME;

	/**
	 * Distributed inter frame space. Wait used by stations to gain access to the medium.
	 */
	public static final long DIFS = SIFS + 2*SLOT_TIME;

	/** Transmit start SIFS. */
	public static final long TX_SIFS = SIFS - RX_TX_TURNAROUND;

	/** Transmit start DIFS. */
	public static final long TX_DIFS = DIFS - RX_TX_TURNAROUND;

	/**
	 * Extended inter frame space. Wait used by stations to gain access to the
	 * medium after an error.
	 */
	public static final long EIFS = SIFS + DIFS + SYNCHRONIZATION*(Constants.SECOND/JistExperiment.getJistExperiment().getBandwidth()) + 8*MacMessage.Ack.SIZE*Constants.MICRO_SECOND;

	/**
	 * Threshold packet size to activate RTS. Default=3000. Broadcast packets
	 * never use RTS. Set to zero to always use RTS.
	 */ 
	public static final int THRESHOLD_RTS = 3000;

	/**
	 * Threshold packet size for fragmentation. Default=2346. Broadcast packets
	 * are not fragmented.
	 */
	public static final int THRESHOLD_FRAGMENT = 2346;

	/** Retransmissions attempted for short packets (those without RTS). */
	public static final byte RETRY_LIMIT_SHORT = 7;

	/** Retransmissions attempted for long packets (those with RTS). */
	public static final byte RETRY_LIMIT_LONG  = 4;

	/** Minimum collision window (for backoff). */
	public static final short CW_MIN;
	static
	{
		if(DSSS) CW_MIN = 31;
		if(FHSS) CW_MIN = 15;
	}

	/** Maximum collision window (for backoff). */
	public static final short CW_MAX = 1023;

	/** Invalid sequence number. */
	public static final short SEQ_INVALID = -1;

	/** Sequence number cache size. */
	public static final short SEQ_CACHE_SIZE = 5;

	// mac modes

	/** mac mode: idle. */
	public static final byte MAC_MODE_SIDLE          = 0;
	/** mac mode: waiting for difs or eifs timer. */
	public static final byte MAC_MODE_DIFS           = 1;
	/** mac mode: waiting for backoff. */
	public static final byte MAC_MODE_SBO            = 2;
	/** mac mode: waiting for virtual carrier sense. */
	public static final byte MAC_MODE_SNAV           = 3;
	/** mac mode: waiting for virtual carrier sense to RTS. */
	public static final byte MAC_MODE_SNAV_RTS       = 4;
	/** mac mode: waiting for CTS packet. */
	public static final byte MAC_MODE_SWFCTS         = 5;
	/** mac mode: waiting for DATA packet. */
	public static final byte MAC_MODE_SWFDATA        = 6;
	/** mac mode: waiting for ACK packet. */
	public static final byte MAC_MODE_SWFACK         = 7;
	/** mac mode: transmitting RTS packet. */
	public static final byte MAC_MODE_XRTS           = 8;
	/** mac mode: transmitting CTS packet. */
	public static final byte MAC_MODE_XCTS           = 9;
	/** mac mode: transmitting unicast DATA packet. */
	public static final byte MAC_MODE_XUNICAST       = 10;
	/** mac mode: transmitting broadcast DATA packet. */
	public static final byte MAC_MODE_XBROADCAST     = 11;
	/** mac mode: transmitting ACK packet. */
	public static final byte MAC_MODE_XACK           = 12;

	public static String getModeString(byte mode)
	{
		switch(mode)
		{
		case MAC_MODE_SIDLE: return "IDLE";
		case MAC_MODE_DIFS: return "DIFS";
		case MAC_MODE_SBO: return "BO";
		case MAC_MODE_SNAV: return "NAV";
		case MAC_MODE_SNAV_RTS: return "NAV_RTS";
		case MAC_MODE_SWFCTS: return "WF_CTS";
		case MAC_MODE_SWFDATA: return "WF_DATA";
		case MAC_MODE_SWFACK: return "WF_ACK";
		case MAC_MODE_XRTS: return "X_RTS";
		case MAC_MODE_XCTS: return "X_CTS";
		case MAC_MODE_XUNICAST: return "X_UNICAST";
		case MAC_MODE_XBROADCAST: return "X_BROADCAST";
		case MAC_MODE_XACK: return "X_ACK";
		default: throw new RuntimeException("unknown mode: "+mode);
		}
	}
	// MacInterface.Mac802_11 interface
		public void cfDone(boolean backoff, boolean delPacket)
		{
			if(backoff)
			{
				//setBackoff();
			}
//			doDifs();
			if(delPacket)
			{
				packet = null;
				packetNextHop = null;
				netEntity.pump(netId);
			}
		}

	//////////////////////////////////////////////////
	// locals
	//

	// entity hookup
	/** Self-referencing mac entity reference. */
	protected final MacInterface.Mac802_11 self;
	/** Radio downcall entity reference. */
	protected RadioInterface radioEntity;
	/** Network upcall entity interface. */
	protected NetInterface netEntity;
	/** network interface number. */
	protected byte netId;

	// properties

	/** link bandwidth (units: bytes/second). */
	protected final int bandwidth;

	/** mac address of this interface. */
	protected MacAddress localAddr;

	/** whether mac is in promiscuous mode. */
	protected boolean promisc;

	// status

	/** current mac mode. */
	protected byte mode;

	/** radio mode used for carrier sense. */
	protected byte radioMode;

	/** whether last reception had an error. */
//	protected boolean needEifs;

	// timer

	/** timer identifier. */
	protected byte timerId;

	// backoff

	/** backoff time remaining. */
	protected long bo;

	/** backoff start time. */
	protected long boStart;

	/** current contention window size. */
	protected short cw;

	// nav

	/** virtual carrier sense; next time when network available. */
	protected long nav;

	// sequence numbers

	/** sequence number counter. */
	protected short seq;


	/** size of received sequence number cache list. */
	protected byte seqCacheSize;

	// retry counts

	/** short retry counter. */
	protected byte shortRetry;

	/** long retry counter. */
	protected byte longRetry;

	// current packet

	/** packet currently being transmitted. */
	protected Message packet;

	/** next hop of packet current being transmitted. */
	protected MacAddress packetNextHop;


	/** hook-up to MobilityInfo */
	protected StreetMobility sm =null;

	private double airTime;

	// stats
	/** collects network stats */
	public static MacStats stats = new MacStats();

	/** mapping of dropped packet type to counts */
	protected HashMap droppedPackets;


	//////////////////////////////////////////////////
	// initialization 
	//

	/**
	 * Instantiate new 802_11b entity.
	 *
	 * @param addr local mac address
	 * @param radioInfo radio properties
	 * @param newstats mac stats object
	 */
	public MacVLCBoris(MacAddress addr, RadioInfo radioInfo, boolean promisc)
	{
		SourceNodeID = addr.hashCode();//BT: store node ID;
		droppedPackets = new HashMap();
		// properties
		bandwidth = radioInfo.getShared().getBandwidth() / 8;

		// proxy
		self = (MacInterface.Mac802_11)JistAPI.proxy(this, MacInterface.Mac802_11.class);
		init(addr, radioInfo, promisc);
	}

	/**
	 * nodeID for source radio.
	 */
	public int SourceNodeID;
	
	/**
	 * Instantiate new 802_11b entity.
	 *
	 * @param addr local mac address
	 * @param radioInfo radio properties
	 */
	public MacVLCBoris(MacAddress addr, RadioInfo radioInfo)
	{  
		SourceNodeID = addr.hashCode();//BT: store node ID;
		bandwidth = radioInfo.getShared().getBandwidth() / 8;
		// proxy
		self = (MacInterface.Mac802_11)JistAPI.proxy(this, MacInterface.Mac802_11.class);
		init(addr, radioInfo, Constants.MAC_PROMISCUOUS_DEFAULT);
	}

	void init(MacAddress addr, RadioInfo radioInfo, boolean promisc)
	{
		// properties
		localAddr = addr;
		this.promisc = promisc;
		// status
		mode = MAC_MODE_SIDLE;
		radioMode = Constants.RADIO_MODE_IDLE;
		//needEifs = false;
		// timer identifier
		timerId = 0;
		// backoff
		bo = 0;
		boStart = 0;
		cw = CW_MIN;
		// virtual carrier sense
		nav = -1;
		// sequence numbers
		seq = 0;

		seqCacheSize = 0;
		// retry counts
		shortRetry = 0;
		longRetry = 0;
		// current packet
		packet = null;
		packetNextHop = null;
	}

	//////////////////////////////////////////////////
	// entity hookup
	//

	/**
	 * Return proxy entity of this mac.
	 *
	 * @return self-referencing proxy entity.
	 */
	public MacInterface.Mac802_11 getProxy()
	{
		return this.self;
	}

	/**
	 * Hook up with the radio entity.
	 *
	 * @param radio radio entity
	 */
	public void setRadioEntity(RadioInterface radio)
	{
		if(!JistAPI.isEntity(radio)) throw new IllegalArgumentException("expected entity");
		this.radioEntity = radio;
	}

	/**
	 * Hook up with the network entity.
	 *
	 * @param net network entity
	 * @param netid network interface number
	 */
	public void setNetEntity(NetInterface net, byte netid)
	{
		if(!JistAPI.isEntity(net)) throw new IllegalArgumentException("expected entity");
		this.netEntity = net;
		this.netId = netid;
	}

	
	//////////////////////////////////////////////////
	// mac states
	//

	/**
	 * Set the current mac mode.
	 *
	 * @param mode new mac mode
	 */
	private void setMode(byte mode)
	{
		this.mode = mode;
	}

	/**
	 * Return whether the mac is currently waiting for a response.
	 *
	 * @return whether mac waiting for response
	 */
	public boolean isAwaitingResponse()
	{
		switch(mode)
		{
			case MAC_MODE_SWFCTS:
			case MAC_MODE_SWFDATA:
			case MAC_MODE_SWFACK:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Return whether the mac is currently transmitting.
	 *
	 * @return whether mac is currently transmitting
	 */
	public boolean isTransmitting()
	{
		switch(mode)
		{
			case MAC_MODE_XRTS:
			case MAC_MODE_XCTS:
			case MAC_MODE_XUNICAST:
			case MAC_MODE_XBROADCAST:
			case MAC_MODE_XACK:
				return true;
			default:
				return false;
		}
	}

	//////////////////////////////////////////////////
	// packet queries
	//

	/**
	 * Return whether mac currently has a packet to send.
	 *
	 * @return whether mac has packet to send.
	 */
	public boolean hasPacket()
	{
		return packet!=null;
	}

	/**
	 * Return whether current packet is to be broadcast.
	 *
	 * @return whether current packet is to be broadcast.
	 */
	private boolean isBroadcast()
	{
		return MacAddress.ANY.equals(packetNextHop);
	}


	/**
	 * Return whether current packet requires fragmentation.
	 *
	 * @return does current packet require fragmentation.
	 */
	private boolean shouldFragment()
	{
		return packet.getSize()>THRESHOLD_FRAGMENT && !isBroadcast();
	}

	/**
	 * Compute packet transmission time at current bandwidth.
	 *
	 * @param msg packet to transmit
	 * @return time to transmit given packet at current bandwidth
	 */
	private long transmitTime(Message msg)
	{
		int size = msg.getSize();
		if(size==Constants.ZERO_WIRE_SIZE)
		{
			return Constants.EPSILON_DELAY;
		}
		return (SYNCHRONIZATION + size) * Constants.SECOND/bandwidth;
	}

	/**
	 * Determine whether channel is idle according to both physical and virtual
	 * carrier sense.
	 *
	 * @return physical and virtual carrier sense
	 */
	private boolean isCarrierIdle()
	{
		return isRadioIdle();
		//maknuo sam nav
	}

	
	//////////////////////////////////////////////////
	// send-related functions
	//

	// MacInterface interface
	@SuppressWarnings("unused")
	public void send(Message msg, MacAddress nextHop)
	{
//		netEntity.packetDropped(msg, nextHop);

		((NetMessage.Ip)msg).Times.add(new TimeEntry(1, "macbt", null));//= JistAPI.getTime();
		MacMessage.Data data = new MacMessage.Data(nextHop, localAddr,0, msg);
				
		if(isRadioIdle())
		{
				// set mode and transmit
				setMode(MAC_MODE_XBROADCAST);
				long delay = RX_TX_TURNAROUND, duration = transmitTime(data);
				radioEntity.transmit(data, delay, duration);
				// wait for EOT, check for outgoing packet
				JistAPI.sleep(delay+duration);
			//	self.cfDone(true, true);
		}	
		else
		{
	//		netEntity.packetDropped(msg, nextHop);
		}
		
		//if(Main.ASSERT) Util.assertion(!hasPacket());
		//if(Main.ASSERT) Util.assertion(nextHop!=null);
	/*	packet = msg;
		packetNextHop = nextHop;
		if(mode==MAC_MODE_SIDLE)
		{
			if(!isCarrierIdle())
			{
			//	setBackoff();
			}
			doDifs();
		}*/
	}

/*	private void doDifs()
	{
		if(isRadioIdle())
		{
			if(waitingNav())
			{
				startTimer(nav-JistAPI.getTime(), MAC_MODE_SNAV);
			}
			else
			{
			//	startTimer(needEifs ? EIFS : DIFS, MAC_MODE_DIFS);
			}
		}
		else
		{
			idle();
		}
	}
	private boolean waitingNav()
	{
		return false;
		//return nav > JistAPI.getTime();
	}
*/

	

/*	private void sendData(boolean afterCts)
	{
		if(isBroadcast())
		{
			sendDataBroadcast();
		}
	}*/

/*	private void sendDataBroadcast()
	{

	//	MacMessage.Data msg;
		/*	for (Integer item : getQualifiyingNodes()) 
		{
			//msg = new MacMessage.Data( new MacAddress(item), localAddr, 0, packet);
			packetNextHop = new MacAddress(item);
			sendDataUnicast(false);
			//send(msg, new MacAddress(item));
		}*/
		
		// create data packet
	/*			MacMessage.Data data = new MacMessage.Data(packetNextHop, localAddr,0, packet);
		//		data.TimeCreated = ((MacMessage)packet).TimeCreated;//.TimeEntry = JistAPI.getTime();
		//		data.TimeEntry = ((MacMessage)packet).TimeEntry;
		//		data.TimeSent =JistAPI.getTime();
				
				// set mode and transmit
				setMode(MAC_MODE_XBROADCAST);
				long delay = RX_TX_TURNAROUND, duration = transmitTime(data);
				radioEntity.transmit(data, delay, duration);
				// wait for EOT, check for outgoing packet
				JistAPI.sleep(delay+duration);
				self.cfDone(true, true);
	}*/

	


	//////////////////////////////////////////////////
	// receive-related functions
	//

	// MacInterface
	public void peek(Message msg)
	{
	//	needEifs = true;
		if (mode == MAC_MODE_SNAV_RTS) 
		{
			idle();
		}
	}

	// MacInterface
	public void receive(Message msg)
	{
		receivePacket((MacMessage)msg);
	}

	private void receivePacket(MacMessage msg)
	{
		//needEifs = false;
		MacAddress dst = msg.getDst();
		switch(msg.getType())
		{
		case MacMessage.TYPE_DATA:
			receiveData((MacMessage.Data)msg);
			break;
		default:
			throw new RuntimeException("illegal frame type");
		}
		/*
		if(localAddr.equals(dst))
		{
			
		}
		else if(MacAddress.ANY.equals(dst))
		{
			switch(msg.getType())
			{
			case MacMessage.TYPE_DATA:
				receiveData((MacMessage.Data)msg);
				break;
			default:
				throw new RuntimeException("illegal frame type");
			}
		}
		else
		{
			// does not belong to node
			receiveForeign(msg);
		}*/
	}

	
	private void receiveData(MacMessage.Data msg)
	{
		
		// not in standard, but ns-2 does.
		// decCW();
		// shortRetry = 0;
	//	if(mode==MAC_MODE_SWFDATA || !isAwaitingResponse())
	//	{
		//	if(MacAddress.ANY.equals(msg.getDst()))
		//	{
				((NetMessage.Ip)msg.getBody()).Times.add(new TimeEntry(3, "macbtrec", null));
				netEntity.receive(msg.getBody(), msg.getSrc(), netId, false);
				//cfDone(false, false);
		//	}
		//	else
		//	{
		//		cancelTimer();
		//	}
		//}
	}
/*
	private void receiveForeign(MacMessage msg)
	{
		long currentTime = JistAPI.getTime();
		long nav2 = currentTime + msg.getDuration() + Constants.EPSILON_DELAY;
		if(nav2 > this.nav)
		{
			this.nav = nav2;
			if (isRadioIdle() && hasPacket())
			{
				// This is what we should do.
				//
				//if (msg.getType()==MacMessage.TYPE_RTS) 
				//{
				// If RTS-ing node failed to get a CTS and start sending then
				// reset the NAV (MAC layer virtual carrier sense) for this
				// bystander node.
				//   startTimer(PROPAGATION + SIFS + 
				//     SYNCHRONIZATION + MacMessage.Cts.SIZE*Constants.SECOND/bandwidth + 
				//     PROPAGATION + 2*SLOT_TIME, 
				//     MAC_MODE_SNAV_RTS);
				//} 
				//else 
				//{
				//   startTimer(NAV - currentTime, MAC_MODE_SNAV);
				//}

				// This is for ns-2 comparison.
				startTimer(nav - currentTime, MAC_MODE_SNAV);
			}
		}
		if (promisc && msg.getType()==MacMessage.TYPE_DATA)
		{
			MacMessage.Data macDataMsg = (MacMessage.Data)msg;
			netEntity.receive(macDataMsg.getBody(), macDataMsg.getSrc(), netId, true);
		}
	}*/

	//////////////////////////////////////////////////
	// radio mode
	//

	// MacInterface
	public void setRadioMode(byte mode)
	{
		this.radioMode = mode;
		switch(mode)
		{
		case Constants.RADIO_MODE_IDLE:
			radioIdle();
			break;
		case Constants.RADIO_MODE_SENSING:
			radioBusy();
			break;
		case Constants.RADIO_MODE_RECEIVING:
			radioBusy();
			/*
          todo:
          ExaminePotentialIncomingMessage(newMode, receiveDuration, potentialIncomingPacket);
			 */
			break;
		case Constants.RADIO_MODE_SLEEP:
			radioBusy();
			break;
		}
	}

	private boolean isRadioIdle()
	{
		return radioMode==Constants.RADIO_MODE_IDLE;
	}

	private void radioBusy()
	{
		switch(mode)
		{
		case MAC_MODE_SBO:
			idle();
			break;
		case MAC_MODE_DIFS:
		case MAC_MODE_SNAV:
			idle();
			break;
		case MAC_MODE_SIDLE:
		case MAC_MODE_SWFCTS:
		case MAC_MODE_SWFDATA:
		case MAC_MODE_SWFACK:    
		case MAC_MODE_XBROADCAST:
		case MAC_MODE_XUNICAST:
			// TODO drc: dunno if the following ones should be here...
		case MAC_MODE_XACK:
			// don't care
			break;
		default:
			// rimtodo: really unexpected?
			//throw new RuntimeException("unexpected mode: "+getModeString(mode));
			System.err.println("unexpected mode: "+getModeString(mode));
		}
	}

	private void radioIdle()
	{
		switch(mode)
		{
		case MAC_MODE_SIDLE:
			// TODO drc: are these in the right place
		case MAC_MODE_SNAV: // not sure, but maybe we want to do Difs here
	//		doDifs();
			break;
		case MAC_MODE_SWFCTS:
		case MAC_MODE_SWFDATA:
		case MAC_MODE_SWFACK:
		case MAC_MODE_DIFS:
		case MAC_MODE_SBO:
		case MAC_MODE_XUNICAST:
		case MAC_MODE_XBROADCAST:
		case MAC_MODE_XACK:
		case MAC_MODE_XCTS: // drc: added here (and below) due to exception
		case MAC_MODE_XRTS:
			// don't care          
			break;
		default:
			// rimtodo: really unexpected?
			//throw new RuntimeException("unexpected mode: "+getModeString(mode));
			System.err.println("unexpected mode: "+getModeString(mode));
		}
	}

	//////////////////////////////////////////////////
	// timer routines
	//

	// MacInterface
	public void startTimer(long delay, byte mode)
	{
		cancelTimer();
		setMode(mode);
		JistAPI.sleep(delay);
		self.timeout(timerId);
	}

	/**
	 * Cancel timer event, by incrementing the timer identifer.
	 */
	private void cancelTimer()
	{
		timerId++;
	}

	private void idle()
	{
		cancelTimer();
		setMode(MAC_MODE_SIDLE);
	}

	// MacInterface
	public void timeout(int timerId)
	{
	
		if(timerId!=this.timerId) return;
		switch(mode)
		{
		case MAC_MODE_SIDLE:
			idle();
			break;
		case MAC_MODE_DIFS:
			{
				idle();
				if (packet==null && packetNextHop==null) 
					netEntity.pump(netId); // DRC: TODO check
			}
			break;
		case MAC_MODE_SBO:
			if(Main.ASSERT) Util.assertion(boStart + bo == JistAPI.getTime());
			if(hasPacket())
			{
		//		sendData(false);
			}
			else
			{
				idle();
			}
			break;
		case MAC_MODE_SNAV_RTS:
		case MAC_MODE_SNAV:
		
		//	doDifs();
			break;
		case MAC_MODE_SWFDATA:

			//doDifs();
			break;

		case MAC_MODE_SWFACK:
			stats.retries++;
		case MAC_MODE_SWFCTS:    
			break;
		default:
			//throw new RuntimeException("unexpected mode: "+mode);
		//	System.err.println("unexpected mode: "+getModeString(mode));
		}
	}

	/**
	 * @param sm The StreetMobility object to set.
	 */
	public void setSm(StreetMobility sm)
	{
		this.sm = sm;
	}

	/* (non-Javadoc)
	 * @see jist.swans.mac.MacInterface.Mac802_11#getRadioActivity()
	 */
	public void updateRadioActivity(double airTime) 
	{
		this.airTime = airTime;
	}

	@Override
	public String toString() 
	{
		return localAddr.toString();
	}

	public void notifyInterference(Sensor[] sensors) 
	{
		System.out.println("interference");
	}
	
	public void notifyError(int errorCode, String message) 
	{
		System.out.println("notfiy error #"+errorCode);
	}

	public void notifyTransmitFail(Message msg, int errorCode) 
	{
		System.out.println("transmit fail error #"+errorCode);
	}
	
	public void notifyReceiveFail(Message msg, int errorCode) {
		System.out.println("receive fail error #"+errorCode);
		
	}
}

