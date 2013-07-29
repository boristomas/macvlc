/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         SegmentNodeInfo.java
 * RCS:          $Id: SegmentNodeInfo.java,v 1.1 2007/04/09 18:49:41 drchoffnes Exp $
 * Description:  SegmentNodeInfo class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 8, 2004
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

import java.util.Vector;
import java.util.HashMap;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The SegmentNodeInfo class contains information that the 
 * A* Search algorithm needs to find paths.
 * 
 */
public class SegmentNodeInfo {

    /** Array of road segments in field */
    public Vector segment;
    /** Array of shapes of road segments in field */
    public HashMap shapes;
    /** quad tree representation of segments */
    public SpatialStreets.HierGrid intersections;
    /** map between street indeces and street names */
    public HashMap streets;
    /** A list of cached SegmentNodes; probably will not be used due to mutation of SN's */
    public Vector segmentNodes;
    /** congestion data */
    public CongestionMonitor cm = null;
    public SegmentNode dest;

    /**
     * SegmentNodeInfo constructor.
     * @param segment vector of RoadSegments
     * @param shapes HashMap of Shapes
     * @param intersections QuadTree of intersections
     * @param streets HashMap of street names
     * @param segmentNodes Vector of SegmentNode objects
     */
    public SegmentNodeInfo(Vector segment, HashMap shapes,
            SpatialStreets.HierGrid intersections, HashMap streets, Vector segmentNodes) {
        super();
        this.segment = segment;
        this.shapes = shapes;
        this.intersections = intersections;
        this.streets = streets;
        this.segmentNodes = segmentNodes;
    }
}
