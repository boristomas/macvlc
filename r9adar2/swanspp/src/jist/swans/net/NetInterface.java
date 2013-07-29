//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <NetInterface.java Tue 2004/04/06 11:32:40 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.net;

import jist.swans.mac.MacAddress;
import jist.swans.mac.MacInterface;
import jist.swans.misc.Message;
import jist.swans.route.RouteInterface;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;

/**
 * Defines the interface of all Network layer entity implementations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: NetInterface.java,v 1.1 2007/04/09 18:49:18 drchoffnes Exp $
 * @since SWANS1.0
 */

public interface NetInterface extends JistAPI.Proxiable
{
	//---------------------------------------------------------------------
	/*
	Mapper getProtocolMapper() throws Continuation;

	NetHandler getProtocolHandler(int protocolId) throws Continuation;
*/
	
	//---------hookup---------
	/** @author tariavo
	 * hook up with upper layer
	 * (conventionally called Transport, that implements 
	 * {@link jist.swans.net.NetInterface.NetHandler NetHandler} interface)
	 */
	void setProtocolHandler(int protocolId, NetHandler netHandler);
	/**@author tariavo
	 * hookup with lower layer(MAC)
	 * @param macEntity
	 * @param q
	 * @return macId
	 * @throws Continuation;
	 */
	byte addMacInterface(MacInterface macEntity, MessageQueue q) throws 
			Continuation;
	/**@author tariavo
	 * hookup with Route layer
	 * @param routeInterfaceEntity
	 */
	void setRouting(RouteInterface routeInterfaceEntity);

	//---------------------------------------------------------------------

  /**
   * Return local network address.
   *
   * @return local network address
   * @throws JistAPI.Continuation never (blocking event)
   */
  NetAddress getAddress() throws JistAPI.Continuation;

  /**
   * Receive a message from the link layer.
   *
   * @param msg incoming network packet
   * @param lastHop link-level source of incoming packet
   * @param macId incoming interface
   * @param promiscuous whether network interface is in promisc. mode
   */
  void receive(Message msg, MacAddress lastHop, byte macId, boolean promiscuous);

  /**
   * Route, if necessary, and send a message (from TRANSPORT).
   *
   * @param msg packet payload (usually from transport or routing layers)
   * @param dst packet destination address
   * @param protocol packet protocol identifier
   * @param priority packet priority
   * @param ttl packet time-to-live value
   */
  void send(Message msg, NetAddress dst, 
      short protocol, byte priority, byte ttl);

  /**
   * Send a message along given interface (usually from ROUTING).
   *
   * @param msg packet (usually from routing layer)
   * @param interfaceId interface along which to send packet
   * @param nextHop packet next hop address
   */
  void send(NetMessage.Ip msg, int interfaceId, MacAddress nextHop);

  /**
   * Request next packet to send, if one exists; indicate that interface has
   * completed processing previous request.
   *
   * @param netid interface identifier
   */
  void pump(int netid);
  
  /**
   * Returns the message queue for the specified interface. This allows 
   * the routing layer to modify packets waiting to be sent.
   * @param interfaceId the interface to which the queue belongs
   * @return the message queue for the specified interface
   * @throws JistAPI.Continuation never (blocking event)
   */
  MessageQueue getMessageQueue(int interfaceId) throws JistAPI.Continuation;

  /**
   * Network layer callback interface.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @version $Id: NetInterface.java,v 1.1 2007/04/09 18:49:18 drchoffnes Exp $
   * @since SWANS1.0
   */
  public static interface NetHandler
  {	
	  	/**
		 * for up hookup with the upper transport layer
		 * @return protocol identifier
		 * @throws Continuation never; marker for rewriter
		 */
		public int getProtocolId() throws Continuation;
    /**
     * Receive a message from network layer.
     *
     * @param msg message received
     * @param src source network address
     * @param lastHop source link address
     * @param macId incoming interface
     * @param dst destination network address
     * @param priority packet priority
     * @param ttl packet time-to-live
     */
    void receive(Message msg, NetAddress src, MacAddress lastHop, 
        byte macId, NetAddress dst, byte priority, byte ttl);

  } // interface: NetHandler

/**
 * Callback from MAC layer indicating the packet was dropped.
 * @param packet
 * @param packetNextHop
 */
  void packetDropped(Message packet, MacAddress packetNextHop);

} // interface NetInterface

