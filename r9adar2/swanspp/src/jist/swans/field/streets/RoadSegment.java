/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         RoadSegment.java
 * RCS:          $Id: RoadSegment.java,v 1.1 2007/04/09 18:49:42 drchoffnes Exp $
 * Description:  RoadSegment class (see below)
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.swans.field.StreetMobilityInfo;
import jist.swans.misc.Location;
/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The RoadSegment class contains information about a piece of road between two
 * intersections. 
 */
public class RoadSegment {
    
	/** hash map of street names */
	public static HashMap streets = new HashMap();
    /** number of bytes in a road segment object */
    public final int ROAD_SEGMENT_SIZE = 41;
    int startAddressLeft;
    int endAddressLeft;
    int startAddressRight;
    int endAddressRight;
    int streetIndex;
    int shapeIndex;
    int selfIndex; // this segment's index in the array of segments
    Location startPoint;
    Location endPoint;
    char roadClass;  // the two numbers after 'A'
    
    // variables for runtime functions
    /** number of vehicles on segment */
    int numberOfCars=0;
    // T-ODO: number of lanes for start and end
    /** number of lanes in each direction in segment */
    int numberOfLanes;
    /** length of road segment */
    float length;
    /** maximum number of cars allowed in each lane for segment */
    int maxCars;
    /** average vehicle length in meters, 
     * from http://www.ite.org/traffic/documents/AB00H1903.pdf 
    */
    public static final int CAR_LENGTH = 8;
    /** stationary space between vehicles in meters */
    public static final int SPACE = 1;

    /** contains queue of cars on road heading toward endPoint */
    LinkedList carsToEnd[];
    /** contains queue of cars on road heading toward startPoint */
    LinkedList carsToStart[];    


    /**
     * 
     */
    public RoadSegment() {
        startPoint=null;
        endPoint=null;
    }
    
