package imglib.mpicbg.imglib.cursor.special.meta;

/**
 * An interface to check two values against arbitrary conditions.
 */
public interface Predicate2D<T extends Comparable<T>> {
	// evaluate a predicate check for a given pair of values
	boolean evaluate(T value1, T value2);
}
