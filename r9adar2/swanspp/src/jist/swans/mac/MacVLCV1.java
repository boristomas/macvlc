//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Mac802_11.java Thu 2005/02/10 11:45:06 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.corba.se.spi.orbutil.fsm.State;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.StreetMobility;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetMessage;
import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioInterface;
import jist.swans.radio.RadioVLC;
import jist.swans.radio.TimeEntry;
import jist.swans.radio.VLCsensor;
import jist.swans.radio.VLCsensor.SensorStates;
import jobs.JobConfigurator;
import driver.JistExperiment;

/**
 * Implementation of MAC VLC for V2V. 
 * @author boris.tomas@foi.hr;
 * @version 1
 */

public class MacVLCV1 implements MacInterface.VlcMacInterface//  MacInterface.Mac802_11
{
	
	/*TODO:
	 * # ok - Koristiti kontrolni kanal.
	 * 
	 * # ok - Napraviti varijantu samo sa redom, poruke se salju na sve senzore 
	 * 
	 * # ok - Napraviti varijantu sa redom i sa slanjem poruke samo na onaj senzor na koji je primljena nekada bila poruka.
	 *   treba napraviti katalog u koji cu zapisivati adresu odredista, na koji senzor je primljena poruka i kada je primljena poruka na tom senzoru.
	 * 
	 * # ok - Napraviti tablicu sensora koji primaju i salju, i ako ja saljem jednom mac sloju i on primi poruku na senzor 5 onda ce na njegovoj strani znaciti da treba poslati poruku
	 *   na senzore 1 i 3, naravno, prvi puta, dok se ne dobije odgovor se salje na sve senzore. obzirom da ce rezultat odabira senzora biti lista onda se salje na prvi koji je slobodan.
	 * 
	 * # ok - u v3. koristim staticni layout senzora, npr ako ja znam da mi je poruka dosla na senzor 6 od posiljatelja A onda cu posiljatelju A slati sa senzora 2 i 4.
	 *   u nekom boljem scenariju svaki cvor bi trebao znati konkretni bearing od senzora + vision angle i sl. pa da u realnom vremenu ja mogu odluciti sa kojeg cu slati, a znam
	 *   ID senzora s kojeg je poruka poslana meni (pise u mac poruci.), a i znam na kojem mojem je prethodna poruka primljena.
	 *
	 * # ja bi implementirao poseban state, security critical - npr u slucaju nesrece. auto koji je izvor odasilje kontrolni signal (mozda neki posebni) i onda mac zaustavlja sve i svaki
	 *   bit koji se primi na fotodiodi se automatski relaya na transmittere iza (suprotno od dolaznog vektora), dok se to relaya MAC/radio pokusava procitati poruku, fora 
	 *   je da su sve poruke koje dolaze iste i evidentno stvaraju koliziju, meðutim! poruka se moze dekodirati jer ako je ista i dolazi u isto vrijeme onda nema problema, mali
	 *   problem je ako dolazi sa vremenskim pomakom od npr duzine jednog bita. prvi bit nece biti u koliziji, meðutim drugi hoce, ali ako znam prethodni i znam stanje kolizije, onda mogu 
	 *   "zakljuciti" o drugom bitu. i tako dalje za sve ostale. ne treba crc i sl za ovo jer se u ovom slucaju poruke salju jako sporo (1mb/s???), to ce biti ako vise cvorova, lako je sa pomakom za jedan bit, ???? 
	 * 
	 * NOTE: 
	 * # 71 i 72 vremena nisu zapisana zbog vremena i njihovo vrijeme nije toliko bitno (ni tocno), ta vremena sluze samo za brojanje poruka koje su poslane i koje su primljene.
	 *
	 *
	 * */
	//TODO: za MAC ce trebati posebna vrsta poruke koja ce se slati na pocetku transmisije gdje cvor na kratak i ne dvosmislen nacin opisuje svoje fizicke karatkeristike (razmjestaj senzora, bearing, kutevi, snaga!!!???)
	//TODO: Vidjeti zasto se radi delay tri puta, jednom na transmit a drugi puta na receive na radio objektu kao i u macu na transmit???
	
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
	//public static final long RX_TX_TURNAROUND = 5*Constants.MICRO_SECOND;

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
//	public static final long TX_SIFS = SIFS - RX_TX_TURNAROUND;

