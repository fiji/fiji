package imglib.mpicbg.imglib.cursor.special.meta;

/**
 * An interface to check a value against arbitrary conditions.
 */
public interface Predicate<T extends Comparable<T>> {

	// evaluate a predicate check for a given value
	boolean evaluate(T value);
}
