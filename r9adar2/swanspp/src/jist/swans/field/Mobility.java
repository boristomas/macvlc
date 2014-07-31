//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Mobility.java Tue 2004/04/06 11:31:03 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.field;

import java.util.Random;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.misc.Location;
import jist.swans.misc.Location.Location2D;
import jist.swans.misc.Util;
import driver.Visualizer;
import driver.VisualizerInterface;

/** 
 * Interface of all mobility models.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Mobility.java,v 1.1 2007/04/09 18:49:27 drchoffnes Exp $
 * @since SWANS1.0
 */
public interface Mobility
{

  /**
   * Initiate mobility; initialize mobility data structures.
   *
   * @param f field entitybear
   * @param id node identifier
   * @param loc node location
   * @return mobility information object
   */
  MobilityInfo init(FieldInterface f, Integer id, Location loc);

  /**
   * Schedule next movement. This method will again be called after every
   * movement on the field.
   *
   * @param f field entity
   * @param id radio identifier
   * @param loc destination of move
   * @param info mobility information object
   */
  void next(FieldInterface f, Integer id, Location loc, MobilityInfo info);


  //////////////////////////////////////////////////
  // mobility information
  //

  /**
   * Interface of algorithm-specific mobility information objects.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static interface MobilityInfo
  {
    /** The null MobilityInfo object. */
    MobilityInfo NULL = new MobilityInfo()
    {
        boolean isStopped = false;
        public float getSpeed(){return 0L;};
        public Location getBearing(){return null;};
        public float getBearingAsAngle()
  	  {
  			// T-ODO Auto-generated method stub
  			return 0;
  	  }
        public void setStopped(boolean stop){ isStopped = stop;};
        public boolean isStopped(){return isStopped;};
    };

    /**
     * Returns the current speed in m/s
     * @return the current speed
     */
    float getSpeed();
    /**
     * Returns the bearing for the current node
     * @return
     */
    Location getBearing();
    float getBearingAsAngle();
    
