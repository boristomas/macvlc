//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Field.java Tue 2004/04/06 11:30:54 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.field;

import jist.swans.radio.RadioInterface;
import jist.swans.radio.RadioInfo;
import jist.swans.misc.Message;
import jist.swans.misc.Location;
import jist.swans.misc.Util;
import jist.swans.Constants;
import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

/** 
 * An abstract parent of Field implementations, which contains
 * the common code.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Field.java,v 1.1 2007/04/09 18:49:27 drchoffnes Exp $
 * @since SWANS1.0
 */

public class Field implements FieldInterface
{

  /** logger for field events. */
  public static final Logger logField = Logger.getLogger(Field.class.getName());

  //////////////////////////////////////////////////
  // locals
  //

  /**
   * Propagation limit (in dBm). This is the signal strength threshold.
   */
  protected double limit;

  /**
   * Pathloss model.
   */
  protected PathLoss pathloss;

  /**
   * Fading model.
   */
  protected Fading fading;

  /**
   * Mobility model.
   */
  protected Mobility mobility;

  /**
   * Spatial data structure.
   */
  protected Spatial spatial;

  /**
   * Self-referencing field entity.
   */
  protected FieldInterface self;

  /**
   * Array of radios on this field.
   */
  protected static RadioData[] radios;


  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Initialize new, empty field with default fading (FadingNone) and pathloss
   * (PathLossFreeSpace) models, and default propagation limits.
   *
   * @param bounds spatial limits of field
   */
  public Field(Location bounds)
  {
    this(bounds, false);
  }

  /**
   * Initialize new, empty field with default fading (FadingNone) and pathloss
   * (PathLossFreeSpace) models, and default propagation limits, possibly
   * with wrapped edges.
   *
   * @param bounds spatial limits of field
   * @param wrap whether to wrap field edges
   */
  public Field(Location bounds, boolean wrap)
  {
    this(wrap 
          ? (Spatial)new Spatial.TiledWraparound(new Spatial.HierGrid(bounds, 5)) 
          : (Spatial)new Spatial.HierGrid(bounds, 5),
        new Fading.None(), 
        new PathLoss.FreeSpace(), 
        new Mobility.Static(),
        Constants.PROPAGATION_LIMIT_DEFAULT);
  }

  /**
   * Initialize new, empty field with given fading and pathloss models, using default
   * propagation limits.
   *
   * @param spatial binning model
   * @param fading fading model
   * @param pathloss pathloss model
   * @param mobility mobility model
   * @param propagationLimit minimum signal threshold (in dBm)
   */
  public Field(Spatial spatial, Fading fading, PathLoss pathloss, 
      Mobility mobility, double propagationLimit)
  {
    radios = new RadioData[10];
    this.spatial = spatial;
    setFading(fading);
    setPathLoss(pathloss);
    setMobility(mobility);
    setPropagationLimit(propagationLimit);
    this.self = (FieldInterface)JistAPI.proxy(this, FieldInterface.class);
  }


  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Set field fading model.
   *
   * @param fading fading model
   */
  public void setFading(Fading fading)
  {
    this.fading = fading;
  }
  
  /**
   * Set field pathloss model.
   *
   * @param pathloss pathloss model
   */
  public void setPathLoss(PathLoss pathloss)
  {
    this.pathloss = pathloss;
  }

  /**
   * Set mobility model.
   *
   * @param mobility mobility model
   */
  public void setMobility(Mobility mobility)
  {
    this.mobility = mobility;
  }

  /**
   * Set signal propagation threshold.
   *
   * @param limit minimum signal threshold (in dBm)
   */
  public void setPropagationLimit(double limit)
  {
    this.limit = limit;
  }


  //////////////////////////////////////////////////
  // entity hookups
  //

  /**
   * Return the proxy entity of this field.
   *
   * @return proxy entity of this field
   */
  public FieldInterface getProxy()
  {
    return this.self;
  }


  //////////////////////////////////////////////////
  // radio management
  //

