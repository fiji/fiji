// Binomial.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;

/**
 * Binomial coefficients
 *
 * @version $Id: Binomial.java,v 1.1 2006/06/01 15:27:32 gene099 Exp $
 *
 * @author Korbinian Strimmer
 */
public class Binomial implements java.io.Serializable
{
	//
	// Public stuff
	//

	/**
	 * Binomial coefficient n choose k
	 */
	public double choose(double n, double k)
	{
		n = Math.floor(n + 0.5);
		k = Math.floor(k + 0.5);

		double lchoose = GammaFunction.lnGamma(n + 1.0) -
		GammaFunction.lnGamma(k + 1.0) - GammaFunction.lnGamma(n - k + 1.0);

		return Math.floor(Math.exp(lchoose) + 0.5);
	}

	/**
	 * get (precomputed) n choose 2
	 */
	public double getNChoose2(int n)
	{
		return nChoose2[n];
	}

	/**
	 * set capacity and precompute the n choose 2 values
	 */
	public void setMax(int max)
	{
		if (nChoose2 == null)
		{
			precalculate(max);
		}
		else if (max >= nChoose2.length)
		{
			precalculate(Math.max(nChoose2.length * 2, max));
		}
	}


	//
	// private stuff
	//

	private double[] nChoose2 = null;

	/**
	 * pre-calculates n choose 2 up to a given number of lineages, if
	 * not already pre-calculated.
	 */
	private void precalculate(int n) {
	
		nChoose2 = new double[n+1];
	
		for (int i=0; i < (n+1); i++) {
			nChoose2[i] = ((double) (i*(i-1))) * 0.5;
		}
	}	
}
