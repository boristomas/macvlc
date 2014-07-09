package jist.swans.radio;

import java.util.ArrayList;
import java.util.HashSet;

import org.python.modules.newmodule;

import jist.runtime.JistAPI;
import jist.swans.misc.Message;
import jist.swans.net.NetMessage;

/**
 * timeid:
 *	0 - // kada je poruka kreirana
 *	1 - //kada je poruka puštena na mac sloj. mac.send
 *	2 - //kada je mac sloj poslao poruku sloju ispod, radioentity.transmit
 *  25- //kada je radio primio poruku s druge strane, radioentity.receive.
 *	3 - //kada je drugi mac sloj primio poruku, mac.receive
 *	4 - //kada je drugi mac sloj poslao poruku sloju iznad, netentity.receive.
 *  5 - //kada je drugi mac sloj poslao poruku sloju iznad, netentity.receive. i poruka je naslovljena za primatelja.
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
		message = (NetMessage.Ip)msg;;
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
