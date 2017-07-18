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
 
public  class MacVLCMessage extends MacMessage
{
 
    //TODO: vidjeti treba li trackati i start i end time u poruci za pojedine cvorove
    //Ne treba jer je cvor id vidljiv iz objekta klase senzor.
 
	public long CreateTime = 0;
    public String getMessageID()
    {
        return ID;
    }
    private boolean wasInQueue = false;
    
    public void SetWasInQueue(boolean value)
    {
    	wasInQueue = value;
    }
    public boolean GetWasInQueue()
    {
    	return wasInQueue;
    }
 
    private byte priority;
    /**
     * Sets message priority
     * @param pri
     */
    public void SetPriority(byte pri)
    {
        //max priority = 15.
        if(pri > 15)
        {
            pri = 15;
        }
        this.priority = pri;
    }
    /**
     * Gets message priority
     * @return
     */
    public byte GetPriority()
    {
        return this.priority;
    }
    /**
     * Increments priority by one but not more than 14, 14 and 15 are reserved for emergency services.
     * 
     */
    public void IncrementPriority() 
    {
        if(this.priority < 13)
        {
            this.priority++;
        }       
    }   
 
    public void DecrementPriority() 
    {
        if(this.priority >= 1)
        {
            this.priority--;
        }       
    }
    
 
    //element control
    private java.util.concurrent.ConcurrentHashMap<Integer, HashSet<Integer>> ElementIDTx = new java.util.concurrent.ConcurrentHashMap<Integer, HashSet<Integer>>();
 
    private java.util.concurrent.ConcurrentHashMap<Integer, HashSet<Integer>> ElementIDRx = new java.util.concurrent.ConcurrentHashMap<Integer, HashSet<Integer>>();
    //public HashSet<Integer> SensorIDRx = new HashSet<Integer>();
 
    private HashMap<VLCelement, Long> StartRx = new HashMap<VLCelement, Long>();
    public long getStartRx(VLCelement element)
    {
        if(!StartRx.containsKey(element))
        {
            StartRx.put(element, (long)0);
        }
        return StartRx.get(element);
    }
    public void setStartRx(VLCelement element, long value)
    {
        StartRx.put(element, value);
    }
 
    private HashMap<VLCelement, Long> EndRx = new HashMap<VLCelement, Long>();
    public long getEndRx(VLCelement element)
    {
        if(!EndRx.containsKey(element))
        {
            EndRx.put(element, (long)0);
        }
        return EndRx.get(element);
    }
    public void setEndRx(VLCelement element, long value)
    {
        EndRx.put(element, value);
    }
    private HashMap<VLCelement, Long> DurationRx = new HashMap<VLCelement, Long>();
    public long getDurationRx(VLCelement element)
    {
        if(!DurationRx.containsKey(element))
        {
            DurationRx.put(element, (long)0);
        }
        return DurationRx.get(element);
    }
    public void setDurationRx(VLCelement element, long value)
    {
        DurationRx.put(element, value);
    }
 
    private HashMap<VLCelement, Double> PowerRx = new HashMap<VLCelement, Double>();
    public double getPowerRx(VLCelement element)
    {
        if(!PowerRx.containsKey(element))
        {
            PowerRx.put(element, (double)0);
        }
        return PowerRx.get(element);
    }
    public void setPowerRx(VLCelement element, double value)
    {
        PowerRx.put(element, value);
    }
 
    private HashMap<VLCelement, Boolean> InterferedRx = new HashMap<VLCelement, Boolean>();
    public boolean getInterferedRx(VLCelement element)
    {
        if(!InterferedRx.containsKey(element))
        {
            InterferedRx.put(element, false);
        }
        return InterferedRx.get(element);
    }
    public void setInterferedRx(VLCelement element, boolean value)
    {
        InterferedRx.put(element, value);
    }
 
