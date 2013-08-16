package distance;

/*
 * Treat pixels as different when one is below threshold, and the other above.
 */
public class Thresholded implements PixelPairs {
	private float threshold;
	private long count, total;

	public Thresholded(int threshold) {
		this.threshold = threshold;
	}

	public void reset() {
		count = total = 0;
	}

	public void add(float v1, float v2) {
		total++;
		if (v1 <= threshold) {
			if (v2 > threshold)
				count++;
		} else if (v2 <= threshold)
			count++;
	}

	public float distance() {
		return count * 255 / (float)total;
	}
}

