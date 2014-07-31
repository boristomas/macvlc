/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         RouteDSR_NS2.java
 * RCS:          $Id: RouteDsr_Ns2.java,v 1.1 2007/04/09 18:49:29 drchoffnes Exp $
 * Description:  RouteDSR_NS2 class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      May 5, 2006 at 2:57:50 PM
 * Language:     Java
 * Package:      jist.swans.route
 * Status:       Alpha Release
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.Constants;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacInterface;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Pickle;
import jist.swans.misc.Util.Int;
import jist.swans.net.MessageQueue;
import jist.swans.net.NetAddress;
import jist.swans.net.NetAddressIpFactory;
import jist.swans.net.NetInterface;
import jist.swans.net.NetInterface.NetHandler;
import jist.swans.net.NetMessage;
import jist.swans.net.NetMessage.Ip;
import jist.swans.net.NetMessage.IpOptionNextHop;
import jist.swans.net.QueuedMessage;
import jist.swans.radio.RadioNoise;
import jist.swans.route.RouteDsrMsg_Ns2.SRPacket;
import jist.swans.route.RouteDsrMsg_Ns2.flow_error;
import jist.swans.route.RouteDsrMsg_Ns2.link_down;
import jist.swans.route.RouteDsrMsg_Ns2.sr_addr;
import jist.swans.route.RouteDsr_Ns2.ID.ID_Type;
import jist.swans.route.RouteDsr_Ns2.ID.Log_Status;
import jist.swans.trans.TransUdp;
import driver.JistExperiment;
import driver.Visualizer;
import driver.VisualizerInterface;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The RouteDSR_NS2 class contains a direct port of the ns-2 
 * implementation of the DSR routing protocol. As much of the 
 * original code as possible was left intact.
 */
public class RouteDsr_Ns2 implements RouteInterface.Dsr_NS2 {
    

    /*
     * dsragent.cc
     * Copyright (C) 2000 by the University of Southern California
     * $Id: RouteDsr_Ns2.java,v 1.1 2007/04/09 18:49:29 drchoffnes Exp $
     *
     * This program is free software; you can redistribute it and/or
     * modify it under the terms of the GNU General Public License,
     * version 2, as published by the Free Software Foundation.
     *
     * This program is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     * GNU General Public License for more details.
     *
     * You should have received a copy of the GNU General Public License along
     * with this program; if not, write to the Free Software Foundation, Inc.,
     * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
     *
     *
     * The copyright of this module includes the following
     * linking-with-specific-other-licenses addition:
     *
     * In addition, as a special exception, the copyright holders of
     * this module give you permission to combine (via static or
     * dynamic linking) this module with free software programs or
     * libraries that are released under the GNU LGPL and with code
     * included in the standard release of ns-2 under the Apache 2.0
     * license or under otherwise-compatible licenses with advertising
     * requirements (or modified versions of such code, with unchanged
     * license).  You may copy and distribute such a system following the
     * terms of the GNU GPL for this module and the licenses of the
     * other code concerned, provided that you include the source code of
     * that other code when and as the GNU GPL requires distribution of
     * source code.
     *
     * Note that people who make modified versions of this module
     * are not obligated to grant this special exception for their
     * modified versions; it is their choice whether to do so.  The GNU
     * General Public License gives permission to release a modified
     * version without this exception; this exception also makes it
     * possible to release a modified version which carries forward this
     * exception.
     *
     */
    //
//     Other copyrights might apply to parts of this software and are so
//     noted when applicable.
    //
    /* 
       dsragent.cc

       requires a radio model such that sendPacket returns true
       iff the packet is recieved by the destination node.
       
       Ported from CMU/Monarch's code, appropriate copyright applies.  
    */


//
//
//    /*==============================================================
//      Declarations and global defintions
//    ------------------------------------------------------------*/
////     #define NEW_IFQ_LOGIC
////     #define NEW_REQUEST_LOGIC
//    #define NEW_SALVAGE_LOGIC
//
//    //#ifdef NEW_SALVAGE_LOGIC
    

    static final double BUFFER_CHECK = 0.03 * Constants.SECOND;   // seconds between buffer checks
    static final double RREQ_JITTER = 0.010 * Constants.SECOND;   // seconds to jitter broadcast route requests
    static final double SEND_TIMEOUT = 30.0 * Constants.SECOND;   // # seconds a packet can live in sendbuf
    static final int SEND_BUF_SIZE = 64;
    static final int RTREP_HOLDOFF_SIZE = 10;
    
    public static ID invalid_addr;
    
    public static int FLOW_TABLE_SIZE  =  3000;
    public static int ARS_TABLE_SIZE   =      5;

    
    /*
     *  Maximum number of times that a packet may be salvaged.
     */
    static int dsr_salvage_max_attempts = 15;
    /*
     *  Maximum number of Route Requests that can be sent for a salvaged
     *  packets that was originated at another node.
     */
    static int dsr_salvage_max_requests = 1;
    /*
     *  May an intermediate node send a propagating Route Request for
     *  a salvaged packet that was originated elsewhere.
     */
    static boolean dsr_salvage_allow_propagating = false;
    public static RouteDsr_Ns2 selfNotEntity;
    public static HashMap pendingTimers = new HashMap();
    private MessageQueue ifq;
    private HashSet ipIds = new HashSet();

    //#endif

    /* couple of flowstate constants... */
    static final boolean dsragent_enable_flowstate = false;
    static final boolean dsragent_prefer_default_flow = true;
    static final boolean dsragent_prefer_shorter_over_default = true;
    static final boolean dsragent_always_reestablish = true;
    static final int min_adv_interval = 5;
    static final int default_flow_timeout = 60;
//    #define DSRFLOW_VERBOSE

    static final boolean verbose = false;
    static final boolean verbose_srr = false;
    static final boolean verbose_ssalv = true;
    
    public static final int CALLBACK_SBT = 1;
    private static final boolean DEBUG = false;
    
    private static final int DROP_RTR_QTIMEOUT = 1;
    private static final int DROP_RTR_TTL = 2;
    private static final int DROP_RTR_ROUTE_LOOP = 3;
    private static final int DROP_RTR_NO_ROUTE = 4;
    private static final int DROP_RTR_SALVAGE = 5;
    private static final int DROP_RTR_MAC_CALLBACK = 6;
    private static final int DROP_IFQ_QFULL = 7;
    private static final int NS_AF_INET = 0;
    
    private static final int IP_BROADCAST = NetAddress.ANY.toInt();
    private static final NetAddress GRAT_ROUTE_ERROR = NetAddressIpFactory.getAddress(0);
    
    private static final boolean NEW_SALVAGE_LOGIC = true;
    private static final boolean NEW_REQUEST_LOGIC = false;
    private static final boolean NEW_IFQ_LOGIC = true;





//    DSRAgent_List agthead = { 0 };

    long arp_timeout = 30 * Constants.MILLI_SECOND; // (sec) arp request timeout
    static long rt_rq_period = 500 *Constants.MILLI_SECOND;    // (sec) length of one backoff period
    long rt_rq_max_period = 10 * Constants.SECOND;   // (sec) maximum time between rt reqs
    long send_timeout = (long) (SEND_TIMEOUT * Constants.SECOND); // (sec) how long a packet can live in sendbuf
    static long route_entry_timeout = 5 * Constants.SECOND; // drc : made this up
//    if( DSR_FILTER_TAP){
//        long dsr_tap = 0;
//        long  dsr_tap_skip = 0;
//    }


    long grat_hold_down_time = 1 * Constants.SECOND; // (sec) min time between grat replies for
                    // same route

    long max_err_hold = 1 * Constants.SECOND;        // (sec) 
//     maximum time between when we recv a route error told to us, and when we
//     transmit a propagating route request that can carry that data.  used to
//     keep us from propagating stale route error data


    /*************** selectors ******************/
    boolean dsragent_snoop_forwarded_errors = true;
//     give errors we forward to our cache?
    boolean dsragent_snoop_source_routes = true;
//     should we snoop on any source routes we see?
    boolean dsragent_reply_only_to_first_rtreq = false;
//     should we only respond to the first route request we receive from a host?
    boolean dsragent_propagate_last_error = true;
//     should we take the data from the last route error msg sent to us
//     and propagate it around on the next propagating route request we do?
//     this is aka grat route error propagation
    boolean dsragent_send_grat_replies = true; 
//     should we send gratuitous replies to effect route shortening?
    boolean dsragent_salvage_with_cache = true;
//     should we consult our cache for a route if we get a xmitfailure
//     and salvage the packet using the route if possible
    boolean dsragent_use_tap = true;
//     should we listen to a promiscuous tap?
    boolean dsragent_reply_from_cache_on_propagating = false;
//     should we consult the route cache before propagating rt req's and
//     answer if possible?
    boolean dsragent_ring_zero_search = true;
//     should we send a non-propagating route request as the first action
//     in each route discovery action?

//     NOTE: to completely turn off replying from cache, you should
//     set both dsragent_ring_zero_search and 
//     dsragent_reply_from_cache_on_propagating to false

    boolean dsragent_dont_salvage_bad_replies = true;
//     if we have an xmit failure on a packet, and the packet contains a 
//     route reply, should we scan the reply to see if contains the dead link?
//     if it does, we won't salvage the packet unless there's something aside
//     from a reply in it (in which case we salvage, but cut out the rt reply)
    boolean dsragent_require_bi_routes = true;
//     do we need to have bidirectional source routes? 
//     [X-XX this flag doesn't control all the behaviors and code that assume
//     bidirectional links -dam 5/14/98]

/*
    boolean lsnode_holdoff_rt_reply = true;
//     if we have a cached route to reply to route_request with, should we
//     hold off and not send it for a while?
    boolean lsnode_require_use = true;
//     do we require ourselves to hear a route requestor use a route
//     before we withold our route, or is merely hearing another (better)
//     route reply enough?
*/

    /******** internal state ********/
    RequestTable request_table;
    RouteCache route_cache;
    SendBufEntry send_buf[] = new SendBufEntry[SEND_BUF_SIZE];
    SendBufferTimer send_buf_timer;
    int route_request_num;  // number for our next route_request
    int num_heldoff_rt_replies;
    RtRepHoldoff rtrep_holdoff[] = new RtRepHoldoff[RTREP_HOLDOFF_SIZE]; // not used 1/27/98
    GratReplyHoldDown grat_hold[] = new GratReplyHoldDown[RTREP_HOLDOFF_SIZE];
    int grat_hold_victim;

    /* for flow state ych 5/2/01 */
    FlowTable flow_table;
    ARSTable  ars_table;

    ID net_id, MAC_id;      // our IP addr and MAC addr
    boolean route_error_held; // are we holding a rt err to propagate?
    ID err_from, err_to;     // data from the last route err sent to us 
    long route_error_data_time; // time err data was filled in
    Random random = JistExperiment.getJistExperiment().random;
    /*


    Our strategy is as follows:

     - it's only worth discovering bidirectional routes, since all data
     paths will have to have to be bidirectional for 802.11 ACKs to work

     - reply to all route requests for us that we recv (not just the first one)
     but reply to them by reversing the route and unicasting.  don't do
     a route request (since that will end up returning the requestor lots of
     routes that are potentially unidirectional). By reversing the discovered 
     route for the route reply, only routes that are bidirectional will make it
     back the original requestor

     - once a packet goes into the sendbuffer, it can't be piggybacked on a 
     route request.  the code assumes that the only thing that removes
     packets from the send buff is the StickPktIn routine, or the route reply
     arrives routine

    */
    public Dsr_NS2 self;
    private NetInterface netEntity;
    private MacInterface mac_;
    private RadioNoise radio;
	private NetAddress localAddr;
	private static int receivedDataAtDest = 0;

    /*===========================================================================
      SendBuf management and helpers
    ---------------------------------------------------------------------------*/
    
    public class SendBufferTimer extends NS2Timer
    {
        private Location loc;
        private NetAddress addr;
        private long creationTime; 
        
        public SendBufferTimer(RouteDsr_Ns2 self, long timeout)
        {
            super(self, pendingTimers);
            this.timeoutInterval = timeout;       
            creationTime = JistAPI.getTime();
        }
        
        /* (non-Javadoc)
         * @see jist.swans.route.RouteGPSR_Old.NS2Timer#callback()
         */
        protected void callback(int id) {
            //long currentTime = JistAPI.getTime();
            self.callback(CALLBACK_SBT, loc, addr, this, id);
        }
        
    }
    

    
    /*
     * ========================================================== 
     *     DSR structures
     * ==========================================================
     */
    
    public static class ID implements Pickle.Serializable {
        
    	public final String[] fieldNames = {"addr", "type", "t", "link_type", "log_stat"};
    	
        public static class Link_Type {public static int LT_NONE=0; public static int LT_TESTED=1; public static int LT_UNTESTED=2;};
        public static class Log_Status {public static int LS_NONE=0; public static int LS_UNLOGGED=1; public static int LS_LOGGED=2;};

        public static class ID_Type {public static int NONE=0; public static int MAC=1; public static int IP=2; };
    
        public long addr;
        public int type;

        public long t;         // when was this ID added to the route
        int link_type;
        int log_stat;
       
        
        public ID(){
            type = ID_Type.NONE; 
            t = -1;
            link_type= Link_Type.LT_NONE;
            log_stat = Log_Status.LS_NONE;
        }
        //  ID():addr(0),type(NONE) {}  // remove for speed? -dam 1/23/98
        //ID(long name, ID_Type t):addr(name),type(t), t(-1), link_type(LT_NONE),log_stat(LS_NONE)
        //{
        //assert(type == NONE || type == MAC || type == IP);
        //}
        public ID(long name, int t){
            addr=name;
            type = t; 
            this.t=-1; 
        
          link_type=Link_Type.LT_NONE;
          log_stat=Log_Status.LS_NONE;
        
            assert(type == ID_Type.NONE || type == ID_Type.MAC || type == ID_Type.IP);
        }
        
