package imglib.mpicbg.imglib.cursor.special.meta;

/**
 * A predicate that always evaluates to true.
 */
public class AlwaysTruePredicate<T extends Comparable<T>> implements Predicate<T> {
	public boolean evaluate(T value) {
		return true;
	}
}
