// ConjugateDirectionSearch.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


// - algorithm is based on Brent's modification of a conjugate direction
//   search method proposed by Powell (praxis)
//   Richard P. Brent.  1973.   Algorithms for finding zeros and extrema
//   of functions without calculating derivatives.  Prentice-Hall.


package pal.math;


/**
 * methods for minimization of a real-valued function of
 * several variables without using derivatives (Brent's modification
 * of a conjugate direction search method proposed by Powell)
 *
 * @author Korbinian Strimmer
 */
public class ConjugateDirectionSearch extends MultivariateMinimum 
{
	// Just put this in here so we can have a public class which
	// doesn't need to be in its own file:
	public static class OptimizationError extends RuntimeException {
		public OptimizationError(String s) {
			super(s);
		}
	}

	//
	// Public stuff
	//

	/**
	 * constructor
	 */
	public ConjugateDirectionSearch()
	{
		// random number generator
		rng = new MersenneTwisterFast();
	}
	
	
	// Variables that control aspects of the inner workings of the
	// minimization algorithm. Setting them is optional, they      
	// are all set to some reasonable default values given below.        

	/**
	 *  controls the printed output from the routine        
	 *  (0 -> no output, 1 -> print only starting and final values,            
	 *   2 -> detailed map of the minimization process,         
	 *   3 -> print also eigenvalues and vectors of the       
	 *   search directions), the default value is 0                                
	 */
	public int prin = 0;
	
	/**
	 * step is a steplength parameter and should be set equal     
	 * to the expected distance from the solution.          
	 * exceptionally small or large values of step lead to   
	 * slower convergence on the first few iterations        
	 * the default value for step is 1.0                     
	 */
	public double step = 1.0;
	
	/**
	 * scbd is a scaling parameter. 1.0 is the default and        
	 * indicates no scaling. if the scales for the different 
	 * parameters are very different, scbd should be set to  
	 * a value of about 10.0.                                
	 */
	public double scbd = 1.0;
	
	/**
	 * illc  should be set to true
	 * if the problem is known to  
	 * be ill-conditioned. the default is false. this   
	 * variable is automatically set, when the problem   
         * is found to to be ill-conditioned during iterations.  
	 */
	public boolean illc = false;

    /** MHL: added this so we can interrupt the optimization if
        we need to. */
    public boolean interrupt = false;
 	
 	// implementation of abstract method 
	
