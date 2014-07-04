//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Constants.java Tue 2004/04/13 18:14:24 barr glenlivet.cs.cornell.edu>
//

// Copyright (C) 2005 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import jist.swans.net.NetMessage;
import jist.swans.radio.TimeEntry;

import org.omg.CORBA.Environment;

/**
 * SWANS constants.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: Constants.java,v 1.1 2007/04/09 18:49:32 drchoffnes Exp $
 * @since SWANS1.0
 */
public final class Constants
{

	//////////////////////////////////////////////////
	// Randomness
	//

	/** Global random number generator. */
	public static Random random = new Random(0);

	//////////////////////////////////////////////////
	// Time
	//

	/** zero delay. */
	public static final long PROCESS_IMMEDIATELY = 0;
	/** smallest possible delay. */
	public static final int EPSILON_DELAY = 1;
	/** one nano-second in simulation time units. */
	public static final long NANO_SECOND  = 1;
	/** one micro-second in simulation time units. */
	public static final long MICRO_SECOND = 1000 * NANO_SECOND;
	/** one milli-second in simulation time units. */
	public static final long MILLI_SECOND = 1000 * MICRO_SECOND;
	/** one second in simulation time units. */
	public static final long SECOND       = 1000 * MILLI_SECOND;
	/** one minute in simulation time units. */
	public static final long MINUTE       = 60 * SECOND;
	/** one hour in simulation time units. */
	public static final long HOUR         = 60 * MINUTE;
	/** one day in simulation time units. */
	public static final long DAY          = 24 * HOUR;

	//////////////////////////////////////////////////
	// Nature
	//

	/** Boltzmann's constant (units: Joules/Kelvin). */
	public static final double BOLTZMANN = 1.3807e-23;
	/** Speed of light in a vacuum (units: meter/second). */
	public static final double SPEED_OF_LIGHT = 2.9979e8;
	/** Pre-computed natural logarithm of 10. */
	public static final double log10 = Math.log(10);

	// pathloss constants
	public static final int PATHLOSS_FREE_SPACE = 1;
	public static final int PATHLOSS_SHADOWING = 2;
	public static final int PATHLOSS_TWO_RAY = 3;
	public static final int PATHLOSS_VLC =4;
	//////////////////////////////////////////////////
	// Field-related constants
	//

	// constants

	/** Default field boundary (units: sim distance, usually meters). */
	public static final float FIELD_BOUND_X = (float)200.0, FIELD_BOUND_Y = (float)200.0;

	/** node placement choice constant. */
	public static final int PLACEMENT_INVALID = -1;
	/** node placement choice constant. */
	public static final int PLACEMENT_RANDOM  = 1;
	/** node placement choice constant. */
	public static final int PLACEMENT_GRID    = 2;
	/** node placement choice constant. */
	public static final int PLACEMENT_STREET_RANDOM = 3;
	/** node placement choice constant. */
	public static final int PLACEMENT_STREET_CIRCUIT = 4;
	/** node placement choice constant. */
	public static final int PLACEMENT_DEFAULT = PLACEMENT_RANDOM;
	/** node placement choice constant. */
	public static final int PLACEMENT_MAX     = 4;

	/** node mobility choice constant. */
	public static final int MOBILITY_INVALID  = -1;
	/** node mobility choice constant. */
	public static final int MOBILITY_STATIC   = 1;
	/** node mobility choice constant. */
	public static final int MOBILITY_WAYPOINT = 2;
	/** node mobility choice constant. */
	public static final int MOBILITY_TELEPORT = 3;
	/** node mobility choice constant. */
	public static final int MOBILITY_WALK     = 4;
	/** node mobility choice constant. */
	public static final int MOBILITY_STRAW_SIMPLE    = 5;
	/** node mobility choice constant. */
	public static final int MOBILITY_STRAW_OD   = 6;  
	/** node mobility choice constant. */
	public static final int MOBILITY_DEFAULT  = MOBILITY_STATIC;

	/** street mobility configuration constant. */
	public static final int MOBILITY_STREET_RANDOM = 1;
	/** street mobility configuration constant. */
	public static final int MOBILITY_STREET_FLOW = 2;
	/** street mobility configuration constant. */
	public static final int MOBILITY_STREET_CIRCUIT = 3;

	/** path cost calculation constants **/
	/** cost based on speed limit **/
	public static final int COST_SPEED_LIMIT = 0;
	/** cost based on aggregated flow data **/
	public static final int COST_FLOW_DATA = 1;
	/** cost based on perfect flow knowledge **/
	public static final int COST_PEFECT_DATA = 2;

	/** vehicular traffic congestion monitor type constant. */
	public static final int CONGESTION_MONITOR_IDEAL = 1;