    /**
     * @param startAddressLeft
     * @param endAddressLeft
     * @param startAddressRight
     * @param endAddressRight
     * @param streetIndex
     * @param shapeIndex
     * @param startPoint
     * @param endPoint
     * @param roadClass
     */
    public RoadSegment(int startAddressLeft, int endAddressLeft,
            int startAddressRight, int endAddressRight, int streetIndex,
            int shapeIndex, Location startPoint, Location endPoint, int selfIndex,
            char roadClass) {
        super();
        this.startAddressLeft = startAddressLeft;
        this.endAddressLeft = endAddressLeft;
        this.startAddressRight = startAddressRight;
        this.endAddressRight = endAddressRight;
        this.streetIndex = streetIndex;
        this.shapeIndex = shapeIndex;
        this.startPoint = startPoint;
        this.selfIndex = selfIndex;
        this.endPoint = endPoint;
        this.roadClass = roadClass;
        
        // set number of lanes per segment
        // Primary Road with limited access/ Interstate Highway - unseparated
        if (roadClass >= 11 && roadClass <= 14)
           numberOfLanes=3;

        // Primary Road with limited access/ Interstate Highway - separated
        else if (roadClass >= 15 && roadClass <= 18)
            numberOfLanes=3;

        // Primary Road without limited access/ US Highway - unseparated
        else if (roadClass >= 21 && roadClass <= 24)
            numberOfLanes=3;

        // Primary Road without limited access / US Highway - separated
        else if (roadClass >= 25 && roadClass <= 28)
            numberOfLanes=3;

        // Secondary and Connecting Roads / State Highways - unseparated
        else if (roadClass >= 31 && roadClass <= 34)
            numberOfLanes=2;

        // Secondary and Connecting Roads / State Highways - separated
        else if (roadClass >= 35 && roadClass <= 38)
            numberOfLanes=2;
        
        // Local, Rural, Neighborhood / City Street - unseparated
        else if (roadClass >= 41 && roadClass <= 44)
            numberOfLanes=1;

        // Local, Rural, Neighborhood / City Street - separated
        else if (roadClass >= 45 && roadClass <= 48)
            numberOfLanes=2;
        // access ramp
        else if (roadClass >=62 && roadClass <=63)
            numberOfLanes=1;
        else 
        {
           System.err.println("Unknown road class " + (int)roadClass + " encountered\n");
           numberOfLanes=1;
        }
        
        // T-ODO number of lanes per direction
        carsToEnd = new LinkedList[numberOfLanes];
        carsToStart = new LinkedList[numberOfLanes];
        
        for (int i = 0; i < numberOfLanes; i++)
        {
            carsToEnd[i] = new LinkedList();
            carsToStart[i] = new LinkedList();
        }
    }
    /**
     * Returns a reference to the lane that the car was added to. The
     * lane chosen is the one with the fewest cars.
     * 
     * @param node id of node added to list... must use 1-based number
     * @param rsEnd The point at the end of the segment in the direction of motion
     * @param nodes a Vector of SMI objects
     * @return
     */
    public LinkedList addNode(StreetMobilityInfo smi, Location rsEnd, Vector nodes)
    {
		LinkedList lane;
		LinkedList lanes[]; // lanes in one direction
    	int index = -1; 
    	int minCars = 1000; // really big
    	if (smi.nextRS!=this) throw new RuntimeException("Adding to wrong street!");
        
//        if (node.intValue() <= nodes.size() &&
//                ((StreetMobilityInfo)nodes.get(node.intValue()-1)).getNextRS()!=this)
//        {
//            throw new RuntimeException("Car added to wrong street!");
//        }
    	
    	// try to stay in current same lane across segments
    	if (smi.currentLane!=null){
    		int laneNum = smi.current.getLane(smi.currentLane);

    		if (rsEnd.distance(this.endPoint)==0 && carsToEnd.length > laneNum)    			
    			lane = carsToEnd[laneNum];
    		else if (carsToStart.length > laneNum) lane = carsToStart[laneNum];
    		else lane = null;
    			
    		// check if there is room for the new car, if not, it will try to change lanes
    		if (lane!=null && lane.size() < maxCars){
    			StreetMobilityInfo last = null;
    			if (lane.size() > 0) last = ((StreetMobilityInfo)lane.getLast());    			
    			if (last!=null && last.getRemainingDist() <= length - (CAR_LENGTH+SPACE)){
	    			lane.addLast(smi);
	    			return lane;
    			}
    			else {
//    		    	System.out.println("Node "+(nodes.indexOf(smi)+1)+": Could not keep in same lane!");
    			}
    		}
    			    		    	
    	}

        if (rsEnd.distance(this.endPoint)==0) lanes = carsToEnd;
        else if (rsEnd.distance(this.startPoint)==0) lanes = carsToStart;
        else throw new RuntimeException("Placed car on wrong street!");
        
        for (int i = numberOfLanes-1; i >-1; i--)
        {
        	if (lanes[i].size() < minCars)
        	{
        		minCars = lanes[i].size();
        		index = i;
        	}
        }
        if (minCars <= maxCars)
        {
        	int position = -1;
        	if (lanes[index].size() > 0){                    
        		// ensure that the car has room to move to next road
        		StreetMobilityInfo last = ((StreetMobilityInfo)lanes[index].getLast());
        		if (last.getRemainingDist() > 
        		length - CAR_LENGTH - SPACE)
        		{
        			return null;
        		}
        		if  (smi.nextEnd==null) // new car on map, place between existing cars
        		{
        			if (lanes[index].size() == 1)
        			{
        				if (((StreetMobilityInfo)lanes[index].getFirst()).getRemainingDist() > 4*CAR_LENGTH)
        				{
        					position = 0;
        				}
        			}
        			else{
        				ListIterator li = lanes[index].listIterator();
        				StreetMobilityInfo one, two;
        				while (li.hasNext())li.next();
        				one = (StreetMobilityInfo)li.previous();
        				while (li.hasPrevious())
        				{
        					two = (StreetMobilityInfo)li.previous();
        					if (one.getRemainingDist()-two.getRemainingDist() > CAR_LENGTH*4)
        					{
        						position = li.nextIndex()+1;
        					}
        					one = two;
        				}
        				
        			}
        		}
        	}
        	if (lanes[index].contains(smi)) throw new RuntimeException("Added redundantly!");
        	if (position ==-1) lanes[index].addLast(smi);
        	else{
        		lanes[index].add(position, smi);
        	}
            if (JistAPI.getTime()>0){
        	// T-ODO remove when lane-changing works
            ListIterator li = lanes[index].listIterator();
            StreetMobilityInfo behind, front = (StreetMobilityInfo)li.next();
            while (li.hasNext()){
                behind = (StreetMobilityInfo)li.next();
                if (behind.current == front.current && behind.nextEnd==front.nextEnd&& behind.nextEnd!=null &&
                        behind.getRemainingDist()<=front.getRemainingDist()) 
                    throw new RuntimeException("Bad following!");
                front = behind;
            }
            }
        	// T-ODO anything else?   
        	return lanes[index];
        }
        else // no room for car
        {
        	return null;
        }

        
           
    }
    
