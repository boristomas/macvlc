/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         CongestionMonitorIdeal.java
 * RCS:          $Id: CongestionMonitorIdeal.java,v 1.1 2007/04/09 18:49:42 drchoffnes Exp $
 * Description:  CongestionMonitorIdeal class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 12, 2005 at 2:33:18 PM
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

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import jist.swans.field.StreetMobilityInfo;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The CongestionMonitorIdeal class implements perfect information 
 * for vehicular congestion.
 */
public class CongestionMonitorIdeal implements CongestionMonitor {

    /** the road segments */
    Vector segments;
    
    /**
     * 
     */
    public CongestionMonitorIdeal(Vector segments) {
        super();
        this.segments = segments;
    }

    /* (non-Javadoc)
     * @see jist.swans.field.streets.CongestionMonitor#getTransitTime(int, boolean)
     */
    public float getTransitTime(int roadSegment, char direction) {
        float tt;
        RoadSegment rs = (RoadSegment)segments.get(roadSegment);
        LinkedList lane;
        if (direction==TO_START) lane = rs.getCarsToStart();
        else lane = rs.getCarsToEnd();
        
        // uncongested estimate
        tt = rs.length/rs.getSpeedLimit();
        
        // check for congestion, only do when lane is more than a third full
        if (lane.size() > 
            Math.min((rs.length/(RoadSegment.CAR_LENGTH+RoadSegment.SPACE)/3), 10))
        {
            ListIterator li = lane.listIterator();
            StreetMobilityInfo smi;
            float totSpeed = 0;
            float rsLength, startupDist; 
            double addedSpeed=0, currentSpeed, speedLimit;
            int numSpeeds=0;
            while (li.hasNext())
            {
                smi = (StreetMobilityInfo)li.next();
                rsLength = smi.current.length;
                currentSpeed = smi.getSpeed();
                speedLimit = rs.getSpeedLimit()+smi.extraSpeed;
                startupDist = (smi.current.getSpeedLimit() / smi.acceleration)*(smi.current.getSpeedLimit()/2);
                if (rsLength - smi.getRemainingDist() < startupDist)
                {
                    continue;
//                    addedSpeed = (rsLength - smi.getRemainingDist())/((speedLimit - currentSpeed)/(smi.acceleration));
                }
                else if (smi.getRemainingDist() < startupDist){
                    continue;
//                    addedSpeed = (startupDist - smi.getRemainingDist())/((speedLimit - currentSpeed)/(smi.acceleration));
                }
//                if (addedSpeed < 0) addedSpeed = 0;
                
                totSpeed += smi.getSpeed();

                numSpeeds++;
            }
            if (totSpeed>0) tt = rs.length/(totSpeed/numSpeeds);
        }
                    
        return tt;
        
    }

    /* (non-Javadoc)
     * @see jist.swans.field.streets.CongestionMonitor#updateTransitTime(int, boolean)
     */
    public void updateTransitTime(int roadSegment, char direction) {
        // nothing to do for ideal

    }

    /* (non-Javadoc)
     * @see jist.swans.field.streets.CongestionMonitor#setExpireInterval(int)
     */
    public void setExpireInterval(int seconds) {
        // nothing to do for ideal

    }

    /* (non-Javadoc)
     * @see jist.swans.misc.Timer.ReusableTimer#timeout(int)
     */
    public void timeout(int idVal) {
        // nothing to do for ideal

    }

}
