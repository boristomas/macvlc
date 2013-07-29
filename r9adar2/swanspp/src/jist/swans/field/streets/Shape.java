/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         Shape.java
 * RCS:          $Id: Shape.java,v 1.1 2007/04/09 18:49:42 drchoffnes Exp $
 * Description:  Shape class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 7, 2004
 * Language:     Java
 * Package:      jist.swans.field.streets
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
package jist.swans.field.streets;


import jist.swans.misc.Location;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Shape class represents the shape of a RoadSegment.
 */
public class Shape {
   public final Location points[];
    
    /**
     * @param points array of points along the RoadSegment
     */
    public Shape(Location[] points) {
        super();
        this.points = points;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String s = "";
        
        for (int i = 0; i < points.length; i++) s+="["+i+"] "+points[i];
        return s;
    }
}
