//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioNoiseAdditive.java Tue 2004/04/13 18:16:53 barr glenlivet.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import java.util.HashMap;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.mac.MacMessage;
import jist.swans.mac.MacMessage.Data;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.NetMessage;
import jist.swans.route.GPSRMessage;
import jist.swans.trans.TransUdp;

/** 
 * <code>RadioNoiseAdditive</code> implements a radio with an additive noise model.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: RadioNoiseAdditive.java,v 1.1 2007/04/09 18:49:47 drchoffnes Exp $
 * @since SWANS1.0
 */

public class RadioNoiseAdditive extends RadioNoise
{
  //////////////////////////////////////////////////
  // constants
  //

  /** signal-to-noise error model constant. */
  public static final byte SNR = 0;

  /** bit-error-rate error model constant. */
  public static final byte BER = 1;
  
  public static final boolean TRACK_DROPS = true;

  //////////////////////////////////////////////////
  // locals
  //
  private HashMap droppedSignals;
  
  //
  // properties
  //

  /**
   * radio type: SNR or BER.
   */
  protected byte type;

  /**
   * threshold signal-to-noise ratio.
   */
  protected float thresholdSNR;

  /**
   * bit-error-rate table.
   */
  protected BERTable ber;

  //
  // state
  //

  /**
   * total signal power.
   */
  protected double totalPower_mW;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new radio with additive noise model.
   *
   * @param id radio identifier
   * @param shared shared radio properties
   */
  public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared)
  {
    this(id, shared, (float)Constants.SNR_THRESHOLD_DEFAULT);
  }

  /**
   * Create a new radio with additive noise model.
   *
   * @param id radio identifier
   * @param shared shared radio properties
   * @param snrThreshold_mW threshold signal-to-noise ratio
   */
  public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared, float snrThreshold_mW)
  {
    super(id, shared);
    this.type = SNR;
    this.totalPower_mW = 0;
    this.thresholdSNR = snrThreshold_mW;
    totalPower_mW = radioInfo.shared.background_mW;
    if(totalPower_mW > radioInfo.shared.sensitivity_mW) mode = Constants.RADIO_MODE_SENSING;
  }

  /**
   * Create a new radio with additive noise model.
   *
   * @param id radio identifier
   * @param shared shared radio properties
   * @param ber bit-error-rate table
   */
  public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared, BERTable ber)
  {
    super(id, shared);
    this.type = BER;
    this.ber = ber;
    totalPower_mW = radioInfo.shared.background_mW;
    if(totalPower_mW > radioInfo.shared.sensitivity_mW) mode = Constants.RADIO_MODE_SENSING;
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Register a bit-error-rate table.
   *
   * @param ber bit-error-rate table
   */
  public void setBERTable(BERTable ber)
  {
    this.ber = ber;
  }


  //////////////////////////////////////////////////
  // reception
  //

  // RadioInterface interface
  /** {@inheritDoc} */
  public void receive(final Message msg, final Double powerObj_mW, final Long durationObj)
  {

    final double power_mW = powerObj_mW.doubleValue();
    final long duration = durationObj.longValue();
    switch(mode)
    {
      case Constants.RADIO_MODE_IDLE:
        if(power_mW >= radioInfo.shared.threshold_mW
            &&  power_mW >= totalPower_mW*thresholdSNR)
        {
            setMode(Constants.RADIO_MODE_RECEIVING);
          lockSignal(msg, power_mW, duration);

        }
        else if(totalPower_mW+power_mW > radioInfo.shared.sensitivity_mW)
        {
          setMode(Constants.RADIO_MODE_SENSING);
        }
        break;
      case Constants.RADIO_MODE_SENSING:
        if(power_mW >= radioInfo.shared.threshold_mW
            &&  power_mW >= totalPower_mW*thresholdSNR)
        {
            setMode(Constants.RADIO_MODE_RECEIVING);
            lockSignal(msg, power_mW, duration);
            

        }
        break;
      case Constants.RADIO_MODE_RECEIVING:
        if(power_mW > signalPower_mW  &&  power_mW >= totalPower_mW*thresholdSNR)
        {
          lockSignal(msg, power_mW, duration);

         // setMode(Constants.RADIO_MODE_RECEIVING);
        }
        else if(type == SNR  
            &&  signalPower_mW < (totalPower_mW-signalPower_mW+power_mW)*thresholdSNR)
        {
            setMode(Constants.RADIO_MODE_SENSING);
            unlockSignal();

        }
        break;
      case Constants.RADIO_MODE_TRANSMITTING:
        break;
      case Constants.RADIO_MODE_SLEEP:
        break;
      default:
        throw new RuntimeException("unknown radio mode");
    }
    if (TRACK_DROPS && signalBuffer!=msg && power_mW >= radioInfo.shared.threshold_mW){
    	if (stats!=null) stats.updateDrops(msg, signalBuffer, radioInfo.getUnique().getID(), mode);
    }
    // cumulative signal
    signals++;
    totalPower_mW += power_mW;
    // schedule an endReceive
    JistAPI.sleep(duration); 
    self.endReceive(powerObj_mW, new Long(seqNumber));
  } // function: receive

  private boolean important(Message msg, Message msg2) {
	
	if (important(msg)){
		return true;
	} else if (important(msg2)) return true;
	return false;
}
  private boolean important(Message msg) {
		
		if (msg instanceof MacMessage.Data){
			MacMessage.Data mmd = (Data) msg;
			if (mmd.getBody() instanceof NetMessage.Ip){
				
				if (((NetMessage.Ip)mmd.getBody()).getPayload() instanceof GPSRMessage){
					GPSRMessage gp = (GPSRMessage) ((NetMessage.Ip)mmd.getBody()).getPayload();
					if (gp.getPayload() instanceof TransUdp.UdpMessage){
						if (((NetMessage.Ip)mmd.getBody()).getNextHop().toInt() == radioInfo.unique.id) return true;
					}
				}
			}
		} 
		return false;
	}

// RadioInterface interface
  /** {@inheritDoc} */
  public void endReceive(Double powerObj_mW, Long seqNumber)
  {
      
    final double power_mW = powerObj_mW.doubleValue();
    // cumulative signal
    signals--;
    if(Main.ASSERT) Util.assertion(signals>=0);
    totalPower_mW = signals==0 
      ? radioInfo.shared.background_mW 
      : totalPower_mW-power_mW;
    switch(mode)
    {
      case Constants.RADIO_MODE_RECEIVING:
        if(JistAPI.getTime()==signalFinish)
        {
          boolean dropped = false;
          if (type == BER)
          {
             dropped = ber.shouldDrop(signalPower_mW/(totalPower_mW-signalPower_mW), 
                  8*signalBuffer.getSize());
          }
          else // use SNR calculation
          {
             //if (Util.fromDB(thresholdSNR) < totalPower_mW - radioInfo.shared.background_mW)
             
              if (signalBuffer == null || power_mW < (totalPower_mW)*thresholdSNR)
             {
                  dropped = true;

             }
          }
          
          if(!dropped)
          {
            this.macEntity.receive(signalBuffer);
          }
          else
          {
              if (stats!=null) stats.droppedInterference++;
          }
          if (signalBuffer != null && !dropped) unlockSignal();
          setMode(totalPower_mW>=radioInfo.shared.sensitivity_mW
              ? Constants.RADIO_MODE_SENSING 
              : Constants.RADIO_MODE_IDLE);
        }
        else
        {
            if (stats!=null)stats.droppedInterference++;
            if (signalFinish<JistAPI.getTime() && mode>Constants.RADIO_MODE_SENSING){
                setMode(totalPower_mW>=radioInfo.shared.sensitivity_mW
                        ? Constants.RADIO_MODE_SENSING 
                        : Constants.RADIO_MODE_IDLE);
            }
        }
        break;
      case Constants.RADIO_MODE_SENSING:
        if(totalPower_mW<radioInfo.shared.sensitivity_mW) setMode(Constants.RADIO_MODE_IDLE);
        break;
      case Constants.RADIO_MODE_TRANSMITTING:
        break;
      case Constants.RADIO_MODE_IDLE:
        break;
      case Constants.RADIO_MODE_SLEEP:
        break;
      default:
        throw new RuntimeException("unknown radio mode");
    }
  } // function: endReceive
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


} // class: RadioNoiseAdditive