    private HashMap<VLCelement, Long> StartTx = new HashMap<VLCelement, Long>();
    public long getStartTx(VLCelement element)
    {
        if(!StartTx.containsKey(element))
        {
            StartTx.put(element, (long)0);
        }
        return StartTx.get(element);
    }
    public void setStartTx(VLCelement element, long value)
    {
        StartTx.put(element, value);
    }
 
    private HashMap<VLCelement, Long> EndTx = new HashMap<VLCelement, Long>();
    public long getEndTx(VLCelement element)
    {
        if(!EndTx.containsKey(element))
        {
            EndTx.put(element, (long)0);
        }
        return EndTx.get(element);
    }
    public void setEndTx(VLCelement element, long value)
    {
        EndTx.put(element, value);
    }
 
    private HashMap<VLCelement, Long> DurationTx = new HashMap<VLCelement, Long>();
    public long getDurationTx(VLCelement element)
    {
        if(!DurationTx.containsKey(element))
        {
            DurationTx.put(element, (long)0);
        }
        return DurationTx.get(element);
    }
    public void setDurationTx(VLCelement element, long value)
    {
        DurationTx.put(element, value);
    }
    private HashMap<VLCelement, Double> PowerTx = new HashMap<VLCelement,Double>();
    public double getPowerTx(VLCelement element)
    {
        if(!PowerTx.containsKey(element))
        {
            PowerTx.put(element, (double)0);
        }
        return PowerTx.get(element);
    }
    public void setPowerTx(VLCelement element, double value)
    {
        PowerTx.put(element, value);
    }
 
    private HashMap<VLCelement, Boolean> InterferedTx = new HashMap<VLCelement, Boolean>();
    public boolean getInterferedTx(VLCelement element)
    {
        if(!InterferedTx.containsKey(element))
        {
            InterferedTx.put(element, false);
        }
        return InterferedTx.get(element);
    }
    public void setInterferedTx(VLCelement element, boolean value)
    {
        InterferedTx.put(element, value);
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
        ElementIDTx.put(nodeID, value);
    }
    public void setElementIDRx(HashSet<Integer> value, Integer nodeID)
    {
       
        this.ElementIDRx.put(nodeID, value);
    }   
    public void setElementIDTx(LinkedList<VLCelement> value, Integer nodeID)
    {
        HashSet<Integer> hs = new HashSet<Integer>();
        for (VLCelement element : value)
        {
            hs.add(element.elementID);
        }
        this.ElementIDTx.put(nodeID, hs);
    }
    public void setElementIDRx(LinkedList<VLCelement> value, Integer nodeID)
    {
        HashSet<Integer> hs = new HashSet<Integer>();
        for (VLCelement element : value)
        {
            hs.add(element.elementID);
            
        }
 
        this.ElementIDRx.put(nodeID, hs);
    }
    /**
     * Gets element ID this message is assigned to for Tx
     * @return
     */
    public HashSet<Integer> getElementIDTx(Integer nodeID)
    {
        if(!ElementIDTx.containsKey(nodeID))
        {
            ElementIDTx.put(nodeID, new HashSet<Integer>());
        }
        return ElementIDTx.get(nodeID);
    }
    /**
     * Gets element ID this message is assigned to for Rx
     * @return
     */
    public HashSet<Integer> getElementIDRx(Integer nodeID)
    {
        if(!ElementIDRx.containsKey(nodeID))
        {
            ElementIDRx.put(nodeID, new HashSet<Integer>());
        }
        return ElementIDRx.get(nodeID);
    }
    /**
     * Add Tx element ID for this message
     * @return
     */
    public void addElementIDTx(int elementID, int nodeID)
    {
        getElementIDTx(nodeID).add(elementID);
    }
 
    public void removeElementIDTx(int elementID, int nodeID)
    {
        getElementIDTx(nodeID).remove(elementID);
    }
    /**
     * Add Rx element ID for this message
     * @return
     */
    public void addElementIDRx(int elementID, int nodeID)
    {
        getElementIDRx(nodeID).add(elementID);
    }
 
