package imglib.mpicbg.imglib.cursor.special.meta;

/**
 * A predicate that is only true iff the value is below the threshold.
 */
public class BelowThresholdPredicate<T extends Comparable<T>> implements Predicate<T> {
	T threshold;

	public BelowThresholdPredicate(T threshold) {
		this.threshold = threshold;
	}

	public boolean evaluate(T value) {
		return value.compareTo(threshold) < 0;
	}

	public void setThreshold(T value) {
		threshold = value;
	}

	public T getThreshold() {
		return threshold;
	}
}
