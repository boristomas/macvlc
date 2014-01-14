package jist.swans.radio;

import java.awt.EventQueue;
import java.awt.Graphics;

import javax.swing.JFrame;

public class Vizbt {

	private JFrame frame;

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
	
	private Graphics defGraph = null;
	public Graphics getGraph()
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
	}

}