    public void removeElementIDRx(int elementID, int nodeID)
    {
        getElementIDRx(nodeID).remove(elementID);
    }
    //////////////////////////////////////////////////
    // frame control
    //
 
    /** RTS packet constant: type = 01, subtype = 1011. */
    //  public static final byte TYPE_RTS  = 27;
 
    /** CTS packet constant: type = 01, subtype = 1100. */
    //public static final byte TYPE_CTS  = 28;
 
    /** ACK packet constant: type = 01, subtype = 1101. */
    //  public static final byte TYPE_ACK  = 29;
 
    /** DATA packet constant: type = 10, subtype = 0000. */
    //  public static final byte TYPE_DATA = 32;
 
    /**
     * packet type.
     */
    //  private byte type;
 
    /**
     * packet retry bit.
     */
    //  private boolean retry;
 
    //////////////////////////////////////////////////
    // initialization
    //
 

 
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
    /*public MacVLCMessage(MacAddress dst, MacAddress src, int duration, short seq, short frag, 
            boolean moreFrag, boolean retry, Message body)
    {
        super(TYPE_DATA, retry);
        this.dst = dst;
        this.src = src;
        this.duration = duration;
    //  this.seq = seq;
    //  this.frag = frag;
        this.body = body;
    }*/
 
    /**
     * Create 802_11 data packet.
     *
     * @param dst packet destination address
     * @param src packet source address
     * @param duration packet transmission duration
     * @param body packet data payload
     * @param prior message priority (0-14)
     */
    public MacVLCMessage(MacAddress dst, MacAddress src, int duration, Message body, byte prior, boolean isencrypted)
    {
        //this(dst, src, duration, (short)-1, (short)-1, false, false, body);
        super(TYPE_DATA, false);
        this.dst = dst;
        this.src = src;
        this.duration = duration;
        this.priority = prior;
        this.isRetry = false;
        this.isEncrypted = isencrypted;
        this.CreateTime = JistAPI.getTime();
         
        this.body = body;
        this.ID = UUID.randomUUID().toString().replaceAll("-", "");
    }
    String ID= "";
 
    //////////////////////////////////////////////////
    // accessors
    //
 
    /**
     * Return packet type.
     *
     * @return packet type
     */
    /*public byte getType()
    {
        return type;
    }*/
 
    /**
     * Return retry bit.
     *
     * @return retry bit
     */
    /*public boolean getRetry()
    {
        return retry;
    }*/
 
 
    /**
     * Packet header size.
     */
    public static final short HEADER_SIZE = 22;
 
    /**
     * Packet sequence number limit.
     */
    //public static final short MAX_SEQ = 4096;
 
    /**
     * Packet destination address.
     */
    private MacAddress dst;
 
    /**
     * Packet source address.
     */
    private MacAddress src;
 
    //  private MacAddress nextHop;
    /**
     * get next hop address
     * @return
     * @author BorisTomas
     */
    /*      public MacAddress getNextHop()
    {
        return nextHop;
    }*/
    /**
     * set next hop address
     * @param value
     * @author BorisTomas
     */
    /*  public void setNextHop(MacAddress value)
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
    //private short seq;
 
    /**
     * Packet fragment number.
     */
    //private short frag;
 
    /**
     * Packet moreFlag bit.
     */
    //  private boolean moreFrag;
 
    /**
     * Packet data payload.
     */
    public Message body;
 
    public boolean isRetry;
    public boolean isEncrypted;
 
    public byte[] encryptedContent;
 
 
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
    /*  public short getSeq()
    {
        return seq;
    }*/
 
    /**
     * Return packet fragment number.
     *
     * @return packet fragment number
     */
    /*public short getFrag()
    {
        return frag;
    }*/
 
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
 
    /*  public class Data extends MacMessage
    {
 
 
    } // class: Data
     */
} // class: MacMessage