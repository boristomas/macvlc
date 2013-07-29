/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         SpatialStreets.java
 * RCS:          $Id: SpatialStreets.java,v 1.1 2007/04/09 18:49:41 drchoffnes Exp $
 * Description:  SpatialStreets class (see below)
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
 * NOTE: This is derived from Spatial.java
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
import java.util.ListIterator;

import jist.swans.Main;
import jist.swans.field.StreetMobility;
import jist.swans.misc.Location;
import jist.swans.misc.Util;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The SpatialStreets class is used to store map information efficiently.
 * The subclasses define structure for a quad tree. This was clearly 
 * dervied from SWANS's Spatial class.
 */
public abstract class SpatialStreets
{
    
    //////////////////////////////////////////////////
    // locals
    //

    /** SpatialStreets road structure endpoints. */
    protected Location bl, br, tl, tr;
    /** Number of nodes in road structure. */
    protected int size;
    private static int intersectionResolution;
    static protected int totalHeight = -1;
    ListIterator it = null;
    public static boolean fuzzy = false;

    //////////////////////////////////////////////////
    // initialize
    //

    /**
     * Create new (abstract) bin.
     *
     * @param tr top-right corner location
     */
    public SpatialStreets(Location tr, int ir)
    {
      this(new Location.Location2D(0, 0), tr, ir);
    }

    /**
     * Create new (abstract) bin.
     *
     * @param bl bottom-left corner location
     * @param tr top-right corner location
     */
    public SpatialStreets(Location bl, Location tr, int ir)
    {
      this(bl, 
          new Location.Location2D(tr.getX(), bl.getY()),
          new Location.Location2D(bl.getX(), tr.getY()),
          tr, ir);
    }

    /**
     * Create new (abstract) bin.
     *
     * @param bl bottom-left corner location
     * @param br bottom-right corner location
     * @param tl top-left corner location
     * @param tr top-right corner location
     */
    public SpatialStreets(Location bl, Location br, Location tl, Location tr, int intersectionReslution)
    {
      this.bl = (Location)bl;
      this.br = (Location)br;
      this.tl = (Location)tl;
      this.tr = (Location)tr;
      this.intersectionResolution = intersectionReslution;
    }

    /**
     * Add a radio to bin.
     *
     * @param roadSegment radio information (location inside bin limits)
     * @param start true if adding the start point of a road segment
     */
    public abstract void add(RoadSegment roadSegment, boolean start);

    /**
     * Delete a radio from bin.
     *
     * @param roadSegment radio information (location inside bin limits)
     */
    public abstract void del(RoadSegment roadSegment);


    /**
     * Returns a linked list of road segments that intersect a point.
     *
     * @param point point to join
     * @return linked list of segments
     */
    public abstract Intersection findIntersectingRoads(Location point);
    
    public abstract Iterator iterator();
    
    /**
     * Get nearest corner to location.
     *
     * @param src location <b>outside</b> bin
     * @return location of nearest corner to given location
     */
    public Location getNearest(Location src)
    {
      if(Main.ASSERT) Util.assertion(!src.inside(bl, tr));
      // cases: 
      //   3  6  9 
      //   2  x  8
      //   1  4  7
      if(src.getX()<=bl.getX())
      {
        if(src.getY()<=bl.getY())
        {
          return bl; // case 1
        }
        else if(src.getY()>=tr.getY())
        {
          return tl; // case 3
        }
        else
        {
          return new Location.Location2D(bl.getX(), src.getY()); // case 2
        }
      }
      else if(src.getX()>=tr.getX())
      {
        if(src.getY()<=bl.getY())
        {
          return br; // case 7
        }
        else if(src.getY()>=tr.getY())
        {
          return tr; // case 9
        }
        else
        {
          return new Location.Location2D(tr.getX(), src.getY()); // case 8
        }
      }
      else
      {
        if(src.getY()<=bl.getY())
        {
          return new Location.Location2D(src.getX(), bl.getY()); // case 4
        }
        else if(src.getY()>=tr.getY())
        {
          return new Location.Location2D(src.getX(), tr.getY()); // case 6
        }
        else
        {
          throw new RuntimeException("get nearest undefined for internal point");
        }
      }
    }

