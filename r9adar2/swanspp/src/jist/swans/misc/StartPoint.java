/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StartPoint.java
 * RCS:          $Id: StartPoint.java,v 1.1 2007/04/09 18:49:20 drchoffnes Exp $
 * Description:  StartPoint class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Mar 12, 2005
 * Language:     Java
 * Package:      jist.swans.misc
 * Status:       Release
 *
 * (C) Copyright 2005, Northwestern University, all rights reserved.
 *
 */
package jist.swans.misc;

import jist.swans.misc.Location.Location2D;


public final class StartPoint
  {
      Location start;
      
    /**
     * @param start
     * @param finish
     */
    public StartPoint(Location start) {
        this.start = start;
        
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        // this will resolve paths on chaining
        StartPoint other = (StartPoint) obj;
        return (other.start.distance(start) ==0);
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        // this isn't great, but it should make the HashMap work decently
        return (new Float(start.getX()+start.getY())).hashCode();
    }
  }