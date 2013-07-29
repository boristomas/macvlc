package jist.swans.config;

import java.util.HashMap;
import java.util.LinkedList;

import jist.swans.Constants;

/**
 * Holds references to to stuff that is directly related to the simulation-settings and special settings.
 * @author ted
 *
 */
public class Configuration {

	public static String configurationFile;
	
	/** A list of XML-files that define different simulations. */
	public static LinkedList listOfSimulations;
	
	/** Contains a string for each car that says the list of roadSegment to go to. Key is the carId */
	public static HashMap carMovements;
	
	/** Contains any special messages to send. Key is carId and the value is a LinkedList containing Event-objects. */
	public static HashMap carSpecialEvents;
	
	/** The path to the special settings file. */
	public static String specialSettings;
	
	/** Some Configuration-constants */
	
	public static boolean DEBUG_CLIENT = false;
	public static boolean DEBUG_SERVER = false;
	
	public static String uniqueSimulationIdentifier;

	public static boolean shouldPrintSQL = true;
	
	/** The time (in milliseconds) between sending traffic data */
	public static long sendTrafficDataInterval;
	
	public static int EVENT_TYPE_CRASH = 1;
	
	public static int port = 3002;
	
	public static int duration = 10;
	
	public static byte ttl = 5;
	
	//-------------- Geocast Settings to be stored here: These are the default values: -------
	//----------------------------------- change them from JistExperiment ----------------
	public static long GCTimeout = 3 * Constants.SECOND;
	public static long GCAbidingTimeout = 3 * Constants.SECOND; 
	public static float GCTransmissionRange = 600;
	public static long GCMaxDistanceBackoff = 10 * Constants.MILLI_SECOND;
	public static float GCDistanceSensitivity = 1;
	public static long GCMaxContentionWindow = 50 * Constants.MICRO_SECOND;
	public static long GCMaxRetransmissions = 5;
	public static long GCRecentTimeThreshold = 3 * Constants.SECOND;
	public static long GCRetransmissionBackoffLong = 1 * Constants.SECOND;
	public static boolean GCHopControl = false;
	
	
	//--------------- Flooding Settings to be stored here: These are the default values: -------
	//----------------------------------- change them from JistExperiment ----------------
	public static long FLTimeout = 3*Constants.SECOND;
	public static long FLMaxMsgAliveTime = 3 * Constants.SECOND;
	public static long FLMaxRandomSlots = 40;
	public static long FLSlotTime = 100*Constants.MICRO_SECOND;
	
}
