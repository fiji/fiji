/**
 * This sample code is made available as part of the book "Digital Image
 * Processing - An Algorithmic Introduction using Java" by Wilhelm Burger
 * and Mark J. Burge, Copyright (C) 2005-2008 Springer-Verlag Berlin,
 * Heidelberg, New York.
 * Note that this code comes with absolutely no warranty of any kind.
 * See http://www.imagingbook.com for details and licensing conditions.
 *
 * Date: 2007/11/10
 *
 * ------
 * the code taken from the trextbook website.
 *
 */

package histogram2;

public class HistogramMatcher {
	// hA ... histogram of target image I_A
	// hR ... reference histogram
	// returns the mapping function F() to be applied to image I_A

	public int[] matchHistograms (int[] hA, int[] hR) {
		int K = hA.length;
		double[] PA = Util.Cdf(hA); // get CDF of histogram hA
		double[] PR = Util.Cdf(hR); // get CDF of histogram hR
		int[] F = new int[K]; // pixel mapping function f()

		// compute pixel mapping function f():
		for (int a = 0; a < K; a++) {
			int j = K - 1;
			do {
				F[a] = j;
				j--;
			} while (j >= 0 && PA[a] <= PR[j]);
		}
		return F;
	}

	public int[] matchHistograms(int[] hA, PiecewiseLinearCdf PR) {
		int K = hA.length;
		double[] PA = Util.Cdf(hA); // get p.d.f. of histogram Ha
		int[] F = new int[K]; // pixel mapping function f()

		// compute pixel mapping function f():
		for (int a = 0; a < K; a++) {
			double b = PA[a];
			F[a] = PR.getInverseCdf(b);
		}
		return F;
	}
}
