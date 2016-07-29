/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         JistExperiment.java
 * RCS:          $Id: JistExperiment.java,v 1.1 2007/04/09 18:49:31 drchoffnes Exp $
 * Description:  JistExperiment class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Nov 17, 2004
 * Language:     Java
 * Package:      driver
 * Status:       Alpha Release
 *
 * (C) Copyright 2005, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package driver;

import java.util.Random;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.StreetMobility;
import jist.swans.misc.Location;
import jist.swans.route.RouteGPSR;
import jist.swans.route.geo.Ideal;


/**
 * @author David Choffnes
 *
 * This class encapsulates settings for a wireless network simulation.
 */
public class JistExperiment implements JistAPI.DoNotRewrite
{
    private static JistExperiment activeInstance;
	
	/** Whether to print a usage statement. */
	public boolean help = false;
    
    /** whether to close the visualization when done */
    public boolean closeWhenDone = false;
    
	// node settings
	/** Number of nodes. */
	public int nodes = 100;
	/** Number of nodes that will transmit signals */
	public int transmitters = nodes/2;
	
	// static node options
	/** number of static (non-mobile) nodes */
	public int staticNodes = 0;
	/** gain on static nodes */
	public double staticGain = 7;
	/** exponent on static nodes */
	public double staticExponent = 2.0;
	/** transmit power for static node */
	public double staticTransmit = 16.0;
	/** static node placement options */
	public int staticPlacement = Constants.PLACEMENT_RANDOM;
	/** static node placement options */
	public String staticPlacementOpts = "";
	/** sensitivity for static node, in dB */
	public double staticSensitivity=-95;
	
	// field settings
	/** Field wrap-around. */
	public boolean wrapField = false;
	/** Field dimensions (in meters). */
	public int fieldX =100;
	public int fieldY =100;
	/** the actual field entity */
	public Location.Location2D field;
	
	// placement settings
	/** Node placement model. */
	public int placement = Constants.PLACEMENT_RANDOM;
	/** Node placement options. */
	public String placementOpts = "";
	/** binning mode. */
	public int spatial_mode = Constants.SPATIAL_HIER;
	/** binning degree. */
	public int spatial_div = 5;
	
	// simulation duration settings
	/** Start of sending (seconds). */
	public int startTime = 30;
	/** Number of seconds to send messages. */
	public int duration = 900;
	/** Number of seconds after messages stop sending to end simulation. */
	public int resolutionTime = 30; 
	
	/** Random seed. 0 leads to different seed each time simulation runs. */
	public int seed = 0;
	
	
	// traffic
	/** bitrate for CBR traffic (in bytes/s) */
	public int cbrRate = 2048; // 2KBps
	/** the amount of data to stick in each packet  */
	public int cbrPacketSize = 1024;
	/** true if simulation should use CBR traffic */
	public boolean useCBR = true;
	/** Size  of packet for non-CBR traffic, in bytes */
	public int packetSize = 1000;
	/** Number of messages sent per minute per node. */
	public double sendRate = 1.0;
	
	// mobility settings
	/** Node mobility model. */
	public int mobility = Constants.MOBILITY_STATIC;
	/** Node mobility options. */
	public String mobilityOpts = "";
	
	// RWP mobility settings
	/** random waypoint pause time. */
	public int pause_time = 1;
	/** random waypoint granularity (in meters). */
	public int granularity = 10;
	/** random waypoint minimum speed. */
	public int min_speed = 1;
	/** random waypoint maximum speed. */
	public int max_speed = 3;
	
	// Settings for street mobility
	/** Segment file name */
	public String segmentFile="segments.dat";
	/** street file name */
	public String streetFile="names.dat";
	/** shape file name */
	public String shapeFile="chains.dat";
	/** degree of binning for quad tree */
	public int degree = 5; 
	/** probability that a node will make a turn */
	public double probability = 0.1;
	/** maximum latitude for streets */
	public float maxLat = 180f;
	/** maximum longitude for streets */
	public float maxLong = 180f;
	/** minimum latitude for streets */
	public float minLat = -180f;
	/** minimum longitude for streets */
	public float minLong = -180f;	 
	/** penetration ratio */
	public float penetrationRatio=1.0f;
	/** std deviation for different between speed limit and actual driver speed */
	public double driverStdDev = 4.0;
    /** time to to elapse between each driver movement (in seconds) */
    public double stepTime = 1.0;
    /** whether to use traffic flows */
    public boolean useFlows = false; 
    /** locations for flows */
    public Location startLocs[];
    public Location endLocs[];
    /** file contianing flow description for vehicles */
    public String flowFile;
    /** seconds between vehicles flowing into map */
    public Integer flowRates[];
    /** lane change model for street mobility */
    public int laneChangeModel = 0;

    public String StaticPlacementOptions;
    public String MACProtocol;
    public boolean MeasurementMode;
    
    /*public float VLCvisionAngleTx=0;
    public float VLCvisionAngleRx=0;
    public float VLCLOSRx = 0;
    public float VLCLOSTx = 0;*/
    public float VehicleLength=0;
    public float VehicleWidth =0;
    public float VehicleLengthDev =0;
    public float VehicleWidthDev =0;
   
