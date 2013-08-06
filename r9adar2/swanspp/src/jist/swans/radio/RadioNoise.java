//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioNoise.java Tue 2004/04/13 18:22:55 barr glenlivet.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import driver.Visualizer;
import jist.swans.field.FieldInterface;
import jist.swans.mac.Mac802_11;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacInterface;
import jist.swans.mac.MacStats;
import jist.swans.misc.Message;
import jist.swans.mac.MacMessage;
import jist.swans.net.NetMessage;
import jist.swans.Constants;
import jist.runtime.JistAPI;

/** 
 * <code>RadioNoise</code> is an abstract class which implements some functionality
 * that is common to the independent and additive radio noise simulation models.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: RadioNoise.java,v 1.1 2007/04/09 18:49:46 drchoffnes Exp $
 * @since SWANS1.0
 */

public abstract class RadioNoise implements RadioInterface
{

  //////////////////////////////////////////////////
  // locals
  //

  // properties

  /**
   * radio properties.
   */
  protected RadioInfo radioInfo;

  // state

  /**
   * radio mode: IDLE, SENSING, RECEIVING, SENDING, SLEEP.
   */
  protected byte mode;

  /**
   * message being received.
   */
  protected Message signalBuffer;

  /**
   * end of transmission time.
   */
  protected long signalFinish;

  /**
   * transmission signal strength.
   */
  protected double signalPower_mW;

  /**
   * number of signals being received.
   */
  protected int signals;

  // entity hookup

  /**
   * field entity downcall reference.
   */
  protected FieldInterface fieldEntity;

  /**
   * self-referencing radio entity reference.
   */
  protected RadioInterface self;

  /**
   * mac entity upcall reference.
   */
  protected MacInterface macEntity;

  /** allows radio to tell high layers the level of congestion */
private static final boolean RECORD_CONGESTION = true;

/** period for measuring congestion */
private static final long congestionInterval = 1 * Constants.SECOND;

protected static final boolean TRACK_DROPS = false;

/** time that channel became idle */
private long idleStartTime;

/** total idle time during current period */
private long totalIdleTime;

private boolean fired = false;

private double lastRatio = 0.0;

private long intervalStartTime;

public MacStats stats = new MacStats();

protected long seqNumber;



  //////////////////////////////////////////////////
  // initialize 
  //

  /**
   * Create a new radio.
   *
   * @param id radio identifier
   * @param sharedInfo shared radio properties
   */
  protected RadioNoise(int id, RadioInfo.RadioInfoShared sharedInfo)
  {
    mode = Constants.RADIO_MODE_IDLE;
    radioInfo = new RadioInfo(new RadioInfo.RadioInfoUnique(), sharedInfo);
    radioInfo.unique.id = new Integer(id);
    unlockSignal();
    signals = 0;
    this.self = (RadioInterface)JistAPI.proxy(this, RadioInterface.class);
    idleStartTime = 0;
    totalIdleTime = 0;
    seqNumber = 0;

  }

  //////////////////////////////////////////////////
  // entity hookups
  //

  /**
   * Return self-referencing radio entity reference.
   *
   * @return self-referencing radio entity reference
   */
  public RadioInterface getProxy()
  {
    return this.self;
  }

  /**
   * Set upcall field entity reference.
   *
   * @param fieldEntity upcall field entity reference
   */
  public void setFieldEntity(FieldInterface fieldEntity)
  {
    if(!JistAPI.isEntity(fieldEntity)) throw new IllegalArgumentException("entity expected");
    this.fieldEntity = fieldEntity;
  }

  /**
   * Set downcall mac entity reference.
   *
   * @param macEntity downcall mac entity reference
   */
  public void setMacEntity(MacInterface macEntity)
  {
    if(!JistAPI.isEntity(macEntity)) throw new IllegalArgumentException("entity expected");
    this.macEntity = macEntity;
  }

  /**
   * Sets the stats object for recording statistics.
   * 
   * @param stats The stats object to set.
   */
  public void setStats(MacStats stats)
  {
      this.stats = stats;
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Return radio properties.
   *
   * @return radio properties
   */
  public RadioInfo getRadioInfo()
  {
    return radioInfo;
  }

  /**
   * Set radio mode. Also notifies mac entity.
   *
   * @param mode radio mode
   */
  public void setMode(byte mode)
  {
    if(this.mode!=mode)
    {
        if (RECORD_CONGESTION){
            if (!fired){
                self.recordActivity(congestionInterval);
            }
            // check if new mode is idle
            if (mode <= Constants.RADIO_MODE_IDLE )
            {
                // check that old mode wasn't idle
                if (this.mode > Constants.RADIO_MODE_IDLE )
                {
                    idleStartTime = JistAPI.getTime();
                }
            }
            else // new mode is not idle, check if old mode was idle
            {
                if (this.mode <= Constants.RADIO_MODE_IDLE)
                    {
                        totalIdleTime += (JistAPI.getTime()-idleStartTime);
                    }
            }
        }
      this.mode = mode;
      this.macEntity.setRadioMode(mode);

    }    
  }

  /**
   * Turn radio off (sleep) or on.
   *
   * @param sleep whether to turn off radio
   */
  public void setSleepMode(boolean sleep)
  {
    setMode(sleep ? Constants.RADIO_MODE_SLEEP : Constants.RADIO_MODE_IDLE);
  }

  //////////////////////////////////////////////////
  // signal acquisition
  //

  /**
   * Lock onto current packet signal.
   *
   * @param msg packet currently on the air
   * @param power_mW signal power (units: mW)
   * @param duration time to EOT (units: simtime)
   */
  protected void lockSignal(Message msg, double power_mW, long duration)
  {
      if (TRACK_DROPS && signalBuffer!=null){
    	  stats.updateDrops(msg, signalBuffer, radioInfo.getUnique().getID(), mode);
      }
      seqNumber++;
    signalBuffer = msg;
    signalPower_mW = power_mW;
    signalFinish = JistAPI.getTime() + duration;
    this.macEntity.peek(msg);
  }
  
  /**
   * Unlock from current packet signal.
   */
  protected void unlockSignal()
  {
    signalBuffer = null;
    signalPower_mW = 0;
    signalFinish = -1;
  }  
  public void recordActivity(long congestionInterval)
  {
      long currentTime = JistAPI.getTime();
      if (mode <= Constants.RADIO_MODE_IDLE){
          totalIdleTime += currentTime - idleStartTime;
          idleStartTime = currentTime;
      }
      
      if (fired && intervalStartTime!=currentTime){
          lastRatio = ((double)totalIdleTime)/congestionInterval;
      }
      else
      {
          lastRatio = 1.0;
          fired = true;
      }
      intervalStartTime = currentTime;
      totalIdleTime = 0; 

      JistAPI.sleep(congestionInterval);
      self.recordActivity(congestionInterval);
  }
  
  public double getActivityRatio()
  {
      long currentInterval = JistAPI.getTime() - intervalStartTime;
      double weight = ((double)currentInterval)/congestionInterval;
      
      return (lastRatio * 1/(1+weight)) + 
      	(((double)totalIdleTime)/currentInterval) * (weight/(1+weight));
  }

} // class: RadioNoise

