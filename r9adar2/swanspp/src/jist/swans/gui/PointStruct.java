//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <PointStruct.java Tue 2004/04/06 11:50:48 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.gui;


/**
 * rimtodo: in development
 *
 * @author Edwin Cheng
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: PointStruct.java,v 1.1 2007/04/09 18:49:47 drchoffnes Exp $
 * @since SWANS1.0
 */
public class PointStruct
{

  /* Structure for point array */
  public static class Point {
    double x;
    double y;	
  };

  /* Structure for point pointer */
  public static class LPoints {
    double x;
    double y;
    double angle;
    double distance;
    LPoints prev,next;
  };

  /* Structure for line pointer */
  public static class LLines {
    LPoints pt1;
    LPoints pt2; 
    LLines next;
  };
}
