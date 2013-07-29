/*
 * C3 Project - Car to Car Communication
 * Created on Sep 16, 2005, by David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * Copyright 2005, David Choffnes. All rights reserved.
 * 
 */
package jist.swans.field.streets;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import jist.swans.field.StreetMobility;
import jist.swans.field.StreetMobilityInfo;

/**
 * @author David Choffnes
 *
 * The Mobil class implements the MOBIL lane change model.
 * http://vwisb7.vkw.tu-dresden.de/~treiber/MicroApplet/MOBIL.html
 */
public class Mobil extends LaneChangeModel {

	/** Average lane change time according to:
	 *  http://scholar.lib.vt.edu/theses/available/etd-382915749731261/unrestricted/Etd.pdf 
	 */
	final static float LANE_CHANGE_TIME = 6.5f; 
	/** amount by which to multiple driver's "extra speed" value */
	static float speedFactor = 1.0f;
	final static float LANE_CHANGE_STD_DEV = 1.5f;
	/** minimum time to perform a lane change. */
	private static final float MIN_CHANGE_TIME = 3.14f;
	/** politeness factor */
	private static final float POLITENESS = 0.4f;
	/** acceleration threshold (lowest acc ability) */
	private static final float ACC_THRESHOLD = 0.2f;
	/** Maximum safe deceleration, in m/s^2 */
	final static float B_SAVE = 4.0f;
	private static final boolean DEBUG = false;
	private static final float MAX_CHANGE_TIME = 13.5f;
	
	/** state for lane change */
	boolean safeLeft = false;
	boolean safeRight = false;

	public Mobil(StreetMobility sm, double stdDev){
		laneChangers = new HashMap();
		this.sm = sm;
		speedFactor = (float)(LANE_CHANGE_STD_DEV * stdDev);
	}



	public void changeLanes(StreetMobilityInfo smi, Integer id) {
				
		if (DEBUG){
			System.out.println("Node " + id + ": Beginning to change lanes!");
		}
        
		int nextLane = -1;		
		LaneChangeInfo lci = new LaneChangeInfo();
		LinkedList nextLaneList;
		
		// set state info
		lci.changeTime = smi.extraSpeed*speedFactor + LANE_CHANGE_TIME;
		if (lci.changeTime < MIN_CHANGE_TIME) lci.changeTime = MIN_CHANGE_TIME;
		if (lci.changeTime > MAX_CHANGE_TIME) lci.changeTime = MAX_CHANGE_TIME;
		lci.remainingTime = lci.changeTime;	
		
		// pick lane (prefer left lane)
		if (safeLeft){
			lci.toLeft = true;
			nextLane = smi.current.getLane(smi.currentLane) - 1;			
		}
		else{
			lci.toLeft = false;
			nextLane = smi.current.getLane(smi.currentLane) + 1;
		}
		// store state
		laneChangers.put(id, lci);

		// add car to new lane, but don't remove from old while changing
		smi.current.addToLane(smi, nextLane);
		nextLaneList = smi.current.getLanes(smi)[nextLane];
		lci.nextLane = nextLane;
		lci.nextCar = getNextCar(nextLaneList, smi);


		updateLaneChangeInfo(smi, lci);
	}

	
	/**
	 * According to the following criterium: 
	 *  acc' (M') - acc (M) > p [  acc (B) + acc (B') - acc' (B) - acc' (B') ] + athr
	 * @param smi
	 * @return
	 */
	public boolean incentiveToChange(StreetMobilityInfo smi) {
		
		float accMPrime, accM, accB=0, accBPrime, accPrimeB=0, accPrimeBPrime;
		
		// no need to change lanes
		if (smi.nextCar==null || smi.nextCar.currSpeed > smi.currSpeed){
			return false;
		}
		
		int currentLane = smi.current.getLane(smi.currentLane);
		
		// get available lanes
		LinkedList lanes[] = smi.current.getLanes(smi);
		int laneNumber = -1;
		
		for (int i = 0; i < 2; i++){
			if (i == 0 && safeLeft) laneNumber = currentLane - 1;
			else if (i==1 && safeRight) laneNumber = currentLane + 1;
			else continue;

			
			ListIterator li = lanes[laneNumber].listIterator();
			StreetMobilityInfo back = null, front = null;
			while (li.hasNext()){
				back = (StreetMobilityInfo)li.next();
				// is there room behind?
				if (back.getRemainingDist() > smi.getRemainingDist()){

					// set next car info
					if (li.hasPrevious()) front = back.nextCar;
					break;
				}
			}

			// use acceleration over next second
			accM = getAcceleration(smi, smi.nextCar);
			if (accM < 0) accM = 0;
			accMPrime = getAcceleration(smi, front);
			if (accMPrime < 0 ) accM = 0;
			// before going further, see if there's any advantage at all
			if (accMPrime - accM > ACC_THRESHOLD){
				if (back == null) return true; // no worries if there is no one back there
				
				int index = smi.currentLane.indexOf(smi)+1;
				StreetMobilityInfo currentBack = null;
				if (index < smi.currentLane.size()) currentBack = (StreetMobilityInfo)smi.currentLane.get(index);
				if (currentBack!=null){
					accB = getAcceleration(currentBack, smi);
					accPrimeB = getAcceleration(currentBack, smi.nextCar);
				}
				
				accBPrime = getAcceleration(back, front);
				accPrimeBPrime = getAcceleration(back, smi);
				
				// finally, we can tell if there is enough incentive
				float incentive = POLITENESS*(accB+accBPrime-accPrimeB-accPrimeBPrime) + ACC_THRESHOLD;
				if (accMPrime - accM > incentive ) return true;
			}
			else if (i==0) safeLeft = false; // indicate no incentive to go into left lane
		}
		
		return false;
	}

