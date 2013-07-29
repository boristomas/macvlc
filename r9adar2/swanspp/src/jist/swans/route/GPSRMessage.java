package jist.swans.route;

import java.util.Vector;

import jist.swans.mac.MacAddress;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.route.RouteGPSR.PerimeterEntry;
import jist.swans.trans.TransUdp;

/** GPSR special message type (i.e., the header)  */
public class GPSRMessage implements Message {
    static final int PERI_DEFAULT_HOPS = 32;	// default max number of hops in peri header
    
    static final int GPSRH_DATA_GREEDY = 0;	// greedy mode data packet
    static final int GPSRH_DATA_PERI = 1;	// perimeter mode data packet
    static final int GPSRH_PPROBE = 2	;	// perimeter probe packet
    static final int GPSRH_BEACON = 3;		// liveness beacon packet
    
    //public static int offset_;
    /** contains PerimeterEntries  */
    public Vector hops_;
    public PerimeterEntry peript_;
    public NetAddress periptip_[] = new NetAddress[3];
    public int maxhops_;
    public int nhops_;
    public int currhop_;
    public int mode_;
    
    Message payload;
    short protocol;
    
    public NetAddress dest; /** final destination */
    
    /** information for using mobility in determining positions 
     * TODO multiply bearing by speed and save a word 
     */
    public float speed;
    public Location bearing;
    
    public GPSRMessage(Message payload, short protocol, NetAddress dst, 
    		NetAddress src) {
        this(src);
        if (payload instanceof GPSRMessage) this.payload = ((GPSRMessage)payload).getPayload();
        else this.payload = payload;
        this.protocol = protocol;
        this.dest = dst;
    }
    
    public GPSRMessage(NetAddress netAddr) {
        hops_ = new Vector(PERI_DEFAULT_HOPS);
        maxhops_ = PERI_DEFAULT_HOPS;
        nhops_ = 0;    
        currhop_ = 0;
        this.peript_ = new PerimeterEntry(null, null, null); 
        for (int i =0; i < periptip_.length; i++) periptip_[i] = netAddr;
    }
    
    public Message getPayload()
    {
        return payload;
    }
    
    // header size is source route + int mode, int src rt len, int src rt ptr
    //      public int size() { return (nhops_ * 12 + 3 * sizeof(int)); }
    //      public static hdr_gpsr access(Packet p) {
    //        return (hdr_gpsr) p->access(offset_);
    //      }
    public void add_hop(NetAddress addip, Location l, MacAddress macAddr) {
        PerimeterEntry pe = new PerimeterEntry(l, addip, macAddr);
        if (hops_.size()==0  && nhops_ > 0)
        {
            hops_.add(null);
            hops_.add(pe);
            nhops_ = 1;
        }
        else
        {
            hops_.add(pe);
            if (hops_.size() > nhops_)nhops_++;
        }

    }
    
    /* (non-Javadoc)
     * @see jist.swans.misc.Message#getSize()
     */
    public int getSize() {
        int payloadSize = 0;
        if (payload != null) payloadSize = payload.getSize();
        return (4+(hops_.size()*(4*4))+4+4+4+payloadSize+(4*4)+4);
    }
    
    /* (non-Javadoc)
     * @see jist.swans.misc.Message#getBytes(byte[], int)
     */
    public void getBytes(byte[] msg, int offset) {
        // TODO actually get some bytes from this header
        payload.getBytes(msg, offset);
        
    }

    /**
     * 
     */
    public void resetHops() {
        if (nhops_ == 0) hops_.removeAllElements();
        else if (nhops_ == 1 ) 
        {
            while (hops_.size() > 1)
            {
                hops_.remove(1);
            }
        }
        else
        {
            throw new RuntimeException("Eek!");
            
        }
        
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     *
     */
    public String toString() {
        if (this.getPayload() instanceof TransUdp.UdpMessage) 
            return getPayload().getClass().toString();
        else {
        	String output = this.getClass().toString();
        	switch (mode_){
        	case GPSRH_BEACON: 
        		output+= "$Beacon";
        		break;
        	case GPSRH_PPROBE: 
        		output+= "$PeriProbe";
        		break;
        	default:
        		output=getPayload().getClass().toString();
        		break;
        	}
        	return output;
        	
        }
    }

	@Override
	protected Object clone() throws CloneNotSupportedException {
		GPSRMessage gpsrm = new GPSRMessage(payload, protocol, dest, periptip_[0]);
		for (int i = 0; i < hops_.size(); i++ ){			
			gpsrm.hops_.add(hops_.get(i));
		}
		gpsrm.bearing = bearing;
		gpsrm.currhop_ = currhop_;
		gpsrm.mode_ = mode_;
		gpsrm.nhops_ = nhops_;
		gpsrm.speed = speed;
		gpsrm.peript_ = peript_;
		return gpsrm;
	}
}