    /**
     * Determines if the cars in the proper order in a lane
     * @param list
     */
    public void checkLane(LinkedList list2) {
    	LinkedList lanes[];
    	for (int j = 0; j < 2; j++){
    		if (j==0) lanes = carsToStart;
    		else lanes = carsToEnd;
//    		// weirdness with cars being added to all three lanes
//    		HashMap count = new HashMap();
//    		for (int i = 0; i < lanes.length; i++){    			
//    			LinkedList list = lanes[i];
//    			ListIterator li = list.listIterator();
//    			while (li.hasNext()){
//    				Object o = li.next();
//    				int temp = (count.get(o)!=null)?(((Integer)count.get(o)).intValue()+1):(1);
//    				count.put(o, new Integer(temp));
//    				if (temp > 2) 
//    					throw new RuntimeException("Added to too many lanes!");
//    			}
//    		}
    	for (int i = 0; i < lanes.length; i++){
    		LinkedList list = lanes[i]; 
    		
    		
    	
    	if (list.size()==0)return;
        ListIterator li = list.listIterator();
        StreetMobilityInfo front = (StreetMobilityInfo)li.next(), behind;
        while (li.hasNext())
        {
            behind = (StreetMobilityInfo) li.next();
            if (behind.getCurrentRS() != this)
            {
                throw new RuntimeException("Car added to wrong street!");
            }
            if (behind.currentLane==front.currentLane && front.currentLane == list && behind.currentLane == list){
	            if (behind.getNextCar()!=front) // T-ODO stronger debugging - check explicitly for lane changing
	            {
	                throw new RuntimeException("Car not following correctly!");
	            }

            }
            if (front.getRemainingDist()>=behind.getRemainingDist()){
            	throw new RuntimeException("Cars not spaced correctly!");
            }
            front = behind;
        }
         
    	}
    	}
    }

    /**
     * This function returns the speed limit (meters/second) of a road based on its class 
     * as specified in the CFCC field of TIGER files.
     */
    public float getSpeedLimit()
    {
       // Primary Road with limited access/ Interstate Highway - unseparated
       if (roadClass >= 11 && roadClass <= 14)
          return 31.2928f;

       // Primary Road with limited access/ Interstate Highway - separated
       else if (roadClass >= 15 && roadClass <= 18)
          return 35.7632f;

       // Primary Road without limited access/ US Highway - unseparated
       else if (roadClass >= 21 && roadClass <= 24)
          return 20.1168f;

       // Primary Road without limited access / US Highway - separated
       else if (roadClass >= 25 && roadClass <= 28)
          return 22.352f;

       // Secondary and Connecting Roads / State Highways - unseparated
       else if (roadClass >= 31 && roadClass <= 34)
          return 20.1168f;

       // Secondary and Connecting Roads / State Highways - separated
       else if (roadClass >= 35 && roadClass <= 38)
          return 22.352f;
       
       // Local, Rural, Neighborhood / City Street - unseparated
       else if (roadClass >= 41 && roadClass <= 44)
          return 11.176f;

       // Local, Rural, Neighborhood / City Street - separated
       else if (roadClass >= 45 && roadClass <= 48)
          return 13.4112f;
       // access ramp
       else if (roadClass >=62 && roadClass <=63)
           return 13.4112f;
       else 
          System.err.println("Unknown road class " + (int)roadClass + " encountered\n");
       	  return 11.0f;
    }

