//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <SwansGui.java Tue 2004/04/06 11:50:51 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.gui;

import javax.swing.JFrame;

import jist.runtime.JistAPI;

/**
 * rimtodo: in development
 *
 * @author Edwin Cheng
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: SwansGui.java,v 1.1 2007/04/09 18:49:47 drchoffnes Exp $
 * @since SWANS1.0
 */
public class SwansGui implements JistAPI.Logger
{
  public SwansGui()
  {
    System.out.println("hello");
    JFrame frame = new JFrame("SWANS GUI");
    frame.pack();
    frame.setVisible(true);
  }

  public void log(Object s)
  {
    System.out.println("jistlog: "+s);
  }
}