  /** 
   * Radio information stored by the Field entity. Includes
   * identifier, location, and a radio entity reference for
   * upcalls in doubly linked list.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class RadioData
  {
    /** 
     * radio entity reference for upcalls.
     */
    protected RadioInterface entity;

    /**
     * timeless radio properties.
     */
    protected RadioInfo info;

    /**
     * radio location.
     */
    protected Location loc;

    /**
     * mobility information.
     */
    protected Mobility.MobilityInfo mobilityInfo;

    /**
     * linked list pointers.
     */
    protected RadioData prev, next;
    
    public Location getLocation()
    {
        return loc;
    }
    
    /**
     * return mobility info, accessed by higher layers for routing use
    */
    public Mobility.MobilityInfo getMobilityInfo()
    {
        return mobilityInfo;
    }

  } // class: RadioData


  /**
   * Add a radio onto the field.
   *
   * @param info radio properties (includes radio identifier)
   * @param entity radio upcall entity reference
   * @param loc radio location
   */
  public void addRadio(RadioInfo info, RadioInterface entity, Location loc)
  {
    if(!JistAPI.isEntity(entity)) throw new IllegalArgumentException("entity expected");
    if(logField.isInfoEnabled())
    {
      logField.info("add radio: info=["+info+"] loc="+loc);
    }
    RadioData data = new RadioData();
    Integer id = info.getUnique().getID();
    data.entity = entity;
    data.info = info;
    data.loc = loc;
    // insert into array
    int idi = id.intValue();
    while(radios.length<=idi)
    {
      RadioData[] radios2 = new RadioData[radios.length*2];
      System.arraycopy(radios, 0, radios2, 0, radios.length);
      radios = radios2;
    }
    radios[idi] = data;
    // add into spatial data structure
    spatial.add(data);
  }

  /**
   * Remove a radio from the field.
   *
   * @param id radio identifier
   */
  public void delRadio(Integer id)
  {
    if(logField.isInfoEnabled())
    {
      logField.info("delete radio: id="+id);
    }
    // remove from array
    RadioData data = getRadioData(id);
    radios[id.intValue()] = null;
    // remove from spatial data structure
    spatial.del(data);
  }

  /**
   * Return radio properties.
   *
   * @param id radio identifier
   * @return radio properties
   */
  public static RadioData getRadioData(Integer id)
  {
    return radios[id.intValue()];
  }

  //////////////////////////////////////////////////
  // FieldInterface implementation
  //

  /** {@inheritDoc} */
  public void moveRadio(Integer id, Location loc)
  {
    if(logField.isInfoEnabled())
    {
      logField.info("move radio id="+id+" to="+loc);
    }
    // update spatial data structure
    RadioData rd = getRadioData(id);
   // rd.loc = loc;
    spatial.moveInside(rd, loc);
 //   System.out.println("BiT: nodeid= " + rd.info.getUnique().getID().toString() + " old loc = " + rd.loc.toString() + " new loc = " + loc.toString() + " -320");
    // schedule next step
    if(rd.mobilityInfo!=null)
    {
      mobility.next(self, id, loc, rd.mobilityInfo);
      //System.out.println("Node ID from Field.java: " + id);
    }
  }

  /** {@inheritDoc} */
  public void moveRadioOff(Integer id, Location delta)
  {
    Location newLoc = getRadioData(id).loc.getClone();
    newLoc.add(delta);
    moveRadio(id, newLoc);
  }

  /**
   * Start mobility; schedule first mobility event.
   *
   * @param id radio identifier
   */
  public void startMobility(Integer id)
  {
    RadioData rd = getRadioData(id);
    rd.mobilityInfo = mobility.init(self, id, rd.loc);
    self.moveRadio(id, rd.loc);
  }


  //////////////////////////////////////////////////
  // communication
  //

  /**
   * Transmission visitor object.
   */
  private Spatial.SpatialTransmitVisitor transmitVisitor = new Spatial.SpatialTransmitVisitor()
  {
    public double computeSignal(RadioInfo srcInfo, Location srcLoc, Location dstLoc)
    {
      double loss = pathloss.compute(srcInfo, srcLoc, srcInfo, dstLoc);
      double fade = fading.compute();
      return srcInfo.getShared().getPower() - loss + fade;
    }
    
    //transmit packet to given location
    public void visitTransmit(RadioInfo srcInfo, Location srcLoc, RadioInfo dstInfo, RadioInterface dstEntity, Location dstLoc, Message msg, Long durationObj)
    {
      if(srcInfo.getUnique().getID()==dstInfo.getUnique().getID()) return;
      // compute signal strength
      double loss = pathloss.compute(srcInfo, srcLoc, dstInfo, dstLoc);
      double fade = fading.compute();
      double dstPower = srcInfo.getShared().getPower() - loss + fade;
      // additional cuttoffs
      double dstPower_mW = Util.fromDB(dstPower);
      //if(dstPower_mW < dstInfo.getShared().getBackground_mW()) return;
      if(dstPower_mW < dstInfo.getShared().getSensitivity_mW()) return;
      
      //compute the direction and points of the radar
      RadioData rd = getRadioData(srcInfo.getUnique().getID());
      Location currNodeBearing = rd.mobilityInfo.getBearing();									//using the radio data to get the bearing for the currnet node
      

      
      dstEntity.receive(msg, new Double(dstPower_mW), durationObj);
    }
  };

  // FieldInterface interface
  /** {@inheritDoc} */
  public void transmit(RadioInfo srcInfo, Message msg, long duration)
  {
    RadioData srcData = getRadioData(srcInfo.getUnique().getID());
    spatial.visitTransmit(transmitVisitor, srcData.info, srcData.loc, msg, new Long(duration), limit);
  }


  //////////////////////////////////////////////////
  // compute density metrics
  //

  /**
   * Compute field density.
   *
   * @return field node density
   */
  public double computeDensity()
  {
    return (double)spatial.size / spatial.area();
  }

  /**
   * Connectivity visitor interface.
   */
  public static interface ConnectivityVisitor 
      extends Spatial.SpatialTransmitVisitor, Spatial.SpatialVisitor
  {
    /**
     * Return average number of links (connectivity).
     *
     * @return average number of links
     */
    double getAvgLinks();
  }

  /**
   * Compute field connectivity.
   *
   * @param sense whether to use radio sensing or reception signal strength
   * @return field connectivity
   */
  public double computeAvgConnectivity(final boolean sense)
  {
    // create new connectivity visitor
    ConnectivityVisitor connectivityVisitor = new ConnectivityVisitor()
    {
      private long links = 0, nodes = 0;
      public double getAvgLinks()
      {
        return (double)links/(double)nodes;
      }
      public double computeSignal(RadioInfo srcInfo, Location srcLoc, Location dstLoc)
      {
        return transmitVisitor.computeSignal(srcInfo, srcLoc, dstLoc);
      }
      public void visitTransmit(RadioInfo srcInfo, Location srcLoc, 
          RadioInfo dstInfo, RadioInterface dstEntity, Location dstLoc,
          Message msg, Long durationObj)
      {
        if(srcInfo.getUnique().getID()==dstInfo.getUnique().getID()) return;
        // compute signal strength
        double loss = pathloss.compute(srcInfo, srcLoc, dstInfo, dstLoc);
        double fade = fading.compute();
        double dstPower = srcInfo.getShared().getPower() - loss + fade;
        // additional cuttoffs
        double dstPower_mW = Util.fromDB(dstPower);
        // if(dstPower_mW < shared.getBackground_mW()) return;
        if(dstPower_mW < (sense
            ? dstInfo.getShared().getSensitivity_mW()
            : dstInfo.getShared().getThreshold_mW())) return;
        links++;
      }
      public void visit(Field.RadioData dst)
      {
        spatial.visitTransmit(this, dst.info, dst.loc, null, null, limit);
        nodes++;
      }
    };
    spatial.visit(connectivityVisitor); 
    return connectivityVisitor.getAvgLinks();
  }

} // class: Field

