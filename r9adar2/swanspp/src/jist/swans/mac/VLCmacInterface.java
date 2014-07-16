package jist.swans.mac;

import jist.swans.misc.Message;
import sun.management.Sensor;

public interface VLCmacInterface
{
	
	/**
	 * Notifies interference occurrence on radio sensor. during receiving of data.
	 * @param sensor
	 */
	void notifyInterference(Sensor[] sensors);
	void notifyError(int errorCode, String message);
	void notifyTransmitFail(Message msg, int errorCode);
	void notifyReceiveFail(Message msg, int errorCode);
}
