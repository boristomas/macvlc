/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         RouteDsrMsg_NS2.java
 * RCS:          $Id: RouteDsrMsg_Ns2.java,v 1.1 2007/04/09 18:49:29 drchoffnes Exp $
 * Description:  RouteDsrMsg_NS2 class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      May 8, 2006 at 9:11:44 AM
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

import jist.swans.misc.Message;
import jist.swans.misc.Pickle;
import jist.swans.net.NetAddress;
import jist.swans.net.NetAddressIpFactory;
import jist.swans.net.NetMessage;
import jist.swans.route.RouteDsr_Ns2.ID;
import jist.swans.route.RouteDsr_Ns2.Path;
import jist.swans.trans.TransUdp;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The RouteDsrMsg_NS2 class ...
 */
public class RouteDsrMsg_Ns2 implements Message, Pickle.Serializable {
    
	public final String[] fieldNames = {"valid_", "salvaged_", "num_addrs_", "cur_addr_", "addrs_", "sr_request_",
			"sr_reply_", "sr_error_", "sr_flow_", "sr_ftime_", "sr_funk_", "sr_fdef_unk", "payload", "nextHeaderType"};
	
    public static final int MAX_SR_LEN  = 16;       // longest source route we can handle
    public static final int MAX_ROUTE_ERRORS = 3;
    private static final int SR_HDR_SZ = 4;
    
    boolean valid_;     /* is this header actually in the packet? 
    and initialized? */
    boolean salvaged_;  /* packet has been salvaged? */
    
    int num_addrs_;
    int cur_addr_;
    sr_addr addrs_[] = new sr_addr[MAX_SR_LEN];
    
    route_request    sr_request_;
    route_reply  sr_reply_;
    route_error  sr_error_;
    
    flow_header      sr_flow_;
    flow_timeout sr_ftime_;
    flow_unknown sr_funk_;
    flow_default_err sr_fdef_unk;
    
    Message payload;
    
    // Removed "private" declaration
    short nextHeaderType;
    
    public static int offset_; 
    
    public static class SRPacket implements Message, Pickle.Serializable {
        
    	public final String[] fieldNames = {"dest", "src", "pkt", "route", "iph"};
    	
        ID dest;
        ID src;
        RouteDsrMsg_Ns2 pkt;            /* the inner NS packet */
        Path route;
        NetMessage.Ip iph;
        
        public SRPacket(NetMessage.Ip iph, RouteDsrMsg_Ns2 srh){
            pkt = srh;
            this.iph = iph;
            route = new Path(srh);
            
        }
        
        public SRPacket() {
            pkt = null;
            route = new Path();
        }

        /* (non-Javadoc)
         * @see jist.swans.misc.Message#getSize()
         */
        public int getSize() {
            return pkt.size()+route.len*4 + 8;
        }

        /* (non-Javadoc)
         * @see jist.swans.misc.Message#getBytes(byte[], int)
         */
        public void getBytes(byte[] msg, int offset) {
            // T-ODO Auto-generated method stub
            
        }
      
    }
    
    public RouteDsrMsg_Ns2(){
        for (int i = 0; i < MAX_SR_LEN; i++) addrs_[i] = new sr_addr();
        
        
        sr_request_ = new route_request();
          sr_reply_ = new route_reply();
          sr_error_ = new route_error();
        
              sr_flow_ = new flow_header();
         sr_ftime_ = new flow_timeout();
         sr_funk_ = new flow_unknown();
         sr_fdef_unk = new flow_default_err();
    }
    
    public static class sr_addr implements Pickle.Serializable {
        
    	public final String[] fieldNames = {"addr", "addr_type"};
    	
    	public int addr_type = -1;      /* same as hdr_cmn in packet.h */
        public NetAddress addr = null;

    /*
     * Metrics that I want to collect at each node
     */
    public double  Pt_;

    public sr_addr() {
        
    }
    /**
     * @param sr_addr
     */
    public sr_addr(sr_addr sr_addr_) {
        if (sr_addr_.addr==null) this.addr = null;
        else this.addr = NetAddressIpFactory.getAddress(sr_addr_.addr.toInt());
        this.addr_type = sr_addr_.addr_type;
        this.Pt_ = sr_addr_.Pt_;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
//    @Override
    public String toString() {
        return addr !=null ? addr.toString() : "null";
    }
    
    
};

    public static class link_down  implements Cloneable, Pickle.Serializable {
    	
