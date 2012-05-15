// ErrorFunction.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * error function and related stuff
 *
 * @version $Id: ErrorFunction.java,v 1.1 2006/06/01 15:27:32 gene099 Exp $
 *
 * @author Korbinian Strimmer
 */
public class ErrorFunction
{
	//
	// Public stuff
	//

	/**
	 * error function
	 *
	 * @param x argument
	 *
	 * @return function value
	 */
	public static double erf(double x)
	{
		if (x > 0.0)
		{
			return GammaFunction.incompleteGammaP(0.5, x*x);
		}
		else if (x < 0.0)
		{
			return -GammaFunction.incompleteGammaP(0.5, x*x);
		}
		else
		{
			return 0.0;
		}
	}
	
	/**
	 * complementary error function = 1-erf(x)
	 *
	 * @param x argument
	 *
	 * @return function value
	 */
	public static double erfc(double x)
	{
		return 1.0-erf(x);
	}

	
	/**
	 * inverse error function
	 *
	 * @param z argument
	 *
	 * @return function value
	 */
	public static double inverseErf(double z)
	{
		return pointNormal(0.5*z+0.5)/Math.sqrt(2.0);
	}


	// Private

	// Returns z so that Prob{x<z}=prob where x ~ N(0,1) and (1e-12) < prob<1-(1e-12)
	private static double pointNormal(double prob)
	{
		// Odeh RE & Evans JO (1974) The percentage points of the normal distribution.
		// Applied Statistics 22: 96-97 (AS70)
	
		// Newer methods:
		// Wichura MJ (1988) Algorithm AS 241: the percentage points of the
		// normal distribution.  37: 477-484.
		// Beasley JD & Springer SG  (1977).  Algorithm AS 111: the percentage 
		// points of the normal distribution.  26: 118-121.
	
		double a0 = -0.322232431088, a1 = -1, a2 = -0.342242088547, a3 = -0.0204231210245;
		double a4 = -0.453642210148e-4, b0 = 0.0993484626060, b1 = 0.588581570495;
		double b2 = 0.531103462366, b3 = 0.103537752850, b4 = 0.0038560700634;
		double y, z = 0, p = prob, p1;
	
		p1 = (p < 0.5 ? p : 1-p);
		if (p1 < 1e-20)
		{
			new IllegalArgumentException("Argument prob out of range");
		}
	
		y = Math.sqrt(Math.log(1/(p1*p1)));   
		z = y + ((((y*a4+a3)*y+a2)*y+a1)*y+a0)/((((y*b4+b3)*y+b2)*y+b1)*y+b0);
		return (p < 0.5 ? -z : z);
	}
}
