package tariavo.misc;

import jist.swans.Constants;
import jist.swans.misc.Util;
import jist.swans.radio.RadioInfo;

public class RadioInfoH extends RadioInfo {

	public RadioInfoH(RadioInfoUnique unique, RadioInfoShared shared) {
		super(unique, shared);
	}
	/**
	 * create the RadioInfoShared object with default properties
	 * @return
	 */
	public static RadioInfoShared createDefaultShared() {
		return createShared(
				Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT,
				Constants.TRANSMIT_DEFAULT, Constants.GAIN_DEFAULT,
				Util.fromDB(Constants.SENSITIVITY_DEFAULT),
				Util.fromDB(Constants.THRESHOLD_DEFAULT),
				Constants.TEMPERATURE_DEFAULT,
				Constants.TEMPERATURE_FACTOR_DEFAULT,
				Constants.AMBIENT_NOISE_DEFAULT);
	}
}
