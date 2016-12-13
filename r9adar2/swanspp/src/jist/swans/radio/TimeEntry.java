package jist.swans.radio;

import java.util.ArrayList;
import java.util.HashSet;

import org.python.modules.newmodule;

import jist.runtime.JistAPI;
import jist.swans.misc.Message;
import jist.swans.net.NetMessage;

/**
 * timeID:
 *	0  - //when and if message is created
 *	1  - //when and if message is passed to MAC layer. (mac.send)
 *  11 - //when and if message can be sent, after check if sensor is idle and control signal states
 *  12 - //when and if message is added to the queue, if not 11 then 12.
 *  13 - //when and if message is at MAC layer and it has specific destination.
 *	2  - //when and if message is relayed by mac layer to the underlying layer (radioentity.transmit before check)
 *  21 - //when and if message is relayed by mac layer to the underlying layer (radioentity.transmit after check)
 *  250- //when radio RadioVLC received message on the other side (radioentity.receive no physical checking done) - usually all messages are tagged with this one
 *  251- //when radio RadioVLC received message on the other side and it is physically ok but before propagation time execution.
 *  252- //when and if radio RadioVLC received message on the other side and after propagation time and interference check, just before relaying to MAC layer.
 *	3  - //when and if MAC layer received message (mac.receive)
 *  31 - //when and if MAC layer received message (mac.receive) and message is on desired destination.
 *	4  - //when and if MAC layer relayed message to upper layer (netentity.receive).
 *	41 - //if message is not dropped on NET layer.
 *  5  - //when and if MAC layer relayed received message to upper layer (netentity.receive) and message is on desired destination
 *  6  - //when and if message has passed ifForMe(...) check on NET layer.
 *  70 - //when and if message is on PHY (radioVLC) layer and it is on desired destination (evaluated after 251)
 *  81 - //when and if message is dropped on a PHY layer due to asymmetry (global position, usually range)
 *  82 - //when and if message is dropped on a PHY layer due to asymmetry (positional)
 *  84 - //when and if message is dropped on a PHY layer due to asymmetry (design)
 *  90 - //when and if message is dropped because of interference
 *  92 - //when and if message is dropped because of transmit fail
 *  93 - //when and if message is dropped because of receive fail
 *       //90,92,93 are events provided by RadioVLC for MAC layer.
 * @author BorisTomas
 * 
 */
public class TimeEntry {
	public static ArrayList<NetMessage.Ip> AllMessages = new ArrayList<NetMessage.Ip>();
	public long Time;
	public int TimeID;
	public Object Signature;
	public NetMessage.Ip message;
	
	public TimeEntry(long time, int timeID, Object signature, Message msg )
	{
		Time = time;
		TimeID = timeID;
		Signature = signature;
		message = (NetMessage.Ip)msg;;
		addmsg(msg);
	}
	public TimeEntry(int timeID, Object signature, Message msg )
	{
		Time = JistAPI.getTime();
		TimeID = timeID;
		Signature = signature;
		message = (NetMessage.Ip)msg;
		addmsg(msg);
	}
	private void addmsg(Message msg)
	{
		if(msg != null)
		{
			this.message = (NetMessage.Ip)msg;
			AllMessages.add((NetMessage.Ip)msg);
		//	System.out.println("added new message, total count = "+ AllMessages.size());
		}
	}
}
