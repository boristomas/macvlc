package jist.swans.radio;

import jist.runtime.JistAPI.Timeless;
/**
 * @author tariavo
 *
 */
public class RadioEvent implements Timeless {
	private byte value;
	
	public static final RadioEvent END_RECEIVE = new RadioEvent((byte)0);
	public static final RadioEvent END_TRANSMIT = new RadioEvent((byte)1);
	
	public RadioEvent(byte value) {
		this.value = value;
	}
	public byte getValue() {
		return this.value;
	}
	public boolean equals(Object o) {
		if(o instanceof RadioEvent &&
				((RadioEvent)o).getValue() == this.getValue()) return true; 
		return false;
	}
	
	public String toString() {
		if(this.equals(RadioEvent.END_RECEIVE)) return "END_RECEIVE";
		if(this.equals(RadioEvent.END_TRANSMIT)) return "END_TRANSMIT";
		return "unknown event";
	}
}
