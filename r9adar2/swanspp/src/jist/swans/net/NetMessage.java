//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <NetMessage.java Sun 2005/03/13 11:08:45 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.net;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashSet;

import driver.JistExperiment;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.radio.TimeEntry;

/**
 * Network packet.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: NetMessage.java,v 1.1 2007/04/09 18:49:18 drchoffnes Exp $
 * @since SWANS1.0
 */
public abstract class NetMessage implements Message, Cloneable
{

	
	
  //////////////////////////////////////////////////
  // IPv4 packet: (size = 20 + 4 * options + body)
  //   version                size: 4 bits
  //   header length          size: 4 bits
  //   type of service (tos)  size: 1
  //     - priority             size: 3 bits
  //     - delay bit            size: 1 bit
  //     - throughput bit       size: 1 bit
  //     - reliability bit      size: 1 bit
  //     - reserved             size: 2 bits
  //   total length           size: 2 (measured in 64 bit chunks)
  //   id                     size: 2
  //   control flags          size: 3 bits
  //     - reserved (=0)        size: 1 bit
  //     - unfragmentable       size: 1 bit
  //     - more fragments       size: 1 bit
  //   fragment offset        size: 13 bits
  //   ttl                    size: 1
  //   protocol               size: 1
  //   header chksum          size: 2
  //   src                    size: 4
  //   dst                    size: 4
  //   options:               size: 4 * number
  //   packet payload:        size: variable
  //

  /**
   * IPv4 network packet.
   */
  public static class Ip extends NetMessage
  {

    /** Fixed IP packet size. */
    public static final int BASE_SIZE = 20;

    
    
    
    public ArrayList<TimeEntry> Times = new ArrayList<TimeEntry>();
    
    /**
     * Is message fresh, first time entered mac? this is false if message was queued at least once.
     * @author BorisTomas
     */
    public boolean isFresh = true;
    
    //////////////////////////////////////////////////
    // message contents
    //

    /** immutable bit. */
    private boolean frozen;
    /** ip packet source address. */
    private NetAddress src;
    /** ip packet destination address. */
    private NetAddress dst;
    /** ip packet payload. */
    private Message payload;
    /** ip packet priority level. */
    private byte priority;
    /** ip packet protocol, such as TCP, UDP, etc. */
    private short protocol;
    /** ip packet time-to-live. */
    private int ttl;
    /** ip packet identification. */
    public short id;
    /** ip packet fragment offset. */
    private short fragOffset;

    // options
    /** source route. */
    public IpOptionSourceRoute srcRoute;
    /** next hop */
    public IpOptionNextHop nextHop;

    /** Next identification number to use. */
    private static short nextId = 0;

    /** destination location */
    public IpOptionLocation location;

    /**
     * Create new IPv4 packet.
     *
     * @param payload packet payload
     * @param src packet source address
     * @param dst packet destination address
     * @param protocol packet protocol
     * @param priority packet priority
     * @param ttl packet time-to-live
     * @param id packet identification
     * @param fragOffset packet fragmentation offset
     */
    public Ip(Message payload, NetAddress src, NetAddress dst, 
        short protocol, byte priority, int ttl, short id, short fragOffset)
    {
      if(payload==null) throw new NullPointerException();
      Times.add(new TimeEntry(0, "create", this));// this.TimeNCreated = JistAPI.getTime();
      this.frozen = false;
      this.payload = payload;
      this.src = src;
      this.dst = dst;
      this.protocol = protocol;
      this.priority = priority;
      this.ttl = ttl;
      this.id = id;
      this.fragOffset = fragOffset;
    }

    /**
     * Create new IPv4 packet with default id.
     *
     * @param payload packet payload
     * @param src packet source address
     * @param dst packet destination address
     * @param protocol packet protocol
     * @param priority packet priority
     * @param ttl packet time-to-live
     */
    public Ip(Message payload, NetAddress src, NetAddress dst,
        short protocol, byte priority, int ttl)
    {
      this(payload, src, dst, protocol, priority, ttl, nextId++, (short)0);
    }


    /**
     * Render packet immutable.
     *
     * @return immutable packet, possibly intern()ed
     */
    public Ip freeze()
    {
      // todo: could perform an intern/hashCons here
      this.frozen = true;
      return this;
    }

    /**
     * Whether packet is immutable.
     *
     * @return whether packet is immutable
     */
    public boolean isFrozen()
    {
      return frozen;
    }

