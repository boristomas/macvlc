/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         AStarSearch.java
 * RCS:          $Id: AStarSearch.java,v 1.1 2007/04/09 18:49:19 drchoffnes Exp $
 * Description:  AStarSearch class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 8, 2004
 * Language:     Java
 * Package:      jist.swans.misc
 * Status:       Release
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
 * NOTE: This code was originally derived from http://www.informit.com/articles/article.asp?p=101142&seqNum=2&rl=1
 *
 */
package jist.swans.misc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jist.swans.field.StreetMobilityOD;

/**
 The AStarSearch class, along with the AStarNode class,
 implements a generic A* search algorithm. The AStarNode
 class should be subclassed to provide searching capability.
 */
public class AStarSearch {
	
	HashMap cachedPaths = null;
	/** debug switch */
	private static final boolean DEBUG = false;
	
	
	/**
	 A simple priority list, also called a priority queue.
	 Objects in the list are ordered by their priority,
	 determined by the object's Comparable interface.
	 The highest priority item is first in the list.
	 */
	public static class PriorityList extends LinkedList {
		
		private static final long serialVersionUID = -4751049056047615550L;
		
		public void add(Comparable object) {
			for (int i=0; i<size(); i++) {
				if (object.compareTo(get(i)) <= 0) {
					add(i, object);
					return;
				}
			}
			addLast(object);
		}
	}
	
	/**
	 * AStarSeach constructor 
	 * @param cachedPaths the HashMap to store cached paths. Null if caching is not implemented.
	 */
	public AStarSearch(HashMap cachedPaths) {
		
		this.cachedPaths = cachedPaths;
	}
	
	/**
	 * AStarSeach constructor 
	 * @param cachedPaths the HashMap to store cached paths. Null if caching is not implemented.
	 * @param smod the StreetMobility object, for debugging
	 */
	public AStarSearch(HashMap cachedPaths, final StreetMobilityOD smod) {
		
		this.cachedPaths = cachedPaths;
	}
	
	/**
	 Construct the path, not including the start node.
	 */
	protected LinkedList constructPath(AStarNode node) {
		LinkedList path = new LinkedList();
		while (node.pathParent != null) {
			path.addFirst(node);
			node = node.pathParent;
		}
		
		return path;
	}
	
	
	/**
	 Find the path from the start node to the end node. A list
	 of AStarNodes is returned, or null if the path is not
	 found. 
	 */
	public LinkedList findPath(AStarNode startNode, AStarNode goalNode) {
		
		PriorityList openList = new PriorityList();
		LinkedList closedList = new LinkedList();;
		
		startNode.costFromStart = 0;
		startNode.estimatedCostToGoal =
			startNode.getEstimatedCost(goalNode);
		startNode.pathParent = null;
		openList.add(startNode);

		while (!openList.isEmpty()) {
			AStarNode node = (AStarNode)openList.removeFirst();
			
			if (node.equals(goalNode)) {
				
				// construct the path from start to goal
				return constructPath(node);
			}
			
			List neighbors = node.getNeighbors();
			for (int i=0; i<neighbors.size(); i++) {
				AStarNode neighborNode =
					(AStarNode)neighbors.get(i);
				boolean isOpen = openList.contains(neighborNode);
				boolean isClosed =
					closedList.contains(neighborNode);
				float costFromStart = node.costFromStart +
				node.getCost(neighborNode);
				
				// check if the neighbor node has not been
				// traversed or if a shorter path to this
				// neighbor node is found.
				if ((!isOpen && !isClosed) ||
						costFromStart < neighborNode.costFromStart)
				{
					neighborNode.pathParent = node;
					neighborNode.costFromStart = costFromStart;
					neighborNode.estimatedCostToGoal =
						neighborNode.getEstimatedCost(goalNode);
					if (DEBUG)
					{
						System.out.println("Estimated cost to goal from : "+ neighborNode.getEstimatedCost(goalNode));
					}
					if (isClosed) {
						closedList.remove(neighborNode);
					}
					if (!isOpen) {
						openList.add(neighborNode);
					}
				}
			}
			closedList.add(node);
		}
		
		// no path found
		
		// this will seem strange. If the AStarSearch returns without a path, then 
		// there must be some kind of problem with road and I want to remove it 
		// from the grid. So I'm returning the closed list with a null first value.
		closedList.addFirst(null);
		return closedList;
	}
	
}