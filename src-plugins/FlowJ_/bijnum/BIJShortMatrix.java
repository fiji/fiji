package bijnum;
import java.awt.*;
import ij.*;

/**
 * Implements methods for large short matrices.
 * Simplified subset of BIJmatrix.
 * Syntax is kept similar to Matlab, including order of operations.
 *
 * Copyright (c) 1999-2003, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * This source code, and any derived programs ('the software')
 * are the intellectual property of Michael Abramoff.
 * Michael Abramoff asserts his right as the sole owner of the rights
 * to this software.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class BIJShortMatrix
{
	/*
	* Compute minimum and maximum of a vector.
	* @param v a short[] matrix.
	* @return a float[] with the minimum in [0] and the maximum in [1].
	*/
	public static float [] minmax(short [] v)
	{
		float [] minmax = new float[2];
		minmax[0] = Short.MAX_VALUE;
		minmax[1] = -Short.MAX_VALUE;
		for (int j = 0; j < v.length; j++)
		{
			float val = v[j] & 0xffff;
			if (val < minmax[0])
	                        minmax[0] = val;
			else if (val > minmax[1])
	                        minmax[1] = val;
		}
		return minmax;
	}
	/*
	* Compute minimum and maximum of a matrix.
	*
	* @param m a float[][] matrix.
	* @return a float[] with the minimum in [0] and the maximum in [1].
	*/
	public static float [] minmax(short [][] m)
	{
		int iN = m.length; int iM = m[0].length;
		float [] minmax = new float[2];
		minmax[0] = Short.MAX_VALUE;
                minmax[1] = -Short.MAX_VALUE;
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		{
			float val = m[j][i] & 0xffff;
			if (val < minmax[0])
	                        minmax[0] = val;
			else if (val > minmax[1])
	                        minmax[1] = val;
		}
		return minmax;
	}
	/*
	* Compute the mean of a vector v.
	* @param m a short[] vector.
	* @return the mean of all elements in v.
	*/
	public static float mean(short [] v)
	{
		double cum = 0;
		int iN = v.length;
		for (int j = 0; j < iN; j++)
		       cum +=(double) (v[j]&0xffff);
		return (float) (cum / iN);
	}
	/**
	* Copy a vector.
	* @param v a float[] vector.
	* @return a copy of v.
	*/
	public static short [] copy(short [] v)
	{
		int iN = v.length;
		short [] nv = new short[iN];
		for (int i = 0; i < iN; i++)
		       nv[i] = v[i];
		return nv;
	}
	/**
	* Add a scalar to all entries in a matrix.
	* @param m a float[][] matrix.
	* @param scalar a scalar to subtract.
	* @return the subtracted matrix.
	*/
	public static short[] add(short [] v, float scalar)
	{
		int iN = v.length;
		for (int j = 0; j < iN; j++)
		       v[j] = (short) Math.round(((float) (v[j]&0xffff)) + scalar);
		return v;
	}
	public static String toString(short [] v)
	{
		StringBuffer sb = new StringBuffer("Vector (");
		int iN = v.length;
		sb.append(iN);
		sb.append("):\n");
		for (int j = 0; j < iN; j++)
		{
			sb.append(v[j]);
			sb.append("\t");
		}
		return sb.toString();
	}
}