	public void optimize(MultivariateFunction f, double[] xvector,
		double tolfx, double tolx)
	{
		t = tolx;
		
		fun = f;
		x = xvector;
		
		checkBounds(x);
		h = step;		

		dim = fun.getNumArguments();;

		d = new double[dim];
		y = new double[dim];
		z = new double[dim];
		q0 = new double[dim];
		q1 = new double[dim];
		v = new double[dim][dim];
		tflin = new double[dim];
		
		small = MachineAccuracy.EPSILON*MachineAccuracy.EPSILON;
		vsmall = small*small;
		large = 1.0/small;
		vlarge = 1.0/vsmall;
		ldfac = (illc ? 0.1 : 0.01);
		nl = kt = 0;
		numFun = 1;
		fx = fun.evaluate(x);
		
		stopCondition(fx, x, tolfx, tolx, true);
		
		qf1 = fx;
		t2 = small + Math.abs(t);
		t = t2;
		dmin = small;

		if (h < 100.0*t) h = 100.0*t;
		ldt = h;
		for (i = 0; i < dim; i++)
		{
			for (j = 0; j < dim; j++)
			{
				v[i][j] = (i == j ? 1.0 : 0.0);
			}
		}
		d[0] = 0.0;
		qd0 = 0.0;
		for (i = 0; i < dim; i++)
			q1[i] = x[i];

		if (prin > 1)
		{
			System.out.println("\n------------- enter function praxis -----------\n");
			System.out.println("... current parameter settings ...");
			System.out.println("... scaling ... " + scbd);
			System.out.println("...   tolx  ... " + t);
			System.out.println("...  tolfx  ... " + tolfx);
			System.out.println("... maxstep ... " + h);
			System.out.println("...   illc  ... " + illc);
			System.out.println("... maxFun  ... " + maxFun);
		}
   		if (prin > 0)
   			System.out.println();

		while(true)
		{
			sf = d[0];
			s = d[0] = 0.0;
	
			/* minimize along first direction */
			
			min1 = d[0];
			min2 = s;
			min(0, 2, fx, false);
			d[0] = min1;
			s = min2;
			
			if (s <= 0.0)
				for (i = 0; i < dim; i++)
				{
					v[i][0] = -v[i][0];
				}	
			if ((sf <= (0.9 * d[0])) || ((0.9 * sf) >= d[0]))
				for (i=1; i < dim; i++)
					d[i] = 0.0;
			
			boolean gotoFret = false;
			for (k=1; k < dim; k++)
			{
				for (i=0; i< dim; i++)
				{
					y[i] = x[i];
				}
				sf = fx;
				illc = illc || (kt > 0);

				boolean gotoNext;
				do
				{
                    if( interrupt ) {
                        return;
                    }

					kl = k;
					df = 0.0;
					if (illc)
					{        /* random step to get off resolution valley */
		          			for (i=0; i < dim; i++)
		          			{
							z[i] = (0.1 * ldt + t2 * Math.pow(10.0,(double)kt)) * (rng.nextDouble() - 0.5);
							s = z[i];
							for (j=0; j < dim; j++)
							{
								x[j] += s * v[j][i];
							}
						}
						
						checkBounds(x);

						fx = fun.evaluate(x);
						numFun++;

                        if( interrupt ) {
                            return;
                        }
						
					}
		       
					/* minimize along non-conjugate directions */ 
					for (k2=k; k2 < dim; k2++)
					{  
						sl = fx;
						s = 0.0;
						
						min1 = d[k2];
						min2 = s;
						min(k2, 2, fx, false);
                        if(interrupt)
                            return;
						d[k2] = min1;
						s = min2;
						
						if (illc)
						{
							double szk = s + z[k2];
		         				s = d[k2] * szk*szk;
			   			}
						else 
							s = sl - fx;
						if (df < s)
						{
							df = s;
							kl = k2;
						}
					}
					
					if (!illc && (df < Math.abs(100.0 * MachineAccuracy.EPSILON * fx)))
					{
						illc = true;
						gotoNext = true;
					}
					else
						gotoNext = false;
				} while (gotoNext);

				
				
	 			if ((k == 1) && (prin > 1))
					vecprint("\n... New Direction ...", d);
				/* minimize along conjugate directions */ 
				for (k2=0; k2<=k-1; k2++)
				{
					s = 0.0;
					
					min1 = d[k2];
					min2 = s;
					min(k2, 2, fx, false);
                    if(interrupt)
                        return;
					d[k2] = min1;
					s = min2;
				}
				f1 = fx;
				fx = sf;
				lds = 0.0;
				for (i=0; i<dim; i++)
				{
					sl = x[i];
					x[i] = y[i];
					y[i] = sl - y[i];
					sl = y[i];
					lds = lds + sl*sl;
				}
				checkBounds(x);
				
				lds = Math.sqrt(lds);
				if (lds > small)
				{
					for (i=kl-1; i>=k; i--)
					{
						for (j=0; j < dim; j++)
							v[j][i+1] = v[j][i];
						d[i+1] = d[i];
					}
					d[k] = 0.0;
					for (i=0; i < dim; i++)
						v[i][k] = y[i] / lds;
					
					min1 = d[k];
					min2 = lds;
					min(k, 4, f1, true);
                    if(interrupt)
                        return;
					d[k] = min1;
					lds = min2;
					
					if (lds <= 0.0)
					{
						lds = -lds;
						for (i=0; i< dim; i++)
							v[i][k] = -v[i][k];
					}
	 			}
				ldt = ldfac * ldt;
				if (ldt < lds)
					ldt = lds;
				if (prin > 1)
					print();
				
				
				if(stopCondition(fx, x, tolfx, tolx, false))
				{
					kt++;
				}
				else
				{
					kt = 0;
				}
				if (kt > 1)
				{
					gotoFret = true;
					break;
				}
				
			}
	   
	   		if (gotoFret) break;
	   
   
			/*  try quadratic extrapolation in case    */
			/*  we are stuck in a curved valley        */
			quadr();
			dn = 0.0;
			for (i=0; i < dim; i++)
			{
				d[i] = 1.0 / Math.sqrt(d[i]);
				if (dn < d[i])
					dn = d[i];
			}
			if (prin > 2)
				matprint("\n... New Matrix of Directions ...",v);
			for (j=0; j < dim; j++)
			{
				s = d[j] / dn;
				for (i=0; i < dim; i++)
					v[i][j] *= s;
			}
			if (scbd > 1.0)
			{       /* scale axis to reduce condition number */
				s = vlarge;
				for (i=0; i < dim; i++)
				{
					sl = 0.0;
					for (j=0; j < dim; j++)
						sl += v[i][j]*v[i][j];
					z[i] = Math.sqrt(sl);
					if (z[i] < MachineAccuracy.SQRT_SQRT_EPSILON)
						z[i] = MachineAccuracy.SQRT_SQRT_EPSILON;
					if (s > z[i])
						s = z[i];
				}
				for (i=0; i < dim; i++)
				{
					sl = s / z[i];
					z[i] = 1.0 / sl;
					if (z[i] > scbd)
					{
						sl = 1.0 / scbd;
						z[i] = scbd;
					}
				}
	   		}
	   		for (i=1; i < dim; i++)
				for (j=0; j<=i-1; j++)
				{
					s = v[i][j];
					v[i][j] = v[j][i];
					v[j][i] = s;
				}
			minfit(dim, MachineAccuracy.EPSILON, vsmall, v, d);
			if (scbd > 1.0)
			{
				for (i=0; i < dim; i++)
				{
					s = z[i];
					for (j=0; j < dim; j++)
						v[i][j] *= s;
				}
				for (i=0; i < dim; i++)
				{
					s = 0.0;
					for (j=0; j < dim; j++)
						s += v[j][i]*v[j][i];
					s = Math.sqrt(s);
					d[i] *= s;
					s = 1.0 / s;
					for (j=0; j < dim; j++)
						v[j][i] *= s;
				}
			}
			for (i=0; i < dim; i++)
			{
				if ((dn * d[i]) > large)
					d[i] = vsmall;
				else if ((dn * d[i]) < small)
					d[i] = vlarge;
				else 
					d[i] = Math.pow(dn * d[i],-2.0);
			}
			sort();               /* the new eigenvalues and eigenvectors */
			dmin = d[dim-1];
			if (dmin < small)
				dmin = small;
			illc = (MachineAccuracy.SQRT_EPSILON * d[0]) > dmin;
			if ((prin > 2) && (scbd > 1.0))
				vecprint("\n... Scale Factors ...",z);
			if (prin > 2)
				vecprint("\n... Eigenvalues of A ...",d);
			if (prin > 2)
				matprint("\n... Eigenvectors of A ...",v);
		
			if ((maxFun > 0) && (nl > maxFun))
			{
				if (prin > 0)
					System.out.println("\n... maximum number of function calls reached ...");
				break;
			}
		}

		if (prin > 0)
		{
			vecprint("\n... Final solution is ...", x);
			System.out.println("\n... Function value reduced to " + fx + " ...");
			System.out.println("... after " + numFun + " function calls.");
		}
	   
		//return (fx);
	}
                                                                	