	/** lane change model number */
	public static final int LANE_CHANGE_MOBIL = 1;

	/** spatial data structure choice constant. */
	public static final int SPATIAL_INVALID = -1;
	/** spatial data structure choice constant. */
	public static final int SPATIAL_LINEAR  = 0;
	/** spatial data structure choice constant. */
	public static final int SPATIAL_GRID    = 1;
	/** spatial data structure choice constant. */
	public static final int SPATIAL_HIER    = 2;
	/** spatial data structure choice constant. */
	public static final int SPATIAL_WRAP    = 16;

	//////////////////////////////////////////////////
	// packet constants
	//

	/** packet with zero wire size. */
	public static final int ZERO_WIRE_SIZE = Integer.MIN_VALUE;

	//////////////////////////////////////////////////
	// Radio-related constants
	//

	// radio modes

	/** Radio mode: sleeping. */
	public static final byte RADIO_MODE_SLEEP        = -1;
	/** Radio mode: idle, no signals. */
	public static final byte RADIO_MODE_IDLE         = 0;
	/** Radio mode: some signals above sensitivity. */
	public static final byte RADIO_MODE_SENSING      = 1;
	/** Radio mode: signal locked and receiving packet. */
	public static final byte RADIO_MODE_RECEIVING    = 2;
	/** Radio mode: transmitting packet. */
	public static final byte RADIO_MODE_TRANSMITTING = 3;

	// radio noise types
	/** Radio noise independent */
	public static final byte RADIO_NOISE_INDEP = 1;
	/** Radio noise additive */
	public static final byte RADIO_NOISE_ADDITIVE = 2;

	// timing constants

	/** RX-TX switching delay. */
	public static final long RADIO_TURNAROUND_TIME = 5 * MICRO_SECOND;
	/** physical layer delay. */
	public static final long RADIO_PHY_DELAY = RADIO_TURNAROUND_TIME;
	/** Constant used to specify the default "delay to the wire". */
	public static final int RADIO_NOUSER_DELAY = -1;

	// defaults

	/** Default radio frequency (units: Hz). */
	public static final double FREQUENCY_DEFAULT = 2.4e9; // 2.4 GHz
	/** Default radio bandwidth (units: bits/second). */
	public static final int BANDWIDTH_DEFAULT = (int)2e6; // 2Mb/s
	/** Default transmission strength (units: dBm). */
	public static final double TRANSMIT_DEFAULT = 15.0;
	/** Default antenna gain (units: dB). */
	public static final double GAIN_DEFAULT = 1.0;
	/** Default radio reception sensitivity (units: dBm) originally -91. */
	public static final double SENSITIVITY_DEFAULT = -91.0;
	/** Default radio reception threshold (units: dBm). Originally -81 */
	public static final double THRESHOLD_DEFAULT = -81.0;
	/** Default temperature (units: degrees Kelvin). */
	public static final double TEMPERATURE_DEFAULT = 290.0;
	/** Default temperature noise factor. formerly 10 */
	public static final double TEMPERATURE_FACTOR_DEFAULT = 10.0;
	/** Default ambient noise (units: mW). */
	public static final double AMBIENT_NOISE_DEFAULT = 0.0;
	/** Default minimum propagated signal (units: dBm). */
	//public static final double PROPAGATION_LIMIT_DEFAULT = -111.0;
	public static final double PROPAGATION_LIMIT_DEFAULT = SENSITIVITY_DEFAULT; 
	/** Default radio height (units: sim distance units, usually meters). */
	public static final double HEIGHT_DEFAULT = 1.5;

	/** Default threshold signal-to-noise ratio. was 10.0*/
	public static final double SNR_THRESHOLD_DEFAULT = 10.0;


	//////////////////////////////////////////////////
	// Mac-related constants
	//

	// defaults

	/** Default mac promiscuous mode. */
	public static final boolean MAC_PROMISCUOUS_DEFAULT = false;
	/** link layer delay. */
	public static final long LINK_DELAY = MICRO_SECOND;
	/** Invalid MAC number. */
	public static final short MAC_INVALID        = -1;
	/** 802.11 MAC number. */
	public static final short MAC_802_11        = 1;
	/** Simple MAC number. */
	public static final short MAC_DUMB        = 2;

	//////////////////////////////////////////////////
	// Network-related constants
	//

	/** network layer loss model choice constant. */
	public static final int NET_LOSS_INVALID       = -1;
	/** network layer loss model choice constant. */
	public static final int NET_LOSS_NONE          = 0;
	/** network layer loss model choice constant. */
	public static final int NET_LOSS_UNIFORM       = 1;
	/** network layer loss model choice constant. */
	public static final int NET_LOSS_DEFAULT       = NET_LOSS_NONE;

