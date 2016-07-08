package jist.swans.radio;

import java.util.ArrayList;
import java.util.HashSet;

import org.python.modules.newmodule;

import jist.runtime.JistAPI;
import jist.swans.misc.Message;
import jist.swans.net.NetMessage;

/**
 * timeid:
 *	0  - //kada je poruka kreirana
 *	1  - //kada je poruka puštena na mac sloj. mac.send
 *  11 - //kada je poruku moguce poslati, nakon provjera zauzetosti senzora i kontrolnih signala 
 *  12 - //kada je poruka dodana u red, znaci moguce je samo 11 ili 12 (u ovom slucaju ce kad tad morati biti jednom 11.)
 *	2  - //kada je mac sloj poslao poruku sloju ispod, radioentity.transmit prije provjera
 *  21 - //kada je mac sloj poslao poruku sloju ispod, radioentity.transmit nakon provjera.
 *  250- //kada je radio primio poruku s druge strane, radioentity.receive, programski samo, bez fizicke provjere
 *  251- //kada je radio primio poruku s druge strane ali fizicki ispravno, prije vremena propagacije.
 *  252- //kada je radio primio poruku s druge strane nakon propagacije i provjere interferencije, tik prije slanja sloju iznad (mac)
 *	3  - //kada je drugi mac sloj primio poruku, mac.receive
 *	4  - //kada je drugi mac sloj poslao poruku sloju iznad, netentity.receive.
 *  5  - //kada je drugi mac sloj poslao poruku sloju iznad, netentity.receive. i poruka je naslovljena za primatelja.
 *  6  - //kada je poruka prosla isforme(...) provjeru na net sloju.
 *  70 - //ako je poruka na phy sloju za cvor u kojem se i nalazi.
 *  71 - //ako je poruka na net sloju i destination poruke odgovara adresi cvora na net sloju.
 *  81 - //ako je poruka droppana zbog asimetrije (pozicija)
 *  82 - //ako je poruka droppana zbog asimetrije (pozicijska)
 *  83 - //ako je poruka droppana zbog asimetrije (design)
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
