//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <PathLoss.java Wed 2004/06/23 09:18:00 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.field;

import java.util.Random;

import driver.JistExperiment;
import bsh.Console;
import jist.swans.Constants;
import jist.swans.misc.Location;
import jist.swans.misc.Util;
import jist.swans.radio.RadioInfo;

/** 
 * Interface for performing pathloss calculations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: PathLoss.java,v 1.1 2007/04/09 18:49:28 drchoffnes Exp $
 * @since SWANS1.0
 */

public interface PathLoss
{

	//////////////////////////////////////////////////
	// interface
	//

	/**
	 * Compute the path loss.
	 *
	 * @param srcRadio source radio information
	 * @param srcLocation source location
	 * @param dstRadio destination radio information
	 * @param dstLocation destination location
	 * @return path loss (units: dB)
	 */
	double compute(RadioInfo srcRadio, Location srcLocation, 
			RadioInfo dstRadio, Location dstLocation);


	//////////////////////////////////////////////////
	// implementations
	//

	/** 
	 * Computes path loss for VLC communication
	 *
	 * @author Boris Tomas boris.tomas@foi.hr;
	 * @since 
	 */
	final class VLCLink implements PathLoss
	{
		//ref: Fundamental Analysis for VLC System using LED Lights.pdf

		// PathLoss interface
		/** {@inheritDoc} */
		public double compute(RadioInfo srcRadio, Location srcLocation, RadioInfo dstRadio, Location dstLocation)
		{			
			double m= 2;
			double n= 1.5;
			double psiC= JistExperiment.getJistExperiment().getVLCvisionAngleRx();
			double txPwr= srcRadio.getShared().getPower();
			double A = 0.0001;
			double Dd= srcLocation.distance(dstLocation);
			double fiAngle = 0;
			double psiAngle = 0;
			double H0 = 0;
			double rxPwr;

	//		txPwr= 1;


			
			//	rxPwr= H0*txPwr;
			if (psiAngle>psiC)
			{	
				H0 = 0;
			}
			else
			{
				H0 = (((m+1)*A)/(2*Math.PI*Dd*Dd)) * ( Math.pow(Math.cos(fiAngle),m) * Ts(psiAngle) * g(psiAngle, psiC, n)* Math.cos(psiAngle));
			}
			return H0 * txPwr;
			//double vrijhe =Util.fromDB(-57);
			//double vrije=H0/(Ts(psiAngle)*g(psiAngle, psiC,n));// rxPwr;

		//	System.out.println("PL: db:" + vrijhe+ " H0: "+ H0 + " fin: "+ vrije);
			//return vrije;
			/*
			double dist = srcLocation.distance(dstLocation);


			double pathloss = - srcRadio.getShared().getGain() - dstRadio.getShared().getGain();
			double valueForLog = 4.0 * Math.PI * dist / srcRadio.getShared().getWaveLength();
			if (valueForLog > 1.0)
			{
				pathloss += Util.log((float)valueForLog) / Constants.log10 * 20.0;
			}
			return pathloss;*/
		}
		private double Ts(double psiAngle)
		{
			return 1;
		}
		private double g(double psiAngle, double psiC, double n)
		{

			if (0<=psiAngle && psiAngle<=psiC)
			{
				return (n*n)/(Math.pow(Math.sin(psiC),2));
			}
			else
			{
				return  0;
			}

		}


	} // class: VLCLink


	/** 
	 * Computes free-space path loss. Equivalent to GloMoSim code.
	 *
	 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
	 * @since SWANS1.0
	 */
	final class FreeSpace implements PathLoss
	{
		// PathLoss interface
		/** {@inheritDoc} */
		public double compute(RadioInfo srcRadio, Location srcLocation, 
				RadioInfo dstRadio, Location dstLocation)
		{
			double dist = srcLocation.distance(dstLocation);
			double pathloss = - srcRadio.getShared().getGain() - dstRadio.getShared().getGain();
			double valueForLog = 4.0 * Math.PI * dist / srcRadio.getShared().getWaveLength();
			if (valueForLog > 1.0)
			{
				pathloss += Util.log((float)valueForLog) / Constants.log10 * 20.0;
			}
			return pathloss;
		}

	} // class: FreeSpace

