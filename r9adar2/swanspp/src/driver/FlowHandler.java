/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         FlowHandler.java
 * RCS:          $Id: FlowHandler.java,v 1.1 2007/04/09 18:49:31 drchoffnes Exp $
 * Description:  FlowHandler class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Jul 25, 2005 at 2:44:47 PM
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

import java.util.Vector;

import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.Placement;
import jist.swans.field.StreetPlacementFlow;
import jist.swans.misc.Mapper;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioInfo.RadioInfoShared;
import jist.swans.route.RouteAodv.AodvStats;
import jist.swans.route.RouteZrp.ZrpStats;

/**
 * The flow handler deals with flows of vehicles from one area on a map 
 * to another.
 * 
 * @author David Choffnes
 *
 */
class FlowHandler implements Runnable {

    JistExperiment je;
    int i; 
    Vector nodes; 
    AodvStats stats; 
    Field field; 
    Placement place; 
    RadioInfoShared radioInfo; 
    Mapper protMap; 
    PacketLoss inLoss; 
    PacketLoss outLoss; 
    Mobility mobility; 
    ZrpStats zrpStats; 
    VisualizerInterface v;
    private int flowIndex;

 
   /**
    * 
    * @param je
    * @param nodeId
    * @param nodes
    * @param field
    * @param place
    * @param radioInfo
    * @param protMap
    * @param inLoss
    * @param outLoss
    * @param mobility
    * @param vis
    * @param flowIndex
    */
    public FlowHandler(JistExperiment je, int nodeId, Vector nodes, 
            Field field, Placement place, RadioInfo.RadioInfoShared radioInfo, Mapper protMap,
            PacketLoss inLoss, PacketLoss outLoss, 
            Mobility mobility,  VisualizerInterface vis, int flowIndex) {
        // T-ODO Auto-generated constructor stub
   
        this.field = field;
      
        this.i = nodeId;
        this.inLoss = inLoss;
   
        this.je = je;
   
        this.mobility = mobility;
        this.nodes = nodes;
        this.outLoss = outLoss;
        this.place = place;
        this.protMap = protMap;
        this.radioInfo = radioInfo;
        this.flowIndex = flowIndex;
  
    
    }

    public void run() {
        StreetPlacementFlow spf = (StreetPlacementFlow)place;
        spf.setNextIndex(flowIndex);
        GenericDriver.addNode(je, i++, nodes, field, place, radioInfo, protMap, inLoss, 
                outLoss, mobility, Visualizer.getActiveInstance());
//        v.setNodeImage(flowIndex,1);
        
    }
    
}