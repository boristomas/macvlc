/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetMobilityInfo.java
 * RCS:          $Id: StreetMobilityInfo.java,v 1.1 2007/04/09 18:49:28 drchoffnes Exp $
 * Description:  StreetMobilityInfo class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Feb 24, 2005
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

import java.util.LinkedList;

import driver.VisualizerInterface;
import jist.swans.field.Mobility.MobilityInfo;
import jist.swans.field.streets.Intersection;
import jist.swans.field.streets.RoadSegment;
import jist.swans.field.streets.Shape;
import jist.swans.misc.Location;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StreetMobilityInfo class contains state for all street 
 * mobility models.
 */
public class StreetMobilityInfo implements MobilityInfo {

    /** the road segment currently being traveled */
    public RoadSegment current;
    /** the location of the end of this segment in the current direction */
    public Location rsEnd; 
    /** the next intersection */
    Intersection nextIs;

    /** current speed */
    public float currSpeed=0;
    /** maximum speed */
    float maxSpeed=0;
    /** average acceleration rate */
    public float acceleration = 2.23f; // 5 mph per second
    /** remaining distance along segment */
    float remainingDist=0;
    /** next car in front of current car */
    public StreetMobilityInfo nextCar = null;
    /** current lane */
    public LinkedList currentLane;
    /** next road segment */
    public RoadSegment nextRS = null;
    /** next end */
    public Location nextEnd =null;
    /** location of the vehicle at the center of the road */
    Location offset = null;
    
    /** If this is a multiline segment, determines which point we're at */
    int ShapePointIndex=-1;
    /** time for each step to take place (seconds) */
    public float stepTime=1;
    
    /** extra speed added by driver */
    public float extraSpeed = 0;
    
    /** reaction time */
    public float spacingBeta = 0.75f;
    /** reciprocal of twice the maximum average deceleration (units: s^2/m)*/
    public float spacingGamma = 0.0070104f;
    
    /** waiting to take a turn? */
    public boolean waiting = false;
    
    /** optional statistical information */
    /** accumulator for average speed */
    float speedSum=0;
    /** linked list for roads taken */
    LinkedList roads= new LinkedList();
    
    public VisualizerInterface v = null;
    /** number of times this node has waited to make a turn */
    public int waitCount = 0;
    
    public boolean isStopped = false;
    
    /**
     * 
     */
    public StreetMobilityInfo() {
        super();
    }
    
    // TODO implement vehicle size in here      
    public double getRemainingDist()
    {
    	return remainingDist;
    }
    public StreetMobilityInfo getNextCar()
    {
    	return nextCar;
    }
    
    /**
     * Sets the max speed according to the driver's habits.
     * @param limit the posted speed limit
     */
    public void setMaxSpeed(float limit)
    {
        // vague evidence online seems to indicate that this distribution is Gaussian
        maxSpeed = limit + extraSpeed;
        
    }

    /**
     * @return
     */
    public RoadSegment getCurrentRS() {
        // TODO Auto-generated method stub
        return current;
    }

    /**
     * @return
     */
    public Location getRSEnd() {
        // TODO Auto-generated method stub
        return rsEnd;
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility.MobilityInfo#getSpeed()
     */
    public float getSpeed() {
        
        return currSpeed;
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility.MobilityInfo#getBearing()
     */
    public Location getBearing() {
        if (nextEnd==null) return new Location.Location2D(0,0); // TODO something better here...
        if (nextEnd.distance(current.getEndPoint())==0)
        {
            return current.getStartPoint().bearing(current.getEndPoint());
        }
        else
        {
            return current.getEndPoint().bearing(current.getStartPoint());
        }
    }
    
	/**
	 * @param start is the starting point
	 * @param end is the destination
	 * returns an angle we call theta to discover the vlc device bounds via the getVLCBounds() method
	 */
	public float getBearingAsAngle()
	{
		float bearingAngle = 0.0f;
		
		bearingAngle = (float) Math.toDegrees(Math.atan2(current.getEndPoint().getY() - current.getStartPoint().getY(), current.getEndPoint().getX() - current.getStartPoint().getX()));
		
		if(bearingAngle < 0)
		{
			return bearingAngle + 360; 
		}
		else
		{
			return bearingAngle;
		}
	}

    /**
     * @return
     */
    public Intersection getNextIntersection() {
        return nextIs;
    }

    /** Returns the speed adjusted for driver behavior */
    public int getAdjustedSpeed() {
        double max = current.getSpeedLimit()*0.2;
        double min = current.getSpeedLimit()*(-0.2);
        if (extraSpeed < 0 ) 
        {
            return (int)(current.getSpeedLimit()+(extraSpeed > min ? extraSpeed : min));
        }
        else 
            return (int)(current.getSpeedLimit()+(extraSpeed < max ? extraSpeed : max));
    }

    public RoadSegment getNextRS() {
        // TODO Auto-generated method stub
        return nextRS;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
//    @Override
    public String toString() {
        return "Current RS: " + current + "\nRemaining dist: "+remainingDist+"\n";
    }

    public LinkedList getCurrentLane() {
        return currentLane;
    }

    public void setCurrentLane(LinkedList list) {
        currentLane = list;
        
    }

	public double getCarSpacing() {
		return (RoadSegment.CAR_LENGTH+RoadSegment.SPACE)+
			currSpeed*(spacingBeta+currSpeed*spacingGamma);
	}

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility.MobilityInfo#setStopped(boolean)
     */
    public void setStopped(boolean stop) {
        isStopped = stop;
        
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility.MobilityInfo#isStopped()
     */
    public boolean isStopped() {
        return isStopped;
    }

    public Location move(Location start, Location end, 
            float distance)
    {
        float hyp = end.distance(start);
        if (distance == hyp) return end;
        float portion = distance/hyp;
        
//        if (distance < 0.01) return end;
        if (StreetMobility.DEBUG)System.out.println("Length: "+ hyp+ " / Distance: "+distance);
        
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
    
	public Location pointAt(Location curr, StreetMobilityInfo info,float dist) 
    {    	
        Shape shape;
        float partialDist=0;
        Location newLocation = curr; // start at current location
        
        StreetMobilityInfo smi = (StreetMobilityInfo)info;
        
        // find shape
        shape = (Shape)StreetMobility.shapes.get(new Integer(smi.current.getShapeIndex()));
        int shapePointIndex = smi.ShapePointIndex; // find index into set of points
        
        // case vehicle is moving toward the "start"
        if (smi.current.getStartPoint().distance(smi.rsEnd)<=StreetMobility.INTERSECTION_RESOLUTION) // move in descending order
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

}



