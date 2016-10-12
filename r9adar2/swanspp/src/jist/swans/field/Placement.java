//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Placement.java Tue 2004/04/06 11:31:11 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.field;

import java.util.ArrayList;

import org.apache.commons.beanutils.ConvertUtils;

import driver.JistExperiment;
import jist.swans.Constants;
import jist.swans.misc.Location;

/** 
 * Interface of all initial placement models.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Placement.java,v 1.1 2007/04/09 18:49:27 drchoffnes Exp $
 * @since SWANS1.0
 */

public interface Placement
{

	/**
	 * Return location of next node.
	 *
	 * @return location of next node
	 */
	Location getNextLocation();


	//////////////////////////////////////////////////
	// random placement model
	//

	/**
	 * Random (uniform) placement.
	 */
	public static class Random implements Placement
	{
		/** placement boundaries. */
		private float x, y;

		/**
		 * Initialize random placement model.
		 *
		 * @param x x-axis upper limit
		 * @param y y-axis upper limit
		 */
		public Random(float x, float y)
		{
			init(x, y);
		}

		/**
		 * Initialize random placement.
		 *
		 * @param loc upper limit coordinate
		 */
		public Random(Location loc)
		{
			init(loc.getX(), loc.getY());
		}

		/**
		 * Initialize random placement.
		 *
		 * @param field field dimensions string
		 */
		public Random(String field)
		{
			String[] data = field.split("x|,");
			if(data.length!=2) throw new IllegalArgumentException("invalid format, expected i,j");
			init(Float.parseFloat(data[0]), Float.parseFloat(data[1]));
		}

		/**
		 * Initialize random placement.
		 *
		 * @param x field x-dimension (in meters)
		 * @param y field y-dimension (in meters)
		 */
		private void init(float x, float y)
		{
			this.x = x;
			this.y = y;
		}

		//////////////////////////////////////////////////
		// Placement interface
		//

		/** {@inheritDoc} */
		public Location getNextLocation()
		{
			return new Location.Location2D(
					Constants.random.nextFloat()*x,
					Constants.random.nextFloat()*y);
		}

	} // class: Random


	//////////////////////////////////////////////////
	// grid placement model
	//

	/**
	 * Placement along a regular grid.
	 */
	public static class Grid implements Placement
	{
		/** field dimensions. */
		private float fieldx, fieldy;
		/** node placement array dimensions. */
		private int nodex, nodey;
		/** number of nodes already placed. */
		private long i;

		/**
		 * Initialize grid placement model.
		 *
		 * @param loc field dimensions (in meters)
		 * @param nodex number of nodes in x-dimension
		 * @param nodey number of nodes in y-dimension
		 */
		public Grid(Location loc, int nodex, int nodey)
		{
			init(loc.getX(), loc.getY(), nodex, nodey);
		}

		/**
		 * Initialize grid placement model.
		 *
		 * @param loc field dimensions (in meters)
		 * @param s node configuration string
		 */
		public Grid(Location loc, String s)
		{
			init(loc, s);
		}

		/**
		 * Initialize grid placement model.
		 *
		 * @param field field dimensions string
		 * @param nodes node configuration string
		 */
		public Grid(String field, String nodes)
		{
			init(field, nodes);
		}

		/**
		 * Initialize grid placement model.
		 *
		 * @param field field dimensions string
		 * @param nodes node configuration string
		 */
		private void init(String field, String nodes)
		{
			String[] data = field.split("x|,");
			if(data.length!=2) throw new IllegalArgumentException("invalid format, expected i,j");
			init(new Location.Location2D(Float.parseFloat(data[0]), Float.parseFloat(data[1])), nodes);
		}

		/**
		 * Initialize grid placement model.
		 *
		 * @param loc field dimensions (in meters)
		 * @param s node configuration string
		 */
		private void init(Location loc, String s)
		{
			Location.Location2D l;
			s= s.trim();
			String[] data = s.split("x|;");
			if(data.length != JistExperiment.getJistExperiment().nodes)
			{
				throw new IllegalArgumentException("number of locations is not equal to number of nodes."); 
			}
			String[] splitdata;
			for (String item : data) 
			{
				splitdata = item.split("x|,");
				l=new Location.Location2D(Float.parseFloat(splitdata[0]), Float.parseFloat(splitdata[1]));
				l.StaticBearing = Float.parseFloat(splitdata[2]);
				locations.add(l);

			}
			locationIndex = 0;
		}
		private int locationIndex = 0;
		private ArrayList<Location.Location2D> locations = new ArrayList<Location.Location2D>();
		/**
		 * Initialize grid placement model.
		 *
		 * @param fieldx field x-dimension (in meters)
		 * @param fieldy field y-dimension (in meters)
		 * @param nodex number of nodes in x-dimension
		 * @param nodey number of nodes in y-dimension
		 */
		private void init(float fieldx, float fieldy, int nodex, int nodey)
		{
			this.fieldx = fieldx;
			this.fieldy = fieldy;
			this.nodex = nodex;
			this.nodey = nodey;
			i = 0;
		}

		private int offsetX = 0;//offset from the edge, for simple layout(static)
		private int offsetY = 0;
		//////////////////////////////////////////////////
		// Placement interface
		//

		/** {@inheritDoc} */
		public Location getNextLocation()
		{
			Location l;
			if(locations.isEmpty())
			{
				if(i/nodex==nodey) throw new IllegalStateException("grid points exhausted");
				l = new Location.Location2D(offsetX+ (i%nodex)*fieldx/nodex, offsetY+ (i/nodex)*fieldy/nodey);
				i++;

			}
			else
			{
				//static is set as placement opts.
				l= locations.get(locationIndex);
				locationIndex++;
			}
			return l;
		}

	} // class: Grid

} // interface: Placement

