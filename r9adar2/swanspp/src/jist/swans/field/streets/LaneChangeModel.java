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
 * The LaneChangeModel interface allows for pluggable lane change models.
 */
public abstract class LaneChangeModel {

    public class LaneChangeInfo{
    	public int nextLane;
    	public float changeTime;
        public float remainingTime;
        public boolean toLeft; // true is into left lane, false is into right
    	public StreetMobilityInfo nextCar;
    }


    HashMap laneChangers;
    StreetMobility sm;
    private boolean DEBUG_PRINT = false;
    private final static boolean DEBUG = false;
    
	/**
	 * Requests a lane change and returns true if one will be performed. 
	 * 
	 * @param smi
	 * @param id
	 * @return
	 */
    public boolean performLaneChange(StreetMobilityInfo smi, Integer id) {
        // see if already changing
        LaneChangeInfo lci = (LaneChangeInfo)laneChangers.get(id);
        if (lci!=null){
            if(lci.remainingTime>0){
                updateLaneChangeInfo(smi, lci);
                return true;
            }
            else{
                // T-ODO handle node changing road segment while changing lane
                laneChangers.remove(id); // remove from list of lane changers
                int index = smi.currentLane.indexOf(smi);
                smi.currentLane.remove(index); // remove from old lane
                if (index<smi.currentLane.size()) {// update next car for back car
                    StreetMobilityInfo nextCar = (StreetMobilityInfo)smi.currentLane.get(index);
                    if (nextCar.currentLane==smi.currentLane){
                        if (index == 0) nextCar.nextCar = null;
                        else nextCar.nextCar = (StreetMobilityInfo)smi.currentLane.get(index-1);                                
                    }
                }                   
                
                if (smi.currentLane==smi.current.getLanes(smi)[lci.nextLane]) throw new RuntimeException("Changed lanes incorrectly!");
                
                smi.currentLane = smi.current.getLanes(smi)[lci.nextLane]; // set next lane
                // set next car
                index = smi.currentLane.indexOf(smi);
                if (smi.currentLane.size()==1 || smi.currentLane.getFirst()==smi) smi.nextCar = null;
                else smi.nextCar = (StreetMobilityInfo)smi.currentLane.get(index-1);
                if (smi.currentLane.size()>index+1){ // I shouldn't have to do this here...
                    StreetMobilityInfo backCar = ((StreetMobilityInfo)smi.currentLane.get(index+1));
                    if (backCar.currentLane == smi.currentLane) backCar.nextCar = smi;
                }               
                
                if (DEBUG_PRINT) System.out.println("Node " + id + ": Done changing lanes!");
                return true;
            }
        }
        // otherwise see if lane change should occur
        if (safeToChange(smi)){
            if (incentiveToChange(smi)){
                changeLanes(smi, id);
                return true;
            }
        }
        return false;
    }

    public abstract boolean safeToChange(StreetMobilityInfo smi);
    
    public abstract boolean incentiveToChange(StreetMobilityInfo smi);
    
    public abstract void changeLanes(StreetMobilityInfo smi, Integer id);
    
	/**
	 * Returns which part of the lane the car is currently in during a lane change.
	 * @param id
	 * @return a number between [0, 1.0)
	 */
    public float getLaneNumber(Integer id) {
        LaneChangeInfo lci = (LaneChangeInfo)laneChangers.get(id);
        if (lci==null) return 0;
        if (lci.toLeft){
            return -1 * (1- lci.remainingTime/lci.changeTime);
        }
        else return (1- lci.remainingTime/lci.changeTime);      
    }

