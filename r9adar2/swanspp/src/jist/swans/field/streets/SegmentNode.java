/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         SegmentNode.java
 * RCS:          $Id: SegmentNode.java,v 1.1 2007/04/09 18:49:42 drchoffnes Exp $
 * Description:  SegmentNode class (see below)
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.StreetMobility;
import jist.swans.misc.AStarNode;
import jist.swans.misc.Location;
import jist.swans.misc.Location.Location2D;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The SegmentNode class treats each segment as a node in a graph
 * for performing an A* shortest path search between and origin 
 * and destination.
 */
public class SegmentNode extends AStarNode {
	
	/** the "Manhattan distance" heuristic  */
	private final static int MANHATTAN_DISTANCE = 0;
	
	/** point representing beginning of segment */    
	public Location point;
	/** info for this segment node */
	public static SegmentNodeInfo info;
	public int segmentID; // segment ID
	public boolean start;         // true if "point" represents "start" of segment  
	/** the heuristic to use */
	int heuristic = MANHATTAN_DISTANCE;
	boolean firstNode = false;
	/** debug switch */
	private static final boolean DEBUG = false;
	
	/**
	 * SegmentNode constructor
	 * @param point the location of the SegmentNode's start point
	 * @param segmentID the index into the "segments" vector for this RS
	 * @param start true if location describe's the RS's "start point"
	 * @param info global object for accessing SM info
	 * @param firstNode true if this is the first node in the A* search
	 */
	public SegmentNode(Location point, 
			int segmentID, boolean start, boolean firstNode) {
		super();
		this.point = point;
		this.segmentID = segmentID;
		this.start = start;
		this.firstNode = firstNode;
	}
	/**
     * @param nextRS
     */
    public SegmentNode(RoadSegment rs, Location end) {
        this.point = end;
        this.segmentID = rs.selfIndex;
        this.start = false;
        this.firstNode = false;
    }
    /* (non-Javadoc)
	 * @see jist.swans.misc.AStarNode#getCost(jist.swans.misc.AStarNode)
	 */
	public float getCost(AStarNode node) {
		
		double d, g, gTemp;
		
		SegmentNode n = (SegmentNode)node;
		RoadSegment rs = ((RoadSegment)info.segment.get(n.segmentID));
		RoadSegment currentRS = ((RoadSegment)info.segment.get(segmentID));
		d = rs.length;
		
		// T-ODO find less arbitrary optimizations
		// penalize non-interstate segments because there is a chance that 
		// you may get the red light! 
        if (n.point.distance(info.dest.point)>1600){
    		if (rs.roadClass > 19)   
    		d += 0.2 * rs.length;
    		
    		// penalize street change a little bit
    		if (((RoadSegment)info.segment.get(segmentID)).streetIndex != 
    		rs.streetIndex)
    		d += 0.1 * rs.length;
        }
		
		// now divide the distance by an approximate speed based on road class
		if (info.cm==null || JistAPI.getTime()<30*Constants.SECOND || firstNode) g = d / rs.getSpeedLimit();
        else{
            char direction;
            if (currentRS.endPoint.distance(rs.startPoint)<StreetMobility.INTERSECTION_RESOLUTION
                    || currentRS.startPoint.distance(rs.startPoint)<StreetMobility.INTERSECTION_RESOLUTION){
                direction = CongestionMonitor.TO_END;
            }
            else{
                direction = CongestionMonitor.TO_START;
            }
            g = info.cm.getTransitTime(rs.getSelfIndex(), direction);
            gTemp  = d/rs.getSpeedLimit();
            if (DEBUG && Math.abs(g - gTemp) > 1) 
                System.out.println("GTemp: "+gTemp+"\tG:"+g);
        }
		
		return (float)g;    
	}
	
