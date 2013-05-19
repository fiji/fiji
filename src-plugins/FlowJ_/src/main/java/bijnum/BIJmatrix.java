package bijnum;
import java.awt.*;

import ij.*;

/**
 * Implements methods for large (by necessity, float) matrices, including covariance of matrix,
 * eigenvectors and others.
 * Syntax is kept similar to Matlab, including order of operations.
 * The basic difference with BIJMatrix is that here the matrices are all float, the syntax
 * is the smae as Matlab and it is active. I do not want to touch BIJMatrix anymore.
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
public class BIJmatrix
{
        /**
	* Compute the matrix product of m and n.
	* @param a a matrix of float[N][M]
	* @param b a matrix of float[M][N]
	* @return a new matrix of float[N][M]
	*/
	public static float [][] mul(float [][] a, float [][] b)
        {
                float [][] n = new float[a.length][b[0].length];
                mul(n, a, b, false);
                return n;
        }
        /**
        * Compute the matrix product of m and n. Show progress.
        * @param a a matrix of float[N][M]
        * @param b a matrix of float[M][N]
        * @return a matrix of float[N][N]
        */
        public static float [][] mul(float [][] a, float [][] b, boolean doShowProgress)
        {
                float [][] n = new float[a.length][b[0].length];
                mul(n, a, b, doShowProgress);
                return n;
        }
        /**
        * Compute the matrix product of m and n into r. Show progress.
        * @param r a matrix of float[N][N]
        * @param a a matrix of float[N][M]
        * @param b a matrix of float[M][N]
        * @return a matrix of float[N][N]
        */
        public static void mul(float [][] m, float [][] a, float [][] b, boolean doShowProgress)
        {
                int iN = a.length; int iM = b[0].length;
                for (int j = 0; j < iN; j++)
                {
                        for (int i = 0; i < iM; i++)
                        {
                                float r = 0;
                                for (int k = 0; k < b.length; k++)
                                         r += a[j][k] * b[k][i];
                                m[j][i] = r;
                        }
                        if (doShowProgress)
                                IJ.showProgress(j, iN);
                }
        }
	/**
         * Compute the outer product of a matrix and a vector v
         * @param a a float[][] matrix
         * @param v a float[] vector.
         * @return a float[] vector  of the same size as v.
         * Preferably use mul(3) instead.
        */
	public static float [] mul(float [][] a, float [] v)
	throws IllegalArgumentException
        {
                if (a[0].length != v.length)
	               throw new IllegalArgumentException("mul dimensions do not match: "+a.length+"x"+a[0].length+" "+v.length);
		float [] n = new float[a.length];
                for (int i = 0; i < n.length; i++)
                for (int k = 0; k < v.length; k++)
                        n[i] += a[i][k]*v[k];
                return n;
        }
        /**
         * Multiply all elements in vector a by elements in b and put in r.
         * @param r a double[] vector
         * @param a a double[] vector
         * @param b a double[] vector
        */
        public static void mulElements(double [] r, double [] a, double [] b)
        {
                for (int i = 0; i < a.length; i++)
                        r[i] = a[i]*b[i];
        }
        /**
         * Multiply all elements in vector a by elements in b and put in r.
         * @param r a float[] vector
         * @param a a float[] vector
         * @param b a float[] vector
        */
        public static void mulElements(float [] r, float [] a, float [] b)
        {
                for (int i = 0; i < a.length; i++)
                        r[i] = a[i]*b[i];
        }
        /**
         * Multiply all elements in vector a by elements in b.
         * @param a a float[] vector
         * @param b a float[] vector
         * @return a float[] vector
        */
        public static float [] mulElements(float [] a, float [] b)
        {
                float [] n = new float[a.length];
                for (int i = 0; i < a.length; i++)
                        n[i] = a[i]*b[i];
                return n;
        }
        /**
         * Divide all elements in vector a by elements in b.
         * @param r a float[] vector for the result.
         * @param a a float[] vector
         * @param b a float[] vector
       */
        public static void divElements(float [] r, float [] a, float [] b)
        {
                for (int i = 0; i < a.length; i++)
                        r[i] = a[i]/b[i];
        }
        /**
         * Compute outer product of vector a and b.
         * @param a a float[] vector.
         * @param b a float[] vector.
         * @return a float[] vector
        */
        public static float [][] mulOuter(float [] a, float [] b)
        {
                if (a.length != b.length)
                       throw new IllegalArgumentException("mul dimensions do not match: "+a.length+"!="+b.length);
                float [][] n = new float[a.length][a.length];
                for (int j = 0; j < n.length; j++)
                for (int i = 0; i < n[0].length; i++)
                        n[j][i] = a[j] * b[i];
                return n;
        }
        /**
        * Compute the <bold>transpose</bold> of the outer product of the <bold>tranpose</bold> of a with b.
        * Is the same as transpose(mul(transpose(a), b)), but saves a lot of space!
        * @param a a matrix of float[M][N]
        * @param b a matrix of float[M][N]
        * @return a matrix of float[M][M]
        */
        public static float [][] mulT(float [][] a, float [][] b, boolean doShowProgress)
        {
                int iN = a[0].length; int iM = b[0].length;
                float [][] n = new float[iM][iN];
                for (int j = 0; j < iN; j++)
                {
                        for (int i = 0; i < iM; i++)
                        {
                                float r = 0;
                                for (int k = 0; k < b.length; k++)
                                         r += a[k][j] * b[k][i];
                                n[i][j] = r;
                        }
                        if (doShowProgress)
                                IJ.showProgress(j, iN);
                }
                return n;
        }
        /**
        * Multiply each element in v by a scalar.
        * @param v a float[] vector.
        * @param scalar the value to multiply by.
        * @return the resulting vector.
        * @deprecated
        */
        public static float [] mul(float [] v, double scalar)
        {
                int iN = v.length;
                float [] n = new float[iN];
                for (int i = 0; i < iN; i++)
                       n[i] = (float) (v[i] * scalar);
                return n;
        }
        /**
        * Multiply each element in v by a scalar and put in r.
        * @param r a float[] vector for the result
        * @param v a float[] vector.
        * @param scalar the value to multiply by.
        */
        public static void mulElements(float [] r, float [] v, double scalar)
        {
                for (int i = 0; i < v.length; i++)
                       r[i] = (float) (v[i] * scalar);
        }
        /**
        * Multiply each element in v by a scalar and put in r.
        * @param r a float[] vector for the result
        * @param v a float[] vector.
        * @param scalar the value to multiply by.
        */
        public static void mulElements(double [] r, double [] v, double scalar)
        {
                for (int i = 0; i < v.length; i++)
                       r[i] = v[i] * scalar;
        }
        /**
        * Multiply each element in matrix m by a scalar and put result in r.
        * @param r a float[][] matrix that will receive the result.
        * @param m a float[][] matrix.
        * @param scalar the value to multiply by
        */
        public static void mul(float [][] r, float [][] m, float scalar)
        {
                int iN = m.length; int iM = m[0].length;
                for (int j = 0; j < iN; j++)
                for (int i = 0; i < iM; i++)
                       r[j][i] = m[j][i] * scalar;
        }
        /**
         * Compute the outer product of a matrix and a vector v and put the result in r.
         * @param a a float[][] matrix
         * @param v a float[] vector.
         * @return a float[] vector  of the same size as v.
        */
        public static void mul(float [] r, float [][] a, float [] v)
        throws IllegalArgumentException
        {
                if (a[0].length != v.length)
                       throw new IllegalArgumentException("mul dimensions do not match: "+a.length+"x"+a[0].length+" "+v.length);
                for (int i = 0; i < a.length; i++)
                for (int k = 0; k < v.length; k++)
                        r[i] += a[i][k]*v[k];
        }
        /**
	* Flatten a float[][] matrix into a float[] matrix of rows each of m[0].width.
	* @param m the matrix
	* @return the flattened matrix, a float[m.length*m[0].length].
	*/
	public static float [] flatten(float [][] m)
        {
		int iN = m.length; int iM = m[0].length;
		float [] n = new float[iN*iM];
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
			n[j*iM+i] = m[j][i];
		return n;
        }
        public static boolean containsNaN(float [] v)
        {
                for (int j = 0; j < v.length; j++)
                        if (Float.isNaN(v[j]))
                                return true;
                return false;
        }
        public static boolean containsNaN(float [][] m)
        {
                for (int j = 0; j < m.length; j++)
                for (int i = 0; i < m[0].length; i++)
                        if (Float.isNaN(m[j][i]))
                                return true;
                return false;
        }
        /**
        * Compute the transpose of a matrix m.
        * @param m a float[][] matrix.
        * @return a float[][] which is the transpose of m.
        */
        public static float [][] transpose(float [][] m)
        {
                float [][] n = new float[m[0].length][m.length];
                for (int j = 0; j < m.length; j++)
                for (int i = 0; i < m[0].length; i++)
                       n[i][j] = m[j][i];
                return n;
        }
        /**
        * Compute the transpose of a matrix m.
        * @param m a byte[][] matrix.
        * @return a byte[][] which is the transpose of m.
        */
        public static byte [][] transpose(byte [][] m)
        {
                byte [][] n = new byte[m[0].length][m.length];
                for (int j = 0; j < m.length; j++)
                for (int i = 0; i < m[0].length; i++)
                       n[i][j] = m[j][i];
                return n;
        }
	/*
	* Compute minimum and maximum of a matrix.
	*
	* @param m a float[][] matrix.
	* @return a float[] with the minimum in [0] and the maximum in [1].
	*/
	public static float [] minmax(float [][] m)
	{
		int iN = m.length; int iM = m[0].length;
		float [] minmax = new float[2];
		minmax[0] = Float.MAX_VALUE;
		minmax[1] = -Float.MAX_VALUE;
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		{
			float val = m[j][i];
			if (val != Float.NaN)
	                {
				if (val < minmax[0])
	                                  minmax[0] = val;
                                else if (val > minmax[1])
	                                  minmax[1] = val;
	                }
		}
		return minmax;
	}
	/*
	* Compute minimum and maximum of a vector.
        * Exclude elements than are NaN.
	* @param v a float[] matrix.
	* @return a float[] with the minimum in [0] and the maximum in [1].
	*/
	public static float [] minmax(float [] v)
	{
		float [] minmax = new float[2];
		minmax[0] = Float.MAX_VALUE;
		minmax[1] = -Float.MAX_VALUE;
		for (int j = 0; j < v.length; j++)
		{
			float val = v[j];
			if (! Float.isNaN(val))
	                {
				if (val < minmax[0])
	                                  minmax[0] = val;
                                else if (val > minmax[1])
	                                  minmax[1] = val;
	                }
                        else
                                ;//System.out.println("minmax found NaN at "+j);
		}
		return minmax;
	}
	/*
	* Compute minimum and maximum of a vector.
	* Exclude the elements that are 0 from analysis.
	* @param v a float[] matrix.
	* @return a float[] with the minimum in [0] and the maximum in [1].
        * @deprecated
	*/
	public static float [] minmaxNot0(float [] v)
	{
		float [] minmax = new float[2];
		minmax[0] = Float.MAX_VALUE;
		minmax[1] = -Float.MAX_VALUE;
		for (int j = 0; j < v.length; j++)
		{
			float val = v[j];
			if (val != 0)
	                {
				if (val < minmax[0])
	                                  minmax[0] = val;
                                else if (val > minmax[1])
	                                  minmax[1] = val;
	                }
		}
		return minmax;
	}
	/*
	* Compute minimum and maximum of a matrix.
	* Exclude elements that are 0.
	*
	* @param m a float[][] matrix.
	* @return a float[] with the minimum in [0] and the maximum in [1].
        * @deprecated
	*/
	public static float [] minmaxNot0(float [][] m)
	{
		int iN = m.length; int iM = m[0].length;
		float [] minmax = new float[2];
		minmax[0] = Float.MAX_VALUE;
		minmax[1] = -Float.MAX_VALUE;
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		{
			float val = m[j][i];
			if (val != 0)
	                {
				if (val < minmax[0])
	                                  minmax[0] = val;
                                else if (val > minmax[1])
	                                  minmax[1] = val;
	                }
		}
		return minmax;
	}
	/**
	 * Return a vector with the absolutes of all elements in vector v.
	 * v is not modified.
	 * @param a float[] vector
	 * @return a new vector with the abs of v[].
	 */
	public static float [] abs(float [] v)
	{
		float [] n = new float[v.length];
		for (int i = 0; i < v.length; i++)
	                n[i] = Math.abs(v[i]);
                return n;
	}
        /**
         * Return a vector containing only the elements in v that have a mask value != 0.
         * @param v a vector.
         * @param mask a vector of the same size as v, containing non-zero values for all elements in v that are valid.
         * @return a float[] vector containing only the masked elements of v (unordered).
         */
        public static float [] mask(float [] v, float [] mask)
        {
                // Count number of valid elements.
                int n = 0;
                for (int i= 0; i < mask.length; i++)
                        if (mask[i] > 0) n++;
                // Make the return vector.
                float [] nv = new float[n];
                int c = 0;
                for (int i= 0; i < v.length; i++)
                        if (mask[i] > 0) nv[c++] = v[i];
                return nv;
        }
	/*
	* add(3) adds a scalar to the elements of vector v only where the mask value is 1.
	* @param v a float[] vector.
	* @param mask a float[] with a value of 1 for all elements of v that are valid.
	* @return modified v.
	*/
	public static float [] add(float [] v, float scalar, float [] mask)
	{
		int iN = v.length;
		for (int j = 0; j < iN; j++)
		{
			if (mask[j] == 1)
	                        v[j] += scalar;
		}
		return v;
	}
         /*
         * Adds each element in a to corresponding element in b and put result in r.
         * r is modified.
         * @param r a float[] vector.
         * @param a a float[] vector.
         * @param b a float[] vector.
         */
         public static void addElements(double [] r, double [] a, double [] b)
         {
                 for (int j = 0; j < a.length; j++)
                         r[j] = a[j] + b[j];
         }
	/*
	* Adds each element in a to corresponding element in b and put result in r.
	* r is modified.
	* @param r a float[] vector.
	* @param a a float[] vector.
	* @param b a float[] vector.
	*/
	public static void addElements(float [] r, float [] a, float[] b)
	//throws IllegalArgumentException
        {
		if (a.length != b.length)
                        throw new IllegalArgumentException("Vector sizes do not match "+a.length+" "+b.length);
                for (int j = 0; j < a.length; j++)
			r[j] = a[j] + b[j];
	}
        /**
        * Add the elements of two matrices a and b and save the results in a matrix r.
        * @param r a float[][] matrix for the result.
        * @param a a float[][] matrix.
        * @param b a float[][] matrix.
        */
        public static void add(float [][] r, float [][] a, float [][] b)
        {
          for (int j = 0; j < a.length; j++)
                for (int k = 0; k < a[0].length; k++)
                      r[j][k] = a[j][k] + b[j][k];
        }
	/*
	* Adds each element in a to corresponding element in b.
	* @param a a float[] vector.
	* @param b a float[] vector.
	* @return a new vector with the result.
	*/
	public static float [] addElements(float [] a, float[] b)
	{
		float [] n = new float[a.length];
		for (int j = 0; j < a.length; j++)
			n[j] = a[j] + b[j];
		return n;
	}
	/**
	* Compute the power'th power of each entry in the matrix m.
	* @param m a float[][] matrix.
	* @param power the exponent to subtract.
	* @return the modified matrix.
	*/
	public static float [][] pow(float [][] m, float power)
	{
		int iN = m.length; int iM = m[0].length;
		float [][] nm = new float[iN][iM];
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		      nm[j][i] = (float) Math.pow(m[j][i], power);
		return nm;
	}
	/**
	 * Return length or norm of vector v.
	 * @param v a float[] matrix.
	 * @return the norm of v.
	 */
	public static float norm(float [] v)
	{
                double l = 0;
                for (int i = 0; i < v.length; i++)
                          l += (double) v[i] * v[i];
                return (float) Math.sqrt(l);
	}
        /**
        * Compute the natural logarithmr of each element in vector v and put into r.
        * If an element is < 0 and power is between -1 and 1, 0 is used to avoid taking the root of negative numbers.
        * @param r a float[] vector for the result.
        * @param v a float[] vector.
        */
        public static void ln(float [] r, float [] v)
        {
                for (int i = 0; i < v.length; i++)
                {
                        if (v[i] == 0)
                                r[i] = (float) Math.log(Float.MIN_VALUE);
                        else
                                r[i] = (float) Math.log(v[i]);
                }
        }
        /**
        * Compute the power'th power of each element in vector v and put into r.
        * If an element is < 0 and power is between -1 and 1, 0 is used to avoid taking the root of negative numbers.
        * @param r a float[] vector for the result.
        * @param v a float[] vector.
        * @param power the power the exponent.
        */
        public static void pow(double [] r, double [] v, double power)
        {
                for (int i = 0; i < v.length; i++)
                {
                        if (power < 1 && power > -1 && v[i] < 0)
                                r[i] = 0;
                        else
                                r[i] = Math.pow(v[i], power);
                }
        }
        /**
        * Compute the power'th power of each element in vector v and put into r.
        * If an element is < 0 and power is between -1 and 1, 0 is used to avoid taking the root of negative numbers.
        * @param r a float[] vector for the result.
        * @param v a float[] vector.
        * @param power the power the exponent.
        */
        public static void pow(float [] r, float [] v, double power)
        {
                for (int i = 0; i < v.length; i++)
                {
                        if (power < 1 && power > -1 && v[i] < 0)
                                r[i] = 0;
                        else
                                r[i] = (float) Math.pow(v[i], power);
                }
        }
	/**
	* Divide each entry in v by divisor.
	* @param v a float[] vector.
	* @param divisor the value to divide by.
	* @return a copy of v.
        * @deprecated: use mul 1/divisor instead.
	*/
	public static float [] div(float [] v, double divisor)
	{
		int iN = v.length;
		float [] nv = new float[iN];
		for (int i = 0; i < iN; i++)
		       nv[i] = (float) (v[i] / divisor);
		return nv;
	}
	/**
	* Divide each entry in m by divisor.
	* @param m a float[] matrix.
	* @param divisor the value to divide by.
	*/
	public static void divElements(float [][] m, double divisor)
	{
		int iN = m.length; int iM = m[0].length;
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		       m[j][i] /= divisor;
	}
	/**
	* Copy a vector.
	* @param v a float[] vector.
	* @return a copy of v.
	*/
	public static float [] copy(float [] v)
	{
		float [] nv = new float[v.length];
		for (int i = 0; i < v.length; i++)
		       nv[i] = v[i];
		return nv;
	}
	/**
	* Copy a matrix.
	* @param m a float[][] matrix.
	* @return a copy of m.
	*/
	public static float [][] copy(float [][] m)
	{
		int iN = m.length; int iM = m[0].length;
		float [][] nm = new float[iN][iM];
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		      nm[j][i] = m[j][i];
		return nm;
	}
	/**
	* Add a scalar to all entries in a matrix m in place.
        * m is modified!
	* @param m a float[][] matrix.
	* @param scalar a scalar to subtract.
	*/
	public static void add(float [][] m, double scalar)
	{
                //IJ.showStatus("add: "+m.length);
		for (int j = 0; j < m.length; j++)
		for (int i = 0; i < m[0].length; i++)
		       m[j][i] += scalar;
                //IJ.showStatus("finished add: "+m.length);
	}
	/**
	* Add (elements in) a matrix b to a matrix a.
        * a is MODIFIED!
	* @param m a float[][] matrix.
	* @param n a float[][] matrix.
	*/
	public static void add(float [][] a, float [][] b)
        throws IllegalArgumentException
        {
                if (a.length != b.length)
                       throw new IllegalArgumentException("sub: vector sizes do not match");
		int iN = a.length; int iM = a[0].length;
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		       a[j][i] += b[j][i];
	}
	/**
	* Subtract a matrix n from a matrix m.
        * @param r a float[][] matrix for the result.
	* @param m a float[][] matrix.
	* @param n a float[][] matrix.
	* @return the subtracted modified matrix.
	*/
	public static void sub(float [][] r, float [][] m, float [][] n)
	{
		int iN = m.length; int iM = m[0].length;
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		       r[j][i] = m[j][i] - n[j][i];
	}
	/**
	* Subtract the elements of a vector v from each element of a matrix m, columnwise.
        * @param r a float[][] matrix for the result.
        * @param m a float[][] matrix.
	* @param v a float[] vector.
	*/
	public static void sub(float [][] r, float [][] m, float [] v)
	throws IllegalArgumentException
	{
		int iN = m.length; int iM = m[0].length;
		if (iM != v.length)
	               throw new IllegalArgumentException("sub: vector size does not match matrix");
                for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		       r[j][i] = m[j][i] - v[i];
	}
	/*
	* Subtracts each element in b from corresponding element in a.
	* @param a a float[] vector.
	* @param b a float[] vector.
	* @return a new vector with the result.
	*/
	public static float [] sub(float [] a, float[] b)
        throws IllegalArgumentException
	{
                if (a.length != b.length)
                       throw new IllegalArgumentException("sub: vector sizes do not match");
		float [] n = new float[a.length];
		for (int j = 0; j < a.length; j++)
			n[j] = a[j] - b[j];
		return n;
	}
	/*
	* Subtract each element in b from corresponding element in b and put result in r.
	* r is modified.
	* @param r a float[] vector.
	* @param a a float[] vector.
	* @param b a float[] vector.
	*/
	public static void subElements(float [] r, float [] a, float[] b)
	{
		for (int j = 0; j < a.length; j++)
			r[j] = a[j] - b[j];
	}
	/*
	* Subtract scalar from each element in b and put result in r.
	* r is modified.
	* @param r a float[] vector.
	* @param a a float[] vector.
	* @param scalar a float scalar.
        * @deprecated
	*/
	public static void sub(float [] r, float [] a, double scalar)
	{
		for (int j = 0; j < a.length; j++)
			r[j] = (float) (a[j] - scalar);
	}
	/**
	* Get the diagonal vector of (square) matrix m.
	* @param m a float[][] matrix.
	* @return a new float[] vector with the diagonal of m.
	*/
	public static float [] diag(float [][] m)
	{
		int iN = m.length;
		float [] v = new float[iN];
		for (int i = 0; i < iN; i++)
		       v[i] = m[i][i];
		return v;
	}
        /**
        * Get a square matrix with v on the diagonal.
        * @param v a float[]
        * @return m a float[][] matrix of the same length as v.
        */
        public static float [][] diag(float [] v)
        {
                int iN = v.length;
                float [][] m = new float[iN][iN];
                for (int i = 0; i < iN; i++)
                       m[i][i] = v[i];
                return m;
        }
	/**
	* Truncate a vector to a new length n.
	* @param v a float[] vector.
	* @return a vector with the first n entries of v.
	*/
	public static float [] trunc(float [] v, int n)
	{
		int iN = v.length;
		if (iN == n)
	               return v;
		float [] nv = new float[n];
		for (int i = 0; i < n; i++)
		       nv[i] = v[i];
		return nv;
	}
	/**
	* Truncate a matrix to a new column length n.
	* @param m a float[][] matrix.
	* @return a matrix with the first n columns of m.
	*/
	public static float [][] trunc(float [][] m, int n)
	{
		int iN = m.length;
		if (iN == n)
	               return m;
		float [][] nm = new float[n][];
		for (int i = 0; i < n; i++)
		       nm[i] = m[i];
		return nm;
	}
        /**
         * Make a subset of v, including only those with an index which is in indices.
         * @param v a float[] vector
         * @param indices an int[] vector containing the valid indices for the subset
         * @return a float[] vector of the same length as indices.
         */
        public static float [] subset(float [] v, int [] indices)
        {
                float [] r = new float[indices.length];
                for (int i = 0; i < r.length; i++)
                        r[i] = v[indices[i]];
                return r;
        }
        /**
         * Make a subset of v, including only those with an index which is equal or larger than start.
         * @param v a float[] vector
         * @param start the start index of the set
         * @return a float[] vector
         */
        public static float [] subset(float [] v, int start)
        {
                float [] r = new float[v.length-start];
                for (int i = start; i < r.length; i++)
                        r[i-start] = v[i];
                return r;
        }
        /**
         * Make a subset of v, including only those with an index which is equal or larger than start up to and including last.
         * @param v a float[] vector
         * @param start the start index of the set
         * @return a float[] vector
         */
        public static float [] subset(float [] v, int start, int last)
        {
                float [] r = new float[last-start+1];
                for (int i = start; i <= last; i++)
                        r[i-start] = v[i];
                return r;
        }
        /**
         * Make a subset of m, including only those with an index which is equal or larger than start up to and including last.
         * @param v a float[] vector
         * @param start the start index of the set
         * @return a float[] vector
         */
        public static float [][] subset(float [][] m, int start, int last)
        {
                float [][] r = new float[last-start+1][];
                for (int i = start; i <= last; i++)
                        r[i-start] = m[i];
                return r;
        }
        /**
         * Make a subset of m, include only those row vectors with an index which is in indices.
         * @param m a float[][] matrix
         * @param indices an int[] vector containing the valid indices for the subset
         * @return a float[][] matrix vector of the same length as indices.
         */
        public static float [][] subset(float [][] m, int [] indices)
        {
                float [][] r = new float[indices.length][];
                for (int i = 0; i < r.length; i++)
                        r[i] = m[indices[i]];
                return r;
        }
        /**
         * Make a subset of m, including only those columns of the row vectors with an index which is in indices.
         * @param m a MxP float[][] matrix
         * @param indices an 1xN int[] vector containing the valid indices for the subset
         * @return a MxN float[][] matrix vector.
         */
        public static float [][] subsetColumns(float [][] m, int [] indices)
        {
                float [][] r = new float[m.length][indices.length];
                for (int i = 0; i < r.length; i++)
                for (int j = 0; j < r[0].length; j++)
                        r[i][j] = m[i][indices[j]];
                return r;
        }
        /**
         * Return the n'th column of matrix m as a vector.
         * @param m a float[][] matrix.
         * @param n the column desired.
         * @return a column vector with the n-th column.
         */
        public static float [] col(float [][] m, int n)
        {
                float [] v = new float[m.length];
                for (int i = 0; i < m.length; i++)
                       v[i] = m[i][n];
                return v;
        }
        /**
         * Concatenate a vector to another vector in the same order.
         * @param a a float[] vector.
         * @param b a float[] vector.
         * @return a float[] vector of length a + b.
         */
        public static float [] concat(float [] a, float [] b)
        {
                int al = 0;
                if (a != null)
                        al = a.length;
                float [] v = new float[al + b.length];
                for (int i = 0; i < al; i++)
                        v[i] = a[i];
                for (int i = 0; i < b.length; i++)
                        v[i + al] = b[i];
                return v;
        }
        /**
         * repmat replicates a vector in tiled form.
         */
        public static float [][] repmat(float [] a, int n, int m)
        {
                float [][] b = new float[n][m * a.length];
                for (int j = 0; j < b.length; j++)
                for (int i = 0; i < b[0].length; i++)
                        b[j][i] = a[i % a.length];
                return b;
        }
        /**
         * repmat replicates a matrix in tiled form.
         */
        public static float [][] repmat(float [][] a, int n, int m)
        {
                float [][] b = new float[n * a.length][m * a[0].length];
                for (int j = 0; j < b.length; j++)
                for (int i = 0; i < b[0].length; i++)
                        b[j][i] = a[j % a.length][i % a[0].length];
                return b;
        }
        /**
         * Invert a symmetric matrix m. Only works for 2x2, 3x3, 4x4 matrix now.
         * @param m the matrix to be inverted.
         * @return a float[][] with the inverse of m.
         * @throws IllegalArgumentException
        */
        public static float [][] inverse(float [][] m)
        throws IllegalArgumentException
        {
                if (m.length > 4)
                        throw new IllegalArgumentException("Cannot invert this matrix "+m.length);
		float [][] mi = new float[m.length][m[0].length];
                if (m.length == 2)
                        invert2x2(mi, m);
                else if (m.length == 3)
                        invert3x3(mi, m);
                else if (m.length == 4)
                        invert4x4(mi, m);
		return mi;
        }
        /**
         * Invert 2x2 matrix m into inverse mi.
         * @param mi, a double[2][2] that will be overwritten with the inverse of
         * @param m, a double[2][2] that contains the matrix.
         */
        private static void invert2x2(float [][] mi, float [][] m)
        {
                float d = determinant(m);
                mi[0][0] = m[1][1]/d;
                mi[0][1] = -m[0][1]/d;
                mi[1][0] = -m[1][0]/d;
                mi[1][1] = m[0][0]/d;
        }
        /**
         * Invert 3x3 matrix m into inverse mi.
         * @param mi, a double[4][4] that will be overwritten with the inverse of
         * @param m, a double[4][4] that contains the matrix.
         */
        private static void invert3x3(float [][] mi, float [][] m)
        {
                float d = determinant(m);
                if (Math.abs(d) <= Float.MIN_VALUE*2)
                {
                        mi[0][0] = 0;
                        mi[0][1] = 0;
                        mi[0][2] = 0;
                        mi[1][0] = 0;
                        mi[1][1] = 0;
                        mi[1][2] = 0;
                        mi[2][0] = 0;
                        mi[2][1] = 0;
                        mi[2][2] = 0;
                }
                else
                /*
                    Inv  abc
                         def
                         ghi
                    =   -fh+ei/d ch-bi/d -ce+bf/d
                        fg-di/d -cg+ai/d cd-af/d
                        -eg+dg/d bg-ah/d -bd+ae/d
                */
                {
                        mi[0][0] = (-m[1][2]*m[2][1]+m[1][1]*m[2][2])/d; // -fh+ei
                        mi[0][1] = (m[0][2]*m[2][1]-m[0][1]*m[2][2])/d;  // ch-bi
                        mi[0][2] = (-m[0][2]*m[1][1]+m[0][1]*m[1][2])/d; // -ce+bf
                        mi[1][0] = (m[1][2]*m[2][0]-m[1][0]*m[2][2])/d;  // fg-di
                        mi[1][1] = (-m[0][2]*m[2][0]+m[0][0]*m[2][2])/d; // -cg+ai
                        mi[1][2] = (m[0][2]*m[1][0]-m[0][0]*m[1][2])/d; // cd-af
                        mi[2][0] = (-m[1][1]*m[2][0]+m[1][0]*m[2][1])/d; // -eg+dh
                        mi[2][1] = (m[0][1]*m[2][0]-m[0][0]*m[2][1])/d;  // bg-ah
                        mi[2][2] = (-m[0][1]*m[1][0]+m[0][0]*m[1][1])/d; // -bd+ae
                }
        }
        /**
         * Invert 4x4 matrix m into inverse mi.
         * Uses Gauss-Jordan inversion.
         * See numerical recipes in C, second version.
         * @param mi, a double[3][3] that will be overwritten with the inverse of
         * @param m, a double[3][3] that contains the matrix.
         * throws IllegalArgumentException if inversion fails.
         */
        private static void invert4x4(float [][] mi, float [][] m)
        throws IllegalArgumentException
        {
                float [] t = new float[20];
                float [] x = new float[4];

                // calculate inverse components of first column (m[0][0], m[1][0], m[2][0], m[3][0])
                t[0]  = m[0][0]; t[1]  = m[0][1];
                t[2]  = m[0][2]; t[3]  = m[0][3];
                t[4]  = 1;

                t[5]  = m[1][0]; t[6]  = m[1][1];
                t[7]  = m[1][2]; t[8]  = m[1][3];
                t[9]  = 0;

                t[10] = m[2][0]; t[11] = m[2][1];
                t[12] = m[2][2]; t[13] = m[2][3];
                t[14] = 0;

                t[15] = m[3][0]; t[16] = m[3][1];
                t[17] = m[3][2]; t[18] = m[3][3];
                t[19] = 0;

                if (GaussElim (t, x) <= 0)
                        throw new IllegalArgumentException("inverse failure");

                mi[0][0] = x[0]; mi[1][0] = x[1];
                mi[2][0] = x[2]; mi[3][0] = x[3];

                // calculate inverse components of second column (m[0][1], m[1][1], m[2]10], m[3][1])
                t[0]  = m[0][0]; t[1]  = m[0][1];
                t[2]  = m[0][2]; t[3]  = m[0][3];
                t[4]  = 0;

                t[5]  = m[1][0]; t[6]  = m[1][1];
                t[7]  = m[1][2]; t[8]  = m[1][3];
                t[9]  = 1;

                t[10] = m[2][0]; t[11] = m[2][1];
                t[12] = m[2][2]; t[13] = m[2][3];
                t[14] = 0;

                t[15] = m[3][0]; t[16] = m[3][1];
                t[17] = m[3][2]; t[18] = m[3][3];
                t[19] = 0;

                if (GaussElim (t, x) <= 0)
                        throw new IllegalArgumentException("inverse failure");

                mi[0][1] = x[0]; mi[1][1] = x[1];
                mi[2][1] = x[2]; mi[3][1] = x[3];

                // calculate inverse components of third column (m[0][2], m[1][2], m[2][2], m[3][2])
                t[0]  = m[0][0]; t[1]  = m[0][1];
                t[2]  = m[0][2]; t[3]  = m[0][3];
                t[4]  = 0;

                t[5]  = m[1][0]; t[6]  = m[1][1];
                t[7]  = m[1][2]; t[8]  = m[1][3];
                t[9]  = 0;

                t[10] = m[2][0]; t[11] = m[2][1];
                t[12] = m[2][2]; t[13] = m[2][3];
                t[14] = 1;

                t[15] = m[3][0]; t[16] = m[3][1];
                t[17] = m[3][2]; t[18] = m[3][3];
                t[19] = 0;

                if (GaussElim (t, x) <= 0)
                        throw new IllegalArgumentException("inverse failure");

                // calculate inverse components of fourth column (m[0][3], m[1][3], m[2][3], m[3][3])
                mi[0][2] = x[0]; mi[1][2] = x[1];
                mi[2][2] = x[2]; mi[3][2] = x[3];

                t[0]  = m[0][0]; t[1]  = m[0][1];
                t[2]  = m[0][2]; t[3]  = m[0][3];
                t[4]  = 0;

                t[5]  = m[1][0]; t[6]  = m[1][1];
                t[7]  = m[1][2]; t[8]  = m[1][3];
                t[9]  = 0;

                t[10] = m[2][0]; t[11] = m[2][1];
                t[12] = m[2][2]; t[13] = m[2][3];
                t[14] = 0;

                t[15] = m[3][0]; t[16] = m[3][1];
                t[17] = m[3][2]; t[18] = m[3][3];
                t[19] = 1;

                if (GaussElim (t, x) <= 0)
                        throw new IllegalArgumentException("inverse failure");

                mi[0][3] = x[0]; mi[1][3] = x[1];
                mi[2][3] = x[2]; mi[3][3] = x[3];
        }
	/**
	 * Gaussian elimination method with backward substitution to solve a system of
	 * linear equations.
	 * E1:  a1,1 x1 + a1,2 x2 + ... a1,n xn = a1,n+1
	 * E2:  a2,1 x1 + a2,2 x2 + ... a2,n xn = a2,n+1
	 *                  .                =    .
	 * En:  an,1 x1 + an,2 x2 + ... an,n xn = an,n+1
         * @param a a vector containing the matrix of coefficients a1,1...an,n and
         * also the y's which are called a1,n+1...an,n+1.
         * @param x a vector containing
         * @return if succesful, 0 if error.
	 */
	public static int GaussElim(float [] a, float [] x)
	{
		int n = x.length;      /* number of unknowns and equations in system */
		for (int i = 0; i < n; i++)
	               x[i] = 0;
                int w = n + 1;
		for (int i = 0; i < (n-1); i++)
		{
			int p;
			for (p = i; p < n; p++)
	                        if (a[(p * w) + i] != 0.0)  break;
                        if (p == n) return (0);
			if (p != i)
			{
				for (int k = 0; k < w; k++)
				{
					// swap a[(i * w) + k] and a[(p * w) + k]
                                        float temp = a[(i * w) + k];
					a[(i * w) + k] = a[(p * w) + k];
					a[(p * w) + k] = temp;
				}
			}
			for (int j = (i+1); j < n; j++)
			{
				float m = a[(j * w) + i] / a[(i * w) + i];
				for (int k = 0; k <= n; k++)
	                                a[(j * w) + k] = a[(j * w) + k] - ( m * a[(i * w) + k] );
			}
		}
		if (a[((n-1) * w) + (n-1)] == 0) return (0);
		x[n-1] = a[((n-1) * w) + n] / a[((n-1) * w) + (n-1)];
		for (int i = (n-2); i >= 0; i--)
		{
			float s = 0;
			for (int j = (i+1); j < n; j++)
	                         s = s + (a[(i * w) + j] * x[j]);
                        x[i] = ( a[(i * w) + n] - s) / a[(i * w) + i];
		}
                return (1);
        }
        /**
        * Compute determinant of m.
        * Only works for 2x2 or 3x3 matrix now.
        * @param m a 2x2 or 3x3 double[][] matrix
        */
        public static float determinant(float [][] m)
        {
                if (m.length == 2)
                        return m[0][0]*m[1][1]-m[1][0]*m[0][1];
                else if (m.length == 3)
                /*
                det abc
                    def
                    ghi
                     = (-ceg+bfg+cdh-afh-bdi+aei)
                */
                {
                        float det =
                        -m[0][2]*m[1][1]*m[2][0]  // -ceg
                        +m[0][1]*m[1][2]*m[2][0]  // +bfg
                        +m[0][2]*m[1][0]*m[2][1]  // +cdh
                        -m[0][0]*m[1][2]*m[2][1]  // -afh
                        -m[0][1]*m[1][0]*m[2][2]  // -bdi
                        +m[0][0]*m[1][1]*m[2][2]; // +aei
                        return det;
                }
                else  return 0;
        }
	public static void test()
	//throws Exception
	{
		IJ.write("Testing matrices.");
		float [][] h = { {1,2,3,4,5},{6,7,8,9,10} };
		IJ.write("h = "+BIJutil.toString(h));
		float [][] i = transpose(h);
		IJ.write("i = "+BIJutil.toString(i));
		float [][] j = mul(i, h);
		IJ.write("i*h=j = "+BIJutil.toString(j));
		float [][] k = mul(h, i);
		IJ.write("h*i=k = "+BIJutil.toString(k));
		BIJJacobi jac = new BIJJacobi(j);
		jac.compute();
		IJ.write(jac.toString());
	}
	/**
	 * Sort a vector and a matrix simultaneously based on the order of items in a third vector.
	 * Straight insertion method.
	 */
	public static void sort(float [] indicator, float [] v, float [][] m)
	{
		for (int i = 0; i < indicator.length; i++)
		{
			int k = i;
			float p = indicator[k];
			for (int j = i + 1; j < indicator.length; j++)
			{
				if (indicator[j] >= p)
				{
					k = j;
					p = indicator[k];
				}
			}
			if (k != i)
			{
				indicator[k] = indicator[i];
				indicator[i] = p;
				// Sort the v.
				float t = v[i];
				v[i] = v[k];
				v[k] = t;
				// Sort the m.
				for (int j = 0; j < m.length; j++)
				{
					p = m[j][i];
					m[j][i] = m[j][k];
					m[j][k] = p;
				}
			}
		}
	}
	/**
	 * Sort a vector, but return the result as a vector of indices, conserving the original vector.
	 * @param v a vector
	 * @return an int[] a vector of indices into v, sorted descending.
	 */
	public static int [] sort(float [] v)
	{
		float [] vv = new float[v.length];
		for (int i = 0; i < v.length; i++)
	                vv[i] = v[i];
		for (int i = 0; i < v.length; i++)
		{
			int k = i;
			float p = vv[k];
			for (int j = i + 1; j < v.length; j++)
			{
				if (vv[j] >= p)
				{
					k = j;
					p = vv[k];
				}
			}
			if (k != i)
			{
				vv[k] = vv[i];
				vv[i] = p;
			}
		}
		// Now put the indices into a vector.
		int [] indices = new int[v.length];
		for (int i = 0; i < v.length; i++)
		{
			for (int j = 0; j < v.length; j++)
			{
	                        if (v[j] == vv[i])
				{
					indices[i] = j;
					break;
				}
			}
		}
		return indices;
	}
        /**
         * Compute the inverse of J[m][n], a float[][] matrix, using the pseudoinverse computed by SVD.
         * J is conserved.
         * If singularLimit != 0, the singular values smaller than singularLimit are
         * replaced by singularLimit.
         * @return the condition number of the W matrix.
         * @deprecated
         */
        public static float pseudoinverse(float [][] JI, float[][] J, double singularLimit)
        {
                int n = JI.length;
                int m = JI[0].length;
                double [][] dJ = new double[m][n];
                for (int i = 0; i < m; i++)
                for (int j = 0;  j < n; j++)
                        dJ[i][j] = J[i][j];
                double [][] dJI = new double[n][m];
                float condnum = (float) pseudoinverse(dJI, dJ, singularLimit);
                for (int i = 0; i < n; i++)
                for (int j = 0;  j < m; j++)
                        JI[i][j] = (float) dJI[i][j];
                return condnum;
        }
        /**
         * LONG revoked by other routines. This is legacy code which should be replaced by better routines.
         * Compute the inverse of J[m][n], a double[][] matrix, using the pseudoinverse computed by SVD.
         * J is conserved.
         * If singularLimit != 0, the singular values smaller than singularLimit are
         * replaced by singularLimit.
         * @param JI the inverse of J after the call.
         * @param J the matrix for which the pseudoinverse will be computed.
         * @param singularLimit, the threshold for computed values in the matrix below which they are treated as 0.
         * @return the condition number of the W matrix.
         * @deprecated
         */
        public static double pseudoinverse(double [][] JI, double[][] J, double singularLimit)
        {
          int n = JI.length;
          int m = JI[0].length;
          if (n > J[0].length || m > J.length)
              throw new IllegalArgumentException("pseudoinverse failure");

          double [][] U = new double [m][n];
          double [][] V = new double [n][n];  // was [n][n]
          double [] W = new double [n];       // was [n+1]
          double [] zero = new double [n];    // was [n]
          double [] temp = new double [m];
          // Copy J to tempJ because it is clobbered in SVDC.
          double [][] tempJ = new double[m][n];
          for(int i=0;i<m;i++)
                for(int j=0;j<n;j++)
                            tempJ[i][j] = J[i][j];
          /*
              Call SVD routine
                          Java: public static void dsvdc_j (double x[][], int n, int p, double s[],
                                 double e[], double u[][], double v[][],
                                 double work[], int job)
          */
          /*
          Jama.Matrix j = new Jama.Matrix(tempJ);
          Jama.SingularValueDecomposition svd = new Jama.SingularValueDecomposition(j);
          Jama.Matrx W = svd.getS();
          */

          try { SVDC.dsvdc_j(tempJ,m,n,W,zero,U,V,temp,21); }
          catch (SVDCException e) { throw new IllegalArgumentException("svdc failure");}

          double [][]DI = new double[n][n];
          /* Compute condition number as the ratio of maximum to
            minimum diagonal element of the SVD diagonal matrix
            and compute the inverse of the diagonal matrix 1/W.*/
          double min = Double.MAX_VALUE;
          double max = -Double.MAX_VALUE;
          for(int i=0;i<n;i++)
                {
                      if(W[i]!=0) DI[i][i] = 1/W[i];
                      else DI[i][i] = Double.MAX_VALUE;
                if(Math.abs(W[i]) > max) max = Math.abs(W[i]);
                      if(Math.abs(W[i]) < min) min = Math.abs(W[i]);
                }
          double condnum;
          if (min != 0) condnum = max/min;
          else condnum=Double.MAX_VALUE;


          double [][]V1 = new double[n][n];
          /* Compute V diag(1/w) U(T) */
          for(int i=0;i<n;i++)
                for(int j=0;j<n;j++)
                      {
                            V1[i][j] = 0;
                            for(int k=0;k<n;k++) V1[i][j] += V[i][k]*DI[k][j]; // V x diag(1/w)
                      }
          for(int i=0;i<n;i++)
                for(int j=0;j<m;j++)
                {
                            JI[i][j] = 0;
                            for(int k=0;k<n;k++) JI[i][j] += V1[i][k]*U[j][k]; // x Utransposed
                      }
          if (condnum < 100000)
                if (! checkinverse(J, JI))
                       throw new IllegalArgumentException("pseudoinverse failure");
          // replace, if applicable, the singular values smaller than singularLimit.
          if (singularLimit != 0)
          {
                for(int i=0;i<n;i++)
                      if (DI[i][i] > (1/singularLimit)) DI[i][i] = 1/singularLimit;
          }
          /* Repeat the inverse computation. */
          /* Compute V diag(1/w) U(T) */
          for(int i=0;i<n;i++)
                for(int j=0;j<n;j++)
                      {
                            V1[i][j] = 0;
                            for(int k=0;k<n;k++) V1[i][j] += V[i][k]*DI[k][j]; // V diag(1/w)
                      }
          for(int i=0;i<n;i++)
                for(int j=0;j<m;j++)
                {
                            JI[i][j] = 0;
                            for(int k=0;k<n;k++) JI[i][j] += V1[i][k]*U[j][k]; // x Utransposed
                      }
           return condnum;
        } // pseudoinverse
        /**
         * Check whether JI is really the inverse of J.
        */
        public static boolean checkinverse(double [][] J, double [][] JI)
        {
                      int n = JI.length;
                      int m = JI[0].length;

                      boolean error=false;
                      /* Calculate I = JI * J. */
                      double [][] I = new double[n][n];
                      for(int i=0;i<n;i++)
                             for(int j=0;j<n;j++)
                             {
                                    I[i][j] = 0;
                                    for(int k=0;k<m;k++)  I[i][j] += JI[i][k]*J[k][j];
                             }
                      for(int i=0;i<n;i++)
                             for(int j=0;j<n;j++)
                             {
                                    if(i==j)
                                    {
                                          if(Math.abs(I[i][i])<0.9999 || Math.abs(I[i][i])>1.0001)
                                                error=true;
                                    }
                                    else if(Math.abs(I[i][j]) > 0.0001)
                                          error=true;
                             }
                      if (error)
                      {
                                  IJ.write("Inverse failure\nJ matrix =");
                                  for(int i=0;i<m;i++)
                                  {
                                        String s = "";
                                        for(int j=0;j<n;j++)
                                              s += J[i][j]+"\t";
                                        IJ.write(s);
                                  }
                                  IJ.write("JI matrix =");
                                  for(int i=0;i<m;i++)
                                  {
                                          String s = "";
                                          for(int j=0;j<n;j++)
                                                s += JI[j][i]+"\t ";
                                          IJ.write(s);
                                  }
                                  IJ.write("I matrix =");
                                  for(int i=0;i<n;i++)
                                  {
                                          String s = "";
                                          for(int j=0;j<n;j++)
                                                s += I[j][i]+"\t ";
                                          IJ.write(s);
                                  }
                      }
                      return (! error);
          } // checkinverse
}

