/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         RouteGeo.java
 * RCS:          $Id: RouteGeo.java,v 1.1 2007/04/09 18:49:30 drchoffnes Exp $
 * Description:  RouteGeo class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Mar 9, 2005
 * Language:     Java
 * Package:      jist.swans.route
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
package jist.swans.route;


import jist.swans.field.Field;
import jist.swans.misc.Location;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The RouteGeo abstract class implements the basic functionality 
 * for geographic routing implementations. Detailed implementations 
 * must extend this class to provide additional services.
 */
public abstract class RouteGeo implements RouteInterface.Geo {

    protected Field field;
    protected Integer selfId;    
//    private RouteGeo self;

    /**
	 * RouteGeo constructor
	 * @param field The field for the simulation, for GPS purposes.
	 */
    public RouteGeo(Field field, int selfId) {
        
        this.field = field;
        this.selfId = new Integer(selfId);
//        this.self = (RouteGeo)JistAPI.proxy(this, RouteGeo.class);
    }
    
    public Location getCurrentLocation()
    {
        return field.getRadioData(selfId).getLocation();
    }
    
    protected float getCurrentSpeed()
    {
        return field.getRadioData(selfId).getMobilityInfo().getSpeed();
    }
    
    protected Location getCurrentBearing()
    {
        return field.getRadioData(selfId).getMobilityInfo().getBearing();
    }
    
    protected static double bearing(Location l1, Location l2)
    {
        double brg;

        // X-XX only deal with 2D for now
        // X-XX check for (0, 0) args, a domain error
        brg = Math.atan2(l2.getY() - l1.getY(), l2.getX() - l1.getX());
        if (brg < 0)
          brg += 2*Math.PI;
        return brg;
    }
    
    protected static Location cross_segment(Location l1, Location l2,
			 Location l3, Location l4,
			 Location li)
    {
        double dy[] = new double[2];
        double dx[] = new double[2];
        double m[]= new double[2];
        double b[] = new double[2];
        double xint, yint;
        
        float y2 = l2.getY();
        float y1 = l1.getY();
        float x2 = l2.getX(); 
        float x1 = l1.getX();
        float y4 = l4.getY();
        float y3 = l3.getY();
        float x4 = l4.getX();
        float x3 = l3.getX();
        
        dy[0] = y2 - y1;
        dx[0] = x2 - x1;
        dy[1] = y4 - y3;
        dx[1] = x4 - x3;
        m[0] = dy[0] / dx[0];
        m[1] = dy[1] / dx[1];
        b[0] = y1 - m[0] * x1;
        b[1] = y3 - m[1] * x3;
        if (m[0] != m[1]) {
          // slopes not equal, compute intercept
          xint = (b[0] - b[1]) / (m[1] - m[0]);
          yint = m[1] * xint + b[1];
            // is intercept in both line segments?
            if ((xint <= Math.max(x1, x2)) && (xint >= Math.min(x1, x2)) &&
                    (yint <= Math.max(y1, y2)) && (yint >= Math.min(y1, y2)) &&
                    (xint <= Math.max(x3, x4)) && (xint >= Math.min(x3, x4)) &&
                    (yint <= Math.max(y3, y4)) && (yint >= Math.min(y3, y4))) {
//                if (li == null) {
                    li = new Location.Location2D((float)xint, (float)yint);
//                }
                return li;
            }
        }
        return null;
    }

}
