package mpicbg.imglib.cursor.special.meta;

/**
 * A predicate that is true if, and only if, the value is above the threshold.
 */
public class AboveThresholdPredicate<T extends Comparable<T>> implements Predicate<T> {
	T threshold;

	public AboveThresholdPredicate(T threshold) {
		this.threshold = threshold;
	}

	public boolean evaluate(T value) {
		return value.compareTo(threshold) > 0;
	}

	public void setThreshold(T value) {
		threshold = value;
	}

	public T getThreshold() {
		return threshold;
	}
}
