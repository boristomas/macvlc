/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         VisualizerInterface.java
 * RCS:          $Id: VisualizerInterface.java,v 1.1 2007/04/09 18:49:31 drchoffnes Exp $
 * Description:  VisualizerInterface interface (see below)
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
 */package driver;

import java.awt.Color;
import java.awt.Polygon;

import jist.swans.field.Field;
import jist.swans.field.streets.RoadSegment;
import jist.swans.misc.Location;

/**
 * Interface for visualization functionality.
 * @author David Choffnes
 *
 */
public interface VisualizerInterface {

	/** key types */
	public static final int CIRCLE = 1;

	static public final int CAR = 2;

	/**
	 * @return
	 */
	public abstract Field getField();

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#updateNodeLocation(float, float, int, jist.swans.misc.Location)
	 */
	public abstract void updateNodeLocation(float newX, float newY, int ip,
			Location brg);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#displaceNode(jist.swans.misc.Location, int, jist.swans.misc.Location)
	 */
	public abstract void displaceNode(Location step, int ip, Location brg);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setFocus(int)
	 */
	public abstract void setFocus(int ip);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#drawTransmitCircle(int)
	 */
	public abstract void drawTransmitCircle(int ip);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#drawAnimatedTransmitCircle(int, java.awt.Color)
	 */
	public abstract void drawAnimatedTransmitCircle(int ip, Color color);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#hideTransmitCircle(int)
	 */
	public abstract void hideTransmitCircle(int ip);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setNodeColor(int, java.awt.Color)
	 */
	public abstract void setNodeColor(int i, Color c);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setToolTip(int, java.lang.String)
	 */
	public abstract void setToolTip(int ip, String text);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setRoutingPaneText(int, java.lang.String)
	 */
	public abstract void setRoutingPaneText(int ip, String text);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setGeneralPaneText(java.lang.String)
	 */
	public abstract void setGeneralPaneText(String text);

	/**
	 * Sets the interference text for the panel.
	 * @param text
	 */
	public abstract void setInterferencePaneText(String text);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#resetColors()
	 */
	public abstract void resetColors();

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#drawCircle(int, jist.swans.misc.Location)
	 */
	public abstract void drawCircle(int r, Location loc);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#removeCircle()
	 */
	public abstract void removeCircle();

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#colorSegment(jist.swans.field.streets.RoadSegment, java.awt.Color)
	 */
	public abstract void colorSegment(RoadSegment rs, Color c);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#colorSegments(java.lang.Object[], java.awt.Color[])
	 */
	public abstract void colorSegments(Object[] objects, Color colors[]);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#colorSegments(java.lang.Object[], java.awt.Color)
	 */
	public abstract void colorSegments(Object[] objects, Color color);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#removeNode(int)
	 */
	public abstract void removeNode(int id);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#updateVisualizer()
	 */
	public abstract void updateVisualizer();

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setNodeImage(int, int)
	 */
	public abstract void setNodeImage(int ip, int imageIndex);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#addNode(float, float, int)
	 */
	public abstract void addNode(float initX, float initY, int ip);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#addNode(float, float, int, int)
	 */
	public abstract void addNode(float x, float y, int ip, int imageIndex);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#drawCircle(int, int)
	 */
	public abstract void drawCircle(int ip, int r);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setNumberOfNodes(int)
	 */
	public abstract void setNumberOfNodes(int totalNodes);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#showCommunication()
	 */
	public abstract boolean showCommunication();

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#resetColor(int)
	 */
	public abstract void resetColor(int nodenum);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#updateTime(long)
	 */
	public abstract void updateTime(long time);

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#registerKeyItem(java.awt.Color, int, java.lang.String)
	 */
	public abstract void registerKeyItem(Color c, int type, String text);

	/**
	 * Return true if the node is still on the map; false otherwise.
	 * @param ip the node identifier
	 * @return
	 */
	public abstract boolean hasNode(int ip);

	public abstract void addPersistentCirlce(Color c, int radius, int duration,
			Integer id);

	/**
	 * @param p
	 */
	public abstract void drawPolygon(Polygon p);

	public abstract void showTabs();

	public abstract void hideTabs();

	public abstract void pause();

	/**
	 * Closes the visualization window.
	 */
	public abstract void exit();

	/**
	 * Shows or hides the field. 
	 * @param show true if field should be shown
	 */
	public abstract void showField(boolean show);

	/** 
	 * Returns true if the simulation should step through communication.
	 * @return
	 */
	public abstract boolean isStep();

	/**
	 * Returns true if the simulation is paused.
	 * @return
	 */
	public abstract boolean isPaused();

	/**
	 * Returns true if text display is enabled.
	 * @return
	 */
	public abstract boolean showText();

}