    /**
     * Make copy of packet, usually in order to modify it.
     *
     * @return mutable copy of packet.
     */
    public Ip copy()
    {
      NetMessage.Ip ip2 = new Ip(payload, src, dst, protocol, priority, ttl);
      ip2.srcRoute = this.srcRoute;
      ip2.location = this.location;
      ip2.nextHop = this.nextHop;
      return ip2;
    }

    //////////////////////////////////////////////////
    // accessors 
    //

    /**
     * Return packet source.
     *
     * @return packet source
     */
    public NetAddress getSrc()
    {
      return src;
    }

    /**
     * Return packet destination.
     *
     * @return packet destination
     */
    public NetAddress getDst()
    {
      return dst;
    }

    /**
     * Return packet payload.
     *
     * @return packet payload
     */
    public Message getPayload()
    {
      return payload;
    }

    /**
     * Return packet priority.
     *
     * @return packet priority
     */
    public byte getPriority()
    {
      return priority;
    }

    /**
     * Return packet protocol.
     *
     * @return packet protocol
     */
    public short getProtocol()
    {
      return protocol;
    }

    /**
     * Return packet identification.
     *
     * @return packet identification
     */
    public short getId()
    {
      return id;
    }

    /**
     * Return packet fragmentation offset.
     *
     * @return packet fragmentation offset
     */
    public short getFragOffset()
    {
      return fragOffset;
    }

    //////////////////////////////////////////////////
    // TTL
    //

    /**
     * Return packet time-to-live.
     *
     * @return time-to-live
     */
    public int getTTL()
    {
      return ttl;
    }

    /**
     * Create indentical packet with decremented TTL.
     */
    public void decTTL()
    {
      if(frozen) throw new IllegalStateException();
      ttl--;
    }
    /**
     * Reassign the TTL.
     * 
     * @param newTTL
     */
    public void setTTL(int newTTL)
    {
        if(frozen) throw new IllegalStateException();
    	ttl = newTTL;
    }

    //////////////////////////////////////////////////
    // source route
    //

    /**
     * Returns whether packet contains source route.
     *
     * @return whether packet contains source route
     */
    public boolean hasSourceRoute()
    {
      return srcRoute!=null;
    }

    /**
     * Return source route. (do not modify)
     *
     * @return source route (do not modify)
     */
    public NetAddress[] getSourceRoute()
    {
      return srcRoute.getRoute();
    }

    /**
     * Return source route pointer.
     *
     * @return source route pointer
     */
    public int getSourceRoutePointer()
    {
      return srcRoute.getPtr();
    }

    /**
     * Set source route.
     *
     * @param srcRoute source route
     */
    public void setSourceRoute(IpOptionSourceRoute srcRoute)
    {
      if(frozen) throw new IllegalStateException();
      this.srcRoute = srcRoute;
    }
    
    /**
     * Returns whether packet contains a next hop.
     *
     * @return whether packet contains a next hop
     */
    public boolean hasNextHop()
    {
      return nextHop!=null;
    }

    /**
     * Return a next hop. (do not modify)
     *
     * @return a next hop(do not modify)
     */
    public NetAddress getNextHop()
    {
    	if (nextHop == null) return null;
    	else return nextHop.getNextHop();
    }
    
    /**
     * Set a next hop.
     *
     * @param nextHop next hop
     */
    public void setNextHop(IpOptionNextHop nextHop)
    {
      if(frozen) throw new IllegalStateException();
      this.nextHop = nextHop;
    }
    
    /**
     * Returns whether packet contains a location.
     *
     * @return whether packet contains a location
     */
    public boolean hasLocation()
    {
      return location!=null;
    }

    /**
     * Return a location. (do not modify)
     *
     * @return location(do not modify)
     */
    public Location getLocation()
    {
        
    	if (location == null) return null;
    	else return location.getLocation(); 
    }
    
    /**
     * Set location.
     *
     * @param locaiton location
     */
    public void setLocation(IpOptionLocation l)
    {
      if(frozen) throw new IllegalStateException();
      this.location = l;
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "ip(src="+src+" dst="+dst+" size="+getSize()+" prot="+protocol+" ttl="+ttl+" route="+srcRoute+" data="+payload+")";
    }

    //////////////////////////////////////////////////
    // message interface
    //

