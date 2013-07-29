/*
 * C3 Project - Car to Car Communication
 * Created on Mar 23, 2005, by David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * Copyright 2005, David Choffnes. All rights reserved.
 * 
 */
package jist.swans.route;

import java.util.HashMap;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.misc.Timer;


/**
 * 
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The NS2Timer class implements a one-shot timer that can be 
 * cancelled and rescheduled. Due to the JiST implementation, 
 * timer events cannot be rescheduled earlier than their original time.
 * 
 */
public abstract class NS2Timer implements Timer.ReusableTimer
{
    protected long currentTimeout;
    protected long timeoutInterval;

    protected int id;
    HashMap timers;
    RouteInterface.NS2TimerInterface route;
//    public Timer.ReusableTimer timerProxy; 


    public NS2Timer(RouteInterface.NS2TimerInterface route, HashMap timers)

    {
        currentTimeout = -1;
        timeoutInterval = -1;
        id = 0;
        this.timers = timers;
        this.route = route;
    }
    


    public void timeout()
    {
        throw new RuntimeException("Don't call timeout with no parameters!");
    }
    
    public void timeout(int idVal){ 
        if (idVal == this.id) // only do callback if the timer is active
        {
//            System.out.println("Current time: "+JistAPI.getTime() + 
//                    "\t Timeout time: "+currentTimeout);
// no
                callback(idVal);

        }
    }
    
    public boolean performCallback(int idVal)
    {
        if (idVal == this.id){
        long currentTime = JistAPI.getTime();
        if (currentTimeout == currentTime) // has timeout been rescheduled?
        {
            currentTimeout = -1;
            return true;
        }
        else if (currentTimeout > currentTime)
        {
            sched(((double)(currentTimeout - currentTime))/Constants.SECOND);
        }
        else
        {
            throw new RuntimeException("Invalid current time!");
        }
            
    }
        return false;
    }
    /**
     * Called to schedule a timeout. Should only be called when a timeout is not 
     * currently active.
     * 
     * @param time number of seconds to pause
     */
    public void sched(double time)
    {
        id++;
        timeoutInterval = (long)(time*Constants.SECOND);
        
        currentTimeout = timeoutInterval + JistAPI.getTime();
      
        
        Long index = new Long(Constants.random.nextLong());

        while (timers.containsKey(index)){
            index = new Long(Constants.random.nextLong());
        }

        
        timers.put(index, this);
        route.timeout(index, timeoutInterval, id);
        

    }
    
    /**
     * Called to schedule another timer interrupt.
     * 
     * @param time
     */
    public void resched(double time)
    {
        long currentTime = JistAPI.getTime();
//        id++;
          if (currentTimeout > currentTime)
          {
              long newValue = currentTime + (long)(time*Constants.SECOND);
              if (newValue > currentTimeout)
              {
                  currentTimeout = newValue;
              }
          }
          else
          {
              sched(time);
          }
        
    }
    
    /**
     * Cancels the timer until another is scheduled.
     *
     */
    public void force_cancel()
    {
        id++;
        
    }
    
    /**
     * Returns the status of the timer.
     * 
     * @return true if the timer is active; false otherwise
     */
    public boolean isActive() {
        return currentTimeout==-1;
    }
    
//    public Timer.ReusableTimer getProxy()
//    {
//        return timerProxy;
//    }
//    
    /**
     * The method that should be invoked on each timer interrupt.
     *
     */
    protected abstract void callback(int id);
    
}