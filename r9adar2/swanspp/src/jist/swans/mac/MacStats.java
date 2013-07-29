/*
 * MacStats.java
 *
 * Created on November 5, 2004, 12:50 PM
 */

package jist.swans.mac;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;
import java.util.Iterator;
import java.util.Map.Entry;
import java.awt.Color;
import java.io.IOException;
import java.lang.String;

//import driver.Statistics;
import driver.Visualizer;

import jist.swans.field.Field;
import jist.swans.misc.Message;
import jist.swans.mac.MacMessage.Data;
import jist.swans.net.NetMessage;
import jist.swans.net.NetMessage.Ip;
import jist.swans.trans.TransUdp;
import jist.swans.trans.TransUdp.UdpMessage;
import jist.runtime.JistAPI;
import jist.swans.misc.Util;
import jist.swans.Constants;

/**
 *
 * MacStats collects stats such as latency, packet loss and general overhead.
 * @author  David Choffnes
 */
public class MacStats {
    
    public final boolean TRACK_DROPS = true;
    public final boolean TRACK_ACKS = false;
    
    /** data structure to track which sent messages were received */
    public HashMap packets;
    
    private Vector unicastLatency;
    
    private Vector broadcastLatency;
    
    public Vector ackTracker;
    
    public HashMap dropReason;
    public HashMap recv; // received packets
    
    public final Integer DROP_TTL = new Integer(0);
    public final Integer DROP_ROUTE_FAILURE = new Integer(1);
    public final Integer DROP_MAC_LAYER = new Integer(2);
    public final Integer DROP_QUEUE = new Integer(3);
    public final Integer DROP_WAITING_FOR_ROUTE = new Integer(4);
    public final Integer DROP_OTHER = new Integer(-1);
    
    /**
     * number of total messages sent
     */
    public int totalMessages = 0;
    
    /** referene to field object to calculate distances */
    public Field field;
    /** stores distances for undelivered packets */
    public Vector distances;
    
    public String packetDetails = "";
    
    /**
     * number of data messags sent
     */
    public int totalDataMessages = 0;

    /** number of retries */
    public int retries = 0;

    /** number of packets dropped due to lack of ACK */
    public int droppedAckPackets = 0;

    public int droppedCtsPackets=0;
    /** number of duplicate messages received */
    public int dupes=0;
/** number of dropped data packets */
    public int droppedDataPackets=0;

public int receivedPackets=0;

public double maxDistance;
public final boolean DISTANCE_TRACKING = false;
public int droppedInterference;


protected static HashMap droppedSignals =new HashMap();
protected static HashMap interferingSignals = new HashMap();
protected static Vector dropCounts = new Vector();
protected static HashMap hopelessSignals = new HashMap();

private static class Counter implements JistAPI.DoNotRewrite{
    int i;    
    
    public Counter(){ i=0;}
    public void increment(){++i;}
    public int value() {return i;}
}
    
    /** Creates a new instance of MacStats */
    public MacStats() 
    {
        init();
        field = null;
    }
    
    public MacStats(Field field) 
    {
        init();
        this.field = field;
    }
    
    public void init()
    {
        packets = new HashMap();
        unicastLatency = new Vector();
        broadcastLatency = new Vector();
        ackTracker = new Vector();
        distances = new Vector();
        dropReason = new HashMap();
        recv = new HashMap();
    }
    
    /**
     * add packet to list of undelivered packets
     * @param msg The mesasge entity that was sent
     */
    public void addPacket(TransUdp.UdpMessage msg)
    {
    	packets.put(new Integer(msg.id), msg);
    	if (TRACK_DROPS)updatePacketSeen(msg, DROP_OTHER);
    }
    
    /**
     * remove packet from list of undelivered packets
     *
     * caller should make sure that these are instance of Ip messages
     * and that their payloads are Udp messages, the kind we're intersted in tracking
     *
     * @param msg The message entity that was sent.
     */
    public boolean removePacket(Message msg)
    {
        TransUdp.UdpMessage rcvMsg = (TransUdp.UdpMessage)msg;
       // Data macMsg = ;
        byte bytes[] = new byte[rcvMsg.getPayload().getSize()];
        //System.out.println("macMsg.getBody() is of type " + macMsg.getBody().getClass());
        rcvMsg.getPayload().getBytes(bytes, 0);
        rcvMsg = (UdpMessage) (packets.remove(new Integer(rcvMsg.id)));
        
       
        if (rcvMsg !=null)
        {                      
            long latency = JistAPI.getTime() - ((TransUdp.UdpMessage)msg).creationTime;
            unicastLatency.add(new Long(latency));
            if (TRACK_DROPS)dropReason.remove(msg);
            return true;
        }
        else return false;
    }
    
