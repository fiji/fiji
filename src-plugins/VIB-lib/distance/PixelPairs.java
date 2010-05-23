package distance;

public interface PixelPairs {
	public void reset();

	// use this to add another pair
	public void add(float value1, float value2);

	public float distance();
}