        public ID( sr_addr a){
            addr=a.addr.toInt(); 
        
          type= a.addr_type;
          t = -1;
          link_type= Link_Type.LT_NONE;
          log_stat = Log_Status.LS_NONE;
        
          assert(type == ID_Type.NONE || type == ID_Type.MAC || type == ID_Type.IP);
        }
         /**
         * @param address
         * @param type2
         */
        public ID(NetAddress address, int type) {
            if (address==null){
                throw new NullPointerException();
            }
           addr = address.toInt();
           this.type = type;
        }
        void fillSRAddr(sr_addr a) {
          a.addr_type = type;
          a.addr = NetAddressIpFactory.getAddress((int)addr);
        }    
         NetAddress getNSAddr_t()  {
          assert(type == ID_Type.IP); return NetAddressIpFactory.getAddress((int)addr);
        }
         boolean equals ( ID id2)  {
          return (type == id2.type) && (addr == id2.addr);
        }
         /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
//        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ID) return this.equals((ID)obj);
            else return false;
        }
        boolean equals (NetAddress na){
             return (na.toInt()==addr) && (type == ID_Type.IP);
         }
         int size()  {return (type == ID_Type.IP ? 4 : 6);}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
//        @Override
        public String toString() {
            return getNSAddr_t().toString();
        } 
         
    }
    
    public static class Path implements Pickle.Serializable {
        /*===========================================================================
        Path methods
      ---------------------------------------------------------------------------*/
      /* rep invariants:
         -1 <= cur_index <= len  (neither bound is really hard)
         0 <= len < RouteDsrMsg_NS2.MAX_SR_LEN
      */
        
    	public final String[] fieldNames = {"len", "cur_index", "path", "path_owner"};
    	
        int len;
        int cur_index;
        ID path[];
        ID path_owner;

      public Path(int route_len,  ID route[])
      {
        path = new ID[RouteDsrMsg_Ns2.MAX_SR_LEN];
        assert(route_len <= RouteDsrMsg_Ns2.MAX_SR_LEN);
        //  route_len = (route == null : 0 ? route_len); 
        // a more cute solution, follow the above with the then clause
        if (route != null)
          {
            for (int c = 0; c < route_len; c++)
              {
          path[c] = route[c];
              }
            len = route_len;
          }
        else
          {
            len = 0;
          }
        cur_index = 0;
      }

      public Path()
      {
        path = new ID[RouteDsrMsg_Ns2.MAX_SR_LEN];
        len = 0;
        cur_index = 0;
      }


      public Path( sr_addr addrs[], int len)
      { /* make a path from the bits of an NS source route header */
        assert(len <= RouteDsrMsg_Ns2.MAX_SR_LEN);
        path = new ID[RouteDsrMsg_Ns2.MAX_SR_LEN];

        for (int i = 0 ; i < len ; i++)            
          path[i] = new ID(addrs[i]);

        this.len = len;
        cur_index = 0;
      }

      public Path(RouteDsrMsg_Ns2 srh)
      { /* make a path from the bits of an NS source route header */
        path = new ID[RouteDsrMsg_Ns2.MAX_SR_LEN];

        if (! srh.valid()) {
            len = 0;
            cur_index = 0;
            return;
        }

        len = srh.num_addrs();
        cur_index = srh.cur_addr();

        assert(len <= RouteDsrMsg_Ns2.MAX_SR_LEN);
        
        for (int i = 0 ; i < len ; i++)
            path[i] = new ID(srh.addrs()[i]);
      }
      
      public boolean full() {return (len >= RouteDsrMsg_Ns2.MAX_SR_LEN);}

      void
      fillSR(RouteDsrMsg_Ns2 srh)
      {
        for (int i = 0 ; i < len ; i++)
          {
            path[i].fillSRAddr(srh.addrs()[i]);
          }
        srh.num_addrs_ = len;
        srh.cur_addr_ = cur_index;
      }

//      Path( Path& old)
//      {
//        path = new ID[RouteDsrMsg_NS2.MAX_SR_LEN];
//        if (old.path != null)
//          {
//            for (int c = 0; c < old.len; c++)
//        path[c] = old.path[c];
//            len = old.len;
//          }
//        else
//          {
//            len = 0;
//          }
//        cur_index = old.cur_index;
//        path_owner = old.path_owner;
//      }

    
      public Path ( Path rhs)
           // makes the lhs a copy of the rhs: lhs may share data with
           // the rhs such that changes to one will be seen by the other
           // use the provided copy operation if you don't want this.
      {
      /* OLD  NOTE:
        we save copying the path by doing a delete[] path; path = rhs.path;
         but then the following code will be fatal (it calls delete[]
         twice on the same address)
           { Path p1();
             { Path p2();
               p2 = p1;
             }
           }
         you'd have to implement reference counts on the path array to
         save copying the path.

         NEW NOTE: we just copy like everything else
      */
        if (!this.equals(rhs))
          {// beware of path = path (see Stroustrup p. 238)
            cur_index = rhs.cur_index;
            path_owner = rhs.path_owner;
            len = rhs.len;
            for (int c = 0 ; c < len ; c++)
        path[c] = rhs.path[c];
          }
        // note: i don't return *this cause I don't think assignments should
        // be expressions (and it has slightly incorrect semantics: (a=b) should
        // have the value of b, not the new value of a)
      }

     public boolean equals( Object rhsObj)
      {
         Path rhs = (Path)rhsObj;
      
        int c;
        if (len != rhs.len) return false;
        for (c = 0; c < len; c++)
          if (path[c] != rhs.path[c]) return false;
        return true;
      }
       
      void 
      appendPath(Path p)
      {
        int i;
        for (i = 0; i < p.length() ; i++)
          {
            path[len] = p.path[i];
            len++;
            if (len > RouteDsrMsg_Ns2.MAX_SR_LEN)
        {
//          System.err.printf("DFU: overflow in appendPath len2 %d\n",
//              p.length());
          len--;
          return;
        }
          }
      }

      /**
     * @return
     */
    int length() {
        return len;
    }

    void 
      removeSection(int from, int to)
        // the elements at indices from -> to-1 are removed from the path
      {
        int i,j;

        if (to <= from) return;
        if (cur_index > from) cur_index = cur_index - (to - from);
        for (i = to, j = 0; i < len ; i++, j++)
          path[from + j] = path[i];
        len = from + j;
      }

      Path copy() 
      {
        Path p = new Path(len,path);
        p.cur_index = cur_index;
        p.path_owner = path_owner;
        return p;
      }

      void
      copyInto(Path to) 
      {
        to.cur_index = cur_index;
        to.len = len;
        if (to.path.length != to.len) to.path = new ID[len];
        for (int c = 0 ; c < len ; c++)
          to.path[c] = path[c];  
        to.path_owner = path_owner;
      }

      Path
      reverse() 
           // return an identical path with the index pointing to the same
           // host, but the path in reverse order
      {
        if (len == 0) return this;
        Path p = new Path();

        int from, to;
        for (from = 0, to = (len-1) ; from < len ; from++,to--)
          p.path[to] = path[from];
        p.len = len;
        p.cur_index = (len - 1) - cur_index;
        return p;
      }

      void
      reverseInPlace()
      {
        if (len == 0) return;
        int fp,bp;     // forward ptr, back ptr
        ID temp;
        for (fp = 0, bp = (len-1) ; fp < bp ; fp++, bp--)
          {
            temp = path[fp];
            path[fp] = path[bp];
            path[bp] = temp;
          }
        cur_index = (len - 1) - cur_index;
      }

      int
      size() 
      {
        // this should be more clever and ask the id's what their sizes are.
        return len*4;
      }

      boolean
      member( ID id) 
//       rtn true iff id is in path
      {
        return member(id, invalid_addr);  
      }

      boolean
      member( ID id,  ID MAC_id) 
//       rtn true iff id or MAC_id is in path
      {
        for (int c = 0; c < len ; c++)
          if (path[c] == id || path[c].equals(MAC_id))
            return true;
        return false;
      }

      /*void
      unparse(FILE *out) 
      {
        // change to put ()'s around the cur_index entry?
        if (len==0)
          {
            fprintf(out,"<empty path>");
            return;
          }
        for (int c = 0 ; c < len-1 ; c ++)
          {
            if (c == cur_index) fprintf(out,"(");
            path[c].unparse(out);
            if (c == cur_index) fprintf(out,")");
            fprintf(out,",");
          }
        if (len-1 == cur_index) fprintf(out,"(");
        path[len-1].unparse(out);
        if (len-1 == cur_index) fprintf(out,")");
      }

      String 
      dump() 
      {
        static int which = 0;
        static char buf[4][100];
        String ptr = buf[which];
        String rtn_buf = ptr;
        which = (which + 1) % 4;
        
        if (len == 0)
          {
            sprintf(rtn_buf,"[<empty path>]");
            return rtn_buf;
          }
        *ptr++ = '[';
        for (int c = 0 ; c < len ; c ++)
          {
            if (c == cur_index) *ptr++ = '(';
            ptr += sprintf(ptr,"%s%s ",path[c].dump(), c == cur_index ? ")" : "");
          }
        *ptr++ = ']';
        *ptr++ = '\0';
        return rtn_buf;
      }*/

      static void
      compressPath(Path path)
//       take a path and remove any double backs from it
//       eg:  A B C B D --> A B D
      {
        // idea: walk one pointer from begining
        //  for each elt1 start at end of path and walk a pointer backwards (elt2)
        //   if forward pointer = backward pointer, go on and walk foward one more
        //   if elt1 = elt2 then append {(elt2 + 1) to end} after forward pointer
        //    update length of path (we just cut out a loopback) and walk forward
        //  when forward walking pointer reaches end of path we're done

        int fp = 0, bp; // the forward walking ptr and the back walking ptr
        while (fp < path.len)
          {
            for (bp = path.len - 1; bp != fp; bp--)
        {
          if (path.path[fp].equals(path.path[bp]))
            { int from, to;
              for (from = bp, to = fp;
               from < path.len ;
               from++, to++)
            path.path[to] = path.path[from];
              path.len = to;
              break;
            } // end of removing double back
        } // end of scaning to check for double back
            fp++; // advance the forward moving pointer
          }
      }

      public static void 
      CopyIntoPath(Path to,  Path from, int start, int stop)
//       sets to[0->(stop-start)] = from[start->stop]
      {
        assert(start >= 0 && stop < from.len);
        int f, t,c ;            // from and to indices
        for(f = start, t = 0; f <= stop; f++, t++)
          to.path[t] = from.path[f];
        if (to.len < stop - start + 1) to.len = stop - start + 1;
        for (c = to.len - 1; c >= 0; c--)
          {
            if (to.path[c] == to.owner()) break;
            if (to.path[c] == ((Path)from).owner()) 
        {
          to.path_owner = ((Path)from).owner();
          break;
        }
          } 
      }

      /**
     * @return
     */
    public ID owner() {
        return path_owner;
    }

    void
      checkpath() 
      {
        for(int c = 0; c < RouteDsrMsg_Ns2.MAX_SR_LEN; c++)
          {     
            assert(path[c].type == ID_Type.NONE ||
                   path[c].type == ID_Type.MAC ||
                   path[c].type == ID_Type.IP);
          }
      }
    public void reset() {len = 0; cur_index = 0;}
    public int index() { return cur_index;}
    public void set_index(int i){ cur_index = i;}
    public void appendToPath( ID id) { 
        assert(len < RouteDsrMsg_Ns2.MAX_SR_LEN); 
        path[len++] = id;}

     void setIterator(int i) {
         
         if (i>=len){
             throw new RuntimeException();
         }
         assert(i>=0 
             && i<len); 

     cur_index = i;}
     void resetIterator() {  cur_index = 0;}


    /**
     * @param i
     * @return
     */
    public ID get(int i) {
        return path[i];
    }

    /**
     * @param who_from
     */
    public void set_owner(ID who_from) {
       path_owner = who_from;
        
    }

    /**
     * @param i
     */
    public void setLength(int i) {
        len = i;
        
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
//    @Override
    public String toString() {
        // T-ODO Auto-generated method stub
        return "len="+len+";cur_index="+cur_index+";path_owner="+path_owner+"\n"+Arrays.toString(path);
    }
    }
    
    
    public static class Cache {
    	
        private  Path cache[];
        private int size;
        private int victim_ptr;       // next victim for eviction
        private  MobiCache routecache;
        private   String name;
        
        public  Cache(String name, int size, MobiCache rtcache){
            this.name = name;
            this.size = size;
            cache = new Path[size];
            routecache = rtcache;
            victim_ptr = 0;
        }
         

          public  int pickVictim(int exclude){
              for(int c = 0; c < size ; c++)
                  if (cache[c].length() == 0) return c;
                
                int victim = victim_ptr;
                while (victim == exclude)
                  {
                    victim_ptr = (victim_ptr+1 == size) ? 0 : victim_ptr+1;
                    victim = victim_ptr;
                  }
                victim_ptr = (victim_ptr+1 == size) ? 0 : victim_ptr+1;

//              #ifdef DSR_CACHE_STATS
//                routecache->checkRoute(&cache[victim], ACTION_CHECK_CACHE, 0);
//                int bad = routecache->checkRoute_logall(&cache[victim], ACTION_EVICT, 0);
//                routecache->trace("SRC %.9f _%s_ evicting %d %d %s",
//                                  Scheduler::instance().clock(), routecache->net_id.dump(),
//                                  cache[victim].length() - 1, bad, name);
//              #endif
                return victim;
          }
          // returns the index of a suitable victim in the cache
          // will spare the life of exclude
          public  boolean searchRoute(final ID dest, Int i, Path path, int index){
              // T-ODO drc: return values
              for (; index < size; index++){
                  if (cache[index]==null) continue;
                  for (int n = 0 ; n < cache[index].length(); n++)
                    if (cache[index].get(n).equals( dest)
                            && cache[index].get(n).t > (JistAPI.getTime()-route_entry_timeout)) // drc added
                {
                  i.set(n);
                  cache[index].copyInto(path);
                  return true;
                }
              }
                return false;
          }
          // look for dest in cache, starting at index, 
          //if found, rtn true with path s.t. cache[index] == path && path[i] == dest
          public  Path addRoute(Path path, Int common_prefix_len){
              // T-ODO drc: return common_prefix_len somehow
              boolean done = false;
              int index, m, n;
              int victim;

              // see if this route is already in the cache
              for (index = 0 ; index < size ; index++)
                { 
                  if (cache[index]==null){
                      cache[index] = new Path();
                      continue;
                  }
                  // for all paths in the cache
                  for (n = 0 ; n < cache[index].length() ; n ++)
                { // for all nodes in the path
                  if (n >= path.length()) break;
                  if (!cache[index].get(n).equals( path.get(n))) break;
                }
                  if (n == cache[index].length()) 
                { // new rt completely contains cache[index] (or cache[index] is empty)
                      common_prefix_len.set(n);
                      for ( ; n < path.length() ; n++)
                        cache[index].appendToPath(path.get(n));
//                  if (verbose_debug)
//                    routecache->trace("SRC %.9f _%s_ %s suffix-rule (len %d/%d) %s",
//                      Scheduler::instance().clock(), routecache->net_id.dump(),
//                          name, n, path.length(), path.dump()); 
                  done = true;
                  break;
                }
                  else if (n == path.length())
                { // new route already contained in the cache
                      common_prefix_len.set(n);
//                  if (verbose_debug)
//                    routecache->trace("SRC %.9f _%s_ %s prefix-rule (len %d/%d) %s",
//                      Scheduler::instance().clock(), routecache->net_id.dump(),
//                      name, n, cache[index].length(), cache[index].dump()); 
                  done = true;
                  break;
                }
                  else 
                { // keep looking at the rest of the cache 
                }
                } 

              if (!done){
              // there are some new goodies in the new route
              victim = pickVictim(-1);
//              if(verbose_debug) {
//                routecache->trace("SRC %.9f _%s_ %s evicting %s",
//                          Scheduler::instance().clock(), routecache->net_id.dump(),
//                          name, cache[victim].dump());  
//                routecache->trace("SRC %.9f _%s_ while adding %s",
//                          Scheduler::instance().clock(), routecache->net_id.dump(),
//                          path.dump()); 
//              }
              cache[victim].reset();
              Path.CopyIntoPath(cache[victim], path, 0, path.length() - 1);
              common_prefix_len.set(0);
              index = victim; // remember which cache line we stuck the path into
              }
            

//            #ifdef DEBUG
//              {
//                Path &p = path;
//                int c;
//                char buf[1000];
//                char *ptr = buf;
//                ptr += sprintf(buf,"Sdebug %.9f _%s_ adding ", 
//                       Scheduler::instance().clock(), routecache->net_id.dump());
//                for (c = 0 ; c < p.length(); c++)
//                  ptr += sprintf(ptr,"%s [%d %.9f] ",p[c].dump(), p[c].link_type, p[c].t);
//                routecache->trace(buf);
//              }
//            #endif //DEBUG

              // freshen all the timestamps on the links in the cache
              for (m = 0 ; m < size ; m++)
                { // for all paths in the cache
//
//            #ifdef DEBUG
//              {
//                if (cache[m].length() == 0) continue;
//
//                Path &p = cache[m];
//                int c;
//                char buf[1000];
//                char *ptr = buf;
//                ptr += sprintf(buf,"Sdebug %.9f _%s_ checking ", 
//                       Scheduler::instance().clock(), routecache->net_id.dump());
//                for (c = 0 ; c < p.length(); c++)
//                  ptr += sprintf(ptr,"%s [%d %.9f] ",p[c].dump(), p[c].link_type, p[c].t);
//                routecache->trace(buf);
//              }
//            #endif //DEBUG
                  
                  for (n = 0 ; n < cache[m].length() - 1 ; n ++)
                { // for all nodes in the path
                  if (n >= path.length() - 1) break;
                  if (!cache[m].get(n).equals(path.get(n))) break;
                  if (cache[m].get(n+1).equals(path.get(n+1)))
                    { // freshen the timestamps and type of the link          

//            #ifdef DEBUG
//            routecache->trace("Sdebug %.9f _%s_ freshening %s->%s to %d %.9f",
//                      Scheduler::instance().clock(), routecache->net_id.dump(),
//                      path.get(n).dump(), path[n+1].dump(), path.get(n).link_type,
//                      path.get(n).t);
//            #endif //DEBUG

                      cache[m].get(n).t = path.get(n).t;
                      cache[m].get(n).link_type = path.get(n).link_type;
                      /* NOTE: we don't check to see if we're turning a TESTED
                     into an UNTESTED link.  Last change made rules -dam 5/19/98 */
                    }
                }
                }
              return cache[index];
          }
          // rtns a pointer the path in the cache that we added
          public  void noticeDeadLink(final  ID from, final  ID to)
          // the link from->to isn't working anymore, purge routes containing
          // it from the cache
          {
              for (int p = 0 ; p < size ; p++)
              { // for all paths in the cache
                  if (cache[p]==null) continue;
                for (int n = 0 ; n < (cache[p].length()-1) ; n ++)
            { // for all nodes in the path
              if (cache[p].get(n).equals(from) && cache[p].get(n+1).equals(to))
                {
//                  if(verbose_debug)
//                routecache->trace("SRC %.9f _%s_ %s truncating %s %s",
//                                            Scheduler::instance().clock(),
//                                            routecache->net_id.dump(),
//                                            name, cache[p].dump(),
//                                            cache[p].owner().dump());
//          #ifdef DSR_CACHE_STATS
//                        routecache->checkRoute(&cache[p], ACTION_CHECK_CACHE, 0);
//                        routecache->checkRoute_logall(&cache[p], ACTION_DEAD_LINK, n);
//          #endif          
                  if (n == 0)
                cache[p].reset();        // kill the whole path
                  else {
                cache[p].setLength(n+1); // truncate the path here
                          cache[p].get(n).log_stat = Log_Status.LS_UNLOGGED;
                        }

//                  if(verbose_debug)
//                routecache->trace("SRC %.9f _%s_ to %s %s",
//                      Scheduler::instance().clock(), routecache->net_id.dump(),
//                      cache[p].dump(), cache[p].owner().dump());

                  break;
                } // end if this is a dead link
            } // end for all nodes
              } // end for all paths
            return;
          }

        
        }
    
    private static class MobiCache extends RouteCache{

        protected Cache primary_cache;   /* routes that we are using, or that we have reason
                         to believe we really want to hold on to */
        protected  Cache secondary_cache; /* routes we've learned via a speculative process
                         that might not pan out */

//          #ifdef DSR_CACHE_STATS
//            void periodic_checkCache(void);
//            void checkRoute(Path *p, int action, int prefix_len);
//            void checkRoute(Path &p, int&, int&, double&, int&, int&, double &);
//          #endif
//          };


        public MobiCache(ID net_id, ID mac_id){
            super();
            this.net_id = net_id;
            MAC_id = mac_id;
            
            primary_cache = new Cache("primary", 30, this);
            secondary_cache = new Cache("secondary", 64, this);
            //secondary_cache = new Cache("secondary", 10000, this);
            assert(primary_cache != null && secondary_cache != null);
        }

        
        /**
         * @param dest
         * @param route
         * @param i
         * @return
         */
        public boolean findRoute(ID dest, Path route, boolean for_me)
//      if there is a cached path from us to dest returns true and fills in
//      the route accordingly. returns false otherwise
//      if for_me, then we assume that the node really wants to keep 
//      the returned route so it will be promoted to primary storage if not there
//      already
     {
       Path path = new Path();
       Path origRoute = route; // for copying into later
       int min_index = -1;
       int min_length = RouteDsrMsg_Ns2.MAX_SR_LEN + 1;
       int min_cache = 0;       // 2 == primary, 1 = secondary
       int index;
       Int len = new Int();

       assert(!(net_id.equals(invalid_addr)));

       index = 0;
       while (primary_cache.searchRoute(dest, len, path, index))
         {
           min_cache = 2;
           if (len.get() < min_length)
        {
          min_length = len.get();
          route = path;
        }
           index++;
         }
       
       index = 0;
       while (secondary_cache.searchRoute(dest, len, path, index))
         {
           if (len.get() < min_length)
        {
          min_index = index;
          min_cache = 1;
          min_length = len.get();
          route = path;
        }
           index++;
         }

       if (min_cache == 1 && for_me)
         { // promote the found route to the primary cache
           Int prefix_len = new Int();
      
           primary_cache.addRoute(secondary_cache.cache[min_index], prefix_len);

           // no need to run checkRoute over the Path* returned from
           // addRoute() because whatever was added was already in
           // the cache.

           //   prefix_len = 0
           //        - victim was selected in primary cache
           //        - data should be "silently" migrated from primary to the
           //          secondary cache
           //   prefix_len > 0
           //        - there were two copies of the first prefix_len routes
           //          in the cache, but after the migration, there will be
           //          only one.
           //        - log the first prefix_len bytes of the secondary cache
           //          entry as "evicted"
           if(prefix_len.get() > 0)
             {
               secondary_cache.cache[min_index].setLength(prefix_len.get());
//     #ifdef DSR_CACHE_STATS
//               checkRoute_logall(&secondary_cache->cache[min_index], 
//                                 ACTION_EVICT, 0);
//     #endif
             }
           secondary_cache.cache[min_index].setLength(0); // kill route
         }

       if (min_cache!=0) 
         {
           route.setLength(min_length + 1);
//           if (verbose_debug)
//        trace("SRC %.9f _%s_ $hit for %s in %s %s",
//              Scheduler::instance().clock(), net_id.dump(),
//              dest.dump(), min_cache == 1 ? "secondary" : "primary",
//              route.dump());    
//     #ifdef DSR_CACHE_STATS
//           int bad = checkRoute_logall(&route, ACTION_FIND_ROUTE, 0);      
//           stat.route_find_count += 1;
//           if (for_me) stat.route_find_for_me += 1;
//           stat.route_find_bad_count += bad ? 1 : 0;
//           stat.subroute_find_count += route.length() - 1;
//           stat.subroute_find_bad_count += bad;
//     #endif
           route.copyInto(origRoute);
           return true;
         }
       else
         {
//           if (verbose_debug)
//             trace("SRC %.9f _%s_ find-route [%d] %s->%s miss %d %.9f",
//                   Scheduler::instance().clock(), net_id.dump(),
//                   0, net_id.dump(), dest.dump(), 0, 0.0);
//     #ifdef DSR_CACHE_STATS
//           stat.route_find_count += 1;
//           if (for_me) stat.route_find_for_me += 1;
//           stat.route_find_miss_count += 1;
//     #endif
           return false;
         }
        }

        /**
         * @param route
         * @param time
         * @param net_id
         */
        public void noticeRouteUsed(Path p, long t, ID who_from) {
            Path stub = new Path();
            if(pre_noticeRouteUsed(p, stub, t, who_from) == 0)
              return;

            Int prefix_len = new Int(0);

//          #ifdef DSR_CACHE_STATS
//            Path *p0 = secondary_cache->addRoute(stub, prefix_len);
//            checkRoute(p0, ACTION_NOTICE_ROUTE, prefix_len);
//          #else
            secondary_cache.addRoute(stub, prefix_len);
//          #endif
            
        }

        /**
         * @param route
         * @param time
         * @param net_id
         */
        public void addRoute(final Path route, long t, final ID who_from)
//      add this route to the cache (presumably we did a route request
//      to find this route and don't want to lose it)
//      who_from is the id of the routes provider
     {
       Path rt = new Path();

       if(pre_addRoute(route, rt, t, who_from) == 0)
         return;

       // must call addRoute before checkRoute
       Int prefix_len = new Int(0);


       primary_cache.addRoute(rt, prefix_len);
     
     
        }

        /**
         * @param id
         * @param id2
         * @param time
         */
        public void noticeDeadLink(ID from, ID to, long time) {
            primary_cache.noticeDeadLink(from, to);
            secondary_cache.noticeDeadLink(from, to);
            return;
            
        }
        
    }
    
    private RouteCache 
    makeRouteCache()
    {
      return new MobiCache(net_id, MAC_id);
    }
    
    public static class LastType { public static int LIMIT0 = 0; public static int UNLIMIT=1;};

    public static class RequestTable extends ArrayList {
 
        int size;
        int ptr;
        
        RequestTable(int s)
        {
          super(s);
          size = s;
        }


        int
        find(final ID net_id, final ID MAC_id) 
        {
            Entry e;
          for (int c = 0 ; c < size() ; c++){
              e = (Entry) get(c);
              if (e!=null && ( (e.net_id!=null && e.net_id.equals(net_id)) || (e.MAC_id!=null && e.MAC_id.equals(MAC_id))))
                  return c;
          }
          return size;
        }

        int
        get(final ID id)
        {
          int existing_entry = find(id, id);

          if (existing_entry >= size)    
            {
              return 0;
            }
          return ((Entry)get(existing_entry)).req_num;
        }


        Entry
        getEntry(final ID id)
        {
          int existing_entry = find(id, id);

          if (existing_entry >= size)    
            {
              Entry e; 
              if (size()==size ) e = (Entry)remove(size()-1);
              else e = new Entry();
              e.MAC_id = invalid_addr;
              e.net_id = id;
              e.req_num = 0;
              e.last_arp = 0;
              e.rt_reqs_outstanding = 0;
              e.last_rt_req = (long) -(rt_rq_period + 1.0*Constants.SECOND);
              existing_entry = 0;
              add(0, e);
            }
          return (jist.swans.route.RouteDsr_Ns2.Entry) (get(existing_entry));
        }

        void
        insert(final ID net_id, int req_num)
        {
          insert(net_id,invalid_addr,req_num);
        }


        void
        insert(final ID net_id, final ID MAC_id, int req_num)
        {
            Entry e;
          int existing_entry = find(net_id, MAC_id);

          if (existing_entry < size)
            {
              e = (Entry) get(existing_entry);
              if (e.MAC_id == invalid_addr)
            e.MAC_id = MAC_id; // handle creations by getEntry
              e.req_num = req_num;
              return;
            }
          if (size()==size) e = (Entry)get(size()-1);
          else{
              e = new Entry();
              add(e);
          }
          // otherwise add it in
          e.MAC_id = MAC_id;
          e.net_id = net_id;
          e.req_num = req_num;
          e.last_arp = 0;
          e.rt_reqs_outstanding = 0;
          e.last_rt_req = (long) -(rt_rq_period + 1.0);
          ptr = (ptr+1)%size;
        }


    }

    public static class Entry {
      ID MAC_id;
      ID net_id;
      int req_num;
      long last_arp;
      int rt_reqs_outstanding;
      long last_rt_req;
      int last_type;
    }
    
    static class ARSTabEnt {
        public int uid;
        public int fid;
        public int hopsEarly; /* 0 overloads as invalid */
      };

      static class ARSTable {
       
          ARSTabEnt table[];
          int victim;
          int size;
          
          public ARSTable(int size_) {
            size = size_;
            victim = 0;
            table = new ARSTabEnt[size_];
//            bzero(table, sizeof(ARSTabEnt)*size_);
          }

//          ARSTable::~ARSTable() {
//            delete table;
//          }

          void insert(int uid, Int fid, int hopsEarly) {
            int i = victim;
            assert(hopsEarly!=0);
            
            do {
                if (table[i].hopsEarly==0)
                    break; // we found a victim
                i = (i+1)%size;
            } while (i != victim);

            if (table[i].hopsEarly > 0) // full. need extreme measures.
                victim = (victim+1)%size;

            table[i].hopsEarly = hopsEarly;
            table[i].uid       = uid;
            table[i].fid       = fid.get();
          }

          int findAndClear(int uid, int fid) {
            int i, retval;

            for (i=0; i<size; i++) {
                    if (table[i].hopsEarly>0 && table[i].uid == uid) {
                        if (table[i].fid == fid) {
                            retval = table[i].hopsEarly;
                        table[i].hopsEarly = 0;
                        return retval;
                    } else {
                        table[i].hopsEarly = 0;
                        return 0;
                    }
                }
            }

            return 0;

      }
      }

      static class DRTabEnt { /* Default Route Table Entry */
        public NetAddress src;
        public NetAddress dst;
        public int fid;
      };

      static class DRTable extends ArrayList {
          int       size;
          int       maxSize;
//          DRTabEnt table[];
        
          DRTable(int size_) {
            super(size_);
              assert (size_ > 0);
            size = 0;
            maxSize = size_;
//            table = new DRTabEnt[size_];
            
          }


          int find(NetAddress src, NetAddress dst, Int flow) {
            for (int i = 0; i < size; i++)
                if (src == ((DRTabEnt)get(i)).src && dst == ((DRTabEnt)get(i)).dst) {
                    flow.set(((DRTabEnt)get(i)).fid);
                    return 1;
                }
            return 0;
          }


          void insert(NetAddress src, NetAddress dst, int flow) {
            assert((flow & 1)!=0);
            for (int i = 0; i < size; i++) {
                if (src == ((DRTabEnt)get(i)).src && dst == ((DRTabEnt)get(i)).dst) {
                    if ((short)((flow) - (((DRTabEnt)get(i)).fid)) > 0) {
                        ((DRTabEnt)get(i)).fid = flow;
                    } else {
                    }
                    return;
                }
            }


            assert(size != maxSize);
            DRTabEnt temp = new DRTabEnt();

            temp.src = src;
            temp.dst = dst;
            temp.fid = flow;
            add(temp);
          }

          void flush(NetAddress src, NetAddress dst) {
              DRTabEnt temp = null;
            for (int i = 0; i < size; i++){
                temp = ((DRTabEnt)get(i));
                if (src == temp.src && dst == temp.dst) {
                    remove(i);
                    return;
                }
            }
//            assert(0);
          }
      }

      static class TableEntry {
          // The following three are a key
          public NetAddress   sourceIP ; // Source IP Addresss
          public NetAddress   destinationIP ;    // Destination IP Addresss
          public int     flowId ;    // 16bit flow id

          // Ugly hack for supporting the "established end-to-end" concept
          public int       count ;     // starts from 0 and when it reaches 
                    // END_TO_END_COUNT it means that the 
                    // flow has been established end to end.

          public NetAddress  nextHop;    // According to the draft, this is a MUST.
                    // Obviously, said info is also in sourceRoute,
                    // but keeping it separate makes my life easier,
                    // and does so for free. -- ych 5/5/01

          long      lastAdvRt;  // Last time this route was "advertised"
                    // advertisements are essentially source routed
                    // packets.

          long      timeout ;   // MUST : Timeout of this flowtable entry
          int       hopCount ;  // MUST : Hop count
          int       expectedTTL ;   // MUST : Expected TTL
          boolean  allowDefault ;  // MUST : If true then this flow
                    // can be used as default if the 
                    // source is this node and the 
                    // flow ID is odd.
                    // Default is 'false' 

          public Path  sourceRoute ;   // SHOULD : The complete source route.
                    // Nodes not keeping complete source 
                    // route information cannot
                    // participate in Automatic Route
                    // Shortening
      };
    
    /**
     * 
     * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
     *
     * The FlowTable class contains TableEntry's.
     */
    private static class FlowTable extends DRTable{

//      end_to_end count - 1, actually, so a misnomer...
        public static int END_TO_END_COUNT  =  2;

        
        /** contains TableEntry's*/
        ArrayList table; // Actual flow state table
        int     size ;      // Number of entries in the table
        int     maxSize ;   // Maximum possible size

        int   counter;    // next flowid to give out
        NetAddress    net_addr;   // for noticeDeadLink()
        DRTable     DRTab;
        
        public FlowTable(int size_) {            
            super(size_);
            assert (size_ > 0);            
            size = 0;
            maxSize = size_;
            table = new ArrayList(size_);
            counter = 0;
            DRTab = new DRTable(size_);
        }


        int find(NetAddress source, NetAddress destination, Int flow) {
            int i;
            TableEntry te;
            for (i=size-1; i>=0; i--){
                te = (TableEntry) table.get(i);
                if (te.sourceIP == source &&
                        te.destinationIP == destination &&
                        te.flowId == flow.get())
                    break;
            }

            return i;
        }

        int find(NetAddress source, NetAddress destination, final Path route) {
            int i;
            TableEntry te;
            for (i=size-1; i>=0; i--){
                te = (TableEntry) table.get(i);
                if (te.sourceIP == source &&
                        te.destinationIP == destination &&
                        te.sourceRoute.equals(route))
                    break;
            }
            

            return i;
        }

        

        int generateNextFlowId(NetAddress destination, boolean allowDefault) {
            if (((counter&1)!=0)^allowDefault) // make sure parity is correct
                counter++;

            assert(((counter & 1)!=0) == allowDefault);
            return counter++;
        }

        int createEntry(NetAddress source, NetAddress destination, 
                       int flow) {
            if (find(source, destination, new Int(flow)) != -1)
                return -1;

            TableEntry newEntry = new TableEntry();
            newEntry.sourceIP = source;
            newEntry.destinationIP = destination;
            newEntry.flowId = flow;
            table.add(newEntry);

            if ((flow & 1)!=0)
                DRTab.insert(source, destination, flow);

            return table.size();
        }

        void noticeDeadLink(final ID from, final ID to) {
            long now = JistAPI.getTime();
            TableEntry te;

            for (int i=0; i<size; i++){
                te = (TableEntry) table.get(i);
                if (te.timeout >= now && te.sourceIP.equals(net_addr))
                    for (int n=0; n < (te.sourceRoute.length()-1); n++)
                        if (te.sourceRoute.get(n).equals(from)&&
                            te.sourceRoute.get(n+1).equals(to)) {
                            te.timeout = now - 1;
                            // X-XX ych rediscover??? 5/2/01
                        }
            }
        }

//         if ent represents a default flow, bad things are going down and we need
//         to rid the default flow table of them.
        static void checkDefaultFlow(DRTable DRTab, final TableEntry ent) {
            Int flow = new Int();
            if (DRTab.find(ent.sourceIP, ent.destinationIP, flow)==0)
                return;
            if (flow.get() == ent.flowId)
                DRTab.flush(ent.sourceIP, ent.destinationIP);
        }

        void cleanup() {
            int front, back;
            long now = JistAPI.getTime();

            return; // it's messing up path orders...
/*
            // init front to the first expired entry
            for (front=0; (front<size) && (table[front].timeout >= now); front++)
                ;

            // init back to the last unexpired entry
            for (back = size-1; (front<back) && (table[back].timeout < now); back--)
                checkDefaultFlow(DRTab, table[back]);

            while (front < back) {
                checkDefaultFlow(DRTab, table[front]);
                bcopy(table+back, table+front, sizeof(TableEntry)); // swap
                back--;

                // next expired entry
                while ((front<back) && (table[front].timeout >= now))
                    front++;
                while ((front<back) && (table[back].timeout < now)) {
                    checkDefaultFlow(DRTab, table[back]);
                    back--;
                }
            }
*/
//            size = back+1;
        }

        void setNetAddr(NetAddress net_id) {
            net_addr = net_id;
        }

        boolean defaultFlow(NetAddress source, NetAddress destination, 
                        Int flow) {
            return DRTab.find(source, destination, flow)!=0;
        }

        
    }
    

   
    
    
    private class RtRepHoldoff {
        ID requestor;
        ID requested_dest;
        int best_length;
        int our_length;
      }

    private class SendBufEntry {
        long t;         // insertion time
        SRPacket p;
      };

      private class GratReplyHoldDown {
        long t;
        Path p;
      };
    
    /**
     * An entry in the Route Request Table.
     */
    public static class RouteRequestTableEntry
    {
        
    
      /** The IP TTL on the last Route Request for this destination. */
      public byte lastRequestTTL;

      /** The time of the last Route Request for this destination. */
      public long lastRequestTime;

      /**
       * The number of Route Requests for this destination since we last
       * received a valid Route Reply.
       */
      public int numRequestsSinceLastReply;

      /**
       * The amount of time necessary to wait (starting at lastRequestTime)
       * before sending out another Route Request.
       */
      public long timeout;

      /** Identification values of recently seen requests coming from this node. */
      public LinkedList ids;
      
      /** the maximum number of route requests allowed for this entry */
      public int maxRequests;

    public int rt_reqs_outstanding;
    
    public ID MAC_id;
    public ID net_id;
    public int req_num;
    long last_arp;
    long last_rt_req;
    int last_type;

      /** Creates a new RouteRequestTableEntry. */
      public RouteRequestTableEntry()
      {
//        lastRequestTTL = MAX_TTL;
        lastRequestTime = JistAPI.getTime();
        numRequestsSinceLastReply = 0;
//        timeout = REQUEST_PERIOD;
        ids = new LinkedList();
//        maxRequests = MAX_SALVAGE_COUNT;
      }
    }

    private class ErrorTableEntry{
      NetAddress src;
      NetAddress dst;
      long time;
      
      public ErrorTableEntry(NetAddress src, NetAddress dst, long time)
      {
          this.src = src;
          this.dst = dst;
          this.time = time;
      }
  }
    /** An entry in the Gratuitous Route Reply Table. */
    private class RouteReplyTableEntry
    {
      /** The originator of the shortened Source Route. */
      public NetAddress originator;

      /**
       * The last hop address of the shortened Source Route before reaching
       * this node.
       */
      public NetAddress lastHop;

      /**
       * Creates a new <code>RouteReplyTableEntry</code>.
       *
       * @param o the originator of the shortened Source Route
       * @param l the last hop address of the shortened Source Route
       */
      public RouteReplyTableEntry(NetAddress o, NetAddress l)
      {
        originator = o;
        lastHop = l;
      }

      /** {@inheritDoc} */
      public int hashCode()
      {
        return originator.hashCode() + lastHop.hashCode();
      }

      /** {@inheritDoc} */
      public boolean equals(Object o)
      {
        if (o == null || !(o instanceof RouteReplyTableEntry)) return false;

        RouteReplyTableEntry other = (RouteReplyTableEntry)o;
        return other.originator.equals(originator) && other.lastHop.equals(lastHop);
      }
    }
    
    public static class PacketEvent{
        public static int DataAtDest=0;
        public static int DataDropped=1;
    }
    
    public RouteDsr_Ns2(){
        
    }
    
    
    private void sendBufferCallback()
    { 
        sendBufferCheck(); 
        send_buf_timer.resched(BUFFER_CHECK + BUFFER_CHECK * random.nextDouble());
     }
    
    public void callback(int id, Location loc, NetAddress na, NS2Timer t, int timerId)
    {
        if (t.performCallback(timerId))
        {
            switch (id)
            {
            case CALLBACK_SBT:
                sendBufferCallback();
                break;

            default:
                break;
            
            }
        }
        
    }    
    

    private void
    dropSendBuff(SRPacket p)
      // log p as being dropped by the sendbuffer in DSR agent
    {
      /*trace("Ssb %.5f _%s_ dropped %s . %s", JistAPI.getTime(), 
        net_id.dump(), p.src.dump(), p.dest.dump());*/
      drop(p.pkt, DROP_RTR_QTIMEOUT);
      p.pkt = null;;
      p.route.reset();
    }

    private void
    stickPacketInSendBuffer(SRPacket p)
    {
      long min = Long.MAX_VALUE;
      int min_index = 0;
      int c;

      /*if (verbose)
        trace("Sdebug %.5f _%s_ stuck into send buff %s . %s",
          JistAPI.getTime(), 
          net_id.dump(), p.src.dump(), p.dest.dump());*/

      for (c = 0 ; c < SEND_BUF_SIZE ; c ++)
        if (send_buf[c].p==null || send_buf[c].p.pkt == null)
          {
        send_buf[c].t = JistAPI.getTime();
        send_buf[c].p = p;
        return;
          }
        else if (send_buf[c].t < min)
          {
        min = send_buf[c].t;
        min_index = c;
          }
      
      // kill somebody
      dropSendBuff(send_buf[min_index].p);
      send_buf[min_index].t = JistAPI.getTime();
      send_buf[min_index].p = p;
    }

    private void
    sendBufferCheck()
      // see if any packets in send buffer need route requests sent out
      // for them, or need to be expired
    { // this is called about once a second.  run everybody through the
      // get route for pkt routine to see if it's time to do another 
      // route request or what not
      int c;

      for (c  = 0 ; c <SEND_BUF_SIZE ; c++) {
          if (send_buf[c] == null || 
                  send_buf[c].p == null || send_buf[c].p.pkt == null)
              continue;
          if (JistAPI.getTime() - send_buf[c].t > send_timeout) {
              dropSendBuff(send_buf[c].p);
              send_buf[c].p.pkt = null;
              continue;
          }
    if( DEBUG){
//          trace("Sdebug %.5f _%s_ checking for route for dst %s",
//            JistAPI.getTime(), net_id.dump(), 
//            send_buf[c].p.dest.dump());
      }

          handlePktWithoutSR(send_buf[c].p, true);
          if( DEBUG){
//          if (send_buf[c].p.pkt == null) 
//              trace("Sdebug %.5f _%s_ sendbuf pkt to %s liberated by handlePktWOSR",
//                JistAPI.getTime(), net_id.dump(), 
//                send_buf[c].p.dest.dump());
          }
      }
    }

    /*==============================================================
      Route Request backoff
    ------------------------------------------------------------*/
    private boolean
    BackOffTest(Entry e, long time)
//     look at the entry and decide if we can send another route
//     request or not.  update entry as well
    {
      long next = ((long) (0x1 << (e.rt_reqs_outstanding * 2))) * rt_rq_period;

      if (next > rt_rq_max_period)
          next = rt_rq_max_period;

      if (next + e.last_rt_req > time)
          return false;

      // don't let rt_reqs_outstanding overflow next on the LogicalShiftsLeft's
      if (e.rt_reqs_outstanding < 15)
          e.rt_reqs_outstanding++;

      e.last_rt_req = time;

      return true;
    }

    /*===========================================================================
      RouteDSR_NS2 OTcl linkage
    ---------------------------------------------------------------------------*/
//    static class DSRAgentClass : public TclClass {
//    public:
//      DSRAgentClass() : TclClass("Agent/RouteDSR_NS2") {}
//      TclObject* create(int, final Stringfinal*) {
//        return (new RouteDSR_NS2);
//      }
//    } class_DSRAgent;

    /*===========================================================================
      RouteDSR_NS2 methods
    ---------------------------------------------------------------------------*/
//    RouteDSR_NS2(): Agent(PT_DSR), request_table(128), route_cache(null),
//    send_buf_timer(this), flow_table(), ars_table()
    public RouteDsr_Ns2(int ip)
    {
    	localAddr = new NetAddress(ip);
        self = (RouteInterface.Dsr_NS2)JistAPI.proxy(this, RouteInterface.Dsr_NS2.class);
        request_table = new RequestTable(128);
        // drc: this is handled w/ start()
//        send_buf_timer = new SendBufferTimer(-1);
        flow_table = new FlowTable(FLOW_TABLE_SIZE);
        ars_table = new ARSTable(ARS_TABLE_SIZE);
        net_id = new ID(ip, ID.ID_Type.IP);
        MAC_id = new ID(ip, ID.ID_Type.MAC);
        
      int c;
      route_request_num = 1;

      route_cache = makeRouteCache();
      
      for (c = 0; c < SEND_BUF_SIZE; c++) send_buf[c] = new SendBufEntry();

      for (c = 0 ; c < RTREP_HOLDOFF_SIZE ; c++){
          rtrep_holdoff[c] = new RtRepHoldoff();
          rtrep_holdoff[c].requested_dest = invalid_addr;
      }
      num_heldoff_rt_replies = 0;

//      target_ = 0;
//      logtarget = 0;

      grat_hold_victim = 0;
      for (c = 0; c < RTREP_HOLDOFF_SIZE ; c++) {
          grat_hold[c] = new GratReplyHoldDown();
        grat_hold[c].t = 0;
        grat_hold[c].p = new Path();
        grat_hold[c].p.reset();
      }

      //bind("off_SR_", &off_sr_);
      //bind("off_ll_", &off_ll_);
      //bind("off_mac_", &off_mac_);
      //bind("off_ip_", &off_ip_);

//      ll = 0;
      ifq = null;
      mac_ = null;

//      LIST_INSERT_HEAD(&agthead, this, link);
      
    // T-ODO drc: evaluation this below
      //#ifdef DSR_FILTER_TAP
    //  bzero(tap_uid_cache, sizeof(tap_uid_cache));
    //#endif
      route_error_held = false;
    }

//    ~RouteDSR_NS2()
//    {
//      System.err.printf("DFU: Don't do this! I haven't figured out ~RouteDSR_NS2\n");
//      exit(-1);
//    }

//    void
//    Terminate()
//    {
//        int c;
//        for (c  = 0 ; c < SEND_BUF_SIZE ; c++) {
//            if (send_buf[c].p.pkt) {
//                drop(send_buf[c].p.pkt, DROP_END_OF_SIMULATION);
//                send_buf[c].p.pkt = null;;
//            }
//        }
//    }

    private void
    testinit()
    {
      /*RouteDsrMsg_NS2 hsr;
      
      if (net_id.equals(new ID(1,IP)))
        {
          printf("adding route to 1\n");
          hsr.init();
          hsr.append_addr( 1, NS_AF_INET );
          hsr.append_addr( 2, NS_AF_INET );
          hsr.append_addr( 3, NS_AF_INET );
          hsr.append_addr( 4, NS_AF_INET );
          
          route_cache.addRoute(Path(hsr.addrs(),
                     hsr.num_addrs()), 0.0, ID(1,IP));
        }
      
      if (net_id.equals(new ID(3,IP)))
        {
          printf("adding route to 3\n");
          hsr.init();
          hsr.append_addr( 3, NS_AF_INET );
          hsr.append_addr( 2, NS_AF_INET );
          hsr.append_addr( 1, NS_AF_INET );
          
          route_cache.addRoute(Path(hsr.addrs(),
                     hsr.num_addrs()), 0.0, ID(3,IP));
        }*/
    }


//    int
//    command(int argc, final Stringfinal* argv)
//    {
//      TclObject *obj;  
//
//      if (argc == 2) 
//        {
//          if (strcasecmp(argv[1], "testinit") == 0)
//        {
//          testinit();
//          return TCL_OK;
//        }
//          if (strcasecmp(argv[1], "reset") == 0)
//        {
//          Terminate();
//          return Agent.command(argc, argv);
//        }
//          if (strcasecmp(argv[1], "check-cache") == 0)
//        {
//          return route_cache.command(argc, argv);
//        }
//          if (strcasecmp(argv[1], "startdsr") == 0)
//        {
//          if (ID(1,IP).equals(net_id)) 
//            { // log the configuration parameters of the dsragent
//      trace("Sconfig %.5f tap: %s snoop: rts? %s errs? %s",
//                JistAPI.getTime(),
//                dsragent_use_tap ? "on" : "off",
//                dsragent_snoop_source_routes ? "on" : "off",
//                dsragent_snoop_forwarded_errors ? "on" : "off");
//      trace("Sconfig %.5f salvage: %s !bd replies? %s",
//                JistAPI.getTime(),
//                dsragent_salvage_with_cache ? "on" : "off",
//                dsragent_dont_salvage_bad_replies ? "on" : "off");
//      trace("Sconfig %.5f grat error: %s grat reply: %s",
//                    JistAPI.getTime(),
//                    dsragent_propagate_last_error ? "on" : "off",
//                    dsragent_send_grat_replies ? "on" : "off");
//      trace("Sconfig %.5f $reply for props: %s ring 0 search: %s",
//                    JistAPI.getTime(),
//                    dsragent_reply_from_cache_on_propagating ? "on" : "off",
//                    dsragent_ring_zero_search ? "on" : "off");
//            }
//          // cheap source of jitter
//          send_buf_timer.sched(BUFFER_CHECK 
//                       + BUFFER_CHECK * Random.uniform(1.0));    
//              return route_cache.command(argc,argv);
//        }
//        }
//      else if(argc == 3) 
//        {
//          if (strcasecmp(argv[1], "addr") == 0) 
//        {
//          int temp;
//          temp = Address.instance().str2addr(argv[2]);
//         net_id = ID(temp, IP);
//         flow_table.setNetAddr(net_id.addr);
//         route_cache.net_id = net_id;
//         return TCL_OK;
//        } 
//          else if(strcasecmp(argv[1], "mac-addr") == 0) 
//        {
//          MAC_id = ID(atoi(argv[2]), ::MAC);
//          route_cache.MAC_id = MAC_id;
//          return TCL_OK;
//        }
//          else if(strcasecmp(argv[1], "rt_rq_max_period") == 0)
//            {
//              rt_rq_max_period = strtod(argv[2],null);
//              return TCL_OK;
//            }
//          else if(strcasecmp(argv[1], "rt_rq_period") == 0)
//            {
//              rt_rq_period = strtod(argv[2],null);
//              return TCL_OK;
//            }
//          else if(strcasecmp(argv[1], "send_timeout") == 0)
//            {
//              send_timeout = strtod(argv[2],null);
//              return TCL_OK;
//            }
//
//          
//          if( (obj = TclObject.lookup(argv[2])) == 0) 
//        {
//          System.err.printf( "RouteDSR_NS2: %s lookup of %s failed\n", argv[1],
//              argv[2]);
//          return TCL_ERROR;
//        }
//
//          if (strcasecmp(argv[1], "log-target") == 0)  {
//              logtarget = (Trace*) obj;
//              return route_cache.command(argc, argv);
//          }
//          else if (strcasecmp(argv[1], "tracetarget") == 0 )
//            {
//          logtarget = (Trace*) obj;
//          return route_cache.command(argc, argv);
//        }
//          else if (strcasecmp(argv[1], "install-tap") == 0)  
//        {
//          mac_ = (Mac*) obj;
//          mac_.installTap(this);
//          return TCL_OK;
//        }
//          else if (strcasecmp(argv[1], "node") == 0)
//        {
//          node_ = (MobileNode *) obj;
//          return TCL_OK;
//        }
//          else if (strcasecmp (argv[1], "port-dmux") == 0) 
//        {
//          port_dmux_ = (NsObject *) obj;
//          return TCL_OK;
//        }
//        }
//      else if (argc == 4)
//        {
//          if (strcasecmp(argv[1], "add-ll") == 0) 
//        {
//          if( (obj = TclObject.lookup(argv[2])) == 0) {
//            System.err.printf( "RouteDSR_NS2: %s lookup of %s failed\n", argv[1],
//                argv[2]);
//            return TCL_ERROR;
//          }
//          ll = (NsObject*) obj;
//          if( (obj = TclObject.lookup(argv[3])) == 0) {
//            System.err.printf( "RouteDSR_NS2: %s lookup of %s failed\n", argv[1],
//                argv[3]);
//            return TCL_ERROR;
//          }
//          ifq = (CMUPriQueue *) obj;
//          return TCL_OK;
//
//        }
//
//
//        }
//      return Agent.command(argc, argv);
//    }

    private void
    sendOutBCastPkt(NetMessage p)
    {
      //hdr_cmn cmh =  hdr_cmn.access(p);
      //if(cmh.direction() == hdr_cmn.UP)
      //  cmh.direction() = hdr_cmn.DOWN;
      // no jitter required
      //Scheduler.instance().schedule(ll, p, 0.0);
        // T-ODO drc: check that this is alreayd an IP message
        assert(((NetMessage.Ip)p).getProtocol()==Constants.NET_PROTOCOL_DSR_NS2);
        visualizePacketType((NetMessage.Ip)p);
        
        netEntity.send((NetMessage.Ip)p, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
        
    }



    /*===========================================================================
      handlers for each class of packet
    ---------------------------------------------------------------------------*/
    private void
    handlePktWithoutSR(SRPacket p, boolean retry)
      /* obtain a source route to p's destination and send it off.
         this should be a retry if the packet is already in the sendbuffer */
    {
      assert((p.pkt).valid());

      if (p.dest.equals(net_id))
        { // it doesn't need a source route, 'cause it's for us
          handlePacketReceipt(p, null, (byte) Constants.NET_INTERFACE_DEFAULT);
          return;
        }

      // Extensions for wired cum wireless simulation mode
      //if pkt dst outside my subnet, route to base_stn

      /*ID dest;
      if (diff_subnet(p.dest,net_id)) {
      dest = ID(node_.base_stn(),IP);
      p.dest = dest;
      }*/

      if (route_cache.findRoute(p.dest, p.route, true))
        { // we've got a route...
          /*if (verbose)
        trace("S$hit %.5f _%s_ %s . %s %s",
              JistAPI.getTime(), net_id.dump(),
              p.src.dump(), p.dest.dump(), p.route.dump());*/      
          sendOutPacketWithRoute(p, true, 0);
          return;
        } // end if we have a route
      else
        { // we don't have a route...
        //  if (verbose) 
        /*trace("S$miss %.5f _%s_ %s . %s", 
              JistAPI.getTime(), net_id.dump(), 
              net_id.dump(), p.dest.dump());*/

          getRouteForPacket(p, retry);
          return;
        } // end of we don't have a route
    }

    private void
    handlePacketReceipt(SRPacket p, MacAddress lastHop, byte macId)
      /* Handle a packet destined to us */
    {
//      hdr_cmn cmh =  hdr_cmn.access(p.pkt);
      RouteDsrMsg_Ns2 srh =  p.pkt;
      NetMessage.Ip iph = p.iph;
      if (srh.route_reply())
        { // we got a route_reply piggybacked on a route_request
          // accept the new source route before we do anything else
          // (we'll send off any packet's we have queued and waiting)
          acceptRouteReply(p);
        }
      
      if (srh.route_request())
        {
          if (dsragent_reply_only_to_first_rtreq  && ignoreRouteRequestp(p)) 
        { //we only respond to the first route request
          // we receive from a host 
//          // Packet.free(p.pkt);     // drop silently
//          p.pkt = null;
          return;
        }
          else
        { // we're going to process this request now, so record the req_num
          request_table.insert(p.src, p.src, srh.rtreq_seq());
          returnSrcRouteToRequestor(p);
        }
        }

      if (srh.route_error())
        { // register the dead route      
          processBrokenRouteError(p);
        }

      if (srh.flow_unknown())
        processUnknownFlowError(p, false);

      if (srh.flow_default_unknown())
        processUnknownFlowError(p, true);

      /* give the data in the packet to our higher layer (our port dmuxer, most 
       likely) */
      //handPktToDmux(p);
      assert(p.dest.equals(net_id) || p.dest.equals(MAC_id));
      
    /*#if 0
      if (iph.dport() == 255) {
        int mask = Address.instance().portmask();
        int shift = Address.instance().portshift();  
        iph.daddr() = ((iph.dport() & mask) << shift) | ((~(mask) << shift) & iph.dst());
      }
    //#endif */
      
//      cmh.size() -= srh.size();   // cut off the SR header 4/7/99 -dam
//      srh.setValid(false); // T-ODO drc removed this because it messes with the references
//      cmh.size() -= IP_HDR_LEN;    // cut off IP header size 4/7/99 -dam
      
      if (srh.getPayload()==null) return;
      // Now go through some strange contortions to get this message received by
      // the proper protocol handler
      notifyVisualizationEvent(PacketEvent.DataAtDest);
      NetMessage.Ip newIp = new NetMessage.Ip(srh.getPayload(), iph.getSrc(), iph.getDst(),
        srh.getNextHeaderType(), iph.getPriority(), iph.getTTL()); 
      assert(newIp.getPayload() instanceof TransUdp.UdpMessage);
       
      netEntity.receive(newIp, lastHop, macId, false);

    }


    private void
    handleDefaultForwarding(SRPacket p) {
      NetMessage.Ip iph = p.iph;// = hdr_ip.access(p.pkt);
      Int flowid = new Int();
      int       flowidx;

      if (!flow_table.defaultFlow(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), flowid)) {
        sendUnknownFlow(p, true, new Int(0));
        assert(p.pkt == null);
        return;
      }

      if ((flowidx = flow_table.find(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), flowid)) == -1) {
        sendUnknownFlow(p, false, flowid);
        assert(p.pkt == null);
        return;
      }

      if (iph.getTTL() != ((TableEntry)flow_table.get(flowidx)).expectedTTL) {
        sendUnknownFlow(p, true, new Int(0));
        assert(p.pkt == null);
        return;
      }

      // X-XX should also check prevhop

      handleFlowForwarding(p, flowidx);
    }

    private void
    handleFlowForwarding(SRPacket p, int flowidx) {
      RouteDsrMsg_Ns2 srh = p.pkt;
      NetMessage.Ip iph = p.iph;// = hdr_ip.access(p.pkt);
     // hdr_cmn cmnh =  hdr_cmn.access(p.pkt);
      int amt = 0;

      assert(flowidx >= 0);
      assert(srh.num_addrs()>0);

      iph.setNextHop(new IpOptionNextHop(((TableEntry)flow_table.get(flowidx)).nextHop));
      //cmnh.addr_type() = IP;

//      cmnh.xmit_failure_ = XmitFlowFailureCallback;
//      cmnh.xmit_failure_data_ = this;
//
//      // make sure we aren't cycling packets
//      //assert(p.pkt.incoming == 0); // this is an outgoing packet
//      assert(cmnh.direction() == hdr_cmn.UP);

      iph.decTTL();
      if (iph.getTTL()<=0) {
        drop(p.pkt, DROP_RTR_TTL);
        p.pkt = null;
        return;
      }

//      trace("SFf %.9f _%s_ %d [%s . %s] %d to %d", 
//        JistAPI.getTime(), net_id.dump(), cmnh.uid(),
//        p.src.dump(), p.dest.dump(), ((TableEntry)flow_table.get(flowidx)).flowId,
//        ((TableEntry)flow_table.get(flowidx)).nextHop);

      // X-XX ych 5/8/01 ARS also should check previous hop
      if (!srh.salvaged() && 
              // T-ODO drc: fix
          (amt = ars_table.findAndClear(p.iph.getId(), ((TableEntry)flow_table.get(flowidx)).flowId))!=0 &&
          p.route.index() - amt > 0) {
//        trace("SFARS %.9f _%s_ %d [%s . %s] %d %d", 
//          JistAPI.getTime(), net_id.dump(), cmnh.uid(),
//          p.src.dump(), p.dest.dump(), ((TableEntry)flow_table.get(flowidx)).flowId, amt);

        // stamp a route in the packet...
        p.route = ((TableEntry)flow_table.get(flowidx)).sourceRoute;
        p.route.set_index(p.route.index()-amt);
        sendRouteShortening(p, p.route.index(), 
                ((TableEntry)flow_table.get(flowidx)).sourceRoute.index());
      }

      if (dsragent_always_reestablish) {
        // X-XX this is an utter hack. the flow_table needs to remember the original
        // timeout value specified, as well as the original time to timeout. No
        // establishment packets are allowed after the original time. Must make sure
        // flowids assigned do not overlap. ych 5/8/01
        ((TableEntry)flow_table.get(flowidx)).timeout = JistAPI.getTime() + 
                      default_flow_timeout;
      }
      // set the direction pkt to be down
//      cmnh.direction() = hdr_cmn.DOWN;
      assert(((RouteDsrMsg_Ns2)p.iph.getPayload()).valid());
      if (p.iph.getDst().toInt()==net_id.addr){
          throw new RuntimeException();
      }
      
      
      self.sendIpMsg(p.iph, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
//      Scheduler.instance().schedule(ll, p.pkt, 0);
//      p.pkt = null;
    }

    /**
     * @param pkt
     * @param drop_rtr_ttl2
     */
    private void drop(RouteDsrMsg_Ns2 pkt, int drop_rtr_ttl2) {
        // T-ODO Auto-generated method stub
        
    }


    private void
    handleFlowForwarding(SRPacket p) {
      RouteDsrMsg_Ns2 srh = p.pkt;
      NetMessage.Ip iph = p.iph;// = hdr_ip.access(p.pkt);
      int flowidx = flow_table.find(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), new Int(srh.flow_id()));

      assert(srh.flow_header());

      if (srh.num_addrs()>0) {
        assert(srh.flow_timeout());

        if (flowidx == -1) {
          flow_table.cleanup();
          flowidx = flow_table.createEntry(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), srh.flow_id());

          assert(flowidx != -1);

          ((TableEntry)flow_table.get(flowidx)).timeout = JistAPI.getTime() + 
                        srh.flow_timeout_time();
          ((TableEntry)flow_table.get(flowidx)).hopCount = srh.hopCount();
          ((TableEntry)flow_table.get(flowidx)).expectedTTL = iph.getTTL();
          ((TableEntry)flow_table.get(flowidx)).sourceRoute = p.route;
          ((TableEntry)flow_table.get(flowidx)).nextHop = srh.get_next_addr();
          assert(srh.hopCount() == srh.cur_addr());
          assert(srh.get_next_type() == ID_Type.IP);
          assert(((TableEntry)flow_table.get(flowidx)).sourceRoute.path[((TableEntry)flow_table.get(flowidx)).hopCount] == 
             net_id);

          ((TableEntry)flow_table.get(flowidx)).count = 0;            // shouldn't be used
          ((TableEntry)flow_table.get(flowidx)).allowDefault = false; // shouldn't be used
        }

        assert(flowidx != -1);
        //assert(((TableEntry)flow_table.get(flowidx)).hopCount == srh.hopCount());
        
        srh.setHopCount(srh.hopCount()+1);
        return;
      }

      if (flowidx == -1) {
        // return an error
        sendUnknownFlow(p, false, new Int(srh.flow_id()));
//        assert(p.pkt == null);
        return;
      }

      //assert(((TableEntry)flow_table.get(flowidx)).hopCount == srh.hopCount());

      srh.setHopCount(srh.hopCount()+1);

      // forward the packet
      handleFlowForwarding(p, flowidx);
    }

    private void
    handleForwarding(SRPacket p)
      /* forward packet on to next host in source route,
       snooping as appropriate */
    {
      RouteDsrMsg_Ns2 srh =  p.pkt;
      NetMessage.Ip iph = p.iph;// = hdr_ip.access(p.pkt);
      //hdr_cmn ch =  hdr_cmn.access(p.pkt);
      boolean flowOnly = srh.num_addrs()==0;

      if (srh.flow_header())
        handleFlowForwarding(p);
      else if (srh.num_addrs()==0)
        handleDefaultForwarding(p);

      if (flowOnly)
        return;

      assert(p.pkt!=null); // make sure flow state didn't eat the pkt


      // drc: this is OK when there is an error intended for a node  
      // and a packet along with it that is not supposed to be forwarded by that node
      if (p.route.index() >= p.route.length())
        {
          //System.err.printf("dfu: ran off the end of a source route\n");
          //*trace("SDFU:  ran off the end of a source route\n");*/
          drop(p.pkt, DROP_RTR_ROUTE_LOOP);
//          p.pkt = null;;
          // maybe we should send this packet back as an error...
          return;
        }

      
      // first make sure we are the ``current'' host along the source route.
      // if we're not, the previous node set up the source route incorrectly.
      assert (p.route==null || p.route.path==null || p.route.path[p.route.index()]==null);
      assert (!p.route.path[p.route.index()].equals(net_id));
      assert(p.route.path[p.route.index()].equals(net_id)
         || p.route.path[p.route.index()].equals(MAC_id));
      
      
      // if there's a source route, maybe we should snoop it too
      if (dsragent_snoop_source_routes)
        route_cache.noticeRouteUsed(p.route, JistAPI.getTime(), 
                     net_id);

      // sendOutPacketWithRoute will add in the size of the src hdr, so
      // we have to subtract it out here
//      ch.size() -= srh.size();

      // we need to manually decr this, since nothing else does.
      iph.decTTL();
      if (iph.getTTL()<=0) {
        drop(p.pkt, DROP_RTR_TTL);
        p.pkt = null;;
        return;
      }

      // now forward the packet...
      sendOutPacketWithRoute(p, false, 0);
    }

    private void
    handleRouteRequest(SRPacket p)
      /* process a route request that isn't targeted at us */
    {
      RouteDsrMsg_Ns2 srh =  p.pkt.copy();
      assert (srh.route_request());

    /*#ifdef notdef
      {
              int src = mac_.hdr_src(HDR_MAC(p.pkt)); 

              if(mac_.is_neighbor(src) == 0) {
                      // Packet.free(p.pkt);
                      p.pkt = null;;
                      return;
              }
      }
    //#endif */

      if (ignoreRouteRequestp(p)) 
        {
          /*if (verbose_srr) 
            trace("SRR %.5f _%s_ dropped %s #%d (ignored)",
                  JistAPI.getTime(), net_id.dump(), p.src.dump(),
                  srh.rtreq_seq());*/
//          // Packet.free(p.pkt);  // pkt is a route request we've already processed
//          p.pkt = null;
          return; // drop silently
        }

      // we're going to process this request now, so record the req_num
      request_table.insert(p.src, p.src, srh.rtreq_seq());

      /*  - if it's a Ring 0 search, check the rt$ for a reply and use it if
         possible.  There's not much point in doing Ring 0 search if you're 
         not going to check the cache.  See the comment about turning off all
         reply from cache behavior near the definition of d_r_f_c_o_p (if your 
         workload had really good spatial locality, it might still make 
         sense 'cause your target is probably sitting next to you)
          - if reply from cache is on, check the cache and reply if possible
          - otherwise, just propagate if possible. */
      if ((srh.max_propagation() == 0 || dsragent_reply_from_cache_on_propagating)
          && replyFromRouteCache(p))
          return;           // all done

    if (NEW_REQUEST_LOGIC){
      /*
       * If we are congested, don't forward or answer the Route Reply
       */
      if(ifq.size() > 10) {
          /*trace("SRR %.9f _%s_ discarding %s #%d (ifq length %d)",
            JistAPI.getTime(),
            net_id.dump(),
            p.src.dump(),
            srh.rtreq_seq(),
            ifq.prq_length());*/
//          // Packet.free(p.pkt);
//          p.pkt = null;
          return;
      }

      /*
       *  If "free air time" < 15%, don't forward or answer the Route Reply
       */
      {
          double atime = radio.getActivityRatio();

          if(atime > 0.0 && atime < 0.15) {
//              trace("SRR %.9f _%s_ discarding %s #%d (free air time %f)",
//                JistAPI.getTime(),
//                net_id.dump(),
//                p.src.dump(),
//                srh.rtreq_seq(),
//                atime);
//              // Packet.free(p.pkt);
//              p.pkt = null;
              return;
          }
      }  
    }

      // does the orginator want us to propagate?
      if (p.route.length() > srh.max_propagation())
        {   // no propagation
//          if (verbose_srr) 
//            trace("SRR %.5f _%s_ dropped %s #%d (prop limit exceeded)",
//                  JistAPI.getTime(), net_id.dump(), p.src.dump(),
//                  srh.rtreq_seq());
//          // Packet.free(p.pkt); // pkt isn't for us, and isn't data carrying
          p.pkt = null;
          return;       
        }

      // can we propagate?
      if (p.route.full())
        {   // no propagation
//          trace("SRR %.5f _%s_ dropped %s #%d (SR full)",
//                JistAPI.getTime(), net_id.dump(), p.src.dump(),
//            srh.rtreq_seq());
//          /* pkt is a rt req, even if data carrying, we don't want to log 
//         the drop using drop() since many nodes could be dropping the 
//         packet in this fashion */
//          // Packet.free(p.pkt);
//          p.pkt = null;
          return;       
        }

      // add ourselves to the source route
      p.route.appendToPath(net_id);

//      if (verbose_srr)
//        trace("SRR %.5f _%s_ rebroadcast %s #%d .%s %s",
//              JistAPI.getTime(), net_id.dump(), p.src.dump(),
//              srh.rtreq_seq(), p.dest.dump(), p.route.dump());

      sendOutPacketWithRoute(p, false, 0);
      return;      
    }

    /*===========================================================================
      Helpers
    ---------------------------------------------------------------------------*/
    private boolean
    ignoreRouteRequestp(SRPacket p)
