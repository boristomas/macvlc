/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         Visualizer.java
 * RCS:          $Id: Visualizer.java,v 1.1 2007/04/09 18:49:31 drchoffnes Exp $
 * Description:  Visualizer class (see below)
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
 */
package driver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.field.Mobility.MobilityInfo;
import jist.swans.field.StreetMobility;
import jist.swans.field.streets.CongestionMonitor;
import jist.swans.field.streets.RoadSegment;
import jist.swans.field.streets.Shape;
import jist.swans.gui.JMultiLineToolTip;
import jist.swans.gui.ListDialog;
import jist.swans.misc.Location;

/**
 * <p>Title: Visualizer</p>
 * <p>Description: Creates a GUI that displays node mobility.</p>
 * @author David Choffnes
 * @version 0.1
 */
public class Visualizer implements VisualizerInterface
{

	private static Visualizer activeInstance;
	/** gui configuration constants */

	/** true if showing routing text */
	final static boolean UPDATE_ROUTING_TEXT = true;
	/** offset from bottom of screen */
	private static final int BOTTOM_OFFSET = 100;
	/** buffer below field */
	private static final int BOTTOM_BUFFER = 50;
	private static final int TOP_PANEL_HEIGHT = 40;

	/** other display constants */
	private static final boolean SHOW_TITLE = 
			JistExperiment.getJistExperiment().isShowTitle();

	static public final boolean SHOW_INTERFERENCE = 
			JistExperiment.getJistExperiment().isShowInterference(); // T-ODO make a checkbox

	/** key types */
	public static final int CIRCLE = 1;
	static public final int CAR = 2;

	/** The JFrame for displaying the GUI. */
	public static JFrame frame;

	/** The JPanel for displaying the GUI. */
	private JPanel panel;

	private JButton pauseButton;

	/** the button for showing congestion colors */
	private ZoomToggle zoomToggle;

	private JScrollPane fieldPane;

	/** the width of the field */
	private int fieldX;
	/** the height of the field */
	private int fieldY;

	/** zoom level */
	private static int zoom = 1;

	/** title pane dimensions */
	private final static int titlePaneHeight = SHOW_TITLE ? 100: 0;
	/** dimensions for the information panes */
	private final static int infoPaneHeight = 100;
	private final static int infoPaneWidth = 400;
	private static int totalTopHeight = infoPaneHeight 
			+ titlePaneHeight+BOTTOM_BUFFER+TOP_PANEL_HEIGHT;

	/** Simulation configuration object */
	public static JistExperiment je;

	/** image for cars */
	/* static Image carImage[] = new Image[]{java.awt.Toolkit.getDefaultToolkit().getImage(
            "small-bug.jpg"), 
            java.awt.Toolkit.getDefaultToolkit().getImage(
            "small-bug.jpg")};*/
	static Image carImage[] = new Image[]{};


	/**
	 * Layered pane put into content pane.
	 */
	JLayeredPane layeredPane;
	/** time label */
	private TimeLabel timeLabel;
	/** Displays routing textual information */
	private JEditorPane routingEditorPane;
	/** Displays general textual information */
	private JEditorPane generalEditorPane;

	/** Displays interference textual information */
	private JEditorPane interferenceEditorPane;
	private JTabbedPane tabbedPane;
	/** the checkbox for displaying text */
	private JCheckBox showTextCheckBox;

	/** Radius for circle representing transmission radius */
	private double radioRadius;
	/** stores the nodes for this sim run */
	public HashMap nodes;

	/** this collects circuit segments */
	private ListDialog dialog = null;

	/** name to store circuit routes in */
	private String circuitFileName;

	/** array of LinkedLists of RoadSegments for circuit routes */
	private LinkedList circuitRoutes[];


	private JCheckBox showCommunication;

	private Field field;
	private JPanel contentPane;
	private JPanel keyPanel;
	private GridBagConstraints c2;

	/** vector of persistent circles */
	Vector pCircles = new Vector();

	/** true if the VFN should be active */
	public boolean vfnOn = true;

	/** true if tabs should be shown */
	public boolean showTabs = true;

	/** tru if field should be shown */
	private boolean showField = true;

	private Polygon currentPolygon;
	private JButton hideTabButton;
	/** button for hiding simulation field */
	private JButton hideFieldButton;
	/** set to true if the vis window should always be on top */
	private boolean alwaysOnTop = false;
	private AbstractButton stopAllNodesButton;

	private JButton startAllNodesButton;
	/** true if interference text should be shown */
	private boolean showInterferenceText = true;	

	/** check box for setting whether to step through communication */
	private JCheckBox stepCheckBox;	

	/** Text Box for identifying nodes  */
	private JTextField identifyNodeBox;


	/** true if showing primarily VFR stuff */
	public boolean showing_VFR = false;



	/**
	 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
	 *
	 * The JPanelCircle class ...
	 */
	public class JPanelCircle extends JPanel {

		Color c;
		int x;
		int y;
		public JPanelCircle (Color c, int x, int y){
			super();
			this.x = x;
			this.y = y;
			this.c = c;
		}
		public void paintComponent(Graphics g)
		{
			((Graphics2D)g).setStroke(new BasicStroke(3));
			g.setColor(c);
			g.drawOval(0,0, x, y);
		}
	}

	static class PopupListener extends MouseAdapter {
		JPopupMenu popup;

		PopupListener(JPopupMenu popupMenu) {
			popup = popupMenu;
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {

				popup.show(e.getComponent(),
						e.getX(), e.getY());
			}
		}
	}

	public class PersistentCircle {
		Color c; // color
		int radius; // radius
		int x; // locations
		int y;
		long expire; // duration 

		public PersistentCircle(Color c, long duration, int radius, int x, int y) {

			this.c = c;
			this.expire = duration;
			this.radius = radius;
			this.x = x;
			this.y = y;
		}

		public void paintComponent(Graphics g)
		{
			((Graphics2D)g).setStroke(new BasicStroke(3));
			g.setColor(c);
			//           g.drawOval((x-radius/2)/zoom,(y-radius/2)/zoom, radius/zoom, radius/zoom);
			g.drawOval((x-radius)/zoom,(y-radius)/zoom, (radius/zoom)*2, (radius/zoom)*2);
		}

	}

	public class RadioAnimator extends Thread {
		Node n;
		JPanel p;
		boolean stop = false;
		public Color color;

