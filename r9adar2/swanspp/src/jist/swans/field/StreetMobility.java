/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetMobility.java
 * RCS:          $Id: StreetMobility.java,v 1.1 2007/04/09 18:49:27 drchoffnes Exp $
 * Description:  StreetMobility class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Tue Dec 7, 2004
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

package jist.swans.field;				//already have newly created radar class via this package import


import java.awt.Color;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.StreetMobilityOD.StreetMobilityInfoOD;
import jist.swans.field.streets.Intersection;
import jist.swans.field.streets.LaneChangeModel;
import jist.swans.field.streets.RoadSegment;
import jist.swans.field.streets.SegmentNode;
import jist.swans.field.streets.Shape;
import jist.swans.field.streets.SpatialStreets;
import jist.swans.field.streets.StreetName;
import jist.swans.misc.AStarNode;
import jist.swans.misc.AStarSearch.PriorityList;
import jist.swans.misc.Location;
import driver.JistExperiment;
import driver.Visualizer;
import driver.VisualizerInterface;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StreetMobility class is the superclass for other street mobility classes.
 * Subclasses must define how nodes move about the street; StreetMobility
 * simply provides functions to load street data, calculate distances and move
 * nodes along streets.
 */
public abstract class StreetMobility implements Mobility {
    
    // T-ODO sleep time should be incremented by a matter of nanoseconds for 
    // according to the position of a vehicle in a lane. This ensures proper 
    // ordering of vehicles when updating
    
    /* Street mobility constants. */
    /** number of bytes in a road segment object */
    final int ROAD_SEGMENT_SIZE = 44;
    /** number of bytes in a street name object */
    final int STREET_NAME_SIZE = 38;
    /** Meters per degree. */
    public final static double METERS_PER_DEGREE = 110874.40;
    /** Degrees per meter. */
    final static double DEGREES_PER_METER = 1/METERS_PER_DEGREE;
    
    /** Maximum number of meters between two streets 
     * to be considered part of the same intersection.
     */
    public static final int INTERSECTION_RESOLUTION = 4;
    
    /* Street mobility data structures. */
    /** array of road segments */
    static Vector segments=new Vector();
    /** array of street names */
    static HashMap streets = new HashMap();
    /** array of shapes */
    static HashMap shapes = new HashMap();
    /** Quad-tree of road segments, for finding intersections quickly */
    static SpatialStreets.HierGrid intersections;
    
    /** contains indexes of used streets */
    static TreeMap usedStreets = new TreeMap();
    /** contains indexes of used shapes */
    static TreeMap usedShapes = new TreeMap();
    
    /** contains array of MobilityInfo objects */
    static Vector mobInfo = new Vector();
    static Vector neighborMobInfo = new Vector(); 			//Added to get the location of the neighbor in order to test the testRadar class
    /** uses HashMap to map ids to mobility objects */
    public static HashMap mobInfoHash = new HashMap();
    
    /** map boundary specified by user */
    private static Location.Location2D bl;			//bottom left boundary
    private static Location.Location2D tr;			//top right boundary
    
    private static Location.Location2D bl0;
    private static Location.Location2D tr0;
    /** will store the bounds of the map according to segments loaded */
    private static float maxX=(float)(-180*METERS_PER_DEGREE);
    private static float maxY=(float)(-180*METERS_PER_DEGREE);
    private static float minX=(float)(180*METERS_PER_DEGREE);
    private static float minY=(float)(180*METERS_PER_DEGREE);
    
    /** maximum number of cars allowed in region */
    int maxCars=0;
    
    /** pause switch */
    Integer carToInspect = new Integer(-1);
    
    /** random object */
    public Random rnd = new Random();
    /** the visualization object */
    public static VisualizerInterface v;
    
    /** the number of segments removed */
    int removedSegments = 0;
    
    /** the lane change model */
    LaneChangeModel lcm = null;
	private Location[] carLocsForRadarToCheck;
    private static StreetMobility myself;

    
    /* debugging constants */
    /** main debug switch */

    final static boolean DEBUG = true;					//DEBUG ORIGINALLY SET TO FALSE

    /** Records paths if true. */
    private static final boolean RECORD_STREETS = false;
    /** Displays real-time debugging info if true. */
    protected static final boolean DEBUG_VIS = true;
    /** Vehicles are displaced from center of road if true. */
    public static final boolean ENABLE_LANE_DISPLACEMENT = true;
    private static final int WAIT_THRESHOLD = 50;
    private static final boolean PRUNE_SEGMENTS = true;
    
    /**
     * 
     * StreetMobility constructor.
     * @param sm
     */
    public StreetMobility(StreetMobility sm)
    {
    	bl = sm.bl;
    	bl0 = sm.bl0;
    	intersections = sm.intersections;
    	maxCars = sm.maxCars;
    	maxX = sm.maxX;
    	maxY = sm.maxY;
    	minX = sm.minX;
    	minY = sm.minY;
    	mobInfo = sm.mobInfo;
    	removedSegments = sm.removedSegments;
    	rnd = sm.rnd;
    	segments = sm.segments;
    	shapes = sm.shapes;
    	streets = sm.streets;
    	tr = sm.tr;
    	tr0 = sm.tr0;
    	usedShapes = sm.usedShapes;
    	usedStreets = sm.usedStreets;
    	v = sm.v;
    	carToInspect = sm.carToInspect;
    	lcm = sm.lcm;
    	
    }
    
    /**
     * Street mobility constructor.
     * 
     * @param segmentFile segment file path
     * @param streetFile street name file path
     * @param shapeFile chain file path
     * @param degree number of spatial binning levels
     * @param bl - bottom left coordinate in long/lat
     * @param tr top right coordinate in long/lat
     */
    public StreetMobility(String segmentFile, String streetFile, 
            String shapeFile, int degree, Location.Location2D bl, Location.Location2D tr) 
    {
        if (myself!=null) throw new RuntimeException("Should not be instantiated twice!");
        RoadSegment rs;
        
        // convert from degrees to meters
        this.bl = new Location.Location2D((float)(bl.getX()*METERS_PER_DEGREE), (float)(bl.getY()*METERS_PER_DEGREE));			//bottom left coordinate
        this.tr = new Location.Location2D((float)(tr.getX()*METERS_PER_DEGREE), (float)(tr.getY()*METERS_PER_DEGREE));			//top right coordinate
        
        System.out.println("Specified region is " + this.bl + ", " + this.tr);					
        float x = Math.abs(this.bl.getX()-this.tr.getX());
        float y = Math.abs(this.bl.getY()-this.tr.getY());
        System.out.println("Specified area is " + x*y + ", Dimensions: (" + x +", " +
                y + ")");
        
        if (this.bl.getX()>this.tr.getX() || this.bl.getY() >this.tr.getY() )
            throw new RuntimeException("StreetMobility constructor: " +
            "Invalid boundaries!");
        
        loadSegmentsFile(segmentFile); // loads the segments
        loadStreetsFile(streetFile); // loads street names
        RoadSegment.streets = streets;
        
        if (DEBUG) System.out.println("Selected region contains " + 
                segments.size() + " segments");
        
        loadShapesFile(shapeFile); // loads shapes        
        
        // update bl and tr
        this.bl = new Location.Location2D(minX, minY);
        this.tr = new Location.Location2D(maxX, maxY);
        
        this.bl0 = new Location.Location2D(-2,-2);						
        this.tr0 = new Location.Location2D(maxX-minX+2, maxY-minY+2);		
        updateLocations();												//Rescales the x,y area to start at 0,0 in the bottom left to the size of the area in the top right ex: (500,500)
        
        if (DEBUG)  
            System.out.println("After loading streets, region is " 
                    + this.bl + ", " + this.tr);
        
        int spacing = INTERSECTION_RESOLUTION * 50;
        degree = (int)(Math.log(Math.max((maxX-minX)/spacing, (maxY-minY)/spacing))/Math.log(2));
        // creates the quad tree to contain the intersection objects
        intersections = new SpatialStreets.HierGrid(
                new Location.Location2D(0, 0), 
                new Location.Location2D(maxX-minX, 0), 
                new Location.Location2D(0, maxY-minY),
                new Location.Location2D(maxX-minX, maxY-minY), 
                degree, INTERSECTION_RESOLUTION);
        
        // insert each segment into the quad tree
        for (int i = 0; i < segments.size(); i++)
        {
            rs = (RoadSegment)segments.elementAt(i);
            // store segment length 
            rs.setLength((Shape)shapes.get(new Integer(rs.getShapeIndex())));
            
            maxCars += rs.getMaxCars()*rs.getNumberOfLanes()
            *rs.getNumberOfDirections();
            
            // this adds each road segment's end to 
            // a distinct intersection object
            intersections.add(rs, true);
            intersections.add(rs, false);	            
        }
        
        if (PRUNE_SEGMENTS)
        {
            System.out.println("Segments before pruning: "+segments.size());
            doPruning();
        }
        
        System.out.println("Maximum number of cars for region: "+maxCars);
        System.out.println("Number of segments loaded: "+segments.size());
        myself=this;
        
    }   

