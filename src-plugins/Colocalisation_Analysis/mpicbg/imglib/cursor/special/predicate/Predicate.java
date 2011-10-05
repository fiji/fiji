package mpicbg.imglib.cursor.special.predicate;

import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.type.Type;

/**
 * An interface to check a value against arbitrary conditions.
 */
public interface Predicate<T extends Type<T>> {

	// evaluate a predicate check for a given value
	boolean test(LocalizableCursor<T> value);
}