	/** Transmit start DIFS. */
	//public static final long TX_DIFS = DIFS - RX_TX_TURNAROUND;

	/**
	 * Extended inter frame space. Wait used by stations to gain access to the
	 * medium after an error.
	 */
	public static final long EIFS = SIFS + DIFS + SYNCHRONIZATION * (Constants.SECOND/JistExperiment.getJistExperiment().getBandwidth()) + 8*MacMessage.Ack.SIZE*Constants.MICRO_SECOND;

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
	private LinkedList<MacMessage> receivedMessages = new LinkedList<MacMessage>();
	private VLCsensor interferedSensor = null;
	
	public enum QueueStrategies
	{
	    Unreliable, Reliable, Concurrent
	}
	private static QueueStrategies QueueStrategy = QueueStrategies.Concurrent;
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
	protected final MacInterface.VlcMacInterface /*MacInterface.Mac802_11*/ self;
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
	public RadioVLC myRadio = null;

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
	public MacVLCV1(MacAddress addr, RadioInfo radioInfo, boolean promisc, RadioVLC vlcradio)
	{
		
	
		myRadio = vlcradio;
		SourceNodeID = addr.hashCode();//BT: store node ID;
		droppedPackets = new HashMap();
		// properties
		bandwidth = radioInfo.getShared().getBandwidth() / 8;

		// proxy
		self = (MacInterface.VlcMacInterface)JistAPI.proxy(this, MacInterface.VlcMacInterface.class);
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
	public MacVLCV1(MacAddress addr, RadioInfo radioInfo, RadioVLC vlcradio)
	{  
		myRadio = vlcradio;
		SourceNodeID = addr.hashCode();//BT: store node ID;
		bandwidth = radioInfo.getShared().getBandwidth() / 8;
		// proxy
		self = (VlcMacInterface)JistAPI.proxy(this, VlcMacInterface.class);
		init(addr, radioInfo, true);// Constants.MAC_PROMISCUOUS_DEFAULT);
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
	public MacInterface/*.Mac802_11*/ getProxy()
	{
		return (MacInterface)this.self;
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


	private LinkedList<MacVLCMessage> MessageQueue = new LinkedList<MacVLCMessage>();
	//private MacVLCMessage CurrentMessageOnAir = null;
	// new ConcurrentLinkedQueue<MacVLCMessage>();  
	//private VLCsensor tmpSensorTx1;
	/**
	 * send message
	 * @param msg
	 * @param destination
	 * @author BorisTomas
	 */
	private void sendMessage(MacVLCMessage msg)
	{
		setMode(MAC_MODE_XBROADCAST);
		long delay = 0; //RX_TX_TURNAROUND; not needed because Tx and Rx are independent 
		long duration = transmitTime(msg);
		
		transmitDelayMultiplier = 1;
		
		radioEntity.transmit(msg, delay, duration);
		JistAPI.sleep(delay+duration);
	}

	private Comparator<MacVLCMessage> comp = new Comparator<MacVLCMessage>() {
		@Override
        public int compare(MacVLCMessage m1, MacVLCMessage m2)
		{
            return (m1.GetPriority() < m2.GetPriority()? 1 : -1);
        }
	};
    private void QueueSort()
    {
    	Collections.sort(MessageQueue, comp);
    }
	private void QueueInsert(MacVLCMessage msg, boolean skipSort)
	{
		
		myRadio.printMessageTransmissionData(msg, 0, "q");
		if(msg.isRetry)
		{
			for (MacVLCMessage item : MessageQueue)
			{
				if(item == msg)
				{
					//poruka vec postoji u queue.
					return;
				}
			}
			msg.DecrementPriority();
		}
		if(!msg.isRetry)
		{
			for (MacVLCMessage item : MessageQueue)
			{
				item.IncrementPriority();
			}
		}
		MessageQueue.add(msg);		
		if(!skipSort)
		{
			QueueSort();
		}
	}
	
	private HashSet<Integer> tmpSensorsTx = new HashSet<Integer>();
	private VLCsensor tmpSensorTx;

	private boolean canSendMessage(MacVLCMessage msg,boolean fixTx, QueueStrategies strategy)
	{
		tmpSensorsTx = new HashSet<Integer>();
		if(!fixTx)
		{
			if(strategy == QueueStrategies.Reliable)
			{
				for (Integer item : msg.getSensorIDTx(myRadio.NodeID)) 
				{
					if(myRadio.GetSensorByID(item).state == SensorStates.Transmitting)
					{
						return false;
					}
				}
			}
			for (Integer item : msg.getSensorIDTx(myRadio.NodeID))//.SensorIDTx) 
			{
				if(myRadio.GetSensorByID(item).state == SensorStates.Idle)
				{
					for (VLCsensor sensor : myRadio.getNearestOpositeSensor(myRadio.GetSensorByID(item))) 
					{
				/*		if(!myRadio.CarrierSense(sensor))
						{
							return false;
						}*/
						if(myRadio.queryControlSignal(sensor, 1))
						{
							return false;
						}
						if(!myRadio.queryControlSignal(sensor, 1))
						{
							if(msg.getDst().equals(MacAddress.ANY))
							{
								//ovdje provjeravam jesu li svi senzori slobodni prije slanja broadcasta. request by mate :)
								if(myRadio.InstalledSensorsTx.size() == msg.getSensorIDTxSize(myRadio.NodeID))
								{
									return true;
								}
								else
								{
									return false;
								}
							}
							return true;
						}
					}
				}
			}
			return false;
		}	
		else
		{
			//fix = true

			for (Integer item : msg.getSensorIDTx(myRadio.NodeID))//.SensorIDTx)
			{
				tmpSensorTx =myRadio.GetSensorByID(item);
				if(tmpSensorTx.state == SensorStates.Idle)
				{
					for (VLCsensor sensor : myRadio.getNearestOpositeSensor(tmpSensorTx)) 
					{
						if(myRadio.CarrierSense(sensor))
						{
							if(!myRadio.queryControlSignal(sensor, 1))
							{
								tmpSensorsTx.add(tmpSensorTx.sensorID);
							}
						}
					}
				}
				else
				{
				}
			}
			msg.setSensorIDTx(tmpSensorsTx, myRadio.NodeID);
			if(msg.getSensorIDTxSize(myRadio.NodeID) != 0)
			{
				return true;
			}
			else
			{
				//System.out.println("neva3 "+ msg.hashCode());
				return false;
			}
		}
	}

	private long tmpDelay;
	private long minDelay;
	private long maxDelay;
	private VLCsensor tmpSensor2;
	/**
	 * Gets end time of transmission of sensor that is transmitting, can get minimum time = earlies ending or 
	 * maximum time =  last transmission.
	 * @param sensors
	 * @param isMin
	 * @return
	 */
	private long getMessageEndTimeForSensors(LinkedList<VLCsensor> sensors, boolean isMin )
	{
		if(sensors.size() == 0)
		{
			return Constants.MICRO_SECOND;//nikada se ne bi trebalo desiti.
		}
		minDelay = Constants.DAY;//jako veliki broj
		maxDelay = 0;
		for (VLCsensor sensor : sensors)
		{
			tmpSensor2 = sensor;//myRadio.getSensorByID(sensor);
			if(tmpSensor2.state == SensorStates.Transmitting)
			{
				tmpDelay = tmpSensor2.Messages.getFirst().getDurationTx(tmpSensor2);//.DurationTx;

				if(isMin)
				{
					if(tmpDelay< minDelay)
					{
						minDelay= tmpDelay;
					}
				}
				else
				{
					if(tmpDelay > maxDelay)
					{
						maxDelay = tmpDelay;
					}
				}
			}
		}

		if(minDelay == Constants.DAY)
		{
			//TODO: pitati matu jel dobro ovako dinamicno mijenjanje vrijeme cekanja na provjeru kanala opet?, ovo bi trebao biti backoff
			transmitDelayMultiplier++;
			minDelay = Constants.MILLI_SECOND*2*transmitDelayMultiplier;
		//	System.out.println("delay" + myRadio.NodeID + " -- "+ minDelay);
		}
		if(maxDelay == 0)
		{
			transmitDelayMultiplier++;
			maxDelay = Constants.MILLI_SECOND*20*transmitDelayMultiplier;
		}
		if(isMin)
		{
		//	minDelay = Constants.MILLI_SECOND;
			return minDelay;
		}
		else
		{		
			return maxDelay;
		}
	}
	private MacVLCMessage tmpMsg;
	private long transmitDelay; 
	private int transmitDelayMultiplier= 1;
	/*
	 * message is null if message is passed from upper layer, it has specific value when it is restored
	 * from timeout. msg is message.getbody().
	 * */
	private void sendMacMessage(Message msg, MacAddress nextHop, MacVLCMessage message)
	{
		if(nextHop == MacAddress.ANY)
		{
			Constants.VLCconstants.broadcasts++;
		}
		MacVLCMessage data = null;
		if(message == null)
		{
		 data = new MacVLCMessage(nextHop, localAddr,0, msg, (byte)0);
		//if(msg.getDst() != MacAddress.ANY && msg.getDst() != MacAddress.LOOP)// || ((NetMessage.Ip)msg).getDst() != NetAddress.ANY)
			if(nextHop != MacAddress.ANY &&nextHop != MacAddress.LOOP)
			{
				((NetMessage.Ip)data.getBody()).Times.add(new TimeEntry(13, "mac send dest", null));
			}
		}
		else
		{
			//doslo je iz timeouta
			data = message;
		}
		if(((NetMessage.Ip)msg).isFresh)
		{
			((NetMessage.Ip)msg).Times.add(new TimeEntry(1, "macbt", null));
			((NetMessage.Ip)msg).isFresh = false;
			
			if (!MessageQueue.isEmpty()) 
			{
				//ako je poruka nova, a red nije prazan odmah ju dodajem u red bez provjera mogucnosti slanja, jer inace red ne bi bio pun.
				((NetMessage.Ip)msg).Times.add(new TimeEntry(12, "macbt", null));
				QueueInsert(data, false);
				if(!TimerRunning)
				{
					TimerRunning = true;
					self.startTimer(transmitDelay, (byte)1);
				}
				return;
			}
		}
		
		switch (QueueStrategy)
		{
			case Reliable :
			{	
				//TODO: v2
				throw new RuntimeException("v2");
			//	break;
			}
			case Unreliable :
			{	
				//TODO: v2
				throw new RuntimeException("v2");
		//		break;
			}
			case Concurrent  :
			{	
				data.setSensorIDTx(GetTransmitSensors(nextHop), myRadio.NodeID);
				
				if(canSendMessage(data, false, QueueStrategy))//mora biti false, inace bi se poruka slala na tx koji su slobodni ali nisu u smjeru primatelja.
				{
					((NetMessage.Ip)msg).Times.add(new TimeEntry(11, "macbt", null));
					sendMessage(data);
				}
				else
				{
					((NetMessage.Ip)msg).Times.add(new TimeEntry(12, "macbt", null));
					QueueInsert(data, false);
					if(!TimerRunning)
					{
						TimerRunning = true;
						transmitDelay = getMessageEndTimeForSensors(myRadio.InstalledSensorsTx, true);//-JistAPI.getTime();
						self.startTimer(transmitDelay, (byte)1);
					}
				}
				break;
			}
		}
	}
	//////////////////////////////////////////////////
	// send-related functions
	//

	// MacInterface interface
	public void send(Message msg, MacAddress nextHop)
	{
		packetNextHop = nextHop;
		sendMacMessage(msg,nextHop, null);
	}
	//////////////////////////////////////////////////
	// timer routines
	//
	// MacInterface
	private boolean TimerRunning = false;
	public void startTimer(long delay, byte mode)
	{		
		JistAPI.sleep(delay);
		self.timeout(timerId);
	}

	// MacInterface

	MacVLCMessage tmpmsg1;
	MacVLCMessage first;
//	private boolean canSendFromQueue = false;
	int queueSize = 0;
	public void timeout(int timerId)
	{
		queueSize = MessageQueue.size();
		
		if(!MessageQueue.isEmpty())
		{			
			first = MessageQueue.peek();
		
			do{
				tmpmsg1 = MessageQueue.poll();
				tmpmsg1.setSensorIDTx(GetTransmitSensors(tmpmsg1.getDst()), myRadio.NodeID);
				
				
				if(canSendMessage(tmpmsg1, true, QueueStrategy))
				{
					sendMacMessage(tmpmsg1.getBody(), tmpmsg1.getDst(), tmpmsg1);
					break;
				}
				else
				{
					tmpmsg1.IncrementPriority();
					QueueInsert(tmpmsg1, true);
				}
			} while(MessageQueue.peek() != first);
			QueueSort();

			if(!MessageQueue.isEmpty())
			{
				transmitDelay = getMessageEndTimeForSensors(myRadio.InstalledSensorsTx, true);//-JistAPI.getTime();
				self.startTimer(transmitDelay, (byte)1);
			}
			else
			{
				TimerRunning = false;
			}
		}
		else
		{
			//red je prazan.
			TimerRunning = false;
			netEntity.pump(netId);//dodao naknadno, testirati
		}
	}

	


	//////////////////////////////////////////////////
	// receive-related functions
	//

	// MacInterface
	public void peek(Message msg)
	{
		//	needEifs = true;
	}

	// MacInterface
	public void receive(Message msg)
	{
		((NetMessage.Ip)(((MacVLCMessage)msg).getBody())).Times.add(new TimeEntry(3, "macbtrec", null));
		
		if(((MacVLCMessage)msg).getDst().addr== myRadio.NodeID 
				&& ((MacVLCMessage)msg).getDst() != MacAddress.ANY 
				&& ((MacVLCMessage)msg).getDst() != MacAddress.LOOP 
				&& ((MacVLCMessage)msg).getDst() != MacAddress.NULL)
		{
			((NetMessage.Ip)(((MacVLCMessage)msg).getBody())).Times.add(new TimeEntry(31, "formenetip", null));  
		}
		receivedMessages.addFirst((MacMessage)msg);
		
		netEntity.receive(((MacVLCMessage)msg).getBody(), ((MacVLCMessage)msg).getSrc(), netId, false);
	}

	private float receivedMessagesEndTime = 0;
	private float receivedMessageAge= 0;
	private long maxReceivedAge = 5000000;//nanoseconds = 5ms
	private int ageCounter= 0;
	private LinkedList<VLCsensor> GetTransmitSensors(MacAddress newdest)
	{
		if(newdest == MacAddress.ANY)
		{
			return myRadio.InstalledSensorsTx;
		}
		if(receivedMessages.size() !=0)
		{
			receivedMessagesEndTime = 0;
			ageCounter = 0;
			//poruke se listaju od namlaðih prema najstarijima
			for (int i = 0; i < receivedMessages.size(); i++) 
			{
				if(receivedMessages.get(i).getSrc() == newdest)
				{
					for (Integer item : receivedMessages.get(i).getSensorIDRx(myRadio.NodeID)) 
					{
						receivedMessagesEndTime = receivedMessages.get(i).getEndRx(myRadio.GetSensorByID(item));
						break;
					}					
					receivedMessageAge = (JistAPI.getTime()  - receivedMessagesEndTime);
					if( receivedMessageAge < maxReceivedAge  )
					{
						return myRadio.getNearestOpositeSensor(receivedMessages.get(i).getSensorIDRx(myRadio.NodeID));
					}
					else
					{
						return myRadio.InstalledSensorsTx;
					}
				}
			}
		}

		return myRadio.InstalledSensorsTx;
	}
	
	

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
		//	idle();
			break;
		case MAC_MODE_DIFS:
		case MAC_MODE_SNAV:
		//	idle();
			break;
		case MAC_MODE_SIDLE:
		case MAC_MODE_SWFCTS:
		case MAC_MODE_SWFDATA:
		case MAC_MODE_SWFACK:    
		case MAC_MODE_XBROADCAST:
		case MAC_MODE_XUNICAST:
			// T-ODO drc: dunno if the following ones should be here...
		case MAC_MODE_XACK:
			// don't care
			break;
		default:
			// rimtodo: really unexpected?
			System.err.println("unexpected mode: "+getModeString(mode));
		}
	}

	private void radioIdle()
	{
		switch(mode)
		{
		case MAC_MODE_SIDLE:
			// T-ODO drc: are these in the right place
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
			System.err.println("unexpected mode: "+getModeString(mode));
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
	

	MacVLCMessage tmpmsg;
	public void notifyInterference(MacMessage msg, VLCsensor sensors) 
	{
		((NetMessage.Ip)(((MacVLCMessage)msg).getBody())).Times.add(new TimeEntry(90, "macinterference", null));		
		System.out.println("interference on node: " + sensors.node.NodeID +" sensor: " + sensors.sensorID + " msg hsh: "+ msg.hashCode());
		interferedSensor = sensors;
		for (VLCsensor sensor : myRadio.getNearestOpositeSensor(sensors)) 
		{
			tmpmsg =sensors.Messages.getFirst();
			if(tmpmsg.getEndTx(sensors) <= JistAPI.getTime()) 
			{
				//znaci da se poruka jos salje
				tmpmsg.isRetry = true;
				QueueInsert(tmpmsg, false);
			}
		}
	}

	public void notifyError(int errorCode, String message) 
	{
	//	((NetMessage.Ip)(((MacVLCMessage)msg).getBody())).Times.add(new TimeEntry(91, "macinterference", null));
		System.out.println("notfiy error #"+errorCode);
	}

	public void notifyTransmitFail(Message msg, int errorCode) 
	{
		netEntity.packetDropped(msg, packetNextHop);
		((NetMessage.Ip)(((MacVLCMessage)msg).getBody())).Times.add(new TimeEntry(92, "macinterference", null));
		System.out.println("transmit fail error #"+errorCode);
	}

	public void notifyReceiveFail(Message msg, int errorCode) 
	{
		//receive fail se desi odmah na pocetku primanja dok interfere na kraju, ne mora znaciti da ce svaki receive fail
		//rezultirati sa interference, jer moze biti da poruka ipak bude primljena ako je rx pwr dovoljno veliki
		
		((NetMessage.Ip)(((MacVLCMessage)msg).getBody())).Times.add(new TimeEntry(93, "macinterference", null));
		System.out.println("recFail #"+errorCode +" n: "+myRadio.NodeID+ " s: "+((MacVLCMessage)msg).getSrc()+" d: "+((MacVLCMessage)msg).getDst()+" msg hsh: "+msg.hashCode()); 
	}
}