    /**
     * add packet to list of packets that should be received by 
     * another MAC entity
     * @param msg The mesasge entity that was sent
     */
    public void addHoppingPacket(MacMessage.Data msg, float dist)
    {
    	if (TRACK_ACKS)ackTracker.add(msg);
    	if (dist > maxDistance)
    	{
    	    distances.add(new Float(dist));
    	}
    	
    }
    
    /**
     * remove packet from list of packets waiting to be received by the next hop
     *
     * @param msg The message entity that was sent.
     */
    public void removeHoppingPacket(MacMessage.Data msg)
    {       
        if (TRACK_ACKS)ackTracker.remove(msg);
    }
    
    /**
     * Check if the message waiting for an ack has already been received
     * @return
     */
    public boolean messageReceived(Message msg)
    {
        ListIterator it = ackTracker.listIterator();
        NetMessage.Ip IpMsg1;
        MacMessage.Data macMsg;
        //IpMsg2 = (NetMessage.Ip)msg;
        
        while (it.hasNext())
        {
            macMsg = (MacMessage.Data)it.next();
            IpMsg1 = (NetMessage.Ip)macMsg.getBody();
            
            
            if (msg == IpMsg1)
            {
                ackTracker.remove(macMsg);
	            //	System.out.println("Message received!");
	            return false;
            }
        }
        
        return true;
        
    }
    /**
     * Returns the average latency in seconds
     *
     * @return average latency in seconds
     */
    public double getAverageLatency()
    {
        long total = 0;
        int count = 0;
        
        for (Iterator iter = unicastLatency.iterator(); iter.hasNext();) 
        {
            total += ((Long)(iter.next())).longValue();
            count++;
        }
        
        //System.out.println("Counted " + count + " messages with latencies");
        if (count < 1) return -1;
        return total / (count*Constants.MILLI_SECOND);
    }


    /**
     * @return number of retries
     */
    public int getRetries() {
        // TODO Auto-generated method stub
        return retries;
    }
    
    public void updatePacketSeen(Message msg, Integer i)
    {
        if (TRACK_DROPS)dropReason.put(msg, i);
    }
    
