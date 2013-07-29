/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         LocationDatabase.java
 * RCS:          $Id: LocationDatabase.java,v 1.1 2007/04/09 18:49:46 drchoffnes Exp $
 * Description:  LocationDatabase class (see below)
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

import java.util.HashMap;

import jist.swans.field.Field;
import jist.swans.misc.Location;
import jist.swans.net.NetAddress;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The LocationDatabase class provides mappings between IP addresses 
 * and the locations of nodes assigned to those addresses.
 */
public abstract class LocationDatabase {

    protected HashMap dbTable;
    protected Field field;
    
    /**
     * 
     */
    public LocationDatabase(Field field) {
        dbTable = new HashMap();
        this.field = field;
    }
    
    public abstract Location getLocation(NetAddress dst);
    public abstract void setLocation(NetAddress dst, Integer id);


}
