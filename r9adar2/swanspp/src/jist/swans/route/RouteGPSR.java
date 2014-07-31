/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         RouteGPSR_Old.java
 * RCS:          $Id: RouteGPSR.java,v 1.1 2007/04/09 18:49:29 drchoffnes Exp $
 * Description:  RouteGPSR_Old class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Mar 9, 2005
 * Language:     Java
 * Package:      jist.swans.route
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
package jist.swans.route; 

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.MessageQueue;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetMessage;
import jist.swans.net.NetMessage.Ip;
import jist.swans.net.QueuedMessage;
import jist.swans.route.geo.AddressGeographic;
import jist.swans.route.geo.LocationDatabase;
import driver.JistExperiment;
import driver.Visualizer;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The RouteGPSR class implements the code almost line for line exactly 
 * as is done in the NS-2 code. This is a test not only  
 * of GPSR performance using more realistic settings in SWANS 
 * but also of the feasibility of porting NS-2 code to SWANS.
 */
public class RouteGPSR extends RouteGeo {
    
    ///////////////////////////////////////////
    // GPSR constants
    //
    
    private static final int DNT_TIMEOUT = 5;
    private static final int PPT_TIMEOUT = 5;
    private static final short GPSR_PORT = 0xff; // port number for GPSR protocol messages
    
    private static final double GPSR_ALIVE_DESYNC = 0.5;	// desynchronizing term for alive beacons
    private static final double GPSR_ALIVE_INT = 1;	// interval between alive beacons
    private static final double GPSR_ALIVE_EXP = // timeout for expiring rx'd beacons
        (3*(GPSR_ALIVE_INT+GPSR_ALIVE_DESYNC*GPSR_ALIVE_INT));
    
    
    private static final double GPSR_PPROBE_INT = 1.5;	// interval between perimeter probes
    private static final double GPSR_PPROBE_DESYNC = 0.5;	// desynchronizing term for perimeter probes
    public static final double GPSR_PPROBE_EXP = 8.0;	// how often must use a perimeter to keep
    // probing them
    
    private static final boolean GPSR_PPROBE_RTX = true;
    
    private static final int PLANARIZE_RNG = 0;
    private static final int PLANARIZE_GABRIEL = 1;
    
    /** if true, will attempt to retarget packets for lost link */
    private static final boolean CONFIG_RECOVER = true;
    /** if true, will use MAC layer unicast to send data packets */
    private static final boolean CONFIG_UNICAST = true;
    /** if true, uses speed information to better determine node locations */
    private static final boolean CONFIG_USE_SPEED = true;
    /** if true, will send data to the visualizer */
    private static final boolean CONFIG_USE_VISUALIZATION = true;
    /** if true, will check neighbor table for destination */
    private static final boolean CONFIG_PREVENT_LOOP = true;
    /** if true, will try to forward to node that will be closest 
     * to the destination in the next second
     * */
    private static final boolean CONFIG_USE_FUTURE_LOCATION = true;
    /** number of seconds in the future to determine node location */
    public float futureStep = 1;
    /////////////////////////////////////////////
    // fields for use in SWANS
    //
    /** Self-referencing proxy entity. */
    private RouteInterface.Geo self;
    /** Network entity. */
    private NetInterface netEntity;
    /** local network address. */
    private NetAddress netAddr;
    /** local mac address */
    private MacAddress macAddr;
    /** stats */
    public static jist.swans.route.RouteGPSR.GPSRStats stats = 
        new jist.swans.route.RouteGPSR.GPSRStats();
    
    public RouteGPSR selfNotEntity = this;
    
    public HashMap pendingTimers;
    
    private static Visualizer v = Visualizer.getActiveInstance();
    
    //////////////////////////////////////////////
    // Fields from NS-2
    //
    
    NeighborTable ntab_;
    GPSR_BeaconTimer beacon_timer_; // Alive beacon timer
    GPSR_LastPeriTimer lastperi_timer_; // Last perimeter used timer
    GPSR_PlanarTimer planar_timer_; // inter-planarization timer
    //Trace tracetarget;		// Trace Target
    boolean use_mac_;			// whether or not to simulate full MAC level
    boolean use_peri_;		// whether or not to use perimeters
    boolean verbose_;			// verbosity (binary)
    boolean drop_debug_;		// whether or not to be verbose on NRTE events
    boolean peri_proact_;		// whether or not to pro-actively send pprobes
    boolean use_implicit_beacon_;	// whether or not all data packets are beacons
    boolean use_planar_;		// whether or not to planarize graph
    boolean use_loop_detect_;		// whether or not to fix loops in peridata pkts
    boolean use_timed_plnrz_;		// whether or not to replanarize w/timer
    double bint_;			// beacon interval
    double bdesync_;		// beacon desync random component range
    double bexp_;			// beacon expiration interval
    double pint_;			// perimeter probe interval
    double pdesync_;		// perimeter probe desync random cpt. range
    double lpexp_;		// perimeter probe generation timeout
    LocationDatabase ldb_;		// location database
    //MobileNode mn_;		// my MobileNode ***already taken care of
    MessageQueue ifq_;		// my ifq
    int off_gpsr_;		// offset of the GPSR packet header in pkt
    private static final long TRANSMISSION_JITTER = 1 * Constants.MILLI_SECOND;
    
    /** message dropping constants */
    private static final int DROP_RTR_MAC_CALLBACK = 0;
    private static final int DROP_RTR_NEXT_SRCRT_HOP = 1;
    private static final int DROP_RTR_NO_ROUTE = 2;
    private static final int DROP_RTR_ROUTE_LOOP = 3;
    private static final int DROP_RTR_TTL = 4;
    
    /** callback ids */
    private static final int CALLBACK_BEACON = 0;
    private static final int CALLBACK_DN = 1;
    private static final int CALLBACK_LAST_PERI = 2;
    private static final int CALLBACK_PERI_PROBE = 3;
    private static final int CALLBACK_PLANAR = 4;

    
    static float minDist = Float.MAX_VALUE;
    private JistExperiment je;
    /** true if neighbor table text is shown */
    private boolean showNeighborText =true;
	private static boolean keySet = false;


    
    public static class GPSRStats
    {
        public int numBeaconsSent;
        public int numPerimeterProbesSent;
        public int numBeaconsRecv;
        public int numPerimeterProbesRecv;
        public int numMessagesForwarded;
        public int numDropped;
        public int numMessagesSent;
        public int arrivedAtDest;
        public int dropMac;
        public int dropNextSRCRT;
        public int dropNoRoute;
        public int dropLoop;
        public int dropTTL;
        public int numDeadNeighbors;
        public int numDataDropped;
        
        public void clear()
        {
            numBeaconsSent =0;
            numPerimeterProbesSent=0;
            numBeaconsRecv=0;
            numPerimeterProbesRecv=0;
            numMessagesForwarded = 0;
            numDropped = 0;
            numMessagesSent=0;
            arrivedAtDest = 0;
            
        }
    }
    
    
    //  underlying data structure: an array, ordered by dst addr
    public class NeighborTable extends Vector {
        
        // used to deal with problem of nodes with same bearing    	
        Vector sameBrg = new Vector();
        /** used to index into vector of nodes with same bearing */
        int index = 0;
        /** total number of nodes with same bearing */
        int totalCount = 0;
        /** minimum bearing for this neighbor table */
        double minbrg_ = Double.MAX_VALUE;
        /** the percent of the transmission radius over which messages are 
         * reliably transmitted */
		private double reliablePercent = 0.5;
        
        public NeighborTable(RouteGPSR mya)
        {
            super();
        }
        
        public void ent_delete(NeighborEntry ent)
        {
            int index = indexOf(ent);
            if (index > -1)
            {
                NeighborEntry removed = (NeighborEntry)get(index);
                remove(index);
                
                
                removed.dnt.force_cancel();
            }
            
        }

        /** adds an element into the array in sorted order */
        public NeighborEntry ent_add(NeighborEntry ne) {
            
            if (ne.address.equals(netAddr))
            {
                return null;                
            }
            
            NeighborEntry existing;
            
            // see if it's already in table
            int index = indexOf(ne);
            if (index >=0) // already in table
            {
                existing = (NeighborEntry)get(index);
                existing.loc = ne.loc;
                existing.lastSeen = JistAPI.getTime();
                existing.dnt.force_cancel();
                
                /* X-XX overwriting table entry shouldn't affect when to probe this
                 perimeter */
                // (*pne)->ppt.force_cancel();              
                return existing;
            }
            
            // invalidate the perimeter that may be cached by this neighbor entry
            ne.perilen = 0;
            // X-XX gross way to indicate entry is *new* entry, graph needs planarizing
            ne.live = -1;
            
            // do insertion to maintain order
            for (int i=0; i<size(); i++) {
                if (ne.address.compareTo(((NeighborEntry)get(i)).address) <= 0) {
                    add(i, ne);
                    return ne;
                }
            }
            add(ne);
            return ne;
        }
        
        public NeighborEntry ent_finddst(NetAddress dst)
        {
            NeighborEntry ne = new NeighborEntry(null, null, null);
            
            
            ne.address = dst;
            int index = indexOf(ne);
            if (index >= 0)
                return (NeighborEntry)get(index);
            else
                return null;
            
        }
        
        public NeighborEntry ent_findshortest(Location target)
        {
            
            NeighborEntry ne = null;
            double shortest, t, tLater, shortestLater, myx, myy, myz;
            int i;
            t = Double.MAX_VALUE;
            shortestLater = Double.MAX_VALUE;
            NeighborEntry temp;
            
            shortest = selfNotEntity.getCurrentLocation().distance(target);
            for (i = 0; i < size(); i++){
                temp = (NeighborEntry)get(i);
                Location tempLoc = temp.loc.getClone();
                if (CONFIG_USE_SPEED && temp.bearing != null)
                {
                    double time = ((double)(JistAPI.getTime()-temp.lastSeen))/Constants.SECOND;
                    tempLoc.add(new Location.Location2D((float)(temp.bearing.getX()*temp.speed*time), 
                            (float)(temp.bearing.getY()*temp.speed*time)));
                }
                t = tempLoc.distance(target);
                
                if (t < shortest && !netAddr.equals(temp.address) && 
                        tempLoc.distance(ldb_.getLocation(netAddr)) <= je.transmitRadius*reliablePercent) 
                    // prevent from finding itself
                {
                    if (CONFIG_USE_FUTURE_LOCATION){
                        tempLoc.add(new Location.Location2D((float)(temp.bearing.getX()*temp.speed*futureStep), 
                            (float)(temp.bearing.getY()*temp.speed*futureStep)));
                        tLater = tempLoc.distance(target);
                        if (tLater < shortestLater) shortestLater = tLater;
                        else { // don't use this node
                            t = shortest;
                            temp = ne;
                        }
                    }
                    shortest = t;
                    ne = temp;
                }
            }
            return ne;
        }
        