    // link-layer settings
	/** mac protocol */
	public int mac = Constants.MAC_802_11;
	/** radio frequency */
	public double frequency = Constants.FREQUENCY_DEFAULT; // 2.4 GHz
	/** Default radio bandwidth (units: bits/second). */
	public int bandwidth = Constants.BANDWIDTH_DEFAULT; //500Kb/s   11Mb/s //2000000

	/** Default transmission strength (units: dBm). */
	public double transmit = Constants.TRANSMIT_DEFAULT;
	/** Default antenna gain (units: dB). */
	public double gain = Constants.GAIN_DEFAULT;
	/** Default radio reception sensitivity (units: dBm) originally -91. */
	public double sensitivity = Constants.SENSITIVITY_DEFAULT;
	/** Default radio reception threshold (units: dBm). Originally -81 */
	public double threshold = Constants.THRESHOLD_DEFAULT;
	/** Default temperature (units: degrees Kelvin). */
	public double temperature = Constants.TEMPERATURE_DEFAULT;
	/** Default temperature noise factor. */
	public double temperature_factor = Constants.TEMPERATURE_FACTOR_DEFAULT;
	/** Default ambient noise (units: mW). */
	public double ambiant_noise = Constants.AMBIENT_NOISE_DEFAULT;
	
	// network loss
	/** Packet loss model. */
	public int loss = Constants.NET_LOSS_NONE;
	/** Packet loss options. */
	public String lossOpts = "";
	
	// air settings
	/** the pathloss model to use */
	public int pathloss = Constants.PATHLOSS_SHADOWING;
	/** the radio noise type to use */
	public int radioNoiseType = Constants.RADIO_NOISE_ADDITIVE;
	/** Exponent for pathloss formula. */
	public double exponent = 2.8;
	/** Standard deviation for log-normal shadow fading. */
	public double stdDeviation = 6.0;
	
	// routing settings 
	/** Routing protocol to use. */
	public int protocol = Constants.NET_PROTOCOL_AODV;

	/** Default port number to send and receive packets. */
	public int port = 3001;    
	
	/** radius for Zrp neighbor */
	public int radius=2;
	
	// AODV options
	/** aodv timeout (s) */
	public int aodvTimeout = 30;
	/** aodv hello message interval (s) */
	public int aodvHelloInterval = 30;
	
	
	// GPSR settings
	/** whether or not the routing protocol will use 802.11 MAC */
	public boolean use_mac_ = true;
	/** whether or not to use perimeters */
	public boolean use_peri_ = true;		
	public boolean verbose_ = false;			// verbosity (binary)
	public boolean drop_debug_ = false;		// whether or not to be verbose on NRTE events
	public boolean peri_proact_ = true;		// whether or not to pro-actively send pprobes
	public boolean use_implicit_beacon_ = true;	// whether or not all data packets are beacons
	public boolean use_planar_ = true;		// whether or not to planarize graph
	public boolean use_loop_detect_ = false;		// whether or not to fix loops in peridata pkts
	public boolean use_timed_plnrz_ = false;		// whether or not to replanarize w/timer
	public boolean mac_promisc = false;			// whether mac is in promiscuous mode
	public double bint_ = 1.5;			// beacon interval
	public double bdesync_ = 0.5;		// beacon desync random component range
	public double bexp_ =  4.5;			// beacon expiration interval (def: 13.5)
	public double pint_ = 1.5;			// perimeter probe interval
	public double pdesync_ = 0.5;		// perimeter probe desync random cpt. range
	public double lpexp_ = RouteGPSR.GPSR_PPROBE_EXP;		// perimeter probe generation timeout
	public int GPSR_ldb = Constants.GPSR_LOCATIONDB_IDEAL;
	public Ideal locDB; // global location database for simulation 
	
    // visualizaiton settings
	/** if true, will visualize the simulation */
	public boolean useVisualizer = false;
    /** if true, will show title in visualization */
    public boolean showTitle = false;
    /** if true, will show interference */
    public boolean showInterference = true;
    /** true if memory should be measured */
    public boolean measureMemory = false;
    /** measures sim thoughput if true */
    public boolean measureThroughput = false;
	
