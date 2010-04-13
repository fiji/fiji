// UnivariateMinimum.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * minimization of a real-valued function of one variable
 * without using derivatives.
 *
 * <p>algorithm: Brent's golden section method
 * (Richard P. Brent.  1973.   Algorithms for finding zeros and extrema
 *  of functions without calculating derivatives.  Prentice-Hall.)
 *
 * @author Korbinian Strimmer
 */
public class UnivariateMinimum
{
	//
	// Public stuff
	//

	/** last minimum */
	public double minx;
	
	/** function value at minimum */
	public double fminx;
	
	/** curvature at minimum */
	public double f2minx; 
	
	/** total number of function evaluations neccessary */
	public int numFun;
	
	/**
	 * maximum number of function evaluations
	 * (default 0 indicates no limit on calls)
	 */
	public int maxFun = 0;

	/**
	 * Find minimum
	 * (first estimate given)
	 *
	 * @param x   first estimate
	 * @param f   function
	 * 
	 * @return position of minimum
	 */
	public double findMinimum(double x, UnivariateFunction f)
	{
		double tol = MachineAccuracy.EPSILON;
		
		return optimize(x, f, tol);
	}

	/**
	 * Find minimum 
	 * (first estimate given, desired number of fractional digits specified)
	 *
	 * @param x   first estimate
	 * @param f   function
	 * @param fracDigits desired fractional digits
	 * 
	 * @return position of minimum
	 */
	public double findMinimum(double x, UnivariateFunction f, int fracDigits)
	{
		double tol = Math.pow(10, -1-fracDigits);
		
		double optx = optimize(x, f, tol);
		
		//return trim(optx, fracDigits);
		return optx;
	}

	/**
	 * Find minimum
	 * (no first estimate given)
	 *
	 * @param f   function
	 * 
	 * @return position of minimum
	 */
	public double findMinimum(UnivariateFunction f)
	{
		double tol = MachineAccuracy.EPSILON;
				
		return optimize(f, tol);
	}

	/**
	 * Find minimum
	 * (no first estimate given, desired number of fractional digits specified)
	 *
	 * @param f   function
	 * @param fracDigits desired fractional digits
	 * 
	 * @return position of minimum
	 */
	public double findMinimum(UnivariateFunction f, int fracDigits)
	{
		double tol = Math.pow(10, -1-fracDigits);
		
		double optx = optimize(f, tol);
		
		//return trim(optx, fracDigits);
		return optx; 
	}

	/**
	 * The actual optimization routine (Brent's golden section method)
         *
	 * @param f univariate function
	 * @param tol absolute tolerance of each parameter
	 *
	 * @return  position of minimum
 	 */
	public double optimize(UnivariateFunction f, double tol)
	{
		numFun = 2;
		double min = f.getLowerBound();
		double max = f.getUpperBound();
		
		return minin(min, max, f.evaluate(min), f.evaluate(max), f, tol);
	}

	/**
	 * The actual optimization routine (Brent's golden section method)
         *
	 * @param x initial guess
	 * @param f univariate function
	 * @param tol absolute tolerance of each parameter
	 *
	 * @return  position of minimum
 	 */
	public double optimize(double x, UnivariateFunction f, double tol)
	{
		double[] range = bracketize(f.getLowerBound(), x, f.getUpperBound(), f);

		return minin(range[0], range[1], range[2], range[3], f, tol);
	}

	//
	// Private stuff
	//

	private static final double C = (3.0- Math.sqrt(5.0))/2.0; // = 0.38197
	private static final double GOLD = (Math.sqrt(5.0) + 1.0)/2.0; // = 1.61803
	private static final double delta = 0.01; // Determines second trial point

	// trim x to have a specified number of fractional digits
	private double trim(double x, int fracDigits)
	{
		double m = Math.pow(10, fracDigits);
		
		return Math.round(x*m)/m;
	}
	
	private double constrain(double x, boolean toMax, double min, double max)
	{
		if (toMax)
		{
			if (x > max)
			{
				return max;
			}
			else
			{
				return x;
			}
		}
		else
		{
			if (x < min)
			{
				return min;
			}
			else
			{
				return x;
			}
		}
	}	

