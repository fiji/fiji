package net.imglib2.predicate;

import net.imglib2.Cursor;
import net.imglib2.type.Type;

/**
 * An interface to check a value against arbitrary conditions.
 */
public interface Predicate<T extends Type<T>> {

	// evaluate a predicate check for a given value
	boolean test(Cursor<T> value);
}