    // values carried by JistExperiment object
	/** street mobility object, not to be set using XML file */
	public StreetMobility sm;
	/** Random object for use in simulation. NOT to be set using XML file. */
	public Random random;
	/** the object that controls the simulation visualization.
	 * NOT to be set using the XML file.
	 */
	public VisualizerInterface visualizer;
	/** nominal transmit radius for current conditions. NOT to be set using XML */
	public double transmitRadius = Double.MAX_VALUE;

	
	/**
	 * @return Returns the maxLat.
	 */
	public float getMaxLat() {
		return maxLat;
	}
	/**
	 * @param maxLat The maxLat to set.
	 */
	public void setMaxLat(float maxLat) {
		this.maxLat = maxLat;
	}
	/**
	 * @return Returns the maxLong.
	 */
	public float getMaxLong() {
		return maxLong;
	}
	/**
	 * @param maxLong The maxLong to set.
	 */
	public void setMaxLong(float maxLong) {
		this.maxLong = maxLong;
	}
	/**
	 * @return Returns the minLat.
	 */
	public float getMinLat() {
		return minLat;
	}
	/**
	 * @param minLat The minLat to set.
	 */
	public void setMinLat(float minLat) {
		this.minLat = minLat;
	}
	/**
	 * @return Returns the minLong.
	 */
	public float getMinLong() {
		return minLong;
	}
	/**
	 * @param minLong The minLong to set.
	 */
	public void setMinLong(float minLong) {
		this.minLong = minLong;
	}
	
	
	/**
	 * @param help
	 * @param exponent
	 * @param stdDeviation
	 * @param protocol
	 * @param nodes
	 * @param fieldX
	 * @param fieldY
	 * @param wrapField
	 * @param placement
	 * @param placementOpts
	 * @param mobility
	 * @param mobilityOpts
	 * @param loss
	 * @param lossOpts
	 * @param sendRate
	 * @param startTime
	 * @param duration
	 * @param resolutionTime
	 * @param seed
	 * @param spatial_mode
	 * @param spatial_div
	 * @param port
	 * @param pause_time
	 * @param granularity
	 * @param min_speed
	 * @param max_speed
	 * @param frequency
	 * @param bandwidth
	 * @param transmit
	 * @param gain
	 * @param sensitivity
	 * @param threshold
	 * @param temperature
	 * @param temperature_factor
	 * @param ambiant_noise
	 * @param transmitters
	 */
	public JistExperiment(boolean help, double exponent, double stdDeviation,
			int protocol, int nodes, int fieldX, int fieldY,
			boolean wrapField, int placement,
			String placementOpts, int mobility, String mobilityOpts, int loss,
			String lossOpts, double sendRate, int startTime, int duration,
			int resolutionTime, int seed, int spatial_mode, int spatial_div,
			int port, int pause_time, int granularity, int min_speed,
			int max_speed, double frequency, int bandwidth, double transmit,
			double gain, double sensitivity, double threshold,
			double temperature, double temperature_factor,
			double ambiant_noise, int transmitters) {
		super();
		this.help = help;
		this.exponent = exponent;
		this.stdDeviation = stdDeviation;
		this.protocol = protocol;
		this.nodes = nodes;
		this.fieldX = fieldX;
		this.fieldY = fieldY;
		this.wrapField = wrapField;
		this.placement = placement;
		this.placementOpts = placementOpts;
		this.mobility = mobility;
		this.mobilityOpts = mobilityOpts;
		this.loss = loss;
		this.lossOpts = lossOpts;
		this.sendRate = sendRate;
		this.startTime = startTime;
		this.duration = duration;
		this.resolutionTime = resolutionTime;
		this.seed = seed;
		this.spatial_mode = spatial_mode;
		this.spatial_div = spatial_div;
		this.port = port;
		this.pause_time = pause_time;
		this.granularity = granularity;
		this.min_speed = min_speed;
		this.max_speed = max_speed;
		this.frequency = frequency;
		this.bandwidth = bandwidth;
		this.transmit = transmit;
		this.gain = gain;
		this.sensitivity = sensitivity;
		this.threshold = threshold;
		this.temperature = temperature;
		this.temperature_factor = temperature_factor;
		this.ambiant_noise = ambiant_noise;
		this.transmitters = transmitters;
		field = new Location.Location2D(fieldX, fieldY);
	}
	/** Creates a new instance of JistExperiment with default values */
	public JistExperiment() {
        if (activeInstance!=null){ 
            System.err.println("WARNING: Should not instantiate more than once!");
        }
		activeInstance = this;
	}
	
    public static JistExperiment getJistExperiment(){
        return activeInstance;
    }
    
    public boolean getMeasurementMode()
    {
    	return MeasurementMode;
    }
    public void setMeasurementMode( boolean measurementMode)
    {
    	this.MeasurementMode = measurementMode;
    }
    
    public String ResultsPath;
    public String getResultsPath()
    {
    	return ResultsPath;
    }
    public void setResultsPath( String resultsPath)
    {
    	this.ResultsPath = resultsPath;
    }
    
    public String getStaticPlacementOptions()
    {
    	return StaticPlacementOptions;
    }
    public void setStaticPlacementOptions( String staticPlacementOptions)
    {
    	this.StaticPlacementOptions = staticPlacementOptions;
    }
    public String getMACProtocol()
    {
    	return MACProtocol;
    }
    public void setMACProtocol( String mACProtocol)
    {
    	this.MACProtocol = mACProtocol;
    }
   
    public float getVehicleLength()
    {
    	return VehicleLength;
    }
    public void setVehicleLength( float vehicleLength)
    {
    	this.VehicleLength = vehicleLength;
    }
    public float getVehicleWidth()
    {
    	return VehicleWidth;
    }
    public void setVehicleWidth( float vehicleWidth)
    {
    	this.VehicleWidth = vehicleWidth;
    }
    public float getVehicleLengthDev()
    {
    	return VehicleLengthDev;
    }
    public void setVehicleLengthDev( float vehicleLengthDev)
    {
    	this.VehicleLengthDev = vehicleLengthDev;
    }
    public float getVehicleWidthDev()
    {
    	return VehicleWidthDev;
    }
    public void setVehicleWidthDev( float vehicleWidthDev)
    {
    	this.VehicleWidthDev = vehicleWidthDev;
    }
    
