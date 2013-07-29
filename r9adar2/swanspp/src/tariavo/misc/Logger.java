package tariavo.misc;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Proxiable;

/**
 * 
 * @author tariavo (tariavo@mail.ru)
 *
 */
public interface Logger extends Proxiable {
	void log(String who, String event, String notes);
}
