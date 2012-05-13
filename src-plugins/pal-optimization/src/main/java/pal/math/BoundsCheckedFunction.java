// BoundsCheckedFunction.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * returns a very large number instead of the function value
 * if arguments are out of bound (useful for minimization with
 * minimizers that don't check argument boundaries)
 *
 * @author Korbinian Strimmer
 */
public class BoundsCheckedFunction implements MultivariateFunction
{	
	/**
	 * construct bound-checked multivariate function
	 * (a large number will be returned on function evaluation if argument
	 * is out of bounds; default is 1000000)
	 *
	 * @param func   unconstrained multivariate function
	 * @param minArg lower constraint
	 * @param maxArg upper constraint
	 */
	public BoundsCheckedFunction(MultivariateFunction func)
	{
		this(func, 1000000);
	}
	
	/**
	 * construct constrained multivariate function
	 *
	 * @param func    unconstrained multivariate function
	 * @param largeNumber  value returned on function evaluation
	 *                     if argument is out of bounds
	 */
	public BoundsCheckedFunction(MultivariateFunction func, double largeNumber)
	{
		f = func;
		veryLarge = largeNumber;
	}
		
	/**
	 * computes function value, taking into account the constraints on the
	 * argument 
	 *
	 * @param x function argument
	 *
	 * @return function value (if argument is not in the predefined constrained area
	 * a very large number is returned instead of the true function value)
	 */
	public double evaluate(double[] x)
	{
		int len = f.getNumArguments();
		
		for (int i = 0; i < len; i++)
		{
			if (x[i] < f.getLowerBound(i) ||
				x[i] > f.getUpperBound(i))
			{
				return veryLarge;
			}
		}
		
		return f.evaluate(x);
	}

	public int getNumArguments()
	{
		return f.getNumArguments();
	}

	public double getLowerBound(int n)
	{
		return f.getLowerBound(n);
	}

	public double getUpperBound(int n)
	{
		return f.getUpperBound(n);
	}


	//
	// Private stuff
	//
	
	private MultivariateFunction f;
	private double veryLarge;
}
