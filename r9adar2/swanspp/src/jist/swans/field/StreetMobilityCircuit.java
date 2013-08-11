/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetMobilityCircuit.java
 * RCS:          $Id: StreetMobilityCircuit.java,v 1.1 2007/04/09 18:49:28 drchoffnes Exp $
 * Description:  StreetMobilityCircuit class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Sep 8, 2005 at 8:49:17 AM
 * Language:     Java
 * Package:      jist.swans.field
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
package jist.swans.field;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;

import jist.swans.field.StreetMobilityOD.StreetMobilityInfoOD;
import jist.swans.misc.Location;
import jist.swans.misc.Location.Location2D;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The StreetMobilityCircuit class supports mobility around a fixed, closed 
 * path.
 */
public class StreetMobilityCircuit extends StreetMobility {

    /** the file with the routes */
    public String file;
    /** the list of routes */
    public LinkedList[] routes = null;
    
    /**
     * @param segmentFile
     * @param streetFile
     * @param shapeFile
     * @param degree
     * @param bl
     * @param tr
     */
    public StreetMobilityCircuit(String segmentFile, String streetFile,
            String shapeFile, int degree, Location2D bl, Location2D tr) {
        super(segmentFile, streetFile, shapeFile, degree, bl, tr);
        
    }
    
    /**
     * @param segmentFile
     * @param streetFile
     * @param shapeFile
     * @param degree
     * @param bl
     * @param tr
     */
    public StreetMobilityCircuit(String segmentFile, String streetFile,
            String shapeFile, int degree, Location2D bl, Location2D tr, 
            String routeFile) {
        super(segmentFile, streetFile, shapeFile, degree, bl, tr);
        file = routeFile;
        loadRoutes();
    }

    /**
     * I probably want to do this using a set of intersection points.
     *
     */
    private void loadRoutes() {
        int numRoutes = 0;
        
        // determine number of routes
        
        routes = new LinkedList[numRoutes];
        
        Vector starts = new Vector();
        Vector ends = new Vector();
        Vector rates = new Vector();
        String line;
        String parsed[];
        
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));

            if (!in.ready())
                throw new IOException();

            while ((line = in.readLine()) != null)
            {
                parsed = line.split("[\\(\\),:]");
//                System.out.println(Arrays.toString(parsed));
                rates.add(Integer.valueOf(parsed[0]));
                starts.add(new Location.Location2D(Float.valueOf(parsed[2]).floatValue()*(float)StreetMobility.METERS_PER_DEGREE, 
                        Float.valueOf(parsed[3]).floatValue()*(float)StreetMobility.METERS_PER_DEGREE));
                ends.add(new Location.Location2D(Float.valueOf(parsed[6]).floatValue()*(float)StreetMobility.METERS_PER_DEGREE, 
                        Float.valueOf(parsed[7]).floatValue()*(float)StreetMobility.METERS_PER_DEGREE));
            }            
            in.close();
//            startLocs = new Location.Location2D[starts.size()];
//            starts.toArray(je.startLocs);
//            endLocs = new Location.Location2D[ends.size()];
//            ends.toArray(je.endLocs);
//            flowRates = new Integer[rates.size()];
//            rates.toArray(je.flowRates);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    /* (non-Javadoc)
     * @see jist.swans.field.StreetMobility#init(jist.swans.field.FieldInterface, java.lang.Integer, jist.swans.misc.Location)
     */
    public MobilityInfo init(FieldInterface f, Integer id, Location loc) {
        // path will be set by placement class
        
        StreetMobilityInfoOD smodi = (StreetMobilityInfoOD)mobInfo.lastElement();
        smodi.v = v; // set visualizer object
        return smodi;
    }

    /* (non-Javadoc)
     * @see jist.swans.field.StreetMobility#setNextRoad(jist.swans.field.StreetMobilityInfo)
     */
    public void setNextRoad(StreetMobilityInfo smi) {
        // TODO Auto-generated method stub

    }

}
