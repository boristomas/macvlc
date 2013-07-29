/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         AddressGeographic.java
 * RCS:          $Id: AddressGeographic.java,v 1.1 2007/04/09 18:49:46 drchoffnes Exp $
 * Description:  AddressGeographic class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Mar 9, 2005
 * Language:     Java
 * Package:      jist.swans.route.geo
 * Status:       Alpha Release
 *
 * (C) Copyright 2005, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package jist.swans.route.geo;

import jist.swans.mac.MacAddress;
import jist.swans.misc.Location;
import jist.swans.net.NetAddress;

/**
 * 
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The AddressGeographic class represents the tuple of physical 
 * location, IP address and MAC address.
 */
public class AddressGeographic
{
    /** GPS location. */
    public Location loc;
    /** IP address. */
    public NetAddress address;
    /** MAC address */
    public MacAddress macAddr;
            
    /**
     * @param loc
     * @param address
     */
    public AddressGeographic(Location loc, NetAddress address) {
        this.loc = loc;
        this.address = address;
    }  
    
    public AddressGeographic(Location loc, NetAddress address, MacAddress macAddress) {
        this(loc, address);
        this.macAddr = macAddress;
    }   
    
    /**
     * @return Returns the address.
     */
    public NetAddress getAddress() {
        return address;
    }
    /**
     * @param address The address to set.
     */
    public void setAddress(NetAddress address) {
        this.address = address;
    }
    /**
     * @return Returns the loc.
     */
    public Location getLoc() {
        return loc;
    }
    /**
     * @param loc The loc to set.
     */
    public void setLoc(Location loc) {
        this.loc = loc;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof AddressGeographic)
        {
            AddressGeographic ag = (AddressGeographic)obj;
            if (ag.address.compareTo(address)==0 
                    && ag.loc.distance(loc)==0)
            {
                return true;
            }
            else return false;
        }
        else return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "IP: "+address+"\n\t Location: " + loc;
    }
}