        public String printNeighborList(Visualizer v)
        {
            String ret = "";
            Iterator it = iterator();
            while (it.hasNext())
            {
                NeighborEntry ne = (NeighborEntry)it.next();
                if (v!=null && CONFIG_USE_VISUALIZATION && v.showCommunication())
                {
                    v.setNodeColor(ne.address.toInt(), Color.CYAN);
                }
                ret += ne.toString() + "\n\n";                
            }
            if (ret == "") ret = "No neighbors!";
            return ret;
        }
        
        public NeighborEntry ent_findcloser_onperi(Location target, Integer perihop)
        {
            NeighborEntry ne = null;
            NeighborEntry neTemp = null;
            double mydist, t;
            int i, j;
            
            mydist = selfNotEntity.getCurrentLocation().distance(target);
            for (i = 0; i < size(); i++)
            {
                neTemp = (NeighborEntry)get(i);
                for (j = 0; j < neTemp.perilen; j++)
                {
                    t = target.distance(((PerimeterEntry)neTemp.peri.get(j)).loc);
                    if (t < mydist) {
                        perihop = new Integer(j);
                        return neTemp;
                    }
                }
            }
            
            return null;
        }
        
        public NeighborEntry ent_findcloser_edgept(Location ptLoc,
                NetAddress ptipa, NetAddress ptipb,
                Location dstLoc,
                Location closerLoc)
        {
            Iterator ni;
            NeighborEntry minne = null, ne;
            
            
            Location myLoc = selfNotEntity.getCurrentLocation();
            ni = iterator();
            while (ni.hasNext()) {
                ne = (NeighborEntry)ni.next();
                closerLoc = ne.closer_pt(selfNotEntity.netAddr, myLoc, ptLoc, ptipa, ptipb,
                        dstLoc, closerLoc);
                if (closerLoc!=null) {
                    // found an edge with a point closer than (ptx, pty)
                    minne = ne;
                    ptLoc = closerLoc;
                    ptipa = selfNotEntity.netAddr;
                    ptipb = ne.address;
                }
            }
            return minne;
        }
        
        public NeighborEntry ent_next_ccw(NeighborEntry inne, boolean p)
        {
            Location myLoc;
            double brg;
            NeighborEntry ne;
            
            // find bearing from mn to (x, y, z)
            myLoc = selfNotEntity.getCurrentLocation();
            brg = bearing(myLoc, inne.loc);
            ne = ent_next_ccw(brg, myLoc, p, inne);
            if (ne==null)
                return inne;
            else
                return ne;
        }
        
        public NeighborEntry ent_next_ccw(double basebrg, Location l, boolean p,
                NeighborEntry inne)
        {
        	// T-ODO this doesn't support vehicles with the exact same bearing
            NeighborEntry minne = null, ne;
            Iterator ni;
            double brg, minbrg = 3*Math.PI;
            
            ni = iterator();
            while (ni.hasNext()) {
                ne = (NeighborEntry)ni.next();
                if (inne!=null && (ne.equals(inne)))
                    continue;
                if (p && ne.live<1)
                    continue;
                // T-ODO displace nodes from center of road to eliminate (mitigate?) this problem
                if (inne!=null && inne.loc.distance(ne.loc)==0)
                    continue;
                brg = bearing(l, ne.loc) - basebrg;
                if (brg == 0) continue; // this is a disgusting hack for preventing an infinite loop
                if (brg < 0)
                    brg += 2*Math.PI;
                if (brg < 0)
                    brg += 2*Math.PI;
//                if (brg == minbrg)
//                {
//                    // make sure that all of the minima are handled round robin
//                    // for now, just do this randomly
//                	if (!sameBrg.contains(ne)){
//                		sameBrg.add(ne);
//                		totalCount++;
//                	}
//                }
                else if (brg < minbrg) {
                    minbrg = brg;
                    minne = ne;
//                    // I think this only occurs with 0 relative bearing
//                    // sameBrg.removeAllElements();
//                    if (brg < minbrg_ ) {
//                        sameBrg.removeAllElements();
//                        index = -1;
//                        totalCount = 0;
//                        minbrg_ = brg;
//                        
//                    }
//                    if (brg == 0)
//                    {
//                        // new cycle
//                        if (!sameBrg.contains(minne)) {
//                            sameBrg.removeAllElements();  
//                            index = -1;
//                            totalCount = 0;
//                        }
//                    }
//                    sameBrg.add(minne);
//                    totalCount++;
                }
            
            }
//            if (sameBrg.size()==1 || minne == null)
                return minne;
//            else if (index < totalCount)
//            {
//            	// T-ODO make sure postincrement is working right here
//                return (NeighborEntry)sameBrg.get(++index);
//            }
//            else // move to next one
//            {
//                minbrg_ = Double.MAX_VALUE;
//                sameBrg.removeAllElements();
//                index = -1;
//                totalCount = 0;
//                
//            	minbrg = 3*Math.PI;
//                ni = iterator();
//                while (ni.hasNext()) {
//                    ne = (NeighborEntry)ni.next();
//                    if (inne!=null && (ne.equals(inne)))
//                        continue;
//                    if (p && ne.live<1)
//                        continue;
//                   
//                    if (inne!=null && inne.loc.distance(ne.loc)==0)
//                        continue;
//                    brg = bearing(l, ne.loc) - basebrg;
//                    if (brg < 0)
//                        brg += 2*Math.PI;
//                    if (brg < 0)
//                        brg += 2*Math.PI;
//
//                    else if (brg < minbrg && brg != 0) {
//                        minbrg = brg;
//                        minne = ne;
//                    }
//                }
//                
//                return minne;
//            }
        }
        
        public NeighborEntry ent_findface(Location l, boolean p)
        {
            double brg;
            
            // find bearing to dst
            Location myLoc = selfNotEntity.getCurrentLocation();
            brg = bearing(myLoc, l);
            // find neighbor with greatest bearing <= brg
            return ent_next_ccw(brg, myLoc, p, null);
        }
        
        public void planarize(int algo, Location l)
        {
            NeighborEntry ne;
            Iterator ni;
            
            ni = iterator();
            while (ni.hasNext()) {
                ne = (NeighborEntry)ni.next();
                ne.planarize(this, algo, l); // remove all crossing edges
            }
        }

        /**
         * Returns the number of neighbors that are alive.
         * @return
         */
        public int getNumAlive() {
            Iterator ni = iterator();
            NeighborEntry ne = null;
            int aliveCount = 0;
            
            while (ni.hasNext()) {
                ne = (NeighborEntry)ni.next();
                if (ne.live > 0) aliveCount++;
            }
            
            return aliveCount;
                
        }

		public void removeStale() {
			Iterator it = iterator();
			while (it.hasNext()){
				NeighborEntry ne = ((NeighborEntry)it.next());
				if (JistAPI.getTime() - ne.lastSeen > bexp_*Constants.SECOND){
					it.remove();
				}
			}
			
		} 
        
    } // end class NeighborTable
    
    public class NeighborEntry extends AddressGeographic
    {
        private RouteGPSR a;
        public boolean timerDisabled = false;
        public Vector peri;		// perimeter via this neighbor
        int perilen;			// length of perimeter
        int maxlen;			// allocated slots in peri
        DeadNeighborTimer dnt;	// timer for expiration of neighbor
        PerimeterProbeTimer ppt;	// timer for generation of perimeter probe to
        //   neighbor
        int live;			// when planarizing, whether edge should be
        //   used
        long lastSeen;
        public float speed;
        public Location bearing;
        
        /**
         * @param loc
         * @param address
         */
        public NeighborEntry(Location loc, NetAddress address, MacAddress macAddress) {
            super(loc, address, macAddress);
            
            
            dnt = new DeadNeighborTimer(this, DNT_TIMEOUT);
            
            ppt = new PerimeterProbeTimer(this, PPT_TIMEOUT);
            peri = new Vector();
            
            lastSeen = JistAPI.getTime();
            
        }       
        
        public Location closer_pt(NetAddress myip, Location myLoc,
                Location ptLoc,
                NetAddress ptipa, NetAddress ptipb,
                Location dstLoc,
                Location closerLoc)
        {
            
            if ((Math.min(address.toInt(), myip.toInt()) == Math.min(ptipa.toInt(), ptipb.toInt())) &&
                    (Math.max(address.toInt(), myip.toInt()) == Math.max(ptipa.toInt(), ptipb.toInt())))
                // this edge is the same edge where (ptx, pty) lies; nope
                return null;
            if (live<1)
                // this edge is not part of the planarized graph
                return null;
            
            closerLoc = cross_segment(ptLoc, dstLoc, myLoc, loc,
                    closerLoc);
            if (closerLoc != null) {
                if (closerLoc.distance(dstLoc) <
                        ptLoc.distance(dstLoc)) {
                    // edge has point closer than (ptx, pty)
                    return closerLoc;
                }
            }
            return null;                        
        }
        
        public void planarize(NeighborTable nt, int algo, Location l) {
            NeighborEntry ne;
            Iterator ni;
            double uvdist, canddist, midx, midy;
            midx = midy = 0;
            
            uvdist = l.distance(this.loc);
            switch(algo) {
            case PLANARIZE_RNG:
                break;
            case PLANARIZE_GABRIEL:
                // find midpt of segment me (u) <-> this (v)
                midx = (l.getX() + loc.getX()) / 2.0;
                midy = (l.getY() + loc.getY()) / 2.0;
                uvdist /= 2.0;
                break;
            default:
                throw new RuntimeException("Unknown graph planarization algorithm: "+algo);
            //break; // unreachable
            }
            ni = nt.iterator();
            while (ni.hasNext()) {
                ne = (NeighborEntry)ni.next();
                if (ne == this)
                    // w and v identical node--w not a witness
                    continue;
                switch(algo) {
                case PLANARIZE_RNG:
                    // find max dist. from me (u) to ne (w) vs. this (v) to ne (w)
                    canddist = Math.max(l.distance(ne.loc), loc.distance(ne.loc));
                    // is max < dist from me (u) to this (v)?
                    if (canddist < uvdist) {
                        this.live = 0;
                        return;
                    }
                    break;
                case PLANARIZE_GABRIEL:
                    // is ne (w) inside circle of radius uvdist?
                    Location.Location2D midLoc = new Location.Location2D((float)midx, (float)midy);
                    if (midLoc.distance(ne.loc) < uvdist) {
                        this.live = 0;
                        return;
                    }
                    break;
                default:
                    break;
                }
            }
            this.live = 1;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj) {
            if (obj == null) return false;
            NeighborEntry ne2 = (NeighborEntry)obj;
            if (ne2.address == null || this.address == null) return false;
            return this.address.equals(ne2.address);
        }
        
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            Location tempLoc = loc.getClone();
            float error=0;
            double time = ((double)(JistAPI.getTime()-lastSeen))/Constants.SECOND;
            
