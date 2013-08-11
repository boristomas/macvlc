/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetMobilityOD.java
 * RCS:          $Id: StreetMobilityOD.java,v 1.1 2007/04/09 18:49:28 drchoffnes Exp $
 * Description:  StreetMobilityOD class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Feb 22, 2005
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.streets.CongestionMonitor;
import jist.swans.field.streets.RoadSegment;
import jist.swans.field.streets.SegmentNode;
import jist.swans.field.streets.SegmentNodeInfo;
import jist.swans.misc.AStarSearch;
import jist.swans.misc.Location;

/**
 * 
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StreetMobilityOD class supports mobility between origins 
 * and destinations.
 */
public class StreetMobilityOD extends StreetMobility
{
    
    /**
     * 
     * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
     *
     * The StreetMobilityInfoOD class extends the StreetMobilityInfo 
     * state object by including the origin, destination and path.
     */
    public static class StreetMobilityInfoOD extends StreetMobilityInfo
    {
        /** node's destination road segment */
        SegmentNode destinationSN = null;
        /** node's destination */        
        Location destinationLocation = null;
        /** a linked list of road segments along the path */
        public LinkedList path = null; 
        /** Determines the mode for OD motion. For example, origins and destinations 
         * can be picked at random or fed from a list of sources and sinks. */
        int config;
        /** for vehicles following a circuit, the index of the route */
        public int routeIndex;
        
    }

    /** the file with the routes */
    public String file;
    /** the list of roads */
    public LinkedList[] routes = null;
    /** node's origin road segment */
    Vector originRS = new Vector();
    /** node's origin address */
    Vector originAddress = new Vector();
    /** node's destination road segment */        
    Vector destinationRS = new Vector();
    /** node's destination address */
    Vector destinationAddress = new Vector();
    /** cache of SegmentNodes */
    Vector segmentNodes;
    /** for caching paths, not currently implemented */
    HashMap hm = new HashMap();
    /** Helper class for referencing StreetMobility objects in the AStarSearch class. */
    SegmentNodeInfo sni = null;
    /** Prints SMOD-specific messages, if true */
    private static final boolean DEBUG_OD = false;
    /** The minimum path to cache. */
    private static final int MIN_CACHED_PATH_LENGTH = 5;
    /** determine whether to use caching */
    private static final boolean USE_CACHING = false;
    /** whether to recalculate shortest path at every intersection */
    private static final boolean RECALCULATE_ALWAYS = false;
    private static final boolean DEBUG_VIS_OD = false;
    /** maximum distance between origin and destination (5 mi) 
     * This reduces the time required for A* */
//    float threshold = 8046.72f; 
    float threshold = Float.MAX_VALUE;
    
    /**
     * StreetMobilityOD constructor.
     * 
     * @param segmentFile the location of the file containing segments
     * @param streetFile the location of the file containing streets
     * @param shapeFile the location of the file containing shapes
     * @param degree the degree of the quad tree
     * @param r the random object to use for repeatability
     */
    public StreetMobilityOD(String segmentFile, String streetFile,
            String shapeFile, int degree,
            Location.Location2D bl, Location.Location2D tr, Random r) {
        super(segmentFile, streetFile, shapeFile, degree, bl, tr);
        this.rnd = r;
        
        segmentNodes = new Vector(segments.size()); // for caching segmentNode objects
        for (int i = 0; i < segments.size(); i++)
        {
            segmentNodes.add(i, null);
        }
        sni = new SegmentNodeInfo(segments, shapes, intersections, streets, segmentNodes);
        SegmentNode.info = sni;
        if (USE_CACHING) hm = new HashMap();
    }
    
    /**
     * StreetMobilityOD constructor.
     * 
     * @param segmentFile the location of the file containing segments
     * @param streetFile the location of the file containing streets
     * @param shapeFile the location of the file containing shapes
     * @param degree the degree of the quad tree
     * @param r the random object to use for repeatability
     * @param circuitFile the file to use for loading circuit routes
     */
    public StreetMobilityOD(String segmentFile, String streetFile,
            String shapeFile, int degree, Location.Location2D bl, 
            Location.Location2D tr, Random r, String circuitFile) {
        
        this(segmentFile, streetFile, shapeFile, degree, bl, tr, r);
        file = circuitFile;
    }
       
    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#init(jist.swans.field.FieldInterface, java.lang.Integer, jist.swans.misc.Location)
     */
    public MobilityInfo init(FieldInterface f, Integer id, Location loc) {
        
        StreetMobilityInfoOD smodi = (StreetMobilityInfoOD)mobInfo.lastElement();
        // calculate first path
        if (smodi.path == null)
        {
            calculatePath(smodi, (RoadSegment)originRS.get(id.intValue()), smodi.rsEnd,
                    (RoadSegment)destinationRS.get(id.intValue()));
        }
        smodi.v = v; // set visualizer object
        return smodi;
    }
    
