// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;

/**
 * converts a multivariate function into a univariate function
 *
 * @author Korbinian Strimmer
 */
public class LineFunction implements UnivariateFunction
{
	/**
	 * construct univariate function from multivariate function
	 *
	 * @param func multivariate function
	 * @param start start point
	 * @param dir direction vector
	 */
	public LineFunction(MultivariateFunction func)
	{
		f = func;
		
		dim = f.getNumArguments();
		
		x = new double[dim];
	}

	/**
	 * update start point and direction
	 * (bounds and search direction are NOT checked)
	 *
	 * @param start new start point
	 * @param dir new direction vector
	 */
	public void update(double[] start, double[] dir)
	{
		s = start;
		d = dir;
		
		computeBounds();
	}

	
	/**
	 * get point associated with the one-dimensional parameter
	 * (bounds of of multivariate function are NOT checked)
	 *
	 * @param lambda argument
	 * @param p array for coordinates of corresponding point
	 */  
	public void getPoint(double lambda, double[] p)
	{
		for (int i = 0; i < dim; i++)
		{
			p[i] = s[i] + lambda*d[i];
		}
	}

	
	// implementation of UnivariateFunction
	
	/**
	 * evaluate f(start+lambda*dir)
	 */
	public double evaluate(double lambda)
	{
		getPoint(lambda, x);
		
		return f.evaluate(x);
	}

	public double getLowerBound()
	{		
		return lowerBound;
	}

	public double getUpperBound()
	{
		return upperBound;
	}
	
	/**
	 * find parameter lambda within the given bounds
	 * that minimizes the univariate function
	 * (due to numerical inaccuaries it may happen
	 * that getPoint for the returned lambda produces
	 * a point that lies
	 * slightly out of bounds)
	 *
	 * @return lambda that achieves minimum
	 */
	public double findMinimum()
	{
		if (um == null)
		{
			um = new UnivariateMinimum();
		}
		return um.findMinimum(this);
	}


	/**
	 * get parameter that limits the upper bound
	 *
	 * @return parameter number 
	 */
	public int getUpperBoundParameter()
	{
		return upperBoundParam;
	}

	/**
	 * get parameter that limits the lower bound
	 *
	 * @return parameter number 
	 */
	public int getLowerBoundParameter()
	{
		return lowerBoundParam;
	}


	/**
	 * check (and modify, if necessary) whether a point lies properly
	 * within the predefined bounds
	 *
	 * @param p coordinates of point
	 *
	 * @return true if p was modified, false otherwise
	 */
	public boolean checkPoint(double[] p)
	{
		boolean modified = false; 
		for (int i = 0; i < dim; i++)
		{
			if (p[i] < f.getLowerBound(i))
			{
				p[i] = f.getLowerBound(i);
				modified = true;
			}
			if (p[i] > f.getUpperBound(i))
			{
				p[i] = f.getUpperBound(i);
				modified = true;
			}
		}
		
		return modified;
	}

	/**
	 * determine active variables at a point p and corresponding
	 * gradient grad (if a component of p lies on a border and
	 * the corresponding component of the gradient points
	 * out of the border the variable is considered inactive)
	 *
	 * @param p coordinates of point
	 * @param grad gradient at that point
	 * @param list of active variables (on return)
	 *
	 * @return number of active variables
	 */
	public int checkVariables(double[] p, double[] grad, boolean[] active)
	{
		// this seems to be a reasonable small value
		double EPS = MachineAccuracy.SQRT_EPSILON;
		
		int numActive = 0;
		for (int i = 0; i < dim; i++)
		{
			active[i] = true;
			if (p[i] <= f.getLowerBound(i)+EPS)
			{
				// no search towards lower boundary
				if (grad[i] > 0) 
				{
					active[i] = false;
				}
			}
			else if (p[i] >= f.getUpperBound(i)-EPS)
			{
				// no search towards upper boundary
				if (grad[i] < 0)
				{
					active[i] = false;
				}
			}
			else
			{
				numActive++;
			}
		}
		
		return numActive;
	}

	/**
	 * check direction vector. If it points out of the defined
	 * area at a point at the boundary the corresponding component
	 * of the direction vector is set to zero. 
	 *
	 * @param p coordinates of point
	 * @param dir direction vector at that point
	 *
	 * @return number of changed components in direction vector
	 */
	public int checkDirection(double[] p, double[] dir)
	{
		// this seems to be a reasonable small value
		double EPS = MachineAccuracy.SQRT_EPSILON;
		
		int numChanged = 0;
		for (int i = 0; i < dim; i++)
		{
			if (p[i] <= f.getLowerBound(i)+EPS)
			{
				// no search towards lower boundary
				if (dir[i] < 0) 
				{
					dir[i] = 0;
					numChanged++;
				}
			}
			else if (p[i] >= f.getUpperBound(i)-EPS)
			{
				// no search towards upper boundary
				if (dir[i] > 0)
				{
					dir[i] = 0;
					numChanged++;
				}
			}
		}
		
		return numChanged;
	}


	//
	// Private stuff
	//
	
	private MultivariateFunction f;
	private int lowerBoundParam, upperBoundParam;
	private int dim;
	private double lowerBound, upperBound;
	private double[] s, d, x, min, max;
	private UnivariateMinimum um = null;
	
	private void computeBounds()
	{
		boolean firstVisit = true;
		for (int i = 0; i < dim; i++)
		{
			if (d[i] != 0)
			{
				double upper = (f.getUpperBound(i) - s[i])/d[i];
				double lower = (f.getLowerBound(i) - s[i])/d[i];			
				if (lower > upper)
				{
					double tmp = upper;
					upper = lower;
					lower = tmp;
				}
				
				if (firstVisit)
				{
					lowerBound = lower;
					lowerBoundParam = i;
					upperBound = upper;
					upperBoundParam = i;
					firstVisit = false;
				}
				else
				{
					if (lower > lowerBound)
					{
						lowerBound = lower;
						lowerBoundParam = i;
					}
					if (upper < upperBound)
					{
						upperBound = upper;
						upperBoundParam = i;
					}
				}
			}
		}
	}
}
