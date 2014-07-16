package jist.swans.radio;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import javax.swing.JFrame;

public class Vizbt {

	private JFrame frame;
	private float scaleX = 5;
	private float scaleY = 5;
	private float offsetX = -500;
	private float offsetY = -500;
	private AffineTransform ats;
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
	
	public void DrawShape(Shape shp, Color clr)
	{
		((Graphics2D)getGraph()).setColor(clr);
		((Graphics2D)getGraph()).draw(att.createTransformedShape(ats.createTransformedShape(shp)));
	}
	private Graphics defGraph = null;
	private Graphics getGraph()
	{
		if(defGraph == null)
		{
			defGraph =  frame.getGraphics();
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
		frame.setBounds(100, 100, 450, 300);
		frame.setExtendedState( frame.getExtendedState()|JFrame.MAXIMIZED_BOTH );
		//frame.getGraphics().dra
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		offsetX= frame.getSize().width/2;
		offsetY = frame.getSize().height/2;
		ats = AffineTransform.getScaleInstance(scaleX, scaleY);
		att = AffineTransform.getTranslateInstance(offsetX, offsetY);
	
	}

}