	/* (non-Javadoc)
	 * @see jist.swans.misc.AStarNode#getEstimatedCost(jist.swans.misc.AStarNode)
	 */
	public float getEstimatedCost(AStarNode node) {
		float h = 0;
		Location end;
		SegmentNode n = (SegmentNode)node;
		RoadSegment rs = ((RoadSegment)info.segment.get(segmentID));
		if (rs == null)
		{
			System.err.println("Null road segment!? at index: "+segmentID);     
			return Float.MAX_VALUE;
		}
        if (this.costFromStart >=40000)
        {
            return this.costFromStart;
        }
		if (start) end = rs.endPoint;
		else end = rs.startPoint;
		
		switch(heuristic)
		{
			/**  approximate distance between two points in meters */
			case MANHATTAN_DISTANCE:
				h = (float)(Math.abs(n.point.getX()- end.getX())+
						Math.abs(n.point.getY() - end.getY()));
				break;        	
			default:
				System.err.println("Unsupported distance heuristic!");
			return 0;
		}
        if (this.equals(node)) 
            return -100;
		return h/ rs.getSpeedLimit(); // don't forget to divide by speed
	}
	/* (non-Javadoc)
	 * @see jist.swans.misc.AStarNode#getNeighbors()
	 */
	public List getNeighbors() {
		LinkedList segmentNodes = new LinkedList();
		LinkedList intersectingRoads;
		RoadSegment rs;
		Location nextPoint;
		boolean nextStart = true;
		
		if (start)
		{
			nextPoint = ((RoadSegment)info.segment.get(segmentID)).endPoint;
            if (info.intersections == null) throw new RuntimeException("Eek!");
			intersectingRoads = info.intersections.findIntersectingRoads(nextPoint).getRoads();
		}
		else
		{
			nextPoint = ((RoadSegment)info.segment.get(segmentID)).startPoint;
            if (info.intersections == null) throw new RuntimeException("Eek!");
			intersectingRoads = info.intersections.findIntersectingRoads(nextPoint).getRoads();
		}
		
		// convert segments to nodes
		Iterator it = intersectingRoads.iterator();
		while (it.hasNext())
		{
			rs = (RoadSegment)it.next();
			
			// do not include removed streets
			if (info.segment.get(rs.selfIndex) == null) continue;		
			
			// determine whether next segment's start point or end point is connected 
			// at the intersection
			if (rs.endPoint.distance(point)<rs.startPoint.distance(point)) nextStart = false; 
			else nextStart = true;
			
			if (firstNode && rs.getSelfIndex()==segmentID) // sometimes the only way to get somewhere is for the node to turn around
			{
				if (rs.endPoint.distance(point)==0) nextPoint = rs.startPoint;
				else nextPoint = rs.startPoint;
				nextStart = !start;
				SegmentNode temp = new SegmentNode(nextPoint, rs.selfIndex, nextStart, false);
				temp.costFromStart = this.costFromStart + 40000; // big penalty for turning around
				segmentNodes.add(temp);
				continue;				
			}
			
			segmentNodes.add(new SegmentNode(nextPoint, rs.selfIndex, nextStart, false));            
		}		
		return segmentNodes;	
	}
	
	/* (non-Javadoc)
	 * @see jist.swans.misc.AStarNode#getNeighbors()
	 */
	public static LinkedList masterNeighborList = new LinkedList(); 
	public List getNeighbors(Location origin, float distanceLimit) {
		LinkedList segmentNodes = new LinkedList();
		LinkedList intersectingRoads;
		RoadSegment rs;
		SegmentNode sn; 
		Location nextPoint;
		boolean nextStart = true;
		int count = -1; 
		
		if (start)
		{
			nextPoint = ((RoadSegment)info.segment.get(segmentID)).endPoint;
            if (info.intersections == null) throw new RuntimeException("Eek!");
			intersectingRoads = info.intersections.findIntersectingRoads(nextPoint).getRoads();
		}
		else
		{
			nextPoint = ((RoadSegment)info.segment.get(segmentID)).startPoint;
            if (info.intersections == null) throw new RuntimeException("Eek!");
			intersectingRoads = info.intersections.findIntersectingRoads(nextPoint).getRoads();
		}
		
		// convert segments to nodes
		Iterator it = intersectingRoads.iterator();
		while (it.hasNext())
		{		
			rs = (RoadSegment)it.next();
			
				// do not include removed streets
				if (info.segment.get(rs.selfIndex) == null) continue;		
				
				// determine whether next segment's start point or end point is connected 
				// at the intersection
				if (rs.endPoint.distance(point)<rs.startPoint.distance(point)) nextStart = false; 
				else nextStart = true;
				
				if (firstNode && rs.getSelfIndex()==segmentID) // sometimes the only way to get somewhere is for the node to turn around
				{
					if (rs.endPoint.distance(point)==0) nextPoint = rs.startPoint;
					else nextPoint = rs.startPoint;
					nextStart = !start;
					SegmentNode temp = new SegmentNode(nextPoint, rs.selfIndex, nextStart, false);
					temp.costFromStart = this.costFromStart + 40000; // big penalty for turning around
					segmentNodes.add(temp);
					continue;
				}
				
				segmentNodes.add(new SegmentNode(nextPoint, rs.selfIndex, nextStart, false));
				count++;
				
				if(!masterNeighborList.contains(segmentNodes.get(count)) && (origin.distance(rs.startPoint) < distanceLimit || origin.distance(rs.endPoint) < distanceLimit))			//only add unique nodes to the master list that are within the distanceLimit
				{
					masterNeighborList.add(new SegmentNode(nextPoint, rs.selfIndex, nextStart, false));
					if(origin.distance(rs.startPoint) < distanceLimit)
					{
						sn = new SegmentNode(rs, rs.getStartPoint());  
						return sn.getNeighbors(origin, distanceLimit); 
					}
					if(origin.distance(rs.endPoint) < distanceLimit)
					{
						sn = new SegmentNode(rs, rs.getEndPoint());  
						return sn.getNeighbors(origin, distanceLimit); 
					}
				}								
		}
		return masterNeighborList; 
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String s = "";
		s+= "SegmentNode: point: " + point + "start: " + start +"\n";
		return s+((RoadSegment)info.segment.get(segmentID)).printStreetName(info.streets);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
//	public int compareTo(Object other) {
//		if (((SegmentNode)other).segmentID == segmentID)
//		{
//			return 0;
//		}
//		else return 1;
//		
//	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {

		return ((SegmentNode)obj).segmentID == segmentID;

	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public SegmentNode getClone()
	{
		SegmentNode sn = new SegmentNode((Location2D)point.getClone(), segmentID, start, firstNode);
		return sn;
	}
}