    public void setStopped(boolean stop);
    public boolean isStopped();
  }


  //////////////////////////////////////////////////
  // static mobility model
  //


  /**
   * Static (noop) mobility model.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static class Static implements Mobility
  {
    //////////////////////////////////////////////////
    // Mobility interface
    //
      /** the visualization object */
      private VisualizerInterface v;
      
    /** {@inheritDoc} */
    public MobilityInfo init(FieldInterface f, Integer id, Location loc)
    {
    	StaticInfo smi = new StaticInfo();
    	//smi.v = v;
    	return smi;
    }

    /** {@inheritDoc} */
    public void next(FieldInterface f, Integer id, Location loc, MobilityInfo info)
    {
    }
    
    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#setGUI(driver.Visualizer)
     */
    public void setGUI(VisualizerInterface visualizer) {
        v = visualizer;
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#setMobilityEnabled(boolean, java.lang.Integer)
     */
    public void setMobilityEnabled(boolean isEnabled, Integer id) {
        
    }

  } // class Static
  public static class StaticInfo implements MobilityInfo
  {
  	  //random direction selected between [0,2pi] uniformly.
  	  public double direction = 2*Math.PI*Constants.random.nextDouble();

  	public Location getBearing() {
  		
  	/*	 if (nextEnd==null) return new Location.Location2D(0,0); // T-ODO something better here...
         if (nextEnd.distance(current.getEndPoint())==0)
         {
             return current.getStartPoint().bearing(current.getEndPoint());
         }
         else
         {
             return current.getEndPoint().bearing(current.getStartPoint());
         }*/
         return null;
  	}
  	public float getBearingAsAngle()
  	  {

  		float bearingAngle = 0.0f;
		
	//	bearingAngle = (float) Math.toDegrees(Math.atan2(current.getEndPoint().getY() - current.getStartPoint().getY(), current.getEndPoint().getX() - current.getStartPoint().getX()));
		
		if(bearingAngle < 0)
		{
			return bearingAngle + 360; 
		}
		else
		{
			return bearingAngle;
		}
  	  }

  	public float getSpeed() {
  		// T-ODO Auto-generated method stub
  		return 0;
  	}

  	public boolean isStopped() {
  		// T-ODO Auto-generated method stub
  		return false;
  	}

  	public void setStopped(boolean stop) {
  		// T-ODO Auto-generated method stub
  		
  	}
  	  
  	  
  } // class: RandomDirectionInfo 


  //////////////////////////////////////////////////
  // random waypoint mobility model
  //

  /**
   * Random waypoint state object.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class RandomWaypointInfo implements MobilityInfo
  {
    /** number of steps remaining to waypoint. */
    public int steps;

    /** duration of each step. */
    public long stepTime;

    /** waypoint. */
    public Location waypoint;
    
    /** speed */
    public float speed;
    /** bearing */
    public Location bearing;
    public float getBearingAsAngle()
	  {
			// T-ODO Auto-generated method stub
			return 0;
	  }
    
    public boolean isStopped;
    
    public float getSpeed(){
        if (steps==0) return 0;
        else return speed;
    }
    
    public Location getBearing(){
        return bearing;
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility.MobilityInfo#setStopped(boolean)
     */
    public void setStopped(boolean stop) {
        isStopped = stop;
        
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility.MobilityInfo#isStopped()
     */
    public boolean isStopped() {
        return isStopped;
    }
  }

  /**
   * Random waypoint mobility model.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class RandomWaypoint implements Mobility
  {
    /** thickness of border (for float calculations). */
    public static final float BORDER = (float)0.0005;

    /** Movement boundaries. */
    private Location.Location2D bounds;

    /** Waypoint pause time. */
    private long pauseTime;

    /** Step granularity. */
    private float precision;

    /** Minimum movement speed. */
    private float minspeed; 

    /** Maximum movement speed. */
    private float maxspeed;
    
    /** Private random number generator. */
    private Random randomWaypoint;
    /** the visualizaiton object */
    private VisualizerInterface v;
    
    /**
     * Initialize random waypoint mobility model.
     *
     * @param bounds boundaries of movement
     * @param pauseTime waypoint pause time
     * @param precision step granularity
     * @param minspeed minimum speed
     * @param maxspeed maximum speed
     */
    public RandomWaypoint(Location.Location2D bounds, long pauseTime, 
        float precision, float minspeed, float maxspeed)
    {
      init(bounds, pauseTime, precision, minspeed, maxspeed);
    }

    /**
     * Initialize random waypoint mobility model.
     *
     * @param bounds boundaries of movement
     * @param pauseTime waypoint pause time
     * @param precision step granularity
     * @param minspeed minimum speed
     * @param maxspeed maximum speed
     * @param random custom random number generator
     */
    public RandomWaypoint(Location.Location2D bounds, long pauseTime, 
        float precision, float minspeed, float maxspeed, Random random)
    {
      init(bounds, pauseTime, precision, minspeed, maxspeed);
      this.randomWaypoint = random;
    }
    
    
    /**
     * Initialize random waypoint mobility model.
     *
     * @param bounds boundaries of movement
     * @param config configuration string
     */
    public RandomWaypoint(Location.Location2D bounds, String config)
    {
    	//START --- Added by Emre Atsan
    	   
    	String wayPointConfigOptions [];
    	wayPointConfigOptions= config.split(":");
    	
    	//DEBUG//
    	/*for(int i=0; i<4;i++)
    	{
    		System.out.println(wayPointConfigOptions[i]);
    	}
    	*/	
    	//END-DEBUG//
    	
    		
    	init(bounds,Long.parseLong(wayPointConfigOptions[0]), Float.parseFloat(wayPointConfigOptions[1]),Float.parseFloat(wayPointConfigOptions[2]),Float.parseFloat(wayPointConfigOptions[3]));
    	
    	// throw new RuntimeException("not implemented");
    		
    	//END -- Added by Emre Atsan
    }
    
    /**
     * Initialize random waypoint mobility model.
     *
     * @param bounds boundaries of movement
     * @param pauseTime waypoint pause time (in ticks)
     * @param precision step granularity
     * @param minspeed minimum speed
     * @param maxspeed maximum speed
     */
    private void init(Location.Location2D bounds, long pauseTime, 
        float precision, float minspeed, float maxspeed)
    {
      this.bounds = bounds;
      this.pauseTime = pauseTime;
      this.precision = precision;
      this.minspeed = minspeed;
      this.maxspeed = maxspeed;
    }

    //////////////////////////////////////////////////
    // Mobility interface
    //

    /** {@inheritDoc} */
    public MobilityInfo init(FieldInterface f, Integer id, Location loc)
    {
      return new RandomWaypointInfo();
    }

    /** {@inheritDoc} */
    public void next(FieldInterface f, Integer id, Location loc, MobilityInfo info)
    {
    	if (Visualizer.getActiveInstance()!=null) 
    		Visualizer.getActiveInstance().updateTime(JistAPI.getTime());
        
    	Location brg = null;
      if(Main.ASSERT) Util.assertion(loc.inside(bounds));
      try
      {
        RandomWaypointInfo rwi = (RandomWaypointInfo)info;
        if(rwi.steps==0)
        {
          // reached waypoint
          JistAPI.sleep(pauseTime);
          rwi.waypoint = new Location.Location2D(
              (float)(BORDER + (bounds.getX()-2*BORDER)*randomWaypoint.nextFloat()),
              (float)(BORDER + (bounds.getY()-2*BORDER)*randomWaypoint.nextFloat()));
          if(Main.ASSERT) Util.assertion(rwi.waypoint.inside(bounds));
          rwi.speed = minspeed + (maxspeed-minspeed) * randomWaypoint.nextFloat();
          float dist = loc.distance(rwi.waypoint);
          rwi.bearing = loc.bearing(rwi.waypoint);
          brg = rwi.bearing;
          rwi.steps = (int)Math.max(Math.floor(dist / precision),1);
          if(Main.ASSERT) Util.assertion(rwi.steps>0);
          float time = dist / rwi.speed;
          rwi.stepTime = (long)(time*Constants.SECOND/rwi.steps);
        }
        // take step
        JistAPI.sleep(rwi.stepTime);
        if (info.isStopped()) f.moveRadio(id, loc);
        else{
            Location step = loc.step(rwi.waypoint, rwi.steps--);
            f.moveRadioOff(id, step);
            if (v!=null) v.displaceNode(step, id.intValue(), brg);
        }
      }
      catch(ClassCastException e) 
      {
        // different mobility model installed
      }
    }
    
    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#setGUI(driver.Visualizer)
     */
    public void setGUI(VisualizerInterface visualizer) {
        v = visualizer;
    }

  } // class RandomWaypoint


  //////////////////////////////////////////////////
  // Teleport mobility model
  //

  /**
   * Teleport mobility model: pick a random location and teleport to it,
   * then pause for some time and repeat.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class Teleport implements Mobility
  {
    /** Movement boundaries. */
    private Location.Location2D bounds;

    /** Waypoint pause time. */
    private long pauseTime;
    /** the visualization object */
    private VisualizerInterface v;

    /**
     * Initialize teleport mobility model.
     *
     * @param bounds boundaries of movement
     * @param pauseTime waypoint pause time (in ticks)
     */
    public Teleport(Location.Location2D bounds, long pauseTime)
    {
      this.bounds = bounds;
      this.pauseTime = pauseTime;
    }

    /** {@inheritDoc} */
    public MobilityInfo init(FieldInterface f, Integer id, Location loc)
    {
      if(pauseTime==0) return null;
      return MobilityInfo.NULL;
      
    }

    /** {@inheritDoc} */
    public void next(FieldInterface f, Integer id, Location loc, MobilityInfo info)
    {
      if(pauseTime>0)
      {
        JistAPI.sleep(pauseTime);
        if (info.isStopped()) f.moveRadio(id, loc);
        else{
            Location loc2 = new Location.Location2D(
                (float)bounds.getX()*Constants.random.nextFloat(),
                (float)bounds.getY()*Constants.random.nextFloat());
            f.moveRadio(id, loc2);
        
            if (v!=null) v.updateNodeLocation(loc.getX(), loc.getY(), id.intValue(), 
                loc.bearing(loc2));
        }
      }
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#setGUI(driver.Visualizer)
     */
    public void setGUI(VisualizerInterface visualizer) {
        v = visualizer;
    }
    
  } // class: Teleport


  /**
   * Random Walk mobility model: pick a direction, walk a certain distance in
   * that direction, with some fixed and random component, reflecting off walls
   * as necessary, then pause for some time and repeat.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class RandomWalk implements Mobility
  {
    /** fixed component of step size. */
    private double fixedRadius;
    /** random component of step size. */
    private double randomRadius;
    /** time wait between steps. */
    private long pauseTime;
    /** field boundaries. */
    private Location.Location2D bounds;
    /** the visualizer object */
    private VisualizerInterface v;

    /**
     * Create and initialize new random walk object.
     *
     * @param bounds field boundaries
     * @param fixedRadius fixed component of step size
     * @param randomRadius random component of step size
     * @param pauseTime time wait between steps
     */
    public RandomWalk(Location.Location2D bounds, double fixedRadius, double randomRadius, long pauseTime)
    {
      init(bounds, fixedRadius, randomRadius, pauseTime);
    }

    /**
     * Create an initialize a new random walk object.
     *
     * @param bounds field boundaries
     * @param config configuration string: "fixed,random,time(in seconds)"
     */
    public RandomWalk(Location.Location2D bounds, String config)
    {
      String[] data = config.split(",");
      if(data.length!=3)
      {
        throw new RuntimeException("expected format: fixedradius,randomradius,pausetime(in seconds)");
      }
      double fixedRadius = Double.parseDouble(data[0]);
      double randomRadius = Double.parseDouble(data[1]);
      long pauseTime = Long.parseLong(data[2])*Constants.SECOND;
      init(bounds, fixedRadius, randomRadius, pauseTime);
    }

    /**
     * Initialize random walk object.
     *
     * @param bounds field boundaries
     * @param fixedRadius fixed component of step size
     * @param randomRadius random component of step size
     * @param pauseTime time wait between steps
     */
    private void init(Location.Location2D bounds, double fixedRadius, double randomRadius, long pauseTime)
    {
      if(fixedRadius+randomRadius>bounds.getX() || fixedRadius+randomRadius>bounds.getY())
      {
        throw new RuntimeException("maximum step size can not be larger than field dimensions");
      }
      this.bounds = bounds;
      this.fixedRadius = fixedRadius;
      this.randomRadius = randomRadius;
      this.pauseTime = pauseTime;
    }

    //////////////////////////////////////////////////
    // mobility interface
    //

    /** {@inheritDoc} */
    public MobilityInfo init(FieldInterface f, Integer id, Location loc)
    {
      if(pauseTime==0) return null;
      return MobilityInfo.NULL;
    }

    /** {@inheritDoc} */
    public void next(FieldInterface f, Integer id, Location loc, MobilityInfo info)
    {
      // compute new random position with fixedRadius+randomRadius() distance
      double randomAngle = 2*Math.PI*Constants.random.nextDouble();
      double r = fixedRadius + Constants.random.nextDouble()*randomRadius;
      double x = r * Math.cos(randomAngle), y = r * Math.sin(randomAngle);
      double lx = loc.getX()+x, ly = loc.getY()+y;
      // bounds check and reflect
      if(lx<0) lx=-lx;
      if(ly<0) ly=-ly;
      if(lx>bounds.getX()) lx = bounds.getX()-lx;
      if(ly>bounds.getY()) ly = bounds.getY()-ly;
      // move
      if(pauseTime>0)
      {
        JistAPI.sleep(pauseTime);
        if (info.isStopped()) f.moveRadio(id, loc);
        else {
            Location l = new Location.Location2D((float)lx, (float)ly);        
            //System.out.println("move at t="+JistAPI.getTime()+" to="+l);
            f.moveRadio(id, l);
            if (v!=null) v.updateNodeLocation(l.getX(), l.getY(), id.intValue(), 
                    loc.bearing(l));
        }
      }
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "RandomWalk(r="+fixedRadius+"+"+randomRadius+",p="+pauseTime+")";
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility#setGUI(driver.Visualizer)
     */
    public void setGUI(VisualizerInterface visualizer) {
        v = visualizer;
    }


  } // class: RandomWalk