		/**
		 * @param n
		 * @param p
		 * @param color 
		 */
		public RadioAnimator(Node n, JPanel p, Color color) {
			this.n = n;
			this.p = p;
			this.color = color;
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */

		public void run() {
			while (n.animateRadiusStep!=Node.MAX_ANIMATION_STEPS && !stop){
				//            n.animateRadius(p.getGraphics());


				try {
					Thread.sleep(Node.INTERSTEP_TIME);
				} catch (InterruptedException e) {
					// T-ODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public void stopThread(){
			stop = true;
		}




	}

	public class TimeLabel extends JLabel{
		long time = 0;

		public TimeLabel(){
			super("0 seconds");
		}

		public void setTime(long time){    		
			if (this.time < time){
				this.time = time;
				this.setText("Time: "+time/Constants.SECOND+" seconds");
			}
		}




	}
	public class ZoomToggle extends JSpinner implements ChangeListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * 
		 */
		public ZoomToggle() {
			super();
			this.addChangeListener(this);
			// T-ODO Auto-generated constructor stub
		}

		/**
		 * @param model
		 */
		public ZoomToggle(SpinnerModel model) {
			super(model);
			// T-ODO Auto-generated constructor stub
			this.addChangeListener(this);
		}

		public void stateChanged(ChangeEvent e) {
			zoom = ((Integer)this.getValue()).intValue();
			updateVisualizer();

		}

	}

	/**
	 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
	 *
	 * The CongestionButton class ...
	 */
	public class CongestionButton extends JButton implements ActionListener {

		/**
		 * 
		 */
		CongestionMonitor cmi;
		/**
		 * 
		 */
		public CongestionButton() {
			this.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			Vector segments = je.sm.getSegments();
			Vector segmentsToColor = new Vector();
			Vector colors = new Vector();
			RoadSegment rs;    
			float ratio, r1, r2;
			for (int i =0; i < segments.size(); i++)
			{
				rs = (RoadSegment)segments.get(i);
				r1 = (rs.getLength()/rs.getSpeedLimit())/cmi.getTransitTime(i, CongestionMonitor.TO_END);
				r2 = (rs.getLength()/rs.getSpeedLimit())/cmi.getTransitTime(i, CongestionMonitor.TO_START);
				ratio = Math.min(r1, r2);

				if (ratio >= 1) continue;
				else if (ratio > 0.66) colors.add(Color.YELLOW);                     
				else if (ratio > 0.33) colors.add(Color.ORANGE);
				else colors.add(Color.RED);
				segmentsToColor.add(rs);
			}
			Color colorArray[] = new Color[colors.size()];
			for (int i = 0; i < colors.size(); i++) colorArray[i] = (Color)colors.get(i);
			colorSegments(segmentsToColor.toArray(), colorArray);

		}

		public void setCMI(CongestionMonitor cmi){
			this.cmi = cmi;
		}

	}

	/**
	 * The Node class represents the information for drawing
	 *  a node on a field. Using a JButton to allow simpler 
	 *  click functionalities.
	 */
	public static class Node extends JButton implements ActionListener {

		/** location on the field */
		public float x, y;
		/** node's identifier */
		public int ip;
		/** default color for this node */
		public Color defaultColor;
		/** displays circle around node if true */
		public boolean showRadius = false;
		/** circle radius for this node */
		public double radioRadius;
		/** button sizes */
		static int buttonHeight = 11;
		static int buttonWidth = 20;  
		static JistExperiment lje = JistExperiment.getJistExperiment();
		final static int MAX_ANIMATION_STEPS = 5;
		final static int INTERSTEP_TIME = 250; // in milliseconds
		Thread t=null;

		//		BufferedImage thumbImage = new BufferedImage(buttonWidth, 
		//                buttonHeight, BufferedImage.TYPE_INT_RGB);
		static Location defaultBrg = new Location.Location2D(1, 0);
		static Location origin = new Location.Location2D(0,0);
		Location currentBrg = defaultBrg;
		private boolean newBrg = true;
		AffineTransform currentXForm;
		private double currentAngle;
		private double xRot, yRot;
		private Dimension d = null;
		private boolean animateRadius;
		private long nextTime;
		private int animateRadiusStep;
		public Color radiusColor = Color.BLUE;
		public Color defaultRadiusColor = radiusColor;
		boolean onVFR = false;
		JMenuItem textItem;

		public Node(float x, float y, int ip)
		{
			super(String.valueOf(ip));
			boolean buttonOptions = false;
			if (JistExperiment.getJistExperiment().getMobility()<5){
				buttonWidth = 20;
				buttonHeight = 20;
				buttonOptions = true;
			}
			else {
				this.setText("");
				
				
		//		this.setIcon(ImageIO.read(this.getClass().getResource("images/small-tt.jpg")));
				this.setIcon(new ImageIcon(carImage[0]));
			}
			this.x = x;
			this.y = y;
			this.ip = ip;
			this.setToolTipText("Node Ip: "+ip);
			this.setPreferredSize(new Dimension(buttonWidth,buttonHeight));
			this.setSize(new Dimension(buttonWidth,buttonHeight));

			this.setMargin(new Insets(0,0,0,0));

			//            Graphics2D graphics2D = thumbImage.createGraphics();
			//            graphics2D.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
			//                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			//            graphics2D.drawImage(carImage, 0, 0, buttonWidth, buttonHeight, null);


			this.setHorizontalAlignment(CENTER);
			this.setVerticalAlignment(CENTER);
			this.setIconTextGap(0);
			defaultColor = getBackground();
			this.addActionListener(this);
			this.setBorderPainted(buttonOptions);
			this.setContentAreaFilled(buttonOptions);


			// handle menu stuff
			JCheckBoxMenuItem menuItem,menuItem2;

			//Create the popup menu.
			JPopupMenu popup = new JPopupMenu();
			//            textItem = new JMenuItem("Node information");
			//            popup.add(textItem);
			popup.addSeparator();
			menuItem = new JCheckBoxMenuItem("Stop this node");
			menuItem2 = new JCheckBoxMenuItem("Turn Off Radio");

			menuItem.setAction(new PopUpAction("Stop this node", null, 
					"Stop/start", menuItem, ip));
			popup.add(menuItem);

			menuItem2.setAction(new PopUpAction("Turn Off Radio", null, 
					"ON/OFF", menuItem2, ip));
			popup.add(menuItem2);

			//            menuItem = new JMenuItem("Another popup menu item");
			//            menuItem.addActionListener(this);
			//            popup.add(menuItem);

			//Add listener to the node so the popup menu can come up.
			MouseListener popupListener = new PopupListener(popup);
			this.addMouseListener(popupListener);

		}

		public JToolTip createToolTip()
		{
			return new JMultiLineToolTip();
		}

		public void adjustBearing(Location brg)
		{
			float dist = brg.distance(currentBrg);
			if (dist>0.1){
				currentBrg = brg;
				newBrg  = true;
			}
		}

		public void actionPerformed(ActionEvent evt) {
			// This method is called to respond when the user
			// presses the button.  It sets the node to inspect.
			if (lje.sm !=null)
			{
				if (lje.sm.getCarToInspect()<0)
					lje.sm.setCarToInspect(ip);
				else{
					int i = lje.sm.getCarToInspect();
					lje.sm.unsetCarToInspect();
					if (ip!=i) lje.sm.setCarToInspect(ip);
				}
			}

		} // end actionPerformed()

		public void updateLocation(float newX, float newY, 
				JCheckBox showRed, LinkedList routes[], Field f){
			MobilityInfo mi = f.getRadioData(new Integer(ip)).getMobilityInfo();
			//            RoadSegment rs = getStreet(new Location.Location2D(x, y));

			this.x = newX;
			this.y = newY;
			setLocation((int)(x/zoom-buttonWidth/2), (int)(y/zoom-buttonHeight/2));
			if (showRed!=null && showRed.isSelected()){

				if (onVFR) setColor(Color.RED);
				else resetColor();
			}
		}


		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (!(obj instanceof Node)) return false;
			Node n = (Node)obj;
			if (n.ip == ip) return true;
			else return false;
		}


		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		public void paintComponent(Graphics g)
		{
			if (this.getIcon()==null) {
				super.paintComponent(g);
				return;
			}

			Graphics2D g2d = (Graphics2D)g;
			AffineTransform origXform = g2d.getTransform();

		//	newBrg =false;
			if (newBrg){

				if (lje.sm !=null)
				{
					if (lje.sm.getCarToInspect()==ip &&currentBrg.distance(defaultBrg)==0)
					{
						System.out.println("Bearing: "+currentBrg+"\tDistance: "+
								currentBrg.distance(defaultBrg));
					}
				}
				if (currentBrg.distance(defaultBrg)<0.01){
					d = new Dimension(buttonWidth, buttonHeight);
					setSize(d);
				}
				else{
					
					tu je gadan problem
					//center of rotation is center of the button
					d = new Dimension((int)Math.ceil(Math.abs(buttonWidth*currentBrg.getX())+Math.abs(buttonHeight*currentBrg.getY())+1),
							(int)Math.ceil(Math.abs(buttonHeight*currentBrg.getX())+Math.abs(buttonWidth*currentBrg.getY()))+1);
				}

				if (d.height != 0 && d.width != 0){
					xRot = d.width/2.0;
					yRot = d.height/2.0;
					currentAngle = Math.acos(currentBrg.getX());
					if (currentBrg.getY()<0)
					{
						currentAngle = 2*Math.PI - currentAngle;
					}

				}
				newBrg = false;
				//                System.out.println(d + "\t"+currentAngle + "\t"+currentBrg + "\t"+
				//                        Math.sqrt(currentBrg.getX()*currentBrg.getX()+currentBrg.getY()*currentBrg.getY()));
			}

			if (currentAngle!=0){
				g2d.rotate(currentAngle, xRot, yRot);
				this.setSize(d);
			}
			//draw image centered in panel
			//            int x = (getWidth() - carImage.getWidth(this))/2;
			//            int y = (getHeight() - carImage.getHeight(this))/2;
			//            g2d.drawImage(carImage, x, y, this);
			super.paintComponent(g);

			g2d.setTransform(origXform);

			//            if (showRadius)
			//            {

			//            }

			//this.setIcon(new ImageIcon(this.getClass().getResource("images/small-bug.jpg")));
		} 

		/**
		 * Recolors this node with the default color.
		 *
		 */
		public void resetColor()
		{
			setBackground(defaultColor);
			this.setBorderPainted(false);
		}

		public void animateRadius() {
			showRadius = true;
			animateRadius = true;
			animateRadiusStep = 0;
			nextTime = System.currentTimeMillis() + INTERSTEP_TIME;

		}

		public void animateRadius(Graphics g) {
			//            Graphics g = p.getGraphics();
			int denom = zoom*2;
			if (animateRadius){
				if (animateRadiusStep==MAX_ANIMATION_STEPS){
					showRadius = false;
					animateRadius = false;   
					denom = 10000;
				}
				else{
					if (System.currentTimeMillis()>nextTime){
						animateRadiusStep++;
						nextTime += INTERSTEP_TIME;
					}
					denom *= (MAX_ANIMATION_STEPS-animateRadiusStep);                        
				}

			}

			int radius = (int)(radioRadius/denom);
			Graphics2D g2d = (Graphics2D)g;
			g2d.setStroke(new BasicStroke(3));              
			g2d.setColor(radiusColor);
			g2d.drawOval((int)(x/zoom-radius), (int)(y/zoom-radius), 
					(int)(radius*2), (int)(radius*2));
			g2d.setColor(Color.BLACK);

		}

		public void setColor(Color c){
			this.setBorderPainted(true);
			this.setBorder(new LineBorder(c, 3));            
		}

		public void addToMenu(String text) {
			//textItem.setText(text);

		}
	}

