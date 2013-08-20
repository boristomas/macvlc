/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         GenericDriver.java
 * RCS:          $Id: GenericDriver.java,v 1.1 2007/04/09 18:49:31 drchoffnes Exp $
 * Description:  GenericDriver class (see below)
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Fading;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.PathLoss;
import jist.swans.field.Placement;
import jist.swans.field.Spatial;
import jist.swans.field.StreetMobility;
import jist.swans.field.StreetMobilityOD;
import jist.swans.field.StreetMobilityRandom;
import jist.swans.field.StreetPlacementFlow;
import jist.swans.field.StreetPlacementRandom;
import jist.swans.field.streets.LaneChangeModel;
import jist.swans.field.streets.Mobil;
import jist.swans.mac.Mac802_11;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacDumb;
import jist.swans.mac.MacInterface;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Message;
import jist.swans.misc.MessageBytes;
import jist.swans.misc.Util;
import jist.swans.net.MessageQueue;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIp;
import jist.swans.net.NetMessage;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioVLC;
import jist.swans.radio.Vizbt;
import jist.swans.route.RouteAodv;
import jist.swans.route.RouteDsr;
import jist.swans.route.RouteDsr_Ns2;
import jist.swans.route.RouteGPSR;
import jist.swans.route.RouteInterface;
import jist.swans.route.geo.Ideal;
import jist.swans.trans.TransUdp;


/**
* Generic simulation configurator. Uses JistExperiment for settings.
* Derived from aodvsim. Future versions will include statistics-gathering 
* functionality, backed by a database.
*
* @author David Choffnes
*
*/
public class GenericDriver {
    /** tracks memory consumption over time */
    private static Vector memoryConsumption = new Vector();

    /** tracks events per second over time */
    private static LinkedHashMap eventsPerSecond = new LinkedHashMap();
    private final static boolean VERBOSE = false;
    private static final int SEND_JITTER = 100;
    private static Ideal locDB = null;
    static int idUdp = 0;

    /**
    * Add node to the field and start it.
    *
    * @param opts command-line options
    * @param i node number, which also serves as its address
    * @param nodes list of nodes to be appended to
    * @param stats statistics collector
    * @param field simulation field
    * @param place node placement model
    * @param radioInfo shared radio information
    * @param protMap registered protocol map
    * @param inLoss packet incoming loss model
    * @param outLoss packet outgoing loss model
    * @param mobility the mobility model to use
    * @param v the visualization object
    */
    public static Vizbt btviz = null;
    @SuppressWarnings("unchecked")
	public static void addNode(JistExperiment je, int i, Vector nodes,				//TODO: START HERE
        Field field, Placement place, RadioInfo.RadioInfoShared radioInfo,
        Mapper protMap, PacketLoss inLoss, PacketLoss outLoss,
        Mobility mobility, VisualizerInterface v) {
        RadioNoise radio;
        Location location;

        if(btviz == null)
        {
        	btviz = new Vizbt();
        }
        if (nodes != null) {
            // radio
        	location = place.getNextLocation();//bt
        	radio = new RadioVLC(i, radioInfo, location);
        	
  /*bt          switch (je.radioNoiseType) {
            case Constants.RADIO_NOISE_INDEP:
                radio = new RadioNoiseIndep(i, radioInfo);
                break;

            case Constants.RADIO_NOISE_ADDITIVE:
                radio = new RadioNoiseAdditive(i, radioInfo);

                break;

            default:
                throw new RuntimeException("Invalid radio model!");
            }
*/
            // placement
            

            if (location == null) {
                return;
            }

            field.addRadio(radio.getRadioInfo(), radio.getProxy(), location, (RadioVLC)radio);
            field.startMobility(radio.getRadioInfo().getUnique().getID());
        } else // nodes that are not participating in transmission
         {
            // radio
            radio = new RadioNoiseIndep(i, radioInfo);
            // placement
            location = place.getNextLocation();

            if (location == null) {
                return;
            }

            field.addRadio(radio.getRadioInfo(), radio.getProxy(), location, (RadioVLC)radio);
            field.startMobility(radio.getRadioInfo().getUnique().getID());

            // add node to GUI
            if ((v != null) && je.useVisualizer) {
                if (i <= je.nodes) {
                    v.addNode(location.getX(), location.getY(), i);
                } else {
                    v.addNode(location.getX(), location.getY(), i, 1);
                }
            }

            return;
        }

        // add node to GUI
        if ((v != null) && je.useVisualizer) {
            if (i <= je.nodes) {
                v.addNode(location.getX(), location.getY(), i);
            } else {
                v.addNode(location.getX(), location.getY(), i, 1);
            }
        }

        MacInterface mac = new Mac802_11(new MacAddress(i), radio.getRadioInfo());
        MacInterface macProxy = null;

        switch (je.mac) {
        case Constants.MAC_802_11:
            mac = new Mac802_11(new MacAddress(i), radio.getRadioInfo());
            ((Mac802_11) mac).setRadioEntity(radio.getProxy());

            macProxy = ((Mac802_11) mac).getProxy();

            break;

        case Constants.MAC_DUMB:
            mac = new MacDumb(new MacAddress(i), radio.getRadioInfo());
            macProxy = ((MacDumb) mac).getProxy();

            break;
        }

        if (mobility instanceof StreetMobility) {
            StreetMobility sm = (StreetMobility) mobility;
            StreetMobility.mobInfoHash.put(new Integer(i),
                field.getRadioData(new Integer(i)).getMobilityInfo());
        }

        // network
        final NetAddress address = new NetAddress(i);
        NetIp net = new NetIp(address, protMap, inLoss, outLoss /*, ipStats*/);

        if (je.mac == Constants.MAC_802_11) {
            ((Mac802_11) mac).setNetEntity(net.getProxy(),
                (byte) Constants.NET_INTERFACE_DEFAULT);
        }

        // transport
        TransUdp udp = new TransUdp();

        // node entity hookup
        radio.setFieldEntity(field.getProxy());
        radio.setMacEntity(macProxy);

        byte intId = net.addInterface(macProxy,
                new MessageQueue.DropMessageQueue(Constants.NET_PRIORITY_NUM,
                    300));
        mac.setRadioEntity(radio.getProxy());
        mac.setNetEntity(net.getProxy(), intId);
        udp.setNetEntity(net.getProxy());

        net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp.getProxy());

