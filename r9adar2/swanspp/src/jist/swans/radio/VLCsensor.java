package jist.swans.radio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;





import org.python.modules.math;

import jist.swans.Constants;
import jist.swans.field.streets.Shape;
import jist.swans.mac.MacMessage;
import jist.swans.mac.MacVLCMessage;
import jist.swans.misc.Location;
import jist.swans.misc.Location.Location2D;
import jist.swans.misc.Message;
import driver.GenericDriver;
//�irina: 1,7 +-0.3
//du�ina: 5+-0.5
//	private float vehicleDevLength =0;// 0.5F ;
//	private float vehicleLength =50.0F;//5
import driver.JistExperiment;

//private float vehicleDevWidth =0;//0.3F;
//	private float vehicleWidth = 50.0F;//1,7




public class VLCsensor
{

	public double distanceLimit = 250;
	public float visionAngle = 60;
	public float offsetX =10;
	public float offsetY = 10;
	public float Bearing = 0;
	public int sensorID = 0;
	public SensorModes mode;
	public SensorStates state;
	public RadioVLC node;
	public Location sensorLocation;
	public Location sensorLocation1;//top
	public Location sensorLocation2;//bottom
	private float sensorBearingNotRelative;
	private float stickOut = 0.00001F; //1cm
	//	public int signalsRx;
	public LinkedList<MacVLCMessage> Messages = new LinkedList<MacVLCMessage>();
	//	public long CurrentMessageEnd;
	//public long CurrentMessageDuration;
	public Path2D.Float coverageShape;

	public HashSet<Integer> controlSignal = new HashSet<Integer>();


	public enum SensorModes
	{
		Receive(0),
		Transmit(1),
		//SendAndReceive(2),
		Unknown(3);
		private int code;

		private SensorModes(int c) {
			code = c;
		}

		public int getCode() {
			return code;
		}
	};
	public enum SensorStates
	{	
		Receiving(0),
		Transmitting(1),
		Idle(2);
		private int code;

		private SensorStates(int c) {
			code = c;
		}

		public int getCode() {
			return code;
		}
	}

	public VLCsensor(int sensorID, RadioVLC node, float distancelimit, float visionAngle,Location originalLoc, float offsetX, float offsetY, float bearing, SensorModes mode) 
	{
		/*notes:
		 * kod 802.11 vlc radio treba slati i primati sa svih senzora i na taj nacin simulirati omni.
		 * na� mac bi trebao selektivno odabrati pojedini senzor (tx i rx) i samo njega koristiti. 
		 * �to je sa preklapanjima, doga�ati �e se da �e i lijevi i desni primiti signal 
		 * MAC:
		 * *Bearing(vektor, GPS), sve za topologiju, hardverski razmjestaj senzora na vozilu.
		 * *svaki cvor ce znati kada ec moci nekome poslati poruku i kada ce nekoga moci cuti 
		 * 
		 * LINKS:
		 * http://en.wikipedia.org/wiki/MIMO
		 * http://djw.cs.washington.edu/papers/mimo_for_dummies.pdf
		 * */
		this.node = node;
		this.distanceLimit = distancelimit; 
		this.visionAngle = visionAngle;
		this.offsetX = offsetX;
		this.offsetY = offsetY;


		float dAB  = (float) Point.distance(this.offsetX, this.offsetY, 0, 0);

		this.offsetX = (1* this.offsetX * (this.stickOut + dAB)) / dAB;
		this.offsetY = (1* this.offsetY * (this.stickOut + dAB)) / dAB;

		this.Bearing = bearing;
		this.sensorID = sensorID;
		this.mode = mode;
		this.state = SensorStates.Idle;
		UpdateShape(originalLoc, node.NodeBearing);
	}
	public void setState( SensorStates newState)
	{
		state = newState;
	}
	public SensorStates getState()
	{
		return state;
	}