	/** 
	 * Computes two-ray path loss. Equivalent to GloMoSim code.
	 *
	 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
	 * @since SWANS1.0
	 */
	final class TwoRay implements PathLoss
	{
		// PathLoss interface
		/** {@inheritDoc} */
		public double compute(RadioInfo srcRadio, Location srcLocation, 
				RadioInfo dstRadio, Location dstLocation)
		{
			double dist = srcLocation.distance(dstLocation);
			double pathloss = - srcRadio.getShared().getGain() - dstRadio.getShared().getGain();
			double planeEarthLoss = (dist * dist) / 
					(srcLocation.getHeight() * dstLocation.getHeight());
			double freeSpaceLoss = 4.0 * Math.PI * dist / srcRadio.getShared().getWaveLength();
			if (planeEarthLoss > freeSpaceLoss)
			{
				if (planeEarthLoss > 1.0)
				{
					pathloss += 20.0 * Math.log(planeEarthLoss) / Constants.log10;
				}
			}
			else
			{
				if (freeSpaceLoss > 1.0)
				{
					pathloss += 20.0 * Math.log(freeSpaceLoss) / Constants.log10;
				}
			}
			return pathloss;
		}
	} // class: TwoRay

	/** 
	 * Computes path loss with shadowing.
	 *
	 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
	 * @since SWANS1.x
	 */
	final class Shadowing implements PathLoss
	{
		final double exp;
		final double stdDeviation;
		int parity = 0;
		double nextresult;
		Random rnd = new Random();

		/**
		 * Shadowing constructor 
		 * @param exp exponent for pathloss equation
		 * @param stdDeviation standard deviation for gaussian lognormal variable (whatever that is)
		 */
		public Shadowing( double exp, double stdDeviation )
		{
			this.exp = exp;
			this.stdDeviation = stdDeviation;
		}

		// PathLoss interface
		/** {@inheritDoc} */
		public double compute(RadioInfo srcRadio, Location srcLocation, 
				RadioInfo dstRadio, Location dstLocation)
		{
			float dist = srcLocation.distance(dstLocation);

			float reference = 1.0f;

			double intercept = 20 * Util.log((float)((4*Math.PI*reference)/
					srcRadio.getShared().getWaveLength()))/Constants.log10;

			if (dist <= reference)
			{
				return - srcRadio.getShared().getGain() - dstRadio.getShared().getGain();
			}

			double pathloss = intercept + 10.0 * exp * Util.log(dist/reference)/Constants.log10 + 
					Constants.random.nextGaussian()*stdDeviation;

			return pathloss;


		}

		/**
		 * 
		 * @param radioInfo
		 * @param sensitivity (dBm)
		 * @param stdDev (dBm)
		 * @return
		 */
		public double computeMaxDistance(RadioInfo.RadioInfoShared radioInfo, double sensitivity, 
				double stdDev)
		{

			double power = radioInfo.getPower() - sensitivity;
			double intercept = 20 * Util.log((float)((4*Math.PI)/
					radioInfo.getWaveLength()))/Constants.log10;

			double num = (power-intercept+stdDev)/(10*exp);


			double dist = Math.pow(10, num);


			return dist;
		}

		/**
		 * Calculates a zero-mean random variable.
		 * Ripped from NS-2.
		 * 
		 * @param stdDeviation Standard deviation for this calculation
		 * @return random value
		 */
		double normal(double stdDeviation)
		{
			double sam1;
			double sam2;
			double rad;

			if (stdDeviation == 0) return 0;
			if (parity == 0) {
				sam1 = 2 * rnd.nextDouble() - 1;
				sam2 = 2 * rnd.nextDouble() - 1;;
				while ((rad = sam1*sam1 + sam2*sam2) >= 1) {
					sam1 = 2*rnd.nextDouble() - 1;
					sam2 = 2*rnd.nextDouble() - 1;
				}
				rad = Math.sqrt((-2*Math.log(rad))/rad);
				nextresult = sam2 * rad;
				parity = 1;
				return (sam1 * rad * stdDeviation);
			}
			else {
				parity = 0;
				return (nextresult * stdDeviation);
			}
		}

		/**
		 *  Friis free space pathloss equation, taken from NS-2.
		 * 
		 * @param Pt transmission power
		 * @param Gt transmitter gain
		 * @param Gr receiver gain
		 * @param lambda wavelength
		 * @param L system loss
		 * @param d distance between two nodes
		 */
		private double Friis(double Pt, double Gt, double Gr, double lambda, double L, double d)
		{
			/*
			 * Friis free space equation:
			 *
			 *       Pt * Gt * Gr * (lambda^2)
			 *   P = --------------------------
			 *       (4 * pi * d)^2 * L
			 */
			double M = lambda / (4 * Math.PI * d);
			return (Pt * Gt * Gr * (M * M)) / L;
		}

		private double rayleighDist(double sigma)
		{
			return (sigma/Math.sqrt(Math.PI/2)) * Math.sqrt(-2.0 * Math.log((float)rnd.nextDouble()));
		}


	} // end class Shadowing


	// todo: MITRE's pathloss format
	// Time (nearest whole second)    Node A     Node B     Path Loss (dB) Range (meters)
	// End of file is indicated by a -1 in the first column.  (And nothing else on the line.)  

} // class: PathLoss