    /**
     * Returns the distance along a road segment
     * @param point
     * @return
     */
    public float getDistance(Shape sh)
    {
    	int numPoints;
    	float d = 0;
        
        // handle straight line
        if (sh == null)
           return endPoint.distance(startPoint);
        
        // handle segment with shape points
        else
        {
            
           numPoints = sh.points.length;

           d = startPoint.distance(sh.points[0]);
           for (int i = 0; i < numPoints-1; i++)
           {
              d += sh.points[i].distance(sh.points[i+1]);   
           }
           d += sh.points[numPoints-1].distance(endPoint);
           return d;
        }
        
    }

    /**
     * Gets the pause time at an intersection, in seconds
     * @param next the next road taken
     * @return pause time in seconds
     * T-ODO come up with more realistic estimates
     */
//    public int getPauseTime(RoadSegment next, int numberOfRoads,
//            boolean stoplights, int pauseTime)
//    {
//    }
    
    /**
     * @return Returns the endAddressLeft.
     */
    public int getEndAddressLeft() {
        return endAddressLeft;
    }
    /**
     * @param endAddressLeft The endAddressLeft to set.
     */
    public void setEndAddressLeft(int endAddressLeft) {
        this.endAddressLeft = endAddressLeft;
    }
    /**
     * @return Returns the endAddressRight.
     */
    public int getEndAddressRight() {
        return endAddressRight;
    }
    /**
     * @param endAddressRight The endAddressRight to set.
     */
    public void setEndAddressRight(int endAddressRight) {
        this.endAddressRight = endAddressRight;
    }
    /**
     * @return Returns the endPoint.
     */
    public Location getEndPoint() {
        return endPoint;
    }
    /**
     * @param endPoint The endPoint to set.
     */
    public void setEndPoint(Location endPoint) {
        this.endPoint = endPoint;
    }
    /**
     * @return Returns the roadClass.
     */
    public char getRoadClass() {
        return roadClass;
    }
    /**
     * @param roadClass The roadClass to set.
     */
    public void setRoadClass(char roadClass) {
        this.roadClass = roadClass;
    }
    /**
     * @return Returns the shapeIndex.
     */
    public int getShapeIndex() {
        return shapeIndex;
    }
    /**
     * @param shapeIndex The shapeIndex to set.
     */
    public void setShapeIndex(int shapeIndex) {
        this.shapeIndex = shapeIndex;
    }
    /**
     * @return Returns the startAddressLeft.
     */
    public int getStartAddressLeft() {
        return startAddressLeft;
    }
    /**
     * @param startAddressLeft The startAddressLeft to set.
     */
    public void setStartAddressLeft(int startAddressLeft) {
        this.startAddressLeft = startAddressLeft;
    }
    /**
     * @return Returns the startAddressRight.
     */
    public int getStartAddressRight() {
        return startAddressRight;
    }
    /**
     * @param startAddressRight The startAddressRight to set.
     */
    public void setStartAddressRight(int startAddressRight) {
        this.startAddressRight = startAddressRight;
    }
    /**
     * @return Returns the startPoint.
     */
    public Location getStartPoint() {
        return startPoint;
    }
    /**
     * @param startPoint The startPoint to set.
     */
    public void setStartPoint(Location startPoint) {
        this.startPoint = startPoint;
    }
    /**
     * @return Returns the streetIndex.
     */
    public int getStreetIndex() {
        return streetIndex;
    }
    /**
     * @param streetIndex The streetIndex to set.
     */
    public void setStreetIndex(int streetIndex) {
        this.streetIndex = streetIndex;
    }
    /**
     * @return Returns the selfIndex.
     */
    public int getSelfIndex() {
        return selfIndex;
    }
    /**
     * @param selfIndex The selfIndex to set.
     */
    public void setSelfIndex(int selfIndex) {
        this.selfIndex = selfIndex;
    }

