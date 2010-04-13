/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package distance;

public class Correlation implements PixelPairs {

	private float sumX, sumY, sumXY,
		sumXSquared, sumYSquared;
	private long count;
	private static boolean verbose = !true;

	public void reset() {
		sumX = sumY = sumXY = 0;
		sumXSquared = sumYSquared = 0;
		count = 0;
	}

	public void add(float v1, float v2) {
		if (verbose)
			System.err.println("got " + v1 + " and " + v2);
		sumX += v1;
		sumY += v2;
		sumXY += v1 * v2;
		sumXSquared += v1 * v1;
		sumYSquared += v2 * v2;
		count++;
	}

	public float correlation() {

		float result = 0;

		float n2 = count * count;
		float numerator = (sumXY/count) - (sumX * sumY) / n2;
		float varX = (sumXSquared / count) - (sumX * sumX) / n2;
		float varY = (sumYSquared / count) - (sumY * sumY) / n2;
		float denominator = (float) (
			Math.sqrt(varX) * Math.sqrt(varY) );

		if( denominator > 0.00000001 ) {
			result = numerator / denominator;
		}
		
		return result;
	}

	public float distance() {
		return 1 - correlation();
	}

}
