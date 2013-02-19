// NumericalDerivative.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


// Known bugs and limitations:
// - the sparse number of function evaluations used can potentially
//   lead to strong inaccuracies if the function is ill-behaved


package pal.math;


/**
 * approximates numerically the first and second derivatives of a
 * function of a single variable and  approximates gradient and
 * diagonal of Hessian for multivariate functions
 *
 * @author Korbinian Strimmer
 */
public class NumericalDerivative
{
	//
	// Public stuff
	//


	/**
	 * determine first derivative
	 *
	 * @param f univariate function
	 * @param x argument
	 *
	 * @return first derivate at x
	 */
	public static double firstDerivative(UnivariateFunction f, double x)
	{	
		double h = MachineAccuracy.SQRT_EPSILON*(Math.abs(x) + 1.0);

		// Centered first derivative
		return (f.evaluate(x + h) - f.evaluate(x - h))/(2.0*h);
	}

	/**
	 * determine second derivative
	 *
	 * @param f univariate function
	 * @param x argument
	 *
	 * @return second derivate at x
	 */
	public static double secondDerivative(UnivariateFunction f, double x)
	{
		double h = MachineAccuracy.SQRT_SQRT_EPSILON*(Math.abs(x) + 1.0);
	
		// Centered second derivative
		return (f.evaluate(x + h) - 2.0*f.evaluate(x) + f.evaluate(x - h))/(h*h);
	}

	
	/**
	 * determine gradient
	 *
	 * @param f multivariate function
	 * @param x argument vector
	 *
	 * @return gradient at x
	 */
	public static double[] gradient(MultivariateFunction f, double[] x)
	{	
		double[] result = new double[x.length];

		gradient(f, x, result);
		
		return result;
	}

	/**
	 * determine gradient
	 *
	 * @param f multivariate function
	 * @param x argument vector
	 * @param grad vector for gradient
	 */
	public static void gradient(MultivariateFunction f, double[] x, double[] grad)
	{	
		for (int i = 0; i < f.getNumArguments(); i++)
		{
			double h = MachineAccuracy.SQRT_EPSILON*(Math.abs(x[i]) + 1.0);
		
			double oldx = x[i];
			x[i] = oldx + h;
			double fxplus = f.evaluate(x);
			x[i] = oldx - h;
			double fxminus = f.evaluate(x);
			x[i] = oldx;

			// Centered first derivative
			grad[i] = (fxplus-fxminus)/(2.0*h);
		}
	}

	/**
	 * determine diagonal of Hessian
	 *
	 * @param f multivariate function
	 * @param x argument vector
	 *
	 * @return vector with diagonal entries of Hessian
	 */
	public static double[] diagonalHessian(MultivariateFunction f, double[] x)
	{
		int len = f.getNumArguments();
		double[] result = new double[len];

		for (int i = 0; i < len; i++)
		{
			double h = MachineAccuracy.SQRT_SQRT_EPSILON*(Math.abs(x[i]) + 1.0);
		
			double oldx = x[i];
			x[i] = oldx + h;
			double fxplus = f.evaluate(x);
			x[i] = oldx - h;
			double fxminus = f.evaluate(x);
			x[i] = oldx;
			double fx = f.evaluate(x);

			// Centered second derivative
			result[i] = (fxplus - 2.0*fx + fxminus)/(h*h);
		}
		
		return result;
	}
}
