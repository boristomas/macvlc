package jist.swans.radio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.border.StrokeBorder;

import org.python.modules.newmodule;

import driver.GenericDriver;
import sun.java2d.ScreenUpdateManager;

import javax.swing.JLabel;

import java.awt.BorderLayout;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.GridLayout;

import net.miginfocom.swing.MigLayout;

public class Vizbt {

	private JFrame frame;
	private float scaleX = 10;
	private float scaleY = 10;
	private float offsetX = -200;
	private float offsetY = -200;
	private AffineTransform ats;
	private AffineTransform ats2;
	private AffineTransform att;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Vizbt window = new Vizbt();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void BringToFront()
	{
		this.frame.toFront();
	}
	public JFrame GetFrame()
	{
		return this.frame;
	}
	
	public void DrawShape(Shape shp, Color clr)
	{
		((Graphics2D)getGraph()).setStroke(oldStroke);
		((Graphics2D)getGraph()).setColor(clr);
		((Graphics2D)getGraph()).draw(att.createTransformedShape(ats.createTransformedShape(shp)));
	}
	public void DrawShape(Shape shp, Color clr, float StrokeWidth)
	{
		//StrokeWidth = 4;
		//((Graphics2D)getGraph()).setStroke(new BasicStroke());	
		((Graphics2D)getGraph()).setStroke(new BasicStroke(StrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 3, 8 }, 0));
		((Graphics2D)getGraph()).setColor(clr);
		((Graphics2D)getGraph()).draw(att.createTransformedShape(ats.createTransformedShape(shp)));
	}
	Font fnt = new Font(Font.MONOSPACED, Font.BOLD, 13);
	int asc =0;
	FontMetrics fm;
	Rectangle2D rect;
	public void DrawString(String str, Color clr, float x, float y)
	{
		DrawString(str, clr, x, y, x, y, 0,0);
	}
	public void DrawString(String str, Color clr, float x, float y, float xc, float yc, float zx, float zy)
	{
		((Graphics2D)getGraph()).setColor(clr);
		Point2D pt =new Point2D.Float(x+(x-xc)*zx,y+ (y-yc)*zy);
		pt= ats.transform(pt, pt);
		pt= att.transform(pt, pt);
		
		((Graphics2D)getGraph()).setFont(fnt);
		fm =((Graphics2D)getGraph()).getFontMetrics(fnt);
		rect = fm.getStringBounds(str, ((Graphics2D)getGraph()));
		asc =fm.getDescent();
		((Graphics2D)getGraph()).drawString(str, (float) (pt.getX() - rect.getWidth()/2), (float) (pt.getY() - rect.getHeight() / 2) + fm.getAscent());
	}
	private Graphics defGraph = null;
	private Stroke oldStroke;
	public Graphics getGraph()
	{
		if(defGraph == null)
		{
			defGraph =  frame.getGraphics();
			oldStroke = ((Graphics2D)defGraph).getStroke();
		}
		return defGraph;
	}

	/**
	 * Create the application.
	 */
	public Vizbt() {
		initialize();
		
		this.frame.setVisible(true);
		((Graphics2D)getGraph()).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setLocationRelativeTo(null);
		frame.getContentPane().setBackground(Color.WHITE);
		
		//frame.setBounds(100, 100, 450, 300);
		frame.setExtendedState( frame.getExtendedState()|JFrame.MAXIMIZED_BOTH );
		
		
		//frame.getGraphics().dra
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		
		final JLabel lblLoc = new JLabel("loc");
		frame.getContentPane().add(lblLoc, BorderLayout.NORTH);
		frame.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent arg0) {
				 lblLoc.setText(arg0.getPoint().toString());
			}
		});
		//this.frame.getContentPane().getSize().width
		
		
		offsetX = -1*(Toolkit.getDefaultToolkit().getScreenSize().width/10);
		offsetY = -1*(Toolkit.getDefaultToolkit().getScreenSize().height/2);//scaleY;
		ats = AffineTransform.getScaleInstance(scaleX, scaleY);
		
		att = AffineTransform.getTranslateInstance(offsetX, offsetY);
	
	}
	

}