	public float getScaledNumber(float num, int exp)
	{
		float res;
		for (int i = 0; i < 10; i++) {

			res = (float) (num / Math.pow(10, i));
			if( res < 1 )
			{
				res = (float) (res * Math.pow(10, i-exp));
				return res;
			}
		}
		return num;
	}
	public void UpdateShape(Location NodeLocation, float NodeBearing)
	{
		//https://www.youtube.com/watch?v=Cd3w8kH9g_c
		if(NodeLocation.getX() > 1000)
		{
			NodeLocation = new Location2D(getScaledNumber(NodeLocation.getX(), 2),
					getScaledNumber(NodeLocation.getY(), 2));

		}
		sensorBearingNotRelative = NodeBearing + Bearing;

		sensorLocation = RadioVLC.rotatePoint(NodeLocation.getX()+ offsetX, NodeLocation.getY()+ offsetY, NodeLocation, NodeBearing);
		//	sensorLocation = new Location.Location2D(10,5);		
		//distanceLimit = 6;

		sensorLocation1 = getVLCCornerPoint(sensorBearingNotRelative - (visionAngle/2), sensorLocation, distanceLimit, visionAngle);
		sensorLocation2 = getVLCCornerPoint(sensorBearingNotRelative + (visionAngle/2), sensorLocation, distanceLimit, visionAngle);
		//	if(node.NodeID == nodeidtst)

		coverageShape = new Path2D.Float();// Polygon();
		//TODO: provjeri zasto ne radi bas dobro za 90�

		double ax = sensorLocation.getX();
		double ay = sensorLocation.getY();
		double bx = sensorLocation1.getX();
		double by = sensorLocation1.getY();
		double cx = sensorLocation2.getX();
		double cy = sensorLocation2.getY();


		/*
			ax = 1;
			ay = 3;
			bx = 4;
			by = 4;
			cx = 4;
			cy = 2;
			distanceLimit =6;
			visionAngle =(float) 36.87;
		 */
		/*ax = 80;
		ay = 100;
		bx = 130;
		by = 70;
		cx = 110;
		cy = 50;
		distanceLimit =(float)84.85;
		visionAngle =(float) 28.07;
		 */
		/*	ax = 80;
		ay = 60;
		bx = 130;
		by = 60;
		cx = 120;
		cy = 30;
		distanceLimit =(float)76.33;
		visionAngle =(float) 36.87;
		 */

		double px = (cx+bx)/2;
		double py = (cy+by)/2;
		double A =ax;
		double B=ay;
		double C=px;
		double D=py;
		double R=distanceLimit;
		//http://www.wolframalpha.com/input/?i=%28x-A%29%5E2+%2B+%28y-B%29%5E2+%3D+R%5E2%2C+y-B%3D%28%28D-B%29%2F%28C-A%29%29*%28x-A%29
		double x1 = (A*A*A+Math.sqrt((R*R*(A-C)*(A-C)*(A*A-2*A*C+B*B-2*B*D+C*C+D*D)))-2*A*A*C+A*B*B-2*A*B*D+A*C*C+A*D*D) / (A*A-2*A*C+B*B-2*B*D+C*C+D*D);
		double y1 = (A*A*A*B+B*Math.sqrt(R*R*(A-C)*(A-C)* (A*A-2*A*C+B*B-2*B*D+C*C+D*D))-D*Math.sqrt(R*R*(A-C)*(A-C)*(A*A-2*A*C+B*B-2*B*D+C*C+D*D))-3*A*A*B*C+A*B*B*B-2*A*B*B*D+3*A*B*C*C+A*B*D*D-B*B*B*C+2*B*B*C*D-B*C*C*C-B*C*D*D)/((A-C)*(A*A-2*A*C+B*B-2*B*D+C*C+D*D));

		double x2 = (A*A*A-Math.sqrt((R*R*(A-C)*(A-C)*(A*A-2*A*C+B*B-2*B*D+C*C+D*D)))-2*A*A*C+A*B*B-2*A*B*D+A*C*C+A*D*D) / (A*A-2*A*C+B*B-2*B*D+C*C+D*D);
		double y2 = (A*A*A*B-B*Math.sqrt(R*R*(A-C)*(A-C)*(A*A-2*A*C+B*B-2*B*D+C*C+D*D))+D*Math.sqrt(R*R*(A-C)*(A-C)*(A*A-2*A*C+B*B-2*B*D+C*C+D*D))-3*A*A*B*C+A*B*B*B-2*A*B*B*D+3*A*B*C*C+A*B*D*D-B*B*B*C+2*B*B*C*D-B*C*C*C-B*C*D*D)/((A-C)*(A*A-2*A*C+B*B-2*B*D+C*C+D*D));


		double d1 = Math.sqrt(Math.pow(x1 - px, 2) + Math.pow(y1 - py, 2) );
		double d2 = Math.sqrt(Math.pow(x2 - px, 2) + Math.pow(y2 - py, 2) );

		double dx,dy;

		if(d1 < d2)
		{
			dx= x1;
			dy= y1;
		}
		else
		{
			dx= x2;
			dy =y2;
		}
		
		coverageShape.moveTo(ax, ay);
		coverageShape.lineTo(bx, by);

		coverageShape.quadTo(dx, dy, cx,cy);// .curveTo(cx, cy, dx+10, dy, dx+10, dy);
		coverageShape.closePath();
		/*
			poly.addPoint((int)sensorLocation.getX(), (int)sensorLocation.getY());
			poly.addPoint((int)sensorLocation1.getX(), (int)sensorLocation1.getY());
			poly.addPoint((int)sensorLocation2.getX(), (int)sensorLocation2.getY());*/
		if(JistExperiment.getJistExperiment().useVisualizer)
		{
			
			if(JistExperiment.getJistExperiment().placement == Constants.PLACEMENT_GRID)
			{
				if(mode == SensorModes.Receive)
				{
					GenericDriver.btviz.DrawShape(coverageShape, Color.blue,1);
				}
				else
				{
					GenericDriver.btviz.DrawShape(coverageShape, Color.red,1);
				}

				GenericDriver.btviz.DrawString(this.sensorID+"", Color.BLACK, sensorLocation.getX(), sensorLocation.getY() , node.NodeLocation.getX(), node.NodeLocation.getY(), 0.3F, 0.9F);
			}
		}
		//((Graphics2D) GenericDriver.btviz.getGraph()).drawArc(x, y, width, height, startAngle, arcAngle); .drawString("a(" + ax + ","+ay+")",(float)ax,(float)ay);

		//((Graphics2D) GenericDriver.btviz.getGraph()).draw(AffineTransform.getTranslateInstance(-500, -500).createTransformedShape(AffineTransform.getScaleInstance(10, 10).createTransformedShape(poly)));
		//GenericDriver.btviz.getGraph().drawPolygon(poly);
		//	GenericDriver.btviz.getGraph().setColor(Color.cyan);
		//	GenericDriver.btviz.getGraph().drawString(""+sensorID, (int)sensorLocation.getX()+5, (int)sensorLocation.getY());

		//	System.out.println("draw bt "+ sensorBearingNotRelative + " - nb= "+NodeBearing+" - "+(int)sensorLocation.getX() + " "+ (int)sensorLocation.getY() + " " +(int)sensorLocation1.getX() + " "+ (int)sensorLocation1.getY() +" " +(int)sensorLocation2.getX() + " "+ (int)sensorLocation2.getY()  );
	}