    	public final String[] fieldNames = {"addr_type", "tell_addr", "from_addr", "to_addr"};
    	
    public int addr_type;      /* same as hdr_cmn in packet.h */
    public NetAddress tell_addr; // tell this host
    public NetAddress from_addr; // that from_addr host can no longer
    public NetAddress to_addr;   // get packets to to_addr host
};


/* ======================================================================
   DSR Packet Types
   ====================================================================== */
    public static class route_request implements Cloneable, Pickle.Serializable {
    	
    	public final String[] fieldNames = {"req_valid_", "req_id_", "req_ttl_"};
    	
        public boolean req_valid_; /* request header is valid? */
        public int req_id_;    /* unique request identifier */
        public int req_ttl_;   /* max propagation */
};

    public static class route_reply implements Cloneable, Pickle.Serializable {
    
    	public final String[] fieldNames = {"rep_valid_", "rep_rtlen_", "rep_addrs_"};
    	
    public boolean rep_valid_; /* reply header is valid? */
    public int rep_rtlen_; /* # hops in route reply */
    public sr_addr  rep_addrs_[] = new sr_addr[MAX_SR_LEN];
};

    public static class route_error implements Cloneable, Pickle.Serializable {
    	
    	public final String[] fieldNames = {"err_valid_", "err_count_", "err_links_"};
    	
        public boolean err_valid_; /* error header is valid? */
        public int err_count_; /* number of route errors */
        public link_down err_links_[] = new link_down[MAX_ROUTE_ERRORS];
};

/* ======================================================================
   DSR Flow State Draft Stuff
   ====================================================================== */

    public static class flow_error  implements Cloneable, Pickle.Serializable {
    	public final String[] fieldNames = {"flow_src", "flow_dst", "flow_id"};
    	
    NetAddress  flow_src;
    NetAddress  flow_dst;
    short flow_id;  /* not valid w/ default flow stuff */
};

public class flow_header  implements Cloneable, Pickle.Serializable {
	
	public final String[] fieldNames = {"flow_valid_", "hopCount_", "flow_id_"};
	
    boolean flow_valid_;
    int hopCount_;
    short flow_id_;
};

public class flow_timeout  implements Cloneable, Pickle.Serializable {
	
	public final String[] fieldNames = {"flow_timeout_valid_", "timeout_"};
	
    boolean flow_timeout_valid_;
    long timeout_;  // timeout in seconds...
};

public class flow_unknown  implements Cloneable, Pickle.Serializable {
	
	public final String[] fieldNames = {"flow_unknown_valid_", "err_count_", "err_flows_"};
	
    boolean flow_unknown_valid_;
    int err_count_;
    flow_error err_flows_[] = new flow_error[MAX_ROUTE_ERRORS];
};

// default flow unknown errors
public class flow_default_err  implements Cloneable, Pickle.Serializable {
	
	public final String[] fieldNames = {"flow_default_valid_", "err_count_", "err_flows_"};
	
    boolean flow_default_valid_;
    int err_count_;
    flow_error err_flows_[] = new flow_error[MAX_ROUTE_ERRORS];
};

/* ======================================================================
   DSR Header
   ====================================================================== */

        /* offset for this header */
    public int offset() { return offset_; }
    /*    public static RouteDsrMsg_NS2 access(final Message p) {
            return (RouteDsrMsg_NS2)p.access(offset_);
    }*/
    public boolean valid() { return valid_; }
    public void setValid(boolean valid) { valid_ = valid;}
    public boolean salvaged() { return salvaged_; }
    public int num_addrs() { return num_addrs_; }
    public void set_num_addrs(int na) { num_addrs_=na; }
    public int cur_addr() { return cur_addr_; }
    public void set_cur_addr(int ca){
    	cur_addr_ = ca;}
    
    public sr_addr[] addrs() { return addrs_; }

    public boolean route_request() {return sr_request_.req_valid_; }
    public void set_route_request(boolean rr){sr_request_.req_valid_=rr;};
    public int rtreq_seq() {return sr_request_.req_id_; }
    public int max_propagation() {return sr_request_.req_ttl_; }

    public boolean route_reply() {return sr_reply_.rep_valid_; }
    public void set_route_reply(boolean rr) {sr_reply_.rep_valid_=rr; }
    public int route_reply_len() {return sr_reply_.rep_rtlen_; }
    public void set_route_reply_len(int rrl) { sr_reply_.rep_rtlen_=rrl; }
    public sr_addr[] reply_addrs() {return sr_reply_.rep_addrs_; }