	static class PopUpAction extends AbstractAction {
		JCheckBoxMenuItem mi = null;

		/** id for the corresponding node */
		int id;
		String description = null;

		public PopUpAction(String text, ImageIcon icon,
				String desc, JCheckBoxMenuItem mi, 
				int id) {
			super(text, icon);
			putValue(SHORT_DESCRIPTION, desc);
			this.mi = mi;          
			this.id = id;
			this.description = text;
			//            putValue(MNEMONIC_KEY, mnemonic);
		}

		public void actionPerformed(ActionEvent e) {
			Field f = Visualizer.getActiveInstance().getField();
			MobilityInfo mobInfo = f.getRadioData(new Integer(id)).getMobilityInfo();

			System.out.println("String = "+this.getValue(SHORT_DESCRIPTION))  ;    

			if(this.getValue(SHORT_DESCRIPTION) == "Stop/start"  )
			{
				if (mi.isSelected()) mobInfo.setStopped(true); // stop motion
				else mobInfo.setStopped(false); // reenable motion
			}
			//            if(this.getValue(SHORT_DESCRIPTION) == "ON/OFF"  )
			//            {
			//            	if (mi.isSelected()) f.getProxy().toggleRadio(new Integer(id),true); // remove radio
			//            	else f.getProxy().toggleRadio(new Integer(id),false); // reenable radio
			//            }
		}
	}

	public class FieldPanel extends JPanel implements MouseListener {
		/** the nodes on the field */
		public HashMap nodes;
		/** if true, draws a circle */
		public boolean drawCircle;
		/** radius of the circle */
		public int circleRadius;
		/** Location where to draw circle */
		public Location circleLoc;
		/** current simulation time */
		public long time;
		/** Displays the streets */
		private BufferedImage streetMap = null;
		/** Amount by which to adjust x coordinate of map points */
		public int mapXDisp = 0;
		public int[] segsToColor=new int[]{-1};
		public Color[] colors=null;