//     should we ignore this route request?
    {
      RouteDsrMsg_Ns2 srh = p.pkt;

      if (request_table.get(p.src) >= srh.rtreq_seq())
        { // we've already processed a copy of this reqest so
          // we should drop the request silently
          return true;
        }
      if (p.route.member(net_id,MAC_id))
        { // we're already on the route, drop silently
          return true;
        }

      if (p.route.full())
        { // there won't be room for us to put our address into
          // the route
          // so drop silently - sigh, so close, and yet so far...
          // Note that since we don't record the req_id of this message yet,
          // we'll process the request if it gets to us on a shorter path
          return true;
        }
      return false;
    }


    private boolean
    replyFromRouteCache(SRPacket p)
      /* - see if can reply to this route request from our cache
         if so, do it and return true, otherwise, return false 
         - frees or hands off p iff returns true */
    {
      Path rest_of_route = new Path();
      Path complete_route = p.route.copy();

      /* we shouldn't yet be on on the pkt's current source route */
      assert(!p.route.member(net_id, MAC_id));

      // do we have a cached route the target?
      /* X-XX what if we have more than 1?  (and one is legal for reply from
         cache and one isn't?) 1/28/97 -dam */
      if (!route_cache.findRoute(p.dest, rest_of_route, false))
        { // no route => we're done
          return false;
        }

      /* but we should be on on the remainder of the route (and should be at
       the start of the route */
      assert(rest_of_route.path[0].equals(net_id) || rest_of_route.path[0].equals(MAC_id));

      if (rest_of_route.length() + p.route.length() >= RouteDsrMsg_Ns2.MAX_SR_LEN)
        return false; // too long to work with...

      // add our suggested completion to the route so far
      complete_route.appendPath(rest_of_route);

      // call compressPath to remove any double backs
      Path.compressPath(complete_route);

      if (!complete_route.member(net_id, MAC_id))
        { // we're not on the suggested route, so we can't return it
          return false;
        }

      // if there is any other information piggybacked into the
      // route request pkt, we need to forward it on to the dst
//      hdr_cmn cmh =  hdr_cmn.access(p.pkt);
      RouteDsrMsg_Ns2 srh =  p.pkt;
      int request_seqnum = srh.rtreq_seq();
      
      if ( srh.payload!=null  || //PT_DSR != cmh.ptype() ||   // there's data // T-ODO drc: check that this was ok to comment out
           srh.route_reply()
          || (srh.route_error() && 
          !srh.down_links()[srh.num_route_errors()-1].tell_addr.equals(GRAT_ROUTE_ERROR) ))
        { // must forward the packet on
          SRPacket p_copy = new SRPacket(p.iph, p.pkt);
          p_copy.dest = p.dest;
          p_copy.src = p.src;
          p.pkt = null;;
          srh.set_route_request(false);

          p_copy.route = complete_route;
          p_copy.route.setIterator(p.route.length());
          
          if (!p_copy.route.path[p_copy.route.index()].equals(net_id)){
              throw new RuntimeException();
          }

          assert(p_copy.route.path[p_copy.route.index()].equals(net_id));
          
//          if (verbose) trace("Sdebug %.9f _%s_ splitting %s to %s",
//                             JistAPI.getTime(), net_id.dump(),
//                             p.route.dump(), p_copy.route.dump());

          sendOutPacketWithRoute(p_copy,false, 0);
        }
      else 
        {
//          // Packet.free(p.pkt);  // free the rcvd rt req before making rt reply
//          p.pkt = null;
        }

      // make up and send out a route reply
//      p.route.appendToPath(net_id); // drc: this seemed to be wrong
      p.route.reverseInPlace();
      for (int i = 0; i < p.route.length(); i++){
          if (p.route.get(i).addr == net_id.addr){
              p.route.set_index(i+1);
              break;
          }
      }
      route_cache.addRoute(p.route, JistAPI.getTime(), net_id);
      p.dest = p.src;
      p.src = net_id;
      p.pkt = new RouteDsrMsg_Ns2();

      p.iph = new NetMessage.Ip(p.pkt, NetAddressIpFactory.getAddress((int)p.src.addr), 
              NetAddressIpFactory.getAddress((int)p.dest.addr), Constants.NET_PROTOCOL_DSR_NS2, 
              Constants.NET_PRIORITY_NORMAL, 255);
 // =  hdr_ip.access(p.pkt);
//      iph.saddr() = Address.instance().create_ipaddr(p.src.addr, RT_PORT);
//      iph.sport() = RT_PORT;
//      iph.daddr() = Address.instance().create_ipaddr(p.dest.addr, RT_PORT);
//      iph.dport() = RT_PORT;
//      iph.setTTL(255);

      srh = p.pkt;
      srh.init();
      for (int i = 0 ; i < complete_route.length() ; i++){
          if (srh.reply_addrs()[i]==null)srh.reply_addrs()[i] = new sr_addr();
        complete_route.path[i].fillSRAddr(srh.reply_addrs()[i]);
      }
      srh.set_route_reply_len(complete_route.length());
      srh.set_route_reply(true);
      srh.set_cur_addr(p.route.cur_index);
      srh.setRouteRequest(false);

      // propagate the request sequence number in the reply for analysis purposes
      srh.set_rtreq_seq(request_seqnum);

//      hdr_cmn cmnh =  hdr_cmn.access(p.pkt);
//      cmnh.ptype() = PT_DSR;
//      cmnh.size() = IP_HDR_LEN;

//      if (verbose_srr)
//        trace("SRR %.9f _%s_ cache-reply-sent %s . %s #%d (len %d) %s",
//          JistAPI.getTime(), net_id.dump(),
//          p.src.dump(), p.dest.dump(), request_seqnum, complete_route.length(),
//          complete_route.dump());
      sendOutPacketWithRoute(p, true, 0); // drc: changed to false so that the iterator would be on the right address
      return true;
    }


    private void
    sendOutPacketWithRoute(SRPacket p, boolean fresh, long delay)
         // take packet and send it out, packet must a have a route in it
         // return value is not very meaningful
         // if fresh is true then reset the path before using it, if fresh
         //  is false then our caller wants us use a path with the index
         //  set as it currently is
    {
      RouteDsrMsg_Ns2 srh =  p.pkt.copy();
      p.pkt = srh;
      p.iph = p.iph.copy();
//      hdr_cmn cmnh = hdr_cmn.access(p.pkt);
      NetMessage.Ip iph = p.iph;
      p.iph.setPayload(p.pkt);

      assert(srh.valid());
//      assert(cmnh.size() > 0);

/*      ID dest;
//      if (diff_subnet(p.dest,net_id)) {
//      dest = ID(node_.base_stn(),IP);
//      p.dest = dest;
//      }
*/
      if (p.dest.equals(net_id))
        { // it doesn't need to go on the wire, 'cause it's for us
          recv(p.iph, new MacAddress((int)net_id.addr));
//          recv(p.pkt, (Handler) 0);
//          p.pkt = null;;
          return;
        }

      if (fresh)
        {
          p.route.resetIterator();
//          if (verbose && !srh.route_request())
//        {
//          trace("SO %.9f _%s_ originating %s %s", 
//            JistAPI.getTime(), 
//            net_id.dump(), packet_info.name(cmnh.ptype()), p.route.dump());
//        }
        }

      p.route.fillSR(srh);
      

      // set direction of pkt to DOWN , i.e downward
//      cmnh.direction() = hdr_cmn.DOWN;

      // let's see if we can snag this packet for flow state... ych 5/2/01
      if (dsragent_enable_flowstate &&
          p.src.equals(net_id) && !srh.route_request() && srh.cur_addr()==0 &&
          // can't yet decode flow errors and route errors/replies together
          // so don't tempt the system... ych 5/7/01
          !srh.route_error() && !srh.route_reply()) {
        iph= p.iph;
        int flowidx;
        Int flowid = new Int(), default_flowid = new Int();
        long now = JistAPI.getTime();

        // hmmm, let's see if we can save us some overhead...
        if (dsragent_prefer_default_flow &&
        flow_table.defaultFlow(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), flowid) &&
        -1 != (flowidx = flow_table.find(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), flowid)) &&
        ((TableEntry)flow_table.get(flowidx)).timeout >= now &&
        (!dsragent_prefer_shorter_over_default || 
          ((TableEntry)flow_table.get(flowidx)).sourceRoute.length() <= p.route.length()) &&
        !(p.route == ((TableEntry)flow_table.get(flowidx)).sourceRoute)) {

          p.route = ((TableEntry)flow_table.get(flowidx)).sourceRoute;
          p.route.fillSR(srh);
        }

        flowidx = flow_table.find(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), p.route);

        if (flowidx == -1 || ((TableEntry)flow_table.get(flowidx)).timeout < now) {
          // I guess we don't know about this flow; allocate it.
          flow_table.cleanup();
          flowid = new Int(flow_table.generateNextFlowId(p.dest.getNSAddr_t(), true));
          flowidx = flow_table.createEntry(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), (short)flowid.get());
          assert(flowidx != -1);

          // fill out the table
          ((TableEntry)flow_table.get(flowidx)).count = 1;
          ((TableEntry)flow_table.get(flowidx)).lastAdvRt = JistAPI.getTime();
          ((TableEntry)flow_table.get(flowidx)).timeout = now + default_flow_timeout;
          ((TableEntry)flow_table.get(flowidx)).hopCount = 0;
          ((TableEntry)flow_table.get(flowidx)).expectedTTL = iph.getTTL();
          ((TableEntry)flow_table.get(flowidx)).allowDefault = true;
          ((TableEntry)flow_table.get(flowidx)).sourceRoute = p.route;
          ((TableEntry)flow_table.get(flowidx)).nextHop = srh.get_next_addr();
          assert(srh.get_next_type() == ID_Type.IP);

          // fix up the srh for the timeout
          srh.set_flow_timeout(true);
          srh.set_flow_timeout_time(default_flow_timeout);
          srh.set_cur_addr(srh.cur_addr() + 1);
        } else if (((TableEntry)flow_table.get(flowidx)).count <= FlowTable.END_TO_END_COUNT ||
            ((TableEntry)flow_table.get(flowidx)).lastAdvRt < 
               (JistAPI.getTime() - min_adv_interval)) {
          // I've got it, but maybe someone else doesn't
          if (((TableEntry)flow_table.get(flowidx)).expectedTTL != iph.getTTL())
        ((TableEntry)flow_table.get(flowidx)).allowDefault = false;

          ((TableEntry)flow_table.get(flowidx)).count++;
          ((TableEntry)flow_table.get(flowidx)).lastAdvRt = JistAPI.getTime();

          srh.set_flow_timeout(true);
          if (dsragent_always_reestablish)
        srh.set_flow_timeout_time(default_flow_timeout);
          else
        srh.set_flow_timeout_time((int)(((TableEntry)flow_table.get(flowidx)).timeout - now));
          srh.set_cur_addr(srh.cur_addr() + 1);
        } else {
          // flow is established end to end
          assert (((TableEntry)flow_table.get(flowidx)).sourceRoute == p.route);
          srh.set_flow_timeout(false);
          srh.set_cur_addr(0);
          srh.set_num_addrs(0);
        }

        if (dsragent_always_reestablish) {
          // X-XX see major problems detailed above (search for dsragent_always_re..)
          ((TableEntry)flow_table.get(flowidx)).timeout = now + default_flow_timeout;
        }

        iph.setNextHop(new IpOptionNextHop(((TableEntry)flow_table.get(flowidx)).nextHop));
        //cmnh.addr_type() = IP;

        if (flow_table.defaultFlow(p.src.getNSAddr_t(), p.dest.getNSAddr_t(), default_flowid) &&
        ((TableEntry)flow_table.get(flowidx)).flowId == (short)default_flowid.get() &&
        srh.num_addrs()==0 && iph.getTTL() == ((TableEntry)flow_table.get(flowidx)).expectedTTL &&
        ((TableEntry)flow_table.get(flowidx)).allowDefault) {
          // we can go without anything... woo hoo!
          assert(!srh.flow_header());
        } else {
          srh.set_flow_header(true);
          srh.set_flow_id((short) ((TableEntry)flow_table.get(flowidx)).flowId);
          srh.set_hopCount(1);
        }