    /**
     * Return number of radios in bin.
     *
     * @return number of radios in bin
     */
    public int getSize()
    {
      return size;
    }

    /**
     * Compute area of bin.
     *
     * @return bin area
     */
    public double area()
    {
      float dx = tr.getX() - bl.getX();
      float dy = tr.getY() - bl.getY();
      return dx*dy;
    }

    /**
     * Return top-right coordinate.
     *
     * @return top-right coordinate
     */
    public Location getTopRight()
    {
      return tr;
    }

    /**
     * Return bottom-left coordinate.
     *
     * @return bottom-left coordinate
     */
    public Location getBottomLeft()
    {
      return bl;
    }
    
    /**
     * Linear-lookup (no binning).
     */
    public static class LinearList extends SpatialStreets
    {
      /** list of radios in bin. */
      private LinkedList intersectionList = new LinkedList();

    private static final boolean DEBUG = false;

      /**
       * Create a new linear-lookup bin.
       *
       * @param tr top-right corner location
       */
      public LinearList(Location tr, int ir)
      {
        super(tr, ir);
      }

      /**
       * Create a new linear-lookup bin.
       *
       * @param bl bottom-left corner location
       * @param tr top-right corner location
       */
      public LinearList(Location bl, Location tr, int ir)
      {
        super(bl, tr, ir);
      }

      /**
       * Create a new linear-lookup bin.
       *
       * @param bl bottom-left corner location
       * @param br bottom-right corner location
       * @param tl top-left corner location
       * @param tr top-right corner location
       */
      public LinearList(Location bl, Location br, Location tl, Location tr, int ir)
      {
        super(bl, br, tl, tr, ir);
      }

      /**
       * Determine whether bin radio list contains a cycle.
       *
       * @return whether bin radio list contains a cycle
       */
//      private boolean hasCycle()
//      {
//        boolean passed = false;
//        for(RoadSegment dst=roadList; dst!=null; dst=dst.nextEnd)
//        {
//          if(dst==roadList && passed) return true;
//          passed = true;
//        }
//        passed = false;
//        for(RoadSegment dst=roadList; dst!=null; dst=dst.nextStart)
//        {
//          if(dst==roadList && passed) return true;
//          passed = true;
//        }
//        return false;
//      }

      /** {@inheritDoc} */
      public void add(RoadSegment road, boolean start)
      {
          Intersection is;
          
        if(Main.ASSERT){
           if (start) {
               Util.assertion(road.startPoint.inside(bl, tr));
           }
           else {
               Util.assertion(road.endPoint.inside(bl, tr));

           }
        }

        if (start)
        {
            if (intersectionList.size()==0)
            {
                is = new Intersection(road.startPoint);
                is.addStreet(road);
                intersectionList.add(is);
            }
            else
            {
                boolean found=false;
                ListIterator it = intersectionList.listIterator();
                // find matching intersection
                while (it.hasNext())
                {
                    is = (Intersection)it.next();
                    if (is.loc.distance(road.startPoint)<intersectionResolution)
                    {
                        is.addStreet(road);
                        found = true;
                    }
                    
                }
                if (!found)
                {
                    is = new Intersection(road.startPoint);
                    is.addStreet(road);
                    intersectionList.add(is);
                }
            }
	        size++;
        }
        else
        {
            if (intersectionList.size()==0)
            {
                is = new Intersection(road.endPoint);
                is.addStreet(road);
                intersectionList.add(is);
            }
            else
            {
                boolean found=false;
                ListIterator it = intersectionList.listIterator();
                // find matching intersection
                while (it.hasNext())
                {
                    is = (Intersection)it.next();
                    if (is.loc.distance(road.endPoint)<intersectionResolution)
                    {
                        is.addStreet(road);
                        found = true;
                    }
                    
                }
                if (!found)
                {
                    is = new Intersection(road.endPoint);
                    is.addStreet(road);
                    intersectionList.add(is);
                }
            }
	        size++;
        }
      }

