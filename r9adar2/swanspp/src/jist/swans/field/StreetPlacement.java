/*
 * C3 Project - Car to Car Communication
 * Created on Sep 8, 2005, by David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * Copyright 2005, David Choffnes. All rights reserved.
 * 
 */
package jist.swans.field;

import java.util.Iterator;

import jist.swans.Constants;
import jist.swans.field.StreetMobilityOD.StreetMobilityInfoOD;
import jist.swans.field.StreetMobilityRandom.StreetMobilityInfoRandom;
import jist.swans.field.streets.RoadSegment;
import jist.swans.field.streets.Shape;
import jist.swans.misc.Location;

/**
 * @author David Choffnes
 *
 * The StreetPlacement class contains code common to placing vehicles on streets.
 */
public abstract class StreetPlacement implements Placement {

	
    private static final boolean VERBOSE = false;
	/** placement boundaries. */
	protected float xBL, yBL, xTR, yTR;
    /** street mobility object, stores roads */
    protected static StreetMobility sm; 
    /** set true for debugging information */
    final protected boolean DEBUG=false;
    /** incremented each time this is called to determine node number */
    protected int numberOfNodes=0;
    /** the random object for repeatability */
    protected java.util.Random rnd;
    /** the maximum distance in meters between first point and destination */
    protected float threshold = 1500.0f;
    /** the std dev for the distribution of speeds about the speed limit. */
    protected double stdDev = 0.0;
    /** step time for street mobility */
    protected double stepTime = 1;
    /** direction constants */
    public final int TO_START = 0;
    public final int TO_END = 1;
    protected RoadSegment rs;
    
	/**
	 * StreetPlacement constructor.
	 */
    public StreetPlacement(Location bl, Location tr, StreetMobility smr,
            double stdDev, double stepTime)
    {
	    init(bl.getX(), bl.getY(), tr.getX(), tr.getY(), smr);
	    this.stdDev = stdDev;
	    this.stepTime = stepTime;
    }