/**
 * Sets the class that will be used to display node locations.
 * @param visualizer the GUI object to set
 */
void setGUI(VisualizerInterface visualizer);

public static class RandomDirectionInfo implements MobilityInfo
{
	  //random direction selected between [0,2pi] uniformly.
	  public double direction = 2*Math.PI*Constants.random.nextDouble();

	public Location getBearing() {
		// T-ODO Auto-generated method stub
		return null;
	}
	public float getBearingAsAngle()
	  {
			// T-ODO Auto-generated method stub
			return 0;
	  }

	public float getSpeed() {
		// T-ODO Auto-generated method stub
		return 0;
	}

	public boolean isStopped() {
		// T-ODO Auto-generated method stub
		return false;
	}

	public void setStopped(boolean stop) {
		// T-ODO Auto-generated method stub
		
	}
	  
	  
} // class: RandomDirectionInfo 

public static class RandomDirection implements Mobility
{
	  private long pauseTime;
	  
	  private float constantVelocity;
	  
	  private Location.Location2D bounds;
	  
	  
	  public RandomDirection(Location.Location2D bounds, String config)
	    {
	    	//START --- Added by Emre Atsan
	   
	    	String directionConfigOptions [];
	    	directionConfigOptions= config.split(":");
	    	
	    	init(bounds,Float.parseFloat(directionConfigOptions[0]),Long.parseLong(directionConfigOptions[1]));
	    	
	    	// throw new RuntimeException("not implemented");
	    		
	    	//END -- Added by Emre Atsan
	    }
	  
