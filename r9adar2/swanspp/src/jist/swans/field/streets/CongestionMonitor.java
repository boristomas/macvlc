/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         CongestionMonitor.java
 * RCS:          $Id: CongestionMonitor.java,v 1.1 2007/04/09 18:49:41 drchoffnes Exp $
 * Description:  CongestionMonitor class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 12, 2005 at 2:22:58 PM
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

import jist.swans.misc.Timer;
/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The CongestionMonitor interface describes functionality of 
 * a database for monitoring vehicular congestion.
 */
public interface CongestionMonitor extends Timer.ReusableTimer {
    /** constants for direction */
    final static char TO_START = 0;
    final static char TO_END = 1;
    
    /**
     * Returns the estimated transit time for a road segment.
     * 
     * @param roadSegment index into the vector of RoadSegments
     * @param direction direction for traffic update
     * @return
     */
    float getTransitTime(int roadSegment, char direction);
    
    /**
     * Sets the transit time for a road segment in a particular direction.
     * 
     * @param roadSegment index into the vector of RoadSegments
     * @param direction direction for traffic update
     */
    void updateTransitTime(int roadSegment, char direction);
    
    /**
     * Sets the timeout for each entry
     * @param seconds
     */
    void setExpireInterval(int seconds);
}