    /* (non-Javadoc)
     * @see jist.swans.field.StreetMobility#setNextRoad(jist.swans.field.StreetMobilityInfo)
     */
    public void setNextRoad(StreetMobilityInfo smi) {
       
        StreetMobilityInfoOD smiod= (StreetMobilityInfoOD)smi;
        
        
        if (smiod.path.size() > 0 || smiod.config == Constants.MOBILITY_STREET_RANDOM)
        {
            // get next intersection
            smiod.nextIs = intersections.findIntersectingRoads(smiod.rsEnd);        

            if (smiod.path.size() == 0 || 
                    (RECALCULATE_ALWAYS && JistAPI.getTime() > 30*Constants.SECOND && smiod.path.size()>2 && smiod.speedSum>0)) // calculate new path
            {
                // indicate that node should be taken off map
                if (smiod.config==Constants.MOBILITY_STREET_FLOW && smiod.path.size() == 0){
                    smi.nextRS = null;
                    return;
                }
                
                if (DEBUG_OD) System.out.println("Calculating new path...");
                // recalculate path based on congestion info
                if (smiod.path.size() > 2 && smiod.nextEnd!=null)
                {
                    LinkedList oldPath = (LinkedList) smiod.path.clone();
                    SegmentNode nextOne = (SegmentNode) smiod.path.getFirst();
                    RoadSegment origin = (RoadSegment)segments.get(nextOne.segmentID);
                    SegmentNode dest = smiod.destinationSN;
                    RoadSegment rs = (RoadSegment)segments.get(smiod.destinationSN.segmentID);
                    
                    calculatePath(smiod, smi.current, smi.rsEnd, rs);
                    
                    if (!smiod.path.getLast().equals(dest)) throw new RuntimeException("Destination altered!");
                    
                    if (smiod.current.equals((RoadSegment)segments.get(((SegmentNode)smiod.path.getFirst()).segmentID))) 
                        smiod.path.remove();
                    if (smiod.path.getFirst().equals(nextOne)) ;
                    else {
                        if (DEBUG_VIS_OD && v!=null){
                        v.drawCircle(20, smiod.rsEnd /*(smiod.rsEnd.distance(smi.current.getEndPoint())==0 
                                ? smi.current.getStartPoint() : smi.current.getEndPoint())*/);
                        oldPath.addLast(new SegmentNode(smi.rsEnd, smi.current.getSelfIndex(), true, true));
                        smiod.path.addLast(new SegmentNode(smi.rsEnd, smi.current.getSelfIndex(), true, true));
                        showPath(oldPath.toArray(), Color.RED);
                        showPath(smiod.path.toArray(), Color.ORANGE);
                        smiod.path.removeLast();
                        System.out.println("Foo!");
                        }
                    }
                    checkPath(smiod);
                    if (smiod.current.equals((RoadSegment)segments.get(((SegmentNode)smiod.path.getFirst()).segmentID))) 
                        smiod.path = oldPath;
                    
                }
                else{
                    // this section makes sure that a valid path is found:
                    // 1) there must be a path from origin to destination on the map
                    // 2) the path must be longer than 4 so that it's substantial
                    boolean valid = false;     
                    while (!valid || smiod.path.size() < 4)
                    {
                        RoadSegment nextDest = null;
                        int segmentSize = sni.segment.size();
                        
                        while (nextDest == null || 
                                nextDest.getSelfIndex()==smiod.current.getSelfIndex()
                                || nextDest.getEndPoint().distance(smiod.current.getEndPoint()) > threshold) // get new destination
                        {
                            int i = rnd.nextInt(segmentSize); 
                            nextDest = (RoadSegment)sni.segment.get(i);
                        }
                        valid = calculatePath(smiod, smi.current, smi.rsEnd, nextDest);   
                        
                    }
                    checkPath(smiod);
                }
                if (DEBUG_OD)System.out.println("Current rsEnd: " + smi.rsEnd);
                if (DEBUG_OD)System.out.println("Current road: " + smi.current.printStreetName(sni.streets));   
                
                
              
            }
            
            SegmentNode sn = (SegmentNode)smiod.path.get(0);
            RoadSegment rs = (RoadSegment)segments.get(sn.segmentID);
            updateNextRoadSegment(smiod, rs); // update next road
            smiod.path.remove(0); // already used the first entry
                        
            while (smiod.path.size() > 0 )
            {
                // weird intersection type that confuses A*
                // getting on a street for no distance...
                sn = (SegmentNode)smiod.path.get(0); // was zero
                rs = (RoadSegment)segments.get(sn.segmentID);
                if (smiod.rsEnd.distance(rs.getStartPoint())==0 ||
                        smiod.rsEnd.distance(rs.getEndPoint())==0)
                {
                    if (DEBUG_OD)System.out.println("rsEnd: "+smiod.rsEnd);
                    if (DEBUG_OD)System.out.println("Current street: " +rs.printStreetName(sni.streets));
                    
                    if (DEBUG_OD)System.out.println("Fixing A* bug...");
                    updateNextRoadSegment(smiod,rs);
                    smiod.path.remove(0);
                    
                }
                else
                {
                    break;
                }
            }
        }
        else if (smiod.config == Constants.MOBILITY_STREET_FLOW)
        {
             smiod.nextRS = null;
             smiod.nextEnd = null;
        }
        else if (smiod.config == Constants.MOBILITY_STREET_CIRCUIT)
        {
        	smiod.path = (LinkedList)routes[smiod.routeIndex].clone();
            SegmentNode sn = (SegmentNode)smiod.path.get(0);
            RoadSegment rs = (RoadSegment)segments.get(sn.segmentID);
            updateNextRoadSegment(smiod, rs); // update next road
            smiod.path.remove(0); // already used the first entry
        }
    }