	public RandomDirection(Location.Location2D bounds, float constantVelocity, long pauseTime )
	{
		init(bounds,constantVelocity,pauseTime);
	}
	  
	  
	private void init(Location.Location2D bounds, float constantVelocity, long pauseTime) {
		
		if(constantVelocity > bounds.getX() || constantVelocity > bounds.getY())
		{
			throw new RuntimeException("Speed (m/sec) cannot be larger than simulation area size!");
		}
		else
		{
			this.bounds = bounds;
			this.constantVelocity = constantVelocity;
			this.pauseTime = pauseTime*Constants.SECOND;
		
		}
	}


	public MobilityInfo init(FieldInterface f, Integer id, Location loc) {
	      if(pauseTime==0) return null;
	      return new RandomDirectionInfo();
	}

	public void next(FieldInterface f, Integer id, Location loc, MobilityInfo info) {
		
		if(Main.ASSERT) Util.assertion(loc.inside(bounds));
	      try
	      {
	        RandomDirectionInfo rdi = (RandomDirectionInfo)info;
	        double nodeAngle = rdi.direction;
	        
	        double x = constantVelocity * Math.cos(nodeAngle), y = constantVelocity * Math.sin(nodeAngle);
	        double lx = loc.getX()+x, ly = loc.getY()+y;
	        boolean sleptBefore =false;
	        // bounds check and reflect
	        if(lx<0) 
	        {
	        	lx=-lx;
	        	//Update Node Movement Angle after reflection from the bound.
	        	rdi.direction = Math.PI-nodeAngle;
	        	JistAPI.sleep(pauseTime);
	        	sleptBefore=true;
	        }
	        else if(lx>bounds.getX()) 
	        {
	        	lx = bounds.getX()-(lx-bounds.getX());
	        	rdi.direction = Math.PI-nodeAngle;
	        	JistAPI.sleep(pauseTime);
	        	sleptBefore=true;
	        }
	        
	        if(ly<0) 
	        {
	        	ly=-ly;
	        	rdi.direction = -nodeAngle;
	        	if(!sleptBefore)
	        	{
	        		JistAPI.sleep(pauseTime);
	        	}
	        }
	        else if(ly>bounds.getY())
	        {
	        	ly = bounds.getY()-(ly-bounds.getY());
	        	rdi.direction = -nodeAngle;
	        	
	        	if(!sleptBefore)
	        	{
	        		JistAPI.sleep(pauseTime);
	        	}
	        }
	       
	        //Sleep for one second in every step of the movement.
	        JistAPI.sleep(1*Constants.SECOND);

	        Location l = new Location.Location2D((float)lx, (float)ly);
	       
	       if(Main.ASSERT) Util.assertion(l.inside(bounds));
	        
	       
	       if(id ==1)
	       {
	    	   System.out.println(id+"\t"+l.getX()+"\t"+l.getY());
	       } 
	       f.moveRadio(id, l);
	        
	      }
	      catch(ClassCastException e) 
	      {
	        // different mobility model installed
	      }
	        
	}
	
