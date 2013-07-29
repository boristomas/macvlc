//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <MessageQueue.java Tue 2004/04/06 11:32:34 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.net;

import jist.runtime.JistAPI;

/**
 * Implements a prioritized queue of items. Items are dequeued in order of
 * their priority, where a lower value priority comes first in the queue.
 * Priority values should be in the range [0, priorities), where
 * <code>priorities</code> is the queue initialization parameter.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MessageQueue.java,v 1.1 2007/04/09 18:49:18 drchoffnes Exp $
 * @since SWANS1.0
 */

public interface MessageQueue
{

  /**
   * Return whether queue is empty.
   *
   * @return whether queue is empty
   */
  public boolean isEmpty();

  /**
   * Return whether the queue is filled to capacity.
   *
   * @return whether queue is filled to capacity
   */
  public boolean isFull();

  /**
   * Return number of items in the queue.
   *
   * @return number of items in the queue
   */
  public int size();

  /**
   * Insert message into queue at end (with lowest priority).
   *
   * @param msg message to insert
   */
  public void insert(QueuedMessage msg);

  /**
   * Return first message, but do not dequeue.
   *
   * @return first message (not dequeued)
   */
  public QueuedMessage get();

  /**
   * Return first message and dequeue.
   *
   * @return first message in queue
   */
  public QueuedMessage remove();

  //////////////////////////////////////////////////
  // priority
  //

  /**
   * Insert message into queue with given priority.
   *
   * @param msg message to insert
   * @param pri message priority
   */
  public void insert(QueuedMessage msg, int pri);

  /**
   * Return priority of first queued message.
   *
   * @return priority of first queued message
   */
  public int getPri();

  /**
   * Return first message of given priority, but do not dequeue.
   *
   * @param pri priority of message requested
   * @return message of given priority (not dequeued), or null if
   *   no such message exists
   */
  public QueuedMessage get(int pri);

  /**
   * Return first message with given priority and dequeue.
   *
   * @param pri priority of message requested
   * @return message of given priority
   */
  public QueuedMessage remove(int pri);

  //////////////////////////////////////////////////
  // IMPLEMENTATIONS
  //

  //////////////////////////////////////////////////
  // NoDropMessage
  //

  public class NoDropMessageQueue implements MessageQueue
  {
    /**
     * Heads of message queues for different priorities.
     */
    private QueuedMessage[] heads; 

    /**
     * Tails of message queues for different priorities.
     */
    private QueuedMessage[] tails;

    /**
     * Index of highest priority.
     */
    private byte topPri;

    /**
     * Length of list.
     */
    private byte size;

    /**
     * List size limit.
     */
    private int capacity;

    /**
     * Initialize prioritized message queue.
     *
     * @param priorities number of priority levels
     * @param capacity maximum number of items allowed in list
     */
    public NoDropMessageQueue(byte priorities, int capacity)
    {
      heads = new QueuedMessage[priorities];
      tails = new QueuedMessage[priorities];
      topPri = (byte)heads.length;
      size = 0;
      this.capacity = capacity;
    }

    /**
     * Return whether list is empty.
     *
     * @return whether list is empty
     */
    public boolean isEmpty()
    {
      return size==0;
    }

    /**
     * Return whether the list is filled to capacity.
     *
     * @return whether list is filled to capacity
     */
    public boolean isFull()
    {
      return size==capacity;
    }

    /**
     * Return number of items in the list.
     *
     * @return number of items in the list
     */
    public int size()
    {
      return size;
    }

    /**
     * Insert message into queue with given priority.
     *
     * @param msg message to insert
     * @param pri message priority
     */
    public void insert(QueuedMessage msg, int pri)
    {
      if(size==capacity)
      {
        throw new IndexOutOfBoundsException("list maximum exceeded");
      }
      size++;
      topPri = (byte)Math.min(pri, topPri);
      QueuedMessage tail = tails[pri];
      if(tail==null)
      {
        heads[pri] = msg;
        tails[pri] = msg;
      }
      else
      {
        tail.next = msg;
        tails[pri] = msg;
      }
    }

    /**
     * Insert message into queue at end (with lowest priority).
     *
     * @param msg message to insert
     */
    public void insert(QueuedMessage msg)
    {
      insert(msg, heads.length-1);
    }

    /**
     * Return priority of first queued message.
     *
     * @return priority of first queued message
     */
    public int getPri()
    {
      while(heads[topPri]==null) topPri++;
      return topPri;
    }

    /**
     * Return first message of given priority, but do not dequeue.
     *
     * @param pri priority of message requested
     * @return message of given priority (not dequeued), or null if
     *   no such message exists
     */
    public QueuedMessage get(int pri)
    {
      return heads[pri];
    }

    /**
     * Return first message, but do not dequeue.
     *
     * @return first message (not dequeued)
     */
    public QueuedMessage get()
    {
      return heads[getPri()];
    }