        locDB.setLocation(new NetAddress(i), new Integer(i));

        RouteInterface route = null;

        // routing
        switch (je.protocol) {
        case Constants.NET_PROTOCOL_AODV:

            RouteAodv aodv = new RouteAodv(address);
            aodv.setNetEntity(net.getProxy());
            aodv.getProxy().start();

            route = aodv.getProxy();

            //net.setProtocolHandler(Constants.NET_PROTOCOL_AODV, myHandler);
            break;

        case Constants.NET_PROTOCOL_DSR:

            //             mac = new Mac802_11(new MacAddress(i), radio.getRadioInfo(), true);
            //             macProxy = ((Mac802_11)mac).getProxy();
            RouteDsr dsr = new RouteDsr(address);
            dsr.setNetEntity(net.getProxy());
            //dsr.getProxy().start();
            route = dsr.getProxy();

            break;

        case Constants.NET_PROTOCOL_DSR_NS2:

            RouteDsr_Ns2 dsr2 = new RouteDsr_Ns2(address.toInt());
            dsr2.setNetEntity(net.getProxy());
            dsr2.setRadio(radio);
            dsr2.getProxy().start();
            route = dsr2.getProxy();

            break;

        case Constants.NET_PROTOCOL_GPSR:

            RouteGPSR gpsr = null;

            if (je.GPSR_ldb == Constants.GPSR_LOCATIONDB_IDEAL) {
                gpsr = new RouteGPSR(field, i, locDB);
            }

            //         gpsr.setStats(gpsrStats); // now it is static
            gpsr.setNetEntity(net.getProxy());
            gpsr.getProxy().start();
            route = gpsr.getProxy();

            //gpsr.setStats(zrpStats);
            break;

        default:
            throw new RuntimeException("invalid routing protocol");
        }

        net.setProtocolHandler(je.protocol, route);
        net.setRouting(route);