    /**
     * Caches the path that was just found.
     * @param smiod
     * @param endNode 
     * @param startNode 
     */
    private void cachePath(StreetMobilityInfoOD smiod, SegmentNode startNode, SegmentNode endNode) {
        if (hm!=null && smiod.path.size() > MIN_CACHED_PATH_LENGTH)
        {               
            Location startPoint = startNode.point;

            LinkedList ll = (LinkedList)hm.get(startPoint);
            if (ll == null)
            {
                ll = new LinkedList();
            }
            // make sure that path doesn't already exist... 
            // must iterate through all of them...
            ListIterator li = ll.listIterator();
            boolean found = false;
            while (li.hasNext())
            {
                LinkedList existingPath = (LinkedList)li.next();
                if (existingPath.getFirst().equals(startNode) && 
                        existingPath.getLast().equals(smiod.path.getLast()))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                LinkedList newPath = (LinkedList)smiod.path.clone();
                newPath.addFirst(startNode);
                ll.add(newPath);
                hm.put(startPoint, ll);
            }
            
        }
    }


    /**
     * @param smiod
     */
    public void checkPath(StreetMobilityInfoOD smiod) {
        SegmentNode temp;
        RoadSegment rsTemp;
        // if the current end point is equal to an end point of the second road in 
        // the list, then remove the first road in the list because it's redundant
        if (smiod.path.size()>1){
            temp = (SegmentNode)smiod.path.get(1);
            rsTemp = (RoadSegment)segments.get(temp.segmentID);
            if (smiod.rsEnd.distance(rsTemp.getStartPoint())==0 ||
                    smiod.rsEnd.distance(rsTemp.getEndPoint())==0)
            {
                if (DEBUG_OD)System.out.println("Removing first node in list because it is redundant!");
                smiod.path.remove(0);
                
            }
        }
        
        // case where the node is about to start moving away from the right intersection
        // diagnosis: current location is equal to an end point on list
        // solution: update "current rs" and "rsEnd" with next value from path
        // then remove first entry in path
//        Location loc = smiod.rsEnd;
//        if (DEBUG_OD)System.out.println("Current location: "+loc);
//        temp = (SegmentNode)smiod.path.get(1);
//        rsTemp = (RoadSegment)segments.get(temp.segmentID);
//        if (loc.distance(rsTemp.getEndPoint())<= StreetMobility.INTERSECTION_RESOLUTION )
//        {
//            if (DEBUG_OD)System.out.println("Avoiding U-turn!");
//            smiod.current = rsTemp;
//            smiod.rsEnd = rsTemp.getStartPoint();
//            smiod.path.remove(0);
//            smiod.path.remove(0);
//        }
//        else if (loc.distance(rsTemp.getStartPoint())<=StreetMobility.INTERSECTION_RESOLUTION)
//        {
//            if (DEBUG_OD)System.out.println("Avoiding U-turn!");
//            smiod.current = rsTemp;
//            smiod.rsEnd = rsTemp.getEndPoint();
//            smiod.path.remove(0);
//            smiod.path.remove(0);
//        }
        
        // case where node is already at intersection for first segment on path
        // diagnosis: current location is equal to one of the points in the first segment

//        temp = (SegmentNode)smiod.path.get(0);
//        rsTemp = (RoadSegment)segments.get(temp.segmentID);
//        
//        if (loc.distance(rsTemp.getEndPoint())<=StreetMobility.INTERSECTION_RESOLUTION )
//        {
//            if (DEBUG_OD)System.out.println("Moving immediately to next segment!");
//            smiod.current = rsTemp;
//            smiod.rsEnd = rsTemp.getStartPoint();
//            smiod.path.remove(0);
//        }
//        else if (loc.distance(rsTemp.getStartPoint())<=StreetMobility.INTERSECTION_RESOLUTION)
//        {
//            if (DEBUG_OD)System.out.println("Moving immediately to next segment!");
//            smiod.current = rsTemp;
//            smiod.rsEnd = rsTemp.getEndPoint();
//            smiod.path.remove(0);
//        }
        
            temp = (SegmentNode)smiod.path.get(0);
            rsTemp = (RoadSegment)segments.get(temp.segmentID);
            // need to add current road onto list to perform U-turn.
            if (smiod.rsEnd.distance(rsTemp.getEndPoint())> INTERSECTION_RESOLUTION 
                    && smiod.rsEnd.distance(rsTemp.getStartPoint())> INTERSECTION_RESOLUTION)
            {
                smiod.path.addFirst(new SegmentNode(smiod.rsEnd, smiod.current.getSelfIndex(), 
                        true, false));
            }
        
        
    }

