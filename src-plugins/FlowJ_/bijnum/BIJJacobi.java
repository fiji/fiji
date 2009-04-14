package bijnum;
import ij.*;
import java.io.*;

/**
 * This class implements the jacobi function as a Java class.
 * Redesigned to be faster, for float. Internal calculations are still double though.
 * From Press et al., 2nd ed., Numerical Solutions in C.
 *
 * @see Jacobi.java
 * for the original sources which I kept to be sure because they worked.
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
public class BIJJacobi
{
	/** The matrix of which you want to compute E. */
	public float [][] a;
	public float eigenvectors[][]; // used to be v[][].
	public float eigenvalues[];    // used to be d[].
	/** Whether to show a showProgress() in ImageJ only. */
	protected boolean doFeedback;
	protected int  n, nrot, sweeps;
	protected final static float UNDERFLOW_LIMIT = (float) 0.0000000000001; // Value used for threshold of underflow.
	/** Modified across methods. */
	private double g, h, sm;

	/**
	 * Create new instance to compute eigenvectors, eigenvalues.
	 * @param m a real, symmetric matrix of nxn.
	 * @param doFeedback, whether a progress routine should be called after each sweep (only for user interaction).
	 */
	public BIJJacobi(float [][] m, boolean doFeedback)
	{
		n = m.length;
		if (m.length != m[0].length)
	               throw new IllegalArgumentException("BIJJacobi: not square matrix");
		a = new float[n][n];  // matrix of which you want the E-values.
		// Save the matrix m.
		for (int ip = 0; ip < n; ip++)
		for (int iq = 0; iq < n; iq++)
	                 a[ip][iq] = m[ip][iq];
		eigenvectors = new float[n][n];
		eigenvalues = new float[n];
		this.doFeedback = doFeedback;
	}
	/**
	 * Create new instance to compute eigenvectors, eigenvalues.
	 * @param m a real symmetric matrix of NxN.
	 */
	public BIJJacobi(float [][] m)
	{
		n = m.length;
		if (m.length != m[0].length)
	               throw new IllegalArgumentException("BIJJacobi: not square matrix");
		a = new float[n][n];  // matrix of which you want the E-values.
		// Save the matrix m.
		for (int ip = 0; ip < n; ip++)
		for (int iq = 0; iq < n; iq++)
	                 a[ip][iq] = m[ip][iq];
		eigenvectors = new float[n][n];
		eigenvalues = new float[n];
	}
	/**
	* Compute all eigenvalues and eigenvectors of a real symmetric matrix a[N][N].
	* Will destroy a!.
	* The eigenvalues/eigenvectors can be obtained from the corresponding public variables.
        * From: Press et al., Numerical Recipes in C, 2nd ed., pp 467-
	*/
	public void compute()
	{
		nrot = sweeps = 0;
		for (int ip = 0; ip < n; ip++)
		{
			for (int iq = 0; iq < n; iq++)
				eigenvectors[ip][iq] = 0;   // v[ip][iq] = 0
			eigenvectors[ip][ip] = 1; // make v an identity matrix.
		}
		/* Initialize b and d (eigenvalues) to the diagonals of a */
		float [] b = new float[n];
		float [] z = new float[n];
		for (int ip = 0; ip < n; ip++)
		{
		        b[ip] = a[ip][ip];
			eigenvalues[ip] = a[ip][ip];
		        z[ip] = 0;
		}
		for (int i = 0; i < 50; i++)
		{
		          if (doFeedback)
		                   IJ.showProgress(i/10d);
	                  sm = 0;
		          for (int ip = 0; ip < (n-1); ip++)
			  for (int iq = ip + 1; iq < n; iq++)
	                           sm += Math.abs(a[ip][iq]);
			  if (sm == 0)
			  {
				  sweeps = i;
				  return;
			  }
			  /**
			  * Java does, but Macintosh PowerPC and 68xxx does not underflow to zero.
			  * IEEE compliant code:
			  * if  (sm <= UNDERFLOW_LIMIT) or possibly sm <= Double.MIN_VALUE
			  * {
			  *        FlowInterface.write("Jacobi: return on underflow");
			  *        return;
			  * }
		          */
			  double thresh = 0;
			  if (i < 3) thresh = 0.2d * sm / (n * n); /* on the first three sweeps */
		          else thresh = 0; /* the rest of the sweeps */
 		          for (int ip = 0; ip < (n-1); ip++)
		          {
				   for (int iq = ip + 1; iq < n; iq++)
				   {
					   // g is class variable.
					   g = 100.0d * Math.abs(a[ip][iq]);
					   /* After 4 sweeps skip the rotation if the off diagonal element is small */
					   if (i  > 3 && (Math.abs(eigenvalues[ip]) + g) == Math.abs(eigenvalues[ip])
					       && (Math.abs(eigenvalues[iq]) + g) == Math.abs(eigenvalues[iq]))
						       a[ip][iq] = 0;
					   else if (Math.abs(a[ip][iq]) > thresh)
					   {
						       double t;
						       // h is class variable.
						       h = eigenvalues[iq] - eigenvalues[ip];
						       if  ((Math.abs(h) + g)  ==  Math.abs(h))
							       t = (a[ip][iq]) / h;
						       else
						       {
							       double theta = 0.5d * h / a[ip][iq];
							       t = 1d / (Math.abs(theta) + Math.sqrt(1d + theta * theta));
							       if (theta < 0) t = -t;
						       }
						       double c = 1d / (float) Math.sqrt(1d + t*t);
						       double s = t * c;
						       double tau = s / (1d + c);
						       h = t * a[ip][iq];
						       z[ip] -= h;
						       z[iq] += h;
						       eigenvalues[ip] -= h;
						       eigenvalues[iq] += h;
						       a[ip][iq] = 0;
						       for (int j = 0; j < ip-1; j++)
							       rotate(a, j, ip, j, iq, s, tau);
						       for (int j = ip + 1; j< iq - 1; j++)
							       rotate(a, ip, j, j, iq, s, tau);
						       for (int j = iq + 1; j < n; j++)
						       	       rotate(a, ip, j, iq, j, s, tau);
						       for (int j = 0; j < n; j++)
						               rotate(eigenvectors, j, ip, j, iq, s, tau);
					               nrot++;
					     }
				   }
			   }
		           for (int ip = 0; ip < n; ip++)
			   {
				   b[ip] += z[ip];
				   eigenvalues[ip] = b[ip];
				   z[ip] = 0;
			   }
		}
		System.err.println("Too many iterations in BIJJacobi");
	}
	/**
	 * Sort the eigenvalues/eigenvectors on the basis of the eigenvalues, largest first.
	 * Straight insertion method.
	 */
	public void sort()
	{
		for (int i = 0; i < n; i++)
		{
			int k = i;
			float p = eigenvalues[k];
			for (int j = i + 1; j < n; j++)
			{
				if (eigenvalues[j] >= p)
				{
					k = j;
					p = eigenvalues[k];
				}
			}
			if (k != i)
			{
				eigenvalues[k] = eigenvalues[i];
				eigenvalues[i] = p;
				for (int j = 0; j < n; j++)
				{
					p = eigenvectors[j][i];
					eigenvectors[j][i] = eigenvectors[j][k];
					eigenvectors[j][k] = p;
				}
			}
		}
	}
	/**
	* Rotate, used to be a macro in C sources.
	* The comment below applies to C source, but I kept it in for historical reasons.
	* Note: Numerical Recipes source uses a #define for function "rotate".
	* On some compilers this may result in a nonfunctional jacobi
	* (took me 3 days to find this out).
	* So please keep "rotate" as a function!
	*/
	protected void rotate(float a[][], int i, int j, int k, int l, double s, double tau)
	{
		// modifies class variables g and h.
		g = a[i][j];
		h = a[k][l];
		a[i][j] = (float) (g - s *(h + g * tau));
		a[k][l] = (float) (h + s *(g - h * tau));
	}
	public int nrot() { return nrot; }
	public int sweeps() { return sweeps; }
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("eigenvalues:\teigenvectors:\n");
		for (int i = 0; i < n; i++)
		{
			sb.append(eigenvalues[i]);
			sb.append(" {");
			for (int j = 0; j < n; j++)
			{
				sb.append(eigenvectors[i][j]);
				sb.append(", ");
			}
			sb.append("} \n");
		}
		sb.append("sweeps: ");
		sb.append(sweeps);
		sb.append(" sm=");
		sb.append(sm);
		return sb.toString();
	}
}
