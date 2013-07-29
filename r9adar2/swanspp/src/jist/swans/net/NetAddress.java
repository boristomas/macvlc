//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <NetAddress.java Tue 2004/04/06 11:32:37 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.net;

import jist.swans.misc.Util;
import jist.swans.misc.Pickle;

import java.net.InetAddress;

/**
 * Contains a Network address.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: NetAddress.java,v 1.1 2007/04/09 18:49:18 drchoffnes Exp $
 * @since SWANS1.0
 */

public class NetAddress implements Comparable
{
  //////////////////////////////////////////////////
  // constants
  //

  /**
   * Loopback network address.
   */
  public static final NetAddress LOCAL = new NetAddress(new byte[] { 127, 0, 0, 1 });

  /**
   * Null/invalid network address.
   */
  public static final NetAddress NULL = new NetAddress(new byte[] { -1, -1, -1, -2 });

  /**
   * Broadcast network address.
   */
  public static final NetAddress ANY = new NetAddress(new byte[] { -1, -1, -1, -1 });

  /**
   * Zero-length array of NetAddress type.
   */
  public static final NetAddress[] EMPTY_ARRAY = new NetAddress[0];

  //////////////////////////////////////////////////
  // address
  //

  /**
   * address data.
   */
  private InetAddress ip;

  /**
   * Create a new network address object.
   *
   * @param ip address data
   */
  public NetAddress(InetAddress ip)
  {
    this.ip = ip;
  }

  /**
   * Create a new network address object.
   *
   * @param addr address data
   */
  public NetAddress(byte[] addr)
  {
    this.ip = Pickle.arrayToInetAddress(addr, 0);
  }
  
  /**
   * Create a new network address object.
   *
   * @param addr address data
   * @param offset offset into data array
   */
  public NetAddress(byte[] addr, int offset)
  {
    this.ip = Pickle.arrayToInetAddress(addr, offset);
  }

  /** 
   * Create a new network address object.
   *
   * @param i address data
   */
  public NetAddress(int i) 
  { 
    this(intToByteArray(i)); 
  }

  /**
   * Compute hash code for network address.
   *
   * @return hash code of address
   */
  public int hashCode()
  {
    return toInt();
  }

  /**
   * Return whether this network address is equal to another object.
   *
   * @param o object to test equality against
   * @return whether object provided is equal to this network address
   */
  public boolean equals(Object o)
  {
    if(this==o) return true;
    if(o==null) return false;
    if(!(o instanceof NetAddress)) return false;
    NetAddress na = (NetAddress)o;
    return this.ip.equals(na.ip);
  }

  /**
   * Return IP address information.
   *
   * @return IP address information
   */
  public InetAddress getIP()
  {
    return ip;
  }

  /**
   * Convert an integer into a byte array.
   *
   * @param i input integer to convert
   * @return corresponding byte array
   */
  private static byte[] intToByteArray(int i)
  {
    byte[] b = new byte[4];
    b[3] = (byte)(i & 0xff);
    b[2] = (byte)((i>>8) & 0xff);
    b[1] = (byte)((i>>16) & 0xff);
    b[0] = (byte)((i>>24) & 0xff);
    return b;
  }

  /** {@inheritDoc} */
  public String toString()
  {
    if(equals(ANY))
    {
      return "ANY";
    }
    else if(equals(LOCAL))
    {
      return "LOCAL";
    }
    else if(equals(NULL))
    {
      return "NULL";
    }
    else
    {
      return ip.toString().substring(1);
    }
  }
  
  public int toInt()
  {
      int i = 0;
      byte b[] = ip.getAddress();
      i += Util.toUnsignedByte(b[3]);
      i += Util.toUnsignedByte(b[2])<<8;
      i += Util.toUnsignedByte(b[1])<<16;
      i += Util.toUnsignedByte(b[0])<<24;
    return i;
  }

/* (non-Javadoc)
 * @see java.lang.Comparable#compareTo(java.lang.Object)
 */
public int compareTo(Object o) {
    NetAddress rightAddress = (NetAddress)o;
    byte left[] = ip.getAddress();
    byte right[] = rightAddress.ip.getAddress();
    for (int i = 0; i < left.length; i++)
    {
        if (left[i] > right[i]) return 1;
        else if (left[i] < right[i]) return -1;
    }
    return 0;
}

} // class NetAddress

