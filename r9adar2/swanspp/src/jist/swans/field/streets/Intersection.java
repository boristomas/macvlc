/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         Intersection.java
 * RCS:          $Id: Intersection.java,v 1.1 2007/04/09 18:49:41 drchoffnes Exp $
 * Description:  Intersection class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 17, 2004
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

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.streets.RoadSegment;
import jist.swans.misc.Location;
import jist.swans.field.StreetMobility;
import jist.swans.field.StreetMobilityInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import driver.VisualizerInterface;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Intersection class represents a convergence of one or 
 * more RoadSegments. This class maintains information regarding 
 * numbers of streets and types of traffic control in addition to 
 * performing the traffic control itself.
 */
public class Intersection {
	/** segments at this intersection */
    LinkedList segments;
    /** Location of this intersection. */
    final Location loc;
    /** number of unique streets at intersection */
    int numberOfStreets=0;
    /** street indexes, for access to RoadSegment objects */
    Vector streetIndexes = new Vector();
    /** debug switch */
    final static boolean DEBUG = false;
    /** true if showing debugging visually */
    private static final boolean DEBUG_VIS = true;
    
    /** constants for intersection types */
    int numRural = 0;
    int numSecondary = 0;
    int numUSHighway = 0;
    int numInterstate = 0;
    int numRamps = 0;
    
    /* synchronization fields for stop sign interaction */
    int currentStreet = -1;
    boolean occupied = false;
    /** Queue of nodes waiting to cross intersection */
    LinkedList waiting = new LinkedList();
    /** SMI for vehicle crossing intersection */
    private StreetMobilityInfo occupier = null;
    
    /** The time to spend stopped when there is no contention at intersection. */
    private static final int STOP_SIGN_PAUSE_NO_CONTENTION = 1;
    /** the time for a vehicle to clear the intersection */
    private static final int STOP_SIGN_WAIT = 3;
    
    /**
     * The intersection constructor. 
     * 
     * @param loc the location of the intersection
     */
    public Intersection(Location loc) {
        super();
        this.loc = loc;
        this.segments = new LinkedList();
    }
    
    /**
     * Adds a street to the intersection and updates data structures.
     * 
     * @param rs the RoadSegment to add.
     */
    public void addStreet(RoadSegment rs)
    {
    	/* for counting unique streets */
        RoadSegment next; 
        boolean found = false;
        
        segments.add(rs); // add to list   
                
        // increment number of unique streets 
        ListIterator it = segments.listIterator();
        while (it.hasNext())
        {
            next = (RoadSegment)it.next();
            if (next.streetIndex==rs.streetIndex && next.selfIndex != rs.selfIndex)
            {                
                found = true;
                break;
            }
        }
        if (!found)
        {
            streetIndexes.add(new Integer(rs.streetIndex));
            numberOfStreets++;
            incrementRoadType(rs.getRoadClass());
        }
    }
    
    /**
     * Increments counters according to road type.
     * @param roadClass the character representing the road class.
     */
    private void incrementRoadType(char roadClass) {
        // Primary Road with limited access/ Interstate Highway - unseparated
        if (roadClass >= 11 && roadClass <= 14)
           numInterstate++;

        // Primary Road with limited access/ Interstate Highway - separated
        else if (roadClass >= 15 && roadClass <= 18)
            numInterstate++;

        // Primary Road without limited access/ US Highway - unseparated
        else if (roadClass >= 21 && roadClass <= 24)
            numUSHighway++;

        // Primary Road without limited access / US Highway - separated
        else if (roadClass >= 25 && roadClass <= 28)
            numUSHighway++;

        // Secondary and Connecting Roads / State Highways - unseparated
        else if (roadClass >= 31 && roadClass <= 34)
            numSecondary++;

        // Secondary and Connecting Roads / State Highways - separated
        else if (roadClass >= 35 && roadClass <= 38)
            numSecondary++;
        
        // Local, Rural, Neighborhood / City Street - unseparated
        else if (roadClass >= 41 && roadClass <= 44)
            numRural++;

        // Local, Rural, Neighborhood / City Street - separated
        else if (roadClass >= 45 && roadClass <= 48)
            numRural++;
        // access ramp
        else if (roadClass >=62 && roadClass <=63)
            numRamps++;
        else 
           System.err.println("Unknown road class " + (int)roadClass + " encountered\n");
        
    }

