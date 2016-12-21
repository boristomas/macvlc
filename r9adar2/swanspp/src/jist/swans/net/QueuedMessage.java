//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <QueuedMessage.java Tue 2004/04/06 11:32:56 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.net;

import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;

/**
 * A message object that can be queued in <code>MessageQueue</code>.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: QueuedMessage.java,v 1.1 2007/04/09 18:49:19 drchoffnes Exp $
 * @since SWANS1.0
 */

public class QueuedMessage implements Message
{
	public String getMessageID()
	  {
		  return "not implemented";
	  }
	
  //////////////////////////////////////////////////
  // locals
  //

  /**
   * Queued message payload.
   */
  private Message payload;

  /**
   * Next hop that message should traverse.
   */
  private MacAddress nextHop;

  /**
   * Pointer to next queued message.
   */
  public QueuedMessage next;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new queued message.
   *
   * @param payload actual message being queued
   * @param nextHop nextHop of message
   */
  public QueuedMessage(Message payload, MacAddress nextHop)
  {
    this.payload = payload;
    this.nextHop = nextHop;
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Return payload.
   *
   * @return payload
   */
  public Message getPayload()
  {
    return payload;
  }

  /**
   * Return next link hop.
   *
   * @return next link hop
   */
  public MacAddress getNextHop()
  {
    return nextHop;
  }

  //////////////////////////////////////////////////
  // message interface
  //

  // Message interface
  /** {@inheritDoc} */
  public int getSize()
  {
    return payload.getSize();
  }

  // Message interface
  /** {@inheritDoc} */
  public void getBytes(byte[] msg, int offset)
  {
    payload.getBytes(msg, offset);
  }

} // class: QueuedMessage

