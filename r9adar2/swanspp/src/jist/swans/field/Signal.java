package jist.swans.field;

import jist.runtime.JistAPI.Timeless;
import jist.swans.misc.Message;

/**
 * The info about the signal on either
 * immutable, timeless
 * @author tariavo
 *
 */

public class Signal implements Timeless {
	private double power;	//unit mW
	private long startTime;
	private long duration;
	private Message message;
	private boolean coll_signal = false;

	public Signal(Message mes, double power, long startTime, long duration) {
		if(power > 0 && startTime >= 0 && duration >= 0) {
			this.power = power;
			this.startTime = startTime;
			this.duration = duration;
			this.message = mes;
		} else throw new RuntimeException("incorrect parametrs");
	}
	public long getDuration() {
		return duration;
	}
	public double getPower() {
		return power;
	}
	public long getStartTime() {
		return startTime;
	}
	public Message getMessage() {
		return this.message;
	}
	public long getFinishTime() {
		return getStartTime() + getDuration();
	}
	public boolean isCollisionSignal() {
		return coll_signal;
	}
	public static Signal createCollisionSignal(Signal signal) {
		Signal ret_sig = new Signal(signal.message, signal.power,
				signal.startTime, signal.duration);
		ret_sig.coll_signal = true;
		return ret_sig;
	}
	
	public String toString() {
		return "{ power = " + getPower() + "; startTime = " + getStartTime() + 
				"; finTime = " + getFinishTime() + "; mes = " + getMessage()
				+ " }"; 
	}
	public boolean equals(Object o) {
		if(o instanceof Signal) {
			Signal new_sig = (Signal)o;
			if(new_sig.coll_signal == this.coll_signal && 
					new_sig.duration == this.duration &&
					new_sig.power == this.power &&
					new_sig.startTime == this.startTime &&
					new_sig.message.equals(this.message))
				return true;
		}
		return false;
	}
}