    /**
     * This method calculates the path from the origin to the destination 
     * road segment.
     * @param smiod The SMIOD object.
     */
    public boolean calculatePath(StreetMobilityInfoOD smiod, RoadSegment origin, Location nextEnd,
            RoadSegment destination) {
        // TODO support loading from OD pairs    
        // after calling constructor, objects for A* search must be created
        boolean found = false;
        
        // TODO the caller should determine direction   
        if (DEBUG_OD)
        {
            System.out.println("Calculating path...");
            System.out.println("Origin:      " + origin.printStreetName(streets));
            System.out.println("Destination: " + destination.printStreetName(streets));
            System.out.println("Distance: " + origin.getStartPoint().distance(destination.getStartPoint()));
        }

        if (nextEnd == null){
            if (smiod.rsEnd.distance(origin.getEndPoint())==0)
            {
                nextEnd = origin.getEndPoint();
            }
            else
            {
                nextEnd = origin.getStartPoint();
            }
        }
        if (DEBUG_OD && DEBUG_VIS_OD && v!=null)
        {
            v.colorSegment(origin, Color.RED);
        }
        
        boolean endStart;
        Location endPoint;
        if (destination.getStartPoint().distance(nextEnd)
                < destination.getEndPoint().distance(nextEnd))
            {
             endPoint = destination.getStartPoint();
             endStart = true;
            }
        else {
            endPoint = destination.getEndPoint();
            endStart = false;
        }
                
        
        SegmentNode startNode = new SegmentNode(nextEnd, 
                origin.getSelfIndex(), origin.getStartPoint().distance(nextEnd)==0, true);        
        SegmentNode endNode = new SegmentNode(endPoint, 
                destination.getSelfIndex(), endStart, false);   
        sni.dest = endNode;
        
        // try to find cached path
        if (hm!=null){
            LinkedList ll = (LinkedList)hm.get(nextEnd);
            if (ll!=null)
            {
                ListIterator li = ll.listIterator();
                
                while (li.hasNext())
                {
                    LinkedList path = (LinkedList)li.next();
                    if (!matches(startNode, endNode, path))
                    {
                        continue;
                    }
                    smiod.path = (LinkedList)path.clone();
                    smiod.path.removeFirst();
                    if (DEBUG_OD)System.out.println("Found cached path!");
                    return true; 
//                    break;
                }
            }
        }
        if (!found) {

//      TODO support locations at arbitrary points on road
        smiod.destinationSN = endNode;
        smiod.destinationLocation = destination.getEndPoint();         
        
        AStarSearch ass = new AStarSearch(hm, this); // TODO rename variable
        smiod.path = ass.findPath(startNode, endNode); // find the path
        }
        
        // no path found
        if (smiod.path.get(0) == null)
         {
            AStarSearch ass = new AStarSearch(hm, this); // TODO rename variable
            smiod.path = ass.findPath(startNode, endNode); // find the path
            if (v!=null){
                v.colorSegments(new RoadSegment[]{origin, destination}, new Color[]{Color.RED, Color.RED});
            }
            smiod.path.remove(0);
            System.out.println("No path found!");
            return false;       
        }
        
        if (DEBUG_VIS_OD && v!=null)
        {
            showPath(smiod.path.toArray(), Color.BLUE);
        }
        
        // check for strange double entry in list     
        for (int i = 1; i < smiod.path.size(); i++)
        {
            if (((SegmentNode)smiod.path.get(smiod.path.size()-i)).segmentID==
                    ((SegmentNode)smiod.path.get(smiod.path.size()-(i+1))).segmentID)
            {
                if (DEBUG_OD)System.out.println("Removed redundant entry!");
                smiod.path.remove(smiod.path.size()-i);
            }
        }
        
        if (DEBUG_OD) printPath(smiod.path);
        if (hm!=null) cachePath(smiod, startNode, endNode);
                
        return true; // path found!
    }
    
