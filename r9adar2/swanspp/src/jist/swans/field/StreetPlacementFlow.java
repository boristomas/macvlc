/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetPlacementFlow.java
 * RCS:          $Id: StreetPlacementFlow.java,v 1.1 2007/04/09 18:49:28 drchoffnes Exp $
 * Description:  StreetPlacementFlow class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jul 25, 2005 at 12:22:20 PM
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
import java.util.Iterator;
import java.util.LinkedList;

import driver.Visualizer;

import jist.swans.Constants;
import jist.swans.field.StreetMobilityOD.StreetMobilityInfoOD;
import jist.swans.field.streets.Intersection;
import jist.swans.field.streets.RoadSegment;
import jist.swans.field.streets.SegmentNode;
import jist.swans.field.streets.SpatialStreets;
import jist.swans.misc.AStarSearch;
import jist.swans.misc.Location;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StreetPlacementFlow class ...
 */
public class StreetPlacementFlow extends StreetPlacement {

    final private boolean DEBUG=false;
    private final static boolean SHOW_ROUTES = false;

    RoadSegment starts[];
    RoadSegment ends[];
    /** the index into the flows for next placement */
    private int currentIndex = 0;
    

    /**
     * 
     * @param bl
     * @param tr
     * @param smr
     * @param stdDev
     * @param stepTime
     * @param starts
     * @param ends
     * @param staticNodes the number of static nodes
     */
    public StreetPlacementFlow(Location bl, Location tr, StreetMobility smr,
            double stdDev, double stepTime, Location[] starts, Location[] ends, int staticNodes)
    {
        super(bl, tr, smr, stdDev, stepTime);
        numberOfNodes = staticNodes;
        convertFromLocations(starts, ends);
        if (SHOW_ROUTES) showRoutes();
    }
    