	private float getAcceleration(StreetMobilityInfo smi, StreetMobilityInfo nextCar) {	
		float accel;
		if (nextCar==null) return smi.acceleration;
		accel = (float)(2*(smi.getRemainingDist() - nextCar.getRemainingDist() 
				- smi.getCarSpacing())/smi.stepTime-2*(smi.currSpeed-nextCar.currSpeed));
		if (accel > smi.acceleration) return smi.acceleration;
		else return accel;
	}
    
	/**
	 * Determines if it is safe to change lanes. According to the following criterium:
	 *  acc' (B') > - bsave
	 * @param smi
	 * @return
	 */
	public boolean safeToChange(StreetMobilityInfo smi) {
		// nothing to do if only one lane
		if (smi.current.numberOfLanes==1) return false;
		
		// set state
		safeLeft = false;
		safeRight = false;
		LinkedList lanes[];
		int currentLane = smi.current.getLane(smi.currentLane);
		
		// get available lanes
		lanes = smi.current.getLanes(smi);
		// check left lane
		if (currentLane-1>=0){
			safeLeft = canChangeToLane(lanes[currentLane-1], smi);
			
		}
		// check right lane
		if (currentLane+1<lanes.length){
			safeRight = canChangeToLane(lanes[currentLane+1], smi);
		}
		return (safeLeft || safeRight);
	}
	
	private boolean canChangeToLane(LinkedList list, StreetMobilityInfo smi) {
		ListIterator li = list.listIterator();
		StreetMobilityInfo back = null;
		while (li.hasNext()){
			back = (StreetMobilityInfo)li.next();
			// is it safe to the side?
			if (back.getRemainingDist() +smi.getCarSpacing() > smi.getRemainingDist()) return false;
			// is there a car behind?
			if (back.getRemainingDist() > smi.getRemainingDist()){
				// make sure there is room in front, too.
				if (back.nextCar!=null && back.getRemainingDist() - 
						back.nextCar.getRemainingDist() < smi.getCarSpacing() // is there room between cars?
						|| back.getRemainingDist() - smi.getCarSpacing() <= smi.getRemainingDist()){
					return false;
				}
				// case where node is near end of segment
				if (back.nextCar == null && smi.getRemainingDist() < smi.getCarSpacing()){
					// get back car from next road
					int index = smi.current.getLane(list);
					LinkedList lanes[];
					if (smi.nextEnd.distance(smi.nextRS.getEndPoint())==0) lanes = smi.nextRS.carsToEnd;
					else lanes = smi.nextRS.carsToEnd;
					if (lanes.length>index){ // there is another lane like this one
						if (lanes[index].size()>0){
							StreetMobilityInfo last = (StreetMobilityInfo)lanes[index].getLast();
							if (last.current.length - last.getRemainingDist() + smi.getRemainingDist() < smi.getCarSpacing()) 
								return false; // too little space
						}
					}
					else{ // lane will end, don't try to change
						return false;
					}
				}
				break;
			}
		}
		if (back == null) return true; // no cars in lane
		// all cars in lane are in front of the current car
		else if (smi.getRemainingDist() > back.getRemainingDist()){
			// is it too close?
			if (smi.getRemainingDist() - back.getRemainingDist() < smi.getCarSpacing()){
				return false;
			}			

		}		
		else{ // is it safe behind?
			float backSpeed = back.currSpeed;
			if (backSpeed > smi.currSpeed){ // will back car have to decelerate?
				double dist = back.getRemainingDist() - smi.getRemainingDist();
				double time = dist / (0.5*(backSpeed - smi.currSpeed));
				if ((backSpeed - smi.currSpeed)/(2*time)<B_SAVE) return true;
			}
			else return true;
		}
		return false;
	}
	
	public StreetMobilityInfo getClosestCarInfo(StreetMobilityInfo smi, Integer id) {
		StreetMobilityInfo returnValue = null;
		LaneChangeInfo lci = (LaneChangeInfo)laneChangers.get(id);
		if (lci == null) return null; // this is wrong "currentlane"
		lci.nextCar = getNextCar(smi.current.getLanes(smi)[lci.nextLane], smi);
		if (lci.nextCar == null) return null;
//		float spacing = (float)smi.getCarSpacing()-1; // floating point imprecision here...
		if (lci.nextCar.getRemainingDist() +RoadSegment.CAR_LENGTH+RoadSegment.SPACE > smi.getRemainingDist()) 
			throw new RuntimeException("Wrong next car!");

		if (smi.nextCar==null || smi.nextCar.getRemainingDist() 
				< lci.nextCar.getRemainingDist()) 
			returnValue = lci.nextCar;
		else returnValue = smi.nextCar;
		
		if (smi.getRemainingDist() <= returnValue.getRemainingDist()) throw new RuntimeException("Wrong car picked!");
		return returnValue;
	}
	
	public void checkNextLane(StreetMobilityInfo smi, Integer id) {
		smi.current.checkLane(null);
	}



}
