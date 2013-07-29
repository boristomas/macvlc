package jist.swans.misc;

import jist.runtime.JistAPI.Proxiable;
/**
 * 
 * @author tariavo
 *
 */
public interface StatInterface extends Proxiable {
	/**
	 * Called from Radio.
	 * @param radioID on which there is the collision
	 * @param endCollTime collision signal
	 */
	void logCollision(Integer radioID, long endCollTime);
	/**
	 * 
	 * @param radioID
	 */
	void logInMes(Integer radioID);
	/**
	 * 
	 * @param radioID
	 */
	void logOutMes(Integer radioID);
	/**
	 * print statistics
	 *
	 */
	void printStat();
}