	/** network packet priority level. */
	public static final byte NET_PRIORITY_CONTROL  = 0;
	/** network packet priority level. */
	public static final byte NET_PRIORITY_REALTIME = 1;
	/** network packet priority level. */
	public static final byte NET_PRIORITY_NORMAL   = 2;
	/** network packet priority level. */
	public static final byte NET_PRIORITY_NUM      = 3;
	/** network packet priority level. */
	public static final byte NET_PRIORITY_INVALID  = -1;


	/** network interface constant. */
	public static final int NET_INTERFACE_INVALID = -1;
	/** network interface constant. */
	public static final int NET_INTERFACE_LOOPBACK = 0;
	/** network interface constant. */
	public static final int NET_INTERFACE_DEFAULT = 1;

	/** network layer delay. */
	public static final long NET_DELAY = MICRO_SECOND;
	/** default time-to-live. */
	public static final byte TTL_DEFAULT = 64;

	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_INVALID        = -1;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_TCP            = 6;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_UDP            = 17;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_OSPF           = 87;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_BELLMANFORD    = 520;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_FISHEYE        = 530;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_AODV           = 123;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_DSR            = 135;
	/** protocol number for alternate DSR impl */
	public static final short NET_PROTOCOL_DSR_NS2            = 136;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_ODMRP          = 145;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_LAR1           = 110;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_ZRP            = 133;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_GPSR           = 888; // TODO non-standard
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_GLS_GPSR		= 887;	// TODO non-standard
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_GLS_GPSR_old	= 886;	// TODO non-standard

	public static final short NET_PROTOCOL_GM = 777; // TODO non-standard
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_TAGR			= 885;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_NHOP			= 884;
	/** zrp-subprotocol number. */
	public static final short NET_PROTOCOL_ZRP_NDP_DEFAULT  = 1;
	/** zrp-subprotocol number. */
	public static final short NET_PROTOCOL_ZRP_IARP_DEFAULT = 2;
	/** zrp-subprotocol number. */
	public static final short NET_PROTOCOL_ZRP_BRP_DEFAULT  = 3;
	/** zrp-subprotocol number. */
	public static final short NET_PROTOCOL_ZRP_IERP_DEFAULT = 4;
	/** zrp-subprotocol number. */
	public static final short NET_PROTOCOL_ZRP_IARP_ZDP     = 5;
	/** zrp-subprotocol number. */
	public static final short NET_PROTOCOL_ZRP_BRP_FLOOD    = 6;

	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_NO_NEXT_HEADER = 59;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_HEARTBEAT      = 500; // rimtodo: non-standard
	/** network protocol number for network table traffic */
	public static final short NET_PROTOCOL_NT = 502;
	/** network protocol number for VFN traffic */
	public static final short NET_PROTOCOL_VFN = 998;
	/** network level (IP) protocol number. */
	public static final short NET_PROTOCOL_MAX            = 999;



	//////////////////////////////////////////////////
	// Routing-related constants
	//

	/** configuration constant for ideal location database */
	public static final int GPSR_LOCATIONDB_IDEAL = 0;
	public static final int GPSR_LOCATIONDB_GLS = 1;

	//////////////////////////////////////////////////
	// Transport-related constants
	//

	/** transport layer delay. */
	public static final long TRANS_DELAY = MICRO_SECOND;
	/** socket delay. */
	public static final long TRANS_PROCESSING_DELAY = MICRO_SECOND;

	/** transport level (tcp/udp) protocol number. */
	public static final short TRANS_PROTOCOL_INVALID = -1;
	/** transport level (tcp/udp) protocol number. */
	public static final short TCP_PROTOCOL_ECHO      =  7;
	/** transport level (tcp/udp) protocol number. */
	public static final short TCP_PROTOCOL_FTP       = 21;
	/** transport level (tcp/udp) protocol number. */
	public static final short TCP_PROTOCOL_TELNET    = 23;
	/** transport level (tcp/udp) protocol number. */
	public static final short TCP_PROTOCOL_SMTP      = 25;
	/** transport level (tcp/udp) protocol number. */
	public static final short TCP_PROTOCOL_TIME      = 37;
	/** transport level (tcp/udp) protocol number. */
	public static final short TCP_PROTOCOL_HTTP      = 80;

	/** max size for UDP packet sent by driver... does not include the header */
	public static final int UDP_MAX_SIZE = 1000;




