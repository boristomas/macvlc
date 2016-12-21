//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <NetIp.java Tue 2004/04/20 10:12:52 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.net;

import java.util.TreeMap;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.mac.Mac802_11;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacInterface;
import jist.swans.mac.MacLoop;
import jist.swans.misc.Mapper;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.radio.TimeEntry;
import jist.swans.route.RouteInterface;

import org.apache.log4j.Logger;

import com.sun.corba.se.impl.orbutil.closure.Constant;

/**
 * IPv4 implementation based on RFC 791. Performs protocol
 * multiplexing, and prioritized packet queuing, but no
 * RED or packet fragmentation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: NetIp.java,v 1.1 2007/04/09 18:49:18 drchoffnes Exp $
 * @since SWANS1.0
 */

public class NetIp implements NetInterface
{
	/**
	 * IP logger.
	 */
	public static final Logger log = Logger.getLogger(NetIp.class.getName());

	/**
	 *
	 * MacStats collects stats such as latency, packet loss and general overhead.
	 * @author  David Choffnes
	 */
	public static class IpStats {

		/** data structure to track which sent messages were received */
		public TreeMap packets;



		/** Creates a new instance of MacStats */
		public IpStats() 
		{
			packets = new TreeMap();
		}


		/**
		 * add packet to list of undelivered packets
		 * @param msg The mesasge entity that was sent
		 */
		@SuppressWarnings("unchecked")
		public void addPacket(NetMessage.Ip msg, MacAddress nextHop)
		{
			packets.put(new Integer(msg.hashCode()), nextHop);
		}

		/**
		 * remove packet from list of undelivered packets
		 *
		 * caller should make sure that these are instance of Ip messages
		 * and that their payloads are Udp messages, the kind we're intersted in tracking
		 *
		 * @param msg The message entity that was sent.
		 */
		public void removePacket(NetMessage.Ip msg, NetAddress localAddr)
		{
			MacAddress ma=null;
			Object o =null;

			if (packets.containsKey(new Integer(msg.hashCode())))
				o = packets.get(new Integer(msg.hashCode()));
			if (o!=null)
				ma = (MacAddress)o;


			if (ma!=null)
			{
				if (ma.toString().compareTo(localAddr.toString())==0)
				{
					packets.remove(msg);
				}

			}
		}

	}


	/**
	 * Information about each network interface.
	 */
	public static class NicInfo
	{
		/** mac entity. */
		public MacInterface mac;
		/** outgoing packet queue. */
		public MessageQueue q;
		/** whether link layer is busy. */
		public boolean busy;
	}

	//////////////////////////////////////////////////
	// constants
	//

	/**
	 * Packet fragmentation threshold.
	 */
	public static final int THRESHOLD_FRAGMENT = 2048;

	/**
	 * Maximum packet queue length.
	 */
	public static final int DEFAULT_QUEUE_LENGTH = 100;
	/**
	 * Maximum packet queue length. Default is 100 in linux
	 */
	public static final int MAX_QUEUE_LENGTH = 100;

	//////////////////////////////////////////////////
	// locals
	//

	// entity hookup
	/** self-referencing proxy entity. */
	protected NetInterface self;

	/** local network address. */
	protected NetAddress localAddr;

	/** routing protocol. */
	protected RouteInterface routing;

	/** protocol number mapping. */
	protected Mapper protocolMap;

	/** protocol handlers. */
	protected NetHandler[] protocolHandlers;

	/** network interfaces. */
	protected NicInfo[] nics;

	/** packet loss models. */
	protected PacketLoss incomingLoss, outgoingLoss;

	/** the stats for the IP layer */
	protected IpStats stats;

	//////////////////////////////////////////////////
	// initialization 
	//