    public boolean route_error() {return sr_error_.err_valid_; }
    public void set_route_error(boolean re) { sr_error_.err_valid_=re; }
    public int num_route_errors() {return sr_error_.err_count_; }

    public link_down[] down_links() {return sr_error_.err_links_; }

    // Flow state stuff, ych 5/2/01
    public boolean flow_header() { return sr_flow_.flow_valid_; }
    public short flow_id() { return sr_flow_.flow_id_; }
    public int hopCount() { return sr_flow_.hopCount_; }
    public void setHopCount(int h){ sr_flow_.hopCount_ = h;}
    
    public boolean flow_timeout() { return sr_ftime_.flow_timeout_valid_; }
    public void set_flow_timeout(boolean ft) { sr_ftime_.flow_timeout_valid_=ft; }
    public long flow_timeout_time() { return sr_ftime_.timeout_; }
    public void set_flow_timeout_time(long ftt) { sr_ftime_.timeout_=ftt; }

    public boolean flow_unknown() { return sr_funk_.flow_unknown_valid_; }
    public void set_flow_unknown(boolean b) { sr_funk_.flow_unknown_valid_ = b; }
    public int num_flow_unknown() { return sr_funk_.err_count_; }
    public void set_num_flow_unknown(int i){sr_funk_.err_count_ = i;}
    public flow_error[] unknown_flows() { return sr_funk_.err_flows_; }
    
    public boolean flow_default_unknown() { return sr_fdef_unk.flow_default_valid_; }
    public void set_flow_default_unknown(boolean b) { sr_fdef_unk.flow_default_valid_ = b; }
    public int num_default_unknown() { return sr_fdef_unk.err_count_; }
    public flow_error[] unknown_defaults() { return sr_fdef_unk.err_flows_; }

    public int size() {
        int sz = 0;
        if (num_addrs_!=0 || route_request() || 
            route_reply() || route_error() ||
            flow_timeout() || flow_unknown() || flow_default_unknown())
            sz += SR_HDR_SZ;

        if (num_addrs_!=0)         sz += 4 * (num_addrs_ - 1);
        if (route_reply())      sz += 5 + 4 * route_reply_len();
        if (route_request())        sz += 8;
        if (route_error())      sz += 16 * num_route_errors();
        if (flow_timeout())     sz += 4;
        if (flow_unknown())     sz += 14 * num_flow_unknown();
        if (flow_default_unknown()) sz += 12 * num_default_unknown();

        if (flow_header())      sz += 4;

        sz = ((sz+3)&(~3)); // align...
        assert(sz >= 0);
/*#if 0
        printf("Size: %d (%d %d %d %d %d %d %d %d %d)\n", sz,
            (num_addrs_ || route_request() ||
            route_reply() || route_error() ||
            flow_timeout() || flow_unknown() || 
            flow_default_unknown()) ? SR_HDR_SZ : 0,
            num_addrs_ ? 4 * (num_addrs_ - 1) : 0,
            route_reply() ? 5 + 4 * route_reply_len() : 0,
            route_request() ? 8 : 0,
            route_error() ? 16 * num_route_errors() : 0,
            flow_timeout() ? 4 : 0,
            flow_unknown() ? 14 * num_flow_unknown() : 0,
            flow_default_unknown() ? 12 * num_default_unknown() : 0,
            flow_header() ? 4 : 0);
#endif*/

        return sz;
    }

    // End Flow State stuff

    public NetAddress get_next_addr() { 
        assert(cur_addr_ < num_addrs_);
        return (addrs_[cur_addr_ + 1].addr);
    }

    public int get_next_type() {
        assert(cur_addr_ < num_addrs_);
        return (addrs_[cur_addr_ + 1].addr_type);
    }

    public void append_addr(NetAddress a, int type) {
        assert(num_addrs_ < MAX_SR_LEN-1);
        addrs_[num_addrs_].addr_type = type;
        addrs_[num_addrs_++].addr = a;
    }