  public String toString()
  {
    return "RandomDirection(speed="+constantVelocity+" ,p="+pauseTime+")";
  }

public void setGUI(VisualizerInterface visualizer) {
	// T-ODO Auto-generated method stub
	
}  
	
} // class: RandomDirection

public static class BoundlessSimulationAreaInfo implements MobilityInfo
{
	  //velocity of the mobile node
	  public double velocity;
	  public double direction;
	  
	  public BoundlessSimulationAreaInfo(float velocity, double direction)
	  {
		  this.velocity = velocity;
		  this.direction= direction;
	  }

	  public float getBearingAsAngle()
	  {
			// T-ODO Auto-generated method stub
			return 0;
	  }
	public Location getBearing() {
		// T-ODO Auto-generated method stub
		return null;
	}

	public float getSpeed() {
		// T-ODO Auto-generated method stub
		return 0;
	}

	public boolean isStopped() {
		// T-ODO Auto-generated method stub
		return false;
	}

	public void setStopped(boolean stop) {
		// T-ODO Auto-generated method stub
		
	}
	  
} // class: BoundlessSimulationAreaInfo 

public static class BoundlessSimulationArea implements Mobility
{
	  private Location.Location2D bounds;
	  
	  private double vMax;
	  private double aMax;
	  private double deltaT;
	  private double maxAngularChange;

