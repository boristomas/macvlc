/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         NetAddressIpFactory.java
 * RCS:          $Id: NetAddressIpFactory.java,v 1.1 2007/04/09 18:49:18 drchoffnes Exp $
 * Description:  NetAddressIpFactory class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jul 31, 2006 at 1:00:40 PM
 * Language:     Java
 * Package:      jist.swans.net
 * Status:       Alpha Release
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
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
package jist.swans.net;

import java.util.HashMap;
import jist.swans.net.NetAddress;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The NetAddressIpFactory class handles reusable NetAddresses to 
 * reduce object creation for NetAddress.Ip objects.
 */
public class NetAddressIpFactory {
    private static HashMap ips;
    private static  Integer ints[];
    private static NetAddressIpFactory self = null;
    
    public static NetAddress getAddress(int i){
        if (self==null) self = new NetAddressIpFactory();
        if (ips.get(getInt(i)) == null){
            ips.put(getInt(i), new NetAddress(i));
        }
        return (NetAddress) ips.get(getInt(i));
    }
    
    private NetAddressIpFactory(){
        ips = new HashMap();
        ints = new Integer[100];
    }
    
    private static Integer getInt(int i){
        if (i >= ints.length){
            Integer tempInts[] = new Integer[ints.length*2];
            while (tempInts.length<i) tempInts = new Integer[tempInts.length*2];
            for (int j = 0; j < ints.length; j++){
                tempInts[j] = ints[j];
            }
            ints = tempInts;            
        }
        if (ints[i]==null) ints[i] = new Integer(i);
        return ints[i];
    }
}
