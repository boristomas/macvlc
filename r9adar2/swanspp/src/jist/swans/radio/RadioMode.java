package jist.swans.radio;

import jist.runtime.JistAPI.Timeless;
/**
 * 
 * @author tariavo
 *
 */
public final class RadioMode implements Timeless {
	private final byte value;
	
	public static final RadioMode READY_TO_RECEIVE = new RadioMode((byte)10);
	public static final RadioMode RECEIVING = new RadioMode((byte)11);
	public static final RadioMode SLEEP = new RadioMode((byte)12);
	public static final RadioMode READY_TO_TRANSMIT = new RadioMode((byte)13);
	public static final RadioMode TRANSMITTING = new RadioMode((byte)14);
	
	public RadioMode(final byte value) {
		this.value = value;
	}
	public byte getValue() {
		return this.value;
	}
	public boolean equals(Object o) {
		if(o instanceof RadioMode &&
				((RadioMode)o).getValue() == this.getValue()) return true; 
		return false;
	}
	public String toString() {
		return 
			this.value == READY_TO_RECEIVE.value ? "READY_TO_RECEIVE" : 
			this.value == RECEIVING.value ?  "RECEIVING": 
			this.value == SLEEP.value ? "SLEEP" : 
			this.value == READY_TO_TRANSMIT.value ? "READY_TO_TRANSMIT" :
			this.value == TRANSMITTING.value ? "TRANSMITTING" : 
				"unknown mode: value = " + this.getValue();
	}
}
