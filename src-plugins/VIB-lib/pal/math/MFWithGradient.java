// MFWithGradient.java
//
// (c) 2000-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * interface for a function of several variables with a gradient
 *
 * @author Korbinian Strimmer
 */
public interface MFWithGradient extends MultivariateFunction
{
	/**
	 * compute both function value and gradient at a point
	 *
	 * @param argument  function argument (vector)
	 * @param gradient  gradient (on return)
	 *
	 * @return function value
	 */
	double evaluate(double[] argument, double[] gradient);
	
	/**
	 * compute gradient at a point
	 *
	 * @param argument  function argument (vector)
	 * @param gradient  gradient (on return)
	 */
	void computeGradient(double[] argument, double[] gradient);
}
