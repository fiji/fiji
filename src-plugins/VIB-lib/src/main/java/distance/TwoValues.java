package distance;

/*
 * This is to compare one material in one image to one material in the other
 * image. Distance is maximal when one and only one of the pair is the desired
 * material.
 */
public class TwoValues implements PixelPairs {
	/* the values of the materials may be different in the two images */
	public float material1, material2;
	private long count, total;

	public TwoValues(int material1, int material2) {
		this.material1 = material1;
		this.material2 = material2;
	}

	public void reset() {
		count = total = 0;
	}

	public void add(float v1, float v2) {
		total++;
		boolean b1 = (v1 == material1);
		boolean b2 = (v2 == material2);
		if (b1 ^ b2)
			count++;
	}

	public float distance() {
		return count * 255 / (float)total;
	}

	public int getMaterial(int index) {
		return (int)Math.round(index == 0 ? material1 : material2);
	}
}