		/**
		 * FieldPanel constructor.
		 * @param nodes the vector containing all of the JButtons
		 */
		public FieldPanel(HashMap nodes)
		{
			super();
			this.nodes = nodes;
			this.setLayout(null);
			this.addMouseListener(this);
			this.setBackground(Color.WHITE);
			//this.setForeground(Color.WHITE);
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		public void paintComponent(Graphics g)
		{
			if (nodes!=null){
				for (int i = 0; i < nodes.size(); i++){
					Node n = (Node)nodes.get(new Integer(i));                    
					if (n!=null && n.animateRadius){
						n.animateRadius(g);
					}

				}
			}
			g.setColor(getBackground());
			g.fillRect(0,0,(int)(Node.buttonWidth*1.4), 
					(int)(Node.buttonHeight*2.2));
			super.paintComponent(g);

			// display time
			//            g.setColor(Color.BLACK);
			//			g.setFont(new Font("Arial", Font.BOLD, 12));
			//			g.drawString(time/Constants.SECOND +" seconds", 25, 25);
			//            timeLabel.setText("Time: " +time/Constants.SECOND +" s");
			//            generalEditorPane.setText("Current time: " + time/Constants.SECOND +" seconds");

			// draw all of the nodes on the map
			if (nodes!=null){

				if (currentPolygon!=null){
					g.setColor(Color.YELLOW);
					((Graphics2D)g).setStroke(new BasicStroke(3));
					g.fillPolygon(currentPolygon);
				}

				if (SHOW_INTERFERENCE){
					Iterator it = pCircles.iterator();
					synchronized(pCircles){

						while (it.hasNext()){
							PersistentCircle pc = (PersistentCircle)it.next();
							if (pc.expire > JistAPI.getTime()){
								pc.paintComponent(g);
							}
							else it.remove();
						}
					}

				}
				// draw a circle, if any
				if (drawCircle)
				{
					g.setColor(Color.CYAN);
					g.fillOval((int)circleLoc.getX()/zoom-circleRadius/2, 
							(int)circleLoc.getY()/zoom-circleRadius/2, 
							circleRadius, circleRadius);
					g.setColor(Color.BLACK);

				}
				// draw the street map
				if (je.sm != null){
					Graphics2D g2 = (Graphics2D)g;
					if (streetMap == null)
					{
						drawStreetMap();
					}
					g2.drawImage(streetMap, null, 0, 0);
				}
			} // end case nodes						
		} 

		/**
		 * Draws roads without coloring segments.
		 *
		 */
		private void drawStreetMap()
		{
			uncolorSegments();
			drawStreetMap(new int[]{-1}, null);

		}

		/**
		 * Draws roads, optionally coloring some of them
		 * @param segmentsToColor the segments to color differently
		 * @param colors the colors to use
		 */
		private void drawStreetMap(int segmentsToColor[], Color colors[])
		{
			/* get the segments */
			StreetMobility sm = je.sm;
			Vector segments = sm.getSegments();
			Iterator it = segments.iterator();
			/* get the bounds of the map */
			Location start, end;
			Location topLeft = sm.getBounds()[0];
			Location bottomRight = sm.getBounds()[3];
			int x = (int)Math.abs(topLeft.getX()-bottomRight.getX());
			int y = (int)Math.abs(topLeft.getY()-bottomRight.getY());

			/* create the map image */
			streetMap = new BufferedImage(x/zoom, y/zoom, 
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2D = streetMap.createGraphics();
			g2D.setColor(Color.black);

			/* paint the map */
			while (it.hasNext())
			{
				RoadSegment rs = (RoadSegment)it.next();
				start = rs.getStartPoint();
				end = rs.getEndPoint();

				g2D.setStroke(new BasicStroke(rs.getStrokeWidth()));

				if(rs.getShapeIndex()==-1){
					g2D.drawLine((int)start.getX()/zoom-mapXDisp, (int)start.getY()/zoom, 
							(int)end.getX()/zoom-mapXDisp, (int)end.getY()/zoom);}
				else{
					HashMap shapes= sm.getShapes();
					Shape s = (Shape)shapes.get(new Integer(rs.getShapeIndex()));
					start = rs.getStartPoint();
					for (int i = 0; i < s.points.length; i++)
					{
						end = s.points[i];
						g2D.drawLine((int)start.getX()/zoom, (int)start.getY()/zoom, 
								(int)end.getX()/zoom, (int)end.getY()/zoom);
						start = end;
					}
					end = rs.getEndPoint();
					g2D.drawLine((int)start.getX()/zoom, (int)start.getY()/zoom, 
							(int)end.getX()/zoom, (int)end.getY()/zoom);
				}
			}
			colorMapSegments(segmentsToColor, colors, g2D);
		}

		private void colorMapSegments(int[] segmentsToColor, Color[] colors, 
				Graphics2D g2D) {

			segsToColor = segmentsToColor;
			this.colors = colors;
			StreetMobility sm = je.sm;
			Vector segments = sm.getSegments();
			Location start;
			Location end;
			// color the segments 
			for (int i = 0; i < segmentsToColor.length; i++)
			{

				if (segmentsToColor[i] >= 0)
				{
					g2D.setColor(colors[i]);	
					RoadSegment rs = (RoadSegment)segments.get(segmentsToColor[i]);
					g2D.setStroke(new BasicStroke(rs.getStrokeWidth()+6));

					start = rs.getStartPoint();
					end = rs.getEndPoint();

					if(rs.getShapeIndex()==-1){
						g2D.drawLine((int)start.getX()/zoom-mapXDisp, (int)start.getY()/zoom, 
								(int)end.getX()/zoom-mapXDisp, (int)end.getY()/zoom);}
					else{
						HashMap shapes= sm.getShapes();
						Shape s = (Shape)shapes.get(new Integer(rs.getShapeIndex()));
						start = rs.getStartPoint();
						for (int j = 0; j < s.points.length; j++)
						{
							end = s.points[j];
							g2D.drawLine((int)start.getX()/zoom, (int)start.getY()/zoom, 
									(int)end.getX()/zoom, (int)end.getY()/zoom);
							start = end;
						}
						end = rs.getEndPoint();
						g2D.drawLine((int)start.getX()/zoom, (int)start.getY()/zoom, 
								(int)end.getX()/zoom, (int)end.getY()/zoom);
					}
				}
			}
		}

		/**
		 * Set the color of a single road segment.
		 * @param rs the RoadSegment to color.
		 * @param c the color to use
		 */
		public void setSegmentColor(RoadSegment rs, Color c)
		{
			//			drawStreetMap(new int[]{rs.getSelfIndex()}, new Color[]{c});
			Graphics2D g2d = streetMap.createGraphics();
			uncolorSegments();
			colorMapSegments(new int[]{rs.getSelfIndex()}, new Color[]{c},g2d);
		}


		/**
		 * Sets the color of several segments.
		 * 
		 * @param rss RoadSegments to color.
		 * @param colors the colors to use
		 */
		public void setSegmentsColor(RoadSegment[] rss, Color colors[]) {
			int[] ids = new int[rss.length];

			for (int i =0; i < rss.length; i++)
			{
				ids[i] = rss[i].getSelfIndex();
			}

			uncolorSegments();
			while (streetMap==null)
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					//                    // T-ODO Auto-generated catch block
					//                    e.printStackTrace();
				}
			colorMapSegments(ids, colors,streetMap.createGraphics());

		}

		private void uncolorSegments() {
			if (colors!=null) {
				Color black[] = new Color[colors.length];

				if(!(showing_VFR)) /* showing_VFR is false by default */
				{
					for (int i =0; i < colors.length;i++)black[i] = Color.BLACK;
					colorMapSegments(segsToColor, black, streetMap.createGraphics());
				}

			}

		}

		public void mouseClicked(MouseEvent e) {
			if (je.sm==null) return;
			Location.Location2D clickLoc = 
					new Location.Location2D(e.getPoint().x*zoom, e.getPoint().y*zoom);
			RoadSegment rs = je.sm.getStreet(clickLoc);
			if (rs!=null){
				StringBuffer sb = new StringBuffer();
				sb.append("Road clicked: "+rs.printStreetName(je.sm.getStreets()) + "\n");

				setGeneralPaneText(sb.toString());
				setSegmentColor(rs, Color.GREEN);
				if (dialog!=null && dialog.isVisible()){
					if (dialog.contains(rs)){
						dialog.removeStreet(rs);
					}
					else{
						RoadSegment last = dialog.getLastStreet();
						if (last== null || last.getStartPoint().distance(rs.getStartPoint())==0
								|| last.getStartPoint().distance(rs.getEndPoint())==0
								|| last.getEndPoint().distance(rs.getStartPoint())==0
								|| last.getEndPoint().distance(rs.getEndPoint())==0){
							dialog.addStreet(rs);
						}
					}

				}
			}
			e.consume();
		}

		public void mousePressed(MouseEvent e) {
			// T-ODO Auto-generated method stub

		}

		public void mouseReleased(MouseEvent e) {
			// T-ODO Auto-generated method stub

		}

		public void mouseEntered(MouseEvent e) {
			// T-ODO Auto-generated method stub

		}

		public void mouseExited(MouseEvent e) {
			// TO-DO Auto-generated method stub

		}
	}	

	/**
	 * Visualizer creates and shows the GUI when an outside synchronized 
	 * thread instantiates an instance of it.
	 *
	 * @param je the simulation configuration object
	 */
	public Visualizer()
	{
		this.je = JistExperiment.getJistExperiment();
		carImage = new Image[]{new ImageIcon(this.getClass().getResource("images/small-bug.jpg")).getImage(), 
				new ImageIcon(this.getClass().getResource("images/small-bug.jpg")).getImage()};

		activeInstance = this;
		if (je.mobility != Constants.MOBILITY_STRAW_OD
				&& je.mobility != Constants.MOBILITY_STRAW_SIMPLE){
			this.fieldX = je.fieldX;
			this.fieldY = je.fieldY;
		}
		else{
			Location loc[] = je.sm.getBounds();
			this.fieldX = (int)Math.abs(loc[3].getX()-loc[0].getX());
			//			this.fieldY = (int)Math.abs(loc[3].getY()-loc[0].getY()); 
			this.fieldY = (int)Math.abs(loc[0].getY()-loc[2].getY());
		}
		nodes = new HashMap();

		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				createAndShowGUI();
			}
		});