      /** {@inheritDoc} */
      public void del(RoadSegment road)
      {

            throw new RuntimeException("Delete not implemented!");
      }
      
      public Intersection findIntersectingRoads(Location point)
      {
          Intersection result = null;
          
          ListIterator li = intersectionList.listIterator();
          while (li.hasNext())
          {
              result = (Intersection)li.next();

              if (result.loc.distance(point)<StreetMobility.INTERSECTION_RESOLUTION)
              {
                  return result;
              }
          }
          if (fuzzy)
          {
              li = intersectionList.listIterator();
              float minDist = Float.MAX_VALUE;
              Intersection tempResult =null;
              while (li.hasNext())
              {
                  tempResult = (Intersection)li.next();

                  if (tempResult.loc.distance(point)<minDist)
                  {
                      minDist = tempResult.loc.distance(point);
                      result = tempResult;
                  }
              }
                            
              fuzzy = false;
              return result;
          }
          if (DEBUG)
          {
              System.err.println("Intersection not found!\nLocation requested: "+point);
              	          
	          li = intersectionList.listIterator();
	          while (li.hasNext())
	          {
	              result = (Intersection)li.next();
	              
	              System.err.println("Intersection at: " + result.loc);
	
	          }
          }
          
          return null;
      }

    public Iterator iterator() {
        ListIterator li =intersectionList.listIterator();
        if (it == null) it = li;
        else{
            while (it.hasNext())
            {
                li.add(li.next());
            }
        }
        return li;
    }

    } // class: LinearList


//////////////////////////////////////////////////
// hierarchical grid implementation
//

/**
 * Hierarchical binning.
 */
public static class HierGrid extends SpatialStreets
{
  /** sub-bin constants. */
  public static int BL=0, BR=1, TL=2, TR=3;
  /** array of sub-bins. */
  private final SpatialStreets[] bins;
  /** bin mid-point. */
  private final Location mid;  
  private final int currentHeight;

  /**
   * Create new hierarchical bin.
   *
   * @param tr top-right corner location
   * @param height height in bin tree
   */
  public HierGrid(Location tr, int height, int ir)
  {
    this(new Location.Location2D(0,0), tr, height, ir);
  }

  /**
   * Create new hierarchical bin.
   *
   * @param bl bottom-left corner location
   * @param tr top-right corner location
   * @param height height in bin tree
   */
  public HierGrid(Location bl, Location tr, int height, int ir)
  {
    this(bl, 
        new Location.Location2D(tr.getX(), bl.getY()),
        new Location.Location2D(bl.getX(), tr.getY()),
        tr, height, ir);
  }

  /**
   * Create new hierarchical bin.
   *
   * @param bl bottom-left corner location
   * @param br bottom-right corner location
   * @param tl top-left corner location
   * @param tr top-right corner location
   * @param height height in bin tree
   */
  public HierGrid(Location bl, Location br, Location tl, Location tr, int height, int ir)
  {    
    super(bl, br, tl, tr, ir);
    if (totalHeight==-1) totalHeight = height;
    currentHeight = height;
        
    //System.err.println("Grid bounds: " + bl + " , " + br +" , "+tl+" , "+tr);
    if(Main.ASSERT) Util.assertion(height>0);
    mid = new Location.Location2D((bl.getX()+tr.getX())/2, (bl.getY()+tr.getY())/2);
    Location left = new Location.Location2D(bl.getX(), mid.getY());
    Location right = new Location.Location2D(tr.getX(), mid.getY());
    Location top = new Location.Location2D(mid.getX(), tr.getY());
    Location bottom = new Location.Location2D(mid.getX(), bl.getY());
    height--;
    bins = new SpatialStreets[4];
    if(height>0)
    {
      bins[BL] = new HierGrid(bl, bottom, left, mid, height, ir);
      bins[BR] = new HierGrid(bottom, br, mid, right, height, ir);
      bins[TL] = new HierGrid(left, mid, tl, top, height, ir);
      bins[TR] = new HierGrid(mid, right, top, tr, height, ir);
    }
    else
    {
      bins[BL] = new LinearList(bl, bottom, left, mid, ir);
      bins[BR] = new LinearList(bottom, br, mid, right, ir);
      bins[TL] = new LinearList(left, mid, tl, top, ir);
      bins[TR] = new LinearList(mid, right, top, tr, ir);
    }
  }

