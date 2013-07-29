/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         AStarNode.java
 * RCS:          $Id: AStarNode.java,v 1.1 2007/04/09 18:49:19 drchoffnes Exp $
 * Description:  AStarNode class (see below)
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
 * NOTE: This code was originally derived from http://www.informit.com/articles/article.asp?p=101142&seqNum=2&rl=1
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
 *
 */
package jist.swans.misc;

import java.util.List;

/**
  The AStarNode class, along with the AStarSearch class,
  implements a generic A* search algorithm. The AStarNode
  class should be subclassed to provide searching capability.
*/
public abstract class AStarNode implements Comparable {

  public AStarNode pathParent;
  public float costFromStart;
  public float estimatedCostToGoal;


  public float getCost() {
    return costFromStart + estimatedCostToGoal;
  }


  public int compareTo(Object other) {
    float thisValue = this.getCost();
    float otherValue = ((AStarNode)other).getCost();

    float v = thisValue - otherValue;
    return (v>0)?1:(v<0)?-1:0; // sign function
  }


  /**
    Gets the cost between this node and the specified
    adjacent (AKA "neighbor" or "child") node.
  */
  public abstract float getCost(AStarNode node);


  /**
    Gets the estimated cost between this node and the
    specified node. The estimated cost should never exceed
    the true cost. The better the estimate, the more
    effecient the search.
  */
  public abstract float getEstimatedCost(AStarNode node);


  /**
    Gets the children (AKA "neighbors" or "adjacent nodes")
    of this node.
  */
  public abstract List getNeighbors();
  public abstract List getNeighbors(Location origin, float distanceLimit); 
}  