	/**
	 * @return Returns the duration.
	 */
	public int getDuration() {
		return duration;
	}
	/**
	 * @param duration The duration to set.
	 */
	public void setDuration(int duration) {
		this.duration = duration;
	}
	/**
	 * @return Returns the exponent.
	 */
	public double getExponent() {
		return exponent;
	}
	/**
	 * @param exponent The exponent to set.
	 */
	public void setExponent(double exponent) {
		this.exponent = exponent;
	}
	
	/**
	 * @return Returns the granularity.
	 */
	public int getGranularity() {
		return granularity;
	}
	/**
	 * @param granularity The granularity to set.
	 */
	public void setGranularity(int granularity) {
		this.granularity = granularity;
	}
	/**
	 * @return Returns the help.
	 */
	public boolean isHelp() {
		return help;
	}
	/**
	 * @param help The help to set.
	 */
	public void setHelp(boolean help) {
		this.help = help;
	}
	/**
	 * @return Returns the loss.
	 */
	public int getLoss() {
		return loss;
	}
	/**
	 * @param loss The loss to set.
	 */
	public void setLoss(int loss) {
		this.loss = loss;
	}
	/**
	 * @return Returns the lossOpts.
	 */
	public String getLossOpts() {
		return lossOpts;
	}
	/**
	 * @param lossOpts The lossOpts to set.
	 */
	public void setLossOpts(String lossOpts) {
		this.lossOpts = lossOpts;
	}
	/**
	 * @return Returns the max_speed.
	 */
	public int getMax_speed() {
		return max_speed;
	}
	/**
	 * @param max_speed The max_speed to set.
	 */
	public void setMax_speed(int max_speed) {
		this.max_speed = max_speed;
	}
	/**
	 * @return Returns the min_speed.
	 */
	public int getMin_speed() {
		return min_speed;
	}
	/**
	 * @param min_speed The min_speed to set.
	 */
	public void setMin_speed(int min_speed) {
		this.min_speed = min_speed;
	}
	/**
	 * @return Returns the mobility.
	 */
	public int getMobility() {
		return mobility;
	}
	/**
	 * @param mobility The mobility to set.
	 */
	public void setMobility(int mobility) {
		this.mobility = mobility;
	}
	/**
	 * @return Returns the mobilityOpts.
	 */
	public String getMobilityOpts() {
		return mobilityOpts;
	}
	/**
	 * @param mobilityOpts The mobilityOpts to set.
	 */
	public void setMobilityOpts(String mobilityOpts) {
		this.mobilityOpts = mobilityOpts;
	}
	/**
	 * @return Returns the nodes.
	 */
	public int getNodes() {
		return nodes;
	}
	/**
	 * @param nodes The nodes to set.
	 */
	public void setNodes(int nodes) {
		this.nodes = nodes;
	}
	/**
	 * @return Returns the pause_time.
	 */
	public int getPause_time() {
		return pause_time;
	}
	/**
	 * @param pause_time The pause_time to set.
	 */
	public void setPause_time(int pause_time) {
		this.pause_time = pause_time;
	}
	/**
	 * @return Returns the placement.
	 */
	public int getPlacement() {
		return placement;
	}
	/**
	 * @param placement The placement to set.
	 */
	public void setPlacement(int placement) {
		this.placement = placement;
		
	}
	/**
	 * @return Returns the placementOpts.
	 */
	public String getPlacementOpts() {
		return placementOpts;
	}
	/**
	 * @param placementOpts The placementOpts to set.
	 */
	public void setPlacementOpts(String placementOpts) {
		if (placement == Constants.PLACEMENT_GRID){
			int area = fieldX * fieldY;
			int sum = fieldX + fieldY;
			int y = (int)Math.ceil(Math.sqrt(nodes) * 2*((double)fieldY/sum));
			int x = (int)Math.ceil(Math.sqrt(nodes) * 2*((double)fieldX/sum));
			if (y > x) y++;
			else if (x > y) x++;
			
			placementOpts = x+"x"+y;
		}
		this.placementOpts = placementOpts;
	}
	/**
	 * @return Returns the port.
	 */
	public int getPort() {
		return port;
	}
	/**
	 * @param port The port to set.
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/**
	 * @return Returns the protocol.
	 */
	public int getProtocol() {
		return protocol;
	}
	/**
	 * @param protocol The protocol to set.
	 */
	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}
	/**
	 * @return Returns the resolutionTime.
	 */
	public int getResolutionTime() {
		return resolutionTime;
	}
	/**
	 * @param resolutionTime The resolutionTime to set.
	 */
	public void setResolutionTime(int resolutionTime) {
		this.resolutionTime = resolutionTime;
	}
	/**
	 * @return Returns the seed.
	 */
	public int getSeed() {
		return seed;
	}
	/**
	 * @param seed The seed to set.
	 */
	public void setSeed(int seed) {
		this.seed = seed;
	}
	/**
	 * @return Returns the sendRate.
	 */
	public double getSendRate() {
		return sendRate;
	}
	/**
	 * @param sendRate The sendRate to set.
	 */
	public void setSendRate(double sendRate) {
		this.sendRate = sendRate;
	}
	/**
	 * @return Returns the spatial_div.
	 */
	public int getSpatial_div() {
		return spatial_div;
	}
	/**
	 * @param spatial_div The spatial_div to set.
	 */
	public void setSpatial_div(int spatial_div) {
		this.spatial_div = spatial_div;
	}
	/**
	 * @return Returns the spatial_mode.
	 */
	public int getSpatial_mode() {
		return spatial_mode;
	}
	/**
	 * @param spatial_mode The spatial_mode to set.
	 */
	public void setSpatial_mode(int spatial_mode) {
		this.spatial_mode = spatial_mode;
	}
	/**
	 * @return Returns the startTime.
	 */
	public int getStartTime() {
		return startTime;
	}
	/**
	 * @param startTime The startTime to set.
	 */
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}
	/**
	 * @return Returns the stdDeviation.
	 */
	public double getStdDeviation() {
		return stdDeviation;
	}
	/**
	 * @param stdDeviation The stdDeviation to set.
	 */
	public void setStdDeviation(double stdDeviation) {
		this.stdDeviation = stdDeviation;
	}
	/**
	 * @return Returns the wrapField.
	 */
	public boolean isWrapField() {
		return wrapField;
	}
	/**
	 * @param wrapField The wrapField to set.
	 */
	public void setWrapField(boolean wrapField) {
		this.wrapField = wrapField;
	}
	/**
	 * @return Returns the fieldX.
	 */
	public int getFieldX() {
		return fieldX;
	}
	/**
	 * @param fieldX The fieldX to set.
	 */
	public void setFieldX(int fieldX) {
		this.fieldX = fieldX;
	}
	/**
	 * @return Returns the fieldY.
	 */
	public int getFieldY() {
		return fieldY;
	}
	/**
	 * @param fieldY The fieldY to set.
	 */
	public void setFieldY(int fieldY) {
		this.fieldY = fieldY;
	}
	/**
	 * @return Returns the ambiant_noise.
	 */
	public double getAmbiant_noise() {
		return ambiant_noise;
	}
	/**
	 * @param ambiant_noise The ambiant_noise to set.
	 */
	public void setAmbiant_noise(double ambiant_noise) {
		this.ambiant_noise = ambiant_noise;
	}
	/**
	 * @return Returns the bandwidth.
	 */
	public int getBandwidth() {
		return bandwidth;
	}
	/**
	 * @param bandwidth The bandwidth to set.
	 */
	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}
	/**
	 * @return Returns the field.
	 */
	public Location.Location2D getField() {
		return field;
	}
	/**
	 * @param field The field to set.
	 */
	public void setField(Location.Location2D field) {
		this.field = field;
	}
	/**
	 * @return Returns the frequency.
	 */
	public double getFrequency() {
		return frequency;
	}
	/**
	 * @param frequency The frequency to set.
	 */
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	/**
	 * @return Returns the gain.
	 */
	public double getGain() {
		return gain;
	}
	/**
	 * @param gain The gain to set.
	 */
	public void setGain(double gain) {
		this.gain = gain;
	}
	/**
	 * @return Returns the sensitivity.
	 */
	public double getSensitivity() {
		return sensitivity;
	}
	/**
	 * @param sensitivity The sensitivity to set.
	 */
	public void setSensitivity(double sensitivity) {
		this.sensitivity = sensitivity;
	}
	/**
	 * @return Returns the temperature.
	 */
	public double getTemperature() {
		return temperature;
	}
	/**
	 * @param temperature The temperature to set.
	 */
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
	/**
	 * @return Returns the temperature_factor.
	 */
	public double getTemperature_factor() {
		return temperature_factor;
	}
	/**
	 * @param temperature_factor The temperature_factor to set.
	 */
	public void setTemperature_factor(double temperature_factor) {
		this.temperature_factor = temperature_factor;
	}
	/**
	 * @return Returns the threshold.
	 */
	public double getThreshold() {
		return threshold;
	}
	/**
	 * @param threshold The threshold to set.
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	/**
	 * @return Returns the transmit.
	 */
	public double getTransmit() {
		return transmit;
	}
	/**
	 * @param transmit The transmit to set.
	 */
	public void setTransmit(double transmit) {
		this.transmit = transmit;
	}
	/**
	 * @return Returns the transmitters.
	 */
	public int getTransmitters() {
		return transmitters;
	}
	/**
	 * @param transmitters The transmitters to set.
	 */
	public void setTransmitters(int transmitters) {
		this.transmitters = transmitters;
	}
	
	/**
	 * Sets the 2D field
	 */
	public void setField() {
		this.field = new Location.Location2D(fieldX, fieldY);
	}
	
	/**
	 * Print contents of class
	 */
	public void printClass()
	{
		System.out.println("help = " + help);
		System.out.println("exponent =" + exponent);
		System.out.println("stdDeviation =" + stdDeviation);
		System.out.println("protocol =" + protocol);
		System.out.println("nodes =" + nodes);
		System.out.println("fieldX =" + fieldX);
		System.out.println("fieldY =" + fieldY);
		System.out.println("wrapField =" + wrapField);
		System.out.println("placement =" + placement);
		System.out.println("placementOpts =" + placementOpts);
		System.out.println("mobility =" + mobility);
		System.out.println("mobilityOpts =" + mobilityOpts);
		System.out.println("loss =" + loss);
		System.out.println("lossOpts =" + lossOpts);
		System.out.println("sendRate =" + sendRate);
		System.out.println("startTime =" + startTime);
		System.out.println("duration =" + duration);
		System.out.println("resolutionTime =" + resolutionTime);
		System.out.println("seed =" + seed);
		System.out.println("spatial_mode =" + spatial_mode);
		System.out.println("spatial_div =" + spatial_div);
		System.out.println("port =" + port);
		System.out.println("pause_time =" + pause_time);
		System.out.println("granularity =" + granularity);
		System.out.println("min_speed =" + min_speed);
		System.out.println("max_speed =" + max_speed);
		System.out.println("frequency =" + frequency);
		System.out.println("bandwidth =" + bandwidth);
		System.out.println("transmit =" + transmit);
		System.out.println("gain =" + gain);
		System.out.println("sensitivity =" + sensitivity);
		System.out.println("threshold =" + threshold);
		System.out.println("temperature =" + temperature);
		System.out.println("temperature_factor =" + temperature_factor);
		System.out.println("ambiant_noise =" + ambiant_noise);
		System.out.println("transmitters =" + transmitters);
	}
	
	/**
	 * @return Returns the degree.
	 */
	public int getDegree() {
		return degree;
	}
	/**
	 * @param degree The degree to set.
	 */
	public void setDegree(int degree) {
		this.degree = degree;
	}
	/**
	 * @return Returns the mac.
	 */
	public int getMac() {
		return mac;
	}
	/**
	 * @param mac The mac to set.
	 */
	public void setMac(int mac) {
		this.mac = mac;
	}
	/**
	 * @return Returns the probability.
	 */
	public double getProbability() {
		return probability;
	}
	/**
	 * @param probability The probability to set.
	 */
	public void setProbability(double probability) {
		this.probability = probability;
	}
	/**
	 * @return Returns the segmentFile.
	 */
	public String getSegmentFile() {
		return segmentFile;
	}
	/**
	 * @param segmentFile The segmentFile to set.
	 */
	public void setSegmentFile(String segmentFile) {
		this.segmentFile = segmentFile;
	}
	/**
	 * @return Returns the shapeFile.
	 */
	public String getShapeFile() {
		return shapeFile;
	}
	/**
	 * @param shapeFile The shapeFile to set.
	 */
	public void setShapeFile(String shapeFile) {
		this.shapeFile = shapeFile;
	}
	/**
	 * @return Returns the streetFile.
	 */
	public String getStreetFile() {
		return streetFile;
	}
	/**
	 * @param streetFile The streetFile to set.
	 */
	public void setStreetFile(String streetFile) {
		this.streetFile = streetFile;
	}
	/**
	 * @return Returns the staticExponent.
	 */
	public double getStaticExponent() {
		return staticExponent;
	}
	/**
	 * @param staticExponent The staticExponent to set.
	 */
	public void setStaticExponent(double staticExponent) {
		this.staticExponent = staticExponent;
	}
	/**
	 * @return Returns the staticGain.
	 */
	public double getStaticGain() {
		return staticGain;
	}
	/**
	 * @param staticGain The staticGain to set.
	 */
	public void setStaticGain(double staticGain) {
		this.staticGain = staticGain;
	}
	/**
	 * @return Returns the staticNodes.
	 */
	public int getStaticNodes() {
		return staticNodes;
	}
	/**
	 * @param staticNodes The staticNodes to set.
	 */
	public void setStaticNodes(int staticNodes) {
		this.staticNodes = staticNodes;
	}
	/**
	 * @return Returns the staticPlacement.
	 */
	public int getStaticPlacement() {
		return staticPlacement;
	}
	/**
	 * @param staticPlacement The staticPlacement to set.
	 */
	public void setStaticPlacement(int staticPlacement) {
		this.staticPlacement = staticPlacement;
	}
	/**
	 * @return Returns the staticTransmit.
	 */
	public double getStaticTransmit() {
		return staticTransmit;
	}
	/**
	 * @param staticTransmit The staticTransmit to set.
	 */
	public void setStaticTransmit(double staticTransmit) {
		this.staticTransmit = staticTransmit;
	}
	/**
	 * @return Returns the staticPlacementOpts.
	 */
	public String getStaticPlacementOpts() {
		return staticPlacementOpts;
	}
	/**
	 * @param staticPlacementOpts The staticPlacementOpts to set.
	 */
	public void setStaticPlacementOpts(String staticPlacementOpts) {
		this.staticPlacementOpts = staticPlacementOpts;
	}
	/**
	 * @return Returns the staticSensitivity.
	 */
	public double getStaticSensitivity() {
		return staticSensitivity;
	}
	/**
	 * @param staticSensitivity The staticSensitivity to set.
	 */
	public void setStaticSensitivity(double staticSensitivity) {
		this.staticSensitivity = staticSensitivity;
	}
	/**
	 * @return Returns the radius.
	 */
	public int getRadius() {
		return radius;
	}
	/**
	 * @param radius The radius to set.
	 */
	public void setRadius(int radius) {
		this.radius = radius;
	}
	/**
	 * @return Returns the aodvHelloInterval.
	 */
	public int getAodvHelloInterval() {
		return aodvHelloInterval;
	}
	/**
	 * @param aodvHelloInterval The aodvHelloInterval to set.
	 */
	public void setAodvHelloInterval(int aodvHelloInterval) {
		this.aodvHelloInterval = aodvHelloInterval;
	}
	/**
	 * @return Returns the aodvTimeout.
	 */
	public int getAodvTimeout() {
		return aodvTimeout;
	}
	/**
	 * @param aodvTimeout The aodvTimeout to set.
	 */
	public void setAodvTimeout(int aodvTimeout) {
		this.aodvTimeout = aodvTimeout;
	}
	/**
	 * @return Returns the penetrationRatio.
	 */
	public float getPenetrationRatio() {
		return penetrationRatio;
	}
	/**
	 * @param penetrationRatio The penetrationRatio to set.
	 */
	public void setPenetrationRatio(float penetrationRatio) {
		this.penetrationRatio = penetrationRatio;
	}
	/**
	 * @return Returns the bdesync_.
	 */
	public double getBdesync_() {
		return bdesync_;
	}
	/**
	 * @param bdesync_ The bdesync_ to set.
	 */
	public void setBdesync_(double bdesync_) {
		this.bdesync_ = bdesync_;
	}
	/**
	 * @return Returns the bexp_.
	 */
	public double getBexp_() {
		return bexp_;
	}
	/**
	 * @param bexp_ The bexp_ to set.
	 */
	public void setBexp_(double bexp_) {
		this.bexp_ = bexp_;
	}
	/**
	 * @return Returns the bint_.
	 */
	public double getBint_() {
		return bint_;
	}
	/**
	 * @param bint_ The bint_ to set.
	 */
	public void setBint_(double bint_) {
		this.bint_ = bint_;
	}
	/**
	 * @return Returns the driverStdDev.
	 */
	public double getDriverStdDev() {
		return driverStdDev;
	}
	/**
	 * @param driverStdDev The driverStdDev to set.
	 */
	public void setDriverStdDev(double driverStdDev) {
		this.driverStdDev = driverStdDev;
	}
	/**
	 * @return Returns the drop_debug_.
	 */
	public boolean getDrop_debug_() {
		return drop_debug_;
	}
	/**
	 * @param drop_debug_ The drop_debug_ to set.
	 */
	public void setDrop_debug_(boolean drop_debug_) {
		this.drop_debug_ = drop_debug_;
	}
	/**
	 * @return Returns the lpexp_.
	 */
	public double getLpexp_() {
		return lpexp_;
	}
	/**
	 * @param lpexp_ The lpexp_ to set.
	 */
	public void setLpexp_(double lpexp_) {
		this.lpexp_ = lpexp_;
	}
	/**
	 * @return Returns the pdesync_.
	 */
	public double getPdesync_() {
		return pdesync_;
	}
	/**
	 * @param pdesync_ The pdesync_ to set.
	 */
	public void setPdesync_(double pdesync_) {
		this.pdesync_ = pdesync_;
	}
	/**
	 * @return Returns the peri_proact_.
	 */
	public boolean isPeri_proact_() {
		return peri_proact_;
	}
	/**
	 * @param peri_proact_ The peri_proact_ to set.
	 */
	public void setPeri_proact_(boolean peri_proact_) {
		this.peri_proact_ = peri_proact_;
	}
	/**
	 * @return Returns the pint_.
	 */
	public double getPint_() {
		return pint_;
	}
	/**
	 * @param pint_ The pint_ to set.
	 */
	public void setPint_(double pint_) {
		this.pint_ = pint_;
	}
	/**
	 * @return Returns the use_implicit_beacon_.
	 */
	public boolean isUse_implicit_beacon_() {
		return use_implicit_beacon_;
	}
	/**
	 * @param use_implicit_beacon_ The use_implicit_beacon_ to set.
	 */
	public void setUse_implicit_beacon_(boolean use_implicit_beacon_) {
		this.use_implicit_beacon_ = use_implicit_beacon_;
	}
	/**
	 * @return Returns the use_loop_detect_.
	 */
	public boolean isUse_loop_detect_() {
		return use_loop_detect_;
	}
	/**
	 * @param use_loop_detect_ The use_loop_detect_ to set.
	 */
	public void setUse_loop_detect_(boolean use_loop_detect_) {
		this.use_loop_detect_ = use_loop_detect_;
	}
	/**
	 * @return Returns the use_mac_.
	 */
	public boolean isUse_mac_() {
		return use_mac_;
	}
	/**
	 * @param use_mac_ The use_mac_ to set.
	 */
	public void setUse_mac_(boolean use_mac_) {
		this.use_mac_ = use_mac_;
	}
	/**
	 * @return Returns the use_peri_.
	 */
	public boolean isUse_peri_() {
		return use_peri_;
	}
	/**
	 * @param use_peri_ The use_peri_ to set.
	 */
	public void setUse_peri_(boolean use_peri_) {
		this.use_peri_ = use_peri_;
	}
	/**
	 * @return Returns the use_planar_.
	 */
	public boolean isUse_planar_() {
		return use_planar_;
	}
	/**
	 * @param use_planar_ The use_planar_ to set.
	 */
	public void setUse_planar_(boolean use_planar_) {
		this.use_planar_ = use_planar_;
	}
	/**
	 * @return Returns the use_timed_plnrz_.
	 */
	public boolean isUse_timed_plnrz_() {
		return use_timed_plnrz_;
	}
	/**
	 * @param use_timed_plnrz_ The use_timed_plnrz_ to set.
	 */
	public void setUse_timed_plnrz_(boolean use_timed_plnrz_) {
		this.use_timed_plnrz_ = use_timed_plnrz_;
	}
	/**
	 * @return Returns the verbose_.
	 */
	public boolean isVerbose_() {
		return verbose_;
	}
	/**
	 * @param verbose_ The verbose_ to set.
	 */
	public void setVerbose_(boolean verbose_) {
		this.verbose_ = verbose_;
	}
	
	/**
	 * @return Returns the cbrPacketSize.
	 */
	public int getCbrPacketSize() {
		return cbrPacketSize;
	}
	/**
	 * @param cbrPacketSize The cbrPacketSize to set.
	 */
	public void setCbrPacketSize(int cbrPacketSize) {
		this.cbrPacketSize = cbrPacketSize;
	}
	/**
	 * @return Returns the cbrRate.
	 */
	public int getCbrRate() {
		return cbrRate;
	}
	/**
	 * @param cbrRate The cbrRate to set.
	 */
	public void setCbrRate(int cbrRate) {
		this.cbrRate = cbrRate;
	}
	/**
	 * @return Returns the pathloss.
	 */
	public int getPathloss() {
		return pathloss;
	}
	/**
	 * @param pathloss The pathloss to set.
	 */
	public void setPathloss(int pathloss) {
		this.pathloss = pathloss;
	}
	/**
	 * @return Returns the useCBR.
	 */
	public boolean isUseCBR() {
		return useCBR;
	}
	/**
	 * @param useCBR The useCBR to set.
	 */
	public void setUseCBR(boolean useCBR) {
		this.useCBR = useCBR;
	}
	
	/**
	 * @return Returns the radioNoiseType.
	 */
	public int getRadioNoiseType() {
		return radioNoiseType;
	}
	/**
	 * @param radioNoiseType The radioNoiseType to set.
	 */
	public void setRadioNoiseType(int radioNoiseType) {
		this.radioNoiseType = radioNoiseType;
	}
	/**
	 * @return Returns the packetSize.
	 */
	public int getPacketSize() {
		return packetSize;
	}
	/**
	 * @param packetSize The packetSize to set.
	 */
	public void setPacketSize(int packetSize) {
		this.packetSize = packetSize;
	}
	/**
	 * @return Returns the mac_promisc.
	 */
	public boolean isMac_promisc() {
		return mac_promisc;
	}
	/**
	 * @param mac_promisc The mac_promisc to set.
	 */
	public void setMac_promisc(boolean mac_promisc) {
		this.mac_promisc = mac_promisc;
	}
	/**
	 * @return Returns the useVisualzer.
	 */
	public boolean isUseVisualizer() {
		return useVisualizer;
	}
	/**
	 * @param useVisualzer The useVisualzer to set.
	 */
	public void setUseVisualizer(boolean useVisualizer) {
		this.useVisualizer = useVisualizer;
	}
    /**
     * @return Returns the flowFile.
     */
    public String getFlowFile() {
        return flowFile;
    }
    /**
     * @param flowFile The flowFile to set.
     */
    public void setFlowFile(String flowFile) {
        this.flowFile = flowFile;
    }
    
    /**
     * @return Returns the stepTime.
     */
    public double getStepTime() {
        return stepTime;
    }
    /**
     * @param stepTime The stepTime to set.
     */
    public void setStepTime(double stepTime) {
        this.stepTime = stepTime;
    }
    /**
     * @return Returns the useFlows.
     */
    public boolean isUseFlows() {
        return useFlows;
    }
    /**
     * @param useFlows The useFlows to set.
     */
    public void setUseFlows(boolean useFlows) {
        this.useFlows = useFlows;
    }

	
	/**
	 * @return Returns the laneChangeModel.
	 */
	public int getLaneChangeModel() {
		return laneChangeModel;
	}
	/**
	 * @param laneChangeModel The laneChangeModel to set.
	 */
	public void setLaneChangeModel(int laneChangeModel) {
		this.laneChangeModel = laneChangeModel;
	}
   
    /**
     * @return Returns the showInterference.
     */
    public boolean isShowInterference() {
        return showInterference;
    }
    /**
     * @param showInterference The showInterference to set.
     */
    public void setShowInterference(boolean showInterference) {
        this.showInterference = showInterference;
    }
    /**
     * @return Returns the showTitle.
     */
    public boolean isShowTitle() {
        return showTitle;
    }
    /**
     * @param showTitle The showTitle to set.
     */
    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    
    /**
     * @return Returns the closeWhenDone.
     */
    public boolean isCloseWhenDone() {
        return closeWhenDone;
    }
    /**
     * @param closeWhenDone The closeWhenDone to set.
     */
    public void setCloseWhenDone(boolean closeWhenDone) {
        this.closeWhenDone = closeWhenDone;
    }
    /**
     * @return Returns the measureMemory.
     */
    public boolean isMeasureMemory() {
        return measureMemory;
    }
    /**
     * @param measureMemory The measureMemory to set.
     */
    public void setMeasureMemory(boolean measureMemory) {
        this.measureMemory = measureMemory;
    }
	/**
	 * @return Returns the measureThroughput.
	 */
	public boolean isMeasureThroughput() {
		return measureThroughput;
	}
	/**
	 * @param measureThroughput The measureThroughput to set.
	 */
	public void setMeasureThroughput(boolean measureThroughput) {
		this.measureThroughput = measureThroughput;
	}
	
}