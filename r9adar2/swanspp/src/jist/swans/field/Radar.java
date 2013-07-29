package jist.swans.field;
import jist.swans.misc.Location;

public class Radar {
	
	float distanceLimit = 250; 	//distance limit the radar can see in front of it
	int visionAngle = 30; 		//The viewing angle the of the radar, default is 30 degrees 
	
	Location origin; 					//serving as the (x,y) point closest to the vechicle as looking away from the car with the radar installed
	Location cornerPoint1; 		//serving as the left most (x,y) point on the triangle as looking away from the car with the radar installed
	Location cornerPoint2; 			//serving as the right most (x,y) point on the triange as looking away from the car with the radar installed
	

	Radar()										//constructor
	{
		this.distanceLimit = 250; 
		this.visionAngle = 30; 
	}
	
	Radar(Location origin, float distanceLimit, int visionAngle)		//overloaded constructor where can specificy the origin (point at which the vision angle is created), the distance limit of the radar, and the angle
	{
		this.origin = origin; 
		this.distanceLimit = distanceLimit;
		this.visionAngle = visionAngle; 
	}
	
	//getters--needed because we have created private fields.
	public Location getBottomBoundaryPoint(Radar radar)				 
	{
		return radar.cornerPoint1; 
	}
	
	public Location getTopBoundaryPoint(Radar radar)
	{
		return radar.cornerPoint2; 
	}
	
	public Location getOrigin(Radar radar)
	{
		return radar.origin; 
	}
	
	public int getVisionAngle(Radar radar)
	{
		return radar.visionAngle;
	}

	public float getDistanceLimit(Radar radar)
	{
		return radar.distanceLimit; 
	}
	//**end getters
	
	/**
	 * calculateBottomBoundaryPoint and calculateTopBoundaryPoint mostly used for testing and self discovery purposes. Not used in actual simulation.
	 * The two methods will find corner points of a radar boundary and are predecessors for the getRadarBounds method 
	 */
	public Location calculateBottomBoundaryPoint(Location origin, float distanceLimit, int visionAngle)				//given an origin point, viewing distance and an angle, return the left boundary point 
	{
		float remainingAngle = (180 - (visionAngle/2) - 90); 
		float xCoordinate = origin.getX() + distanceLimit; 
		float yCoordinate = (float) (origin.getY() - (distanceLimit/(Math.tan(remainingAngle*Math.PI/180))));	
		
		cornerPoint1 = new Location.Location2D(xCoordinate, yCoordinate); 						//create a new location based on the coordinate
		
		return cornerPoint1; 
		
	}
	
	public Location calculateTopBoundaryPoint(Location origin, int distanceLimit, int visionAngle)				//given an origin point, viewing distance and an angle, return the right boundary point 
	{
		float remainingAngle = (180 - (visionAngle/2) - 90); 
		float xCoordinate = origin.getX() + distanceLimit; 
		float yCoordinate = (float) (origin.getY() + (distanceLimit/(Math.tan(remainingAngle*Math.PI/180))));
		
		cornerPoint2 = new Location.Location2D(xCoordinate, yCoordinate);
		return cornerPoint2; 
	}
	
	/**
     * Is the location being checked visible to our radar system?
     * 
     * @param p1,p2 are the coordinates to be checked corresponding to the x,y values
     * @param x1,y1 are the x,y coordinates of the first point on the triangle, probably the origin -- or starting location of the node/car
     * @param x2,y2 are the x,y coordinates of the second point on the triangle, 1 corner point calculated by the getRadarBounds method
     * @param x3,y3 are the x,y coordinates of the third point on the triangle, a second point calculated by the getRadarBounds method
     * @return true/false of whether or not the location falls within our calculated radar bounds. 
     */
	public boolean visibleToRadar(float p1, float p2, float x1, float y1, float x2, float y2, float x3, float y3)		
	{		
		
		if(tripletOrientation(x1,y1,x2,y2,p1,p2)*tripletOrientation(x1,y1,x2,y2,x3,y3)>0 && tripletOrientation(x2,y2,x3,y3,p1,p2)*tripletOrientation(x2,y2,x3,y3,x1,y1)>0 && tripletOrientation(x3,y3,x1,y1,p1,p2)*tripletOrientation(x3,y3,x1,y1,x2,y2)>0)
		{
			return true;
		}
		else
		{
			return false; 
		}
	}
	
	public float tripletOrientation(float x1, float y1, float x2, float y2, float x3, float y3)
	{
		return x1*(y2 - y3) + x2*(y3 - y1) + x3*(y1 - y2);  
	}
	
	/**
	 * @param start is the starting point
	 * @param end is the destination
	 * returns an angle we call theta to discover the radar bounds via the getRadarBounds() method
	 */
	public float getBearing(Location start, Location end)
	{
		float bearingAngle = 0.0f;
		
		bearingAngle = (float) Math.toDegrees(Math.atan2(end.getY() - start.getY(), end.getX() - start.getX()));
		
		if(bearingAngle < 0)
		{
			return bearingAngle + 360; 
		}
		else
		{
			return bearingAngle;
		}
	}
	
	/**
	 * This method will need to be called two times. Each call will detect one corner point of the radar's view
	 * @param theta: the angle of the bearing +- visionAngle
	 * @param origin: starting point
	 * @param distanceLimit: the max distance the radar can see
	 * @param visionAngle: the angle at which the radar can see from the front of the car
	 */
	public Location getRadarCornerPoint(float theta, Location origin, float distanceLimit, int visionAngle)
	{
		Location cornerPoint; 
		int quadrant = 0; 
		float hypotenuse = (float)(distanceLimit/Math.cos(((visionAngle/2)*Math.PI)/(180))); 
		
		//first detect what quadrant theta falls, to see how bearing affects corner points
		if(theta >= 0 && theta <= 90)
		{
			quadrant = 1;
			
			cornerPoint = new Location.Location2D((float) (hypotenuse * Math.cos((Math.PI*(theta))/(180))), (float) (hypotenuse * Math.sin((Math.PI*(theta))/(180))));			
		}
		else if(theta > 90 && theta <= 180)
		{
			quadrant = 2; 	
			cornerPoint = new Location.Location2D((float) (-1 * hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))), (float) (hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180)))); 
		}
		else if(theta > 180 && theta <= 270)
		{
			quadrant = 3;
			//x coordinate needs sin function here. the Y axis between the 3rd and 4th quadrants is being used to compute the angle.
			//likewise, cos needs to be used here to find the y coordinate.
			cornerPoint = new Location.Location2D((float) (-1 * hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180))), (float) (-1 * hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))));					
		}
		else
		{
			quadrant = 4;		
			cornerPoint = new Location.Location2D((float) (hypotenuse * Math.cos((Math.PI*(90*quadrant-theta))/(180))), (float) (-1 * hypotenuse * Math.sin((Math.PI*(90*quadrant-theta))/(180))));
		}
		
		return cornerPoint; 
	}

}
