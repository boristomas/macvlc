//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <DrawPanel.java Tue 2004/04/06 11:50:45 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.gui;

import java.awt.*;
import java.lang.*;
import javax.swing.*;

import jist.runtime.JistAPI;

/**
 * rimtodo: in development
 *
 * @author Edwin Cheng
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: DrawPanel.java,v 1.1 2007/04/09 18:49:47 drchoffnes Exp $
 * @since SWANS1.0
 */
public class DrawPanel extends JPanel 
{
  private int width;
  private int height;
  private int windowWidth;
  private int windowHeight;
  private int xDiameter;
  private int yDiameter;
  private int xOffset;
  private int yOffset;

  private Color   pivotPoint;
  private Color   pointNormal;
  private Color   pointFocus;
  private Color   background;
  private Color   lineFocus;
  private Color   lineNormal;
  private Color   axis;

  private PointStruct.LPoints pivot;
  private PointStruct.LPoints pointHead;
  private PointStruct.LPoints focusPoint;
  private PointStruct.LLines  lineHead;
  private PointStruct.LLines  focusLine;

  /* Constructor */
  public DrawPanel(int width, int height) 
  {
    this.width  = width;
    this.height = height;

    this.windowWidth  = width;
    this.windowHeight = height;

    if(width < 500)
      this.windowWidth = 500;

    if(height < 500)
      this.windowHeight = 500;

    this.windowWidth  += 20;
    this.windowHeight += 20;
    setPreferredSize(new Dimension(this.windowWidth,this.windowHeight));

  } //endDrawPanel 

  /* Method calculating the offset for x and y axis */
  public void setOffset(int x, int y) 
  {
    if(x < this.windowWidth/2)
      this.xOffset = this.windowWidth/2;
    else
      this.xOffset = this.windowWidth-x-10;

    if(y < this.windowHeight/2)
      this.yOffset = this.windowHeight/2;
    else
      this.yOffset = this.windowHeight-y-10;  
  } //endsetOffset

  /* Method setting the diameter of the points */
  public void setPointDiameter(int x, int y) 
  {
    this.xDiameter = x;
    this.yDiameter = y;
  } //endsetPointDiameter

  /* Method setting the color of each component */
  public void setColorAttr(Color a, Color b, Color c, Color d, Color e, Color f, Color g) 
  {
    setBackground(a);

    this.pivotPoint  = b;
    this.pointFocus  = c;
    this.pointNormal = d;
    this.lineFocus   = e;
    this.lineNormal  = f;
    this.axis        = g;   
  } //endsetColorAtt

  /* Method setting the pivot(extreme) point */
  public void setPivotPoint(PointStruct.LPoints pivot) 
  {
    this.pivot = pivot;
  } //endsetPivotPoint

  /* Method setting the point in action */
  public void setFocusPoint(PointStruct.LPoints Head) 
  {
    this.focusPoint = Head;
  } //endsetfocusPoint

  /* Method setting the list of points */
  public void setPointList(PointStruct.LPoints Head) 
  {
    this.pointHead = Head;
  } //endsetPointList

  /* Method setting the line in action */
  public void setFocusLine(PointStruct.LLines Head) 
  {
    this.focusLine = Head;
  } //endsetfocusLine

  /* Method setting the list of lines */
  public void setLineList(PointStruct.LLines Head) 
  {
    this.lineHead = Head;
  } //endsetLineList

  /* Method generating the graphics */
  public void paintComponent(Graphics g) 
  {
    PointStruct.LPoints point = this.pointHead;
    PointStruct.LLines  line  = this.lineHead;

    g.setColor(getBackground());
    g.fillRect(0,0,this.windowWidth, this.windowHeight);

    //Draw normal points
    g.setColor(this.pointNormal);
    while(point != null) 
    {
      g.fillOval(this.xOffset-this.xDiameter/2+(int)point.x,
          this.yOffset-this.yDiameter/2-(int)point.y,
          this.xDiameter,
          this.yDiameter);
      point = point.next;
    }

    //Draw focus point
    if(focusPoint != null) 
    {
      g.setColor(this.pointFocus);
      g.fillOval(this.xOffset-this.xDiameter/2+(int)focusPoint.x,
          this.yOffset-this.yDiameter/2-(int)focusPoint.y,
          this.xDiameter,
          this.yDiameter);
    }

    //Draw pivot point
    if(this.pivot != null) 
    {
      g.setColor(this.pivotPoint);
      g.fillOval(this.xOffset-this.xDiameter/2+(int)this.pivot.x,
          this.yOffset-this.yDiameter/2-(int)this.pivot.y,
          this.xDiameter,
          this.yDiameter);
    }   

    //Draw normal lines
    line = this.lineHead;   
    g.setColor(this.lineNormal);
    while(line != null) {
      g.drawLine(this.xOffset+(int)line.pt1.x,
          this.yOffset-(int)line.pt1.y,
          this.xOffset+(int)line.pt2.x,
          this.yOffset-(int)line.pt2.y);
      line = line.next;
    }

    //Draw focus line
    if(this.focusLine != null) 
    {
      g.setColor(this.lineFocus);
      g.drawLine(this.xOffset+(int)this.focusLine.pt1.x,
          this.yOffset-(int)this.focusLine.pt1.y,
          this.xOffset+(int)this.focusLine.pt2.x,
          this.yOffset-(int)this.focusLine.pt2.y);
    }

    //Draw axis
    g.setColor(this.axis);    
    g.drawLine(this.xOffset, 0, this.xOffset, getSize().height);
    g.drawLine(0, this.yOffset, getSize().width, this.yOffset);

  } //endpaintComponent

  /* Method updating the graphics */
  public void update(Graphics g) 
  {
    repaint();
  } //endrepaint

} //endclass

