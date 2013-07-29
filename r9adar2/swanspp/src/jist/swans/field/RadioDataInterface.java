package jist.swans.field;

import java.util.Set;

import jist.runtime.JistAPI.Continuation;
import jist.runtime.JistAPI.Proxiable;
import jist.swans.radio.Channel;
/**
 * 
 * @author tariavo (tariavo@mail.ru)
 */
public interface RadioDataInterface extends Proxiable {
	/**
	 * 
	 * @param channel
	 * @param signal
	 * @return wheter the signal has been removed
	 * @throws Continuation never
	 */
	public boolean addSignalOnChannel(Channel channel, Signal signal)
			throws Continuation;
	/**
	 * 
	 * @param channel
	 * @return signals on channel on this radio(really: copy of it)
	 * @throws Continuation never
	 */
	public Set/* <Signal> */getSignalsOnChannel(Channel channel)
			throws Continuation;
	/**
	 * 
	 * @param channel
	 * @param signal
	 * @throws Continuation never
	 */
	public void delSignalFromChannel(Channel channel, Signal signal);
}