  /**
   * Helper method to determine sub-bin for location.
   *
   * @param loc location to descend towards
   * @return sub-bin containing location
   */
  private SpatialStreets getBin(Location loc)
  {
    return loc.getX()<mid.getX() 
      ? (loc.getY()<mid.getY() ? bins[BL] : bins[TL])
      : (loc.getY()<mid.getY() ? bins[BR] : bins[TR]);
  }

  /** {@inheritDoc} */
  public void add(RoadSegment rs, boolean start)
  {
    if(Main.ASSERT){
        if (start)Util.assertion(rs.startPoint.inside(bl, tr));
        else Util.assertion(rs.endPoint.inside(bl, tr));
    }

    if (start) getBin(rs.getStartPoint()).add(rs, start);
    else getBin(rs.getEndPoint()).add(rs, start);
    size++;
  }

  /** {@inheritDoc} */
  public void del(RoadSegment rs)
  {
      if(Main.ASSERT){
          Util.assertion(rs.startPoint.inside(bl, tr));
          Util.assertion(rs.endPoint.inside(bl, tr)); // should not compare both, but delete is not implemented
      }

      getBin(rs.startPoint).del(rs);
      getBin(rs.endPoint).del(rs);
    size--;
  }

  /** {@inheritDoc} */
  public RoadSegment move(RoadSegment rs, Location l2)
  {
      throw new RuntimeException("RoadSegment::move: not implemented");
  }
  
  public Intersection findIntersectingRoads(Location point)
  {      
      Intersection i = getBin(point).findIntersectingRoads(point);
      
      // disgusting hack to get intersections that are in different bins
      if (i==null)
      {
//          int levels = totalHeight - currentHeight;
//          int divisor = 1 << (levels+1);
//          double halfDiagonal = 1.414*bl.distance(br)/divisor;
//          
//          Location l1 = new Location.Location2D(point.getX(), 
//                  point.getY()-intersectionResolution);
//          Location l2 = new Location.Location2D(point.getX(), 
//                  point.getY()+intersectionResolution);
//          Location l3 = new Location.Location2D(point.getX()
//                  +intersectionResolution, point.getY());
//          Location l4 = new Location.Location2D(point.getX()
//                  -intersectionResolution, point.getY());
//          if (l1.distance(mid)<= halfDiagonal)
//          {
              i=bins[BL].findIntersectingRoads(point);
              if (i!= null && i.loc.distance(point)<StreetMobility.INTERSECTION_RESOLUTION) return i;
//          }
//          if (l2.distance(mid)<= halfDiagonal)
//          {
              i=bins[BR].findIntersectingRoads(point);
              if (i!= null && i.loc.distance(point)<StreetMobility.INTERSECTION_RESOLUTION) return i;
//          }
//          if (l3.distance(mid)<= halfDiagonal)
//          {
              i=bins[TL].findIntersectingRoads(point);
              if (i!= null && i.loc.distance(point)<StreetMobility.INTERSECTION_RESOLUTION) return i;
//          }
//          if (l4.distance(mid)<= halfDiagonal)
//          {
              i=bins[TR].findIntersectingRoads(point);
              if (i!= null && i.loc.distance(point)<StreetMobility.INTERSECTION_RESOLUTION) return i;
//          }
      }
      return i;
  }
  
  public Iterator iterator()
  {
      it = null;      
      bins[BL].iterator();
      bins[BR].iterator();
      bins[TL].iterator();
      bins[TR].iterator();
      return it;
  }

} // class: HierGrid


}