		synchronized(this)
		{
			while(frame==null)
			{
				try
				{
					wait();
				}
				catch(InterruptedException e)
				{
				}
			}
		}
		frame.invalidate();
	}

	/**
	 * @return
	 */
	public static boolean isStreetMobility() {
		if (je.mobility == Constants.MOBILITY_STRAW_SIMPLE || 
				je.mobility== Constants.MOBILITY_STRAW_OD) return true;
		else return false;
	}

	/**
	 * @return
	 */
	public Field getField() {
		return field;
	}

	/**
	 * Creates the field and shows it.
	 */
	private synchronized void createAndShowGUI()
	{        
		JFrame frame = new JFrame("Ceratias");
		Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		if (d.width < fieldX || d.height < fieldY)
		{
			zoom = Math.max((int)Math.ceil((double)fieldX/d.width), fieldY/(d.height-BOTTOM_OFFSET-totalTopHeight-20));
		}

		//fieldX = Math.max((int)(4*infoPaneWidth)+30, fieldX);
		d.setSize(Math.min(d.width, fieldX+10), 
				Math.min(d.height-BOTTOM_OFFSET, fieldY/(zoom)+totalTopHeight+60));


		// ensure proper width for info panes

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//		frame.setBounds(0,0, this.fieldX, this.fieldY+infoPaneHeight+10);
		frame.setSize(d);
		frame.setPreferredSize(d);
		frame.setMaximizedBounds(new Rectangle(d.width, d.height));

		contentPane = new JPanel();
		contentPane.setOpaque(true);
		//		contentPane.setBounds(0,0, this.fieldX, this.fieldY+infoPaneHeight+10);
		contentPane.setSize(d);
		contentPane.setPreferredSize(d);

		if (SHOW_TITLE){
			// create panel for title
			JPanel titlePanel = new JPanel();
			titlePanel.setSize(d.width, titlePaneHeight);
			titlePanel.setPreferredSize(new Dimension(d.width, titlePaneHeight));

			java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
			JButton c3Button = new JButton();
			c3Button.setSize(100, 100);
			c3Button.setIcon(new ImageIcon("swanspp/driver/images/c3_s.jpg"));
			c3Button.setPreferredSize(new Dimension(100,100));
			c3Button.setText("");
			c3Button.setMargin(new Insets(0,0,0,0));
			c3Button.setHorizontalAlignment(JButton.CENTER);
			c3Button.setVerticalAlignment(JButton.CENTER);
			c3Button.setIconTextGap(0);
			c3Button.setBorderPainted(false);
			c3Button.setContentAreaFilled(false);

			JButton aqButton = new JButton();
			aqButton.setSize(100, 100);
			aqButton.setIcon(new ImageIcon("swanspp/driver/images/aq_s.jpg"));
			aqButton.setPreferredSize(new Dimension(100,100));
			aqButton.setText("");
			aqButton.setMargin(new Insets(0,0,0,0));
			aqButton.setHorizontalAlignment(JButton.CENTER);
			aqButton.setVerticalAlignment(JButton.CENTER);
			aqButton.setIconTextGap(0);
			aqButton.setBorderPainted(false);
			aqButton.setContentAreaFilled(false);

			JLabel titleLabel = new JLabel("  Car-to-Car Cooperation for Vehicular Ad-hoc Networks  ");
			titleLabel.setFont(new Font("Garamond", Font.BOLD, 40));        
			titlePanel.add(c3Button);

			titlePanel.add(titleLabel);
			titlePanel.add(aqButton);
			c3Button.setLocation(0,0);
			aqButton.setLocation(d.width-100, 0);
			contentPane.add(titlePanel);
		}

		layeredPane = new JLayeredPane();
		layeredPane.setBackground(Color.WHITE);
		layeredPane.setBounds(0,0, fieldX/zoom, fieldY/zoom);
		layeredPane.setSize(fieldX/zoom, fieldY/zoom);
		layeredPane.setPreferredSize(new Dimension(fieldX/zoom, fieldY/zoom));
		layeredPane.setOpaque(true);

		contentPane.setLocation(0,0);

		JPanel topPanel = new JPanel();
		topPanel.setSize(600,TOP_PANEL_HEIGHT);
		topPanel.setPreferredSize(new Dimension(600,TOP_PANEL_HEIGHT));
		// button for pausing
		pauseButton = new JButton();
		pauseButton.setEnabled(true);
		pauseButton.setText("Pause"); 
		pauseButton.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				if (pauseButton.getText()=="Pause"){
					JistAPI.pause();
					pauseButton.setText("Resume");
				}
				else{
					JistAPI.resume();
					pauseButton.setText("Pause");
				}

			}

		});
		topPanel.add(pauseButton);

		timeLabel = new TimeLabel();
		timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		topPanel.add(timeLabel);


		// button for showing/hiding tabs
		hideTabButton = new JButton();
		hideTabButton.setEnabled(true);
		hideTabButton.setText("Hide Tabs"); 
		hideTabButton.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				if (showTabs){
					hideTabs();
					hideTabButton.setText("Show Tabs");
				}
				else{
					showTabs();
					hideTabButton.setText("Hide Tabs");
				}

			}

		});
		topPanel.add(hideTabButton);
		contentPane.add(topPanel);

		tabbedPane = new JTabbedPane();

		JPanel controlTab = new JPanel();
		tabbedPane.addTab("Control", null, controlTab,
				"For controlling visualization");

		tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

		JPanel textTab = new JPanel(new GridBagLayout());
		tabbedPane.addTab("Text", null, textTab,
				"For displaying text debugging info");
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);


		contentPane.add(tabbedPane);

		panel = new FieldPanel(nodes); // added
		panel.setOpaque(false);
		//		frame.setSize(this.fieldX, this.fieldY+infoPaneHeight+1);


		panel.setBounds(0,0, this.fieldX/zoom, this.fieldY/zoom);
		panel.setSize(fieldX/zoom, fieldY/zoom);
		panel.setPreferredSize(new Dimension(fieldX/zoom, fieldY/zoom));
		layeredPane.add(panel);


		JPanel middlePanel = new JPanel(new GridBagLayout());     
		//middlePanel.setBorder(BorderFactory.createLineBorder(Color.black));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0;
		c.gridx = 0;
		c.insets = new Insets(5,20,0,20);


		// button for showing/hiding tabs
		hideFieldButton = new JButton();
		hideFieldButton.setEnabled(true);
		hideFieldButton.setText("Hide Field"); 
		hideFieldButton.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				if (showField){
					showField=!showField;
					updateVisualizer();
					hideFieldButton.setText("Show Field");
				}
				else{
					showField=!showField;
					updateVisualizer();
					hideFieldButton.setText("Hide Field");
				}

			}

		});
		c.gridy++;
		middlePanel.add(hideFieldButton, c);

		// zoom JSpinner
		JPanel spinnerPanel = new JPanel();
		JLabel spinnerLabel = new JLabel("Zoom factor: ");
		SpinnerModel model = new SpinnerNumberModel(zoom, 1, 200, 1);
		zoomToggle = new ZoomToggle(model);
		spinnerPanel.add(spinnerLabel);
		spinnerPanel.add(zoomToggle);
		c.gridy=0;
		c.gridx+=2;
		middlePanel.add(spinnerPanel, c);



		controlTab.add(middlePanel);

		// checkbox for communication display
		JPanel checkBoxPanel2 = new JPanel();
		JLabel checkBoxLabel2 = new JLabel("Show communication: ");
		showCommunication = new JCheckBox();
		showCommunication.setAction(new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				if (!showCommunication.isSelected()) resetColors();

			}

		});
		checkBoxPanel2.add(checkBoxLabel2);
		checkBoxPanel2.add(showCommunication);
		c.gridy++;
		middlePanel.add(checkBoxPanel2, c);

		// checkbox for communication display
		JPanel checkBoxPanel3 = new JPanel();
		JLabel checkBoxLabel3 = new JLabel("Step through communication: ");
		stepCheckBox= new JCheckBox();

		checkBoxPanel3.add(checkBoxLabel3);
		checkBoxPanel3.add(stepCheckBox);
		c.gridy++;
		middlePanel.add(checkBoxPanel3, c);

		controlTab.add(middlePanel);  

		// buttons for starting/stopping nodes
		stopAllNodesButton = new JButton();
		stopAllNodesButton.setEnabled(true);
		stopAllNodesButton.setText("Stop all nodes"); 
		stopAllNodesButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				controlNodeMotion(true);                
			}        
		});
		c.gridx+=2;
		c.gridy=0;
		middlePanel.add(stopAllNodesButton, c);

		startAllNodesButton = new JButton();
		startAllNodesButton.setEnabled(true);
		startAllNodesButton.setText("Resume all nodes"); 
		startAllNodesButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				controlNodeMotion(false);                
			}        
		});        
		c.gridy++;
		middlePanel.add(startAllNodesButton, c);


		// TextBox for node identification
		//       JFormattedTextField format = new JFormattedTextField();
		identifyNodeBox = new JTextField(3);       
		identifyNodeBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				try
				{   	
					int node_id = Integer.valueOf(identifyNodeBox.getText()).intValue();
					if(node_id < nodes.size() && node_id !=0)
					{
						resetColors();
						setNodeColor(Math.abs(node_id), Color.RED); 
						panel.setComponentZOrder(((Node)nodes.get(node_id)), 10);
						identifyNodeBox.selectAll();            
					} 
					else
					{
						identifyNodeBox.setText("Err");
						identifyNodeBox.selectAll();
						System.out.println("NODE DOES NOT EXIST");
					}
				}
				catch(NumberFormatException excptn)
				{
					identifyNodeBox.setText("Err");
					identifyNodeBox.selectAll();          		
					System.out.println("ILLEGAL ARGUMENT FOR NODE");
				}

			}        
		});

		c.gridy = 0;
		c.gridx++;
		middlePanel.add(new JLabel("Identify Node:"));
		c.gridx++;
		middlePanel.add(identifyNodeBox, c);

		// key panel        
		keyPanel = new JPanel(new GridBagLayout());
		//        keyPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.BOTH;
		//        c2.gridwidth = GridBagConstraints.REMAINDER;

		c2.gridy = 0;
		c2.gridx=0;


		tabbedPane.addTab("Key", null, keyPanel,
				"For describing meaning of visualization components");

		tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
		//keyPanel.setPreferredSize(new Dimension(150, 150));
		//c2.gridy++; // for space


		//        JPanel textPanel = new JPanel();
		GridBagConstraints textConstraints = new GridBagConstraints();
		textConstraints.fill = GridBagConstraints.BOTH;
		textConstraints.insets = new Insets(10,10,0,10);
		//        textConstraints.ipadx = 10;
		textConstraints.gridy = 0;
		textConstraints.gridx=0;
		// This is used for general info
		generalEditorPane = new JEditorPane();
		generalEditorPane.setEditable(false);

		// This is used for routing-protocol-specific info
		routingEditorPane = new JEditorPane();
		routingEditorPane.setEditable(false);

		//		Put the editor pane in a scroll pane.

		showTextCheckBox = new JCheckBox("Show text");
		textConstraints.gridy = 0;
		textConstraints.gridx = 1;
		textTab.add(showTextCheckBox, textConstraints);
		textConstraints.gridy = 0;
		textConstraints.gridx = 0;
		showTextCheckBox.setSelected(true);

		JLabel routingLabel = new JLabel("Node-specific information");
		textTab.add(routingLabel, textConstraints);
		textConstraints.gridy++;

		JScrollPane editorScrollPane = new JScrollPane(routingEditorPane);
		editorScrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		editorScrollPane.setPreferredSize(new Dimension(infoPaneWidth, infoPaneHeight-10));
		editorScrollPane.setMinimumSize(new Dimension(10, 10));
		textTab.add(editorScrollPane, textConstraints);
		routingEditorPane.setText("Node-specific information");
		textConstraints.gridx++;
		textConstraints.gridy=0;

		//		Put the editor pane in a scroll pane.
		JLabel generalLabel = new JLabel("Summary information");
		textTab.add(generalLabel, textConstraints);
		textConstraints.gridy++;

		JScrollPane generalScrollPane = new JScrollPane(generalEditorPane);
		generalScrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		generalScrollPane.setPreferredSize(new Dimension(infoPaneWidth/2-10, infoPaneHeight-10));
		generalScrollPane.setMinimumSize(new Dimension(10, 10));
		textTab.add(generalScrollPane, textConstraints);
		//generalEditorPane.setLocation(0, fieldY+5);
		generalEditorPane.setText("General info");

		textConstraints.gridx++;
		textConstraints.gridy=0;

		// This is used for interference info
		JLabel intLabel = new JLabel("Interference information");
		textTab.add(intLabel, textConstraints);
		textConstraints.gridy++;

		interferenceEditorPane = new JEditorPane();
		interferenceEditorPane.setEditable(false);

		//		Put the editor pane in a scroll pane.
		JScrollPane interferenceScrollPane = new JScrollPane(interferenceEditorPane);
		interferenceScrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		interferenceScrollPane.setPreferredSize(new Dimension(infoPaneWidth, infoPaneHeight-10));
		interferenceScrollPane.setMinimumSize(new Dimension(10, 10));
		textTab.add(interferenceScrollPane,textConstraints);
		//interferenceEditorPane.setLocation(0, fieldY+5);
		interferenceEditorPane.setText("Inteference info");

		//		contentPane.add(layeredPane);


		fieldPane=new JScrollPane(layeredPane);
		fieldPane.setSize(d.width, fieldY/zoom+20);
		fieldPane.setPreferredSize(new Dimension(d.width, fieldY/zoom+20));
		fieldPane.setBackground(Color.WHITE);
		//fieldPane.setOpaque(true);

		//        fieldPane.setVerticalScrollBarPolicy(
		//                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		contentPane.add(fieldPane);



		frame.setContentPane(contentPane);

		// show frame
		frame.pack();
		frame.setVisible(true);

		layeredPane.repaint();

		this.frame = frame;
		updateVisualizer();
		frame.setAlwaysOnTop(alwaysOnTop);
		notifyAll();

	}




	/**
	 * Stops all nodes if <code>moving</code> is true; starts 
	 * them all otherwise.
	 * @param moving whether to start or stop
	 */
	protected void controlNodeMotion(boolean moving) {
		Iterator it = nodes.keySet().iterator();
		Integer id;
		while (it.hasNext()){
			id = (Integer) it.next();
			MobilityInfo mobInfo = field.getRadioData(id).getMobilityInfo();
			mobInfo.setStopped(moving); 
		}		
	}



	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#updateNodeLocation(float, float, int, jist.swans.misc.Location)
	 */
	public void updateNodeLocation(float newX, float newY, int ip, Location brg)
	{				
		Node n = (Node)nodes.get(new Integer(ip));
		if (n==null) return;
		n.updateLocation(newX, newY, null, circuitRoutes, field);  
		if (brg!=null) n.adjustBearing(brg);
		//  n.setIcon(new ImageIcon(carImage[0]));
		n.repaint();
		//		n.setIcon(new ImageIcon(carImage[0]));

	}


	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#displaceNode(jist.swans.misc.Location, int, jist.swans.misc.Location)
	 */
	public void displaceNode(Location step, int ip, Location brg) {

		Node n = (Node)nodes.get(new Integer(ip));
		if (n==null) return;
		n.x += step.getX();
		n.y += step.getY();
		n.updateLocation(n.x, n.y, null, circuitRoutes, field); 
		n.setToolTipText("Node "+ip+" @ ("+n.x+", "+ n.y+ ")");


		if (brg!=null) n.adjustBearing(brg);
		n.repaint();
		//        panel.repaint();
	}


	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setFocus(int)
	 */
	public void setFocus(int ip)
	{
		//        int pos = nodes.indexOf(new Node(0, 0, ip));
		Node n = (Node)nodes.get(new Integer(ip));
		if (n==null) return;
		n.requestFocusInWindow();
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#drawTransmitCircle(int)
	 */
	public void drawTransmitCircle(int ip)
	{
		//		int pos = nodes.indexOf(new Node(0, 0, ip));
		Node n = (Node)nodes.get(new Integer(ip));
		if (n==null) return;
		n.showRadius = true; 
		n.radioRadius = radioRadius;
		panel.repaint();

	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#drawAnimatedTransmitCircle(int, java.awt.Color)
	 */
	public void drawAnimatedTransmitCircle(int ip, Color color)
	{
		Node n = (Node)nodes.get(new Integer(ip));
		if (n!=null){
			n.radioRadius = radioRadius;
			n.animateRadius();
			//            if (n.t==null){
			//                
			//            RadioAnimator t = new RadioAnimator(n, panel, color);
			//            n.t=t;
			//            t.start();
			//            }
			//            else{
			//                ((RadioAnimator)n.t).stopThread();
			//                RadioAnimator t = new RadioAnimator(n, panel, color);
			//                n.t=t;
			//                t.start();
			//            }
			n.radiusColor = color;
			panel.repaint();
		}
		//        else{
		//            System.out.println("Visualizer: Could not find node "+ip);
		//        }
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#hideTransmitCircle(int)
	 */
	public void hideTransmitCircle(int ip)
	{
		//		int pos = nodes.indexOf(new Node(0, 0, ip));
		//		Node n = (Node)nodes.get(new Integer(ip));
		//		n.showRadius = false; 
		panel.repaint();
	}


	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setNodeColor(int, java.awt.Color)
	 */
	public void setNodeColor(int i, Color c) {
		Node n = (Node)nodes.get(new Integer(i));
		if (n == null){
			//            throw new RuntimeException("Blah!");
			return;
		}

		n.setBackground(c);
		n.setColor(c);
		panel.repaint();
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setToolTip(int, java.lang.String)
	 */
	public void setToolTip(int ip, String text) {
		//		int pos = nodes.indexOf(new Node(0, 0, ip));
		Node n = (Node)nodes.get(new Integer(ip));
		if (n==null) return;
		n.setToolTipText(text);
		n.addToMenu(text);

	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setRoutingPaneText(int, java.lang.String)
	 */
	public void setRoutingPaneText(int ip, String text) {
		if (UPDATE_ROUTING_TEXT && showTextCheckBox.isSelected())
			routingEditorPane.setText("Node "+ip+":\n"+text);				
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setGeneralPaneText(java.lang.String)
	 */
	public void setGeneralPaneText(String text) {
		if (!showTextCheckBox.isSelected()) return;
		generalEditorPane.setText(text);   
	}

	/**
	 * Sets the interference text for the panel.
	 * @param text
	 */
	public void setInterferencePaneText(String text) {
		if (!showInterferenceText || !showTextCheckBox.isSelected()) return;
		interferenceEditorPane.setText(text);   
	}

	/**
	 * Sets the max base distance that a message will propagate 
	 * (i.e., within 1 std dev).
	 * @param maxDistance
	 */
	public void setBaseTranmit(double maxDistance) {
		this.radioRadius = maxDistance;

	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#resetColors()
	 */
	public void resetColors() {
		Iterator it = nodes.values().iterator();
		while (it.hasNext())
		{
			Node n = (Node)it.next();
			n.resetColor();
			//			n.showRadius = false;

		}    
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#drawCircle(int, jist.swans.misc.Location)
	 */
	public void drawCircle(int r, Location loc) {
		// T-ODO update such that center of circle is at loc
		FieldPanel fp = (FieldPanel)panel;
		fp.circleRadius = r;
		fp.circleLoc = loc;
		fp.drawCircle = true;

	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#removeCircle()
	 */
	public void removeCircle() {
		FieldPanel fp = (FieldPanel)panel;
		fp.drawCircle = false;

	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#colorSegment(jist.swans.field.streets.RoadSegment, java.awt.Color)
	 */
	public void colorSegment(RoadSegment rs, Color c) {
		((FieldPanel)panel).setSegmentColor(rs, c);
		((FieldPanel)panel).repaint();
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#colorSegments(java.lang.Object[], java.awt.Color[])
	 */
	public void colorSegments(Object[] objects, Color colors[]) {

		RoadSegment[] rs = new RoadSegment[objects.length];
		for (int i = 0; i < objects.length; i++) rs[i] = (RoadSegment)objects[i];
		((FieldPanel)panel).setSegmentsColor(rs, colors);
		((FieldPanel)panel).repaint();
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#colorSegments(java.lang.Object[], java.awt.Color)
	 */
	public void colorSegments(Object[] objects, Color color) {
		Color[] colors = new Color[objects.length];
		RoadSegment[] rs = new RoadSegment[objects.length];
		for (int i = 0; i < objects.length; i++){
			rs[i] = (RoadSegment)objects[i];
			colors[i] = color;
		}
		((FieldPanel)panel).setSegmentsColor(rs, colors);
		((FieldPanel)panel).repaint();
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#removeNode(int)
	 */
	public void removeNode(int id) {

		Node n = (Node)nodes.remove(new Integer(id));
		panel.remove(n);
		n.setVisible(false);

		panel.repaint();

	}


	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#updateVisualizer()
	 */
	public void updateVisualizer()
	{
		Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		d.setSize(Math.min(d.width, Math.max(fieldX/zoom, 
				tabbedPane.isVisible()?tabbedPane.getWidth():0)+30), 
				Math.min(d.height-BOTTOM_OFFSET, 
						(showField ? fieldY/(zoom) : 0 )+totalTopHeight+90));

		frame.setPreferredSize(new Dimension(d.width, d.height+20));
		frame.setPreferredSize(new Dimension(d.width, d.height+70));
		frame.setMaximizedBounds(new Rectangle(d.width, d.height+20));

		contentPane.setSize(d);
		contentPane.setPreferredSize(d);

		layeredPane.setBounds(0,0, fieldX/zoom, fieldY/zoom);
		layeredPane.setSize(fieldX/zoom, fieldY/zoom);
		layeredPane.setPreferredSize(new Dimension(fieldX/zoom, fieldY/zoom));

		panel.setBounds(0,0, this.fieldX/zoom, this.fieldY/zoom);
		panel.setSize(fieldX/zoom, fieldY/zoom);
		panel.setPreferredSize(new Dimension(fieldX/zoom, fieldY/zoom));



		//      contentPane.add(layeredPane);
		fieldPane.setVisible(showField);
		if (showField){
			fieldPane.setSize(d.width-25, d.height-totalTopHeight-30);
			fieldPane.setPreferredSize(new Dimension(d.width-25, d.height-totalTopHeight-60));
			fieldPane.setLocation(0, totalTopHeight+10);  
		}

		//        fieldPane.setVerticalScrollBarPolicy(
		//                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		FieldPanel fp = ((FieldPanel)panel);
		if (je.sm!=null) fp.drawStreetMap(fp.segsToColor, fp.colors);
		frame.pack();
		if (nodes!=null){
			Iterator it = nodes.entrySet().iterator();
			while (it.hasNext()){
				Node n = (Node)((Entry)it.next()).getValue();
				if (n!=null)
					n.updateLocation(n.x, n.y, null, circuitRoutes, field);
			}
		}        
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setNodeImage(int, int)
	 */
	public void setNodeImage(int ip, int imageIndex) {
		Node n = (Node)nodes.get(new Integer(ip));
		if (n==null) return;
		n.setIcon(new ImageIcon(carImage[imageIndex]));

	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#addNode(float, float, int)
	 */
	public void addNode(float initX, float initY, int ip)
	{       
		// add node to field
		Node n = new Node(initX, initY, ip);
		n.radioRadius = radioRadius;
		nodes.put(new Integer(ip), n);
		panel.add(n);
		((FieldPanel)panel).nodes = nodes;
		n.setLocation((int)(initX/zoom), (int)(initY/zoom));
		//        frame.pack();
		//        
		//        frame.repaint();
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#addNode(float, float, int, int)
	 */
	public void addNode(float x, float y, int ip, int imageIndex) {
		// add node to field
		int mob = JistExperiment.getJistExperiment().getMobility();
		Node n = new Node(x, y, ip);
		if (mob==5 || mob ==6) 
			n.setIcon(new ImageIcon(carImage[imageIndex]));
		n.radioRadius = radioRadius;
		nodes.put(new Integer(ip), n); 
		panel.add(n);
		((FieldPanel)panel).nodes = nodes;
		n.setLocation((int)(x/zoom), (int)(y/zoom));

		//        frame.pack();
		//        
		//        frame.repaint();

	}

	public LinkedList[] getCircuits() {
		LinkedList route;
		Vector routes = new Vector();
		LinkedList routesLL[];
		String s[] = new String[]{};
		do {
			Frame f = JOptionPane.getFrameForComponent(frame);
			FieldPanel fp = new FieldPanel(new HashMap());
			fp.setBounds(0,0, this.fieldX/zoom, this.fieldY/zoom);
			fp.setSize(fieldX/zoom, fieldY/zoom);
			fp.setPreferredSize(new Dimension(fieldX/zoom, fieldY/zoom));

			JScrollPane jsp = new JScrollPane(fp);
			dialog = new ListDialog(f,
					generalEditorPane,
					"The route you've chosen so far is shown below. Click OK when done.",
					"Circuit Chooser",
					s,
					"todo: insert long value here                                                  ", 
					jsp);
			dialog.fieldPanel = fp;
			dialog.setVisible(true);
			route = dialog.value;
			if (route.size()>0) routes.add(route);
		} while (route!=null && route.size()!=0);

		routesLL = new LinkedList[routes.size()];
		for (int i =0; i < routes.size(); i++)
		{
			routesLL[i] = (LinkedList)routes.get(i);
		}

		// get file name
		circuitFileName = (String)JOptionPane.showInputDialog(
				frame,
				"Enter a file name to save path or " +
						"leave blank to skip saving: ",
						"Path Save Dialog",
						JOptionPane.QUESTION_MESSAGE,
						null,
						null,
						"defaultPath.path");

		if ((circuitFileName == null) || (circuitFileName.length() == 0)) {
			circuitFileName = null;
		}

		return routesLL;
	}

	public String getCircuitFileName() {
		return circuitFileName;
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#drawCircle(int, int)
	 */
	public void drawCircle(int ip, int r) {
		FieldPanel fp = (FieldPanel)panel;
		fp.circleRadius = r;
		Node n = (Node)nodes.get(new Integer(ip));
		if (n==null) return;
		fp.circleLoc = new Location.Location2D(n.x, n.y);
		fp.drawCircle = true;
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setNumberOfNodes(int)
	 */
	public void setNumberOfNodes(int totalNodes) {
		nodes = new HashMap(totalNodes);

	}



	/**
	 * Sets the routes that are used for VFN.
	 * @param routes array of LinkedLists of RoadSegments
	 */
	public void setCircuits(LinkedList[] routes) {
		this.circuitRoutes = routes;

	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#showCommunication()
	 */
	public boolean showCommunication() {
		return showCommunication.isSelected();
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#setField(jist.swans.field.Field)
	 */
	public void setField (Field f){

		this.field = f;
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#resetColor(int)
	 */
	public void resetColor(int nodenum) {
		Node n = (Node)nodes.get(new Integer(nodenum));
		if (n!=null) n.resetColor();

	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#updateTime(long)
	 */
	public void updateTime(long time) {
		timeLabel.setTime(time);
	}

	/* (non-Javadoc)
	 * @see driver.VisualizerInterface#registerKeyItem(java.awt.Color, int, java.lang.String)
	 */
	public void registerKeyItem(Color c, int type, String text){
		switch(type){
		case Visualizer.CIRCLE:
			int x= 10; int y=10;
			JPanelCircle jp = new JPanelCircle(c, x, y);
			jp.setSize(new Dimension(x,y));
			jp.setPreferredSize(new Dimension(x,y));
			if (c2.gridy % 5 == 0 && c2.gridy>0) {
				c2.gridy=1;
				c2.gridx+=3;
			}
			else c2.gridy++;
			keyPanel.add(jp, c2);
			c2.gridx++;
			JLabel jl = new JLabel(text);
			keyPanel.add(jl, c2);
			c2.gridx--;
			break;
		case Visualizer.CAR:
			JButton jb = new JButton();
			jb.setIcon(new ImageIcon(carImage[0]));
			jb.setSize(new Dimension(Node.buttonWidth, Node.buttonHeight));
			jb.setPreferredSize(new Dimension(Node.buttonWidth, Node.buttonHeight));
			jb.setBorderPainted(true);
			jb.setBorder(new LineBorder(c, 3)); 
			if (c2.gridy % 5 == 0 && c2.gridy>0) {
				c2.gridy=1;
				c2.gridx+=3;
			}
			else c2.gridy++;
			keyPanel.add(jb, c2);
			c2.gridx++;
			JLabel jl2 = new JLabel(text);
			keyPanel.add(jl2, c2);
			c2.gridx--;
			break;
		default: 
			throw new RuntimeException("Unsupported!");
		}
		frame.pack();
	}

	public static Visualizer getActiveInstance(){
		return activeInstance;
	}

	/**
	 * Return true if the node is still on the map; false otherwise.
	 * @param ip the node identifier
	 * @return
	 */
	public boolean hasNode(int ip) {
		return nodes.get(new Integer(ip)) != null;
	}

	public void addPersistentCirlce(Color c, int radius, int duration, Integer id){
		Location l = field.getRadioData(id).getLocation();
		synchronized(pCircles) {
			pCircles.add(new PersistentCircle(c, JistAPI.getTime()+duration*Constants.SECOND, radius, 
					(int)(l.getX()), (int)(l.getY())));
		}

	}

	public void addPersistentCircle(Color c, int radius, int duration, Location loc){
		synchronized(pCircles) {
			pCircles.add(new PersistentCircle(c, JistAPI.getTime()+duration*Constants.SECOND, radius, 
					(int)(loc.getX()), (int)(loc.getY())));
		}
	}

	/**
	 * @param p
	 */
	public void drawPolygon(Polygon p) {
		currentPolygon = p;


	}

	/**
	 * @param nodenum
	 * @param onVFR
	 */
	public void setOnVFR(int nodenum, boolean onVFR) {
		Node n = (Node)nodes.get(new Integer(nodenum));
		if (n!=null) n.onVFR = onVFR;

	}

	public void showTabs(){
		showTabs = true;
		totalTopHeight = infoPaneHeight 
				+ titlePaneHeight+BOTTOM_BUFFER+TOP_PANEL_HEIGHT;
		tabbedPane.setVisible(true);
		updateVisualizer();
	}

	public void hideTabs(){
		showTabs = false;
		totalTopHeight = BOTTOM_BUFFER;
		tabbedPane.setVisible(false);
		updateVisualizer();
	}

	public void pause(){
		pauseButton.doClick();
	}

	/**
	 * 
	 */
	 public void exit() {
		try {
			frame.setVisible(false);
			frame.dispose();
		} catch (Throwable e) {
			// T-ODO Auto-generated catch block
			e.printStackTrace();
		}

	 }

	 /**
	  * Shows or hides the field. 
	  * @param show true if field should be shown
	  */
	 public void showField(boolean show) {
		 showField = show;
		 updateVisualizer();

	 }


	 /**
	  * @return
	  */
	 public long getUsedMemory() {
		 long total = Runtime.getRuntime().totalMemory();
		 long free = Runtime.getRuntime().freeMemory();
		 return total - free;
	 }


	 /** 
	  * Returns true if the simulation should step through communication.
	  * @return
	  */
	 public boolean isStep() {
		 return stepCheckBox.isSelected();
	 }

	 /**
	  * Returns true if the simulation is paused.
	  * @return
	  */
	 public boolean isPaused() {
		 return pauseButton.getText().equals("Resume");
	 }

	 /**
	  * Returns true if text display is enabled.
	  * @return
	  */
	 public boolean showText(){
		 return showTextCheckBox.isSelected();
	 }

}