    /**
     * @return Returns the length of the road segment.
     */
    public float getLength() {
        return length;
    }
    /**
     * Sets segment length and maximum number of cars in segment
     * @param sh The shape describing the segment.
     */
    public void setLength(Shape sh) {
        this.length = getDistance(sh);
        this.maxCars = (int)Math.max((Math.floor(length/(CAR_LENGTH+SPACE))), 1);
    }
    /**
     * @return Returns the numberOfCars.
     */
    public int getNumberOfCars() {
        return numberOfCars;
    }
    /**
     * @param numberOfCars The numberOfCars to set.
     */
    public void setNumberOfCars(int numberOfCars) {
        this.numberOfCars = numberOfCars;
    }
    /**
     * @return Returns the maxCars for a direction.
     */
    public int getMaxCars() {
        return maxCars;
    }
    /**
     * @param maxCars The maxCars to set.
     */
    public void setMaxCars(int maxCars) {
        this.maxCars = maxCars;
    }
    /**
     * @return Returns the numberOfLanes.
     */
    public int getNumberOfLanes() {
        return numberOfLanes;
    }
    /**
     * @param numberOfLanes The numberOfLanes to set.
     */
    public void setNumberOfLanes(int numberOfLanes) {
        this.numberOfLanes = numberOfLanes;
    }
    /**
     * @return Returns the lane with the fewest number of cars
     * in this direction.
     */
    public LinkedList getCarsToEnd() {
        int index = -1;
        int minCars = 1000;
        
        for (int i = 0; i < numberOfLanes; i++)
        {
           if (carsToEnd[i].size() < minCars)
           {
               minCars = carsToEnd[i].size();
               index = i;
           }
        }
        return carsToEnd[index];
    }
    /**
     * @return Returns the lane with the fewest number of cars 
     * in this direction.
     */
    public LinkedList getCarsToStart() {
        int index = -1;
        int minCars = 1000;
        
        for (int i = 0; i < numberOfLanes; i++)
        {
		   if (carsToStart[i].size() < minCars)
		   {
		       minCars = carsToStart[i].size();
		       index = i;
		   }
        }
        return carsToStart[index];
    }
    
    /**
     * @return Returns the space between the front of a car and
     * the front of the car behind it (when stopped).
     */
    public float getCarSpacing(float speed, float beta, float gamma) {
        return (CAR_LENGTH+SPACE)+speed*beta+speed*speed*gamma;
    }

    /**
     * 
     * @param id node number to remove... must use 1-based number
     * @param rsEnd
     * @param mobInfo
     */
    public void removeNode(StreetMobilityInfo smi, LinkedList currentLane, Vector mobInfo) {
        
        if (currentLane.size() >0)
        {
            // make sure it's at the front of the queue
            if (((StreetMobilityInfo)currentLane.getFirst()) == smi)
            {
                currentLane.removeFirst();
            }
            else 
            {
            	// for lane-changing
            	if (((StreetMobilityInfo)currentLane.getFirst()).currentLane!=smi.currentLane 
            			&& smi.getRemainingDist()==0) {
            		currentLane.remove(smi);
            		return;
            	}
            	
            	System.err.println("Tried to remove: "+mobInfo.indexOf(smi));
            	
            	printCarList(currentLane, mobInfo);
            	
                throw new RuntimeException("Removed node that wasn't" 
                        + " at front of queue");
            }
            // T-ODO anything else?   
            return;
        }
        else // empty list
        {
        	System.err.println("Tried to remove: "+mobInfo.indexOf(smi)+" from segment "+selfIndex );
        	
        	printCarList(currentLane, mobInfo);
        	
            throw new RuntimeException("Tried to remove from " +
                    "empty list!");
        }       

    }
    
    public String printCarList(LinkedList currentLane, Vector mobInfo)
    {

    	Iterator it;
		
    	it = currentLane.iterator();
    	String s = "";
//    	Integer car;
    	StreetMobilityInfo smri;
    	while (it.hasNext())
    	{
    		
            smri = (StreetMobilityInfo)it.next();
//    		smri = (StreetMobilityInfo)mobInfo.get(car.intValue()-1);
    		
    		s += mobInfo.indexOf(smri) +" - remaining: "+smri.getRemainingDist()+
			" - next: "+smri.getNextCar() + "\n";

    		
    	}

        System.out.println(s);
    	return s;
    	
    }

    /**
     * @return
     */
    public String printStreetName(HashMap streets) {
        
        return "(" + startAddressLeft + "/" + startAddressRight +") - (" +
        "(" + endAddressLeft + "/" + endAddressRight +")" + 
        ((StreetName)streets.get(new Integer(streetIndex))).toString() + " ["+ selfIndex +"] " +
         startPoint + " - " + endPoint +"\tRoad class: "+((int)roadClass);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return ((RoadSegment)obj).selfIndex == this.selfIndex;
    }