    /**
     * Return first message with given priority and dequeue.
     *
     * @param pri priority of message requested
     * @return message of given priority
     */
    public QueuedMessage remove(int pri)
    {
      QueuedMessage msg = heads[pri];
      heads[pri] = msg.next;
      if(msg.next==null)
      {
        tails[pri] = null;
      }
      else
      {
        msg.next = null;
      }
      size--;
      return msg;
    }

    /**
     * Return first message and dequeue.
     *
     * @return first message in queue
     */
    public QueuedMessage remove()
    {
      return remove(getPri());
    }

  }
  
  //////////////////////////////////////////////////
  // DropMessage
  //

  public class DropMessageQueue implements MessageQueue
  {
    /** toggles debug output */  
    private static boolean DEBUG_MODE = true;
      
    /**
     * Heads of message queues for different priorities.
     */
    private QueuedMessage[] heads; 

    /**
     * Tails of message queues for different priorities.
     */
    private QueuedMessage[] tails;

    /**
     * Index of highest priority.
     */
    private byte topPri;

    /**
     * Length of list.
     */
    private byte size;

    /**
     * List size limit.
     */
    private int capacity;
    
    /**
     * Initialize prioritized message queue.
     *
     * @param priorities number of priority levels
     * @param capacity maximum number of items allowed in list
     */
    public DropMessageQueue(byte priorities, int capacity)
    {
      init(priorities, capacity);
    }
    
    
    public void init(byte priorities, int capacity)
    {
        heads = new QueuedMessage[priorities];
        tails = new QueuedMessage[priorities];
        topPri = (byte)heads.length;
        size = 0;
        this.capacity = capacity;
    }

  /////////////////////////////////////////////
  // DEBUG CODE
  //

  /**
   * Println given string with JiST time and local net address, if debug mode on.
   * 
   * @param s string to print
   */  
  private void printlnDebug(String s)
  {
    if (DEBUG_MODE)
    {
      System.out.println(JistAPI.getTime()+"\t: "+s);
    }
  }
    
    /**
     * Return whether list is empty.
     *
     * @return whether list is empty
     */
    public boolean isEmpty()
    {
      return size==0;
    }

    /**
     * Return whether the list is filled to capacity.
     *
     * @return whether list is filled to capacity
     */
    public boolean isFull()
    {
      return size==capacity;
    }

    /**
     * Return number of items in the list.
     *
     * @return number of items in the list
     */
    public int size()
    {
        assert(size>=0);
      return size;
    }

    /**
     * Insert message into queue with given priority.
     *
     * @param msg message to insert
     * @param pri message priority
     */
    public void insert(QueuedMessage msg, int pri)
    {
      if(size==capacity)
      {
      	/** now prints conditionally */
      	printlnDebug("Dropped a message going to " + msg.getNextHop() +
      			": " + msg.getPayload().toString());
      	
//      	if (((NetMessage.Ip)msg.getPayload()).getPayload() instanceof TransUdp.UdpMessage)
//      	stats.updatePacketSeen(((NetMessage.Ip)msg.getPayload()).getPayload(), 
//      	        stats.DROP_QUEUE);
        return;
      }
      size++;
      topPri = (byte)Math.min(pri, topPri);
      QueuedMessage tail = tails[pri];
      if(tail==null)
      {
        heads[pri] = msg;
        tails[pri] = msg;
      }
      else
      {
        tail.next = msg;
        tails[pri] = msg;
      }
    }

    /**
     * Insert message into queue at end (with lowest priority).
     *
     * @param msg message to insert
     */
    public void insert(QueuedMessage msg)
    {
      insert(msg, heads.length-1);
    }

    /**
     * Return priority of first queued message.
     *
     * @return priority of first queued message
     */
    public int getPri()
    {
      while(heads[topPri]==null) topPri++;
      return topPri;
    }

    /**
     * Return first message of given priority, but do not dequeue.
     *
     * @param pri priority of message requested
     * @return message of given priority (not dequeued), or null if
     *   no such message exists
     */
    public QueuedMessage get(int pri)
    {
      return heads[pri];
    }

    /**
     * Return first message, but do not dequeue.
     *
     * @return first message (not dequeued)
     */
    public QueuedMessage get()
    {
      return heads[getPri()];
    }

    /**
     * Return first message with given priority and dequeue.
     *
     * @param pri priority of message requested
     * @return message of given priority
     */
    public QueuedMessage remove(int pri)
    {
      QueuedMessage msg = heads[pri];
      heads[pri] = msg.next;
      if(msg.next==null)
      {
        tails[pri] = null;
      }
      else
      {
        msg.next = null;
      }
      size--;
      assert(size>=0);
      return msg;
    }

    /**
     * Return first message and dequeue.
     *
     * @return first message in queue
     */
    public QueuedMessage remove()
    {
      return remove(getPri());
    }

  } // end DropMessageQueue

} // class: MessageQueue
