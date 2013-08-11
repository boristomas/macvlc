package jist.swans.field;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.misc.Location;
import jist.swans.radio.Channel;
import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioInterface;

/**
 * @author Rimon Barr
 * Changes concerns the time-actuality of signals on channel.<br>
 * In the original SWANS version signals for radio are lost if this
 * radio is sleeping.
 * @author tariavo (tariavo@mail.ru)
 */
/*be careful: non-timeless class*/
public class RadioData implements RadioDataInterface {
	/*----------------<tariavo's addons>-----------------*/
	//TODO change it
	private final boolean debug = true;
	
	//<Channel, Set<Signal> >
	private Hashtable channel_signals = new Hashtable();

	/**{@inheritDoc}*/
	public Set/*<Signal>*/ getSignalsOnChannel(Channel channel)
	throws Continuation {
		if(channel == null) throw null;
		//to avoid ConcurentModificationException in the methods which use it.
		//the actal data not so important
		Set signals = (Set)channel_signals.get(channel);
		if(signals == null) {
			return null;
		}
		Object[] ob = signals.toArray();
//		System.out.println("RadioData.getSignalsOnChannel() size in rd = " + 
//				ob.length + " time = " + JistAPI.getTime());
		return new HashSet(Arrays.asList(ob));
//		return (Set)channel_signals.get(channel);
	}
	
	/**{@inheritDoc}*/
	public boolean addSignalOnChannel(Channel channel, Signal signal)
	throws Continuation {
		if(signal == null) throw null;
		Set sig_set = (Set)channel_signals.get(channel);
		if(sig_set == null) {
			sig_set = new HashSet();
			channel_signals.put(channel, sig_set);
		}
		sig_set.add(signal);
//		System.out.println("signal added at time = " + JistAPI.getTime());
		return true;
	}
	
	/**{@inheritDoc}*/
	public void delSignalFromChannel(Channel channel, Signal signal) {
		if(signal == null) throw null;
		Set sig_set = (Set)channel_signals.get(channel);
		boolean result = sig_set.remove(signal);
		//debug
		if(debug && result == false)
			throw new RuntimeException("non-existing signal");
//		System.out.println("sig removed at time = " + JistAPI.getTime());
	}
	
	/*--------------------</tariavo's addons>------------------*/
	
	/** 
	 * radio entity reference for upcalls.
	 */
	private RadioInterface entity;

	/**
	 * timeless radio properties.
	 */
	private RadioInfo info;

	/**
	 * radio location.
	 */
	private Location loc;

	/**
	 * mobility information.
	 */
	private Mobility.MobilityInfo mobilityInfo;

	/**
	 * linked list pointers.
	 */
	private RadioData prev, next;

	private RadioDataInterface self = (RadioDataInterface)JistAPI.proxy(this,
			RadioDataInterface.class);
	
//////////////////////////////////////////////////
	public RadioInterface getEntity() {
		return entity;
	}

	public void setEntity(RadioInterface entity) {

		this.entity = entity;
	}

	public RadioInfo getInfo() {

		return info;
	}

	public void setInfo(RadioInfo info) {

		this.info = info;
	}

	public Location getLoc() {

		return loc;
	}

	public void setLoc(Location loc) {

		this.loc = loc;
	}

	public Mobility.MobilityInfo getMobilityInfo() {
		return mobilityInfo;
	}

	public void setMobilityInfo(Mobility.MobilityInfo mobilityInfo) {
		this.mobilityInfo = mobilityInfo;
	}

	public RadioData getNext() {
		return next;
	}

	public void setNext(RadioData next) {
		this.next = next;
	}

	public RadioData getPrev() {
		return prev;
	}

	public void setPrev(RadioData prev) {
		this.prev = prev;
	}
	public RadioDataInterface getProxy() {
		return this.self;
	}
}