    public int getLane(LinkedList currentLane) {
        
        LinkedList lanes[] = carsToEnd;
        
        for (int i = 0; i < lanes.length; i++)
        {
            if (currentLane.equals(lanes[i])) return i;
        }
        
        lanes = carsToStart;
        
        for (int i = 0; i < lanes.length; i++)
        {
            if (currentLane.equals(lanes[i])) return i;
        }
        return -1;
    }

    /**
     * Returns the number of directions for this road
     * @return
     */
    public int getNumberOfDirections() {
        // T-ODO update when support for one-way streets is included in 
        // map data.
        return 2;
    }
    
    /**
     * Returns the stroke width for painting a this road segment.
     */
    public char getStrokeWidth()
    {
       // Primary Road with limited access/ Interstate Highway - unseparated
       if (roadClass >= 11 && roadClass <= 14)
          return 7;

       // Primary Road with limited access/ Interstate Highway - separated
       else if (roadClass >= 15 && roadClass <= 18)
          return 7;

       // Primary Road without limited access/ US Highway - unseparated
       else if (roadClass >= 21 && roadClass <= 24)
          return 6;

       // Primary Road without limited access / US Highway - separated
       else if (roadClass >= 25 && roadClass <= 28)
          return 6;

       // Secondary and Connecting Roads / State Highways - unseparated
       else if (roadClass >= 31 && roadClass <= 34)
          return 5;

       // Secondary and Connecting Roads / State Highways - separated
       else if (roadClass >= 35 && roadClass <= 38)
          return 5;
       
       // Local, Rural, Neighborhood / City Street - unseparated
       else if (roadClass >= 41 && roadClass <= 44)
          return 3;

       // Local, Rural, Neighborhood / City Street - separated
       else if (roadClass >= 45 && roadClass <= 48)
          return 3;
       // access ramp
       else if (roadClass >=62 && roadClass <=63)
           return 3;
       else 
          System.err.println("Unknown road class " + (int)roadClass + " encountered\n");
          return 3;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
//    @Override
    public String toString() {
        return  "(" + startAddressLeft + "/" + startAddressRight +") - (" +
        "(" + endAddressLeft + "/" + endAddressRight +") " 
        + ((StreetName)streets.get(new Integer(streetIndex))).toString() 
        + " ["+ selfIndex +"] " +
         startPoint + " - " + endPoint +"\tRoad class: "+((int)roadClass);
    }

    /**
     * Returns the width of lanes for this segment.
     * @return
     */
	public float getLaneWidth() {
		// T-ODO make more accurate
		return 3.6576f; // 12 ft (average lane width in US)
	}

	public void addToLane(StreetMobilityInfo smi, int nextLane) {
		
		if (smi.current!=this) throw new RuntimeException("Adding to wrong street!");
		Location rsEnd = smi.rsEnd;
		LinkedList lane;
		
        if (rsEnd.distance(this.endPoint)==0)
        {
           lane = carsToEnd[nextLane];              
        }
        else {
        	lane = carsToStart[nextLane];
        }
        
        if (lane.contains(smi)) throw new RuntimeException("Adding redundantly!");
        
        ListIterator li = lane.listIterator();
        StreetMobilityInfo current;
        int i = 0;
        while (li.hasNext()){
        	current = (StreetMobilityInfo)li.next();
        	if (current.getRemainingDist()>smi.getRemainingDist()+CAR_LENGTH + SPACE ){
        		lane.add(i, smi);
        		if (current.currentLane==smi.currentLane) current.nextCar = smi;
//        		if (li.hasPrevious()) smi.nextCar = (StreetMobilityInfo)li.previous();
//        		else smi.nextCar = null;
        		return;
        	}
        	i++;
        }
        lane.addLast(smi);
//        checkLane(lane);
//        throw new RuntimeException("Could not find place to add car to lane!");
	}

	public LinkedList[] getLanes(StreetMobilityInfo smi) {
		Location rsEnd = smi.rsEnd;
        if (rsEnd.distance(this.endPoint)==0)
        {
           return carsToEnd;              
        }
        else {
        	return carsToStart;
        }
	}
}
