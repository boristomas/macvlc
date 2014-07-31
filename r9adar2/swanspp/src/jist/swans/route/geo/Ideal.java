/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         Ideal.java
 * RCS:          $Id: Ideal.java,v 1.1 2007/04/09 18:49:46 drchoffnes Exp $
 * Description:  Ideal class (see below)
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

import jist.swans.field.Field;
import jist.swans.misc.Location;
import jist.swans.net.NetAddress;

/**
 * 
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Ideal class provides ideal (i.e., no-cost) location services.
 */
public class Ideal extends LocationDatabase
{


    /**
     * @param field
     * @param database 
     */
    public Ideal(Field field) {
        super(field);
        
        
    }

    /* (non-Javadoc)
     * @see jist.swans.route.geo.LocationDatabase#getLocation(jist.swans.net.NetAddress)
     */
    public Location getLocation(NetAddress dst) {
        try {
        return this.field.getRadioData((Integer)this.dbTable.get(dst)).getLocation();
        }
        catch (NullPointerException e) {
            System.out.println("Address requested: " + dst.toString());
            throw e;
        }
        //return null;
        
    }

    /* (non-Javadoc)
     * @see jist.swans.route.geo.LocationDatabase#setLocation(jist.swans.net.NetAddress, jist.swans.misc.Location)
     */
    public void setLocation(NetAddress dst, Integer id) {
        this.dbTable.put(dst, id);            
    }
    
}