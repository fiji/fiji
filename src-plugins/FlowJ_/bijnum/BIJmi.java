package bijnum;

import java.io.*;
import ij.*;
import ij.process.*;
import java.awt.*;
import ij.gui.*;
import volume.*;
import bijnum.*;

/**
 * Mutual infromation processing  routines.
 *
 * Copyright (c) 1999-2003, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class BIJmi
{
	protected float min;
	protected float scale;
	protected int bins;
	protected int [] Pu, Pv, Puv;
	protected int n = 0;

	public BIJmi(float min, float max, float scale)
	{
		this.min = min;
		this.scale = scale;
		bins = Math.round((max - min) * scale)+1;
		Pu = new int[bins];
		Pv = new int[bins];
		Puv = new int[bins*bins];
	}
	/**
	 * Get a nice scale leading to a bin size of 128.
	 */
	public static float getNiceScale(float min, float max)
	{
	        float scale =  126f / (max - min);
		return scale;
	}
	public int [] getJointHistogram()
	{
	       return (int []) Puv.clone();
	}
	public int getBins() { return bins; }
	/**
	 * Get the current scale.
	 */
	public float getScale()
	{
		return scale;
	}
	/*
	* Compute mutual information for two short arrays a and b.
	*/
	public float mi(short [] a, short [] b)
	{
		zeroHistograms();
		n = 0;
		// Compute histograms
	        for (int i = 0; i < a.length; i++)
		{
			int ai = a[i]&0xffff;
			int bi = b[i]&0xffff;
			if (! Float.isNaN(ai) && ! Float.isNaN(bi))
                        {
				// Scale the pixel value.
				int ix0 = Math.round((a[i] - min) * scale);
				int ix1 = Math.round((b[i] - min) * scale);
				if (ix0 >= 0 && ix1 >= 0)
				{
					//if (ix0 > bins || ix1 >= bins)
					//     IJ.write("ix0 "+ix0+" ix1 "+ix1+"bins "+bins+" min "+min+ " scale "+scale);
					Pu[ix0]++;
					Pv[ix1]++;
					Puv[ix0*bins+ix1]++;
					n++;
				}
                        }
		}
		return computeProbs(n);
	}
        protected void zeroHistograms()
	{
		for (int i = 0; i < Pu.length; i++)
		{ Pu[i] = 0; Pv[i] = 0; }
		for (int j = 0; j < Puv.length; j++)
			Puv[j] = 0;
	}
	/*
	* Compute -mutual information for two float arrays a and b.
	* @param a a float[] array
	* @param b a float[] array
	*/
	public float mi(float [] a, float [] b)
	{
		zeroHistograms();
		n = 0;
		// Compute histograms
	        for (int i = 0; i < a.length; i++)
		{
			float ai = a[i];
			float bi = b[i];
			if (Float.isNaN(ai) && Float.isNaN(bi))
                        {
				// Normalize and scale the pixel value.
				int ix0 = Math.round((a[i] - min) * scale);
				int ix1 = Math.round((b[i] - min) * scale);
				if (ix0 >= Pu.length || ix0 < 0 || ix1 >= Pv.length || ix1 < 0)
                                {
	                               IJ.error("array index out of bounds: ix0="+ix0+" ix1= "+ix1);
                                       return Float.NaN;
                                }
                                if (ix0 >= 0 && ix1 >= 0)
				{
					Pu[ix0]++;
					Pv[ix1]++;
					Puv[ix0*bins+ix1]++;
					n++;
				}
                        }
		}
		//IJ.write("min = "+min+"scale="+scale);
		return computeProbs(n);
	}
	/*
	* Compute probabilities and Mutual Information from joint histogram.
	* Uses n, Pv, Pu, Puv.
	* -MI = -H(U) - H(U|V)
	* p(u,v) = H(gi, gj) / N, p(u) = H(gi)/N and p(v) = H(gj)/N
	* -MI = - SUM SUM 0 iff H(gi, gj) =0, else H(gi, gj)/N ln (N H(gi, gj)/ H(gi) H(gj))
	*/
	protected float computeProbs(int n)
	{
		float Cmi = 0;
		// Compute probabilities
		for (int k = 0; k < bins; k++)
		for (int l = 0; l < bins; l++)
		{
		      float Pkl = Puv[k*bins+l];
		      if (Pkl > 0)
			    Cmi -= Pkl / n * Math.log(n * Pkl / (Pu[k] * Pv[l]));
		}
		return Cmi;
	}
}
