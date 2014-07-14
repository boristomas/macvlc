//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioNoiseIndep.java Tue 2004/04/20 09:00:20 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.misc.Message;
import jist.swans.misc.Util;

/** 
 * <code>RadioNoiseIndep</code> implements a radio with an independent noise model.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: RadioNoiseIndep.java,v 1.1 2007/04/09 18:49:46 drchoffnes Exp $
 * @since SWANS1.0
 */

public final class RadioNoiseIndep extends RadioNoise
{

  //////////////////////////////////////////////////
  // locals
  //

  /**
   * threshold signal-to-noise ratio.
   */
  protected double thresholdSNR;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new radio with independent noise model.
   *
   * @param id radio identifier
   * @param sharedInfo shared radio properties
   */
  public RadioNoiseIndep(int id, RadioInfo.RadioInfoShared sharedInfo)
  {
    this(id, sharedInfo, Constants.SNR_THRESHOLD_DEFAULT);
  }

  /**
   * Create new radio with independent noise model.
   *
   * @param id radio identifier
   * @param sharedInfo shared radio properties
   * @param thresholdSNR threshold signal-to-noise ratio
   */
  public RadioNoiseIndep(int id, RadioInfo.RadioInfoShared sharedInfo, double thresholdSNR)
  {
    super(id, sharedInfo);
    setThresholdSNR(thresholdSNR);
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Set signal-to-noise ratio.
   *
   * @param snrThreshold threshold signal-to-noise ratio
   */
  public void setThresholdSNR(double snrThreshold)
  {
    this.thresholdSNR = snrThreshold;
  }

  //////////////////////////////////////////////////
  // reception
  //

  // RadioInterface interface
  /** {@inheritDoc} */
  public void receive(Message msg, Double powerObj_mW, Long durationObj)
  {
    final double power_mW = powerObj_mW.doubleValue();
    final long duration = durationObj.longValue();
    // ignore if below sensitivity
    if(power_mW < radioInfo.shared.sensitivity_mW) return;
    // discard message if below threshold
    if(power_mW < radioInfo.shared.threshold_mW  || power_mW < radioInfo.shared.background_mW * thresholdSNR) msg = null;
    switch(mode)
    {
      case Constants.RADIO_MODE_IDLE:
        if(msg!=null) setMode(Constants.RADIO_MODE_RECEIVING);
        lockSignal(msg, power_mW, duration);
        break;
      case Constants.RADIO_MODE_RECEIVING:
        if(Main.ASSERT) Util.assertion(signals>0);
        if(power_mW >= radioInfo.shared.threshold_mW
            &&  power_mW > signalPower_mW*thresholdSNR)
        {
          lockSignal(msg, power_mW, duration);
        }
        break;
      case Constants.RADIO_MODE_TRANSMITTING:
        break;
      case Constants.RADIO_MODE_SLEEP:
        break;
      default:
        throw new RuntimeException("invalid radio mode: "+mode);
    }
    // increment number of incoming signals
    signals++;
    // schedule an endReceive
    JistAPI.sleep(duration); 
    self.endReceive(powerObj_mW, new Long(seqNumber));
  }
  
  // RadioInterface interface
  /** {@inheritDoc} */
  public void endReceive(final Double powerObj_mW, Long seqNumber)
  {
    if(mode==Constants.RADIO_MODE_SLEEP) return;
    if(Main.ASSERT) Util.assertion(signals>0);
    signals--;
    if(mode==Constants.RADIO_MODE_RECEIVING)
    {
      if(signalBuffer!=null && JistAPI.getTime()==signalFinish)
      {
        this.macEntity.receive(signalBuffer);
        unlockSignal();
      }
      if(signals==0) setMode(Constants.RADIO_MODE_IDLE);
    }
  }
  //////////////////////////////////////////////////
  // transmission
  //

  // RadioInterface interface
  /** {@inheritDoc} */
  public void transmit(Message msg, long delay, long duration)
  {  
    // radio in sleep mode
    if(mode==Constants.RADIO_MODE_SLEEP) return;
    // ensure not currently transmitting
    if(mode==Constants.RADIO_MODE_TRANSMITTING) throw new RuntimeException("radio already transmitting");
    // clear receive buffer
    assert(signalBuffer==null);
    signalBuffer = null;
    
    // use default delay, if necessary
    if(delay==Constants.RADIO_NOUSER_DELAY) delay = Constants.RADIO_PHY_DELAY;
    // set mode to transmitting
    setMode(Constants.RADIO_MODE_TRANSMITTING);
    // schedule message propagation delay
    JistAPI.sleep(delay);
    fieldEntity.transmit(radioInfo, msg, duration);
    // schedule end of transmission
    JistAPI.sleep(duration);
    self.endTransmit();
  }

  // RadioInterface interface
  /** {@inheritDoc} */
  public void endTransmit()
  {
    // radio in sleep mode
    if(mode==Constants.RADIO_MODE_SLEEP) return;
    // check that we are currently transmitting
    if(mode!=Constants.RADIO_MODE_TRANSMITTING) throw new RuntimeException("radio is not transmitting");
    // set mode
    setMode(signals>0 ? Constants.RADIO_MODE_RECEIVING : Constants.RADIO_MODE_IDLE);
  }


} // class: RadioNoiseIndep