    /** {@inheritDoc} */
    public int getSize()
    {
      int size = payload.getSize();
      if(size==Constants.ZERO_WIRE_SIZE)
      {
        return Constants.ZERO_WIRE_SIZE;
      }
      // todo: options
      return BASE_SIZE + size;
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /**
     * @param gpsrh
     */
    public void setPayload(Message payload) {
        if(frozen) throw new IllegalStateException();
        this.payload = payload;
        
    }

    /**
     * @param dst The dst to set.
     */
    public void setDst(NetAddress dst) {
        this.dst = dst;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
//    @Override
    public boolean equals(Object obj) {
       
        if (obj instanceof NetMessage.Ip){
            NetMessage.Ip ipMsg = (NetMessage.Ip)obj;
            return src.equals(ipMsg.src) && 
            dst.equals(ipMsg.dst) && 
//            id == ipMsg.id && 
            priority == ipMsg.priority && 
            protocol == ipMsg.protocol && 
            fragOffset == ipMsg.fragOffset && 
            payload.equals(ipMsg.payload);
            //&& ttl == ttl;
            
        }
        return false;
    }
  } // class: Ip


  /**
   * A generic IP packet option.
   */
  public abstract static class IpOption implements Message
  {
    /**
     * Return option type field.
     *
     * @return option type field
     */
    public abstract byte getType();

    /**
     * Return option length (in bytes/octets).
     *
     * @return option length (in bytes/octets)
     */
    public abstract int getSize();

  } // class: IpOption


  /**
   * An IP packet source route option.
   */
  public static class IpOptionSourceRoute extends IpOption
  {
    /** option type constant: source route. */
    public static final byte TYPE = (byte)137;

    /** source route. */
    private final NetAddress[] route;
    /** source route pointer: index into route. */
    private final int ptr;

    /**
     * Create new source route option.
     *
     * @param route source route
     */
    public IpOptionSourceRoute(NetAddress[] route)
    {
      this(route, (byte)0);
    }

    /**
     * Create new source route option.
     *
     * @param route source route
     * @param ptr source route pointer
     */
    public IpOptionSourceRoute(NetAddress[] route, int ptr)
    {
      this.route = route;
      this.ptr = ptr;
    }

    /**
     * Return source route.
     *
     * @return source route (do not modify)
     */
    public NetAddress[] getRoute()
    {
      return route;
    }

    /**
     * Return source route pointer: index into route.
     *
     * @return source route pointer: index into route
     */
    public int getPtr()
    {
      return ptr;
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return TYPE;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      return (byte)(route.length*4 + 3);
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return ptr+":["+Util.stringJoin(route, ",")+"]";
    }

  } // class: IpOptionSourceRoute

  /**
   * An IP packet next hop option.
   */
  public static class IpOptionNextHop extends IpOption
  {
    /** option type constant: source route. */
    public static final byte TYPE = (byte)138; // T-ODO find a real number for this

    /** next hop. */
    private NetAddress nextHop;

    /**
     * Create new next hop option.
     *
     * @param nextHop next hop 
     */
    public IpOptionNextHop(NetAddress nextHop)
    {
        assert(nextHop!=null);
    	this.nextHop = nextHop;
    }


    /**
     * Return source route.
     *
     * @return source route (do not modify)
     */
    public NetAddress getNextHop()
    {
      return nextHop;
    }
    
    public void setNextHop(NetAddress newNextHop)
    {
    	nextHop = newNextHop;
    }


    /** {@inheritDoc} */
    public int getSize()
    {
      return (byte)(4+1);
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "["+nextHop+"]";
    }


	/* (non-Javadoc)
	 * @see jist.swans.net.NetMessage.IpOption#getType()
	 */
	public byte getType() {
		return TYPE;
	}

  } // class: IpOptionNextHop

  
  /**
   * An IP packet destination location option.
   */
  public static class IpOptionLocation extends IpOption
  {
    /** option type constant: location. */
    public static final byte TYPE = (byte)139; // T-ODO find a real number for this

    /** location. */
    private Location location;

    /**
     * Create new location option.
     *
     * @param location location
     */
    public IpOptionLocation(Location location)
    {
        if (location == null) throw new RuntimeException("Setting location to null!");
    	this.location= location;
    }


    /**
     * Return source route.
     *
     * @return source route (do not modify)
     */
    public Location getLocation()
    {
      return location;
    }
    
    public void setLocation(Location l)
    {
    	this.location = l;
    }


    /** {@inheritDoc} */
    public int getSize()
    {
      return (byte)(3*4+1);
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "["+location+"]";
    }


	/* (non-Javadoc)
	 * @see jist.swans.net.NetMessage.IpOption#getType()
	 */
	public byte getType() {
		return TYPE;
	}

  } // class: IpOptionLocation
  
} // class: NetMessage

/*
todo:
#define IP_MAXPACKET    65535       // maximum packet size
#define MAXTTL      255     // maximum time to live (seconds) 
#define IPDEFTTL    64      // default ttl, from RFC 1340 
#define IP_MSS      576     // default maximum segment size 
*/