	/**
	 * Performs cleanup operations when changing lanes and crossing road segments.
	 * Call <i>before<i> updating the current road segment and other features.
	 * 
	 * @param id
	 * @param smi
	 * @param nextLaneTemp the next lane
	 */
    public void moveToNextRoad(Integer id, StreetMobilityInfo smi, LinkedList nextLaneTemp) {

        LinkedList newNextLane = null;

        // get LCI object
        LaneChangeInfo lci = (LaneChangeInfo)laneChangers.get(id);
        if (lci!=null){
            
            // remove from old "next lane"
            LinkedList oldNextLane = smi.current.getLanes(smi)[lci.nextLane];
            
            // if the car turned or if the lanes narrowed, cancel the lane change. 
            if (smi.current.streetIndex != smi.nextRS.streetIndex 
                    || smi.nextRS.numberOfLanes<2 
                    || Math.abs(smi.nextRS.getLane(nextLaneTemp)-lci.nextLane)!=1){
                smi.current.removeNode(smi, oldNextLane, sm.getMobInfo());
                StreetMobilityInfo carToUpdate = null;
                if (oldNextLane.size()>0) carToUpdate = (StreetMobilityInfo)oldNextLane.getFirst();
                if (carToUpdate!=null && carToUpdate.currentLane==oldNextLane) carToUpdate.nextCar = null;
                laneChangers.remove(id);    
                if (DEBUG_PRINT ) System.out.println("Node "+id+": Aborting lane change (turn, narrow, force change)!");
                return;
            }
            

            smi.current.removeNode(smi, oldNextLane, sm.getMobInfo());
            StreetMobilityInfo nextCar;
            if (oldNextLane.size()>0){
                nextCar = ((StreetMobilityInfo)oldNextLane.getFirst());
                if (nextCar.currentLane == oldNextLane) nextCar.nextCar = null;
            }
            
            // add to new "next lane"
            if (smi.nextRS.numberOfLanes > lci.nextLane){ // does the lane still exist?
                newNextLane = (smi.nextEnd.distance(smi.nextRS.endPoint)==0) ? (smi.nextRS.carsToEnd[lci.nextLane]) :
                    (smi.nextRS.carsToStart[lci.nextLane]);
                if (newNextLane.size() > 0){ // is there a car already there?
                    StreetMobilityInfo last = (StreetMobilityInfo)newNextLane.getLast();
                    // is there room?
                    if (last.getRemainingDist() < smi.nextRS.length-RoadSegment.CAR_LENGTH-RoadSegment.SPACE){
                        if (newNextLane.contains(smi)) throw new RuntimeException("Adding redundnatly!");
                        newNextLane.addLast(smi);
                    }
                    else{ // no room in new lane after all
                        laneChangers.remove(id);
                        System.out.println("Node "+id+": Aborting lane change (no room in new lane)!");
                        return;
                    }
                }
                else{ // no cars in next new lane
                    if (newNextLane.contains(smi)) throw new RuntimeException("Adding redundnatly!");
                    newNextLane.addLast(smi);
                }
            }
            else{ // no more lane to change to
                laneChangers.remove(id);
                System.out.println("Node "+id+": Aborting lane change (why am I here?)!");
                return;
            }
            
            // update "nextCar"
            lci.nextCar = getNextCar(newNextLane, smi);
        }
    }

	/**
	 * Returns the car to follow while changing lanes.
	 * @param smi
	 * @param id
	 * @return
	 */
    public abstract StreetMobilityInfo getClosestCarInfo(StreetMobilityInfo smi, Integer id);

    public abstract void checkNextLane(StreetMobilityInfo smi, Integer id);
	
    
    public boolean isChangingLanes(StreetMobilityInfo behind) {
        return laneChangers.containsValue(behind);
    }
    
    public void updateLaneChangeInfo(StreetMobilityInfo smi, LaneChangeInfo lci) {
        lci.remainingTime -= smi.stepTime;                  
    }
    
    public StreetMobilityInfo getNextCar(LinkedList nextLane, StreetMobilityInfo smi) {        
        if (nextLane.size()==1 || nextLane.getFirst()==smi) return null;
        if (nextLane.contains(smi)) return (StreetMobilityInfo)nextLane.get(nextLane.indexOf(smi)-1); 
        
        ListIterator li = nextLane.listIterator();
        StreetMobilityInfo next = null;
        while (li.hasNext()){
            next = (StreetMobilityInfo)li.next();
            if (next.getRemainingDist() >= smi.getRemainingDist()){
                if (li.hasPrevious()) next = (StreetMobilityInfo)li.previous();
                break;
            }
        }

        return next;
    }
}