	/** TCP States. */
	public final class TCPSTATES
	{
		/**
		 * TCP state: LISTEN - represents waiting for a connection request from
		 * any remote TCP and port.
		 */
		public static final int LISTEN = 800;
		/**
		 * TCP state: SYN-SENT - represents waiting for a matching connection
		 * request after having sent a connection request.
		 */
		public static final int SYN_SENT = 801;
		/**
		 * TCP state: SYN-RECEIVED - represents waiting for a confirming
		 * connection request acknowledgment after having both received and sent a
		 * connection request.
		 */
		public static final int SYN_RECEIVED = 802;
		/**
		 * TCP state: ESTABLISHED - represents an open connection, data received
		 * can be delivered to the user.  The normal state for the data transfer
		 * phase of the connection.
		 */
		public static final int ESTABLISHED = 803;
		/**
		 * TCP state: FIN-WAIT-1 - represents waiting for a connection
		 * termination request from the remote TCP, or an acknowledgment of the
		 * connection termination request previously sent.
		 */
		public static final int FIN_WAIT_1 = 804;
		/**
		 * TCP state: FIN-WAIT-2 - represents waiting for a connection
		 * termination request from the remote TCP.
		 */
		public static final int FIN_WAIT_2 = 805;
		/**
		 * TCP state: CLOSE-WAIT - represents waiting for a connection termination
		 * request from the local user.
		 */
		public static final int CLOSE_WAIT = 806;
		/**
		 * TCP state: CLOSING - represents waiting for a connection termination
		 * request acknowledgment from the remote TCP.
		 */
		public static final int CLOSING = 807;
		/**
		 * TCP state: LAST-ACK - represents waiting for an acknowledgment of the
		 * connection termination request previously sent to the remote TCP (which
		 * includes an acknowledgment of its connection termination request).
		 */
		public static final int LAST_ACK = 808;
		/**
		 * TCP state: TIME-WAIT - represents waiting for enough time to pass to be
		 * sure the remote TCP received the acknowledgment of its connection
		 * termination request.
		 */
		public static final int TIME_WAIT = 809;
		/**
		 * TCP state: CLOSED - represents no connection state at all.
		 */
		public static final int CLOSED = 810;

	} // class: TCPSTATES

	//////////////////////////////////////////////////
	//MultiChannel
	//
	public static final byte DEFAULT_CHANNEL = 1;
	public static final int NUMBER_OF_CHANNELS = 6;
	public static final byte RADIO_MODE_CHANNEL_SWITCH = -2;
	public static final long CHANNEL_SWITCH_DELAY = 100 * MICRO_SECOND;
	public static final byte ASSIGN_LINEAR = 1;
	public static final byte ASSIGN_RANDOM_UNIFORM = 2;
	public static final byte ASSIGN_RANDOM_PERIODIC = 3;
	public static final long ASSIGN_REASSIGN_TIME = 5*SECOND;
	public static class VLCconstants
	{
		/**
		 * Total number of sent messages
		 */
		public static int SentDirect= 0;
		/**
		 * Total number of messages sent to broadcast
		 */
		public static int SentBroadcast = 0;
		public static int Received = 0;
		public static int DroppedOnSend = 0;
		public static int DroppedOnReceive = 0;
		public static String MACimplementationUsed;
		public static int NetIPReceived= 0;
		public static int NetIPReceivedForMe= 0;
		public static int NetIPDropped= 0;
		public static int NetIPSent= 0;
		public static long CBRmessages= 0;
		public static String PrintData()
		{

			String res = "";
			for (NetMessage.Ip item : TimeEntry.AllMessages)
			{
				res+= "\n msg: "+ item.hashCode() + " times: "; 
				for (TimeEntry time : item.Times) 
				{
					res += "t"+time.TimeID+"= " +time.Time + ", ";
				}
				
			}
			String filename =System.getProperty("user.home") + "/Desktop/" +"measureData.csv";
			
			PrintWriter writer;
			try {
				writer = new PrintWriter(filename, "UTF-8");
				writer.println("The first line");
				writer.println("The second line");
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			System.out.println();
			return "-----VLC data-----" + "\n"+
					"MAC implementation = "+ MACimplementationUsed + "\n"+
					"Sent Direct = " + SentDirect + "\n"+
					"Sent Broadcasts = "+ SentBroadcast + "\n" +
					"Received = " + Received + "\n"+
					"Dropped on send = " + DroppedOnSend + "\n"+
					"Dropped on receive = " + DroppedOnReceive + "\n"+
					"CBR messages total = " + CBRmessages + "\n"+
					"NetIP sent = " + NetIPSent + "\n"+
					"NetIP received = " + NetIPReceived + "\n"+
					"NetIP received for me= " + NetIPReceivedForMe + "\n"+
					"NetIP dropped = " + NetIPDropped+ "\n"+
					"timestats = " + res+ "\n"+
					"-----VLC data-----" + "\n";
		}
	}
} // class: Constants