	//
	// Private stuff
	//

	// some global variables
	private int i, j, k, k2, nl, kl, kt;
	private double s, sl, dn, dmin,
		fx, f1, lds, ldt, sf, df,
		qf1, qd0, qd1, qa, qb, qc, small, vsmall, large, 
		vlarge, ldfac, t2;
		
	// need to be initialised
	private double[] d;
	private double[] y;
	private double[] z;
	private double[] q0;
	private double[] q1;
	private double[][] v;

	private double[] tflin;

	private int dim;	
	private double[] x;
	private MultivariateFunction fun;

	// these will be set by praxis to the global control parameters
	private double h, t;

	// Random number generator
	private MersenneTwisterFast rng;

	// sort d and v in descending order
	private void sort()		
	{
		int k, i, j;
		double s;

		for (i=0; i < dim-1; i++)
		{
			k = i; s = d[i];
			for (j=i+1; j < dim; j++)
			{
				if (d[j] > s)
				{
					k = j;
					s = d[j];
				}
			}
			if (k > i)
       			{
				d[k] = d[i];
				d[i] = s;
				for (j=0; j < dim; j++)
	  			{
					s = v[j][i];
					v[j][i] = v[j][k];
					v[j][k] = s;
				}
			}
		}
	}

	private void vecprint(String s, double[] x)
	{
		System.out.println(s);
		for (int i=0; i < x.length; i++)
			System.out.print(x[i] + "  ");
		System.out.println();
	}