//        trace("SF%ss %.9f _%s_ %d [%s . %s] %d(%d) to %d %s", 
//        srh.num_addrs() ? "EST" : "",
//        JistAPI.getTime(), net_id.dump(), cmnh.uid(),
//        p.src.dump(), p.dest.dump(), ((TableEntry)flow_table.get(flowidx)).flowId,
//        srh.flow_header(), ((TableEntry)flow_table.get(flowidx)).nextHop,
//        srh.num_addrs() ? srh.dump() : "");

//        cmnh.size() += srh.size();
//        cmnh.xmit_failure_ = srh.num_addrs() ? XmitFailureCallback : 
//                             XmitFlowFailureCallback;
//        cmnh.xmit_failure_data_ = this;

        assert(srh.num_addrs()!=0 || srh.flow_timeout());
      } else {
        // old non-flowstate stuff...
        assert(p.src.equals(net_id) || !srh.flow_header());
//        cmnh.size() += srh.size();
//        NetMessage.Ip iph = p.iph;
        if (srh.route_request())
          { // broadcast forward
//            cmnh.xmit_failure_ = 0;
            iph.setNextHop(new IpOptionNextHop(NetAddress.ANY));
//            iph.setDst(NetAddress.ANY);
            
//            cmnh.addr_type() = NS_AF_ILINK;
          }
        else
          { // forward according to source route
//            cmnh.xmit_failure_ = XmitFailureCallback;
//            cmnh.xmit_failure_data_ = this;

            if (srh.get_next_addr()==null){
                iph.setNextHop(new IpOptionNextHop(iph.getDst()));
            }
            else iph.setNextHop(new IpOptionNextHop(srh.get_next_addr()));
//            cmnh.addr_type() = srh.get_next_type();
            srh.set_cur_addr(srh.cur_addr() + 1);
          } /* route_request() */
      } /* can snag for path state */

      /* put route errors at the head of the ifq somehow? -dam 4/13/98 */

      // make sure we aren't cycling packets
      
    //#ifdef notdef