    /**
     * Moves coordinates to the (0,0) reference frame, 
     * the origin being in the top-left corner.
     */
    private void updateLocations() {
        Iterator it = segments.iterator();
        // fix segments
        while (it.hasNext())
        {
            RoadSegment rs = (RoadSegment)it.next();
            
            rs.setEndPoint(convertFromStreets(rs.getEndPoint()));
            rs.setStartPoint(convertFromStreets(rs.getStartPoint()));
        }
        
        // fix shapes
        it = shapes.values().iterator();
        while (it.hasNext())
        {
            Shape sh = (Shape)it.next();
            for (int i = 0; i < sh.points.length; i++)
            {
                sh.points[i] = convertFromStreets(sh.points[i]);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#init(jist.swans.field.FieldInterface, java.lang.Integer, jist.swans.misc.Location)
     */
    public abstract MobilityInfo init(FieldInterface f, Integer id, Location loc);
    
    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#next(jist.swans.field.FieldInterface, java.lang.Integer, jist.swans.misc.Location, jist.swans.field.Mobility.MobilityInfo)
     */
    public void next(FieldInterface f, Integer id, Location loc,			//get the locations on cars, get all segments in certain dist, getallneighbors--this is all cars for point inclusion in a triangle (radar area)
            MobilityInfo info) 
    {
    	
    	if (Visualizer.getActiveInstance()!=null) Visualizer.getActiveInstance().updateTime(JistAPI.getTime());
        LinkedList nextLaneTemp = null;
        Location displacement;
        Location centerLine;
        RoadSegment oldRs;
        
        try
        {
            
            StreetMobilityInfo smi = (StreetMobilityInfo)info;
            centerLine = loc.getClone();
            
            if(ENABLE_LANE_DISPLACEMENT){
                
                displacement = getInverseDisplacement(smi);  
                centerLine.add(displacement); // remove displacement from center
                
            }
            
            smi.speedSum+=smi.currSpeed; // update speed
            
            if (DEBUG_VIS && v!=null) v.setToolTip(id.intValue(), 
                    "Node " +id+":\nRemaining distance: "+smi.remainingDist);           
            
            // arrived at end of road, perform inter-segment calculations
            if(smi.remainingDist<0.01)
            {
                oldRs = smi.current;
                
                // this means that the vehicle is done moving and should be removed from the map
                if (smi.nextRS == null)
                {
                    Location.Location2D offMap = 
                        new Location.Location2D(Float.MAX_VALUE, Float.MAX_VALUE);
                    
                    smi.current.removeNode(smi, smi.currentLane, mobInfo); // remove from old one
                    
                    JistAPI.sleep(Constants.SECOND);                   
                    f.moveRadio(id, offMap); // make actual move                 
//                  JistAPI.sleep(2000 * Constants.SECOND); // don't call again for a long time                  
                    return;
                }                
                
                // find intersecting roads 
                Intersection is = intersections.findIntersectingRoads(smi.rsEnd);
                if (is == null)
                {
                    v.colorSegment(smi.current, Color.RED);
                    throw new RuntimeException("Null intersection error! Try reducing the degree of" +
                    " the quad tree");
                }
                
                // find out if we must stop at the intersection
                float pause = is.getPauseTime(smi.current, smi.nextRS, smi);
                // don't move at all if at red light or stop sign
                if (pause > 0)
                {                  
                    // pause at intersection if necessary 
                    JistAPI.sleep((long)(pause*Constants.SECOND));    
                    
                    if (ENABLE_LANE_DISPLACEMENT){
                        smi.offset = getLaneDisplacement(smi, null);
                        centerLine.add(smi.offset); // add displacement from center
                    }
                    f.moveRadio(id, centerLine); // make actual move
                    return;
                }           
                // simple case, waiting to make a turn
                else if (smi.waiting)
                {
                    // check if there is room to add car to road
                    // if not, wait for a second and exit
                    nextLaneTemp = smi.nextRS.addNode(smi, smi.nextEnd, mobInfo);
                    if (nextLaneTemp == null)
                    {
                        smi.waitCount++;
                        if (smi.waitCount > WAIT_THRESHOLD)
                        {
                            // turn around only if on random mode
                            if (smi instanceof StreetMobilityRandom.StreetMobilityInfoRandom) setNextRoad(smi);
                            
                        }
                        
                        JistAPI.sleep(Constants.SECOND); // T-ODO use turn signaling
                        
                        if (ENABLE_LANE_DISPLACEMENT){
                            smi.offset = getLaneDisplacement(smi, null);
                            centerLine.add(smi.offset ); // add displacement from center
                        }
                        f.moveRadio(id, centerLine); // make actual move
                        return;
                    }
                    else // the vehicle has been moved to the next segment
                    {
                        moveToNextRoad(id, nextLaneTemp, smi, is);
                        smi.currSpeed = 2.32f; // T-ODO: more dynamic
                    }
                    
                } // end waiting to turn            
                else // attempt to move node to next segment
                {
                    // check if there is room to add car to road
                    // if not, wait for a second and exit
                    nextLaneTemp = smi.nextRS.addNode(smi, smi.nextEnd, mobInfo);
                    if (nextLaneTemp == null)
                    {                    
                        smi.waiting = true;
                        smi.currSpeed = 0;
                        JistAPI.sleep(Constants.SECOND);
                        
                        if (ENABLE_LANE_DISPLACEMENT){
                            smi.offset = getLaneDisplacement(smi, null);
                            centerLine.add(smi.offset); // add displacement from center
                        }
                        
                        f.moveRadio(id, centerLine); // make actual move
                        if (v!=null) v.updateNodeLocation(centerLine.getX(), centerLine.getY(), id.intValue(), centerLine);
                        return;
                    }
                    else // there is room in the next segment
                    {
                        
                        // if there was a turn
                        if (smi.nextRS.getStreetIndex()!=smi.current.getStreetIndex())
                        {
                            // set new speed to 5mph
                            smi.currSpeed = 2.32f;
                        }
                        else if (smi.current.getSelfIndex() == smi.nextRS.getSelfIndex()) // dead end and turned around
                        {  
                            smi.currSpeed = 0;
                        }

                        moveToNextRoad(id, nextLaneTemp, smi, is);
                                        
                    }
                } // end case not waiting yet
                smi.waitCount = 0;
                // now that we've moved to the next street, we must calculate
                // motion-related info
                smi.setMaxSpeed(smi.current.getSpeedLimit());
                
                smi.ShapePointIndex = -1; // set new shapePointIndex
                // deal with segments
                Shape shape = null;
                if (smi.current.getShapeIndex()>0)
                {
                    shape = (Shape)shapes.get(new Integer(smi.current.getShapeIndex()));
                    
                    // if starting at segment "end", the first point in the shape 
                    // will be the one with the highest index
                    if(smi.current.getEndPoint().distance(centerLine)<= INTERSECTION_RESOLUTION)
                        smi.ShapePointIndex = shape.points.length; 
                } 
                
                // caculate distance along segment
                smi.remainingDist = smi.current.getLength(); 
                
                // move car to next segment
                Location newLoc = 
                    (smi.rsEnd.distance(smi.current.getEndPoint())==0 ? 
                            smi.current.getStartPoint() : smi.current.getEndPoint());
                
                
                if (newLoc.distance(centerLine) <= INTERSECTION_RESOLUTION)
                {
                    centerLine = newLoc.getClone();
                }
                else // vehicle tried to turn before reaching intersection
                {      
                    throw new RuntimeException("At " + JistAPI.getTime()+ ": Node attempted to turn to invalid road!");
                }
                
            } // end if at end of road segment        
            
            // advance simulation time
            // T-ODO make this speed-based
            JistAPI.sleep((long)(smi.stepTime*Constants.SECOND + 
                    + smi.currentLane.indexOf(smi))); // proper ordering    
            
            // adjust speed, look for car in front and move appropriately
            Location step = step(centerLine, smi, id);
            centerLine = step.getClone();
            
            if (ENABLE_LANE_DISPLACEMENT){
                smi.offset = getLaneDisplacement(smi, null);
                centerLine.add(smi.offset); // add displacement from center
            }
            
            f.moveRadio(id, centerLine); // make actual move  
            if (Visualizer.getActiveInstance()!=null) 
                Visualizer.getActiveInstance().updateNodeLocation(
                        centerLine.getX(), centerLine.getY(), id.intValue(), centerLine);
            
        }
        catch(ClassCastException e) 
        {
            // different mobility model installed
        }
        catch(RuntimeException e) // very useful for debugging
        {
            printStreetList(id.intValue());
            throw e;
        }
    }


	private boolean performLaneChange(StreetMobilityInfo smi, Integer id) {
		if (lcm!=null) return lcm.performLaneChange(smi, id);
		return false;
	}

	private void moveToNextRoad(Integer id, LinkedList nextLaneTemp, StreetMobilityInfo smi, Intersection is) {
		
		// reset any values from waiting
        smi.waiting = false;
        smi.waitCount = 0;
        is.removeWaiting(smi);	   

        smi.current.removeNode(smi, smi.currentLane, mobInfo); // remove from old one
        if (smi.currentLane.size()>0){
        	StreetMobilityInfo nextCar = (StreetMobilityInfo)smi.currentLane.getFirst();
        	if (nextCar.currentLane == smi.currentLane) nextCar.nextCar = null;
        }
        
        
        if (lcm!=null){
        	lcm.moveToNextRoad(id, smi, nextLaneTemp);
        }
        
        smi.currentLane = nextLaneTemp; // copy over the linked list
        smi.current = smi.nextRS; // update road features	                  
        smi.rsEnd = smi.nextEnd;	     
        
        // caculate distance along segment
        smi.remainingDist = smi.current.getLength();         
        setNextCar(smi);    // set the car in front

        if (smi.nextCar != null && smi.nextCar == smi)
        {
            throw new RuntimeException("Car is following itself!");
        }
        
        setNextRoad(smi); // set next road info        

        smi.current.checkLane(smi.currentLane); // T-ODO remove when stable
	}

    /**
     * @param smi
     * @return
     */
    Location getEndPoint(StreetMobilityInfo smi) {
        Location endPoint = smi.rsEnd;
        if (smi.current.getShapeIndex()>-1)
        {
            Location points[] = ((Shape)shapes.get(new Integer(smi.current.getShapeIndex()))).points;
            if (smi.ShapePointIndex < points.length -1 && smi.ShapePointIndex>=-1){
                endPoint = points[smi.ShapePointIndex+1];                        
            }
            else if (smi.ShapePointIndex == points.length)
                endPoint = points[smi.ShapePointIndex-1];
        }
        return endPoint;
    }

    /**
     * Prints debugging info graphically. 
     * 
     * @param id the vehicle id
     * @param smi the state object
     * @param is the current intersection
     */
    private void printSMInfo(Integer id, StreetMobilityInfo smi, Intersection is, Location l) {
        v.setFocus(id.intValue());
        v.drawCircle(50, l);
        v.resetColors();
        v.setNodeColor(id.intValue(), Color.GREEN);
        if (this instanceof StreetMobilityOD) ((StreetMobilityOD)this).showPath(((StreetMobilityInfoOD)smi).path.toArray(), Color.GREEN);
        String output = "Node "+id+": \n-----------\n";
        output +="Remaining distance: "+smi.remainingDist+"\n";
        output += "Current road: " + smi.current.printStreetName(streets)+"\n";
        output +="Next road: "+smi.nextRS.printStreetName(streets)+"\n";
        output += "Next road capacity: " + smi.nextRS.getMaxCars()+"\n";
        LinkedList nextLane = smi.nextRS.getEndPoint().distance(smi.nextEnd)==0 ?
                smi.nextRS.getCarsToEnd() : smi.nextRS.getCarsToStart();
                output+= "Next road occupancy: " + nextLane.size();
                output += "Next road info: " + smi.nextRS.printCarList(nextLane, mobInfo);
                if (is!=null) output += is.printStreets(streets);
                v.setGeneralPaneText(output);
                
    }
    
    /**
     * Gets the vector to remove a node's displacement from the center of the road.
     * 
     * @param info the SMI object for the node
     * @return a displacement vector
     */
    public Location getInverseDisplacement(StreetMobilityInfo smi) {
        
        Location l = smi.offset;
        return new Location.Location2D(l.getX() * (-1), l.getY() * (-1) );
    }
    
    /**
     * Gets the vector to add a node's displacement from the center of the road. 
     * Assumes that lane 0 is the inside lane regardless of direction. 
     * 
     * @param info the SMI object for the node
     * @param id the node id
     * @return a displacement vector
     */
    private static final float LANE_WIDTH = 3.6576f;
    public Location getLaneDisplacement(StreetMobilityInfo smi, Integer id55) {        
        
        Location start;
        Location finish;
        Location normalized;
        int laneNumber = smi.current.getLane(smi.currentLane);
        float displacement = LANE_WIDTH/2 + LANE_WIDTH * laneNumber;
        
        // calculate normal vector for displacement
        
        // simple case: no subsegments
        if (smi.current.getShapeIndex() < 0)
        {
            if (smi.rsEnd.distance(smi.current.getEndPoint())==0)
            {
                start = smi.current.getStartPoint();
                finish = smi.current.getEndPoint();
            }
            else
            {
                start = smi.current.getEndPoint();
                finish = smi.current.getStartPoint();
            }
            
        } // end if no subsegments
        else // use shape points
        {
            Shape s = (Shape)shapes.get(new Integer(smi.current.getShapeIndex()));
            
            if (smi.rsEnd.distance(smi.current.getEndPoint())==0)
            {
                if (smi.ShapePointIndex==-1)
                {
                    start = smi.current.getStartPoint();
                } 
                else
                {
                    if (smi.ShapePointIndex==s.points.length) start = s.points[smi.ShapePointIndex-1];
                    else start = s.points[smi.ShapePointIndex];
                }
                if (smi.ShapePointIndex >= s.points.length-1)
                {
                    finish = smi.current.getEndPoint();
                }
                else
                {
                    finish = s.points[smi.ShapePointIndex+1];
                }
            } // end if heading toward end point
            else // heading toward "start point"
            {
                if (smi.ShapePointIndex==s.points.length)
                {
                    start = smi.current.getEndPoint();
                }
                else if (smi.ShapePointIndex < 0)
                {
                    start = s.points[0];
                }
                else
                {
                    start = s.points[smi.ShapePointIndex];
                }
                if (smi.ShapePointIndex > 0)
                {
                    finish = s.points[smi.ShapePointIndex-1];
                }
                else
                {
                    finish = smi.current.getStartPoint();
                }
            } // end else heading toward starting point 
        } // end else using shape points
        
        // normalize
        double temp = 1.0/(start.distance(finish));
        normalized = new Location.Location2D((float)(temp* (finish.getX()-start.getX())), 
                (float)(temp * (finish.getY()-start.getY())));
        
        // find normal, multiply by displacement: 
        if (Math.signum(normalized.getX()) == Math.signum(normalized.getY()))
            return new Location.Location2D(displacement * normalized.getY()*(-1), 
                    normalized.getX()*displacement );       
        if (normalized.getX()== 0 )
            if (normalized.getY() > 0)
            return new Location.Location2D(displacement * normalized.getY()* (-1), 
                    normalized.getX()*displacement);  
            else 
                return new Location.Location2D(displacement * normalized.getY() * (-1), 
                        normalized.getX()*displacement);  
        if (normalized.getY()== 0 )
            if (normalized.getX() > 0)
            return new Location.Location2D(displacement * normalized.getY(), 
                    normalized.getX()*displacement); 
            else             return new Location.Location2D(displacement * normalized.getY(), 
                    normalized.getX()*displacement); 
        return new Location.Location2D(displacement * normalized.getY() * (-1), 
                    normalized.getX()*displacement);    
    }
    
    /**
     * This method returns the point along the road segment that is the
     * specified distance from the current location. The method determines 
     * the point using the information contained in the MobilityInfo object.
     * 
     * @param curr current location
     * @param info mobility info object
     * @param dist distance to travel
     * @return 2D location after moving the specified distance
     */
    public Location pointAt(Location curr, MobilityInfo info, 
            float dist)
    {    	
        Shape shape;
        float partialDist=0;
        Location newLocation = curr; // start at current location
        
        StreetMobilityInfo smi = (StreetMobilityInfo)info;
        
        // find shape
        shape = (Shape)shapes.get(new Integer(smi.current.getShapeIndex()));
        int shapePointIndex = smi.ShapePointIndex; // find index into set of points
        
        // case vehicle is moving toward the "start"
        if (smi.current.getStartPoint().distance(smi.rsEnd)<=INTERSECTION_RESOLUTION) // move in descending order
        {
            // case at beginning of shape
            if (shapePointIndex==shape.points.length)
            {
                // distance to from start point to last point in shape entry
                partialDist = curr.distance(shape.points[shapePointIndex-1]);
                
                if (dist <= partialDist) // didn't reach new shape point
                {
                    return move(newLocation, shape.points[shapePointIndex-1], 
                            dist);	                    
                }
                else // move to next point
                {
                    dist-= partialDist;
                    shapePointIndex--;
                    newLocation = shape.points[shapePointIndex];
                }
            }
            
            // get distance to next point in shape entry (or end of road segment)
            if (shapePointIndex > 0)
            {
                partialDist = newLocation.distance(shape.points[shapePointIndex-1]);
            }
            else
            {
                partialDist = newLocation.distance(smi.current.getStartPoint());
            }
            
            // iterate through shape points until distance has been covered
            while(partialDist < dist && shapePointIndex > 1)
            {
                shapePointIndex--;
                newLocation = shape.points[shapePointIndex];
                dist-=partialDist;
                partialDist = newLocation.distance(shape.points[shapePointIndex-1]);
            }
            
            // fix for poorly written loop
            if (partialDist < dist && shapePointIndex>0)
            {
                shapePointIndex--;
                newLocation = shape.points[shapePointIndex];
                dist-=partialDist;
            }
            
            // update index of shape point in vehicle state
            smi.ShapePointIndex = shapePointIndex;
            if (shapePointIndex > 0)
            {
                return move(newLocation, shape.points[shapePointIndex-1], 
                        dist);
            }
            else
            {	
                float realDist = newLocation.distance(smi.rsEnd);
                if (dist > realDist) dist = realDist;
                return move(newLocation, smi.rsEnd, dist);
            }
        } // end if moving toward "start"
        
        else // move in ascending order through shape
        {
            // beginning of shape
            if (shapePointIndex==-1)
            {
                partialDist = curr.distance(shape.points[0]);
                
                if (dist <= partialDist) // didn't reach new shape point
                {
                    return move(newLocation, shape.points[0], dist);	                    
                }
                else // move to next point
                {	
                    dist-= partialDist;
                    shapePointIndex++;
                    newLocation = shape.points[0];
                }
            }
            
            // iterate through shape points until distance has been covered
            if (shapePointIndex == shape.points.length-1)
            {
                partialDist = newLocation.distance(smi.current.getEndPoint());
                
            }
            else
            {
                partialDist = newLocation.distance(shape.points[shapePointIndex+1]);
            }
            
            while(partialDist < dist && shapePointIndex < shape.points.length-2)
            {
                shapePointIndex++;
                newLocation = shape.points[shapePointIndex];
                dist-=partialDist;
                partialDist = newLocation.distance(shape.points[shapePointIndex+1]);
            }
            // fix for poorly written loop
            if (partialDist < dist&& shapePointIndex<shape.points.length-1)
            {
                shapePointIndex++;
                newLocation = shape.points[shapePointIndex];
                dist-=partialDist;
            }
            
            // update index in shape point
            smi.ShapePointIndex = shapePointIndex;
            if (shapePointIndex < shape.points.length-1)
            {
                return move(newLocation, shape.points[shapePointIndex+1], 
                        dist);
            }
            else
            {	
                float realDist = newLocation.distance(smi.rsEnd);
                if (dist > realDist) dist = realDist;
                return move(newLocation, smi.rsEnd, dist);
            }
            
        } // end case moving toward "end"
        
    }
    
    /**
     * Returns the next position for a node moving on a road. Provides heart 
     * of the car-following implementation
     * 
     * @param curr the current point on the segment
     * @param info the mobility info for this node
     * @return next location after step
     */
    public Location step(Location curr, MobilityInfo info, Integer id) 
    {

        if (DEBUG) System.out.println("Entering step...");
        Location newLocation = curr;
        boolean firstCar = false;
        StreetMobilityInfo nextInfo=null; // next car in current lane
        
        StreetMobilityInfo smi = (StreetMobilityInfo)info;
               
        // find next car on road, if any
        if (smi.nextCar==null || 
                ((StreetMobilityInfo)smi.currentLane.getFirst())==info)
        {
            firstCar = true;
        }
        else
        {
            nextInfo = smi.nextCar;
        }
        
        // find maximum new speed
        float newSpeed = Math.min(smi.currSpeed+smi.acceleration*smi.stepTime, 
                smi.getAdjustedSpeed());
        
        if (smi.nextRS != null) // do this only if there is a next RS
        {
            // check if necessary to to slow down for upcoming intersection
            if (smi.nextIs.getPauseTime(smi.current, smi.nextRS, smi)>0 ||
                    smi.current.getStreetIndex()!=smi.nextRS.getStreetIndex() /* turn */)
            {           	
                // in this case, yes, so slow down if close to red light or if turning
                // (assumes uniform slowdown)
                float stopDist = (smi.currSpeed/smi.acceleration)*(smi.currSpeed/2);
                if (smi.remainingDist <= stopDist) // slow down only if close to intersection
                {
                    // T-ODO adjust acceleration according to actual room for stopping
                    newSpeed = Math.max(smi.currSpeed-smi.acceleration*smi.stepTime, 0);
                }   
            } // end case slowing down for next intersection
        }           
        
        // find maximum distance to travel
        float dist = (float)Math.min(smi.stepTime*(newSpeed+smi.currSpeed)/2, 
                smi.remainingDist);
        
        // is there a car to follow?
        if (!firstCar)
        {
            // get next car's distance and speed
            float nextSpeed = nextInfo.currSpeed;
            float nextDist = nextInfo.remainingDist;
            
            // adjust displacement if there is a car in front
            float difference = smi.remainingDist - nextDist -
            smi.current.getCarSpacing(smi.currSpeed, smi.spacingBeta,
                    smi.spacingGamma);
            
            // this is the essence of the car-following model; namely,
            // there is a car in front
            if (difference < dist)
            {
                dist = difference; // adjust distance to travel
                
                // case where there is no room to move forward
                if (dist <=0 ) 
                {
                    // wait a short while
                    JistAPI.sleep(Constants.SECOND/4);
                    smi.currSpeed = 0; // set current speed to zero
                    return curr; // don't move                    
                }
                newSpeed = nextSpeed; // match speed of car in front
            } // end case using car following    
        } // end case not first car in lane            
        
        // ensure that the car doesn't blow past the intersection
        if (smi.remainingDist < dist)
        {
            dist = curr.distance(smi.rsEnd);
            smi.remainingDist = dist;
        }
        if (dist < 0)
        { 
            dist = 0;            
        }
        // update speed, remaining distance after each step
        smi.remainingDist -= dist;
        smi.currSpeed=newSpeed;
        
        // simple case: no shape points in segment
        if (smi.current.getShapeIndex()<0)
        {           
            newLocation = move(curr, smi.rsEnd, dist); // get the new location
            
            // to avoid precision errors, recalculate the remaining distance
            smi.remainingDist = newLocation.distance(smi.rsEnd);            
            return newLocation;
        }
        // otherwise, use pointAt to move along shape file
        return pointAt(curr, info, dist);
    }
    
    private StreetMobilityInfo getNextCarInfo(StreetMobilityInfo smi, Integer id) {
		StreetMobilityInfo nextCar = null;
		if (lcm==null && (smi.nextCar==null || smi.currentLane.getFirst() == smi)) return null;
		if (lcm==null) return smi.nextCar;
		else {
            if (smi.currentLane.getFirst() == smi) smi.nextCar = null;
			nextCar = lcm.getClosestCarInfo(smi, id);
			if (nextCar == null ) nextCar = smi.nextCar;
			
		}
        if (nextCar!=null && smi.getRemainingDist()<=nextCar.getRemainingDist()) 
            throw new RuntimeException("Wrong next car!");

        return nextCar;
	}

	/**
     * Changes coordinates from 0-based lat/long to coordinates on this map
     * in latitude and longitude.
     * @param loc the location in meters
     * @return the location on the map
     */
    public Location metersToDegrees(Location loc) {
        loc = (Location.Location2D)loc;
        
        return new Location.Location2D((float)(loc.getX()*(METERS_PER_DEGREE))+minX, 
                (float)(loc.getY()*(METERS_PER_DEGREE))+minY);
    }
    
    /**
     * Changes coordinates degrees latitude and longitude to
     * 0-based meters.
     * 
     * @param loc the location in meters
     * @return the location on the map
     */
    public Location degreesToMeters(Location loc) {
        loc = (Location.Location2D)loc;
        
        return new Location.Location2D((loc.getX()-minX)/(float)METERS_PER_DEGREE, 
                (loc.getY()-minY)/(float)METERS_PER_DEGREE);
    }
    /**
     * Returns an array Location.Location2D's describing 
     * the bounds of the region.
     * 
     * [0] - (minX, maxY)
     * [1] - (maxX, maxY) (adjusted for lane displacement)
     * [2] - (minX, minY) (adjusted for lane displacement)
     * [3] - (maxX, minY)
     * 
     * @return array of locations
     */
    public Location[] getBounds()
    {
        Location.Location2D corners[] = new Location.Location2D[4];
        Location topCorner = new Location.Location2D(minX, maxY);
        corners[0] = new Location.Location2D(minX - topCorner.getX(), topCorner.getY() - minY);
        corners[1] = new Location.Location2D(maxX - topCorner.getX() +20, topCorner.getY() - minY+20);
        corners[2] = new Location.Location2D(minX - topCorner.getX()-20, topCorner.getY() - maxY-20);
        corners[3] = new Location.Location2D(maxX - topCorner.getX(), topCorner.getY() - maxY);
        
//        float initX = old.getX();
//        float initY = old.getY();
//        Location topCorner = getBounds()[2];
//        return new Location.Location2D(initX - topCorner.getX(), 
//                topCorner.getY() - initY);
        
        return corners;
    }
    
    
    /**
     * Returns the area of the test region.
     * 
     * @return area of the test region
     */
    public double getArea()
    {
        return intersections.area();
    }
    
    /**
     * This function loads the specified file into memory and 
     * extracts its road segments. Assumes that files are stored
     * in little-endian format.
     * 
     * @param filename the file containing segment data
     */
    private void loadSegmentsFile(String filename)
    {
        long length;
        int numRecs, saLeft, eaLeft, saRight, eaRight, streetIndex, shapeIndex;
        float startX, startY, endX, endY;
        char roadClass;
        Location.Location2D start, end;
        
        try
        {
            File f = new File(filename);
            FileInputStream fs = new FileInputStream(filename);
            
            FileChannel fc = fs.getChannel();
            
            // map the file into a byte buffer           
            MappedByteBuffer mbb = fc.map(
                    FileChannel.MapMode.READ_ONLY, 0, 
                    fc.size());
            
            // set byte order to be little-endian
            mbb.order(ByteOrder.LITTLE_ENDIAN);
            
            //get length of file and number of records
            length = f.length();
            numRecs = (int)(length/ROAD_SEGMENT_SIZE);
            
            int j =0; // counter for number of streets
            
            // read all records from file
            for (int i = 0; i < numRecs; i++)
            {
                saLeft = mbb.getInt();
                eaLeft = mbb.getInt();
                saRight = mbb.getInt();
                eaRight = mbb.getInt();
                streetIndex = mbb.getInt();
                shapeIndex = mbb.getInt();
                // start point
                startX = (float)(METERS_PER_DEGREE*mbb.getInt()/1000000.0f);
                startY = (float)(METERS_PER_DEGREE*mbb.getInt()/1000000.0f);	          
                start = new Location.Location2D(startX, startY);
                
                // end point
                endX = (float)(METERS_PER_DEGREE*mbb.getInt()/1000000.0f);
                endY = (float)(METERS_PER_DEGREE*mbb.getInt()/1000000.0f);
                end = new Location.Location2D(endX, endY);
                
                roadClass = (char)mbb.get();
                if (roadClass < 11 || roadClass > 74)
                {
                    // T-ODO figure out what road classes 51 and 64 are
                    System.out.println("Unknown road class for road: " + roadClass);
                }
                
                mbb.position(mbb.position()+3); // advance to next record 
                
                // make sure this segment is within the specified bounds
                if(!start.inside(bl, tr) || !end.inside(bl, tr))
                {
                    continue;
                }
                else
                {
                    if (start.distance(end)>0){
                        if (roadClass > 10 && roadClass < 70
                                && roadClass!=51 && roadClass!=64)
                        {
                            // update the bounds for the region based on roads 
                            // because it may be different than what the user 
                            // specified
                            if (startX < minX ) minX = startX;
                            if (startX > maxX) maxX = startX;
                            if (startY < minY ) minY = startY;
                            if (startY > maxY) maxY = startY;	
                            if (endX < minX ) minX = endX;
                            if (endX > maxX) maxX = endX;
                            if (endY < minY ) minY = endY;
                            if (endY > maxY) maxY = endY;
                            
                            // create RoadSegment and store reference in vector
                            segments.add(new RoadSegment(saLeft, eaLeft,
                                    saRight, eaRight, streetIndex,
                                    shapeIndex, start, end, j, roadClass));
                            
                            // mark the street name as used for future reference
                            usedStreets.put(new Integer(streetIndex), null);
                            
                            // mark shape index as used, if there's a shape
                            if (shapeIndex!=-1)usedShapes.put(new Integer(shapeIndex), null);
                            
                            j++; // increment index for RoadSegment index in Vector
                        } // end if
                    } // end else
                }
                
            } // end for
            
            fs.close(); // we are done with the file           

        } // end try
        catch (FileNotFoundException e)
        {
            System.out.println("Segments file does not exist at the specified location!");
            System.exit(0);
        }
        catch (IOException e)
        {
            System.out.println("StreetMobility::loadSegmentFile: I/O error");
            System.exit(0);
        }
        
    } // loadSegmentsFile
    
    private void doPruning() {
        RoadSegment rs;
        Vector segClone = (Vector) segments.clone();
        Vector vecOfVecs = new Vector();
        while (segClone.size()>0)
        {
            boolean removed[] = new boolean[segments.size()];
            Vector connected = new Vector(2);
            RoadSegment rs2 = (RoadSegment) segClone.get(0);
            connected.add(rs2);
            findConnected(rs2.getEndPoint(), connected, segClone, removed);
            findConnected(rs2.getStartPoint(), connected, segClone, removed);
            vecOfVecs.add(connected);
            // remove segments
            RoadSegment rs3 = new RoadSegment();
            for (int i = 0; i < removed.length; i++){
                if (removed[i]){
                    rs3.setSelfIndex(i); // this is efficient because segments are ordered
                    segClone.remove(rs3);
                    removedSegments++;
                }
            }
        }
        int index = 0;
        int size = -1;
        for (int i = 0; i < vecOfVecs.size(); i++)
        {
            if (((Vector)vecOfVecs.get(i)).size() > size)
            {
                index = i;
                size = ((Vector)vecOfVecs.get(i)).size();
            }
        }
        segments = (Vector)vecOfVecs.get(index);
        for (int i = 0; i < segments.size(); i++)
        {
            rs = (RoadSegment)segments.get(i);
            rs.setSelfIndex(i);
        }
        
        // T-ODO intersection cleanup
        
    }
    
    private void findConnected(Location firstPoint, Vector connected, 
            Vector segClone, boolean removed[]) {
        RoadSegment rs2;
        LinkedList openList = new LinkedList();
        openList.add(firstPoint);

        while (openList.size()!=0){            
            Location point = (Location)openList.remove();
            Intersection is = intersections.findIntersectingRoads(point);
            if (is!=null)
            {  
                LinkedList ll = is.getRoads();
                ListIterator li = ll.listIterator();
                while (li.hasNext())
                {
                    Location nextPoint = null;
                    rs2 = (RoadSegment) li.next();
                    if (removed[rs2.getSelfIndex()]) continue;
                    if (point.distance(rs2.getEndPoint())<=INTERSECTION_RESOLUTION) nextPoint = rs2.getStartPoint();
                    else if (point.distance(rs2.getStartPoint())<=INTERSECTION_RESOLUTION) nextPoint = rs2.getEndPoint();
                    
                    if (nextPoint != null){ 
                        connected.add(rs2);
                        removed[rs2.getSelfIndex()]=true;
                        if (!listContains(openList, nextPoint)) openList.add(nextPoint);
                    }
                }
            }           
        }

    }
    

    private boolean listContains(LinkedList openList, Location nextPoint) {
        ListIterator li = openList.listIterator();
        Location l;
        while (li.hasNext())
        {
            l = (Location)li.next();
            if (l.distance(nextPoint) <= INTERSECTION_RESOLUTION) return true;
        }
        return false;
    }
//
//    /**
//     * Removes unconnected segments from map
//     *
//     */
//    private void doPruning() {
//        RoadSegment rs;
//        Vector segClone = (Vector) segments.clone();
//        Vector vecOfVecs = new Vector();
//        while (segClone.size()>0)
//        {
//            Vector connected = new Vector();
//            RoadSegment rs2 = (RoadSegment) segClone.remove(0);
//            connected.add(rs2);
//            findConnected(rs2.getEndPoint(), connected, segClone);
//            findConnected(rs2.getStartPoint(), connected, segClone);
//            vecOfVecs.add(connected);
//            for (int i = 0; i < connected.size(); i++) segClone.remove(connected.get(i));
//        }
//        int index = 0;
//        int size = -1;
//        for (int i = 0; i < vecOfVecs.size(); i++)
//        {
//            if (((Vector)vecOfVecs.get(i)).size() > size)
//            {
//                index = i;
//                size = ((Vector)vecOfVecs.get(i)).size();
//            }
//        }
//        segments = (Vector)vecOfVecs.get(index);
//        for (int i = 0; i < segments.size(); i++)
//        {
//            rs = (RoadSegment)segments.get(i);
//            rs.setSelfIndex(i);
//        }
//        
//    }
//    
//    private void findConnected(Location point, Vector connected, Vector segClone)
//    {
//        RoadSegment rs2;
//        for (int i =0; i < segClone.size(); i++)
//        {
//            Location nextPoint = null;
//            rs2 = (RoadSegment) segClone.get(i);
//            if (point.distance(rs2.getEndPoint())<=INTERSECTION_RESOLUTION) nextPoint = rs2.getStartPoint();
//            else if (point.distance(rs2.getStartPoint())<=INTERSECTION_RESOLUTION) nextPoint = rs2.getEndPoint();
//
//            if (nextPoint != null){ 
//                if ((segments.size()/100) == connected.size()) System.out.print("\rProgress: " + connected.size()/(segments.size()/100) + "%");
//                connected.add(segClone.remove(i));
//                findConnected(nextPoint, connected, segClone);
////                findConnected(rs2.getStartPoint(), connected, segClone);
//                i--;
////                connected.add(segClone.get(i));
////                findConnected((RoadSegment)segClone.get(i), connected, segClone);
//            }
//        }
//    }
    

    /**
     * This function loads the street names into memory.
     * 
     * @param filename the file containing street names
     */
    private void loadStreetsFile(String filename)
    {
        long length = 0;
        int numRecs = 0;
        /* prefix, name, type and suffix sizes accoring to TIGER file format */
        char prefix[] = new char[2];
        char name[] = new char[30];
        char suffix[] = new char[4];
        char type[] = new char[2];
        int next = -1;	
        int currentPos=0;
        
        try
        {
            File f = new File(filename);
            FileInputStream fs = new FileInputStream(filename);
            DataInputStream ds = new DataInputStream(fs);
            
            //get length of file and number of records
            length = f.length();
            numRecs = (int)(length/STREET_NAME_SIZE);
            
            // get iterator for street list
            Iterator streetIt = usedStreets.keySet().iterator();	       
            
            // read all records from file
            while (streetIt.hasNext())
            {
                // advance to next one 
                currentPos = next;
                next = ((Integer)streetIt.next()).intValue();

                
                if (next > 0)
                {
                    ds.skip((next-currentPos-1)*STREET_NAME_SIZE);
                }
                               
                prefix[0] = (char)ds.readUnsignedByte();
                prefix[1] = (char)ds.readUnsignedByte();	            
                
                
                // street name
                for (int j = 0; j < name.length; j++)
                {
                    name[j] = (char)ds.readUnsignedByte();
                }
                
                // street suffix
                for (int j = 0; j < suffix.length; j++)
                {
                    suffix[j] = (char)ds.readUnsignedByte();
                }
                
                // street type
                for (int j = 0; j < type.length; j++)
                {
                    type[j] = (char)ds.readUnsignedByte();
                }
                
                
                streets.put(new Integer(next), new StreetName(
                        new String(prefix), new String(name),
                        new String(type), new String(suffix)));
                
            }	     
            
            // we are done with the file
            ds.close();
            fs.close();
            usedStreets = null; // no longer needed
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Streets file does not exist at the specified location!");
            System.exit(0);
        }
        catch (IOException e)
        {
            System.out.println("Length: " + numRecs + " next: "+ next);
            System.out.println("StreetMobility::loadStreetFile: I/O error");
            e.printStackTrace();
            System.exit(0);
        }
        
    } // loadStreetsFile
    
    /**
     * This function loads the shape data into memory. A segment has 
     * a shape if it's a multiline segment. 
     * 
     * @param filename the file containing shape data
     */
    private void loadShapesFile(String filename)
    {
        int numRecs, numPoints;
        double x, y;
        
        try
        {
            FileInputStream fs = new FileInputStream(filename);
            
            FileChannel fc = fs.getChannel();
            
            // map the file into a byte buffer            
            MappedByteBuffer mbb = fc.map(
                    FileChannel.MapMode.READ_ONLY, 0, 
                    fc.size());
            
            // set byte order to be little-endian
            mbb.order(ByteOrder.LITTLE_ENDIAN);
            
            // get number of records
            numRecs = mbb.getInt();
            
            if (DEBUG) System.out.println("Number of shapes: " + numRecs);
            
            // find indexes for used shapes for this region	
            Iterator shapeIt = usedShapes.keySet().iterator();
            
            if (!shapeIt.hasNext())
            {
                if (DEBUG) System.out.println("No shapes in selected region!");
                return;
            }
            
            int next = ((Integer)shapeIt.next()).intValue();
            
            // read all records from file
            for (int i = 0; i < numRecs; i++)
            {
                numPoints = mbb.getInt();
                if (i == next)
                {
                    Location.Location2D points[] = new Location.Location2D[numPoints];
                    
                    // read each shape point
                    for (int j = 0; j < numPoints; j++)
                    {
                        x = (METERS_PER_DEGREE*mbb.getInt()/1000000.0f);
                        y = (METERS_PER_DEGREE*mbb.getInt()/1000000.0f);
                        
                        // update the bounds for the region based on roads 
                        // because it may be different than what the user 
                        // specified
                        if (x < minX ) minX = (float)x;
                        if (x > maxX) maxX = (float)x;
                        if (y < minY ) minY = (float)y;
                        if (y > maxY) maxY = (float)y;   
                        
//                        System.out.println((float)x);
                        points[j] = new Location.Location2D((float)x,(float)y);
                    }
                    
                    shapes.put(new Integer(i), new Shape(points));
                    
                    if (!shapeIt.hasNext()) break;
                    next = ((Integer)shapeIt.next()).intValue();
                    
                }
                else
                {
                    mbb.position(mbb.position()+8*numPoints); // 8 bytes per point
                }
            }	     
            
            // we are done with the file
            fs.close();
            usedShapes = null; // no longer necessary
            
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Shape file does not exist at the specified location!");
            System.exit(0);
        }
        catch (IOException e)
        {
            System.out.println("loadShapesFile:I/O error");
            System.exit(0);
        }
        
    } // loadShapesFile
    
    /**
     * Move along a straight line the specified distance.
     * 
     * @param start Point where motion begins.
     * @param end End point for line segement.
     * @param distance Distance (meters) to more along this segment.
     * @return Returns the location after moving distance units from start to end.
     */
    public Location move(Location start, Location end, 
            float distance)
    {
        float hyp = end.distance(start);
        if (distance == hyp) return end;
        float portion = distance/hyp;
        
//        if (distance < 0.01) return end;
        if (DEBUG)System.out.println("Length: "+ hyp+ " / Distance: "+distance);
        
        // handles floating-point imprecision
        if (portion>1.1)
        {
            if (distance < 0.01) return end;
            System.out.println("Length: "+ hyp+ " / Distance: "+distance);
            System.out.println("Distance/length: " +portion);	
            System.out.println("Start: "+start+" End: "+end);
            throw new RuntimeException("StreetMobility:move: Move error!");   	    
        }
        
        float dx = distance * (end.getX() - start.getX())/hyp;
        float dy = distance * (end.getY() - start.getY())/hyp;     
                 
        return new Location.Location2D(start.getX()+dx, 
                start.getY()+dy);
    }
    
    /**
     * Move along a straight line the specified distance.
     * THIS IS A TEST METHOD IN ORDER TO ATTEMPT TO GET THE NEIGHBORS FOR A CURRENT NODE
     * @param start Point where motion begins.
     * @param end End point for line segement.
     * @param distance Distance (meters) to more along this segment.
     * @return Returns the location after moving distance units from start to end.
     */
    public Location move(Location start, Location end, 
            float distance, StreetMobilityInfo smi)
    {
        float hyp = end.distance(start);
        if (distance == hyp) return end;
        float portion = distance/hyp;
        
//        if (distance < 0.01) return end;
        if (DEBUG)System.out.println("Length: "+ hyp+ " / Distance: "+distance);
        
        // handles floating-point imprecision
        if (portion>1.1)
        {
            if (distance < 0.01) return end;
            System.out.println("Length: "+ hyp+ " / Distance: "+distance);
            System.out.println("Distance/length: " +portion);	
            System.out.println("Start: "+start+" End: "+end);
            throw new RuntimeException("StreetMobility:move: Move error!");   	    
        }
        
        float dx = distance * (end.getX() - start.getX())/hyp;
        float dy = distance * (end.getY() - start.getY())/hyp;

        //TESTING RADAR CLASS--- GET BEARING OF CURRENT NODE
   /*     VLC vlcDevice = new VLC(); 
        vlcDevice.origin = new Location.Location2D(start.getX(), start.getY()); 						//set the location of the radar on the map
      
        //may need to use smi.getEndPoint() to get the destination location
        float bearingAngle = vlcDevice.getBearing(vlcDevice.origin, end); 					//get the bearing between current location and destination
        vlcDevice.cornerPoint1 = vlcDevice.getVLCCornerPoint(bearingAngle - (vlcDevice.visionAngle/2), vlcDevice.origin, vlcDevice.distanceLimit, vlcDevice.visionAngle);
        vlcDevice.cornerPoint2 = vlcDevice.getVLCCornerPoint(bearingAngle + (vlcDevice.visionAngle/2), vlcDevice.origin, vlcDevice.distanceLimit, vlcDevice.visionAngle);
*/
        //get the neighbors of the vehicle within a certain distance radius
        PriorityList openList = new PriorityList();
        Location nextEnd = smi.getCurrentRS().getEndPoint();        
        
		SegmentNode startNode = new SegmentNode(nextEnd, 
				smi.current.getSelfIndex(), smi.current.getStartPoint().distance(nextEnd)==0, true);
        				
		openList.add(startNode); 
        AStarNode node = (AStarNode)openList.removeFirst(); 
		
		//List neighbors = node.getNeighbors();													//get the neighboring road segments to the current segment being traveled
  //      List neighbors = node.getNeighbors(vlcDevice.origin, vlcDevice.distanceLimit);
		
		//for all neighbors within a certain radius away..
		                     
		SegmentNode neighboringNode = null;		
		RoadSegment neighboringSegment = null;
		
		LinkedList carsToRsEndLinkedList = new LinkedList();						//list of cars from current point to the end of all the road segments
		LinkedList carsToRsStartLinkedList = new LinkedList();
		ArrayList carsToStart = new ArrayList();
		ArrayList carsToEnd = new ArrayList();	
		
		//Mate: 	get all nodes in the simulation
		//			Use the fact that nodes get sequential IDs starting at 1.
		//			Get the location of each of the nodes
		int numNodes = JistExperiment.getJistExperiment().getNodes();		
		Location[] nodeLocations = new Location[numNodes];
		boolean[] nodeVisibleToRadar = new boolean[numNodes];
		//go over all nodes, check if they're inside current radar's range
		for(int ii=1;ii<numNodes; ii++) 
		{
			nodeLocations[ii] = jist.swans.field.Field.getRadioData(ii).getLocation();				
		//	nodeVisibleToRadar[ii] = vlcDevice.visibleToVLCdevice(nodeLocations[ii].getX(), nodeLocations[ii].getY(),vlcDevice.origin.getX(), vlcDevice.origin.getY(), vlcDevice.cornerPoint2.getX(), vlcDevice.cornerPoint2.getY(), vlcDevice.cornerPoint1.getX(), vlcDevice.cornerPoint1.getY());
		}
		//System.out.println("okay?");
		
		/*for(int i = 0; i < neighbors.size(); i ++)												//get the cars for all neighboring road segments
		{
			System.out.println("neighbor (" + i + "): " + neighbors.get(i));
			
			neighboringNode = ((SegmentNode) neighbors.get(i));														//turn each neighbor (of type list) into a segmentNode type in order to get the road segment information
			neighboringSegment = ((RoadSegment)SegmentNode.info.segment.get(neighboringNode.segmentID));			//once the road segment is obtained, get the cars on that road segment 
			if (JistExperiment.getJistExperiment().useVisualizer) {
				v.colorSegment(smi.current, Color.PINK);
				v.colorSegment(neighboringSegment, Color.YELLOW);
			}
			//If the neighboring road segment's start point is within the distance limit, store the cars on it
			
			carsToRsStartLinkedList = neighboringSegment.getCarsToStart();
			
			for(int m = 0; m < carsToRsStartLinkedList.size(); m++)
			{
				carsToStart.add(carsToStart.size(), carsToRsStartLinkedList.get(m)); 					//add each car one by one (as the for loop executes) to the end of the list
			}
			
			carsToRsEndLinkedList = neighboringSegment.getCarsToEnd();
			for(int m = 0; m < carsToRsEndLinkedList.size(); m++)
			{
				carsToEnd.add(carsToEnd.size(), carsToRsEndLinkedList.get(m)); 					//add each car one by one (as the for loop executes) to the end of the list
			}			
							
		}*/
				
		/*try
		{*/			
			carLocsForRadarToCheck = new Location[carsToEnd.size() + carsToStart.size()];
			int detectedNeighbors = 0; 
			
			//T-ODO: get file output to work.
			//BufferedWriter out = new BufferedWriter(new FileWriter("carsOutput.txt"));
			int bounds = 0; 
			
			for(int i = 0; i < carsToEnd.size() + carsToStart.size(); i++)					//for each car detected
			{				
				if(i <  carsToEnd.size())
				{
					String out1 = "Ending car " + i + ": " + carsToEnd.get(i);
		
		
						MobilityInfo sm = (MobilityInfo)carsToEnd.get(i);					//turn into street mobility object in order to get its location
						StreetMobilityInfo neighborSMI = (StreetMobilityInfo)sm;
						StreetMobility streetMob = (StreetMobility)sm; 
						streetMob.neighborMobInfo.add(neighborSMI);		
						
						//Exception Thrown on this line. 
						carLocsForRadarToCheck[i] = ((StreetMobilityInfo) sm).pointAt(neighborSMI.current.getEndPoint(), neighborSMI, neighborSMI.remainingDist);
						if(!carLocsForRadarToCheck[i].equals(null))
						{
						//		if(vlcDevice.visibleToVLCdevice(carLocsForRadarToCheck[i].getX(), carLocsForRadarToCheck[i].getY(), vlcDevice.origin.getX(), vlcDevice.origin.getY(), vlcDevice.cornerPoint2.getX(), vlcDevice.cornerPoint2.getY(), vlcDevice.cornerPoint1.getX(), vlcDevice.cornerPoint1.getY()) == true)
								{
									detectedNeighbors++; 
								}
						}
					
					System.out.print("CAR LOCATION TO CHECK??:: " + carLocsForRadarToCheck[i]);
					
					System.out.println(out1 +"\n");
					//out.write(out1);
					//out.newLine(); 
				}
					
				if(i < carsToStart.size())
				{
					String out2 = "Starting car " + i + ": " + carsToStart.get(i);
					System.out.println(out2+"\n");
					//out.write(out2);
					//out.newLine();
				}
			}
			
			if(detectedNeighbors > 0)
			{
				System.out.println("Number of neighbors detected by radar: "+ detectedNeighbors);
			}
			System.out.println("-------------------------------------------------------------------------");
			//out.write("-------------------------------------------------------------------------");
			//out.newLine();
			//out.close();
		//}
/*		catch(Exception e)
		{
			System.out.println("File Error");
		}*/
		
		SegmentNode.masterNeighborList = new LinkedList(); 									//reinitialize masterNeighborList so it is ready for the next move and let the garbage collector pick up the loose LinkedList.
		
        return new Location.Location2D(start.getX()+dx, start.getY()+dy);
    }
    
    /**
     * Prints the average speed for all nodes in the experiment.
     * @param seconds The duration of the simulation.
     * @return A String containing the average speed for all the vehicles.
     */
    public String printAverageSpeed(int seconds, boolean verbose)
    {
        Iterator it = mobInfo.iterator();
        StreetMobilityInfo smri;
        float total=0;
        
        int i = 1;
        while (it.hasNext())
        {
            smri = (StreetMobilityInfo)it.next();
            if (verbose) System.out.println("Average speed for node "+i+": "
                    +smri.speedSum/seconds);
            total+=smri.speedSum/seconds;
            
            if (verbose && smri.speedSum/seconds < 1)
            {
                printStreetList(i);
            }
            i++;
        }
        
        System.out.println("Average speed (overall): "+ total/mobInfo.size());
        return total/mobInfo.size()+"";
        
    }
    
    /**
     * Prints the streets traversed by a vehicle.
     * @param i The id of the node to print streets for.
     */
    public void printStreetList(int i)
    {
        if (RECORD_STREETS){
            System.out.println("Streets for node "+i+": ");
            StreetMobilityInfo smri = (StreetMobilityInfo)mobInfo.get(i-1);
            
            Iterator it = smri.roads.iterator();
            RoadSegment rs;
            
            while (it.hasNext())
            {
                rs = (RoadSegment)segments.get(((Integer)it.next()).intValue());
                System.out.println(rs.printStreetName(streets));
            }
        }
    }
    
    /**
     * Sets the next car for the node belonging to the SMI object.
     * @param smi The SMI object corresponding to the node changing streets.
     */
    public void setNextCar( StreetMobilityInfo smi )
    {       
        if (smi.currentLane.size()>1)
        {
            smi.nextCar = 
                (StreetMobilityInfo)smi.currentLane.get(smi.currentLane.size()-2);
            if (smi.nextCar.current!=smi.current) 
            	throw new RuntimeException("Wrong car!");
        }	                  
        else smi.nextCar=null;
        

    }
    
    /**
     * Updates the next road segment and next endpoint in the SMI object.
     * @param smi SMI object to update
     * @param rs new next road segment
     */
    public void updateNextRoadSegment(StreetMobilityInfo smi, RoadSegment newRS) 
    {
        smi.nextRS=newRS;
        // update current road segment and end point for this node
        if(newRS.getStartPoint().distance(smi.rsEnd)< INTERSECTION_RESOLUTION)
            smi.nextEnd = newRS.getEndPoint();
        else if (newRS.getEndPoint().distance(smi.rsEnd) < INTERSECTION_RESOLUTION)
            smi.nextEnd = newRS.getStartPoint();
        else{
            System.out.println("Road Segment: "+newRS.printStreetName(streets));
            System.out.println("Current: "+smi.current.printStreetName(streets));
            System.out.println("Junction: "+smi.rsEnd);
            throw new RuntimeException("Bad intersection!");
        }
        
        if (RECORD_STREETS)
            smi.roads.add(new Integer(smi.nextRS.getSelfIndex()));
    }
    
    /**
     * This sets the next road for the vehicle belonging to 
     * the specified StreetMobilityInfo object. This is 
     * to be called after moving to a new road, so the 
     * SMI object's rsEnd and current fields must be set 
     * as such. 
     * 
     * @param smi the StreetMobilityInfo object to update
     */
    public abstract void setNextRoad(StreetMobilityInfo smi);
    
    
    /**
     * Sets the Random object for use in the mobility model.
     * @param rnd The random object to set.
     */
    public void setRnd(Random rnd) {
        this.rnd = rnd;
    }
    
    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#setGUI(driver.Visualizer)
     */
    public void setGUI(VisualizerInterface visualizer) {
        v = visualizer;
    }
    
    public Vector getSegments() {
        return this.segments;
    }
    
    /**
     * Converts from meters that use lat/long as zero points to 
     * those that use the upper left corner of the map as the (0,0) point.
     * 
     * @param initX the x coord using lat/long
     * @param initY the y coord using lat/long
     * @return a location2D object with the converted values
     */
    public Location convertFromStreets(Location old) {
        float initX = old.getX();
        float initY = old.getY();
        Location topCorner = new Location.Location2D(minX, maxY);
        return new Location.Location2D(initX - topCorner.getX(), 
                topCorner.getY() - initY);
    }
    
    /**
     * Returns the number of streets for the specified region.
     * @return the number of streets
     */
    public int getNumberOfStreets()
    {
        return streets.size();
    }

    /**
     * 
     * @return the HashMap of RoadSegment shapes
     */
    public HashMap getShapes() {
        return shapes;
        
    }

    /**
     * @return Returns the carToInspect.
     */
    public int getCarToInspect() {
        return carToInspect.intValue();
    }
    
    /**
     * Resets the carToInspect value.
     *
     */
    public void unsetCarToInspect() {
        carToInspect = new Integer(-1);
    }

    /**
     * @param carToInspect The carToInspect to set.
     */
    public void setCarToInspect(int carToInspect) {
        this.carToInspect = new Integer(carToInspect);
    }

    public SpatialStreets getIntersections() {
        return intersections;
        
    }

    public HashMap getStreets() {        
        return streets;
    }
    
    public void setLcm(LaneChangeModel lcm){
    	this.lcm = lcm;
    }

	public Vector getMobInfo() {
		return mobInfo;
	}

    /**
     * @param clickLoc
     */
    public RoadSegment getStreet(Location clickLoc) {
        SpatialStreets.fuzzy = true;
        Intersection is = getIntersections().findIntersectingRoads(clickLoc);
        if (is==null) return null; // T-ODO make sure this never happens
        LinkedList ll = is.getRoads();
        ListIterator li = ll.listIterator();
        RoadSegment rs = null;
        while (li.hasNext()){
            rs = (RoadSegment)li.next();
            
            Location min = rs.getStartPoint();
            Shape s = (Shape) shapes.get(rs.getShapeIndex());
            Location max = null;
            boolean found = false;
            if (s!=null){
	            for ( Location l : s.points){
	            	if (max!=null) min = max;
	            	max = l;
	            	if (inside(clickLoc, min, max)){
	            		found = true;
	            		break;
	            	}
	            }
	            if (found) break;
            }
            
            if (max!=null) min = max;
            max = rs.getEndPoint();
            if (inside(clickLoc, min, max)) break;
	        
        }
        SpatialStreets.fuzzy = false;
        return rs;
    }
    
    private boolean inside(Location clickLoc, Location min, Location max) {
    	float maxX, minX, maxY, minY;
    	if (min.getX()>max.getX()){
            maxX=min.getX();
            minX = max.getX();
        }
        else{
            maxX = max.getX();
            minX = min.getX();
        }
        if (min.getY()>max.getY()){
            maxY=min.getY();
            minY = max.getY();
        }
        else{
            maxY = max.getY();
            minY = min.getY();
        }
        min = new Location.Location2D(minX-10, minY-10);
        max = new Location.Location2D(maxX+10, maxY+10);
        if (clickLoc.inside(min, max)){

            return true; // T-ODO better way of narrowing down which street was clicked
        }
        else return false;
	}

	public static StreetMobility getStreetMobility(){
        return myself;
    }

}