	private void print() /* print a line of traces */
	{
		System.out.println();
		System.out.println("... function value reduced to ... " + fx);
		System.out.println("... after " + numFun + " function calls ...");
		System.out.println("... including " + nl + " linear searches ...");
		vecprint("... current values of x ...", x);
	}

	private void matprint(String s, double[][] v)
	{
		System.out.println(s);
		for (int k=0; k<v.length; k++)
		{
			for (int i=0; i<v.length; i++)
			{
				System.out.print(v[k][i] + " ");
			}
			System.out.println();
		}
	}

	private double flin(double l, int j)
	{
		if (j != -1)
		{ /* linear search */
			for (int i = 0; i < dim; i++)
				tflin[i] = x[i] + l*v[i][j];
		}
		else
		{ /* search along parabolic space curve */
			qa = l*(l-qd1)/(qd0*(qd0+qd1));
			qb = (l+qd0)*(qd1-l)/(qd0*qd1);
			qc = l*(l+qd0)/(qd1*(qd0+qd1));
			for (int i = 0; i < dim; i++)
			{
				tflin[i] = qa*q0[i]+qb*x[i]+qc*q1[i];
			}
		}
		
		checkBounds(tflin);
		
		numFun++;
		return fun.evaluate(tflin);
	}

	private void checkBounds(double[] p)
	{
		for (int i = 0; i < dim; i++)
		{
			if (p[i] < fun.getLowerBound(i))
			{
				p[i] = fun.getLowerBound(i);
			}
			if (p[i] > fun.getUpperBound(i))
			{
				p[i] = fun.getUpperBound(i);
			}
		}
	}

	private double min1;
	private double min2;

	private void min(int j, int nits, double f1, boolean fk)
	{
		int k;
		double x2, xm, f0, f2, fm, d1, t2, s, sf1, sx1;

		sf1 = f1; sx1 = min2;
		k = 0; xm = 0.0; fm = f0 = fx;
		boolean dz = (min1 < MachineAccuracy.EPSILON);

		/* find step size */
		s = 0;
		for (int i=0; i < dim; i++)
		{
			s += x[i]*x[i];
		}
		s = Math.sqrt(s);
		if (dz)
		{
			t2 = MachineAccuracy.SQRT_SQRT_EPSILON*
				Math.sqrt(Math.abs(fx)/dmin + s*ldt) +
				MachineAccuracy.SQRT_EPSILON*ldt;
		}
		else
		{
			t2 = MachineAccuracy.SQRT_SQRT_EPSILON*
				Math.sqrt(Math.abs(fx)/(min1) + s*ldt) +
				MachineAccuracy.SQRT_EPSILON*ldt;
		}
		s = s*MachineAccuracy.SQRT_SQRT_EPSILON + t;
		if (dz && t2 > s)
			t2 = s;
		if (t2 < small)
			t2 = small;
		if (t2 > 0.01*h)
			t2 = 0.01 * h;
		if (fk && f1 <= fm)
		{
			xm = min2;
			fm = f1;
		}
		if (!fk || Math.abs(min2) < t2)
		{
			min2 = (min2 > 0 ? t2 : -t2);
            f1 = flin(min2, j);
            if(interrupt)
                return;           
		}
		if (f1 <= fm)
		{
			xm = min2;
			fm = f1;
		}

		boolean gotoNext;
		do
		{
			if (dz)
			{
				x2 = (f0 < f1 ? -(min2) : 2*(min2));
				f2 = flin(x2, j);
                if(interrupt)
                    return;

				if (f2 <= fm)
				{
					xm = x2;
					fm = f2;
				}
				min1 = (x2*(f1-f0) - (min2)*(f2-f0))/((min2)*x2*((min2)-x2));
			}
			d1 = (f1-f0)/(min2) - min2*min1;
			dz = true;
			if (min1 <= small)
			{
				x2 = (d1 < 0 ? h : -h);
			}
			else
			{
				x2 = - 0.5*d1/(min1);
			}
			if (Math.abs(x2) > h)
				x2 = (x2 > 0 ? h : -h);

			f2 = flin(x2, j);
            if(interrupt)
                return;
		
			gotoNext = false;
		
			while ((k < nits) && (f2 > f0))
			{
				k++;
				if ((f0 < f1) && (min2*x2 > 0.0))
				{
					gotoNext = true;
					break;
				}
				
				
				x2 *= 0.5;
				f2 = flin(x2, j);
                if(interrupt)
                    return;
			}
		} while (gotoNext);
		

		nl++;
		if (f2 > fm)
			x2 = xm;
		else fm = f2;
		if (Math.abs(x2*(x2-min2)) > small)
		{
			min1 = (x2*(f1-f0) - min2*(fm-f0))/(min2*x2*(min2-x2));
		}
		else
		{
			if (k > 0) min1 = 0;
		}
		if (min1 <= small)
			min1 = small;
		min2 = x2; fx = fm;
		if (sf1 < fx)
		{
			fx = sf1;
			min2 = sx1;
		}
		if (j != -1)
		{
			for (i=0; i < dim; i++)
			{
				x[i] += (min2)*v[i][j];
			}
			checkBounds(x);
		}
	}