	  public BoundlessSimulationArea(Location.Location2D bounds,double vMax, double aMax,double deltaT,double maxAngularChange)
	  {
		 init(bounds,vMax,aMax,deltaT,maxAngularChange);
		  
	  }
	  
	  public BoundlessSimulationArea(Location2D bounds, String config) {
		// T-ODO Auto-generated constructor stub
	    	String directionConfigOptions [];
	    	directionConfigOptions= config.split(":");
	    	
	    	init(bounds,Double.parseDouble(directionConfigOptions[0]),Double.parseDouble(directionConfigOptions[1]),Double.parseDouble(directionConfigOptions[2]),Double.parseDouble(directionConfigOptions[3]));
	    	
	}

	private void init(Location.Location2D bounds,double vMax, double aMax,double deltaT,double maxAngularChange)
	  {
		  this.aMax = aMax;
		  this.vMax = vMax;
		  this.deltaT = deltaT;
		  this.maxAngularChange = maxAngularChange*Math.PI;
		  this.bounds = bounds;
	  }
	  
	public MobilityInfo init(FieldInterface f, Integer id, Location loc) {
		
		return new BoundlessSimulationAreaInfo(0,0);
	}

	public void next(FieldInterface f, Integer id, Location loc, MobilityInfo info) {
		
		if(Main.ASSERT) Util.assertion(loc.inside(bounds));
		
		 try
	      {
	        BoundlessSimulationAreaInfo bsai = (BoundlessSimulationAreaInfo)info;
	        
	        double currentVelocity = bsai.velocity;
	        double currentDirection = bsai.direction;
	       
	        //change in the velocity which is uniformly distributed between [-aMax*deltaT,aMax*deltaT]
	       double deltaV = ((Constants.random.nextDouble()*2.0*aMax)-aMax)*deltaT;
	       //change in the direction which is uniformly distributed between [-MaxAngularChange*deltaT,maxAngularChange*deltaT]
	       double changeInDirection = ((Constants.random.nextDouble()*2.0*maxAngularChange)-maxAngularChange)*deltaT;
	       
	    	   double nextVelocity = Math.min(Math.max(currentVelocity+deltaV,0.0),vMax);
	    	   double nextDirection = currentDirection + changeInDirection;
	    	 
	    	   //coordinates of calculated next location.
	    	   double lx = (loc.getX() + currentVelocity*Math.cos(currentDirection));
	    	   double ly = (loc.getY() + currentVelocity*Math.sin(currentDirection));
	    	   
	    	   //update the MobilityInfo data for the next step calculations.
	    	   bsai.velocity = nextVelocity;
	    	   bsai.direction = nextDirection;
	    	   
	    	   // bounds check and wrap-around
		        if(lx<0) 
		        {
		        	lx = bounds.getX()+lx;
		        }
		        else if(lx>bounds.getX()) 
		        {
		        	lx=lx - bounds.getX();
		        }
		        
		        if(ly<0) 
		        {
		        	ly = bounds.getY()+ly;
		        }
		        else if(ly>bounds.getY())
		        {
		        	ly=ly - bounds.getY();
		        }
		       
		        //Sleep for one second in every step of the movement.
		        JistAPI.sleep((long)deltaT*Constants.SECOND);
		        
		        Location l = new Location.Location2D((float)lx, (float)ly);
		       
		       if(Main.ASSERT) Util.assertion(l.inside(bounds));
		        
		      // if(id==1)
		       //{System.out.println(id+"\t"+l.getX()+"\t"+l.getY());}
		        
		        f.moveRadio(id, l);
	    	   
	      }
	      catch(ClassCastException e) 
	      {
	        // different mobility model installed
	      }
		
	}
	
	public String toString()
  {
    return "BoundlessSimulationArea(Max. Velocity="+vMax+" ,Max.Accelaration="+aMax+" ,deltaT="+deltaT+" ,Max. Angular Change in direction (per sec.)"+maxAngularChange+")";
  }

	public void setGUI(VisualizerInterface visualizer) {
		// T-ODO Auto-generated method stub
		
	}  
	
}//class: BoundlessSimulationArea

} // interface Mobility

/*
rimtodo: other mobility models
   Gauss-Markov Model
   Nomadic Community Model
*/
