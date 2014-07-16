package jist.swans.radio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.HashSet;

import jist.swans.field.streets.Shape;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import driver.GenericDriver;
//�irina: 1,7 +-0.3
//du�ina: 5+-0.5
//	private float vehicleDevLength =0;// 0.5F ;
//	private float vehicleLength =50.0F;//5

//private float vehicleDevWidth =0;//0.3F;
//	private float vehicleWidth = 50.0F;//1,7




public class VLCsensor
{

	public float distanceLimit = 250;
	public float visionAngle = 60;
	public float offsetX =10;
	public float offsetY = 10;
	public float sensorBearing = 0;
	public int sensorID = 0;
	public SensorModes mode;
	public SensorStates state;
	public RadioVLC node;
	public Location sensorLocation;
	public Location sensorLocation1;//top
	public Location sensorLocation2;//bottom
	private float sensorBearingNotRelative;
	private float stickOut = 0.001F; //1cm
	public int signalsRx;
	public Message CurrentMessage;
	public long CurrentMessageEnd;

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
		this.signalsRx = 0;
		this.distanceLimit = distancelimit; 
		this.visionAngle = visionAngle;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		if(this.offsetX >0)
		{
			this.offsetX += stickOut;
		}
		else
		{
			this.offsetX -= stickOut;
		}
		if(this.offsetY >0)
		{
			this.offsetY += stickOut;
		}
		else
		{
			this.offsetY -= stickOut;
		}
		this.sensorBearing = bearing;
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

	private Path2D.Float poly;
	public void UpdateShape(Location NodeLocation, float NodeBearing)
	{
		sensorBearingNotRelative = NodeBearing + sensorBearing;

		sensorLocation = RadioVLC.rotatePoint(NodeLocation.getX()+ offsetX, NodeLocation.getY()+ offsetY, NodeLocation, NodeBearing); //new Location.Location2D(tmpx, tmpy);//start.
		sensorLocation1 = getVLCCornerPoint(sensorBearingNotRelative - (visionAngle/2), sensorLocation, distanceLimit, visionAngle);
		sensorLocation2 = getVLCCornerPoint(sensorBearingNotRelative + (visionAngle/2), sensorLocation, distanceLimit, visionAngle);
		//	if(node.NodeID == nodeidtst)
		{
			poly = new Path2D.Float();// Polygon();
			
			poly.moveTo(sensorLocation.getX(), sensorLocation.getY());
			poly.lineTo(sensorLocation1.getX(), sensorLocation1.getY());
			poly.lineTo(sensorLocation2.getX(), sensorLocation2.getY());
			poly.closePath();
			/*
			poly.addPoint((int)sensorLocation.getX(), (int)sensorLocation.getY());
			poly.addPoint((int)sensorLocation1.getX(), (int)sensorLocation1.getY());
			poly.addPoint((int)sensorLocation2.getX(), (int)sensorLocation2.getY());*/
			if(mode == SensorModes.Receive)
			{
				GenericDriver.btviz.DrawShape(poly, Color.yellow);
				
			}
			else
			{
				GenericDriver.btviz.DrawShape(poly, Color.red);
			}
			
			//((Graphics2D) GenericDriver.btviz.getGraph()).draw(AffineTransform.getTranslateInstance(-500, -500).createTransformedShape(AffineTransform.getScaleInstance(10, 10).createTransformedShape(poly)));
			//GenericDriver.btviz.getGraph().drawPolygon(poly);
		//	GenericDriver.btviz.getGraph().setColor(Color.cyan);
		//	GenericDriver.btviz.getGraph().drawString(""+sensorID, (int)sensorLocation.getX()+5, (int)sensorLocation.getY());

		}
		//	System.out.println("draw bt "+ sensorBearingNotRelative + " - nb= "+NodeBearing+" - "+(int)sensorLocation.getX() + " "+ (int)sensorLocation.getY() + " " +(int)sensorLocation1.getX() + " "+ (int)sensorLocation1.getY() +" " +(int)sensorLocation2.getX() + " "+ (int)sensorLocation2.getY()  );
	}


	/**
	 * This method will need to be called two times. Each call will detect one corner point of the vlc device's view
	 * @param theta: the angle of the bearing +- visionAngle
	 * @param origin: starting point
	 * @param distanceLimit: the max distance the vlc device can see
	 * @param visionAngle: the angle at which the vlc device can see from the front of the car
	 */
	public Location getVLCCornerPoint(float theta, Location origin, float distLimit, float visionAngle)
	{
		Location cornerPoint; 
		int quadrant = 0; 
		float hypotenuse = (float)(distanceLimit/Math.cos(((visionAngle/2)*Math.PI)/(180))); 

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