	// Look for a minimum along the curve q0, q1, q2
	private void quadr()	
	{
		int i;
		double l, s;

		s = fx; fx = qf1; qf1 = s; qd1 = 0.0;
		for (i=0; i < dim; i++)
		{
			s = x[i]; l = q1[i]; x[i] = l; q1[i] = s;
			qd1 = qd1 + (s-l)*(s-l);
		}
		s = 0.0; qd1 = Math.sqrt(qd1); l = qd1;
		if (qd0>0.0 && qd1>0.0 &&nl>=3*dim*dim)
		{
			min1 = s;
			min2 = l;
			min(-1, 2, qf1, true);
			s = min1;
			l = min2;
			
			qa = l*(l-qd1)/(qd0*(qd0+qd1));
			qb = (l+qd0)*(qd1-l)/(qd0*qd1);
			qc = l*(l+qd0)/(qd1*(qd0+qd1));
		}
		else
		{
			fx = qf1; qa = qb = 0.0; qc = 1.0;
		}
		qd0 = qd1;
		for (i=0; i<dim; i++)
		{
			s = q0[i]; q0[i] = x[i];
			x[i] = qa*s + qb*x[i] + qc*q1[i];
		}
		
		checkBounds(x);
	}

	// Singular value decomposition
	private void minfit(int n, double eps, double tol, double[][] ab, double[] q)
	{
		int l=0, kt, l2, i, j, k;
		double c, f, g, h, s, x, y, z;

		double[] e = new double[dim];


		/* householder's reduction to bidiagonal form */
		x = g = 0.0;
		for (i=0; i<n; i++)
		{
			e[i] = g; s = 0.0; l = i+1;
			for (j=i; j<n; j++)
				s += ab[j][i] * ab[j][i];
			if (s < tol)
			{
				g = 0.0;
			}
			else
			{
				f = ab[i][i];
				if (f < 0.0) 
					g = Math.sqrt(s);
				else
					g = -Math.sqrt(s);
				h = f*g - s; ab[i][i] = f - g;
				for (j=l; j<n; j++)
				{
					f = 0.0;
					for (k=i; k<n; k++)
						f += ab[k][i] * ab[k][j];
					f /= h;
					for (k=i; k<n; k++)
						ab[k][j] += f * ab[k][i];
				}
			}
			q[i] = g; s = 0.0;
			if (i < n)
				for (j=l; j<n; j++)
					s += ab[i][j] * ab[i][j];
			if (s < tol)
			{
				g = 0.0;
			}
			else
		    	{
				f = ab[i][i+1];
				if (f < 0.0)
					g = Math.sqrt(s);
				else 
					g = - Math.sqrt(s);
				h = f*g - s;
				ab[i][i+1] = f - g;
				for (j=l; j<n; j++)
			    		e[j] = ab[i][j]/h;
				for (j=l; j<n; j++)
				{
					s = 0;
					for (k=l; k<n; k++)
						s += ab[j][k]*ab[i][k];
					for (k=l; k<n; k++)
						ab[j][k] += s * e[k];
				}
			}
			y = Math.abs(q[i]) + Math.abs(e[i]);
			if (y > x)
				x = y;
		}
		/* accumulation of right hand transformations */
		for (i=n-1; i >= 0; i--)
		{
			if (g != 0.0)
			{
				h = ab[i][i+1]*g;
		   		for (j=l; j<n; j++) ab[j][i] = ab[i][j] / h;
		    		for (j=l; j<n; j++)
				{
			   		s = 0.0;
					for (k=l; k<n; k++) s += ab[i][k] * ab[k][j];
					for (k=l; k<n; k++) ab[k][j] += s * ab[k][i];
				}
			}
			for (j=l; j<n; j++)
			ab[i][j] = ab[j][i] = 0.0;
			ab[i][i] = 1.0; g = e[i]; l = i;
		}
		/* diagonalization to bidiagonal form */
		eps *= x;
		boolean converged = false;
		for (k=n-1; k>= 0; k--)
		{
			kt = 0;

			do
			{
				kt++;

				boolean skipNext = false;
				for (l2=k; l2>=0; l2--)
				{
					l = l2;
					if (Math.abs(e[l]) <= eps)
					{
						skipNext = true;
						break;
					}
			    
					if (Math.abs(q[l-1]) <= eps)
						break;
				}
		
				if (skipNext == false)
				{
			
					c = 0.0; s = 1.0;
					for (i=l; i<=k; i++)
					{
						f = s * e[i]; e[i] *= c;
						if (Math.abs(f) <= eps)
							break;
						g = q[i];
						if (Math.abs(f) < Math.abs(g))
						{
							double fg = f/g;
							h = Math.abs(g)*Math.sqrt(1.0+fg*fg);
					   	}
						else
						{
							double gf = g/f;
							h = (f!=0.0 ? Math.abs(f)*Math.sqrt(1.0+gf*gf) : 0.0);
						}
						q[i] = h;
						if (h == 0.0)
						{
							h = 1.0; g = 1.0;
						}
						c = g/h; s = -f/h;
					}
				}
			
				z = q[k];
				if (l == k)
				{
					converged = true;
					break;
				}
			
			
				/* shift from bottom 2x2 minor */
				x = q[l]; y = q[k-l]; g = e[k-1]; h = e[k];
				f = ((y-z)*(y+z) + (g-h)*(g+h)) / (2.0*h*y);
				g = Math.sqrt(f*f+1.0);
				if (f <= 0.0)
					f = ((x-z)*(x+z) + h*(y/(f-g)-h))/x;
				else
					f = ((x-z)*(x+z) + h*(y/(f+g)-h))/x;
				/* next qr transformation */
				s = c = 1.0;
				for (i=l+1; i<=k; i++)
				{
					g = e[i]; y = q[i]; h = s*g; g *= c;
					if (Math.abs(f) < Math.abs(h))
					{
						double fh = f/h;
						z = Math.abs(h) * Math.sqrt(1.0 + fh*fh);
					}
					else
					{
						double hf = h/f;
						z = (f!=0.0 ? Math.abs(f)*Math.sqrt(1.0+hf*hf) : 0.0);
					}
					e[i-1] = z;
					if (z == 0.0) 
						f = z = 1.0;
					c = f/z; s = h/z;
					f = x*c + g*s; g = - x*s + g*c; h = y*s;
					y *= c;
					for (j=0; j<n; j++)
					{
						x = ab[j][i-1]; z = ab[j][i];
						ab[j][i-1] = x*c + z*s;
						ab[j][i] = - x*s + z*c;
					}
					if (Math.abs(f) < Math.abs(h))
					{
						double fh = f/h;
						z = Math.abs(h) * Math.sqrt(1.0 + fh*fh);
					}
					else
					{
						double hf = h/f;
						z = (f!=0.0 ? Math.abs(f)*Math.sqrt(1.0+hf*hf) : 0.0);
					}
					q[i-1] = z;
					if (z == 0.0)
						z = f = 1.0;
					c = f/z; s = h/z;
					f = c*g + s*y; x = - s*g + c*y;
				}
				e[l] = 0.0; e[k] = f; q[k] = x;
       
       
			} while (kt <= 30);
       
			if (!converged)
			{
				e[k] = 0.0;
				// System.out.println("\n+++ qr failed\n");
				// System.exit(1);
				throw new OptimizationError("ConjugateDirectionSearch: +++ qr failed");
			}

 
			if (z < 0.0)
			{
				q[k] = - z;
				for (j=0; j<n; j++)
					ab[j][k] = - ab[j][k];
			}
		}
	}
}