	private double[] bracketize(double min, double a, double max, UnivariateFunction f)
	{
		if (min > max)
		{
			throw new IllegalArgumentException("Argument min (" + min +
			") larger than argument max (" + max + ")");
		}
		
		if (a < min)
		{
			a = min;
		}
		else if (a > max)
		{
			a = max;
		}


		if (a < min || a > max)
		{
			throw new IllegalArgumentException("Starting point not in given range ("
			+ min + ", " + a + ", " + max + ")");
		}
		
		
		// Get second point
		double b;
		if (a - min < max - a)
		{
			b = a + delta*(max - a);
		}
		else
		{
			b = a - delta*(a - min);
		}
	
		numFun = 0;
    
		double fa = f.evaluate(a); numFun++;
		double fb = f.evaluate(b); numFun++;
		
		double tmp;
		if (fb > fa)
		{
			tmp = a; a = b; b = tmp;
			tmp = fa; fa = fb; fb = tmp;
		}
		
		// From here on we always have fa >= fb
		// Our aims is to determine a new point c with fc >= fb
		
		// Direction of search (towards min or towards max)
		boolean searchToMax;
		double ulim;
		if (b > a)
		{
			searchToMax = true;
			ulim = max;
		}
		else
		{
			searchToMax = false;
			ulim = min;
		}
		
		// First guess: default magnification
		double c = b + GOLD * (b - a);
		c = constrain(c, searchToMax, min, max);
		double fc = f.evaluate(c); numFun++;
        
		while (fb > fc)
		{
			// Compute u as minimum of a parabola through a, b, c
			double r = (b - a) * (fb - fc);
			double q = (b - c) * (fb - fa);
			if (q == r)
			{
                		q += MachineAccuracy.EPSILON;
			}
			double u = b - ((b - c) * q - (b - a) * r) / 2.0 / (q - r);
			u = constrain(u, searchToMax, min, max);
			double fu = 0; // Don't evaluate now
			
			boolean magnify = false;
			
			// Check out all possibilities
			
			// u is between b and c
			if ((b - u) * (u - c) > 0)
			{
				fu = f.evaluate(u); numFun++;
				
				// minimum between b and c
				if (fu < fc)
				{
					a = b; b = u;
					fa = fb; fb = fu;
					
					break;
				}
				// minimum between a and u
				else if (fu > fb)
				{
					c = u;
					fc = fu;
					
					break;
				}
				
				magnify = true;
            		}
			// u is between c and limit
			else if ((c - u) * (u - ulim) > 0)
			{
				fu = f.evaluate(u); numFun++;
				
				// u is not a minimum
				if (fu < fc)
				{
					b = c; c = u;
					fb = fc; fc = fu; 
					
					magnify = true;
				}
            		}
			//  u equals limit
			else if (u == ulim)
			{
				fu = f.evaluate(u); numFun++;
			}
			// All other cases
			else
			{
				magnify = true;
			}

			if (magnify)
			{
				// Next guess: default magnification
				u = c + GOLD * (c - b);
				u = constrain(u, searchToMax, min, max);
				fu = f.evaluate(u); numFun++;
			}

			a = b; b = c; c = u;
			fa = fb; fb = fc; fc = fu;
		}
		
		// Once we are here be have a minimum in [a, c]
		double[] result = new double[4];
		result[0] = a;
		result[1] = c;
		result[2] = fa;
		result[3] = fc;
		return result;
	}


	private double minin(double a, double b, double fa , double fb, UnivariateFunction f, double tol)
	{
		double z, d = 0, e, m, p, q, r, t, u, v, w, fu, fv, fw, fz, tmp;

		if (tol <= 0)
		{
			throw new IllegalArgumentException("Nonpositive absolute tolerance tol");
		}

		if (a == b)
		{
			minx = a;
			fminx = fa;
		
			f2minx = NumericalDerivative.secondDerivative(f, minx);
		
			return  minx;

			//throw new IllegalArgumentException("Borders of range not distinct");
		}

		if (b < a)
		{
			tmp = a; a = b; b = tmp;
			tmp = fa; fa = fb; fb = tmp;
		}

		w = a; fw = fa;
		z = b; fz = fb;
		if (fz > fw) // Exchange z and w
		{
			v = z; z = w; w = v;
			v = fz; fz = fw; fw = v;
		}
		v = w;
		fv = fw;
		e = 0.0;
		while (maxFun == 0 || numFun <= maxFun)
		{
			m = (a + b)*0.5;
			double tol_act = MachineAccuracy.SQRT_EPSILON + tol; // Absolute tolerance
			//double tol_act = MachineAccuracy.SQRT_EPSILON*Math.abs(z) + tol/3; // Actual tolerance
			double tol_act2 = 2.0*tol_act;
			if (Math.abs(z-m) <= tol_act2-(b - a)*0.5)
			{
				break;
			}
			p = q = r = 0.0;
			if (Math.abs(e) > tol_act)
			{
				r = (z-w)*(fz-fv);
				q = (z-v)*(fz-fw);
				p = (z-v)*q-(z-w)*r;
				q = (q-r)*2.0;
				if (q > 0.0)
				{
					p = -p;
				}
				else
				{
					q = -q;
				}
				r = e;
				e = d;
			}
			if (Math.abs(p) < Math.abs(q*r*0.5) && p > (a-z)*q && p < (b-z)*q)
			{
				d = p/q;
				u = z+d;
				if (u-(a) < tol_act2 || (b)-u < tol_act2)
				{
					d = ((z < m) ? tol_act : -tol_act);
				}
			}
			else
			{
				e = ((z < m) ? b : a) - z;
				d = C*e;
			}
			u = z + ((Math.abs(d) >= tol_act) ? d : ((d > 0.0) ? tol_act : -tol_act));
			fu = f.evaluate(u); numFun++;
			if (fu <= fz)
			{
				if (u < z)
				{
					b = z;
				}
				else
				{
					a = z;
				}
				v = w;
				fv = fw;
				w = z;
				fw = fz;
				z = u;
				fz = fu;
			}
			else
			{
				if (u < z)
				{
					a = u;
				}
				else
				{
					b = u;
				}
				if (fu <= fw)
				{
					v = w; fv = fw;
					w = u; fw = fu;
				}
				else if (fu <= fv || v == w)
				{
					v = u;
					fv = fu;
				}
			}
		}
		minx = z;
		fminx = fz;
		
		f2minx = NumericalDerivative.secondDerivative(f, minx);
		
		return  z;
	}
}