    private boolean matches(SegmentNode startNode, SegmentNode endNode, 
            LinkedList path) {
        SegmentNode pathStart = ((SegmentNode)path.getFirst());
        SegmentNode pathEnd = ((SegmentNode)path.getLast());
        
        if( startNode.segmentID == pathStart.segmentID 
        && endNode.segmentID == pathEnd.segmentID)
        {
            if(startNode.point.distance(pathStart.point)==0)
            {
                if (startNode.start != pathStart.start)
                    return false;
            }
            else if (startNode.start == pathStart.start)
                return false;
            
            if(endNode.point.distance(pathEnd.point)==0)
            {
                if (endNode.start != pathEnd.start)
                    return false;
            }
            else if (endNode.start == pathEnd.start)
                return false;
        }
        else return false;
        
        return true;
    }

    /**
     * Prints the list of roads along a path.
     * @param path The list of roads to print.
     */
    public void printPath(List path)
    {
        System.out.println("Path: ");
        Iterator it = path.iterator();
        while (it.hasNext())
        {
            SegmentNode sn = (SegmentNode)it.next();
            RoadSegment rs = (RoadSegment)segments.get(sn.segmentID);
            System.out.println(rs.printStreetName(streets));
        }
    }

    public void setCongestionMonitor(CongestionMonitor cmi) {
        sni.cm = cmi;
        
    }
    
    /**
     * @param roads
     * @param colors
     * @param color 
     */
    public void showPath(Object[] roads, Color color) {
        Color colors[] = new Color[roads.length];
        RoadSegment rs[] = new RoadSegment[roads.length];
        SegmentNode sn;
        for (int j=0;j<roads.length;j++){
            sn = (SegmentNode)roads[j];
            rs[j] = (RoadSegment)segments.get(sn.segmentID);
            colors[j] = color;   
        }
        v.colorSegments(rs, colors);
    }
    
    /**
     * @param routes
     * @param mi
     * @return
     */
    public boolean isOnVFR(MobilityInfo mi) {
        StreetMobilityInfo smi = (StreetMobilityInfo)mi;
       SegmentNode sn = new SegmentNode(smi.current.getStartPoint(), 
       smi.current.getSelfIndex(), true, false);
        boolean colored = false;
        for (int i = 0; i<routes.length; i++){
            if (routes[i].contains(sn)){
                colored = true;
                break;
            }
        }
        return colored;
    }
}