	/**
	 * Initialize IP implementation with given address and protocol mapping.
	 *
	 * @param addr local network address
	 * @param protocolMap protocol number mapping
	 * @param in incoming packet loss model
	 * @param out outgoing packet loss model
	 */
	public NetIp(NetAddress addr, Mapper protocolMap, PacketLoss in, PacketLoss out)
	{
		// proxy entity
		this.self = (NetInterface)JistAPI.proxy(this, NetInterface.class);
		// local address
		setAddress(addr);
		// protocol number mapping
		this.protocolMap = protocolMap;
		// protocol handlers
		this.protocolHandlers = new NetHandler[protocolMap.getLimit()];
		// network interfaces
		this.nics = new NicInfo[0];
		// packet loss
		this.incomingLoss = in;
		this.outgoingLoss = out;
		// add loopback mac:
		//   therefore, loopback = 0, Constants.NET_INTERFACE_LOOPBACK
		//              next     = 1, Constants.NET_INTERFACE_DEFAULT
		MacLoop loopback = new MacLoop(); 
		byte netid = addInterface(loopback.getProxy());
		if(Main.ASSERT) Util.assertion(netid==Constants.NET_INTERFACE_LOOPBACK);
		loopback.setNetEntity(getProxy(), netid);
	}

	/**
	 * Initialize IP implementation with given address and protocol mapping.
	 *
	 * @param addr local network address
	 * @param protocolMap protocol number mapping
	 * @param in incoming packet loss model
	 * @param out outgoing packet loss model
	 */
	public NetIp(NetAddress addr, Mapper protocolMap, PacketLoss in, PacketLoss out, IpStats stats)
	{
		// proxy entity
		this.self = (NetInterface)JistAPI.proxy(this, NetInterface.class);
		// local address
		setAddress(addr);
		// protocol number mapping
		this.protocolMap = protocolMap;
		// protocol handlers
		this.protocolHandlers = new NetHandler[protocolMap.getLimit()];
		// network interfaces
		this.nics = new NicInfo[0];
		// packet loss
		this.incomingLoss = in;
		this.outgoingLoss = out;
		this.stats = stats;
		// add loopback mac:
		//   therefore, loopback = 0, Constants.NET_INTERFACE_LOOPBACK
		//              next     = 1, Constants.NET_INTERFACE_DEFAULT
		MacLoop loopback = new MacLoop();
		byte netid = addInterface(loopback.getProxy());
		if(Main.ASSERT) Util.assertion(netid==Constants.NET_INTERFACE_LOOPBACK);
		loopback.setNetEntity(getProxy(), netid);

	}

	//////////////////////////////////////////////////
	// entity hookup
	//

	/**
	 * Return self-referencing proxy entity.
	 *
	 * @return self-referencing proxy entity
	 */
	public NetInterface getProxy()
	{
		return this.self;
	}

	//////////////////////////////////////////////////
	// address
	//

	/**
	 * Set local network address.
	 *
	 * @param addr local network address
	 */
	public void setAddress(NetAddress addr)
	{
		if(Main.ASSERT) Util.assertion(addr!=null);
		this.localAddr = addr;
	}

	/**
	 * Whether packet is for local consumption.
	 *
	 * @param msg packet to inspect
	 * @return whether packet is for local consumption
	 */
	private boolean isForMe(NetMessage.Ip msg)
	{
		NetAddress addr = msg.getDst();
		return NetAddress.ANY.equals(addr)
				|| NetAddress.LOCAL.equals(addr)
				|| localAddr.equals(addr);
	}

	//////////////////////////////////////////////////
	// routing, protocols, interfaces
	//

	/**
	 * Set routing implementation.
	 *
	 * @param routingEntity routing entity
	 */
	public void setRouting(RouteInterface routingEntity)
	{
		if(!JistAPI.isEntity(routingEntity)) throw new IllegalArgumentException("expected entity");
		this.routing = routingEntity;
	}

