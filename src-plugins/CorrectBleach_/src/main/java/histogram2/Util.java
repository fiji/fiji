/**
 * This sample code is made available as part of the book "Digital Image
 * Processing - An Algorithmic Introduction using Java" by Wilhelm Burger
 * and Mark J. Burge, Copyright (C) 2005-2008 Springer-Verlag Berlin,
 * Heidelberg, New York.
 * Note that this code comes with absolutely no warranty of any kind.
 * See http://www.imagingbook.com for details and licensing conditions.
 *
 * Date: 2007/11/10
 * ------
 * the code taken from the trextbook website.
 *
 */

package histogram2;

public class Util {

	static int[] makeGaussianHistogram () {
		return makeGaussianHistogram(128, 50);
	}

	public static int[] makeGaussianHistogram (double mean, double sigma) {
		int[] h = new int[256];
		double sigma2 = 2 * sigma * sigma;
		for (int i=0; i<h.length; i++) {
			double x = mean - i;
			double g = Math.exp(-(x*x)/sigma2) / sigma;
			h[i] = (int) Math.round(10000 * g);
		}
		return h;
	}

	public static double[] normalizeHistogram (double[] h) {
		// find max histogram entry
		double max = h[0];
		for (int i=0; i<h.length; i++) {
			if (h[i] > max)
				max = h[i];
		}
		if (max == 0) return null;
		// normalize
		double[] hn = new double[h.length];
		double s = 1.0/max;
		for (int i=0; i<h.length; i++) {
			hn[i] = s * h[i];
		}
		return hn;
	}

	//------------------------------------------------------

	public static double[] normalizeHistogram (int[] h) {
		// find the max histogram entry
		int max = h[0];
		for (int i=0; i<h.length; i++) {
			if (h[i] > max)
				max = h[i];
		}
		if (max == 0) return null;
		// normalize
		double[] hn = new double[h.length];
		double s = 1.0/max;
		for (int i=0; i<h.length; i++) {
			hn[i] = s * h[i];
		}
		return hn;
	}

	public static double[] Cdf (int[] h) {
		// returns the cumul. probability distribution function (cdf) for histogram h
		int K = h.length;
		int n = 0;		// sum all histogram values
		for (int i=0; i<K; i++)	{
			n += h[i];
		}
		double[] P = new double[K];
		int c = h[0];
		P[0] = (double) c / n;
	    for (int i=1; i<K; i++) {
		c += h[i];
	        P[i] = (double) c / n;
	    }
	    return P;
	}

	static double[] Pdf (int[] h) {
		// returns the probability distribution function (pdf) for histogram h
		int K = h.length;
		int n = 0;			// sum all histogram values
		for (int i=0; i<K; i++)	{
			n += h[i];
		}
		double[] p = new double[K];
		for (int i=0; i<h.length; i++) {
			p[i] =  (double) h[i] / n;
		}
		return p;
	}

}