//      if (ifq!=null && ifq.size() > 25){
//          trace("SIFQ %.5f _%s_ len %d",
//            JistAPI.getTime(),
//            net_id.dump(), ifq.prq_length());
    //#endif
    if(NEW_IFQ_LOGIC){
      /*
       *  If the interface queue is full, there's no sense in sending
       *  the packet.  Drop it and generate a Route Error?
       */
      /* question for the author: this seems rife with congestion/infinite loop
       * possibilities. you're responding to an ifq full by sending a rt err.
       * sounds like the source quench problem. ych 5/5/01
       */
      if(ifq!=null && ifq.isFull()) {
          xmitFailed(p.pkt, "DROP_IFQ_QFULL");
//          p.pkt = null;;
          return;
      }
    }

      // ych debugging
      assert(!srh.flow_header() || srh.num_addrs()==0 || srh.flow_timeout());
      // drc: added this, now removed
//      if (srh.addrs()[srh.cur_addr_].addr.equals(net_id.getNSAddr_t())) srh.cur_addr_++;

      RouteDsrMsg_Ns2 tmp = (RouteDsrMsg_Ns2)p.iph.getPayload();
      // off it goes!
      if (srh.route_request()) 
        { // route requests need to be jittered a bit
          JistAPI.sleep((long) (random.nextDouble()*RREQ_JITTER + delay));
          if (p.iph.getDst().toInt()==net_id.addr){
              throw new RuntimeException();
          }
          
          assert(((RouteDsrMsg_Ns2)p.iph.getPayload()).valid());
          assert(tmp.cur_addr_ !=0 );
          self.sendIpMsg(p.iph, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
//          Scheduler.instance().schedule(ll, p.pkt, 
//                         );
        }
      else
        { 
          if (p.iph.getDst().toInt()==net_id.addr){
              throw new RuntimeException();
          }
          // drc: jitter the reply too...
//          if (srh.route_reply()) JistAPI.sleep((long) (random.nextDouble()*RREQ_JITTER + delay));
          // no jitter required 
          JistAPI.sleep(delay);
          assert(((RouteDsrMsg_Ns2)p.iph.getPayload()).valid());
          assert(tmp.cur_addr_ !=0 );
          self.sendIpMsg(p.iph, Constants.NET_INTERFACE_DEFAULT, iph.getNextHop()!=null? 
                  new MacAddress( iph.getNextHop().toInt() ): MacAddress.ANY  );
//          Scheduler.instance().schedule(ll, p.pkt, delay);
        }
//      p.pkt = null; /* packet sent off */
    }

    private void
    getRouteForPacket(SRPacket p, boolean retry)
      /* try to obtain a route for packet
         pkt is freed or handed off as needed, unless retry == true
         in which case it is not touched */
    {
      // since we'll commonly be only one hop away, we should
      // arp first before route discovery as an optimization...

      Entry e = request_table.getEntry(p.dest);
      long time = JistAPI.getTime();

    //#if 0
      /* pre 4/13/98 logic -dam removed b/c it seemed more complicated than
         needed since we're not doing piggybacking and we're returning
         route replies via a reversed route (the copy in this code is
         critical if we need to piggyback route replies on the route request to
         discover the return path) */

      /* make the route request packet */
//      SRPacket rrp = p;
//      rrp.pkt = p.pkt.copy();
//      RouteDsrMsg_NS2 srh = RouteDsrMsg_NS2.access(rrp.pkt);
//      NetMessage.Ip iph; // = hdr_ip.access(rrp.pkt);
//      hdr_cmn cmh =  hdr_cmn.access(rrp.pkt);
//      //iph.daddr() = p.dest.getNSAddr_t();
//      iph.daddr() = Address.instance().create_ipaddr(p.dest.getNSAddr_t(),RT_PORT);
//      iph.dport() = RT_PORT;
//      //iph.saddr() = net_id.getNSAddr_t();
//      iph.saddr() = Address.instance().create_ipaddr(net_id.getNSAddr_t(),RT_PORT);
//      iph.sport() = RT_PORT;
//      cmnh.ptype() = PT_DSR;
//      cmnh.size() = size_;
//      cmnh.num_forwards() = 0;
    //#endif

      /* make the route request packet */
      SRPacket rrp = new SRPacket();
      rrp.dest = p.dest;
      rrp.src = net_id;
      rrp.pkt = new RouteDsrMsg_Ns2();      
      rrp.iph = p.iph;

      RouteDsrMsg_Ns2 srh = p.pkt; 
      NetMessage.Ip iph = p.iph; // = hdr_ip.access(rrp.pkt);
//      hdr_cmn cmnh =  hdr_cmn.access(rrp.pkt);
      
      rrp.iph = new NetMessage.Ip(iph.getPayload(), net_id.getNSAddr_t(), p.dest.getNSAddr_t(),
              iph.getProtocol(), iph.getPriority(), iph.getTTL());
//      iph.daddr() = Address.instance().create_ipaddr(p.dest.getNSAddr_t(),RT_PORT);
//      iph.dport() = RT_PORT;
//      iph.saddr() = Address.instance().create_ipaddr(net_id.getNSAddr_t(),RT_PORT);
//      iph.sport() = RT_PORT;
//      cmnh.ptype() = PT_DSR;
//      cmnh.size() = size_ + IP_HDR_LEN; // add in IP header
//      cmnh.num_forwards() = 0;
      
      rrp.pkt.init();


      if (BackOffTest(e, time)) {
          // it's time to start another route request cycle

if (NEW_SALVAGE_LOGIC){
          if(!p.src.equals(net_id)) {

              assert(dsr_salvage_max_requests > 0);
              assert(p.pkt!=null);

              if(e.rt_reqs_outstanding > dsr_salvage_max_requests) {
                  drop(p.pkt, DROP_RTR_NO_ROUTE);
                  p.pkt = null;;

                  // dump the route request packet we made up
//                  Packet.free(rrp.pkt);
                  rrp.pkt = null;;

                  return;
              }
          }
}

          if (dsragent_ring_zero_search) {
              // do a ring zero search
              e.last_type = LastType.LIMIT0;
              sendOutRtReq(rrp, 0);
          } else {
              // do a propagating route request right now
              e.last_type = LastType.UNLIMIT;
              sendOutRtReq(rrp, RouteDsrMsg_Ns2.MAX_SR_LEN);
          }

          e.last_arp = time;
      }  else if (LastType.LIMIT0 == e.last_type &&              
           (time - e.last_arp) > arp_timeout) {
          boolean ok = true;
          // try propagating rt req since we haven't heard back
          // from limited one
          if(NEW_SALVAGE_LOGIC){
              if (!(dsr_salvage_allow_propagating || p.src.equals(net_id))) ok = false;
          }
          if (ok){
              e.last_type = LastType.UNLIMIT;
              sendOutRtReq(rrp, RouteDsrMsg_Ns2.MAX_SR_LEN);
          }
      }
      else {
          // it's not time to send another route request...
//          if (!retry && verbose_srr)
//              trace("SRR %.5f _%s_ RR-not-sent %s . %s", 
//                JistAPI.getTime(), 
//                net_id.dump(), rrp.src.dump(), rrp.dest.dump());
//          Packet.free(rrp.pkt); // dump the route request packet we made up
          rrp.pkt = null;;
      }

      /* for now, no piggybacking at all, queue all pkts */
      if (!retry) {
          SRPacket p_copy = new SRPacket(p.iph, p.pkt);
          p_copy.route = p.route;
          p_copy.dest = p.dest;
          p_copy.src = p.src;          
          stickPacketInSendBuffer(p_copy);
          p.pkt = null;; // pkt is handled for now (it's in sendbuffer) 
      }

    }

    private void
    sendOutRtReq(SRPacket p, int max_prop)
      // turn p into a route request and launch it, max_prop of request is
      // set as specified
      // p.pkt is freed or handed off
    {
      RouteDsrMsg_Ns2 srh =  p.pkt;
      assert(srh.valid());

      srh.setRouteRequest(true);
      srh.set_rtreq_seq( route_request_num++);
      srh.set_max_propagation( max_prop);
      p.route.reset();
      p.route.appendToPath(net_id);

      if (dsragent_propagate_last_error && route_error_held 
          && JistAPI.getTime() - route_error_data_time  < max_err_hold)
        {
          assert(srh.num_route_errors() < RouteDsrMsg_Ns2.MAX_ROUTE_ERRORS);
          srh.set_route_error(true);
          if ((srh.down_links()[srh.num_route_errors()])==null){
              (srh.down_links()[srh.num_route_errors()]) = new link_down();
          }
          link_down deadlink = (srh.down_links()[srh.num_route_errors()]);
          deadlink.addr_type = NS_AF_INET;
          deadlink.from_addr = err_from.getNSAddr_t();
          deadlink.to_addr = err_to.getNSAddr_t();
          deadlink.tell_addr = GRAT_ROUTE_ERROR;
          srh.set_num_route_errors(srh.num_route_errors() + 1);
          /*
           * Make sure that the Route Error gets on a propagating request.
           */
          if(max_prop > 0) route_error_held = false;
        }
/*
      if (verbose_srr){
        trace("SRR %.5f _%s_ new-request %d %s #%d . %s", 
          JistAPI.getTime(), net_id.dump(), 
          max_prop, p.src.dump(), srh.rtreq_seq(), p.dest.dump());
      }*/
      sendOutPacketWithRoute(p, false, 0);
    }

    private void
    returnSrcRouteToRequestor(SRPacket p)
      // take the route in p, add us to the end of it and return the
      // route to the sender of p
      // doesn't free p.pkt
    {
      RouteDsrMsg_Ns2 old_srh = p.pkt;

      if (p.route.full()) 
        return; // alas, the route would be to long once we add ourselves

      SRPacket p_copy = new SRPacket();
      p_copy.pkt = new RouteDsrMsg_Ns2();
      p_copy.dest = p.src;
      p_copy.src = net_id;
      p_copy.route = p.route.copy();

      p_copy.route.appendToPath(net_id);
      RouteDsrMsg_Ns2 new_srh =  p_copy.pkt;

      NetMessage.Ip new_iph =  new NetMessage.Ip(new_srh, p_copy.src.getNSAddr_t(), 
              p_copy.dest.getNSAddr_t(), 
              Constants.NET_PROTOCOL_DSR_NS2, Constants.NET_PRIORITY_NORMAL, (short)255); // =  hdr_ip.access(p_copy.pkt);
//      //new_iph.daddr() = p_copy.dest.addr;
//      new_iph.daddr() = Address.instance().create_ipaddr(p_copy.dest.getNSAddr_t(),RT_PORT);
//      new_iph.dport() = RT_PORT;
//      //new_iph.saddr() = p_copy.src.addr;
//      new_iph.saddr() =
//        Address.instance().create_ipaddr(p_copy.src.getNSAddr_t(),RT_PORT); 
//      new_iph.sport() = RT_PORT;
//      new_iph.setTTL(255);

      p_copy.iph = new_iph;
      new_srh.init();
      for (int i = 0 ; i < p_copy.route.len ; i++){
          new_srh.reply_addrs()[i] = new sr_addr();
        p_copy.route.get(i).fillSRAddr(new_srh.reply_addrs()[i]);
      }
      new_srh.set_route_reply_len(p_copy.route.length());
      new_srh.set_route_reply(true);

      // propagate the request sequence number in the reply for analysis purposes
      new_srh.set_rtreq_seq( old_srh.rtreq_seq()) ;
      
//      hdr_cmn new_cmnh =  hdr_cmn.access(p_copy.pkt);
//      new_cmnh.ptype() = PT_DSR;
//      new_cmnh.size() = IP_HDR_LEN;

//      if (verbose_srr)
//        trace("SRR %.9f _%s_ reply-sent %s . %s #%d (len %d) %s",
//          JistAPI.getTime(), net_id.dump(),
//          p_copy.src.dump(), p_copy.dest.dump(), old_srh.rtreq_seq(),
//          p_copy.route.length(), p_copy.route.dump());

      // flip the route around for the return to the requestor, and 
      // cache the route for future use
      p_copy.route.reverseInPlace();
      route_cache.addRoute(p_copy.route, JistAPI.getTime(), net_id);

      p_copy.route.resetIterator();
      p_copy.route.fillSR(new_srh);
      
      // drc added
      if (p_copy.route.len > 1 && p_copy.iph.getSrc().toInt()==p_copy.route.get(p_copy.route.cur_index).addr){
          p_copy.route.cur_index = 1;
          new_srh.set_cur_addr(1);
      }
      p_copy.iph.setNextHop(new IpOptionNextHop(p_copy.route.get(p_copy.route.cur_index).getNSAddr_t()));
//      new_cmnh.size() += new_srh.size();
      
      /* we now want to jitter when we first originate route replies, since
         they are a transmission we make in response to a broadcast packet 
         -dam 4/23/98
         sendOutPacketWithRoute(p_copy, true); */
      {
          double d = random.nextDouble()*(RREQ_JITTER) + RREQ_JITTER;
    //#if 0
//          //System.err.printf( "Random Delay: %f\n", d);
    //#endif
          JistAPI.sleep((long)d);
          assert(((RouteDsrMsg_Ns2)p_copy.iph.getPayload()).valid());
//          self.peek(p_copy.iph, new MacAddress((int)net_id.addr));
          if (p_copy.iph.getDst().toInt()==net_id.addr){
              throw new RuntimeException();
          }
          self.sendIpMsg(p_copy.iph, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
//          Scheduler.instance().schedule(this, p_copy.pkt, d);
          // T-ODO drc: this implies being sent through DSR's receive... should I do this?
      }
    }

    /*boolean
    diff_subnet(ID dest, ID myid) 
    {
        long dst = dest.addr;
        long id = myid.addr;
        String dstnet = Address.instance().get_subnetaddr(dst);
        String  subnet = Address.instance().get_subnetaddr(id);
        if (subnet != null) {
            if (dstnet != null) {
                if (strcmp(dstnet, subnet) != 0) {
                    delete [] dstnet;
                    return true;
                }
                delete [] dstnet;
            }
            delete [] subnet;
        }
        assert(dstnet == null);
        return false;
    }*/


    private void
    acceptRouteReply(SRPacket p)
      /* - enter the packet's source route into our cache
         - see if any packets are waiting to be sent out with this source route
         - doesn't free the pkt */
    {
//      RouteDsrMsg_NS2 srh =  p.pkt
        // T-ODO drc: what is this?
      Path reply_route = new Path(p.pkt.reply_addrs(), p.pkt.route_reply_len());

      if (!p.pkt.route_reply())
        { // somethings wrong...
          //*trace("SDFU non route containing packet given to acceptRouteReply");*/
          //System.err.printf(
          //    "dfu: non route containing packet given to acceptRouteReply\n");
        }

      boolean good_reply = true;  
      /*if( USE_GOD_FEEDBACK ){
      // check to see if this reply is valid or not using god info 
      int i;
      
      /*for (i = 0; i < reply_route.length()-1 ; i++) 
        if (God.instance().hops(reply_route[i].getNSAddr_t(), 
                      reply_route[i+1].getNSAddr_t()) != 1)
          {
        good_reply = false;
        break;
          }
    }*/

//      if (verbose_srr)
//        trace("SRR %.9f _%s_ reply-received %d from %s  %s #%d . %s %s",
//          JistAPI.getTime(), net_id.dump(),
//          good_reply ? 1 : 0,
//          p.src.dump(), reply_route[0].dump(), srh.rtreq_seq(),
//          reply_route[reply_route.length()-1].dump(),
//          reply_route.dump());

      // add the new route into our cache
      route_cache.addRoute(reply_route, JistAPI.getTime(), p.src);

      // back down the route request counters
      Entry e = request_table.getEntry(reply_route.get(
              reply_route.length()-1));
      e.rt_reqs_outstanding = 0;
      e.last_rt_req = 0;  

      // see if the addtion of this route allows us to send out
      // any of the packets we have waiting
      long delay = 0;
      ID dest;
      for (int c = 0; c < SEND_BUF_SIZE; c++)
        {
          if (send_buf[c].p == null || send_buf[c].p.pkt == null) continue;

          // check if pkt is destined to outside domain
          if (diff_subnet(send_buf[c].p.dest,net_id)) {
        dest = new ID(net_id.addr,ID_Type.IP);
        send_buf[c].p.dest = dest;
          }

          if (route_cache.findRoute(send_buf[c].p.dest, send_buf[c].p.route, true))
        { // we have a route!
    //#ifdef DEBUG
//          hdr_cmn ch = HDR_CMN(send_buf[c].p.pkt);
//          if(ch.size() < 0) {
//            drop(send_buf[c].p.pkt, "X-XX");
//            abort();
//          }
    //#endif
//          if (verbose)
//            trace("Sdebug %.9f _%s_ liberated from sendbuf %s.%s %s",
//              JistAPI.getTime(), net_id.dump(),
//              send_buf[c].p.src.dump(), send_buf[c].p.dest.dump(), 
//              send_buf[c].p.route.dump());
          /* we need to spread out the rate at which we send packets
             in to the link layer to give ARP time to complete.  If we
             dump all the packets in at once, all but the last one will
             be dropped.  X-XX THIS IS A MASSIVE HACK -dam 4/14/98 */
          sendOutPacketWithRoute(send_buf[c].p, true, delay);
          delay += arp_timeout; 
          send_buf[c].p.pkt = null;
        }
        }
    }

    private void
    processUnknownFlowError(SRPacket p, boolean asDefault) {
      RouteDsrMsg_Ns2 srh = p.pkt;
      int flowidx = -1;
      flow_error fe, fe2;
      Int flowid = new Int();
      boolean skip_proc = false;

      if (asDefault) {
        assert (srh.flow_default_unknown() && srh.num_default_unknown()!=0);
        fe = srh.unknown_defaults()[srh.num_default_unknown()-1];
//        fe2 = srh.unknown_defaults()[srh.num_default_unknown()-2];
      } else {
        assert (srh.flow_unknown() && srh.num_flow_unknown()!=0);
        fe = srh.unknown_flows()[srh.num_flow_unknown()-1];
//        fe2 = srh.unknown_flows()[srh.num_flow_unknown()-2];
        if (!flow_table.defaultFlow(fe.flow_src, fe.flow_dst, flowid))
          skip_proc = true;
      }

      if (!skip_proc){
      /* not for us; hope it gets the right place... */
      if (fe.flow_src.toInt() != (int) net_id.addr)
        return;

      if (-1 != (flowidx = flow_table.find(fe.flow_src, fe.flow_dst, 
                           asDefault ? flowid : new Int(fe.flow_id))))
        ((TableEntry)flow_table.get(flowidx)).count = 0;
      }

//      trace("SFEr %.9f _%s_ from %d re %d : %d [%d]",
//        JistAPI.getTime(), net_id.dump(), p.src.addr, fe.flow_dst,
//        asDefault ? -1 : fe.flow_id, 
//        flowidx != -1 ? ((TableEntry)flow_table.get(flowidx)).count : -1);

      if ((asDefault ? srh.num_default_unknown() : srh.num_flow_unknown()) == 1)
        return;

      SRPacket p_copy = p;
      p_copy.pkt = p.pkt.copy();

      RouteDsrMsg_Ns2 new_srh = p_copy.pkt;
      NetMessage.Ip new_iph;// = hdr_ip.access(p_copy.pkt);
      
      // remove us from the list of errors
      if (asDefault)
        new_srh.set_num_default_unknown(new_srh.num_default_unknown()-1);
      else
        new_srh.set_num_flow_unknown(new_srh.num_flow_unknown()-1);
      
      // send the packet to the person listed in what's now the last entry
      p_copy.dest = new ID(srh.unknown_flows()[srh.num_flow_unknown()-1].flow_src, ID_Type.IP);
      p_copy.src = net_id;

      //new_iph.daddr() = p_copy.dest.addr;
//      new_iph.daddr() = Address.instance().create_ipaddr(p_copy.dest.getNSAddr_t(),RT_PORT);
//      new_iph.dport() = RT_PORT;
//      //new_iph.saddr() = p_copy.src.addr;
//      new_iph.saddr() = Address.instance().create_ipaddr(p_copy.src.getNSAddr_t(),RT_PORT);
//      new_iph.sport() = RT_PORT;
//      new_iph.setTTL(255);

      new_srh.set_flow_header(false);
      new_srh.set_flow_timeout(false);

      // an error packet is a first class citizen, so we'll
      // use handlePktWOSR to obtain a route if needed
      handlePktWithoutSR(p_copy, false);
    }

    private void
    processBrokenRouteError(SRPacket p)
//     take the error packet and proccess our part of it.
//     if needed, send the remainder of the errors to the next person
//     doesn't free p.pkt
    {
      RouteDsrMsg_Ns2 srh = p.pkt;

      if (!srh.route_error())
        return; // what happened??
      
      /* if we hear A.B is dead, should we also run the link B.A through the
         cache as being dead, since 802.11 requires bidirectional links 
          X-XX -dam 4/23/98 */

      // since CPU time is cheaper than network time, we'll process
      // all the dead links in the error packet
      assert(srh.num_route_errors() > 0);
      for (int c = 0 ; c < srh.num_route_errors() ; c++)
        {
//          assert(srh.down_links()[c].addr_type == NS_AF_INET);
          route_cache.noticeDeadLink(new ID(srh.down_links()[c].from_addr.toInt(),ID_Type.IP),
                     new ID(srh.down_links()[c].to_addr.toInt(),ID_Type.IP),
                     JistAPI.getTime());
          flow_table.noticeDeadLink(new ID(srh.down_links()[c].from_addr.toInt(),ID_Type.IP),
                     new ID(srh.down_links()[c].to_addr.toInt(),ID_Type.IP));
          // I'll assume everything's of type NS_AF_INET for the printout... X-XX
//          if (verbose_srr)
//            trace("SRR %.9f _%s_ dead-link tell %d  %d . %d",
//                  JistAPI.getTime(), net_id.dump(),
//                  srh.down_links()[c].tell_addr,
//                  srh.down_links()[c].from_addr,
//                  srh.down_links()[c].to_addr);
        }

      ID who = new ID(srh.down_links()[srh.num_route_errors()-1].tell_addr, ID_Type.IP);
      if (!who.equals(net_id) && 
    		  !who.equals(MAC_id))
        { // this error packet wasn't meant for us to deal with
          // since the outer entry doesn't list our name
          return;
        }

      // record this route error data for possible propagation on our next
      // route request
      route_error_held = true;
      err_from = new ID(srh.down_links()[srh.num_route_errors()-1].from_addr,ID_Type.IP);
      err_to = new ID(srh.down_links()[srh.num_route_errors()-1].to_addr,ID_Type.IP);
      route_error_data_time = JistAPI.getTime();

      if (1 == srh.num_route_errors())
        { // this error packet has done its job
          // it's either for us, in which case we've done what it sez
          // or it's not for us, in which case we still don't have to forward
          // it to whoever it is for
          return;
        }

      /* make a copy of the packet and send it to the next tell_addr on the
         error list.  the copy is needed in case there is other data in the
         packet (such as nested route errors) that need to be delivered */
//      if (verbose) 
//        trace("Sdebug %.5f _%s_ unwrapping nested route error",
//              JistAPI.getTime(), net_id.dump());
      
      SRPacket p_copy = new SRPacket(p.iph, p.pkt);
      p_copy.src = p.src;
      p_copy.dest = p.dest;
      p_copy.route = p_copy.route.copy();
      p_copy.pkt = p.pkt.copy();

      RouteDsrMsg_Ns2 new_srh = p_copy.pkt;
      NetMessage.Ip new_iph = new NetMessage.Ip(new_srh, p_copy.src.getNSAddr_t(), 
              p.dest.getNSAddr_t(), Constants.NET_PROTOCOL_DSR_NS2, 
              Constants.NET_PRIORITY_NORMAL, 255);
      p_copy.iph = new_iph;// = hdr_ip.access(p_copy.pkt);
      
      // remove us from the list of errors
      new_srh.set_num_route_errors(new_srh.num_route_errors()-1);
      
      // send the packet to the person listed in what's now the last entry
      p_copy.dest = new ID(new_srh.down_links()[new_srh.num_route_errors()-1].tell_addr, ID_Type.IP);
      if (p_copy.dest.equals(net_id)) return;
      p_copy.src = net_id;

      //new_iph.daddr() = p_copy.dest.addr;
//      new_iph.daddr() = Address.instance().create_ipaddr(p_copy.dest.getNSAddr_t(),RT_PORT);
//      new_iph.dport() = RT_PORT;
//      //new_iph.saddr() = p_copy.src.addr;
//      new_iph.saddr() = Address.instance().create_ipaddr(p_copy.src.getNSAddr_t(),RT_PORT);
//      new_iph.sport() = RT_PORT;
//      new_iph.setTTL(255);

      new_srh.set_flow_header(false);
      new_srh.set_flow_timeout(false);
          
      // an error packet is a first class citizen, so we'll
      // use handlePktWOSR to obtain a route if needed
      handlePktWithoutSR(p_copy, false);
    }



//     Process flow state Automatic Route Shortening
    private void
    processFlowARS(final NetMessage.Ip packet) {
      
      RouteDsrMsg_Ns2 srh = (RouteDsrMsg_Ns2) packet.getPayload();
      NetMessage.Ip iph = packet; // = hdr_ip.access(packet);
//      hdr_cmn cmh = hdr_cmn.access(packet);
      //RouteDsrMsg_NS2  srh = (RouteDsrMsg_NS2*) ((Message )packet).access(off_sr_);
      //hdr_ip  iph = (hdr_ip*) ((Message )packet).access(off_ip_);
      //hdr_cmn cmh =  (hdr_cmn*)((Message )packet).access(off_cmn_);
      Int flowid = new Int();
      int flowidx;
      int shortamt;

      assert(srh.num_addrs()==0);

      if (srh.flow_header()) {
        flowid.set( srh.flow_id());

        // do I know about this flow?
        if (-1 == (flowidx = flow_table.find(iph.getSrc(), iph.getDst(), flowid)))
          return;

        shortamt = ((TableEntry)flow_table.get(flowidx)).hopCount - srh.hopCount();
      } else {
        // do I know which flow is default?
        if (!flow_table.defaultFlow(iph.getSrc(), iph.getDst(), flowid))
          return;

        // do I know about this flow?
        if (-1 == (flowidx = flow_table.find(iph.getSrc(), iph.getDst(), flowid)))
          return;

        shortamt = iph.getTTL() - ((TableEntry)flow_table.get(flowidx)).expectedTTL;
      }

      // transmitter downstream from us
      if (shortamt <= 0)
        return;

      // this is a _MAJOR_ problem!!!
      if (((TableEntry)flow_table.get(flowidx)).sourceRoute.length() < shortamt)
        return;
// T-ODO drc: reimplement this
      ars_table.insert(iph.getId(), flowid, shortamt);
    }

//    void 
//    tap(final Message packet)
//      /* process packets that are promiscously listened to from the MAC layer tap
//      *** do not change or free packet *** */
//    {
//
//    }

    static GratReplyHoldDown 
    FindGratHoldDown(GratReplyHoldDown hd[], int sz, Path query)
    {
      int c;
      for (c = 0; c < sz; c++)
        if (query == hd[c].p) return hd[c];
      return null;
    }

    private void
    sendRouteShortening(SRPacket p, int heard_at, int xmit_at)
      // p was overheard at heard_at in it's SR, but we aren't supposed to
      // get it till xmit_at, so all the nodes between heard_at and xmit_at
      // can be elided.  Send originator of p a gratuitous route reply to 
      // tell them this.
    {
      // this shares code with returnSrcRouteToRequestor - factor them -dam */

      if (!dsragent_send_grat_replies) return;

      //if (verbose)
        //*trace("Sdebug %s consider grat arp for %s", net_id.dump(), p.route.dump());*/
      GratReplyHoldDown g = FindGratHoldDown(grat_hold, RTREP_HOLDOFF_SIZE, 
                          p.route);
      if (g==null)
        { 
          grat_hold[grat_hold_victim].p = p.route;
          grat_hold_victim = (grat_hold_victim + 1) % RTREP_HOLDOFF_SIZE;
          g = grat_hold[grat_hold_victim];      
        }
      else if (JistAPI.getTime() - g.t < grat_hold_down_time) return;
      g.t = JistAPI.getTime();

      SRPacket p_copy = p;
      p_copy.pkt = new RouteDsrMsg_Ns2();
      p_copy.dest = p.route.path[0];   // tell the originator of this long source route
      p_copy.src = net_id;

      // reverse the route to get the packet back
      p_copy.route.path[p_copy.route.index()] = net_id;
      p_copy.route.reverseInPlace();
      p_copy.route.removeSection(0,p_copy.route.index());

      NetMessage.Ip iph; // =  hdr_ip.access(p_copy.pkt);
      NetMessage.Ip new_iph = new NetMessage.Ip(p_copy.pkt, p_copy.src.getNSAddr_t(), 
              p_copy.dest.getNSAddr_t(), Constants.NET_PROTOCOL_DSR_NS2, 
              Constants.NET_PRIORITY_NORMAL, 255);
      p_copy.iph = new_iph;
      //new_iph.daddr() = p_copy.dest.addr;
//      new_iph.daddr() = Address.instance().create_ipaddr(p_copy.dest.getNSAddr_t(),RT_PORT);
//      new_iph.dport() = RT_PORT;
//      //new_iph.saddr() = p_copy.src.addr;
//      new_iph.saddr() = Address.instance().create_ipaddr(p_copy.src.getNSAddr_t(),RT_PORT);
//      new_iph.sport() = RT_PORT;
//      new_iph.setTTL(255);

      // shorten's p's route
      p.route.removeSection(heard_at, xmit_at);
      RouteDsrMsg_Ns2 new_srh =  p_copy.pkt;
      new_srh.init();
      for (int i = 0 ; i < p.route.length() ; i++){
          new_srh.reply_addrs()[i] = new sr_addr();
        p.route.get(i).fillSRAddr(new_srh.reply_addrs()[i]);
      }
      new_srh.set_route_reply_len(p.route.length());
      new_srh.set_route_reply(true);
      // grat replies will have a 0 seq num (it's only for trace analysis anyway)
      new_srh.set_rtreq_seq(0);

//      hdr_cmn new_cmnh =  hdr_cmn.access(p_copy.pkt);
//      new_cmnh.ptype() = PT_DSR;
//      new_cmnh.size() += IP_HDR_LEN;

      if (verbose_srr){
        /*trace("SRR %.9f _%s_ gratuitous-reply-sent %s . %s (len %d) %s",
          JistAPI.getTime(), net_id.dump(),
          p_copy.src.dump(), p_copy.dest.dump(), p.route.length(), 
          p.route.dump());*/
      }

      // cache the route for future use (we learned the route from p)
      route_cache.addRoute(p_copy.route, JistAPI.getTime(), p.src);
      sendOutPacketWithRoute(p_copy, true, 0);
    }

    /*==============================================================
      debug and trace output
    ------------------------------------------------------------*/
//    void
//    trace(String fmt, ...)
//    {
//      va_list ap;
//      
//      if (!logtarget) return;
//
//      va_start(ap, fmt);
//      vsprintf(logtarget.pt_.buffer(), fmt, ap);
//      logtarget.pt_.dump();
//      va_end(ap);
//    }


    /*==============================================================
      Callback for link layer transmission failures
    ------------------------------------------------------------*/
//     X-XX Obviously this structure and FilterFailure() is not used anywhere, 
//     because off_cmn_ in this structure cannot be populated at all!
//     Instead of deleting, I'm simply commenting them out, perhaps they'll be 
//     salvaged sometime in the future. - haoboy

//      filterfailuredata {
//        NetAddress dead_next_hop;
//        int off_cmn_;
//        RouteDSR_NS2 agent;
//      };

//      int
//      FilterFailure(Message p, void data)
//      {
//        filterfailuredata ffd = (filterfailuredata *) data;
//        hdr_cmn cmh = (hdr_cmn*)p.access(ffd.off_cmn_);
//        int remove = cmh.next_hop() == ffd.dead_next_hop;

//        if (remove)
//            ffd.agent.undeliverablePkt(p,1);
//        return remove;
//      }

    private void
    undeliverablePkt(NetMessage.Ip pkt, boolean mine)
      /* when we've got a packet we can't deliver, what to do with it? 
         frees or hands off p if mine = 1, doesn't hurt it otherwise */
    {
      RouteDsrMsg_Ns2 srh = (RouteDsrMsg_Ns2) pkt.getPayload();
      NetMessage.Ip iph = pkt; // = hdr_ip.access(pkt);
//      hdr_cmn cmh;

      SRPacket p = new SRPacket(iph, srh);
      //p.dest = new ID(iph.dst(),IP);
      //p.src = new ID(iph.src(),IP);
      p.dest = new ID(iph.getDst().toInt(),ID_Type.IP);
      p.src = new ID(iph.getSrc().toInt(),ID_Type.IP);
      p.pkt = mine ? (RouteDsrMsg_Ns2)pkt.getPayload() : 
          ((RouteDsrMsg_Ns2)pkt.getPayload()).copy();

      srh = p.pkt;
      iph = p.iph.copy();
//      cmh = hdr_cmn.access(p.pkt);

      // we're about to salvage. flowstate rules say we must strip all flow
      // state info out of this packet. ych 5/5/01
//      cmh.size() -= srh.size(); // changes affect size of header...
      srh.set_flow_timeout(false);
      srh.set_flow_header(false);
//      cmh.size() += srh.size(); // done fixing flow state headers

      
      if (iph.getSrc().toInt() == net_id.addr) {
        // it's our packet we couldn't send
//        cmh.size() -= srh.size(); // remove size of SR header
//        assert(cmh.size() >= 0);
        
        handlePktWithoutSR(p, false);
        
        return;
      }

      /*
       * Am I allowed to salvage?
       */
      if(!dsragent_salvage_with_cache) {
          assert(mine);
          drop(srh, DROP_RTR_NO_ROUTE);  
          return;
      }

    if( NEW_SALVAGE_LOGIC) {
        // T-ODO change type to int
      if((srh.salvaged()?1:0) >= dsr_salvage_max_attempts) {
          assert(mine);
          drop(srh, DROP_RTR_SALVAGE);
          return;
      }
    }

      // it's a packet we're forwarding for someone, save it if we can...
      Path salvage_route = new Path();
          
      if (route_cache.findRoute(p.dest, salvage_route, false)) {
          // be nice and send the packet out
//    /#if 0
//          /* we'd like to create a ``proper'' source route with the
//             IP src of the packet as the first node, but we can't actually 
//             just append the salvage route onto the route used so far, 
//             since the append creates routes with loops in them 
//             like  1 2 3 4 3 5 
//             If we were to squish the route to remove the loop, then we'd be
//             removing ourselves from the route, which is verboten.
//             If we did remove ourselves, and our salvage route contained
//             a stale link, we might never hear the route error.
//             -dam 5/13/98
//
//             Could we perhaps allow SRs with loops in them on the air?
//             Since it's still a finite length SR, the pkt can't loop
//             forever... -dam 8/5/98 */
//
//          // truncate the route at the bad link and append good bit
//          int our_index = p.route.index();
//
//          p.route.setLength(our_index);
//          // yes this cuts us off the route,
//
//          p.route.appendPath(salvage_route);
//          // but we're at the front of s_r
//          p.route.setIterator(our_index);
//    #else
          p.route = salvage_route;
          p.route.resetIterator();
    //#endif

          if (dsragent_dont_salvage_bad_replies && srh.route_reply()) {
              // check to see if we'd be salvaging a packet
              // with the dead link in it

              ID to_id;
              if (srh.cur_addr()+1==srh.num_addrs()){
                  to_id = new ID(iph.getDst().toInt(), ID_Type.IP);
              }
              else to_id = new ID(srh.addrs()[srh.cur_addr()+1].addr,
                   srh.addrs()[srh.cur_addr()].addr_type);
              boolean bad_reply = false;

              for (int i = 0 ; i < srh.route_reply_len()-1; i++) {

                  if (net_id.equals(new ID(srh.reply_addrs()[i])) &&
                      to_id == new ID(srh.reply_addrs()[i+1]) ||
                      (dsragent_require_bi_routes &&
                       to_id == new ID(srh.reply_addrs()[i]) &&
                       net_id.equals(new ID(srh.reply_addrs()[i+1])))) {
                          
                      bad_reply = true;
                      break;
                  }
              }
              if (bad_reply) {
                  // think about killing this packet
                  srh.set_route_reply(false);
                  if ( // PT_DSR == cmh.ptype() && drc: this is already true
                      ! srh.route_request() &&
                      ! srh.route_error()) {
                      // this packet has no reason to live
//                      if (verbose_srr)
//                          trace("SRR %.5f _%s_ --- %d dropping bad-reply %s . %s", 
//                            JistAPI.getTime(), net_id.dump(), 
//                            cmh.uid(), p.src.dump(), p.dest.dump());
                      if (mine)
                          drop(srh, DROP_RTR_MAC_CALLBACK);
                      return;
                  }
              }
          }

          /* if (verbose_ssalv) 
              trace("Ssalv %.5f _%s_ salvaging %s . %s --- %d with %s",
                JistAPI.getTime(), net_id.dump(),
                p.src.dump(), p.dest.dump(),
                cmh.uid(), p.route.dump());*/

          // remove size of SR header, added back in sendOutPacketWithRoute
//          cmh.size() -= srh.size(); 
//          assert(cmh.size() >= 0);
          // T-ODO update
          if( NEW_SALVAGE_LOGIC)
             srh.set_salvaged(true);

//          p.pkt.valid_ = true; // T-ODO drc: is this ok?
          sendOutPacketWithRoute(p, false, 0);
      }

      else if(NEW_SALVAGE_LOGIC && dsr_salvage_max_requests > 0) {
          /*
           * Allow the node to perform route discovery for an
           * intermediate hop.
           */
         /* if (verbose_ssalv) 
              trace("Ssalv %.5f _%s_ adding to SB --- %d %s . %s [%d]", 
                JistAPI.getTime(), 
                net_id.dump(),
                cmh.uid(),
                p.src.dump(), p.dest.dump(),
                srh.salvaged()); */
          stickPacketInSendBuffer(p);
      }
    //#endif
      else {
          // we don't have a route, and it's not worth us doing a
          // route request to try to help the originator out, since
          // it might be counter productive
          /*if (verbose_ssalv) 
              trace("Ssalv %.5f _%s_ dropping --- %d %s . %s [%d]", 
                JistAPI.getTime(), 
                net_id.dump(), cmh.uid(),
                p.src.dump(), p.dest.dump(),
                srh.salvaged()); */
          if (mine)
              drop(p.pkt, DROP_RTR_NO_ROUTE);
      }
    }

    // T-ODO drc: put this up top
    //#ifdef USE_GOD_FEEDBACK
    // static int linkerr_is_wrong = 0;
    //#endif

    private void
    sendUnknownFlow(SRPacket p, boolean asDefault, Int flowid) {
      RouteDsrMsg_Ns2 srh = p.pkt;
      NetMessage.Ip iph = p.iph; //  = hdr_ip.access(p.pkt);
//      hdr_cmn cmh = hdr_cmn.access(p.pkt);
      flow_error fe = null;

      assert(srh.num_addrs()==0); // flow forwarding basis only.
    /*#if 0
      // this doesn't always hold true; if an xmit fails, we'll dump the
      // thing from our flow table, possibly before we even get here (though how
      // we found out, other than from this packet, is anyone's guess, considering
      // that underliverablePkt() should have been called in any other circumstance,
      // so we shouldn't go through the failed stuff.
      assert(p.src.equals(net_id)); // how'd it get here if it were?

      // this doesn't always hold true; I may be sending it default, fail,
      // the flow times out, but I still know the flowid (whacked paths through
      // the code, I know... ych 5/7/01
      assert(srh.flow_header() ^ asDefault); // one or the other, not both
    //#endif */

      if (p.src.equals(net_id)) {
        // Packet.free(p.pkt);
        p.pkt = null;;
        return; // gimme a break, we already know!
      }

      undeliverablePkt(iph, false); // salvage, but don't molest.
     
      /* warp into an error... */
      if (asDefault) {
        if (!srh.flow_default_unknown()) {
          srh.set_num_default_unknown(1);
          srh.set_flow_default_unknown(true);
          fe = srh.unknown_defaults()[0];
          if (fe == null) {
              srh.unknown_defaults()[0] = new flow_error();
              fe = srh.unknown_defaults()[0];
          }
        } else if (srh.num_default_unknown() < RouteDsrMsg_Ns2.MAX_ROUTE_ERRORS) {
          fe = srh.unknown_defaults()[srh.num_default_unknown()];
          if (fe == null) {
              srh.unknown_defaults()[srh.num_default_unknown()] = new flow_error();
              fe = srh.unknown_defaults()[srh.num_default_unknown()];
          }
          srh.set_num_default_unknown(srh.num_default_unknown()+1);
        } else {
//          trace("SYFU  %.5f _%s_ dumping maximally nested Flow error %d . %d",
//          JistAPI.getTime(), net_id.dump(), p.src.addr, p.dest.addr);

          // Packet.free(p.pkt);        // no drop needed
          p.pkt = null;;
          return;
        }
      } else {
        if (!srh.flow_unknown()) {
          srh.set_num_flow_unknown(1);
          srh.set_flow_unknown(true);
          fe = srh.unknown_flows()[0];
          if (fe == null) {
              srh.unknown_defaults()[0] = new flow_error();
              fe = srh.unknown_defaults()[0];
          }
        } else if (srh.num_default_unknown() < RouteDsrMsg_Ns2.MAX_ROUTE_ERRORS) {
          fe = srh.unknown_flows()[ srh.num_flow_unknown()];
          if (fe == null) {
              srh.unknown_defaults()[srh.num_default_unknown()] = new flow_error();
              fe = srh.unknown_defaults()[srh.num_default_unknown()];
          }
          srh.set_num_flow_unknown(srh.num_flow_unknown()+1);
        } else {
//          trace("SYFU  %.5f _%s_ dumping maximally nested Flow error %d . %d",
//          JistAPI.getTime(), net_id.dump(), p.src.addr, p.dest.addr);

          // Packet.free(p.pkt);        // no drop needed
          p.pkt = null;
          return;
        }
      }

//      trace("SFErr %.5f _%s_ %d . %d : %d",
//        JistAPI.getTime(), net_id.dump(), p.src.addr, p.dest.addr,
//        flowid);

      srh.set_route_reply(false);
      srh.set_route_request(false);
      srh.set_flow_header(false);
      srh.set_flow_timeout(false);

      p.iph = new NetMessage.Ip(p.pkt, p.src.getNSAddr_t(), p.dest.getNSAddr_t(),
             Constants.NET_PROTOCOL_DSR_NS2, Constants.NET_PRIORITY_NORMAL, 255);
      //iph.daddr() = p.src.addr;
//      iph.daddr() = Address.instance().create_ipaddr(p.src.getNSAddr_t(),RT_PORT);
//      iph.dport() = RT_PORT;
//      //iph.saddr() = net_id.addr;
//      iph.saddr() = Address.instance().create_ipaddr(net_id.getNSAddr_t(),RT_PORT);
//      iph.sport() = RT_PORT;
//      iph.setTTL(255);

      //fe.flow_src = p.src.addr;
      fe.flow_src = p.src.getNSAddr_t();
      //fe.flow_dst = p.dest.addr;
      fe.flow_dst = p.dest.getNSAddr_t();
      fe.flow_id  = (short) flowid.get();

      //p.src = new ID(iph.src(), IP);
      //p.dest = new ID(iph.dst(), IP);
      p.dest = new ID(iph.getDst(),ID_Type.IP);
      p.src = new ID(iph.getSrc(),ID_Type.IP);


//      cmh.ptype() = PT_DSR;                // cut off data
//      cmh.size() = IP_HDR_LEN;
//      cmh.num_forwards() = 0;
      // assign this packet a new uid, since we're sending it
      
      // T-ODO drc: ignoring for now
//      cmh.uid() = uidcnt_++;

      handlePktWithoutSR(p, false);
//      assert(p.pkt == null);
    }

    private void 
    xmitFlowFailed(Message pkt, final String reason)
    {
        NetMessage.Ip iph = (NetMessage.Ip)pkt;
        RouteDsrMsg_Ns2 srh = (RouteDsrMsg_Ns2) iph.getPayload();
       // = hdr_ip.access(pkt);
//      hdr_cmn cmh = hdr_cmn.access(pkt);
      int flowidx = flow_table.find(iph.getSrc(), iph.getDst(), new Int(srh.flow_id()));
      Int default_flow = new Int();

      assert(srh.num_addrs()==0);

      if (!srh.flow_header()) {
       if (!flow_table.defaultFlow(iph.getSrc(), iph.getDst(), default_flow)) {
          SRPacket p = new SRPacket(iph, srh);
          //p.src = new ID(iph.src(), IP);
          //p.dest = new ID(iph.dst(), IP);
          p.dest = new ID(iph.getDst(),ID_Type.IP);
          p.src = new ID(iph.getSrc(),ID_Type.IP);


          sendUnknownFlow(p, true, new Int());
          return;
        }
        flowidx = flow_table.find(iph.getSrc(), iph.getDst(), default_flow);
      }

      if (flowidx == -1 || 
          ((TableEntry)flow_table.get(flowidx)).timeout < JistAPI.getTime()) {
        // blah, the flow has expired, or been forgotten.
        SRPacket p = new SRPacket(iph, srh);
        //p.src = new ID(iph.src(), IP);
        //p.dest = new ID(iph.dst(), IP);
        p.dest = new ID(iph.getDst(),ID_Type.IP);
        p.src = new ID(iph.getSrc(),ID_Type.IP);


        return;
      }

//      cmh.size() -= srh.size(); // gonna change the source route size
//      assert(cmh.size() >= 0);
      
      ((TableEntry)flow_table.get(flowidx)).sourceRoute.fillSR(srh);
      srh.set_cur_addr(((TableEntry)flow_table.get(flowidx)).hopCount);
      assert(srh.addrs()[srh.cur_addr()].addr.toInt() == net_id.addr);
//      cmh.size() += srh.size();

      // xmitFailed is going to assume this was incr'ed for send
      srh.set_cur_addr(srh.cur_addr()+1);
      xmitFailed(pkt, reason);
    }

    private void 
    xmitFailed(Message pkt, final String reason)
      /* mark our route cache reflect the failure of the link between
         srh[cur_addr] and srh[next_addr], and then create a route err
         message to send to the orginator of the pkt (srh[0])
         p.pkt freed or handed off */
    {

      NetMessage.Ip iph = (NetMessage.Ip)pkt; // = hdr_ip.access(pkt);
      RouteDsrMsg_Ns2 srh = ((RouteDsrMsg_Ns2) iph.getPayload()).copy();
//      hdr_cmn cmh = hdr_cmn.access(pkt);

//      assert(cmh.size() >= 0);

      // T-ODO why is this happening?
//      if (srh.cur_addr()>0 || !net_id.getNSAddr_t().equals(srh.addrs()[0].addr)) {
//          srh.set_cur_addr(srh.cur_addr()-1); // correct for inc already done on sending
//      }
      srh.set_cur_addr(srh.cur_addr()-1);
      if (srh.cur_addr() < 0){
          // T-ODO figure out why this is happening
          srh.set_cur_addr(0);
          //throw new RuntimeException();
      }

      if (srh.cur_addr() >= srh.num_addrs() - 1)
        {
          //*trace("SDFU: route error beyond end of source route????");*/
          //System.err.printf("SDFU: route error beyond end of source route????\n");
//          Packet.free(pkt);
          return;
        }

      if (srh.route_request())
        {
          //*trace("SDFU: route error forwarding route request????");*/
          //System.err.printf("SDFU: route error forwarding route request????\n");
//          Packet.free(pkt);
          return;
        }


      ID tell_id = new ID(srh.addrs()[0].addr,
             (int) srh.addrs()[srh.cur_addr()].addr_type);
      ID from_id = new ID(srh.addrs()[srh.cur_addr()].addr,
             (int) srh.addrs()[srh.cur_addr()].addr_type);
      ID to_id = new ID(srh.addrs()[srh.cur_addr()+1].addr,
             (int) srh.addrs()[srh.cur_addr()].addr_type);

      assert(from_id.equals(net_id) || from_id.equals(MAC_id));

//      trace("SSendFailure %.9f _%s_ %d %d %d:%d %d:%d %s.%s %d %d %d %d %s",
//        JistAPI.getTime(), net_id.dump(), 
//        cmh.uid(), cmh.ptype(),
//        iph.saddr(), iph.sport(),
//        iph.daddr(), iph.dport(),
//        from_id.dump(),to_id.dump(),
//        God.instance().hops(from_id.getNSAddr_t(), to_id.getNSAddr_t()),
//        God.instance().hops(iph.saddr(),iph.daddr()),
//        God.instance().hops(from_id.getNSAddr_t(), iph.daddr()),
//        srh.num_addrs(), srh.dump());

    //#ifdef USE_GOD_FEEDBACK
//      if (God.instance().hops(from_id.getNSAddr_t(), to_id.getNSAddr_t()) == 1)
//        { /* god thinks this link is still valid */
//          linkerr_is_wrong++;
//          trace("SxmitFailed %.5f _%s_  %d.%d god okays #%d",
//                JistAPI.getTime(), net_id.dump(),
//                from_id.getNSAddr_t(), to_id.getNSAddr_t(), linkerr_is_wrong);
//          System.err.printf(
//              "xmitFailed on link %d.%d god okays - ignoring & recycling #%d\n",
//              from_id.getNSAddr_t(), to_id.getNSAddr_t(), linkerr_is_wrong);
//          /* put packet back on end of ifq for xmission */
//          srh.cur_addr() += 1; // correct for decrement earlier in proc 
//          // make sure we aren't cycling packets
//          // also change direction in pkt hdr
//          cmh.direction() = hdr_cmn.DOWN;
//          ll.recv(pkt, (Handler) 0);
//          return;
//        }
    //#endif

      if(reason.equals("DROP_IFQ_QFULL")) {
          assert(!reason.equals("DROP_RTR_MAC_CALLBACK"));

          /* kill any routes we have using this link */
          route_cache.noticeDeadLink(from_id, to_id,
                          JistAPI.getTime());
          flow_table.noticeDeadLink(from_id, to_id);

          /* give ourselves a chance to save the packet */
          undeliverablePkt(iph.copy(), true);

          /* now kill all the other packets in the output queue that would
             use the same next hop.  This is reasonable, since 802.11 has
             already retried the xmission multiple times => a persistent
             failure. */

          /* X-XX YCH 5/4/01 shouldn't each of these packets get Route Errors
           * if one hasn't already been sent? ie if two different routes
           * are using this link?
           */
          {
              int size = ifq.size();
              MessageQueue ifq_ = ifq;
              QueuedMessage qm; // T-ODO check
              MacAddress nextHop = new MacAddress(iph.getNextHop().toInt());

              Vector unchanged = new Vector();
              Vector refactor = new Vector();
              refactor.add(new QueuedMessage(iph, nextHop)); // add dropped packet to list
              

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
              
              assert(ifq.size()==0);
              
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
              
              assert(unchanged.size()==0 || ifq.size()>0);
              
              QueuedMessage rt;
              
              // retarget all packets we got to a new next hop
              it = refactor.iterator();
              while (it.hasNext()) {
                  rt = (QueuedMessage)it.next();
                  
                  if (!(rt.getPayload() instanceof NetMessage.Ip)) {
                      // no such thing in SWANS; removing from queue is effectively dropping it
                      // no callback for this packet.
//                      drop(rt, DROP_RTR_MAC_CALLBACK);
                      continue;
                  }
                  
                  Ip ipmsg = (NetMessage.Ip)rt.getPayload();
                 
                  
                  if (ipmsg.getPayload() instanceof RouteDsrMsg_Ns2)
                  {
                      undeliverablePkt(ipmsg, true);
                          
                  } // end if DSRMessage
                  else
                  {
                      throw new RuntimeException("Found non-DSR message in queue!");
                  }
              } // end while for retargeting packets
              
              if(!(size==0 || ifq.size()>=0)){
                  throw new RuntimeException("No packets refactored!");
                  
              }
              /*
            Message r, nr, queue1 = null, queue2 = null;
            // pkts to be recycled
            
            while((r = ifq.prq_get_nexthop(to_id.getNSAddr_t()))) {
              r.next_ = queue1;
              queue1 = r; 
            }

            // the packets are now in the reverse order of how they
            // appeared in the IFQ so reverse them again
            for(r = queue1; r; r = nr) {
              nr = r.next_;
              r.next_ = queue2;
              queue2 = r;
            }

            // now process them in order
            for(r = queue2; r; r = nr) {
              nr = r.next_;
              undeliverablePkt(r, 1);
            }
            */
          }
      }
      
      /* warp pkt into a route error message */
      if (tell_id.equals(net_id) || tell_id.equals(MAC_id))
        { // no need to send the route error if it's for us
          // drc: this is bs. we need to propagate it so nearby nodes know its bogus too; of course, the way this is 
          // structured makes it difficult to do...
//          if (verbose) 
//            trace("Sdebug _%s_ not bothering to send route error to ourselves", 
//              tell_id.dump());
//          Packet.free(pkt);    // no drop needed
//          pkt = 0;
          return;
        }

      if (srh.num_route_errors() >= RouteDsrMsg_Ns2.MAX_ROUTE_ERRORS)
        { // no more room in the error packet to nest an additional error.
          // this pkt's been bouncing around so much, let's just drop and let
          // the originator retry
          // Another possibility is to just strip off the outer error, and
          // launch a Route discovey for the inner error X-XX -dam 6/5/98
//          trace("SDFU  %.5f _%s_ dumping maximally nested error %s  %d . %d",
//            JistAPI.getTime(), net_id.dump(),
//            tell_id.dump(),
//            from_id.dump(),
//            to_id.dump());
//          Packet.free(pkt);    // no drop needed
//          pkt = 0;
          return;
        }

      if ((srh.down_links()[srh.num_route_errors()])==null){
          (srh.down_links()[srh.num_route_errors()]) = new link_down();
      }
      link_down deadlink = (srh.down_links()[srh.num_route_errors()]);
      if (srh.addrs()[srh.cur_addr()].addr_type == -1){
          throw new NullPointerException();
      }
      // DRC: check to make sure that the error is not already on this packet
      if (srh.num_route_errors()>0){
          link_down[] downlinks = srh.down_links();
          for (int i = 0; i < srh.num_route_errors(); i++){
              if (downlinks[i].from_addr==srh.addrs()[srh.cur_addr()].addr && 
                      downlinks[i].to_addr==srh.addrs()[srh.cur_addr()+1].addr && 
                      downlinks[i].tell_addr==srh.addrs()[0].addr){
                  (srh.down_links()[srh.num_route_errors()]) = null; // clear it to indicate that the error already exists
              }
          }
      }
      
      if ((srh.down_links()[srh.num_route_errors()]) !=null){
          deadlink.addr_type = srh.addrs()[srh.cur_addr()].addr_type;
          deadlink.from_addr = srh.addrs()[srh.cur_addr()].addr;
          deadlink.to_addr = srh.addrs()[srh.cur_addr()+1].addr;
          deadlink.tell_addr = srh.addrs()[0].addr;
          srh.set_num_route_errors(srh.num_route_errors()+1);
      }

//      if (verbose)
//        trace("Sdebug %.5f _%s_ sending into dead-link (nest %d) tell %d  %d . %d",
//              JistAPI.getTime(), net_id.dump(),
//              srh.num_route_errors(),
//              deadlink.tell_addr,
//              deadlink.from_addr,
//              deadlink.to_addr);

      srh.set_route_error(true);
      srh.set_route_reply(false);
      srh.set_route_request(false);
      srh.set_flow_header(false);
      srh.set_flow_timeout(false);


      // drc: this all gets set in sendOutPacketWithRoute
      //iph.daddr() = deadlink.tell_addr;
//      iph.daddr() = Address.instance().create_ipaddr(deadlink.tell_addr,RT_PORT);
//      iph.dport() = RT_PORT;
      //iph.saddr() = net_id.addr;
//      iph.saddr() = Address.instance().create_ipaddr(net_id.addr,RT_PORT);
//      iph.sport() = RT_PORT;
//      iph.setTTL(255);

//      cmh.ptype() = PT_DSR;        // cut off data
//      cmh.size() = IP_HDR_LEN;
//      cmh.num_forwards() = 0;
      // assign this packet a new uid, since we're sending it
      // T-ODO drc: ignoring this for now
      //cmh.uid() = 
      //    uidcnt_++;

      SRPacket p = new SRPacket(iph, srh);
      p.route.setLength(p.route.index()+1);
      p.route.reverseInPlace();
      p.dest = tell_id;
      p.src = net_id;

      /* send out the Route Error message */
      sendOutPacketWithRoute(p, true, 0);
    }

    private void
    XmitFailureCallback(Message pkt, Object data)
    {
      RouteDsr_Ns2 agent = (RouteDsr_Ns2)data; // cast of trust
      agent.xmitFailed(pkt, "");
    }

    private void
    XmitFlowFailureCallback(Message pkt, Object data)
    {
      RouteDsr_Ns2 agent = (RouteDsr_Ns2)data;
      agent.xmitFlowFailed(pkt, "");
    }

    //#if 0

    /* this is code that implements Route Reply holdoff to prevent route 
       reply storms.  It's described in the kluwer paper and was used in 
       those simulations, but isn't currently used.  -dam 8/5/98 */

    /*==============================================================
      Callback Timers to deal with holding off  route replies

      Basic theory: if we see a node S that has requested a route to D
      send a packet to D via a route of length <= ours then don't send
      our route.  We record that S has used a good route to D by setting
      the best_length to -1, meaning that our route can't possibly do
      S any good (unless S has been lied to, but we probably can't know
      that).
      
      NOTE: there is confusion in this code as to whether the requestor
      and requested_dest ID's are MAC or IP... It doesn't matter for now
      but will later when they are not the same.

    ------------------------------------------------------------*/
    /*public class RtHoldoffData {
        RouteDSR_NS2 t;
        Message p;
        int index;
        
        RtHoldoffData(RouteDSR_NS2 th, Message pa, int ind)
      {
            t = th; 
            p=pa; index = ind;
      }
      
    }

    void
    RouteReplyHoldoffCallback(Node node, long time, EventData data)
//     see if the packet inside the data is still in the
//     send buffer and expire it if it is
    {
      Message p = ((RtHoldoffData )data).p;
      RouteDSR_NS2 t = ((RtHoldoffData )data).t;
      int index = ((RtHoldoffData )data).index;

      RtRepHoldoff entry = (t.rtrep_holdoff[index]);
      assert((entry.requestor == p.dest));

      // if we haven't heard the requestor use a route equal or better
      // than ours then send our reply.
      if ((lsnode_require_use && entry.best_length != -1)
          || (!lsnode_require_use && entry.best_length > entry.our_length))
        { // we send
          world_statistics.sendingSrcRtFromCache(t,time,p);
          t.sendPacket(t,time,p);
        }
      else
        { // dump our packet
          delete p;
        }
      entry.requestor = invalid_addr;
      entry.requested_dest = invalid_addr;
      delete data;
      t.num_heldoff_rt_replies--;
    }

    void
    scheduleRouteReply(long t, Message new_p)
      // schedule a time to send new_p if we haven't heard a better
      // answer in the mean time.  Do not modify new_p after calling this
    {
      for (int c = 0; c < RTREP_HOLDOFF_SIZE; c ++)
        if (rtrep_holdoff[c].requested_dest == invalid_addr) break;
      assert(c < RTREP_HOLDOFF_SIZE);

      Path our_route = (new_p.data.getRoute().source_route);
      rtrep_holdoff[c].requested_dest = (*our_route)[our_route.length() - 1];
      rtrep_holdoff[c].requestor = new_p.dest;
      rtrep_holdoff[c].best_length = MAX_ROUTE_LEN + 1;
      rtrep_holdoff[c].our_length = our_route.length();

      long send_time = t +
        (long) (our_route.length() - 1) * rt_rep_holdoff_period
        + U(0.0, rt_rep_holdoff_period);
      RegisterCallback(this,&RouteReplyHoldoffCallback, send_time,
               new RtHoldoffData(this,new_p,c));
      num_heldoff_rt_replies++;
    }

    void
    snoopForRouteReplies(long t, Message p)
      // see if p is a route reply that we're watching for
      // or if it was sent off using a route reply we're watching for
    {
      for (int c = 0 ; c <RTREP_HOLDOFF_SIZE ; c ++)
        {
          RtRepHoldoff entry = (rtrep_holdoff[c]);

          // there is no point in doing this first check if we're always
          // going to send our route reply unless we hear the requester use one
          // better or equal to ours
          if (entry.requestor == p.dest
          && (p.type == ::route_reply || p.data.sourceRoutep()))
        { // see if this route reply is one we're watching for
          Path srcrt = (p.data.getRoute().source_route);
          if (!(entry.requested_dest == (*srcrt)[srcrt.length()-1]))
            continue;       // it's not ours
          if (entry.best_length > srcrt.length())
            entry.best_length = srcrt.length();
        } // end if we heard a route reply being sent
          else if (entry.requestor == p.src
               && entry.requested_dest == p.dest)
        { // they're using a route  reply! see if ours is better
              if (p.route.length() <= entry.our_length)
                { // Oh no! they've used a better path than ours!
                  entry.best_length = -1; //there's no point in replying.
                }
            } // end if they used used route reply
          else
            continue;
        }
    }

    //#endif //0 */



    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface#peek(jist.swans.net.NetMessage, jist.swans.mac.MacAddress)
     */
    public void peek(NetMessage msg, MacAddress lastHop) {
        
        if (ifq == null)
        {
            ifq =  netEntity.getMessageQueue(Constants.NET_INTERFACE_DEFAULT);
        }
        NetMessage.Ip iph = (Ip)msg;
        if (iph.getPayload() instanceof TransUdp.UdpMessage) return;
        
        // drc: tap code comes first
        tap(msg, lastHop);
        
        // condition for executing "receive" code
        if (iph.getDst().equals(net_id.getNSAddr_t()) || 
                iph.getNextHop()!=null && iph.getNextHop().equals(NetAddress.ANY)
                || iph.getDst().equals(NetAddress.ANY)){

            recv(msg, lastHop);
            return;
            
        } // end if for receive code
        
        

    }


    /**
     * @param msg
     * @param lastHop
     * @param iph
     * @param srh
     * @param p
     */
    private void recv(NetMessage msg, MacAddress lastHop) {
        
        NetMessage.Ip iph = (Ip)msg; // = hdr_ip.access(packet);
        RouteDsrMsg_Ns2 srh =  (RouteDsrMsg_Ns2) iph.getPayload();           
        SRPacket p = new SRPacket(iph, srh);
        // special process for GAF
        // T-ODO drc: wtf is this?
        /*if (cmh.ptype() == PT_GAF) {
          if (iph.daddr() == (int)IP_BROADCAST) { 
            if(cmh.direction() == hdr_cmn.UP)
          cmh.direction() = hdr_cmn.DOWN;
            Scheduler.instance().schedule(ll,packet,0);
            return;
          } else {
            target_.recv(packet, (Handler)0);
            return;     
          }
        }*/


        p.dest = new ID(iph.getDst().toInt(),ID_Type.IP);
        p.src = new ID(iph.getSrc().toInt(),ID_Type.IP);

//            assert(logtarget != 0);

        if (!srh.valid()) {
          int dest = iph.getNextHop()==null? 0 : iph.getNextHop().toInt();
          if (dest == IP_BROADCAST) {
            // extensions for mobileIP --Padma, 04/99.
            // Brdcast pkt - treat differently
            if (p.src.equals(net_id))
          // I have originated this pkt
          sendOutBCastPkt(new NetMessage.Ip(msg, iph.getSrc(), iph.getDst(), 
                  Constants.NET_PROTOCOL_DSR_NS2, iph.getPriority(), iph.getTTL()));
            else {
          //hand it over to port-dmux
                // Now go through some strange contortions to get this message received by
                // the proper protocol handler
                NetMessage.Ip newIp = new NetMessage.Ip(srh.getPayload(), iph.getSrc(), iph.getDst(),
                  srh.getNextHeaderType(), iph.getPriority(), iph.getTTL());   
                
                assert(newIp.getPayload() instanceof TransUdp.UdpMessage);
                notifyVisualizationEvent(PacketEvent.DataAtDest);
                netEntity.receive(newIp, lastHop, (byte)1, false);
            }
            
          } else {
            // this must be an outgoing packet, it doesn't have a SR header on it
            
            srh.init();       // give packet an SR header now

//                if (verbose)
//              trace("S %.9f _%s_ originating %s . %s",
//                    JistAPI.getTime(), net_id.dump(), p.src.dump(), 
//                    p.dest.dump());
            handlePktWithoutSR(p, false);
            
            return;
          }
        }
        else if (srh.valid()) 
          {
            if (p.dest.equals(net_id) || p.dest.equals(NetAddress.ANY))
          { // this packet is intended for us
            handlePacketReceipt(p, lastHop, (byte)1);
            
            return;
          }
            
            // should we check to see if it's an error packet we're handling
            // and if so call processBrokenRouteError to snoop
            if (dsragent_snoop_forwarded_errors && srh.route_error())
          {
            processBrokenRouteError(p);
          }

            if (srh.route_request())
          { // propagate a route_request that's not for us
            handleRouteRequest(p);
          }
            else
          { // we're not the intended final recpt, but we're a hop
            handleForwarding(p);
          }
          }
        else {
          // some invalid pkt has reached here
          //System.err.printf("dsragent: Error-received Invalid pkt!\n");
          // Packet.free(p.pkt);
//              p.pkt =null; // drop silently
            throw new RuntimeException("dsragent: Error-received Invalid pkt!");
        }

//            assert(p.pkt == null);
        
//            p.pkt = null;
        return;
    }

    /**
     * @param msg
     * @param lastHop
     */
    private void tap(NetMessage msg, MacAddress lastHop) {
        
        NetMessage.Ip iph = (Ip)msg; // = hdr_ip.access(packet);
        if (!(iph.getPayload() instanceof RouteDsrMsg_Ns2)) return;
        RouteDsrMsg_Ns2 srh =  (RouteDsrMsg_Ns2) iph.getPayload();           
        SRPacket p = new SRPacket(iph, srh);
        
        if (!dsragent_use_tap) return;
        if (!srh.valid()) return;
        
        if (srh.num_addrs()==0) {
            processFlowARS(iph);
            return;
          }
        
//      no route shortening on any
        // DSR packet
//        hdr_cmn cmh =  hdr_cmn.access(packet);
        
        if (!dsragent_use_tap) return;

        if (!srh.valid()) return;    // can't do anything with it

        if (srh.num_addrs()==0) {
          processFlowARS((Ip) msg);
          return;
        }

        // don't trouble me with packets I'm about to receive anyway
        /* this change added 5/13/98 -dam */
        if (srh.num_addrs_<srh.cur_addr_){        
            ID next_hop = new ID(srh.addrs()[srh.cur_addr()]);
            if (next_hop.equals(net_id) || next_hop.equals(MAC_id)) return;
        }

        //p.dest = new ID(iph.dst(),IP);
        //p.src = new ID(iph.src(),IP);
        p.dest = new ID(iph.getDst().toInt(),ID_Type.IP);
        p.src = new ID(iph.getSrc().toInt(),ID_Type.IP);

        // don't trouble me with my own packets
        if (p.src.equals(net_id)) return; 

      //#ifdef DSR_FILTER_TAP
        /* 
         * Don't process packets more than once.  In real implementations
         * this can be done with the (IP Source, IP ID) pair, but it is
         * simpler to implement it with the global "uid" in simulation.
         */
        {
                int uid = iph.getId();
                if(ipIds.contains(new Integer(uid))) {
//                dsr_tap_skip++;
                        return;
            }
//            dsr_tap++;
                ipIds.add(new Integer(uid));
        }
      //#endif

        /* snoop on the SR data */
        if (srh.route_error())
          {
//            if (verbose)
          //*trace("Sdebug _%s_ tap saw error %d",  net_id.dump(), cmh.uid());*/
            processBrokenRouteError(p);
          }

        if (srh.route_reply())
          {
            Path reply_path = new Path(srh.reply_addrs(), srh.route_reply_len());
//            if(verbose)
//          trace("Sdebug _%s_ tap saw route reply %d  %s",
//                 net_id.dump(), cmh.uid(), reply_path.dump());
            route_cache.noticeRouteUsed(reply_path, JistAPI.getTime(), 
                         p.src);
          } 

        /* we can't decide whether we should snoop on the src routes in 
           route requests.  We've seen cases where we hear a route req from a
           node, but can't complete an arp with that node (and so can't actually
           deliver packets through it if called on to do so) -dam 4/16/98 */

        if (srh.route_request()){
            
            return; // don't path shorten route requests
        }
        // the logic is wrong for shortening rtreq's anyway, cur_addr always = 0

        if (dsragent_snoop_source_routes)
          {
//            if (verbose)
//          trace("Sdebug _%s_ tap saw route use %d %s", net_id.dump(), 
//                cmh.uid(), p.route.dump());
            route_cache.noticeRouteUsed(p.route, JistAPI.getTime(), 
                         net_id);
          }


        /* I think we ended up sending grat route replies for source routes on 
           route replies for route requests that were answered by someone else's
           cache, resulting in the wrong node receiving the route.  For now, I 
           outlaw it.

           The root of the problem is that when we salvage a pkt from a failed
           link using a route from our cache, we break what had been an invariant
           that the IP src of a packet was also the first machine listed on the
           source route.  Here's the route of the problem that results in the 
           simulator crashing at 8.56135 when 44 recieves a route reply that
           has 24 listed as the first node in the route.

      SSendFailure 8.52432 24 [10 |24 46 45 1 40 ]
      S$hit 8.52432 salvaging 10 . 40 with [(24) 44 50 9 40 ]
      S$hit 8.52432 salvaging 44 . 40 with [(24) 44 50 9 40 ]
      D 8.52432 [20 42 2e 18 800] 24 DSR 156 -- 10.40 6 [0] [1 9 39] [0 0 0.0]
      s 8.52438 [1b 45e 2c 18 0] 24 MAC 20
      r 8.52446 [1b 45e 2c 18 0] 44 MAC 20
      s 8.52454 [101b 27e 23 1b 0] 27 MAC 20
      s 8.52564 [101b 27e 23 1b 0] 27 MAC 20
      s 8.52580 [101b 45e 2c 18 0] 24 MAC 20
      r 8.52588 [101b 45e 2c 18 0] 44 MAC 20
      s 8.52589 [1c 41c 18 0 0] 44 MAC 14
      r 8.52595 [1c 41c 18 0 0] 24 MAC 14
      s 8.52600 [20 42 2c 18 800] 24 DSR 244 -- 10.40 5 [0] [1 9 39] [0 0 24.46]
      r 8.52698 [20 42 2c 18 800] 44 DSR 216 -- 10.40 5 [0] [1 9 39] [0 0 24.46]

      s 8.53947 [20 42 2c 18 800] 24 DSR 204 -- 44.40 5 [0] [1 8 39] [0 0 0.0]
      r 8.54029 [20 42 2c 18 800] 44 DSR 176 -- 44.40 5 [0] [1 8 39] [0 0 0.0]
      Sdebug 50 consider grat arp for [24 (44) 50 9 40 ]
      SRR 8.54029 50 gratuitous-reply-sent 50 . 44 [24 (50) 9 40 ]
      SF 8.54029 44 [44 . 40] via 0x3200 [24 |44 50 9 40 ]
      s 8.54030 [1d 0 18 0 0] 44 MAC 14
      r 8.54036 [1d 0 18 0 0] 24 MAC 14
      s 8.54044 [101b 54f 32 2c 0] 44 MAC 20
      r 8.54053 [101b 54f 32 2c 0] 50 MAC 20
      s 8.54054 [1c 50d 2c 0 0] 50 MAC 14
      r 8.54059 [1c 50d 2c 0 0] 44 MAC 14
      s 8.54064 [20 42 32 2c 800] 44 DSR 304 -- 10.40 5 [0] [1 9 39] [0 0 24.46]
      r 8.54186 [20 42 32 2c 800] 50 DSR 276 -- 10.40 5 [0] [1 9 39] [0 0 24.46]
      SF 8.54186 50 [10 . 40] via 0x900 [24 44 |50 9 40 ]

      s 8.56101 [20 42 2c 18 800] 24 DSR 84 -- 50.44 2 [0] [1 4 40] [0 0 0.0]
      r 8.56135 [20 42 2c 18 800] 44 DSR 56 -- 50.44 2 [0] [1 4 40] [0 0 0.0]

      */


        /* check to see if we can shorten the route being used */
        if (p.route.index() < p.route.length() && !p.route.get(p.route.index()).equals(net_id)
            && !p.route.get(p.route.index()).equals(MAC_id))
          { // it's not immeadiately to us
            for (int i = p.route.index() + 2; i < p.route.length(); i++)
          if (p.route.get(i).equals(net_id) || p.route.get(i).equals(MAC_id))
            { // but it'll get here eventually...
              sendRouteShortening(p, p.route.index(), i);
            }
          }
        
        
    }


    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface#send(jist.swans.net.NetMessage)
     */
    public void send(NetMessage msg) {
        Ip ipMsg = (Ip)msg;
        RouteDsrMsg_Ns2 srh;
        
        if (ipMsg.getPayload() instanceof TransUdp.UdpMessage){
            srh = new RouteDsrMsg_Ns2();
            srh.setPayload( ipMsg.getPayload());
            srh.setNextHeaderType(ipMsg.getProtocol());
            ipMsg = new NetMessage.Ip(srh, ipMsg.getSrc(), ipMsg.getDst(), 
                    Constants.NET_PROTOCOL_DSR_NS2, ipMsg.getPriority(), ipMsg.getTTL());
//            ipMsg.setPayload(srh);
//            srh.setValid(true);
            
            
            
        }
        else{
//            srh = (RouteDsrMsg_NS2) ipMsg.getPayload();
//            srh.setNextHeaderType(ipMsg.getProtocol());
            
        }
        recv(ipMsg, MacAddress.NULL);
//        SRPacket p = new SRPacket(ipMsg, srh);
//        //p.dest = new ID(iph.dst(),IP);
//        //p.src = new ID(iph.src(),IP);
//        p.dest = new ID(ipMsg.getDst().toInt(),ID_Type.IP);
//        p.src = new ID(ipMsg.getSrc().toInt(),ID_Type.IP);
//        handlePktWithoutSR(p, false);
        
    }

    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface#packetDropped(jist.swans.misc.Message, jist.swans.mac.MacAddress)
     */
    public void packetDropped(Message packet, MacAddress packetNextHop) {
        xmitFailed(packet, "DROP_IFQ_QFULL");
        
    }

    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface#forwardMessages(jist.swans.net.NetInterface.NetHandler)
     */
    public void forwardMessages(NetHandler handler) {
        // T-ODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface#unForwardMessages()
     */
    public void unForwardMessages() {
        // T-ODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see jist.swans.net.NetInterface.NetHandler#receive(jist.swans.misc.Message, jist.swans.net.NetAddress, jist.swans.mac.MacAddress, byte, jist.swans.net.NetAddress, byte, int)
     */
    public void receive(Message msg, NetAddress src, MacAddress lastHop, 
            byte macId, NetAddress dst, byte priority, byte ttl) {
        /* handle packets with a MAC destination address of this host, or
        the MAC broadcast addr */
   
        /* moved content to peek, since that is really where it should 
         * go, according to the above comment.         
        */
        
        // T-ODO drc: figure out what to do with internal calls to receive...
        recv(new NetMessage.Ip(msg, src, dst, Constants.NET_PROTOCOL_DSR_NS2, priority, ttl), lastHop);
        
    }

    /**
     * @param dataAtDest
     */
    private void notifyVisualizationEvent(int e) {
        if (Visualizer.getActiveInstance()!=null){
            if (e== PacketEvent.DataAtDest){
                VisualizerInterface v = Visualizer.getActiveInstance();
                v.setGeneralPaneText("Received: "+ ++receivedDataAtDest );
            }
        }
        
    }


    /* (non-Javadoc)
     * @see jist.swans.misc.Protocol#start()
     */
    public void start() {
        send_buf_timer = new SendBufferTimer(this, (long) (BUFFER_CHECK 
                + BUFFER_CHECK * random.nextDouble()));
        send_buf_timer.sched((long) (BUFFER_CHECK 
                + BUFFER_CHECK * random.nextDouble()));  
        
    }

    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface.Geo#timeout(java.lang.Long, long, int)
     */
    public void timeout(Long id, long interval, int timerId) {
        if (interval < 0) throw new RuntimeException("Invalid sleep interval!");
        
        JistAPI.sleep(interval);
        
        NS2Timer t = (NS2Timer)pendingTimers.remove(id);
        t.timeout(timerId);
        
    }

    /* (non-Javadoc)
     * @see jist.swans.route.RouteInterface.Geo#sendIpMsg(jist.swans.net.NetMessage.Ip, int, jist.swans.mac.MacAddress)
     */
    public void sendIpMsg(Ip msg, int ni, MacAddress ma) {
        assert(msg.getDst().toInt()!=net_id.addr);
        assert(msg.getProtocol()==Constants.NET_PROTOCOL_DSR_NS2);
//        if (net_id.addr == msg.getSrc().toInt() && net_id.addr == 161){
//        	System.out.println("Blah!");
//        }
        
        visualizePacketType(msg);
        
        netEntity.send(msg, ni, ma);
        
        
    }
    
    /**
     * Visualizes the messgages.
     * @param msg the message to visualize
     */
    private void visualizePacketType(Ip msg) {
        if (Visualizer.getActiveInstance()!=null && Visualizer.getActiveInstance().showCommunication()){
            VisualizerInterface v = Visualizer.getActiveInstance();
            if (msg.getPayload() instanceof RouteDsrMsg_Ns2){
                RouteDsrMsg_Ns2 dsrMsg = (RouteDsrMsg_Ns2) msg.getPayload();
                if (dsrMsg.route_error()) v.drawAnimatedTransmitCircle((int) net_id.addr, Color.RED);
                if (dsrMsg.route_reply()){ 
                    v.drawAnimatedTransmitCircle((int) net_id.addr, Color.GREEN);
                    v.resetColors();
                    v.setNodeColor(msg.getSrc().toInt(), Color.RED);
                    for (int i = 0; i < dsrMsg.route_reply_len(); i++){
                        v.setNodeColor(dsrMsg.reply_addrs()[i].addr.toInt(), Color.GREEN);
                    }
                    if (v.isStep()){
                        v.pause();
                        while (v.isPaused()){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                // T-ODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if (dsrMsg.route_request()){
                    v.drawAnimatedTransmitCircle((int) net_id.addr, Color.YELLOW);
                    v.resetColors();
                    v.setNodeColor(msg.getSrc().toInt(), Color.YELLOW);
                    v.setNodeColor(msg.getDst().toInt(), Color.BLUE);
                }
                if (dsrMsg.getPayload() instanceof TransUdp.UdpMessage) {
                    v.drawAnimatedTransmitCircle((int) net_id.addr, Color.BLUE);
                    v.resetColors();
                    for (int i = 0; i < dsrMsg.num_addrs_; i++){
                        v.setNodeColor( dsrMsg.addrs()[i].addr.toInt(), Color.GREEN);
                    }
                    v.setNodeColor(msg.getDst().toInt(), Color.BLUE);
                }
            }
        }
        
    }


    private boolean diff_subnet(ID dest, ID myid) 
    {
        return false;
        // T-ODO drc: figure out why this is useful and do a real implemenation
        /*int dst = dest.addr;
        int id = myid.addr;
        char* dstnet = Address::instance().get_subnetaddr(dst);
        char * subnet = Address::instance().get_subnetaddr(id);
        if (subnet != NULL) {
            if (dstnet != NULL) {
                if (strcmp(dstnet, subnet) != 0) {
                    delete [] dstnet;
                    return true;
                }
                delete [] dstnet;
            }
            delete [] subnet;
        }
        assert(dstnet == NULL);
        return false;*/
    }


    /**
     * @param proxy
     */
    public void setNetEntity(NetInterface proxy) {
        netEntity = proxy;
//        ifq = netEntity.getMessageQueue(Constants.NET_INTERFACE_DEFAULT);
        
    }


    /**
     * @param radio2
     */
    public void setRadio(RadioNoise radio) {
        this.radio = radio;
        
    }


    /**
     * @return
     */
    public Dsr_NS2 getProxy() {
        // T-ODO Auto-generated method stub
        return self;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
//    @Override
    public String toString() {
        return "Ip: " + net_id.getNSAddr_t();
    }


    /**
     * Returns the local address
     * @return
     */
	public NetAddress getLocalAddr() {
		return localAddr;
	}


	public int getProtocolId() throws Continuation {
		return Constants.NET_PROTOCOL_DSR_NS2;
	}
    
    


}