        nodes.add(route);
    }

    /**
    * Constructs field and nodes with given command-line options, establishes
    * client/server pairs and starts them.
    *
    * @param je command-line parameters
    * @param nodes vectors to place zrp objects into
    */
    private static void buildField(JistExperiment je, final Vector nodes) {
        // initialize node mobility model
        Mobility mobility = null;
        Location.Location2D tr;
        Location.Location2D bl;

        // set the random seed, if necessary
        Random r;

        if (je.seed == -1) {
            r = new Random();
        } else {
            r = new Random(je.seed);
        }

        // set random object for use in other simulation objects
        je.random = r;

        LaneChangeModel lcm = null;

        switch (je.mobility) {
        case Constants.MOBILITY_STATIC:
            mobility = new Mobility.Static();

            break;

        case Constants.MOBILITY_WAYPOINT:
            mobility = new Mobility.RandomWaypoint(je.field, je.pause_time,
                    je.granularity, je.max_speed, je.min_speed, r);

            //mobility = new Mobility.RandomWaypoint(je.field, je.mobilityOpts);
            break;

        case Constants.MOBILITY_TELEPORT:
            mobility = new Mobility.Teleport(je.field,
                    Long.parseLong(je.mobilityOpts));

            break;

        case Constants.MOBILITY_WALK:
            mobility = new Mobility.RandomWalk(je.field, je.mobilityOpts);

            break;

        case Constants.MOBILITY_STRAW_SIMPLE:
            tr = new Location.Location2D(je.maxLong, je.maxLat);
            bl = new Location.Location2D(je.minLong, je.minLat);
            mobility = new StreetMobilityRandom(je.segmentFile, je.streetFile,
                    je.shapeFile, je.degree, je.probability, je.granularity,
                    bl, tr, r);

            break;

        case Constants.MOBILITY_STRAW_OD:
            tr = new Location.Location2D(je.maxLong, je.maxLat);
            bl = new Location.Location2D(je.minLong, je.minLat);
            mobility = new StreetMobilityOD(je.segmentFile, je.streetFile,
                    je.shapeFile, je.degree, bl, tr, r);

            break;

        default:
            throw new RuntimeException("unknown node mobility model");
        }

        //if street mobility, get lane change model
        if ((je.mobility == Constants.MOBILITY_STRAW_OD) ||
                (je.mobility == Constants.MOBILITY_STRAW_SIMPLE)) {
            switch (je.laneChangeModel) {
            case Constants.LANE_CHANGE_MOBIL:
                lcm = new Mobil((StreetMobility) mobility, je.driverStdDev);
                ((StreetMobility) mobility).setLcm(lcm);

                break;

            default:
                //lcm = null;//bt
            	   lcm = new Mobil((StreetMobility) mobility, je.driverStdDev);
                   ((StreetMobility) mobility).setLcm(lcm);

                break;
            }
        }

        // initialize spatial binning
        Spatial spatial = null;

        // make all four points
        Location.Location2D[] corners = new Location.Location2D[4];

        if ((je.mobility != Constants.MOBILITY_STRAW_SIMPLE) &&
                (je.mobility != Constants.MOBILITY_STRAW_OD)) {
            corners[0] = new Location.Location2D(0, 0);
            corners[1] = new Location.Location2D(je.field.getX(), 0);
            corners[2] = new Location.Location2D(0, je.field.getY());
            corners[3] = new Location.Location2D(je.field.getX(),
                    je.field.getY());
        } else {
            je.sm = (StreetMobility) mobility;

            StreetMobility smr = (StreetMobility) mobility;
            Location.Location2D[] cornersTemp = new Location.Location2D[4];
            cornersTemp = (Location.Location2D[]) smr.getBounds();
            corners[0] = cornersTemp[2];
            corners[1] = cornersTemp[3];
            corners[2] = cornersTemp[0];
            corners[3] = cornersTemp[1];
            System.out.println("Area: " + je.sm.getArea());
        }

        switch (je.spatial_mode) {
        case Constants.SPATIAL_LINEAR:
            spatial = new Spatial.LinearList(corners[0], corners[1],
                    corners[2], corners[3]);

            break;

        case Constants.SPATIAL_GRID:
            spatial = new Spatial.Grid(corners[0], corners[1], corners[2],
                    corners[3], je.spatial_div);

            break;

        case Constants.SPATIAL_HIER:
            spatial = new Spatial.HierGrid(corners[0], corners[1], corners[2],
                    corners[3], je.spatial_div);

            break;

        default:
            throw new RuntimeException("unknown spatial binning model");
        }

        if (je.wrapField) {
            spatial = new Spatial.TiledWraparound(spatial);
        }

        PathLoss pl;

        // pathloss model
        switch (je.pathloss) {
        case Constants.PATHLOSS_FREE_SPACE:
            pl = new PathLoss.FreeSpace();

            break;

        case Constants.PATHLOSS_SHADOWING:
            pl = new PathLoss.Shadowing(je.exponent, je.stdDeviation);

            break;

        case Constants.PATHLOSS_TWO_RAY:
            pl = new PathLoss.TwoRay();

            break;

        default:
            throw new RuntimeException("Unsupported pathloss model!");
        }

        Visualizer v = null;

        if (je.useVisualizer) {
            v = new Visualizer();            
            mobility.setGUI(v); // TODO deprecate this and use static getter from Visualizer
        }

        je.visualizer = v;

        // initialize field
        Field field = new Field(spatial, new Fading.None(), pl, mobility,
                Constants.PROPAGATION_LIMIT_DEFAULT);
        locDB = new Ideal(field);

        if (v != null) {
            v.setField(field);
        }

        // initialize shared radio information
        RadioInfo.RadioInfoShared radioInfo = RadioInfo.createShared(je.frequency,
                je.bandwidth, je.transmit, je.gain,
                Util.fromDB(je.sensitivity), Util.fromDB(je.threshold),
                je.temperature, je.temperature_factor, je.ambiant_noise);

        // initialize shared raido information for non participating nodes
        RadioInfo.RadioInfoShared noRadio = RadioInfo.createShared(je.frequency,
                je.bandwidth, 0, 0, Util.fromDB(10000), Util.fromDB(10000),
                je.temperature, je.temperature_factor, je.ambiant_noise);

        if (pl instanceof PathLoss.Shadowing) {
            PathLoss.Shadowing pls = (PathLoss.Shadowing) pl;
            double maxDist = pls.computeMaxDistance(radioInfo, je.sensitivity,
                    je.stdDeviation);
            System.out.println("Base xmit distance = " + maxDist);

            if (je.useVisualizer) {
                v.setBaseTranmit(maxDist);
            }

            je.transmitRadius = maxDist;
        }

        // initialize shared protocol mapper
        Mapper protMap = new Mapper(new int[] {
                    Constants.NET_PROTOCOL_UDP, je.protocol,
                    Constants.NET_PROTOCOL_HEARTBEAT, Constants.NET_PROTOCOL_VFN,
                    Constants.NET_PROTOCOL_NT, Constants.NET_PROTOCOL_DSR,
                    Constants.NET_PROTOCOL_DSR_NS2
                });

        // initialize packet loss models
        PacketLoss outLoss = new PacketLoss.Zero();
        PacketLoss inLoss = null;

        switch (je.loss) {
        case Constants.NET_LOSS_NONE:
            inLoss = new PacketLoss.Zero();

            break;

        case Constants.NET_LOSS_UNIFORM:
            inLoss = new PacketLoss.Uniform(Double.parseDouble(je.lossOpts));

            break;

        default:
            throw new RuntimeException("unknown packet loss model");
        }

        // initialize node placement model
        Placement place = null;
        Placement placeFlow = null; // for traffic flows
        StreetMobility smr;
        Location.Location2D[] bounds;

        switch (je.placement) {
        case Constants.PLACEMENT_RANDOM:
            place = new Placement.Random(je.field);

            break;

        case Constants.PLACEMENT_GRID:
            je.setPlacementOpts("");
            place = new Placement.Grid(je.field, je.placementOpts);

            break;

        case Constants.PLACEMENT_STREET_RANDOM:
            smr = (StreetMobility) mobility;
            bounds = (Location.Location2D[]) smr.getBounds();
            place = new StreetPlacementRandom(bounds[0], bounds[3], smr,
                    je.driverStdDev, je.stepTime);

            if (je.useFlows) {
                loadFlows(je);
                placeFlow = new StreetPlacementFlow(bounds[0], bounds[3], smr,
                        je.driverStdDev, je.stepTime, je.startLocs, je.endLocs,
                        je.nodes);
            }

            break;

        case Constants.PLACEMENT_STREET_CIRCUIT:
            smr = (StreetMobility) mobility;
            bounds = (Location.Location2D[]) smr.getBounds();

            break;

        default:
            throw new RuntimeException("unknown node placement model");
        }

        // set total number of nodes
        int totalNodes = je.nodes + je.staticNodes;

        if (je.startLocs != null) {
            for (int l = 0; l < je.startLocs.length; l++)
                totalNodes += ((je.duration + je.startTime + je.resolutionTime) / je.flowRates[l].intValue());
        }

        if (je.visualizer != null) {
            je.visualizer.setNumberOfNodes(totalNodes);
        }

        int i;

        // create each mobile node
        for (i = 1; i <= je.nodes; i++) {
            if ((je.penetrationRatio < 1.0f) &&
                    (((je.penetrationRatio * i) -
                    Math.floor(je.penetrationRatio * i)) >= je.penetrationRatio)) {
                // create nodes that won't transmit
                addNode(je, i, null, field, place, noRadio, protMap, inLoss,
                    outLoss, mobility, v);
            } else // create nodes that will transmit
             {
                addNode(je, i, nodes, field, place, radioInfo, protMap, inLoss,
                    outLoss, mobility, v);
            }
        }

        // create each static node
        if (je.staticNodes >= 1) {
            Placement staticPlace = null;

            switch (je.staticPlacement) {
            case Constants.PLACEMENT_RANDOM:
                staticPlace = new Placement.Random(je.field);

                break;

            case Constants.PLACEMENT_GRID:
                staticPlace = new Placement.Grid(je.field,
                        je.staticPlacementOpts);

                break;

            case Constants.PLACEMENT_STREET_RANDOM: // TODO update and add one for intersections only
                smr = (StreetMobility) mobility;
                bounds = (Location.Location2D[]) smr.getBounds();
                staticPlace = new StreetPlacementRandom(bounds[0], bounds[3],
                        smr, je.stdDeviation, je.stepTime);

                break;

            default:
                throw new RuntimeException("unknown node placement model");
            }

            Field staticField = new Field(spatial, new Fading.None(),
                    new PathLoss.FreeSpace(), new Mobility.Static(),
                    Constants.PROPAGATION_LIMIT_DEFAULT /* check */);

            // initialize shared radio information
            RadioInfo.RadioInfoShared staticRadioInfo = RadioInfo.createShared(je.frequency,
                    je.bandwidth, je.staticTransmit, je.staticGain,
                    Util.fromDB(je.staticSensitivity), // TODO update these with decent values
                    Util.fromDB(je.threshold), je.temperature,
                    je.temperature_factor, je.ambiant_noise);
            int max = je.staticNodes + i;

            for (int j = i; j <= max; j++, i++) {
                addNode(je, j, nodes, staticField, staticPlace,
                    staticRadioInfo, protMap, inLoss, outLoss, null, v);
            }
        } // end if static nodes

        // schedule flows of vehicles
        if (je.useFlows && ((je.mobility == 5) || (je.mobility == 6))) {
            for (int k = 0; k < je.startLocs.length; k++) {
                int startValue = i;

                for (int l = 0; l < k; l++)
                    startValue += ((je.duration + je.startTime +
                    je.resolutionTime) / je.flowRates[l].intValue());

                FlowHandler fh = new FlowHandler(je, startValue, nodes, field,
                        placeFlow, radioInfo, protMap, inLoss, outLoss,
                        mobility, v, k);

                for (long time = 0;
                        time < (je.duration + je.startTime + je.resolutionTime);
                        time += je.flowRates[k].intValue()) {
                    JistAPI.runAt(fh, time * Constants.SECOND);
                }
            }
        }

        // pick random sources
        Vector sources = new Vector();
        int num_sources = 0;
        final int MAX_SOURCES = je.transmitters;
        boolean[] nodelist = new boolean[nodes.size()];

        // random object for predicatability between runs
        Random myRandom = new Random(0);

        if (nodes.size() > 0) {
            int index = myRandom.nextInt(nodes.size());

            while (num_sources < MAX_SOURCES) {
                sources.add(new Integer(index));
                nodelist[index] = true;

                do {
                    index = myRandom.nextInt(nodes.size());
                } while ((nodelist[index] == true) &&
                        (num_sources < (MAX_SOURCES - 1)));

                num_sources++;
            }

            // set up message sending events
            JistAPI.sleep(je.startTime * Constants.SECOND);

            if (je.useCBR) {
                generateCBRTraffic(je, sources, nodes, myRandom);
            }
            else {
                int numTotalMessages = (int) Math.floor(((double) je.sendRate / 60) * MAX_SOURCES * je.duration);
                long delayInterval = (long) Math.ceil(((double) je.duration * (double) Constants.SECOND) / (double) numTotalMessages);
                System.out.println("Messages to deliver: " + numTotalMessages);

                // send messages
                for (i = 0; i < numTotalMessages; i++) {
                    //pick random send node
                    int srcIdx = myRandom.nextInt(sources.size());
                    int destIdx;

                    do {
                        //pick random dest node
                        destIdx = myRandom.nextInt(nodes.size());
                    } while (nodes.elementAt(destIdx) == sources.elementAt(
                                srcIdx));

                    // store time sent as payload 
                    //     int id = 0;
                    byte[] data = new byte[je.packetSize];
                    Message payload = new MessageBytes(data);
                    NetMessage msg;
                    TransUdp.UdpMessage udpMsg = new TransUdp.UdpMessage(je.port,
                            je.port, payload);

                    switch (je.protocol) {
                    case Constants.NET_PROTOCOL_AODV:

                        RouteInterface srcAodv = (RouteInterface) nodes.elementAt((Integer) sources.elementAt(
                                    srcIdx));
                        RouteInterface destAodv = (RouteInterface) nodes.elementAt(destIdx);

                        msg = new NetMessage.Ip(udpMsg,
                                new NetAddress((Integer) sources.elementAt(
                                        srcIdx) + 1),
                                new NetAddress(destIdx + 1),
                                Constants.NET_PROTOCOL_UDP,
                                Constants.NET_PRIORITY_NORMAL,
                                (byte) Constants.TTL_DEFAULT);
                        srcAodv.send(msg);

                        break;

                    case Constants.NET_PROTOCOL_DSR:

                        RouteDsr srcDsr = (RouteDsr) nodes.elementAt((Integer) sources.elementAt(
                                    srcIdx));
                        RouteDsr destDsr = (RouteDsr) nodes.elementAt(destIdx);

                        msg = new NetMessage.Ip(udpMsg, srcDsr.getLocalAddr(),
                                destDsr.getLocalAddr(),
                                Constants.NET_PROTOCOL_UDP,
                                Constants.NET_PRIORITY_NORMAL,
                                (byte) Constants.TTL_DEFAULT);
                        srcDsr.getProxy().send(msg);

                        break;

                    case Constants.NET_PROTOCOL_DSR_NS2:

                        RouteDsr_Ns2 srcDsr_ns2 = (RouteDsr_Ns2) nodes.elementAt((Integer) sources.elementAt(
                                    srcIdx));
                        RouteDsr_Ns2 destDsr_ns2 = (RouteDsr_Ns2) nodes.elementAt(destIdx);

                        msg = new NetMessage.Ip(udpMsg,
                                srcDsr_ns2.getLocalAddr(),
                                destDsr_ns2.getLocalAddr(),
                                Constants.NET_PROTOCOL_UDP,
                                Constants.NET_PRIORITY_NORMAL,
                                (byte) Constants.TTL_DEFAULT);
                        srcDsr_ns2.getProxy().send(msg);

                        break;

                    case Constants.NET_PROTOCOL_GPSR:

                        RouteGPSR srcGPSR = (RouteGPSR) nodes.elementAt((Integer) sources.elementAt(
                                    srcIdx));
                        RouteGPSR destGPSR = (RouteGPSR) nodes.elementAt(destIdx);

                        msg = new NetMessage.Ip(udpMsg, srcGPSR.getLocalAddr(),
                                destGPSR.getLocalAddr(),
                                Constants.NET_PROTOCOL_UDP,
                                Constants.NET_PRIORITY_NORMAL,
                                Constants.TTL_DEFAULT);
                        srcGPSR.getProxy().send(msg);

                        break;

                    default:
                        throw new RuntimeException(
                            "Unsupported routing protocol!");
                    } // end switch

                    JistAPI.sleep(delayInterval);
                }
            }
        } // end if nodes

        if (je.measureMemory) { // get memory usage every 5 %

            int numTotalIters = 20;
            long delayInterval = (long) Math.ceil(((double) je.duration * (double) Constants.SECOND) / (double) numTotalIters);
            long currentTime = 0;

            for (int j = 0; j < numTotalIters; j++) {
                JistAPI.runAt(new Runnable() {
                        public void run() {
                            long baseMem = jist.runtime.Util.getUsedMemory();
                            //            long threadMem = Visualizer.getActiveInstance().getUsedMemory();
                            memoryConsumption.add(new Long(baseMem));
                        }
                    }, currentTime);

                currentTime += delayInterval;
            }
        }
    } // buildField

    /**
     * Loads patterns for mobility around fixed, closed routes.
     * @param smod the street mobility object
     * @param circuitFile the file containing the routes
     */
    private static void loadCircuits(StreetMobilityOD smod, String circuitFile) {
        FileInputStream fis;

        try {
            fis = new FileInputStream(circuitFile);

            ObjectInputStream ois = new ObjectInputStream(fis);

            smod.routes = (LinkedList[]) ois.readObject();

            ois.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            //      e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Loads data for flows of vehicles.
     * @param je the configuration object
     */
    private static void loadFlows(JistExperiment je) {
        Vector starts = new Vector();
        Vector ends = new Vector();
        Vector rates = new Vector();
        String line;
        String[] parsed;

        try {
            BufferedReader in = new BufferedReader(new FileReader(je.flowFile));

            if (!in.ready()) {
                throw new IOException();
            }

            while ((line = in.readLine()) != null) {
                parsed = line.split("[\\(\\),:]");
                //            System.out.println(Arrays.toString(parsed));
                rates.add(Integer.valueOf(parsed[0]));
                starts.add(new Location.Location2D(
                        Float.valueOf(parsed[2]).floatValue() * (float) StreetMobility.METERS_PER_DEGREE,
                        Float.valueOf(parsed[3]).floatValue() * (float) StreetMobility.METERS_PER_DEGREE));
                ends.add(new Location.Location2D(
                        Float.valueOf(parsed[6]).floatValue() * (float) StreetMobility.METERS_PER_DEGREE,
                        Float.valueOf(parsed[7]).floatValue() * (float) StreetMobility.METERS_PER_DEGREE));
            }

            in.close();
            je.startLocs = new Location.Location2D[starts.size()];
            starts.toArray(je.startLocs);
            je.endLocs = new Location.Location2D[ends.size()];
            ends.toArray(je.endLocs);
            je.flowRates = new Integer[rates.size()];
            rates.toArray(je.flowRates);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Genereates constant bitrate (CBR) traffic.
     * @param je the configuration object
     * @param sources sources for traffic
     * @param nodes set of all nodes
     * @param myRandom the random object to use
     */
    private static void generateCBRTraffic(JistExperiment je, Vector sources,
        Vector nodes, Random myRandom) {
        long delayInterval = (long) (((double) je.cbrPacketSize / je.cbrRate) * 1 * Constants.SECOND);
        long iterations = (long) Math.ceil(((double) je.duration * (double) Constants.SECOND) / delayInterval);
        byte[] data = new byte[je.cbrPacketSize];
        Message payload = new MessageBytes(data);
        long currentTime = je.startTime * Constants.SECOND;

        System.out.println("Messages to send: " +
            (iterations * je.transmitters));

        int[] dests = new int[je.transmitters];
        boolean[] chosen = new boolean[nodes.size()];

        // pick destinations for streams
        for (int i = 0; i < je.transmitters; i++) {
            //pick send node
            do {
                //pick random dest node
                dests[i] = myRandom.nextInt(nodes.size());
            } while ((dests[i] == ((Integer) sources.get(i)).intValue()) ||
                    chosen[dests[i]]);

            chosen[dests[i]] = true;
        }

        // send messages
        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < je.transmitters; j++) {
                //             if (j%numProtocols == 0 ) currentProtocol = je.protocol;
                //             else currentProtocol = je.protocol2;

                // store time sent as payload 
                NetMessage msg;
                TransUdp.UdpMessage udpMsg = new TransUdp.UdpMessage(je.port,
                        je.port, payload);

                int src = ((Integer) sources.get(j)).intValue();
                int dest = dests[j] + 1;

                //             NetIp srcNet = (NetIp) sources.elementAt(j);
                //             NetIp destNet = (NetIp) nodes.elementAt(dests[j]);
                //             
                //             srcNet.send(udpMsg, destNet.getAddress(), (short)currentProtocol, 
                //                     Constants.NET_PRIORITY_NORMAL, Constants.TTL_DEFAULT);
                RouteInterface srcRoute = (RouteInterface) nodes.elementAt(src);

                msg = new NetMessage.Ip(udpMsg,
                        new NetAddress((1 + src) * (int) (Math.ceil(
                                1 / je.penetrationRatio))),
                        new NetAddress(dest * (int) (Math.ceil(
                                1 / je.penetrationRatio))),
                        Constants.NET_PROTOCOL_UDP,
                        Constants.NET_PRIORITY_NORMAL,
                        (byte) Constants.TTL_DEFAULT);
                srcRoute.send(msg);
            } // send message for each transmitter

            JistAPI.sleep(delayInterval +
                (long) (SEND_JITTER * Constants.MICRO_SECOND * Constants.random.nextDouble()));
            currentTime += delayInterval;
        }
    }

    /**
     * Display statistics at end of simulation.
     * @param nodes list of nodes
     * @param je the configuation object
     * @param startTime the start time for simulation
     * @param freeMemory the amount of free memory
     */
    public static void showStats(Vector nodes, final JistExperiment je,
        Date startTime, final long freeMemory) {

        String output = "";

        Date endTime = new Date();
        long elapsedTime = endTime.getTime() - startTime.getTime();

        output += (((float) elapsedTime / 1000) + "\t");

        if (je.measureMemory) {
            output += printMemStats();
        }

        System.out.println();
        System.gc();
        System.out.println("freemem:  " + Runtime.getRuntime().freeMemory());
        System.out.println("maxmem:   " + Runtime.getRuntime().maxMemory());
        System.out.println("totalmem: " + Runtime.getRuntime().totalMemory());

        long usedMem = Runtime.getRuntime().totalMemory() -
            Runtime.getRuntime().freeMemory();
        System.out.println("used:     " + usedMem);

        System.out.println("start time  : " + startTime);
        System.out.println("end time    : " + endTime);
        System.out.println("elapsed time: " + elapsedTime);
        System.out.flush();

        if ((je.mobility == Constants.MOBILITY_STRAW_SIMPLE) ||
                (je.mobility == Constants.MOBILITY_STRAW_OD)) {
            output += (je.sm.printAverageSpeed(je.duration + je.startTime +
                je.resolutionTime, VERBOSE) + "\t");
        }

        output += je.penetrationRatio;

        //clear memory
        nodes = null;

        if (je.closeWhenDone) {
            je.visualizer = null;
            Visualizer.getActiveInstance().exit();
        }
    }

    /**
     * Prints memory consumption info.
     */
    private static String printMemStats() {
        String output = "";
        double avg;
        long sum = 0;

        for (int i = 0; i < memoryConsumption.size(); i++) {
            sum += ((Long) memoryConsumption.get(i)).longValue();
        }

        avg = (double) sum / memoryConsumption.size();

        output += (avg + "\t");

        double stdSum = 0.0;

        //std deviation
        for (int i = 0; i < memoryConsumption.size(); i++) {
            double diff = ((Long) memoryConsumption.get(i)).longValue() - avg;
            stdSum += (diff * diff);
        }

        output += ((Math.sqrt(stdSum / memoryConsumption.size())) + "\t");

        return output;
    }

    /**
    * Main entry point.
    *
    * @param args command-line arguments
    */
    public static void main(String[] args) {
        try {
            final JistExperiment je = (JistExperiment) (Util.readObject(args[0]));

            // constructs a new 2D field based on input
            je.setField();

            // store current free memory
            final long freeMemory = Runtime.getRuntime().freeMemory();

            long endTime = je.startTime + je.duration + je.resolutionTime;

            if (endTime > 0) {
                JistAPI.endAt(endTime * Constants.SECOND);
            }

            /** TODO change seed default value to named constant */
            // set random seed for simulator
            if (je.seed != 0) {
                Constants.random = new Random(je.seed);
            } else {
                Constants.random = new Random();
            }

            final Vector nodes = new Vector(je.nodes);

           // final RouteZrp.ZrpStats zrpStats = new RouteZrp.ZrpStats();

            final Date startTime = new Date();

            buildField(je, nodes);

   //         final String name = args[0];

            JistAPI.runAt(new Runnable() {
                    public void run() {
                        showStats(nodes, je, startTime, freeMemory);
                    }
                }, JistAPI.END);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