    /**
     * Returns the pause time at this intersection for the vehicle 
     * described by "smi". Anything above zero means that the vehicle 
     * had better slow down before reaching the intersection. This method 
     * defines the traffic control types based on the types of roads at 
     * the intersection. It would be nice to have per-intersection traffic 
     * control info...
     * 
     * @param current the current road segment
     * @param next the next road segment after crossing the intersection
     * @param smi the vehicle's SMI object
     * @return the number of seconds to pause
     */
    public float getPauseTime(RoadSegment current, RoadSegment next, StreetMobilityInfo smi)
    {
    	// determine whether the vehicle is turning
        boolean turn=(current.getStreetIndex()!=next.getStreetIndex());
        
        
        // Local, Rural, Neighborhood / City Street - unseparated
        // Local, Rural, Neighborhood / City Street - separated
        if (current.roadClass >= 41 && current.roadClass <= 48)
        {
            if (next.roadClass >=62 && next.roadClass <=63)
                return 0; // no pause for ramps
            else // otherwise, there is a stop sign
            {                   
               return stopSign(current, next, smi);                
            }
        }

        // Secondary and Connecting Roads / State Highways - unseparated
        // Secondary and Connecting Roads / State Highways - separated
        else if (current.roadClass >= 31 && current.roadClass <= 38)
        {
            if (next.roadClass >=62 && next.roadClass <=63)
                return 0; // no pause for ramps
            else // otherwise, pause for stoplight time
            {
                // bias in favor of highway
                if (numUSHighway > 0 || numInterstate > 0)
                {
                    return stopLightPause(30, 0.75, false);
                }
                // equal time
                else if (numSecondary > 1)
                {
                    return stopLightPause(30, 0.5, true);
                }
                else  // no light for lesser roads
                {
                    // TODO there should be no stoplight...the intersecting road 
                    // should have a stop sign.
                    return 0;
                }
            }
        }

        // Primary Road without limited access/ US Highway - unseparated
        // Primary Road without limited access / US Highway - separated
        else if (current.roadClass >= 21 && current.roadClass <= 28)
        {
            if (next.roadClass >=62 && next.roadClass <=63)
                return 0; // no pause for ramps
            else // otherwise, pause for stoplight time
            {
                // equal stop time
                if (numUSHighway > 1 || numInterstate > 0)
                {
                    return stopLightPause(30, 0.5, true);
                }
                else if (numSecondary > 0)  // bias light in favor of highway
                {
                    return stopLightPause(30, 0.75, true);
                }
                else
                {
                    return 0; // no stopping for lesser streets
                }
            }
        }
        
        // Primary Road with limited access/ Interstate Highway - unseparated
        // Primary Road with limited access/ Interstate Highway - separated
        else if (current.roadClass <= 18)
        {
           if (!turn || next.roadClass >=62 && next.roadClass <=63)
               return 0; // no pause for ramps or staying on same road
           else // otherwise, pause for stoplight time
           {
               // equal stop time
               if (numUSHighway > 0 || numInterstate > 1)
               {                
                   return stopLightPause(30, 0.5, true);
               }
               else if (numSecondary > 0)  // bias light in favor of highway
               {
                   return stopLightPause(30, 0.75, true);
               }
               else
               {
                   return 0; // no stopping for lesser streets
               }
           }
        } // end primary road


        // access ramp
        else if (current.roadClass >=62 && current.roadClass <=63)
        {
            if (next.roadClass >=62 && next.roadClass <=63)
                return 0; // no pause for ramps
            else // otherwise, pause for stoplight time
            {
                // no pause for highway
                if (next.roadClass <=28)
                {
                    return 0;
                }
                else  // stop sign
                { // TODO implement stop lights at secondary roads?
                    return stopSign(current, next, smi);
                }
            }
        }
        else 
           System.err.println("Unknown road class " + current.roadClass + " encountered\n");
        	  return 0;
       
    }
    
