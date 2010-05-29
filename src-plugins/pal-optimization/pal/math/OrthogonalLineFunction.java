// OrthogonalLineFunction.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * converts a multivariate function into a univariate function
 * by keeping all but one argument constant
 *
 * @author Korbinian Strimmer
 */
public class OrthogonalLineFunction implements UnivariateFunction
{
	/**
	 * construct univariate function from multivariate function
	 *
	 * @param func multivariate function
	 */
	public OrthogonalLineFunction(MultivariateFunction func)
	{
		f = func;
		numArgs = f.getNumArguments();
		x = new double[numArgs];
	}

	/**
	 * set (change) values of all arguments (start values)
	 *
	 * @param start start values
	 */
	public void setAllArguments(double[] start)
	{
		for (int i = 0; i < numArgs; i++)
		{
			x[i] = start[i];
		}
	}

	/**
	 * set (change) value of a single argument
	 * (the one currently active)
	 *
	 * @param val value of argument
	 */
	public void setArgument(double val)
	{
		x[n] = val;
		bak = x[n];
	}


	/**
	 * use only the specified argument in the
	 * constructed univariate function
	 * and keep all others constant
	 *
	 * @param num argument number
	 */
	public void selectArgument(int num)
	{
		n = num;
		bak = x[n];
	}
	
	// implementation of UnivariateFunction
	
	public double evaluate(double arg)
	{
		x[n] = arg;
		double v = f.evaluate(x);
		x[n] = bak;
		
		return v;
	}

	public double getLowerBound()
	{
		return f.getLowerBound(n);
	}

	public double getUpperBound()
	{
		return f.getUpperBound(n);
	}


	//
	// Private stuff
	//
	
	private MultivariateFunction f;
	private int numArgs, n;
	private double bak;
	private double[] s, x;
}