    /**
     * 
     */
    private void showRoutes() {
        LinkedList paths = new LinkedList();
        for (int i = 0; i < starts.length; i++){
            if (starts[i]!=null && ends[i]!=null){
                SegmentNode startNode = new SegmentNode(starts[i].getStartPoint(), 
                        starts[i].getSelfIndex(), true, true);        
                SegmentNode endNode = new SegmentNode(ends[i].getEndPoint(), 
                        ends[i].getSelfIndex(), false, false);
                SegmentNode.info.dest = endNode;                
                AStarSearch ass = new AStarSearch(null); // TODO rename variable
                LinkedList path = ass.findPath(startNode, endNode); // find the path
                paths.addAll(path);
            }
        }
        Visualizer.getActiveInstance().colorSegments(
                segmentNodeToRS(paths.toArray()), Color.RED);       
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @param objects
     * @return
     */
    private RoadSegment[] segmentNodeToRS(Object[] sn) {
        RoadSegment rs[] = new RoadSegment[sn.length];
        for (int i = 0; i < sn.length; i++) rs[i] = 
            (RoadSegment) StreetMobility.getStreetMobility().getSegments().get(
                    ((SegmentNode)sn[i]).segmentID);
        return rs;
    }

    private void convertFromLocations(Location[] s, Location[] e) {
        Intersection is;
        LinkedList ll;
        RoadSegment rs = null, rsTemp;
        Iterator it;
        float minDist;
        starts = new RoadSegment[s.length];
        ends = new RoadSegment[e.length]; 
        
        // convert to system coordinates
        for (int i = 0; i < s.length; i++){
            s[i] = sm.convertFromStreets(s[i]);
            e[i] = sm.convertFromStreets(e[i]);
        }
        
        // get road segments
        for (int i = 0; i < s.length; i++){
            
            SpatialStreets.fuzzy = true;
            is = sm.intersections.findIntersectingRoads(s[i]);
            if (is==null) continue; // TODO more intelligent things
            ll = is.getRoads();
            it = ll.iterator();
            minDist = Float.MAX_VALUE;
            while (it.hasNext())
            {
                rsTemp = (RoadSegment)it.next();
                if (rsTemp.getStartPoint().distance(e[i])< minDist)
                {
                    minDist = rsTemp.getStartPoint().distance(e[i]);
                    rs = rsTemp;
                }
                if (rsTemp.getEndPoint().distance(e[i])< minDist)
                {
                    minDist = rsTemp.getEndPoint().distance(e[i]);
                    rs = rsTemp;
                }
            }
            starts[i] = rs;
        }
        
        for (int i = 0; i < e.length; i++){
            
            SpatialStreets.fuzzy = true;
            is = sm.intersections.findIntersectingRoads(e[i]);
            if (is==null) continue; // TODO more intelligent things
            ll = is.getRoads();
            it = ll.iterator();
            minDist = Float.MAX_VALUE;
            while (it.hasNext())
            {
                rsTemp = (RoadSegment)it.next();
                if (rsTemp.getStartPoint().distance(s[i])< minDist)
                {
                    minDist = rsTemp.getStartPoint().distance(s[i]);
                    rs = rsTemp;
                }
                if (rsTemp.getEndPoint().distance(s[i])< minDist)
                {
                    minDist = rsTemp.getEndPoint().distance(s[i]);
                    rs = rsTemp;
                }
            }
            ends[i] = rs;
        }
        
    }
    
    private void convertFromIntersections(String[] s, String[] e) {
        
//      HierGrid ss = sm.intersections;
//      starts = new RoadSegment[s.length];
//      ends = new RoadSegment[e.length]; 
//      // find potential segments
//      
//      // find all street indexes
//      Iterator it = sm.streets.entrySet().iterator();
//      StreetName sn;
//      
//      while (it.hasNext())
//      {
//      Map.Entry ent = (Map.Entry)it.next();
//      
//      sn = (StreetName)it.next();
//      for (int i = 0; i < )
//      if (sn.getName().compareToIgnoreCase())
//      }
//      // find all road segments with those street names
//      
//      // look at all intersections for rs ids
//      
//      
//      it = sm.segments.iterator();
//      while (it.hasNext())
//      {
//      RoadSegment rs = (RoadSegment)it.next();
//      StreetName sn = (StreetName)sm.streets.get(new Integer(rs.getStreetIndex()));
//      for (int i = 0; i < s.length; i++){
//      String split[] = s[i].split(" & ");
//      if (sn.getName().compareToIgnoreCase(split[0])==0)
//      {
//      
//      }
//      }
//      }           
        
    }

    public void setNextIndex(int nextIndex)
    {
        currentIndex = nextIndex;
    }

	int setInitialSegment(StreetMobilityInfo smri) {
        int direction=0;
        boolean valid = false; // did this find a valid street pair?
        
        ((StreetMobilityInfoOD)smri).config = Constants.MOBILITY_STREET_FLOW;
        
        // TODO better workaround
        while (starts[currentIndex]==null || ends[currentIndex]==null){
            if (currentIndex < starts.length-1) currentIndex++;
            else currentIndex = 0;
        }
        
        rs = starts[currentIndex];

        smri.current = rs;
        // set the rsEnd for calculating path
        if (rs.getStartPoint().distance(ends[currentIndex].getStartPoint()) < 
                rs.getEndPoint().distance(ends[currentIndex].getStartPoint()))
        {
            smri.rsEnd = rs.getStartPoint();
            direction = TO_START;
        }
        else
        {
            smri.rsEnd = rs.getEndPoint();
            direction = TO_END;
        }
        
        StreetMobilityOD smod = (StreetMobilityOD)sm;               
        
        // pick random dest road segment            
        RoadSegment dest = ends[currentIndex];
        
        // calculate OD path
        valid = smod.calculatePath((StreetMobilityInfoOD)smri, rs, smri.rsEnd, dest);
        
//        smod.showPath(smri.path.toArray(), Color.ORANGE);
        // only valid path may force car to be in other lane
        if (valid)
        {
            StreetMobilityInfoOD smiod=(StreetMobilityInfoOD)smri;
            SegmentNode sn = (SegmentNode)smiod.path.getFirst();
            RoadSegment rsTemp = (RoadSegment)sm.segments.get(sn.segmentID);
            
            if (DEBUG)System.out.println("Current rsEnd: " + smri.rsEnd);
            if (DEBUG)System.out.println("Current road: " + rs.printStreetName(sm.streets));                                
            
            if (!(smri.rsEnd.distance(rsTemp.getStartPoint())==0 
                    || smri.rsEnd.distance(rsTemp.getEndPoint())==0)){
                if (direction == TO_START)
                {
                    smri.rsEnd = rs.getEndPoint();
                    direction = TO_END;
                }
                else {
                    smri.rsEnd = rs.getStartPoint();
                    direction = TO_START;
                }  
            }
        }

        
        if (sm.v!=null)sm.v.drawCircle(30, smri.rsEnd);
        
        return direction;
	}
    
    
}// class: StreetPlacementFlow