    /**
     * Enables real-world stop sign behavior. Vehicles traveling parallel to each 
     * other are all allowed to go at once. Otherwise, this method produces 
     * lockstep behavior around parallel roads under contention.
     * 
     * @param oldRS
     * @param next
     * @param smi
     * @return
     */
    private int stopSign(RoadSegment oldRS, RoadSegment next, StreetMobilityInfo smi) 
    {
        int pause;
        VisualizerInterface v = smi.v;
        
        if (DEBUG_VIS && v!= null)
        {
            v.drawCircle(50, loc);
        }
        
        // tried to cross, but no room. Add to the waiting list.
        if (occupier!=null && occupier.waiting == true )
        {
            if (!waiting.contains(occupier)) waiting.add(occupier);
            occupied = false;
            occupier = null;
            // set next street to move; this ensures total ordering under contention
            int nextIndex = streetIndexes.indexOf(new Integer(currentStreet))+1;
            nextIndex = nextIndex % streetIndexes.size();
            currentStreet = ((Integer)streetIndexes.get(nextIndex)).intValue();
        }
        
        // node actually at intersection
        if (smi.getRemainingDist()< RoadSegment.CAR_LENGTH/2)
        {            
            // must wait
            if (occupied && currentStreet != oldRS.streetIndex && occupier != smi) // not your turn
            {
                if (DEBUG) System.out.println("Stopping because it is not my turn!");
                pause = STOP_SIGN_WAIT;
                // check if vehicle is currently in list
                if (!waiting.contains(smi))
                {
                    waiting.add(smi);
                }
                
                // determine if the current set of cars have cleared the intersection
                if (occupier != null)
                {
                    if (occupier.getNextIntersection() != this )
                    {
                        occupied = false;
                        occupier = null;
                        // set next street to move; this ensures total ordering under contention
                        int nextIndex = streetIndexes.indexOf(new Integer(currentStreet))+1;
                        nextIndex = nextIndex % streetIndexes.size();
                        currentStreet = ((Integer)streetIndexes.get(nextIndex)).intValue();
                    }
                } // end if intersection is occupied
            } // end case not this lane's turn
            else if (!occupied && currentStreet != oldRS.streetIndex) // ok, whose turn is it?
            {
                if (waiting.size() > 1) // more than just the current car
                {
                    if (((StreetMobilityInfo)waiting.getFirst()).getNextIntersection() != this)
                    {
                        waiting.removeFirst();
                    }
                    if (oldRS.streetIndex != ((StreetMobilityInfo)waiting.getFirst()).getCurrentRS().streetIndex 
                            //&& ((StreetMobilityInfo)waiting.getFirst()).waiting == false
                            )
                    {
                        if (DEBUG) System.out.println("Not occupied, but stopping because it is not my turn!");
                        // now wait
                        // check if vehicle is currently in list
                        if (!waiting.contains(smi))
                        {
                            waiting.add(smi);
                        }
                        pause = STOP_SIGN_WAIT;
                    }
                    else // you can go because the first in the list is waiting for the same street as you
                    {                        
                        pause = 0;
                	    currentStreet = oldRS.streetIndex;
                        occupied = true;
                        occupier = smi;
                        while (waiting.remove(smi)){;};
                    }
                
                } // end case not occupied, but current lane is not chosen for going
                else // no one is waiting, so just go
                {
                    pause = 0;
            	    currentStreet = oldRS.streetIndex;
                    occupied = true;
                    occupier = smi;
                    while (waiting.remove(smi)){;};
                }
                
            }
            else // no wait
            {
                pause = 0;
        	    currentStreet = oldRS.streetIndex;
                occupied = true;
                occupier = smi;
                
                while (waiting.remove(smi)){;};
            }
                                   
        } // end case node at intersection
        else // node determining if it needs to slow down before intersection
        {
            pause = STOP_SIGN_PAUSE_NO_CONTENTION;
            
            // determine if the current set of cars have cleared the intersection
//            if (occupier != null)
//            {
//                if (occupier.getNextIntersection() != this )
//                {
//                    occupied = false;
//                    occupier = null;
//                    // set next street to move; this ensures total ordering under contention
//                    int nextIndex = streetIndexes.indexOf(new Integer(currentStreet))+1;
//                    nextIndex = nextIndex % streetIndexes.size();
//                    currentStreet = ((Integer)streetIndexes.get(nextIndex)).intValue();
//                }
//            }
        }
              
        if (DEBUG_VIS && v!= null)
        {
            v.removeCircle();
        }
	    return pause; // ensures that vehicles know that they will have to stop
    }

    /**
     * Note that all stop lights are changing at the exact same time. Bad idea, 
     * but simple to implement.
     * 
     * @param period number of seconds that each road gets to move freely if 
     * "green light" time is distributed equally 
     * @param bias percent of period that light is green for caller
     * @param longer True if the caller will get a longer light
     * @return the pause time
     */
    
    public float stopLightPause(int period, double bias, boolean longer)
    {
        int retVal;
    	period*=(numInterstate+numSecondary+numUSHighway);
        long modTime = (JistAPI.getTime()%(period*Constants.SECOND))/Constants.SECOND;
        if (longer)
        {
	        if (modTime <= period*bias) retVal = 0;
	        else retVal = Math.abs((int)(period-modTime));
        }
        else
        {
	        if (modTime > period*bias) retVal = 0;
	        else retVal = Math.abs((int)(period*bias - modTime));
        }
        
        if (DEBUG) 
        {
        	System.out.println("Pause time: "+retVal);
            if (numberOfStreets > 2) System.out.println("More than 2 streets!");
        }
        return retVal;
    }
    
    /**
     * Returns the roads at this intersection.
     * @return a LinkedList of RoadSegments.
     */
    public LinkedList getRoads()
    {
        return (LinkedList)segments.clone();
    }
    
    /**
     * @return Returns the location of the intersection.
     */
    public Location getLoc() {
        return loc;
    }
    
    /**
     * @return Returns the number of streets at this intersection.
     */
    public int getSize() {
        return numberOfStreets;
    }

    /**
     * Prints the streets at this intersection
     * @param streets the HashMap of street names.
     * @return The String to print.
     */
    public String printStreets(HashMap streets) {
        String s = "\nLocation: " + loc + "\nStreet list:\n----------\n";
        Iterator it = segments.iterator();
        while (it.hasNext())
        {
            s+=((RoadSegment)it.next()).printStreetName(streets)+"\n";
        }
        
        return s;
    }

    /**
     * Removes a vehicle from the list of those waiting.
     * @param smi The vehicle to remove.
     */
    public void removeWaiting(StreetMobilityInfo smi) {
        waiting.remove(smi);  
        if (smi == occupier) {
            occupier = null;
            occupied = false;
        }
            
    }

}