	/**
	 * Add network interface with default queue.
	 *
	 * @param macEntity link layer entity
	 * @return network interface identifier
	 */
	public byte addInterface(MacInterface macEntity)
	{
		return addInterface(macEntity, 
				new MessageQueue.NoDropMessageQueue(
						Constants.NET_PRIORITY_NUM, MAX_QUEUE_LENGTH));
	}

	/**
	 * Add network interface.
	 *
	 * @param macEntity link layer entity
	 * @return network interface identifier
	 */
	public byte addInterface(MacInterface macEntity, MessageQueue q)
	{
		//  if(!JistAPI.isEntity(macEntity)) throw new IllegalArgumentException("expected entity");
		// create new nicinfo
		NicInfo ni = new NicInfo();
		ni.mac = macEntity;
		ni.q = q;
		ni.busy = false;
		// store
		NicInfo[] nics2 = new NicInfo[nics.length+1];
		System.arraycopy(nics, 0, nics2, 0, nics.length);
		nics2[nics.length] = ni;
		nics = nics2;
		// return interface id
		return (byte)(nics.length-1);
	}

	/**
	 * Set network protocol handler.
	 *
	 * @param protocolId protocol identifier
	 * @param handler protocol handler
	 */
	public void setProtocolHandler(int protocolId, NetHandler handler)
	{
		if(protocolId < 0) throw new RuntimeException("protocolId must be >= 0");
		protocolHandlers[protocolMap.getMap(protocolId)] = handler;
	}

	/**
	 * Return network protocol handler.
	 *
	 * @param protocolId protocol identifier
	 * @return procotol handler
	 */
	private NetHandler getProtocolHandler(int protocolId)
	{
		return protocolHandlers[protocolMap.getMap(protocolId)];
	}

	//////////////////////////////////////////////////
	// NetInterface implementation
	//

	/** {@inheritDoc} */
	public NetAddress getAddress() throws JistAPI.Continuation
	{
		return localAddr;
	}

	/** {@inheritDoc} */
	public void receive(Message msg, MacAddress lastHop, byte macId, boolean promisc)
	{
		if(msg==null) throw new NullPointerException();
		NetMessage.Ip ipmsg = (NetMessage.Ip)msg;
		ipmsg.Times.add(new TimeEntry(4, "netiprec", null));

		if(incomingLoss.shouldDrop(ipmsg)) return;
		ipmsg.Times.add(new TimeEntry(41, "netiprec2", null));
		if(log.isInfoEnabled())
		{
			log.info("receive t="+JistAPI.getTime()+" from="+lastHop+" on="+macId+" data="+msg);
		}

		if(routing!=null) routing.peek(ipmsg, lastHop);
		
		if(localAddr.equals(ipmsg.getDst()))
		{
			ipmsg.Times.add(new TimeEntry(5, "formenetip", null));
		}
		if(!promisc)
		{
			
			if(isForMe(ipmsg))
			{       
				ipmsg.Times.add(new TimeEntry(6, "formenetip", null));
			
				JistAPI.sleep(Constants.NET_DELAY);
				getProtocolHandler(ipmsg.getProtocol()).receive(ipmsg.getPayload(), 
						ipmsg.getSrc(), lastHop, macId, ipmsg.getDst(), 
						ipmsg.getPriority(), (byte)ipmsg.getTTL());
			}
			else
			{
				if(ipmsg.getTTL()>0)
				{
					if(ipmsg.isFrozen()) ipmsg = ipmsg.copy();
					ipmsg.decTTL();
					sendIp(ipmsg);
				}
			}
		}
	}

	/** {@inheritDoc} */
	public void send(Message msg, NetAddress dst, 
			short protocol, byte priority, byte ttl) 
	{

		sendIp(new NetMessage.Ip(msg, localAddr, dst, 
				protocol, priority, ttl));
	}

