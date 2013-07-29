/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetPlacementRandom.java
 * RCS:          $Id: StreetPlacementRandom.java,v 1.1 2007/04/09 18:49:27 drchoffnes Exp $
 * Description:  StreetPlacementRandom class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 10, 2004
 * Language:     Java
 * Package:      jist.swans.field
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
package jist.swans.field;

import jist.swans.Constants;
import jist.swans.field.StreetMobilityOD.StreetMobilityInfoOD;
import jist.swans.field.streets.RoadSegment;
import jist.swans.field.streets.SegmentNode;
import jist.swans.field.streets.StreetName;
import jist.swans.misc.Location;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StreetPlacementRandom class places nodes at random streets
 * in the current map.
 */
public class StreetPlacementRandom extends StreetPlacement {
    
    
    /**
     * Initialize random placement.
     *
     * @param br bottom right corner
     * @param tl upper left corner
     * @param smr the StreetMobility object
     * @param stdDev the std dev for driver speeds
     * @param stepTime the step time for street mobility
     */
    public StreetPlacementRandom(Location bl, Location tr, StreetMobility smr,
            double stdDev, double stepTime)
    {
        super(bl, tr, smr, stdDev, stepTime);
    }    
    
    //////////////////////////////////////////////////
    // StreetPlacement interface
    //
    

	int setInitialSegment(StreetMobilityInfo smri) {
		int segmentID=0,direction=0;
        boolean full=false;
        boolean valid = false; // did this find a valid street pair?
    	
		while (!valid) // ensure non-isolated streets for OD mobility
        {         
            // get random street
            do
            {
                segmentID = Constants.random.nextInt(sm.segments.size());
                direction = Constants.random.nextInt(2); // pick direction
                rs = (RoadSegment)sm.segments.elementAt(segmentID);
//                if (rs == null) continue; // due to pruning
                
                // make sure lane isn't full
                if (direction==TO_START)
                {
                    if ((rs.getCarsToStart().size()+1)>rs.getMaxCars()) full=true;
                    else full=false;
                }
                else
                {
                    if ((rs.getCarsToEnd().size()+1)>rs.getMaxCars()) full=true;
                    else full=false;
                }
                
                if (DEBUG &&!full)System.out.println("At street index " + rs.getStreetIndex() + ", street name is "+
                        (StreetName)sm.streets.get(new Integer(rs.getStreetIndex())));
                if (DEBUG && !full)System.out.println("Segment index is: "+rs.getSelfIndex());
                
            } while (((StreetName)sm.streets.get(new Integer(rs.getStreetIndex()))).getName().equals("")
                    || full);
            
            if (DEBUG) System.out.println("Placed node " + (numberOfNodes+1)+ " at " +
                    (StreetName)sm.streets.get(new Integer(rs.getStreetIndex()))+"!");
            
            // set the rsEnd for calculating path
            if (direction == TO_START) smri.rsEnd = rs.getStartPoint();
            else smri.rsEnd = rs.getEndPoint();
            
            // in the case of the SMOD model, some values have to be initialized
            if (sm instanceof StreetMobilityOD)
            {
                StreetMobilityOD smod = (StreetMobilityOD)sm;
                smri.current = rs;
                
                // pick random dest road segment            
                RoadSegment dest = null;
                while (dest == null || dest.getSelfIndex() == rs.getSelfIndex() 
                        || dest.getEndPoint().distance(rs.getEndPoint())>threshold) 
                {
                    // this should pick a relatively local location
                    dest = (RoadSegment)smod.segments.get(rnd.nextInt(smod.segments.size()));
                }
                
                // calculate OD path
                valid = smod.calculatePath((StreetMobilityInfoOD)smri, rs, smri.rsEnd, dest);
                
                // only valid path may force car to be in other lane
                if (valid)
                {
                    StreetMobilityInfoOD smiod=(StreetMobilityInfoOD)smri;
                    SegmentNode sn = (SegmentNode)smiod.path.getFirst();
                    RoadSegment rsTemp = (RoadSegment)sm.segments.get(sn.segmentID);
//                    smod.checkPath(smiod);
//                    if (true) smod.printPath(smiod.path);                    
                    
                    if (DEBUG)System.out.println("Current rsEnd: " + smri.rsEnd);
                    if (DEBUG)System.out.println("Current road: " + rs.printStreetName(sm.streets));	                
                    
                    // redundant first entry
//                    if (sn.segmentID != rs.getSelfIndex() && 
//                            rs.getStartPoint().distance(rsTemp.getStartPoint())==0 && 
//                            rs.getEndPoint().distance(rsTemp.getEndPoint())==0)
//                    {
//                        rs = (RoadSegment)sm.segments.get(((SegmentNode)smiod.path.removeFirst()).segmentID);
//                        sn = (SegmentNode)smiod.path.getFirst();
//                        rsTemp = (RoadSegment)sm.segments.get(sn.segmentID);
//                    }
                    
                    // needs to be in opposite lane
                    if (sn.segmentID == rs.getSelfIndex() && 
                            sn.point.distance(smri.rsEnd)<StreetMobility.INTERSECTION_RESOLUTION )
                             
                    {
                        if (smri.rsEnd.distance(rs.getEndPoint())==0)
                        {
                            smri.rsEnd = rs.getEndPoint();
                            direction = TO_END;
                        }
                        else
                        {
                            smri.rsEnd = rs.getStartPoint();
                            direction = TO_START;
                        }
                        smiod.path.remove(0);
                        if (DEBUG)System.out.println("Removed road during placement!");
                    }
                    
                    // another case where it needs to be in opposite lane
                    // diagnosis: rsEnd is not an endpoint in the next segment
                    // needs to be in opposite lane
                    if (rsTemp.getEndPoint().distance(smri.rsEnd)>StreetMobility.INTERSECTION_RESOLUTION
                            && rsTemp.getStartPoint().distance(smri.rsEnd)>StreetMobility.INTERSECTION_RESOLUTION)
                    {
                        if (smri.rsEnd.distance(rs.getEndPoint())==0)
                        {
                            smri.rsEnd = rs.getStartPoint();
                            direction = TO_START;
                            if (rs.getCarsToStart().size()>=rs.getMaxCars()) full=true;
                        }
                        else
                        {
                            smri.rsEnd = rs.getEndPoint();
                            direction = TO_END;
                            if (rs.getCarsToEnd().size()>=rs.getMaxCars()) full=true;
                        }
                        
                        if (full){
                            valid = false;
                            continue;
                        }
                        if (DEBUG)System.out.println("Put node in opposite lane!");
                    }
                }
            }
            else // not using OD pairs
            {
                valid = true;
            }
        } // while valid streets have not been picked
		
		return direction;
	}
    
    
}// class: StreetPlacementRandom
