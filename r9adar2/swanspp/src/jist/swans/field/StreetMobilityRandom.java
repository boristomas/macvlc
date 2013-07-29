/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetMobilityRandom.java
 * RCS:          $Id: StreetMobilityRandom.java,v 1.1 2007/04/09 18:49:27 drchoffnes Exp $
 * Description:  StreetMobilityRandom class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 9, 2005
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

import java.awt.Color;
import java.util.LinkedList;
import java.util.Random;

import jist.swans.Constants;
import jist.swans.field.streets.Intersection;
import jist.swans.field.streets.LaneChangeModel;
import jist.swans.field.streets.RoadSegment;
import jist.swans.field.streets.StreetName;
import jist.swans.misc.Location;
 
/**
 * 
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StreetMobilityRandom class represents movement along roads 
 * with random waypoints chosen from a set of intersections encountered.
 */
public class StreetMobilityRandom extends StreetMobility
{
    public static class StreetMobilityInfoRandom extends StreetMobilityInfo
    {
        /** probability that the node will turn at an intersection */
        double probability;    
    }
    
    /** probability that the node will turn at an intersection (for all nodes) */
    double probability;    
    /** movement granularity */
    int granularity;
    /** use of stoplights or stop signs */
    final boolean STOPLIGHTS = true;
    
    
    /**
     * @param segmentFile
     * @param streetFile
     * @param shapeFile
     * @param degree
     * @param probability probability with which a car will turn at an intersection
     * @param bl bottom left corner of map in lat/long
     * @param tr top right corner of map in lat/long
     * @param r
     */
    public StreetMobilityRandom(String segmentFile, String streetFile,
            String shapeFile, int degree, double probability, int gran,
			Location.Location2D bl, Location.Location2D tr, Random r) 
    {
        super(segmentFile, streetFile, shapeFile, degree, bl, tr);
        this.probability = probability;
        this.granularity = gran;
        this.rnd = r;
    }
    
    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#init(jist.swans.field.FieldInterface, java.lang.Integer, jist.swans.misc.Location)
     */
    public MobilityInfo init(FieldInterface f, Integer id, Location loc) {
        
    	// make sure that the placement function has set all necessary values
    	StreetMobilityInfo mi = (StreetMobilityInfo)mobInfo.get(id.intValue()-1);
    	mi.v = v;
    	((StreetMobilityInfoRandom)mi).probability = probability; // per-vehcile turn prob
    	return mi;    
    }
    
    /* (non-Javadoc)
     * @see jist.swans.field.StreetMobility#setNextRoad(jist.swans.field.StreetMobilityInfo)
     */
    public void setNextRoad(StreetMobilityInfo smi)
    {        
        RoadSegment rs;
        StreetMobilityInfoRandom smir = (StreetMobilityInfoRandom)smi;
        
        // get next intersection
        smir.nextIs = intersections.findIntersectingRoads(smir.rsEnd);
        
        if (smir.nextIs == null)
        {
            if (v!=null) v.colorSegment(smi.current, Color.RED);
            System.out.println("Road considered"+ smir.current.printStreetName(streets));
            System.out.println("End point: "+smir.rsEnd);
            throw new RuntimeException("No intersecting roads!");
        }
        
        LinkedList ll = smir.nextIs.getRoads();
        
        if (DEBUG) System.out.println("At end of " + (StreetName)streets.get(new Integer(smir.current.getStreetIndex())) +
        		", there are " + ll.size() + " roads");        
        
        // turn only if the probability is high enough and there are intersecting roads
        if (rnd.nextDouble() < smir.probability && ll.size()>1)
        {
        	setNextRoadTurn(smir, ll);
        }
        else // continue on current road
        {
            if (DEBUG) System.out.println("Staying on same road...");
            
            boolean found = false;
            
            // find the next road segment for current street
            for (int i = 0; i < ll.size(); i++)
            {
                rs = (RoadSegment)ll.get(i);
                if (DEBUG) System.out.println("Intersecting Road:" + (StreetName)streets.get(new Integer(rs.getStreetIndex())));


                // stop only if this is the same street
                if(rs.getStreetIndex()==smir.current.getStreetIndex()
                		&& rs.getSelfIndex()!=smir.current.getSelfIndex())
                {
                    updateNextRoadSegment(smir, rs);
                    found = true;
                    break;
                }
            } // end for
            if (!found) // current road ends
            { 
            	if (DEBUG)System.out.println("Can't go straight!");
            	
            	// if possible, turn (e.g., T-intersection)
            	if (ll.size()>1)
            	{
            		setNextRoadTurn(smir, ll);
            		//smir.currSpeed=2.23f;
            	}
            	else // this really is a dead end
            	{
            	    updateNextRoadSegment(smir, smir.current);	                
	                //smir.currSpeed = 0;

            	} // end case dead end
            } // end case road does not continue
            
        } // end case continue on current street
        
    } // end method setNextRoad
    
	/**
	 * Sets information for the next road if there is a turn.
	 * 
	 * @param smir The SMIR object to update.
	 * @param ll The list of RoadSegments at the specified intersection.
	 */
    private void setNextRoadTurn(StreetMobilityInfoRandom smir, LinkedList ll)
    {
    	int index; 
    	RoadSegment rs;
    	Intersection is;
        
    	if (DEBUG)System.out.print("Will turn..."); 
        
    	// find next new street
        do
        {
            index = rnd.nextInt(ll.size());            
            rs = (RoadSegment)ll.get(index);                        
            ll.remove(index);            

        } while (((StreetName)streets.get(new Integer(rs.getStreetIndex()))).toString().equals(
                ((StreetName)streets.get(new Integer(smir.current.getStreetIndex()))).toString()) 
                && !ll.isEmpty());
        
        // corner case: new road has same name
        if (ll.isEmpty())
        {
        	is = intersections.findIntersectingRoads(smir.rsEnd);
        	ll = is.getRoads();
        	do
            {
                index = rnd.nextInt(ll.size());                
                rs = (RoadSegment)ll.get(index);                
                ll.remove(index);                
                //if (DEBUG) System.out.println("possible street: " + streets[rs.getStreetIndex()].toString());
            } while (rs.getStartAddressLeft() != smir.current.getStartAddressLeft() && ll.size()>0);
        }

	    updateNextRoadSegment(smir, rs);	                
        
        if (DEBUG) System.out.println(" onto " + (StreetName)streets.get(new Integer(smir.nextRS.getStreetIndex())));
    }    
}

