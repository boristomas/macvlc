package jist.swans.radio;

import jist.runtime.JistAPI.Timeless;



/**
 * May include info about some radio properties, for instance:
 * code-encoding method, frequency(for calculation time to send in mac level)
 * 
 * Remember: two different radios can talk only if they use the same channel(s)
 * for this talk
 * @author tariavo
 *
 */
public class Channel implements Timeless {
	private static int cur_value = 0;
	/*u can add the additional info*/
	/*be careful, this object Timeless(immuttable!)*/
	protected int value = getStatValueAndInc();
	
	public static final Channel DEFAULT = new Channel();
	public boolean equals(Object o) {
		if(o instanceof Channel && (((Channel)o).value == this.value))
				return true;
		return false;
	}
	public String toString() {
		return "[ch_value = " + value + " ]";
	}
	
	private synchronized int getStatValueAndInc() {
		return cur_value++;
	}
}