	/** {@inheritDoc} */
	public void send(NetMessage.Ip msg, int interfaceId, MacAddress nextHop) 
	{
		if(msg==null) throw new NullPointerException();
		if(outgoingLoss.shouldDrop(msg)) return;
		/*
    if(msg.getSize()>THRESHOLD_FRAGMENT)
    {
      throw new RuntimeException("ip fragmentation not implemented");
    }
		 */
		if(log.isDebugEnabled())
		{
			log.debug("queue t="+JistAPI.getTime()+" to="+nextHop+" on="+interfaceId+" data="+msg);
		}
		NicInfo ni = nics[interfaceId];
		//Constants.VLCconstants.NetIPSent++;
		if (ni.q.isFull())
		{
			// T-ODO call a separate function -- this should not be confused with 
			// a lost link
			packetDropped(msg, nextHop); // inform of packets being dropped from queue
		}

		if (ni.q.isEmpty()){
			if (ni.busy)
				ni.busy = false;
		}

		ni.q.insert(new QueuedMessage(msg, nextHop), msg.getPriority());

		if(!ni.busy)
			pump(interfaceId);
	}

	//////////////////////////////////////////////////
	// send/receive
	//

	/**
	 * Send an IP packet. Knows how to broadcast, to deal
	 * with loopback. Will call routing for all other destinations.
	 *
	 * @param msg ip packet
	 */
	private void sendIp(NetMessage.Ip msg) 
	{
		
		if (NetAddress.ANY.equals(msg.getDst()))
		{
			// broadcast
			send(msg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
		}
		else if(NetAddress.LOCAL.equals(msg.getDst()) || localAddr.equals(msg.getDst()))
		{
			// loopback
			send(msg, Constants.NET_INTERFACE_LOOPBACK, MacAddress.LOOP);
		}
		else if (msg.getNextHop()==null || msg.getNextHop().equals(localAddr))
		{
			// route and send
			
			routing.send(msg);
		}
	}

	//////////////////////////////////////////////////
	// send pump
	//

	/** {@inheritDoc} */
	public void pump(int interfaceId)
	{
		NicInfo ni = nics[interfaceId];
		if(ni.q.isEmpty())
		{
			ni.busy = false;
		}
		else
		{
			ni.busy = true;
			QueuedMessage qmsg = ni.q.remove();
			NetMessage.Ip ip = (NetMessage.Ip)qmsg.getPayload();
			ip = ip.freeze(); // immutable once packet leaves node
			if(log.isInfoEnabled())
			{
				log.info("send t="+JistAPI.getTime()+" to="+qmsg.getNextHop()+" data="+ip);
			}
			JistAPI.sleep(Constants.NET_DELAY);
			
			if(qmsg.getNextHop() !=  MacAddress.ANY && qmsg.getNextHop() != MacAddress.NULL && qmsg.getNextHop() != MacAddress.LOOP &&  !localAddr.equals(ip.getDst()) )
			{
				ip.Times.add(new TimeEntry(14, "mac send dest", null));
			}
			//T-ODO dont understatnd it with javadocs(notifying)
			ni.mac.send(ip, qmsg.getNextHop());
		}
	}

	//////////////////////////////////////////////////
	// display
	//

	/** {@inheritDoc} */
	public String toString()
	{
		return "ip:"+localAddr;
	}


	///////////////////////////////////////////////////
	// access to message queue for routing layer
	//
	public MessageQueue getMessageQueue(int interfaceId)  throws JistAPI.Continuation
	{
		NicInfo ni = nics[interfaceId];
		return ni.q;
	}

	/* (non-Javadoc)
	 * @see jist.swans.net.NetInterface#packetDropped(jist.swans.misc.Message, jist.swans.mac.MacAddress)
	 */
	public void packetDropped(Message packet, MacAddress packetNextHop) {
		// anything to do here?
		//Constants.VLCconstants.NetIPDropped++;
		routing.packetDropped(packet, packetNextHop);    
	}

	/**
	 * {@inheritDoc}
	 */
	public byte addMacInterface(MacInterface macEntity, MessageQueue q) throws Continuation {
		return addInterface(macEntity, q);
	}

}

