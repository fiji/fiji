package bijnum;
import ij.*;

/**
 * This class implements the jacobi function as a Java class.
 * From Press et al., 2nd ed., Numerical Solutions in C.
 * @deprecated
 * @see bijnum.BIJJacobi
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
public class Jacobi
{
	public double eigenvectors[][]; // used to be v[][].
	public double eigenvalues[];    // used to be d[].
	private double g, h;
	private int  n, nrot, sweeps;
	private final static double UNDERFLOW_LIMIT = 0.0000000000000000000000001; // Value used for threshold of underflow.

	/**
	 * Create the eigenvectors and eigenvalues that will be computed.
	 */
	public Jacobi(int n)
	{
		this.n = n;
		eigenvectors = new double[n][n];
		eigenvalues = new double[n];
	}
	/*
	    The comment below applies to C source, but I kept it in for historical reasons.

      Note: Numerical Recipes source uses a #define for function "rotate".
      On some compilers this may result in a nonfunctional jacobi
      (took me 3 days to find this out).
	    So please keep "rotate" as a function!
  */
    private void rotate(double a[][], int i, int j, int k, int l, double s, double tau)
    {
        // modifies class variables g and h.
	      g = a[i][j];
	      h = a[k][l];
	      a[i][j] = g - s *(h + g * tau);
	      a[k][l] = h + s *(g - h * tau);
    } /* rotate */
    public int nrot()
    { return nrot; }
    public int sweeps()

    { return sweeps; }
    public void compute(float [][] aa)
    {
          double [][] daa = new double[aa.length][aa.length];
          for (int i = 0; i < aa.length; i++)
                for (int j = 0; j < aa.length; j++)
                      daa[i][j] = aa[i][j];
          compute(daa);
    }
    /*
        Compute all eigenvalues and eigenvectors of a real symmetric matrix
        aa[N][N].
        eigenvalues (d) [N] returns the eigenvalues of a. eigenvalues (v)[N][N] is a matrix whose columns
        contain, on output, the normalized eigenvectors of a.
        From: Press et al., Numerical Recipes in C, 2nd ed., pp 467-
    */
    public void compute(double [][] aa)
    {
        nrot = sweeps = 0;
        double [][] a = new double[n][n];  // matrix of which you want the E-values.
        for (int ip = 0; ip < n; ip++)
	      {
	  	      for (int iq = 0; iq < n; iq++)
            {
                  eigenvectors[ip][iq] = 0;   // v[ip][iq] = 0
	  		          a[ip][iq] = aa[ip][iq];     // Copy aa
            }
            eigenvectors[ip][ip] = 1; // make v an identity matrix.
        }
	      /* Initialize b and d (eigenvalues) to the diagonals of a */
	      double [] b = new double[n]; double [] z = new double[n];
        for (int ip = 0; ip < n; ip++)
	      {
		        b[ip] = eigenvalues[ip] = a[ip][ip];
		        z[ip] = 0;
	      }
	      for (int i = 0; i < 100; i++)
	      {
		          double sm = 0;
              double thresh;

		          for (int ip = 0; ip < (n-1); ip++)
		          {
			                for (int iq = ip + 1; iq < n; iq++)
				                  sm += Math.abs(a[ip][iq]);
		          }
              if (sm == 0)
              {
                    sweeps = i;
                    return;
              }
              /*
			              Macintosh PowerPC and 68xxx does not underflow to zero.
                    IEEE compliant code:
                    if  (sm <= UNDERFLOW_LIMIT)
                    {
                          FlowInterface.write("Jacobi: return on underflow");
                          return;
                    }
		          */
              if (i < 3) thresh = 0.2 * sm /(n * n); /* on the first three sweeps */
		          else thresh = 0.0; /* the rest of the sweeps */
 		          for (int ip = 0; ip < (n-1); ip++)
		          {
			  for (int iq = ip + 1; iq < n; iq++)
			  {
				  // g is class variable.
          g = 100.0 * Math.abs(a[ip][iq]);
				  /* After 4 sweeps skip the rotation if the
			 	    off diagonal element is small */
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
				  		  double theta = 0.5 * h / a[ip][iq];
				  		  t = 1.0 / (Math.abs(theta) + Math.sqrt(1.0 + theta * theta));
				  		  if (theta < 0) t = -t;
				  	}
					  double c = 1.0 / Math.sqrt(1.0 + t*t);
					  double s = t *  c;
					  double tau = s / (1.0 + c);
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
        if (i >= 50)
            IJ.write("i >= "+i+" in jacobi");
    }  // for
  } /* jacobi */
  public void sort()
  /* Sort the eigenvalues and the corresponding eigenvectors */
  {
        /* Most likely, already ordered				   */
        if (eigenvalues.length > 2)   // 3 eigenvalues.
        {
                if (eigenvalues[0] < eigenvalues[2]) /* Largest eigenvalue first */
                {
                      /* swap eigenvalues */
                      double t = eigenvalues[0]; eigenvalues[0] = eigenvalues[2]; eigenvalues[2] = t;
                      /* swap eigenvectors components */
                      t = eigenvectors[0][0]; eigenvectors[0][0] = eigenvectors[0][1]; eigenvectors[0][1] = t;
                      t = eigenvectors[2][0]; eigenvectors[2][0] = eigenvectors[2][1]; eigenvectors[2][1] = t;
                }
        }
        if (eigenvalues[0] < eigenvalues[1]) // 2 or 3 eigenvalues
        {
                  /* swap eigenvalues */
                  double t = eigenvalues[0]; eigenvalues[0] = eigenvalues[1]; eigenvalues[1] = t;
                  /* swap eigenvectors components */
                  t = eigenvectors[0][0]; eigenvectors[0][0] = eigenvectors[0][1]; eigenvectors[0][1] = t;
                  t = eigenvectors[1][0]; eigenvectors[1][0] = eigenvectors[1][1]; eigenvectors[1][1] = t;
        }
        if (eigenvalues.length > 2)   // 3 eigenvalues.
        {

                if (eigenvalues[1] < eigenvalues[2]) /* Largest eigenvalue first */
                {
                      /* swap eigenvalues */
                      double t = eigenvalues[1]; eigenvalues[1] = eigenvalues[2]; eigenvalues[2] = t;
                      /* swap eigenvectors components */
                      t = eigenvectors[1][0]; eigenvectors[1][0] = eigenvectors[1][1]; eigenvectors[1][1] = t;
                      t = eigenvectors[2][0]; eigenvectors[2][0] = eigenvectors[2][1]; eigenvectors[2][1] = t;
                }
        }
   }
	public int check(double [][] mm)
    /*
          check eigenvalues, eigenvectors.
          returns 0 if ok.
    */
    {
        double diff1[], diff2[], length;

        diff1 = new double[n];
        diff2 = new double[n];
        /* Compute angle between two eigenvectors - should be orthogonal */
        double angle = Math.acos((eigenvectors[0][0]*eigenvectors[0][1]+eigenvectors[1][0]*eigenvectors[1][1])/
	        ( Math.sqrt(eigenvectors[0][0]*eigenvectors[0][0]+eigenvectors[1][0]*eigenvectors[1][0])*
	          Math.sqrt(eigenvectors[0][1]*eigenvectors[0][1]+eigenvectors[1][1]*eigenvectors[1][1])))*180.0/Math.PI;
        if (angle < 89.5 || angle > 90.5)
            return 1;

        /* eigenvectors test */
        diff1[0] = mm[0][0]*eigenvectors[0][0]+mm[0][1]*eigenvectors[1][0];
        diff1[1] = mm[1][0]*eigenvectors[0][0]+mm[1][1]*eigenvectors[1][0];
        diff1[0] = diff1[0] - eigenvalues[0]*eigenvectors[0][0];
        diff1[1] = diff1[1] - eigenvalues[0]*eigenvectors[1][0];
        if((length = Math.sqrt(diff1[0]*diff1[0]+diff1[1]*diff1[1])) > 0.1)
              return 2;
        diff2[0] = mm[0][0]*eigenvectors[0][1]+mm[0][1]*eigenvectors[1][1];
        diff2[1] = mm[1][0]*eigenvectors[0][1]+mm[1][1]*eigenvectors[1][1];
        diff2[0] = diff2[0] - eigenvalues[1]*eigenvectors[0][1];
        diff2[1] = diff2[1] - eigenvalues[1]*eigenvectors[1][1];
        if((length = Math.sqrt(diff2[0]*diff2[0]+diff2[1]*diff2[1])) > 0.1)
              return 3;
        if (nrot > 50)
              return 4;
        return 0;
  }
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
		return sb.toString();
	}
}