    public void init() {
        valid_ = true;
        salvaged_ = false;
        num_addrs_ = 0;
        cur_addr_ = 0;

        sr_request_ = new route_request();
        sr_reply_ = new route_reply();
        sr_error_ = new route_error();
        sr_flow_ = new flow_header();
        sr_ftime_ = new flow_timeout();
        sr_fdef_unk = new flow_default_err();
        sr_funk_ = new flow_unknown();
        /*route_reply() = 0;
        route_reply_len() = 0;
        route_error() = 0;
        num_route_errors() = 0;

        flow_timeout() = 0;
        flow_unknown() = 0;
        flow_default_unknown() = 0;
        flow_header() = 0;*/
    }
    /* (non-Javadoc)
     * @see jist.swans.misc.Message#getSize()
     */
    public int getSize() {
        return size();
    }
    /* (non-Javadoc)
     * @see jist.swans.misc.Message#getBytes(byte[], int)
     */
    public void getBytes(byte[] msg, int offset) {
        // T-ODO Auto-generated method stub
        
    }
    /**
     * @param b
     */
    public void setRouteRequest(boolean b) {
       sr_request_.req_valid_ = b;
        
    }
    /**
     * @param i
     */
    public void set_rtreq_seq(int i) {
        sr_request_.req_id_ = i;
        
    }
    /**
     * @param max_prop
     */
    public void set_max_propagation(int max_prop) {
        sr_request_.req_ttl_ = max_prop;
        
    }
    /**
     * @return Returns the payload.
     */
    public Message getPayload() {
        return payload;
    }
    /**
     * @param payload The payload to set.
     */
    public void setPayload(Message payload) {
        assert (payload instanceof jist.swans.trans.TransUdp.UdpMessage);
        this.payload = payload;
    }
    /**
     * @return
     */
    public short getNextHeaderType() {
        // T-ODO Auto-generated method stub
        return nextHeaderType;
    }
    
    public void setNextHeaderType(short nht){
        nextHeaderType = nht;
    }
    /**
     * @param i
     */
    public void set_flow_header(boolean b) {
        sr_flow_.flow_valid_ = b;
        
    }
    /**
     * @param s
     */
    public void set_flow_id(short s) {
        sr_flow_.flow_id_ = s;
        
    }
    /**
     * @param i
     */
    public void set_hopCount(int i) {
       sr_flow_.hopCount_ = i;
        
    }
    /**
     * @param i
     */
    public void set_num_route_errors(int i) {
        sr_error_.err_count_ = i;
        
    }
    /**
     * @param i
     */
    public void set_num_default_unknown(int i) {
        sr_fdef_unk.err_count_ = i;
        
    }
    /**
     * @return
     */
    public RouteDsrMsg_Ns2 copy() {
        RouteDsrMsg_Ns2 copy = new RouteDsrMsg_Ns2();
        
        copy.init();
        copy.valid_ = valid_;     /* is this header actually in the packet? 
        and initialized? */
        copy.salvaged_ = salvaged_;  /* packet has been salvaged? */
        
        copy.num_addrs_=num_addrs_;
        copy.cur_addr_ = cur_addr_;
        
        for (int i = 0; i < MAX_SR_LEN; i++) copy.addrs_[i] = new sr_addr(addrs_[i]);
        
        copy.sr_request_ = sr_request_;
        copy.sr_reply_ = sr_reply_;
        copy.sr_error_ = sr_error_;
        
        copy.sr_flow_ = sr_flow_;
        copy.sr_ftime_ = sr_ftime_;
        copy.sr_funk_=sr_funk_;
        copy.sr_fdef_unk = sr_fdef_unk;
        
        copy.payload = payload;
        copy.nextHeaderType = nextHeaderType;
        
//        copy.offset_ = offset_; 
        
        
        return copy;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
//    @Override
    public String toString() {
        String out = getClass().toString();
        if (route_error()) out += "(Error)";
       
        if (route_reply()) out += "(Reply)";
        if (route_request()) out+="(Request)";
        if (getPayload() instanceof TransUdp.UdpMessage) 
           out+="(Data)";
        return out;
    }
    /**
     * @param b
     */
    public void set_salvaged(boolean b) {
       this.salvaged_ = true;
        
    }

//#if 0
//#ifdef DSR_CONST_HDR_SZ
//  /* used to estimate the potential benefit of removing the 
//     src route in every packet */
//    public int size() { 
//        return SR_HDR_SZ;
//    }
//#else
//    public int size() { 
//        int sz = SR_HDR_SZ +
//            4 * (num_addrs_ - 1) +
//            4 * (route_reply() ? route_reply_len() : 0) +
//            8 * (route_error() ? num_route_errors() : 0);
//        assert(sz >= 0);
//        return sz;
//    }
//#endif // DSR_CONST_HDR_SZ
//#endif // 0 

//  void dump(char *);
//  char* dump();
}