    /**
     * Records types of dropped messages and the types that 
     * caused the interferece.
     * @param msg the incoming message causing interference
     * @param signalBuffer the message that arrived first
     * @param id the id of the node at source of interference
     */
    public void updateDrops(Message msg, Message signalBuffer, Integer id, 
    		int radioState) {
      StringBuffer text = new StringBuffer("Dropped:\n");
      
      if (signalBuffer instanceof MacMessage.Data){
          
          if (((MacMessage.Data)signalBuffer).getBody() instanceof NetMessage.Ip){
              Message payload = ((NetMessage.Ip)((MacMessage.Data)signalBuffer).getBody()).getPayload();
              String key = payload.toString();
              if (!key.contains("swans")) key = payload.getClass().toString();
              if (key.contains("@")) key = key.substring(key.indexOf(".")+7, key.indexOf('@'));
              else key = key.substring(key.indexOf(".")+7);
              int count = 1;
              if (!droppedSignals.containsKey(key)){
                  droppedSignals.put(key, new Integer(count));                       
              }
              else{
                  count = ((Integer)droppedSignals.remove(key)).intValue();
                  droppedSignals.put(key, new Integer(count+1));
              }

          }
      }
      

      if (msg instanceof MacMessage.Data){
          
          if (((MacMessage.Data)msg).getBody() instanceof NetMessage.Ip){
                      
        	  Message payload = ((NetMessage.Ip)((MacMessage.Data)msg).getBody()).getPayload();
              String key = payload.toString();
              if (!key.contains("swans")) key = payload.getClass().toString();
              if (key.contains("@")) key = key.substring(key.indexOf(".")+7, key.indexOf('@'));
              else key = key.substring(key.indexOf(".")+7);
              
              int count = 1;
              if (signalBuffer!=null){
	              if (!interferingSignals.containsKey(key)){
	                  interferingSignals.put(key, new Integer(count));                       
	              }
	              else{
	                  count = ((Integer)interferingSignals.remove(key)).intValue();
	                  interferingSignals.put(key, new Integer(count+1));
	              }
              }
              else{
                  String modeKey = "";
                  switch (radioState){
                  case Constants.RADIO_MODE_RECEIVING:
                	  modeKey = "Receiving";                  
                      break;
                    case Constants.RADIO_MODE_SENSING:
                  	  modeKey = "Sensing";                  
                  	  break;
                    case Constants.RADIO_MODE_TRANSMITTING:
                  	  modeKey = "Transmitting";                  
                      break;
                    case Constants.RADIO_MODE_IDLE:
                  	  modeKey = "Idle";                  
                      break;
                    case Constants.RADIO_MODE_SLEEP:
                  	  modeKey = "Sleep";                  
                    	break;
                  }
                  HashMap temp;
	              if (!hopelessSignals.containsKey(modeKey)){
	            	  temp = new HashMap();
	            	  hopelessSignals.put(modeKey, temp);                       
	              }
	              else{
	                  temp = (HashMap) hopelessSignals.get(modeKey);
	              }
	              
	              if (!temp.containsKey(key)){
	            	  temp.put(key, new Integer(count));                       
	              }
	              else{
	                  count = ((Integer)temp.remove(key)).intValue();
	                  temp.put(key, new Integer(count+1));
	              }
              }
          }
      }
      
      // track drops per unit time
      long time = JistAPI.getTime();
      int seconds = (int)Math.floor(time/Constants.SECOND);
      while (dropCounts.size()<=seconds)dropCounts.add(new Counter());
      ((Counter)dropCounts.get(seconds)).increment();

      // only visualization code after this point
      if (Visualizer.getActiveInstance()==null || !Visualizer.getActiveInstance().showText()) return;
      
      Set s = droppedSignals.entrySet();
      Iterator it = s.iterator();
      while (it.hasNext()){
          Entry ent = (Entry)it.next();
         // if (!((String)ent.getKey()).equals(""))
              text.append(ent.getKey() + "\t" + ent.getValue() +"\n");                         
      }
      
      text.append("Dropped by:\n");
      
      s = interferingSignals.entrySet();
      it = s.iterator();
      while (it.hasNext()){
          Entry ent = (Entry)it.next();          
         // if (!((String)ent.getKey()).equals(""))
              text.append(ent.getKey() + "\t" + ent.getValue() +"\n");                         
      }
      
      text.append("Unable to be received:\n");
      s = hopelessSignals.entrySet();
      it = s.iterator();
      while (it.hasNext()){
          Entry ent = (Entry)it.next();
         // if (!((String)ent.getKey()).equals(""))
              text.append(ent.getKey() + "\n");
              HashMap temp = (HashMap)ent.getValue();
              Set s2 = temp.entrySet();
              Iterator it2 = s2.iterator();
              while (it2.hasNext()){
            	  ent = (Entry)it2.next();
            	  text.append("\t" + ent.getKey() + "\t" + ent.getValue() +"\n"); 
              }
                                      
      }

      Visualizer.getActiveInstance().setInterferencePaneText(text.toString());
      if (Visualizer.SHOW_INTERFERENCE && signalBuffer!=null){
          Visualizer.getActiveInstance().addPersistentCirlce(Color.RED, 5, 
                  10, id);
      }
  }

    /**
     * @param string
     */
    public void printDropGraphData(String fname) {
        String output = "";
        for (int i = 0; i < dropCounts.size(); i++){
            output += i + "\t" + ((Counter)dropCounts.get(i)).value()+"\n";
        }
        try {
            Util.writeResult(fname, output);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @param fname
     */
    public void printUndeliveredIDs(String fname) {
        String output = "";
        TransUdp.UdpMessage  msg;
       Iterator it = packets.values().iterator();
       while (it.hasNext()){
           msg = (TransUdp.UdpMessage)it.next();
            output += msg.id+"\n";
        }
        try {
            Util.writeResult(fname, output);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    /*
    public void setStatistics(Statistics stat){
    	int droppedDataInterference=0;
    	int droppedDataOther=0;
    	int droppedControl=0;
    	
        Set s = droppedSignals.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()){
            Entry ent = (Entry)it.next();
            if (ent.getKey().toString().contains("Udp")) 
            	droppedDataInterference = ((Integer)ent.getValue()).intValue();
            else droppedControl += ((Integer)ent.getValue()).intValue();                       
        }
        
        s = hopelessSignals.entrySet();
        it = s.iterator();
        while (it.hasNext()){
            Entry ent = (Entry)it.next();

                HashMap temp = (HashMap)ent.getValue();
                Set s2 = temp.entrySet();
                Iterator it2 = s2.iterator();
                while (it2.hasNext()){
              	  ent = (Entry)it2.next();
                  if (ent.getKey().toString().contains("Udp")) 
                  	droppedDataOther = ((Integer)ent.getValue()).intValue();
                  else droppedControl += ((Integer)ent.getValue()).intValue();    
                }
                                        
        }
        stat.setDropData(droppedDataInterference, droppedDataOther, droppedControl);
    }*/
}