	/* (non-Javadoc)
	 * @see jist.swans.field.Placement#getNextLocation()
	 */
	public Location getNextLocation() {
        Location initialLocation;
        rs = null;
        int direction = 0, position = 0;
        float remainingDist = 0;

        
        // determine mobility model
        StreetMobilityInfo smri;
        if (sm instanceof StreetMobilityRandom)
        {
            smri = new StreetMobilityInfoRandom();
        }
        else if (sm instanceof StreetMobilityOD)
        {
            smri = new StreetMobilityInfoOD();
            ((StreetMobilityInfoOD)smri).config = Constants.MOBILITY_STREET_RANDOM;
        }
        else if (sm instanceof StreetMobilityCircuit)
        {
        	// T-ODO finish
        	smri = new StreetMobilityInfoOD();
        	
        }
        else{
        	throw new RuntimeException("Unsupported mobility model!");
        }
       
        
        direction = setInitialSegment(smri);
        if (direction < 0) return null;
        
        smri.nextRS = rs;
        
        // add car to road
        smri.currentLane = rs.addNode(smri, smri.rsEnd, sm.mobInfo);
        smri.current = rs;

        // current lane may be full
        if( smri.currentLane == null)
        {
            if (DEBUG){
                if (direction == TO_START)
                {
                    System.out.println("Cars in lane: " + rs.getCarsToStart().size());
                    System.out.println("Max cars in lane: " + rs.getMaxCars());
                }
                else
                {
                    System.out.println("Cars in lane: " + rs.getCarsToEnd().size());
                    System.out.println("Max cars in lane: " + rs.getMaxCars());
                }
            }
            // T-ODO deal with this intelligently
            return null;
//            throw new RuntimeException("Not enough room for car!");
        }
        
        numberOfNodes++; // increment number of nodes           

        smri.currSpeed=0;
        smri.setMaxSpeed(rs.getSpeedLimit());
        smri.stepTime=(float)stepTime;
        // set the amount by which drivers' speeds will vary from the speed limit
        smri.extraSpeed = (float)(stdDev*rnd.nextGaussian());
        
        // calculate position, remaining distance and next car
        if (direction==TO_START)
        {            
            if (rs.getShapeIndex()>=0)
            {
                smri.ShapePointIndex = 
                    ((Shape)sm.shapes.get(new Integer(rs.getShapeIndex()))).points.length;
            }            
            // for calculating remaining distance
            position = smri.currentLane.indexOf(smri);           
            if (smri.currentLane.getLast()!=smri) {
                ((StreetMobilityInfo)smri.currentLane.get(position+1)).nextCar = smri;
            }
            
            if (position>0){ 
                smri.nextCar = (StreetMobilityInfo)smri.currentLane.get(position-1);

                remainingDist = smri.nextCar.remainingDist + 
                rs.getCarSpacing(smri.currSpeed, smri.spacingBeta, 
                        smri.spacingGamma);
            }
        }
        else
        {
            smri.ShapePointIndex = -1;            
            // for calculating remaining distance
            position = smri.currentLane.indexOf(smri);
            if (smri.currentLane.getLast()!=smri) {
                ((StreetMobilityInfo)smri.currentLane.get(position+1)).nextCar = smri;
            }
            
            if (position>0) 
            {
                smri.nextCar = (StreetMobilityInfo)smri.currentLane.get(position-1);

                remainingDist = smri.nextCar.remainingDist + 
                rs.getCarSpacing(smri.currSpeed, smri.spacingBeta, 
                        smri.spacingGamma);
            }
        }
        smri.remainingDist = remainingDist;
        if (DEBUG) System.out.println("Remaining distance: "+remainingDist);
        if (DEBUG) System.out.println("Setting next road after placement...");
        
        sm.setNextRoad(smri);
        
        // add car to mobility info object
        sm.mobInfo.add(smri); // T-ODO this is NOT thread safe       
        
        if (DEBUG)
        {
            System.out.println("carsToEnd contains:");            
            Iterator it = smri.current.getCarsToEnd().iterator();
            
            Integer car;
            StreetMobilityInfo smriTemp;
            while (it.hasNext())
            {                
                car = (Integer)it.next();                
                smriTemp = (StreetMobilityInfo)sm.mobInfo.get(car.intValue()-1);
                System.out.println(car +" - remaining: "+smriTemp.getRemainingDist()+
                        " - next: "+smriTemp.getNextCar());               
            }
            
            System.out.println("carsToStart contains:");           
            it = smri.current.getCarsToStart().iterator();
            while (it.hasNext())
            {                
                car = (Integer)it.next();               
                smriTemp = (StreetMobilityInfo)sm.mobInfo.get(car.intValue()-1);
                System.out.println(car +" - remaining: "+smriTemp.getRemainingDist()+
                        " - next: "+smriTemp.getNextCar());                
            }
            
            System.out.println("Next car is: "+smri.getNextCar());           
            System.out.println("mobInfo size: " + sm.mobInfo.size());
        } // end debug block
        
        // now we have to move the vehicle back from the end of the road if 
        // there is one or more cars ahead
        if (direction==TO_START)
        {
            // we have no intermediate segments
            if (smri.current.getShapeIndex()==-1)
            {
                initialLocation = sm.move(rs.getEndPoint(), smri.rsEnd, rs.getLength()-remainingDist);
                
                if (DEBUG)
                    if (!initialLocation.inside(new Location.Location2D(xBL, yBL), 
                            new Location.Location2D(xTR, yTR)))
                        throw new RuntimeException("Movement bug!");
            }
            else
            {
                initialLocation = sm.pointAt(rs.getEndPoint(), smri, 
                        rs.getLength()-remainingDist);
                
                if (DEBUG)
                    if (!initialLocation.inside(new Location.Location2D(xBL, yBL), 
                            new Location.Location2D(xTR, yTR)))
                        throw new RuntimeException("Movement bug!");
            }
        }
        else
        {
            // we have no intermediate segments
            if (smri.current.getShapeIndex()==-1)
            {
                initialLocation = sm.move(rs.getStartPoint(), smri.rsEnd, rs.getLength()-remainingDist);
                
                if (DEBUG)
                    if (!initialLocation.inside(new Location.Location2D(xBL, yBL), 
                            new Location.Location2D(xTR, yTR)))
                        throw new RuntimeException("Movement bug!");
            }
            else
            {
                initialLocation = sm.pointAt(rs.getStartPoint(), smri, 
                        rs.getLength()-remainingDist); 
                
                if (DEBUG)
                    if (!initialLocation.inside(new Location.Location2D(xBL, yBL), 
                            new Location.Location2D(xTR, yTR)))
                        throw new RuntimeException("Movement bug!");
            }
        }
        
        if (VERBOSE) System.out.print("\rPlaced node "+numberOfNodes);
        
        if (StreetMobility.ENABLE_LANE_DISPLACEMENT){
            smri.offset = sm.getLaneDisplacement(smri, null);
            initialLocation = initialLocation.getClone();
            initialLocation.add(smri.offset); // displace due to lane
        }
        
        return initialLocation;

	}

	/**
	 * Sets the first road that the car will travel on.
	 * @param rs the road segment to set
	 * @param smri the street mobility object to play with
	 * @return the direction to move on the selected street
	 */
    abstract int setInitialSegment(StreetMobilityInfo smri) ;

	/**
     * Common initialization code.
     * 
     * @param xBL bottom-left x position (in meters)
     * @param yBL bottom-left y position (in meters)
     * @param xTR top-right x position (in meters)
     * @param yTR top-right x position (in meters)
     * @param smr the StreetMobility object
     */
    private void init(float xBL, float yBL, float xTR, float yTR, 
            StreetMobility smr)
    {
        this.xBL = xBL;
        this.yBL = yBL;
        this.xTR = xTR;
        this.yTR = yTR;
        this.sm = smr;
        this.rnd = smr.rnd;
    }
}