	/**
	 * This method will need to be called two times. Each call will detect one corner point of the vlc device's view
	 * @param theta: the angle of the bearing +- visionAngle
	 * @param origin: starting point
	 * @param distanceLimit: the max distance the vlc device can see
	 * @param visionAngle: the angle at which the vlc device can see from the front of the car
	 */
	public Location getVLCCornerPoint(float theta, Location origin, double distLimit, float visionAngle)
	{
		Location cornerPoint; 
		int quadrant = 0; 
		//	distLimit = distLimit * (float)Math.cos(Math.toRadians(visionAngle/2));

		double hypotenuse = distLimit;// (float)(distLimit/Math.cos(((visionAngle/2)*Math.PI)/(180))); 

		//first detect what quadrant theta falls, to see how bearing affects corner points
		if(theta >= 0 && theta <= 90)
		{
			quadrant = 1;

			cornerPoint = new Location.Location2D(origin.getX()+ (float) (hypotenuse * Math.cos((Math.PI*(theta))/(180))),origin.getY()+ (float) (hypotenuse * Math.sin((Math.PI*(theta))/(180))));			
		}
		else if(theta > 90 && theta <= 180)
		{
			quadrant = 2; 	
			cornerPoint = new Location.Location2D(origin.getX()+(float) (-1 * hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))),origin.getY()+ (float) (hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180)))); 
		}
		else if(theta > 180 && theta <= 270)
		{
			quadrant = 3;
			//x coordinate needs sin function here. the Y axis between the 3rd and 4th quadrants is being used to compute the angle.
			//likewise, cos needs to be used here to find the y coordinate.
			cornerPoint = new Location.Location2D(origin.getX()+(float) (-1 * hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180))),origin.getY()+ (float) (-1 * hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))));					
		}
		else
		{
			quadrant = 4;		
			cornerPoint = new Location.Location2D(origin.getX()+(float) (hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))),origin.getY()+ (float) (-1 * hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180))));
		}
		//cornerPoint = new Location.Location2D(//co, y)
		return cornerPoint; 
	}

	/*public LinkedList<Location> getCoverageNodes()
			{

			}*/

}//VLCsensor
