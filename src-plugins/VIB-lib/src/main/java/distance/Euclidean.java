/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package distance;

public class Euclidean implements PixelPairs {

	private float sum;
	private long count;
	private static boolean verbose = !true;

	public void reset() {
		sum=0;
		count=0;
	}

	public void add(float v1, float v2) {
		if (verbose)
			System.err.println("got " + v1 + " and " + v2);
		double diff=v1-v2;
		sum+=diff*diff;
		count++;
	}

	public float distance() {
		if (verbose) {
			System.err.println("calculated sum: " + Math.sqrt(sum/count));
			verbose = false;
		}
		return (float)Math.sqrt(sum/count);
	}
}