            if (bearing != null){
                tempLoc.add(new Location.Location2D((float)(bearing.getX()*speed*time), 
                        (float)(bearing.getY()*speed*time)));
	            error = tempLoc.distance(ldb_.getLocation(address));
            }

            return "\t"+super.toString() + 
            "\n\tAdjusted for bearing: "+ ((bearing!=null)?(tempLoc.toString()+
                    "\n\tError: "+error+"\tSpeed: "+speed+ "\n"):"No bearing\n")  +
            "\tLast seen: "+time +" seconds ago";
            
        }
    }
    /**
     *
     * The PerimeterEntry class does not seem to add any functionality 
     * to the generic AddressGeographic class. This is here for comparison 
     * with the NS-2 implementation. 
     * 
     */
    public static class PerimeterEntry extends AddressGeographic
    {
        
        /**
         * @param loc
         * @param address
         */
        public PerimeterEntry(Location loc, NetAddress address, MacAddress macAddr) {
            super(loc, address, macAddr);
            
        }
    }
    
    public class DeadNeighborTimer extends NS2Timer
    {
        private Location loc;
        private NetAddress addr;
        private long creationTime;
        
        public DeadNeighborTimer(NeighborEntry ne, long timeout)
        {
            super(selfNotEntity, pendingTimers);
            this.loc = ne.loc;
            this.addr = ne.address;
            this.timeoutInterval = timeout;       
            creationTime = JistAPI.getTime();
        }
        
        /* (non-Javadoc)
         * @see jist.swans.route.RouteGPSR_Old.NS2Timer#callback()
         */
        protected void callback(int id) {
            //long currentTime = JistAPI.getTime();
            self.callback(CALLBACK_DN, loc, addr, this, id);
        }
        
    }
    
    public class PerimeterProbeTimer extends NS2Timer
    {
        private NeighborEntry ne;
        
        public PerimeterProbeTimer(NeighborEntry ne, long timeout)
        {
            super(selfNotEntity, pendingTimers);
            this.ne = ne;
        }
        
        /* (non-Javadoc)
         * @see jist.swans.route.RouteGPSR_Old.NS2Timer#callback()
         */
        protected void callback(int id) {
            self.callback(CALLBACK_PERI_PROBE, ne.loc, ne.address, this, id);            
        }
        
    }
    
    public class GPSR_BeaconTimer extends NS2Timer
    {
        
        public GPSR_BeaconTimer(RouteGPSR a)
        {
            super(a, pendingTimers);
        }
        
        /* (non-Javadoc)
         * @see jist.swans.route.RouteGPSR_Old.NS2Timer#callback()
         */
        protected void callback(int id) {
            self.callback(CALLBACK_BEACON, null, null, this, id);            
        }
        
    }
    
    public class GPSR_LastPeriTimer extends NS2Timer
    {
        
        public GPSR_LastPeriTimer(RouteGPSR a)
        {
            super(a, pendingTimers);
        }
        
        /* (non-Javadoc)
         * @see jist.swans.route.RouteGPSR_Old.NS2Timer#callback()
         */
        protected void callback(int id) {
            self.callback(CALLBACK_LAST_PERI, null, null, this, id);            
        }
        
    }
    
    public class GPSR_PlanarTimer extends NS2Timer
    {
        
        public GPSR_PlanarTimer(RouteGPSR a)
        {
            super(a, pendingTimers);
        }
        
        /* (non-Javadoc)
         * @see jist.swans.route.RouteGPSR_Old.NS2Timer#callback()
         */
        protected void callback(int id) {
            self.callback(CALLBACK_PLANAR, null, null, this, id);            
        }
        
    }
    
    /**
     * @param field
     * @param selfId
     */
    public RouteGPSR(Field field, int selfId, LocationDatabase ldb ) {
        super(field, selfId);
        init(selfId, ldb, JistExperiment.getJistExperiment());
        
    }
    
    /**
     * Constructor that allows interaction with global configuration object.
     * @param field
     * @param selfId
     * @param je
     */
    public RouteGPSR(Field field, int selfId, LocationDatabase ldb, JistExperiment je) {
        super(field, selfId);
        init(selfId, ldb, je);
    }
    
    private void init(int selfId, LocationDatabase ldb, JistExperiment je)
    {
        netAddr = new NetAddress(selfId);
        macAddr = new MacAddress(selfId);
        this.ldb_ = ldb;
        this.je = je;
        ntab_ = new NeighborTable(this);
        self = (RouteInterface.Geo)JistAPI.proxy(this, RouteInterface.Geo.class);
        pendingTimers = new HashMap();
        
        this.bdesync_ = je.bdesync_;
        this.bexp_ = je.bexp_;
        this.bint_ = je.bint_;
        this.drop_debug_ = je.drop_debug_;
        this.lpexp_ = je.lpexp_;
        this.pdesync_ = je.pdesync_;
        this.peri_proact_ = je.peri_proact_;
        this.pint_ = je.pint_;
        this.use_implicit_beacon_ = je.use_implicit_beacon_;
        this.use_loop_detect_ = je.use_loop_detect_;
        this.use_mac_ = je.use_mac_;
        this.use_peri_ = je.use_peri_;
        this.use_planar_ = je.use_planar_;
        this.use_timed_plnrz_ = je.use_timed_plnrz_;
        this.verbose_ = je.verbose_;
        // ignoring offset_gpsr becaues i think that i won't need it
        
        if (!keySet  && Visualizer.getActiveInstance()!=null){
        	Visualizer v = Visualizer.getActiveInstance();
            v.registerKeyItem(Color.MAGENTA, Visualizer.CAR, " Packet received at dest");
            v.registerKeyItem(Color.CYAN, Visualizer.CAR, " Neighbor");
            v.registerKeyItem(Color.GREEN, Visualizer.CAR, " Current node");
            v.registerKeyItem(Color.BLUE, Visualizer.CAR, " Next hop for packet");
            v.registerKeyItem(Color.ORANGE, Visualizer.CAR, " Destination for packet");
            v.registerKeyItem(Color.YELLOW, Visualizer.CAR, " Packet dropped for dest");
            v.registerKeyItem(Color.GREEN, Visualizer.CIRCLE, "  GPSR beacon sent");    
            v.registerKeyItem(Color.BLUE, Visualizer.CIRCLE, "  GPSR greedy packet sent");        
            v.registerKeyItem(Color.YELLOW, Visualizer.CIRCLE, "  GPSR peri packet sent");    
            keySet = true;
        }
        
    }
    
    // T-ODO implement this method
    void tracepkt(Message p, double now, int me, String type)
    {
        
        //      char buf[1024];
        //
        //      hdr_gpsr gpsrh = hdr_gpsr::access(p);
        //
        //      snprintf (buf, 1024, "V%s %.5f _%d_:", type, now, me);
        //
        //      if (gpsrmsg.mode_ == GPSRH_BEACON) {
        //        snprintf (buf, 1024, "%s (%f,%f,%f)", buf, gpsrmsg.hops_[0].x,
        //    	      gpsrmsg.hops_[0].y, gpsrmsg.hops_[0].z);
        //        if (verbose_)
        //          trace("%s", buf);
        //      }
    }
    
    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterfaceGeo#timeout()
     */
    public void timeout(Long id, long interval, int timerId) {
        
        if (interval < 0) throw new RuntimeException("Invalid sleep interval!");
        
        JistAPI.sleep(interval);
        
        NS2Timer t = (NS2Timer)pendingTimers.remove(id);
        t.timeout(timerId);
    }
    
    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface#peek(jist.swans.net.NetMessage, jist.swans.mac.MacAddress)
     */
    public void peek(NetMessage msg, MacAddress lastHop) {
        
        if (msg instanceof NetMessage.Ip)
        {
            NetMessage.Ip ipmsg = (NetMessage.Ip)msg;
            
            /* ignore non-IP packets.
             ignore beacons; we process those on regular receive.
             assumes the MAC peek includes all unicast packets bound for us.
             process those here, and avoid calls to beacon_proc() elsewhere. */
            if (use_implicit_beacon_ &&
                    //(ipmsg. == AF_INET) &&
                    (ipmsg.hasNextHop() && ipmsg.getNextHop() != NetAddress.ANY))
            {
                // snoop it as proof of its sender's existence.
                if (ipmsg.getPayload() instanceof GPSRMessage)
                {
                    GPSRMessage gpsrmsg = (GPSRMessage)ipmsg.getPayload();
                    PerimeterEntry pe;
                    
                    switch (gpsrmsg.mode_) {
                    case GPSRMessage.GPSRH_DATA_GREEDY:
                        if (gpsrmsg.hops_.size()< 1)
                        {
                            break;
                        }
                        // prev hop position lives in hops_[0]
                        pe = (PerimeterEntry)gpsrmsg.hops_.get(0);
                        // T-ODO figure out why messages are not being cloned properly
                        if (pe.address.equals(netAddr)) 
                        	break;
                    	beacon_proc(pe.address, pe.loc, pe.macAddr, gpsrmsg.speed, gpsrmsg.bearing);
                    	
                    	// accept message even if GPSR was too dumb to realize that the 
                    	// destination was actually within range
                    	if (!ipmsg.getNextHop().equals(netAddr) && 
                    	        ipmsg.getDst().equals(netAddr)) 
                    	{
                    	    self.receive(gpsrmsg, ipmsg.getSrc(), lastHop,
                                (byte)Constants.NET_INTERFACE_DEFAULT, ipmsg.getDst(),
                                ipmsg.getPriority(), (byte) ipmsg.getTTL());
                    	}
                    
                    break;
                    case GPSRMessage.GPSRH_PPROBE:
                        // prev hop position lives in hops_[nhops_-1]
                        pe = (PerimeterEntry)gpsrmsg.hops_.lastElement();
                    	beacon_proc(pe.address, pe.loc, pe.macAddr, gpsrmsg.speed, gpsrmsg.bearing);
                    break;
                    case GPSRMessage.GPSRH_DATA_PERI:
                        // X-XX was hops_[gpsrmsg.currhop_-1]
                        // prev hop position lives in hops_[0]
                        pe = (PerimeterEntry)gpsrmsg.hops_.get(0);
                        if (pe!=null)
                        	beacon_proc(pe.address, pe.loc, pe.macAddr, gpsrmsg.speed, gpsrmsg.bearing);
                        
                    	

                    	// accept message even if GPSR was too dumb to realize that the 
                    	// destination was actually within range
                    	if (!ipmsg.getNextHop().equals(netAddr) && 
                    	        ipmsg.getDst().equals(netAddr)) 
                    	{
                    	    self.receive(gpsrmsg, ipmsg.getSrc(), lastHop,
                                (byte)Constants.NET_INTERFACE_DEFAULT, ipmsg.getDst(),
                                ipmsg.getPriority(), (byte) ipmsg.getTTL());
                    	}
                    break;
                    default:
                        throw new RuntimeException("Yow! tap got packet of unk type " 
                                + gpsrmsg.mode_ + "!");            
                    //break; // unreachable
                    }
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface#send(jist.swans.net.NetMessage)
     */
    public void send(NetMessage msg) {
  

        
        if (!(msg instanceof NetMessage.Ip))
        {
            throw new RuntimeException("Only Ip messages supported!");
        }

                
        NetMessage.Ip ipMsg = ((NetMessage.Ip)msg);
        
        // new message
        if (netAddr.equals(ipMsg.getSrc()) && 
                !(ipMsg.getPayload() instanceof GPSRMessage))
        {	    	
            stats.numMessagesSent++;
            GPSRMessage gpsrh = new GPSRMessage(ipMsg.getPayload(), ipMsg.getProtocol(), ipMsg.getDst(), 
            		netAddr);    	
            // encapuslate original message, use GPSR as protocol to allow stripping 
            // of headers before receipt at upper layer.
            ipMsg = new NetMessage.Ip(gpsrh, ipMsg.getSrc(), ipMsg.getDst(), 
                    Constants.NET_PROTOCOL_GPSR, ipMsg.getPriority(), 128);	    	
            
            Location destLoc;

            // Fill in the destination's position
            destLoc = ldb_.getLocation(ipMsg.getDst());
            ipMsg.setLocation(new NetMessage.IpOptionLocation(destLoc));
            
            
            // all data packets begin life in greedy mode
            gpsrh.mode_ = GPSRMessage.GPSRH_DATA_GREEDY;
        }
        if (ipMsg.getPayload() instanceof GPSRMessage)
        {
            stats.numMessagesForwarded++;
        }
        if (netAddr.equals(ipMsg.getSrc()) || 
                ipMsg.getNextHop().equals(netAddr))
            forwardPacket(ipMsg, false);
        else
        {
            System.err.println("Why am I here?");
        }
        
    }
    
    /* (non-Javadoc)
     * @see jist.swans.misc.Protocol#start()
     */
    public void start() {
        
        beacon_timer_ = new GPSR_BeaconTimer(this);
        beacon_timer_.sched(Constants.random.nextDouble()*bint_);
        if (!peri_proact_) {
            lastperi_timer_ = new GPSR_LastPeriTimer(this);
            lastperi_timer_.sched(lpexp_);
        }
        if (use_timed_plnrz_) {
            planar_timer_ = new GPSR_PlanarTimer(this);
            // X-XX should make interval configurable!!
            planar_timer_.sched(1.0);
        }
    }
    
    /* (non-Javadoc)
     * @see jist.swans.net.NetInterface.NetHandler#receive(jist.swans.misc.Message, jist.swans.net.NetAddress, jist.swans.mac.MacAddress, byte, jist.swans.net.NetAddress, byte, byte)
     */
    public void receive(Message msg, NetAddress src, MacAddress lastHop,
            byte macId, NetAddress dst, byte priority, byte ttl) {
        
        if (msg instanceof GPSRMessage){
            
            NetMessage.Ip ipMsg;
            GPSRMessage gpsrh = (GPSRMessage)msg;
            Location destLoc;
            
            //    		NetAddress src = ipMsg.getSrc();
            //    		NetAddress dst = ipMsg.getDst();
            
            // I received a packet that I sent.
            if ((src.equals(netAddr)) && 
                    (gpsrh.mode_ == GPSRMessage.GPSRH_DATA_GREEDY)) {
                /* GPSR peri probes and peri data packets *can* visit the src twice;
                 only greedy data packets shouldn't. */
                drop(msg, DROP_RTR_ROUTE_LOOP);
                return;
            }
            else {
                /* packet I'm forwarding.
                 Check the TTL. If it is zero, then discard. */
                ttl--;
                if (ttl == 0) {
                    drop(msg, DROP_RTR_TTL);
                    return;
                }
            }
            
            //    		// this handler gets called only if the protocol is GPSR
            //    		if (ipMsg.getProtocol() == Constants.NET_PROTOCOL_GPSR) {
            // callees here must free the packet if they don't forward it!!!
            switch (gpsrh.mode_) {
            case GPSRMessage.GPSRH_BEACON:
                //    		        System.out.println("In GPSR:receive, received beacon!");
                if (!src.equals(netAddr))
                {
                    stats.numBeaconsRecv++;
                    // don't receive my own beacons; I'm not my neighbor
                    beaconIn(gpsrh, src);
                }
            break;
            case GPSRMessage.GPSRH_PPROBE:
                stats.numPerimeterProbesRecv++;
            periIn(gpsrh, false);
            break;
            case GPSRMessage.GPSRH_DATA_GREEDY:
                
                stats.arrivedAtDest++;
            ipMsg = new NetMessage.Ip(gpsrh.payload, src, gpsrh.dest, 
                    gpsrh.protocol, priority, ttl);
            
            netEntity.receive(ipMsg, lastHop, macId, false);
            
            if (je.visualizer!=null && CONFIG_USE_VISUALIZATION && je.visualizer.showCommunication()){
	            je.visualizer.setGeneralPaneText("Dropped: "+stats.numDataDropped+
	                    "\n"+"Received: "+stats.arrivedAtDest);
	            je.visualizer.resetColors();
	            je.visualizer.setNodeColor(netAddr.toInt(), Color.MAGENTA);
            }
            
            //System.err.println("greedy data pkt @ "+src+":GPSR_PORT!");
            break;
            case GPSRMessage.GPSRH_DATA_PERI:
                stats.arrivedAtDest++;
            ipMsg = new NetMessage.Ip(gpsrh.payload, src, netAddr, 
                    gpsrh.protocol, priority, (ttl));
            
            netEntity.receive(ipMsg, lastHop, macId, false);
            if (je.visualizer!=null && CONFIG_USE_VISUALIZATION && je.visualizer.showCommunication()){
	            je.visualizer.setGeneralPaneText("Dropped: "+stats.numDataDropped+
	                    "\n"+"Received: "+stats.arrivedAtDest);
	            je.visualizer.resetColors();
	            je.visualizer.setNodeColor(netAddr.toInt(), Color.MAGENTA);
            }

            
            //System.err.println("peri data pkt @ "+src+":GPSR_PORT!");
            break;
            default:
                System.err.println("unk pkt type "+gpsrh.mode_+" @ "
                        +src+":GPSR_PORT!");
            break;
            }
            //    		}
            //    		else
            //    			forwardPacket((NetMessage.Ip)msg, false);
        }
    }
    
    ///////////////////////////////////////////////
    // GPSR functions from NS-2
    //
    public void beacon_callback()	// generate a beacon (timer-triggered)
    {

        NetMessage.Ip p = makeAlive();
        
        // schedule the transmission of this beacon
        self.sendIpMsg(p, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
        
        Visualizer v = Visualizer.getActiveInstance();
        if (v!=null && CONFIG_USE_VISUALIZATION && v.showCommunication()){
            v.drawAnimatedTransmitCircle(netAddr.toInt(), Color.GREEN);
        }
        
        stats.numBeaconsSent++;
        // schedule the next beacon generation event
        beacon_resched(true);
    }
    public void deadneighb_callback(Location loc, NetAddress addr) // neighbor gone (timer/MAC-trig)
    {
        NeighborEntry toRemove = new NeighborEntry(loc, addr, null);
        long now = JistAPI.getTime();
        stats.numDeadNeighbors++;
        if (verbose_){
            //    	    trace ("VTO %.5f _%d_ %d->%d", now, mn_->address(), mn_->address(),
            //    		   ne->dst);
        }
        // remove the neighbor entry from the table!
        ntab_.ent_delete(toRemove);
        // need to re-planarize, if option dictates
        if (use_planar_) {    	   
            ntab_.planarize(PLANARIZE_RNG, getCurrentLocation());
        }
    }
    
    public void periprobe_callback(NeighborEntry ne) // gen perimeter probe (timer-trig)
    {
        GPSRMessage gpsrmsg = new GPSRMessage(netAddr);
        gpsrmsg.mode_ = GPSRMessage.GPSRH_PPROBE;
        //gpsrmsg.nhops_ = 0;
        gpsrmsg.add_hop(this.netAddr, getCurrentLocation(), this.macAddr);
        
        NetMessage.Ip p = new NetMessage.Ip(gpsrmsg, netAddr, NetAddress.ANY,
                Constants.NET_PROTOCOL_GPSR, Constants.NET_PRIORITY_NORMAL, 128);  
        NetMessage.IpOptionNextHop nh = new NetMessage.IpOptionNextHop(ne.address);
        
        p.setNextHop(nh);
        
        // schedule probe transmission
        self.sendIpMsg((NetMessage.Ip)p, Constants.NET_INTERFACE_DEFAULT, ne.macAddr);
        
        stats.numPerimeterProbesSent++;
        
        if (use_implicit_beacon_)
        {
            beacon_resched(false);
        }
        
        // schedule next probe timer
        ne.ppt.resched(pint_ +
                (Constants.random.nextDouble()*(2 * pdesync_ * pint_)) - pdesync_ * pint_);
        
    }
    
    /**
     * 
     */
    private void beacon_resched(boolean newEvent) {
        double interval = bint_ + (Constants.random.nextDouble()*(2*bdesync_* bint_)) -
        bdesync_ * bint_;
        
        beacon_timer_.resched(interval);	
    }
    
    public void lastperi_callback()	// turn off peri probes when unused for timeout
    {
        NeighborEntry ne;
        Iterator ni = ntab_.iterator();
        
        // don't probe perimeters proactively anymore
        peri_proact_ = false;
        // cancel all perimeter probe timers
        while (ni.hasNext())
        {
            ne = (NeighborEntry)ni.next();
            
            ne.ppt.force_cancel();
        }
    }
    public void planar_callback()	// planarization callback
    {
        // re-planarize graph
        if (use_planar_) {
            
            ntab_.planarize(PLANARIZE_RNG, getCurrentLocation());
        }
        // reschedule us
        // X-XX should make interval tunable!!!
        planar_timer_.resched(1.0);
    }
    //    public abstract int command(int argc, const char * const * argv);
    public void lost_link(Message p, MacAddress nextHop)
    {
        NeighborEntry ne;
        
        if (use_mac_ == false) {
            if (p instanceof QueuedMessage)
            {
                QueuedMessage qm = (QueuedMessage)p;
                NetMessage.Ip ipmsg = (NetMessage.Ip)qm.getPayload();
                GPSRMessage gpsrmsg = (GPSRMessage)ipmsg.getPayload();
                if (gpsrmsg.getPayload() == null) return;
            }
            drop(p, DROP_RTR_MAC_CALLBACK);
            return;
        }
        if (p instanceof NetMessage.Ip)
        {
            NetMessage.Ip ipmsg = (NetMessage.Ip)p;
            
            //DEBUG
            //printf("(%d)..Lost link..\n",myaddr_);
            if (verbose_ )
            {
                //              trace ("VLL %.8f %d->%d lost at %d",
                //          	   Scheduler::instance().clock(),
                //          	   ((hdr_ip *) p->access(off_ip_))->src_,
                //          	   ((hdr_ip *) p->access(off_ip_))->dst_,
                //          	   mn_->address());
            }
            
            
            //   if (hdrc->addr_type_ == AF_INET) {
            ne = ntab_.ent_finddst(ipmsg.getNextHop());
            if (verbose_){
                //                              trace ("VLP %.5f %d:%d->%d:%d lost at %d [hop %d]",
                //                        	     Scheduler::instance().clock(),
                //                        	     ((hdr_ip *) p->access(off_ip_))->src_,
                //                        	     ((hdr_ip *) p->access(off_ip_))->sport_,
                //                        	     ((hdr_ip *) p->access(off_ip_))->dst_,
                //                        	     ((hdr_ip *) p->access(off_ip_))->dport_,
                //                        	     mn_->address(), hdrc->next_hop_);
            }
            if (ne != null) {
                ne.dnt.force_cancel();
                deadneighb_callback(ne.loc, ne.address);
            }
            //   }
            
            if (CONFIG_RECOVER)
            {
                ifq_ = netEntity.getMessageQueue(Constants.NET_INTERFACE_DEFAULT);
                QueuedMessage qm; // T-ODO check

                Vector unchanged = new Vector();
                Vector refactor = new Vector();
                refactor.add(new QueuedMessage(ipmsg, nextHop)); // add dropped packet to list
                

                // T-ODO this section needs thorough debugging
                // grab packets in the ifq bound for the same next hop
                while(!ifq_.isEmpty()) {
                    qm = ifq_.remove(Constants.NET_PRIORITY_NORMAL);
                    if (qm.getNextHop().equals(nextHop) )
                    {
                        refactor.add(qm);
                    }
                    else
                    {
                        unchanged.add(qm);
                    }
                    
                } // end extracting packets
                
                Iterator it;
                // put all unchanged back into the queue
                if (unchanged.size() > 0)
                {
                    it = unchanged.iterator();
                    
                    while (it.hasNext())
                    {
                        ifq_.insert((QueuedMessage)it.next(), Constants.NET_PRIORITY_NORMAL);
                    }
                }
                
                QueuedMessage rt;
                
                // retarget all packets we got to a new next hop
                it = refactor.iterator();
                while (it.hasNext()) {
                    rt = (QueuedMessage)it.next();
                    
                    if (!(rt.getPayload() instanceof NetMessage.Ip)) {
                        // no such thing in SWANS; removing from queue is effectively dropping it
                        // no callback for this packet.
                        drop(rt, DROP_RTR_MAC_CALLBACK);
                        continue;
                    }
                    
                    ipmsg = (NetMessage.Ip)rt.getPayload();
                    // for GPSR perimeter probes, chop off our own perimeter entry before
                    // passing the probe back into the agent for reforwarding
                    //if ( ((hdr_ip *) rt->access(off_ip_))->dport_ == GPSR_PORT) {
                    
                    if (ipmsg.getPayload() instanceof GPSRMessage)
                    {
                        GPSRMessage gpsrmsg = (GPSRMessage)ipmsg.getPayload();
                        
                        if (gpsrmsg.mode_ == GPSRMessage.GPSRH_PPROBE) {
                            if (gpsrmsg.hops_.size() == 1) 
                            {
                                /* we originated it. the neighbor is gone, according to the MAC
                                 layer. drop the probe--it was *only* meant for that neighbor. */
//                                drop(rt, DROP_RTR_NEXT_SRCRT_HOP);
                                continue;
                            }
                            /* we were forwarding the probe, so instead try to recover by
                             forwarding it to a remaining appropriate next hop */
                            gpsrmsg.nhops_--;
                            //gpsrmsg.hops_.remove(gpsrmsg.hops_.size()-1);
                            //hdrc->size() = gpsrmsg.size() + IP_HDR_LEN;
                            periIn(gpsrmsg, GPSR_PPROBE_RTX);
                        }
                        
                        else {
                            switch (gpsrmsg.mode_) {
                            case GPSRMessage.GPSRH_DATA_GREEDY:
                                // give the packet another chance--exercise greedy's good recovery
                                forwardPacket((NetMessage.Ip)rt.getPayload(), false);
                            break;
                            case GPSRMessage.GPSRH_DATA_PERI:
                                if (use_planar_)
                                    // not src-routed; give it another chance via another neighbor
                                    forwardPacket(ipmsg, true);
                                else
                                    // punt the packet; its chosen src-routed next hop is gone
                                    drop(ipmsg, DROP_RTR_NEXT_SRCRT_HOP);
                            break;
                            default:
                                throw new RuntimeException(
                                "yow! non-data packet for non-GPSR port bounced by MAC!\n");
                            //break; // unreachable
                            }
                        } // end else for data packet
                    } // end if GPSRMessage
                    else
                    {
                        throw new RuntimeException("Found non-GPSR message in queue!");
                    }
                } // end while for retargeting packets
            } // end if for recovering
            else
            {
                drop (p, DROP_RTR_MAC_CALLBACK);
            }
        }
        
    }
    
    private NetMessage.Ip makeAlive()
    {
        GPSRMessage gpsrmsg = new GPSRMessage(netAddr);
        gpsrmsg.mode_ = GPSRMessage.GPSRH_BEACON;
        //  	  gpsrmsg.nhops_ = 1;
        gpsrmsg.add_hop(this.netAddr, getCurrentLocation(), this.macAddr);
        
        if (CONFIG_USE_SPEED)
        {
            setSpeedInfo(gpsrmsg);
        }
        
        NetMessage.Ip p = new NetMessage.Ip((Message)gpsrmsg, netAddr, NetAddress.ANY,
                Constants.NET_PROTOCOL_GPSR, Constants.NET_PRIORITY_NORMAL, Constants.TTL_DEFAULT);    	     	    	  
        
        NetMessage.IpOptionNextHop nh = new NetMessage.IpOptionNextHop(NetAddress.ANY);
        
        p.setNextHop(nh);
        
        return p;
    }
    
    private void setSpeedInfo(GPSRMessage m)
    {
        m.speed = field.getRadioData(selfId).getMobilityInfo().getSpeed();
        m.bearing = field.getRadioData(selfId).getMobilityInfo().getBearing();
        
    }
    
    private void beaconIn(GPSRMessage m, NetAddress src)
    {    	         
        //        Iterator it = m.hops_.iterator();
        //        while (it.hasNext())
        //        {
        //            System.out.println(it.next());
        //        }
        //        System.out.println("Hops size: " + m.hops_.size());
        beacon_proc(src, ((PerimeterEntry)m.hops_.get(0)).loc, 
                ((PerimeterEntry)m.hops_.get(0)).macAddr, m.speed, m.bearing);
    }
    
    private void trace(String fmt)
    {
        
    }
    
    @SuppressWarnings("unchecked")
	private void forwardPacket(NetMessage.Ip p, boolean rtxflag)
    {
//    	 clean up stale entries (T-ODO this should not be necessary if i could find the bug causing stale entries)
    	ntab_.removeStale();
    	
        if (p.getDst().equals(this.netAddr)){
        	System.out.println("Wtf!");
        }
        
        if (v!= null && CONFIG_USE_VISUALIZATION && v.showCommunication()){
            v.resetColors();
            v.setNodeColor(netAddr.toInt(), Color.GREEN);
            
            if (showNeighborText) v.setRoutingPaneText(netAddr.toInt(), ntab_.printNeighborList(v));
            v.setNodeColor(p.getDst().toInt(), Color.ORANGE);

        }
        
        NetMessage.Ip ipmsg = p;
        GPSRMessage gpsrh = null;
		try {
			gpsrh = (GPSRMessage) ((GPSRMessage)ipmsg.getPayload()).clone();
			ipmsg = p.copy();
			ipmsg.setPayload(gpsrh);
		} catch (CloneNotSupportedException e) {
			// T-ODO Auto-generated catch block
			e.printStackTrace();
		}
        MacAddress nextHop = MacAddress.ANY;
        NetAddress oldNextHop = p.getNextHop();
        
        NeighborEntry ne, logne;
        long now = JistAPI.getTime();
        boolean escape = false;       
        
        switch(gpsrh.mode_) {
        case GPSRMessage.GPSRH_DATA_GREEDY:
        	
        	
            // prevent unnecessary loops
            if (CONFIG_PREVENT_LOOP) {
                int index = ntab_.indexOf(new NeighborEntry(ipmsg.getLocation(), ipmsg.getDst(), null));
                
                if (index > 0) ne = (NeighborEntry)ntab_.get(index);
                // use destination location here...
                else ne = ntab_.ent_findshortest(ipmsg.getLocation());
            }
            else ne = ntab_.ent_findshortest(ipmsg.getLocation());
        if (ne != null){
            if (ipmsg.isFrozen())
            {
                ipmsg = ipmsg.copy();
                
            }
            ipmsg.setNextHop(new NetMessage.IpOptionNextHop(ne.address));
            nextHop = ne.macAddr;
        }
        else {
            Integer closer = null;
            
            if (use_peri_ && use_planar_) {
                // no proactive probes, so no peri_proact_ to worry about
                ne = ntab_.ent_findface(ipmsg.getLocation(), use_planar_);
                // T-ODO  is it ok that this is returning the node that it was just forwarded from?
                if (ne==null) {
                    // no face toward the destination
                    
                    // the following line seems necessary because there is something wrong
                    // when calling forwardPacket recursively from lost_link. I suppose it 
                    // has something to do with unsynchronized access to the IFQ, or that 
                    // messages are not properly being removed from the IFQ.
                    gpsrh.hops_.add(0, new PerimeterEntry(getCurrentLocation(), netAddr, this.macAddr));
                    drop(p, DROP_RTR_NO_ROUTE);
                    return;
                }
                // put packet in peri data mode, forward
                gpsrh.mode_ = GPSRMessage.GPSRH_DATA_PERI;
                // mark point of entry into peri data mode
                gpsrh.peript_.loc = getCurrentLocation();
                // mark ips of edge endpoints
                gpsrh.periptip_[0] = ne.address; // prev edge on peri
                gpsrh.periptip_[1] = netAddr; // myself
                gpsrh.periptip_[2] = ne.address; // next edge on peri
                /* N.B. first dst hop is hops_[1]
                 (leave room for hop-by-hop ip, position in hops_[0])!! */
                gpsrh.nhops_ = 1;
                gpsrh.currhop_ = 1;
                gpsrh.resetHops();                
                gpsrh.add_hop(netAddr, gpsrh.peript_.loc, macAddr);
                if (ipmsg.isFrozen())
                {
                    ipmsg = ipmsg.copy();
                    
                }
                ipmsg.setNextHop(new NetMessage.IpOptionNextHop(ne.address));
                nextHop = ne.macAddr;
                // add 20 bytes (pos, 3 X edge endpt ips) of entry point to peri mode
                /* subtract 12 bytes: if use_implicit_beacon, hop-by-hop pos/ip
                 already accounted for in size; if not, don't count hops_[0]! */
                //cmh.size() += gpsrh.size() + 20 - 12;
            }
            else if (use_peri_ && peri_proact_) {
                // record we had a data packet that needed a perimeter
                if (lastperi_timer_ != null)
                    lastperi_timer_.resched(lpexp_);
                // but wait! maybe we can get closer along a perimeter.
                ne = ntab_.ent_findcloser_onperi(ipmsg.getLocation(),
                        closer);
                if (ne==null) {
                    // we're well and truly hung; nothing closer on a peri, either
                    if (drop_debug_ /*&& (cmh.opt_num_forwards_ != 16777215)*/) {
                        Iterator ni = ntab_.iterator();
                        while (ni.hasNext()) {
                            logne = (NeighborEntry)ni.next();
                            
                            //    							trace("VPER _%d_ (%.5f, %.5f):",
                            //    									logne.dst, logne.x, logne.y);
                            //    							for (int j = 0; j < logne.perilen; j++) {
                            //    								trace("VPER\t\t_%d_ (%.5f, %.5f)",
                            //    										logne.peri[j].ip, logne.peri[j].x, logne.peri[j].y);
                            //    							}
                        }
                    }
                    drop(p, DROP_RTR_NO_ROUTE);
                    return;
                }
                else 
                {
                    // put packet in peri mode
                    gpsrh.mode_ = GPSRMessage.GPSRH_DATA_PERI;
                    /* N.B. first dst hop is hops_[1]
                     (leave room for hop-by-hop ip, position in hops_[0])!! */
                    gpsrh.nhops_ = 1;
                    gpsrh.resetHops();
                    gpsrh.currhop_ = 1;
                    // add gpsrh with *shorter* path to closer node
                    int i;
                    PerimeterEntry pe;
                    if (closer.intValue() < (ne.perilen / 2)) {
                        for (i = 0; i <= closer.intValue(); i++)
                        {
                            pe = (PerimeterEntry)ne.peri.get(i);
                            gpsrh.add_hop(pe.address, pe.loc, pe.macAddr);
                        }
                    }
                    else {
                        for (i = ne.perilen-1; i >= closer.intValue(); i--)
                        {
                            pe = (PerimeterEntry)ne.peri.get(i);
                            gpsrh.add_hop(pe.address, pe.loc, pe.macAddr);
                        }
                    }
                    if (ipmsg.isFrozen())
                    {
                        ipmsg = ipmsg.copy();
                        
                    }
                    ipmsg.setNextHop(new NetMessage.IpOptionNextHop(
                            ((PerimeterEntry)gpsrh.hops_.get(1)).address));
                    nextHop = ((PerimeterEntry)gpsrh.hops_.get(1)).macAddr;
                    
                    /* subtract 12 bytes: if use_implicit_beacon, hop-by-hop pos/ip
                     already accounted for in size; if not, don't count hops_[0]! */
                    //cmh.size() += gpsrh.size() - 12;
                }
            }
            else {
                // no closer neighbor! unforwardable; drop it.
                /* T-ODO someday, may want to queue up packets for currently unforwardable
                 destinations */
                // record we had a data packet that needed a perimeter
                if (lastperi_timer_ != null)
                    lastperi_timer_.resched(lpexp_);
                // we could have used a perimeter here--turn them on
                peri_proact_ = true;
                drop(p, DROP_RTR_NO_ROUTE); 
                return;
            }
        }
        break;
        case GPSRMessage.GPSRH_DATA_PERI:

            if (use_peri_) {
                if (use_planar_) {
                    Location closer = null;
                    // non-source-routed perimeter forwarding rule
                    /* to resume greedy forwarding, this *node* must be closer than
                     the point where the packet entered peri mode. */                       
                    if (getCurrentLocation().distance(ipmsg.getLocation()) <
                            gpsrh.peript_.loc.distance(ipmsg.getLocation())) {
                        gpsrh.mode_ = GPSRMessage.GPSRH_DATA_GREEDY;
                        /* always add back (- - is +) 12 bytes: if use_implicit_beacon_,
                         src added 12 to size, don't re-add hops_[0]; otherwise,
                         still don't want to count hops_[0]. */
                        //cmh.size() -= gpsrh.size() + 20 - 12;
                        gpsrh.currhop_ = 0;
                        gpsrh.nhops_ = 0;
                        gpsrh.resetHops();
                        // recursive, but must call target_.recv in callee frame
                        
                        // drc: this should be ok since i've been modifying references 
                        // from the QueuedMessage
                        
                        forwardPacket(ipmsg, false);
                        return;
                    }
                    // forward along current face, or change faces where appropriate
                    /* don't choose *any* edge--only consider edges on the
                     face we're forwarding on at the moment. */
                    ne = ntab_.ent_finddst(((PerimeterEntry)gpsrh.hops_.get(gpsrh.hops_.size()-1)).address);
                    
                    if (ne!=null) {
                        //pastHere = true;
                        ne = ntab_.ent_next_ccw(ne, use_planar_);
                        
                        /* drop if we've looped on this perimeter:
                         are about to revisit the first edge we took on it */
                        if ((gpsrh.periptip_[1].equals(netAddr)) &&
                                (gpsrh.periptip_[2].equals(ne.address))) {
                            drop(p, DROP_RTR_NO_ROUTE);
                            return;
                        }
                        if (use_loop_detect_) {
                            /* if we've looped at a point *other* than the first hop on
                             the perimeter, clear the packet's state (other than peript,
                             which is still the "point to beat"), and forward it along the
                             appropriate face. */
                            int j;
                            
                            for (j = 1; j < gpsrh.hops_.size()-1 && !escape; j++) {
                                if ((((PerimeterEntry)gpsrh.hops_.get(j)).address.equals(netAddr)) &&
                                        (((PerimeterEntry)gpsrh.hops_.get(j+1)).address.equals(ne.address))) {
                                    ne = ntab_.ent_findface(ipmsg.getLocation(), use_planar_);
                                    if (ne == null) {
                                        // no face toward the destination
                                        drop(p, DROP_RTR_NO_ROUTE);
                                        return;
                                    }
                                    //cmh.size() -= gpsrh.size() + 20 - 12;
                                    gpsrh.periptip_[0] = ne.address; // prev edge on peri
                                    gpsrh.periptip_[1] = netAddr; // myself
                                    gpsrh.periptip_[2] = ne.address; // next edge on peri
                                    gpsrh.nhops_ = 1;
                                    gpsrh.currhop_ = 1;
                                    gpsrh.resetHops();
                                    gpsrh.add_hop(netAddr, getCurrentLocation(), macAddr);
                                    if (ipmsg.isFrozen())
                                    {
                                        ipmsg = ipmsg.copy();
                                        
                                    }
                                    
                                    ipmsg.setNextHop(new NetMessage.IpOptionNextHop(ne.address));    				    			
                                    nextHop = ne.macAddr;
                                    /* add 20 bytes (pos, 3 X edge endpt ips) of entry point to
                                     peri mode */
                                    /* subtract 12 bytes: if use_implicit_beacon, hop-by-hop pos/ip
                                     already accounted for in size; if not, don't count
                                     hops_[0]! */
                                    //cmh.size() += gpsrh.size() + 20 - 12;
                                    
                                    // drc: removed "goto finish_pkt" here.
                                    escape = true;
                                    break;
                                }
                            }
                            
                        }
                        if (escape) break;
                        
                        // does the candidate next edge have a closer pt?
                        if (ne.closer_pt(netAddr, getCurrentLocation(),
                                gpsrh.peript_.loc, gpsrh.periptip_[1], gpsrh.periptip_[0],
                                ipmsg.getLocation(), closer)!=null) {
                            
                            /* yes. choose a new next hop on the peri pierced by the line
                             to the destination. */
                            /* several neighboring edges may be cut by line to destination;
                             choose that cut at the point closest to destination */
                            closer = ne.closer_pt(netAddr, getCurrentLocation(),
                                    gpsrh.peript_.loc, gpsrh.periptip_[1], gpsrh.periptip_[0],
                                    ipmsg.getLocation(), closer);
                            while (closer!=null) {
                                // fake that ingress edge was edge from ne
                                if (use_loop_detect_) {
                                    // clear all old hops, record new first one
                                    //cmh.size() -= gpsrh.size() + 20 - 12;
                                    gpsrh.nhops_ = 1;
                                    gpsrh.currhop_ = 1;
                                    gpsrh.hops_ .removeAllElements();
                                    gpsrh.add_hop(ne.address, ne.loc, ne.macAddr);
                                    gpsrh.hops_.set(0, new PerimeterEntry(getCurrentLocation(), netAddr, this.macAddr));

                                    //cmh.size() += gpsrh.size() + 20 - 12;
                                }
                                else {
                                    // re-use single-hop history
                                    ((PerimeterEntry)gpsrh.hops_.get(gpsrh.hops_.size()-1)).address = ne.address;
                                    ((PerimeterEntry)gpsrh.hops_.get(gpsrh.hops_.size()-1)).loc = ne.loc;
                                }
                                // record closest point on edge to ne
                                gpsrh.peript_.loc = closer;
                                ne = ntab_.ent_next_ccw(ne, use_planar_);
                                
                                closer = ne.closer_pt(netAddr, getCurrentLocation(),
                                        gpsrh.peript_.loc, gpsrh.periptip_[1], gpsrh.periptip_[0],
                                        ipmsg.getLocation(), closer);
                            }
                            // record edge endpt ips
                            gpsrh.periptip_[0] = ne.address; // prev hop
                            gpsrh.periptip_[1] = netAddr; // self
                            gpsrh.periptip_[2] = ne.address; // next hop
                           
                            forwardPacket(ipmsg, false);
                            return;
                        }
                    }
                    // forward to next ccw neighbor from ingress edge
                    /* in theory, a data peri packet received from an unknown neighbor
                     should serve as a beacon from that neighbor... */
                    /* BUT, don't add the previous hop more than once when we retransmit a
                     packet--the prev hop information is stale in that case */
                    if (!rtxflag && (ne == null)) {
                        PerimeterEntry pe = (PerimeterEntry)gpsrh.hops_.get(gpsrh.hops_.size()-1);
                        NeighborEntry nne = new NeighborEntry(pe.loc, pe.address, pe.macAddr);
                        
                        ne = ntab_.ent_add(nne);
                        if (ne!= null) ne.dnt.sched(bexp_);
                        else {
                            drop(p, DROP_RTR_NO_ROUTE); 
                            return;
                        }
                        if (CONFIG_USE_SPEED && gpsrh.bearing != null)
                        {
                            ne.speed = gpsrh.speed;
                            ne.bearing = gpsrh.bearing;
                        }
                        // now that we have state for the ingress edge, try again...
                        forwardPacket(ipmsg, false);
                        return;
                    }
                    else if (ne == null) {
                        // X-XX might we now be able to forward anyway?? know loc of prev hop.
                        /* we're trying to retransmit a packet, but the ingress hop is
                         gone. drop it. */
                        drop(p, DROP_RTR_MAC_CALLBACK);
                        return;
                    }
                    if (ipmsg.isFrozen())
                    {
                        ipmsg = ipmsg.copy();
                        
                    }
                    ipmsg.setNextHop(new NetMessage.IpOptionNextHop(ne.address));
                    
                    if (ipmsg.getNextHop().equals(oldNextHop))
                    {
                        System.err.println("6: Problem here!");    		
                    }
                    
                    nextHop = ne.macAddr;
                    if (use_loop_detect_) {
                        gpsrh.add_hop(netAddr, getCurrentLocation(), this.macAddr);
                        //cmh.size() += 12;
                    }
                    else {
                        ((PerimeterEntry)gpsrh.hops_.get(gpsrh.hops_.size()-1)).address = netAddr;
                        ((PerimeterEntry)gpsrh.hops_.get(gpsrh.hops_.size()-1)).loc = getCurrentLocation();
                    }
                }
                else {
                    // am I the right waypoint?
                    if (((PerimeterEntry)gpsrh.hops_.get(gpsrh.currhop_)).address.equals(netAddr)) {
                        // am I the final waypoint?
                        if (gpsrh.currhop_ == (gpsrh.hops_.size()-1)) {
                            // yes! return packet to greedy mode
                            gpsrh.mode_ = GPSRMessage.GPSRH_DATA_GREEDY;
                            //cmh.size() -= gpsrh.size() - 12;
                            gpsrh.currhop_ = 0;
                            gpsrh.nhops_ = 0;
                            gpsrh.resetHops();
                            // recursive, but must either drop or target_.recv in callee frame
    
                            forwardPacket(ipmsg, false);
                            return;
                        }
                        else {
                            // forward using source route...
                            gpsrh.currhop_++;
                            if (ipmsg.isFrozen())
                            {
                                ipmsg = ipmsg.copy();
                                
                            }
                            ipmsg.setNextHop( new NetMessage.IpOptionNextHop(
                                    ((PerimeterEntry)gpsrh.hops_.get(gpsrh.currhop_)).address));    		    			
                            nextHop = ((PerimeterEntry)gpsrh.hops_.get(gpsrh.currhop_)).macAddr;
                        }
                    }
                    else {
                        // topology must have changed; I'm not the right hop
                        drop(p, DROP_RTR_NO_ROUTE);
                        return;
                    }
                }
            }
            else {
                throw new RuntimeException(
                "yow! got peri mode packet when not using perimeters!\n");
                
            }
        break;
        default:
            throw new RuntimeException( "yow! got non-data packet in forward_packet()!\n");
        
        //break; // unreachable code
        }
        //finish_pkt:
        // pass along
        //    	cmh.addr_type_ = AF_INET;
        //    	cmh.xmit_failure_ = mac_callback;
        //    	cmh.xmit_failure_data_ = this;
        //    	// point the packet *down* the stack
        //    	cmh.direction_ = -1;
        //    	// data packet can serve as implicit beacon; put self in hops_[0]
        //    	double myx, myy, myz;
        //    	mn_.getLoc(&myx, &myy, &myz);
        /* the packet may *already* have hops stored; don't allocate with
         add_hop()! */
        
        if (gpsrh.hops_.size()==0){
        	gpsrh.add_hop(netAddr, getCurrentLocation(), this.macAddr);
        }
        // setting hop by hop value
        gpsrh.hops_.set(0, new PerimeterEntry(getCurrentLocation(), netAddr, this.macAddr));
        
        //    	if (!netAddr.equals(ipmsg.getSrc())) stats.numMessagesForwarded++;
        if (verbose_){
            //    		trace ("VFP %.5f _%d . %d_ %d:%d . %d:%d", now, mn_.address(),
            //    				ne.dst,
            //					Address::instance().get_nodeaddr(iph.src_),
            //					iph.sport_,
            //					Address::instance().get_nodeaddr(iph.dst_),
            //					iph.dport_);
        }    
        
        /* stats and debugging */
        stats.numMessagesForwarded++;
        if ( (oldNextHop != null && ipmsg.getNextHop()!=null))
        {
            if( oldNextHop.equals(ipmsg.getNextHop())) 
            {
                System.err.println("Didn't set next hop!");
            }
        }
        
        if (gpsrh.hops_.size() == 0){
            System.out.println("What!?");
        }
        
        if (v!=null && CONFIG_USE_VISUALIZATION && v.showCommunication()){
            v.setNodeColor(ipmsg.getNextHop().toInt(), Color.BLUE);
            v.drawAnimatedTransmitCircle(netAddr.toInt(), Color.BLUE);

            if (v.isStep()) v.pause();

        }
        
        if (CONFIG_USE_SPEED)
        {
            setSpeedInfo(gpsrh);
        }
        
//        if (gpsrh.mode_ == GPSRMessage.GPSRH_DATA_PERI){
//        	if (gpsrh.hops_.size()>0 && !((PerimeterEntry)gpsrh.hops_.get(0)).address.equals(netAddr)){
//        		System.out.println("Wtf!");
//        	}
//        }
        
        if (CONFIG_UNICAST){
            self.sendIpMsg(ipmsg, Constants.NET_INTERFACE_DEFAULT, nextHop);
            // the following is ok because if the packet is dropped, we will 
            // be notified...
            ntab_.ent_finddst(ipmsg.getNextHop()).dnt.resched(bexp_);
        }
        else
            self.sendIpMsg(ipmsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
        if (use_implicit_beacon_)
            beacon_resched(false);
        
    }
    
    private void periIn(GPSRMessage m, boolean rtxflag)
    {    	  
        
        NeighborEntry ne =null, inne;
        long time = JistAPI.getTime();
        try {
			m = (GPSRMessage) m.clone();
		} catch (CloneNotSupportedException e) {
			// T-ODO Auto-generated catch block
			e.printStackTrace();
		}
        // update neighbor record for previous hop
        
        // did I originate it?
        if (((PerimeterEntry)m.hops_.get(0)).address.equals(netAddr)) {
            // cache the perimeter
            ne = ntab_.ent_finddst(((PerimeterEntry)m.hops_.get(1)).address);
            
            if (ne == null) {
                // apparently, neighbor we launched probe via is now gone
                //Packet::free(p);
                return;
            }
            
            ne.peri = (Vector)m.hops_.clone();
            ne.peri.remove(0);
            
            ne.perilen = m.hops_.size() - 1;
            
            /* no timer work to do--perimeter probe timer is governed by
             beacons/absence of beacons from a neighbor */
            // we consumed the packet; free it!
            //Packet::free(p);
            return;
        }
        // add self to GPSR header perimeter    	  
        m.add_hop(netAddr, getCurrentLocation(), macAddr);
        // compute candidate next hop: sweep ccw about self from ingress hop
        PerimeterEntry pe = (PerimeterEntry)m.hops_.get(m.hops_.size()-2);
        ne = inne = ntab_.ent_finddst(pe.address);

        /* in theory, a perimeter probe received from an unknown neighbor should
         serve as a beacon from that neighbor... */
        /* BUT, don't add the previous hop more than once when we retransmit a
         peri probe--the prev hop information is stale in that case */
        if (!rtxflag && (ne == null)) {
            NeighborEntry nne = new NeighborEntry(pe.loc, pe.address, pe.macAddr);
            
            inne = ne = ntab_.ent_add(nne);
            if (ne == null) return; // attempting to add self to table
            ne.dnt.sched(bexp_);
            
            if (CONFIG_USE_SPEED && m.bearing != null)
            {
                ne.speed = m.speed;
                ne.bearing = m.bearing;
            }
            // no perimeter probe is pending; launch one
            if (peri_proact_)
                ne.ppt.sched(pint_ +
                        (Constants.random.nextDouble()*(2 * pdesync_ * pint_)) - pdesync_ * pint_);
        }
        else if (ne == null) {
            /* we're trying to retransmit a peri probe, but the ingress hop is gone.
             drop it. */
            //    	    drop(m, DROP_RTR_MAC_CALLBACK);
            return;
        }
        NeighborEntry neOld;
        int i = 0;
        neOld = ne = ntab_.ent_next_ccw(ne, use_planar_);
        while (!ne.equals(inne))
        {
            // verify no crossing
            if (!crosses(ne, m))
                break;
            ne = ntab_.ent_next_ccw(ne, use_planar_);
            
            if (neOld.equals(ne)) {
                return; // couldn't find a place to forward it
            }
            if (ntab_.getNumAlive() <= 1)
            {
                return;
            }
            i++;
            // this is for debugging the infinite loop
            if (i > ntab_.size()*10)
            {
                i++;
            }
        }
        if (CONFIG_USE_SPEED)
        {
            setSpeedInfo(m);
        }
        // forward probe to ne
        NetMessage.Ip newMsg = new NetMessage.Ip(m, netAddr, ne.address,
                Constants.NET_PROTOCOL_GPSR, Constants.NET_PRIORITY_NORMAL, Constants.TTL_DEFAULT);
        newMsg.setNextHop(new NetMessage.IpOptionNextHop(ne.address));
        if (m.mode_ == GPSRMessage.GPSRH_DATA_PERI){
        	if (m.hops_.size()>0 && !((PerimeterEntry)m.hops_.get(0)).address.equals(netAddr)){
        		System.out.println("Wtf!");
        	}
        }
        self.sendIpMsg(newMsg, Constants.NET_INTERFACE_DEFAULT, ne.macAddr);
        if (use_implicit_beacon_)
            beacon_resched(false);
        
    }
    
    /**
     * @param newMsg
     * @param net_interface_default
     * @param any
     */
    public void sendIpMsg(Ip msg, int ni, MacAddress ma) {
        

        
        boolean print = false;
        //transmission delay
        JistAPI.sleep(Util.randomTime(TRANSMISSION_JITTER));
        
        
        // append location of destination
        if (!msg.getDst().equals(NetAddress.ANY))
        {  
            msg.setLocation(new NetMessage.IpOptionLocation(ldb_.getLocation(msg.getDst())));
            
            // debugging code for transmission distance
           /* if (verbose_ && msg.getNextHop()!= null 
                    && !msg.getNextHop().equals(netAddr) 
                    && !ldb_.isLikelyRcv(netAddr, msg.getNextHop(), (float)je.transmitRadius) )
            {
               float dist = getCurrentLocation().distance(ldb_.getLocation(msg.getNextHop()));
                if (minDist > dist) {
                    minDist = dist;
                    print = true;
                }
                if (print) System.err.print("\rWarning: Msg from "+netAddr+" to "+msg.getNextHop()+
                        " is not likely to be received! (dist = "+
                        dist+")");
                
            }*/
        }
        
        GPSRMessage g = (GPSRMessage)msg.getPayload();
        
        // send msg
        netEntity.send(msg, ni, ma);
    }
    
    private boolean crosses(NeighborEntry ne, GPSRMessage gpsrh)
    {
        int i;
        PerimeterEntry pe, pe2, peEnd;
        peEnd = (PerimeterEntry)gpsrh.hops_.get(gpsrh.hops_.size() -1);
        // check all neighboring hops in perimeter thus far (through self)
        for (i = 0; i < (gpsrh.hops_.size() - 1); i++) {
            pe = (PerimeterEntry)gpsrh.hops_.get(i);
            pe2 = (PerimeterEntry)gpsrh.hops_.get(i+1);
            
            if ((pe.address != ne.address) &&
                    (pe2.address != ne.address) &&
                    (pe.address != peEnd.address) &&
                    (pe2.address != peEnd.address) &&
                    cross_segment(pe.loc,
                            pe2.loc,
                            peEnd.loc,
                            ne.loc, null)!=null)
                return true;
        }
        return false;
    }
    private void beacon_proc(NetAddress src, Location loc, MacAddress macAddress, float speed, 
            Location bearing)
    {
    	float dist = selfNotEntity.getCurrentLocation().distance(loc);
    	if (dist > je.transmitRadius){
    		System.err.println("What the fuck!");
    	}
    	
        NeighborEntry ne;
        NeighborEntry nne = new NeighborEntry(loc, src, macAddress);
        
        ne = ntab_.ent_add(nne);
        if (ne == null) return;
        if (ne.live == -1) {
            // entry wasn't in table before. need to planarize, if option dictates.
            ne.live = 1;
            if (use_planar_) {                      
                ntab_.planarize(PLANARIZE_RNG, this.getCurrentLocation());
            }
        }
        ne.dnt.resched(bexp_);
        
        if (CONFIG_USE_SPEED && bearing != null)
        {
            ne.speed = speed;
            ne.bearing = bearing;
        }
        else
        {
            int i = 1; // garbage
        }
        if (use_peri_ && (!ne.ppt.isActive()))
            // no perimeter probe is pending; launch one
            if (peri_proact_)
            {
                ne.ppt.sched(pint_ +
                        (Constants.random.nextDouble()*(2 * pdesync_ * pint_)) - pdesync_ * pint_);
            }
    }
    
    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface#packetDropped(jist.swans.misc.Message, jist.swans.mac.MacAddress)
     */
    public void packetDropped(Message packet, MacAddress packetNextHop) {
        lost_link(packet, packetNextHop);
        
    }
    
    private void drop(Message m, int reason)
    {

        stats.numDropped++;
        
        switch (reason)
        {
        case DROP_RTR_MAC_CALLBACK:
            stats.dropMac++;
            break;            
        case DROP_RTR_NEXT_SRCRT_HOP:
            stats.dropNextSRCRT++;   
            break;
        case DROP_RTR_NO_ROUTE:
            stats.dropNoRoute++;   
            break;
        case DROP_RTR_ROUTE_LOOP:
            stats.dropLoop++;   
            break;
        case DROP_RTR_TTL:
            stats.dropTTL++;   
            break;           
        default: 
            System.out.println("Drop reason: "+reason);
        break;
        }
        
                if (drop_debug_ && m instanceof NetMessage.Ip)
                {
                    NetMessage.Ip ipmsg = (NetMessage.Ip)m;
                    if (v!=null && CONFIG_USE_VISUALIZATION  && v.showCommunication()){
                        v.setNodeColor(ipmsg.getDst().toInt(), Color.YELLOW);
                    }
                    stats.numDataDropped++;
                }
                if (drop_debug_ && m instanceof QueuedMessage)
                {
                    QueuedMessage qm= (QueuedMessage)m;
                    if (v!=null && CONFIG_USE_VISUALIZATION && v.showCommunication()){
                        v.setNodeColor(((NetMessage.Ip)qm.getPayload()).getDst().toInt(), Color.YELLOW);
                    }
                    stats.numDataDropped++;
                }
                
                if (v!=null && CONFIG_USE_VISUALIZATION && v.showCommunication()){
                v.setGeneralPaneText("Dropped: "+stats.numDataDropped+
                        "\n"+"Received: "+stats.arrivedAtDest);
                }
                
        //            if (ipmsg.getPayload() instanceof GPSRMessage)
        //            {
        //                GPSRMessage gm = (GPSRMessage)ipmsg.getPayload();
        //                
        //                if (gm.mode_ == GPSRMessage.GPSRH_DATA_GREEDY ||
        //                        gm.mode_ == GPSRMessage.GPSRH_DATA_PERI)
        //                {
        //                    System.out.print("Dropped data packet due to: ");
        //                    
        //                    switch (reason)
        //                    {
        //                    case DROP_RTR_MAC_CALLBACK:
        //                        System.out.println("MAC layer drop!");
        //                        break;            
        //                    case DROP_RTR_NEXT_SRCRT_HOP:
        //                        System.out.println("SRCRT Hop!");   
        //                        break;
        //                    case DROP_RTR_NO_ROUTE:
        //                        System.out.println("No route!");
        //                        break;
        //                    case DROP_RTR_ROUTE_LOOP:
        //                        System.out.println("Route loop!");
        //                        break;
        //                    case DROP_RTR_TTL:
        //                        System.out.println("TTL expiration!");
        //                        break;           
        //                    default: 
        //                        
        //                        break;
        //                    }
        //                }
        //            }
        //        }       
        
        if (drop_debug_ && verbose_){
            System.out.print("At "+ JistAPI.getTime()+":Dropped data packet due to: ");
            
            switch (reason)
            {
            case DROP_RTR_MAC_CALLBACK:
                System.out.println("MAC layer drop!");
                break;            
            case DROP_RTR_NEXT_SRCRT_HOP:
                System.out.println("SRCRT Hop!");   
                break;
            case DROP_RTR_NO_ROUTE:
                System.out.println("No route!");
                break;
            case DROP_RTR_ROUTE_LOOP:
                System.out.println("Route loop!");
                break;
            case DROP_RTR_TTL:
                System.out.println("TTL expiration!");
                break;           
            default: 
                
                break;
            }
        }
    }
    
    /**
     * @param proxy
     */
    public void setNetEntity(NetInterface proxy) {
        this.netEntity = proxy;      
    }
    
    /**
     * @return
     */
    public RouteInterface.Geo getProxy() {       
        return self;
    }
    
    /**
     * @return
     */
    public NetAddress getLocalAddr() {
        return netAddr;
    }
    
    /**
     * @param gpsrStats
     */
    public void setStats(jist.swans.route.RouteGPSR.GPSRStats gpsrStats) {
        stats = gpsrStats;
        RouteGPSR.stats = stats;
        
    }
    
    public void callback(int id, Location loc, NetAddress na, NS2Timer t, int timerId)
    {
        if (t.performCallback(timerId))
        {
            switch (id)
            {
            case CALLBACK_BEACON:
                beacon_callback();
                break;
            case CALLBACK_DN:
                deadneighb_callback(loc, na);
                break;
            case CALLBACK_LAST_PERI:
                lastperi_callback();
                break;
            case CALLBACK_PERI_PROBE:
                int index = ntab_.indexOf(new NeighborEntry(loc, na, null));
                if (index > -1)
                    periprobe_callback((NeighborEntry)ntab_.get(index));
                break;
            case CALLBACK_PLANAR:
                planar_callback();
                break;
            default:
                break;
            
            }
        }
        
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        // T-ODO add more info
        return netAddr.toString();
    }

	public int getProtocolId() throws Continuation {
		return Constants.NET_PROTOCOL_GPSR;
	}
}

