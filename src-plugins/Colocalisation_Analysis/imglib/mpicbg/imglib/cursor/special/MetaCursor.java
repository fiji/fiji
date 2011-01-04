package imglib.mpicbg.imglib.cursor.special;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.Iterable;
import mpicbg.imglib.type.Type;

/**
 * Represents a cursor that could be used to drive an arbitrary number
 * of other cursors.
 *
 * @author Dan White & Tom Kazimiers
 */
public class MetaCursor< T extends Type< T > > implements Iterable
{
	// the list of cursors that is driven by this class
	List<Cursor<T>> cursors = new ArrayList<Cursor<T>>();

	public MetaCursor(Collection<Cursor<T>> cursors){
		if (cursors.contains(null))
			throw new IllegalArgumentException("At least one cursor is null");

		this.cursors.addAll(cursors);
	}

	/**
	 * Calls fwd() on all managed cursors.
	 */
	public void fwd() {
		for(Cursor<T> c : cursors)
			c.fwd();
	}

	/**
	 * Gets the logical And-Operation of all cursors hasNext() that are
	 * driven by this MetaCursor.
	 */
	public boolean hasNext() {
		boolean hasNext = true;
		for(Cursor<T> c : cursors)
			hasNext &= c.hasNext();

		return hasNext;
	}

	/**
	 * Calls fwd(long arg) on all managed cursors.
	 */
	public void fwd(long arg) {
		for(Cursor<T> c : cursors)
			c.fwd(arg);
	}

	/**
	 * Calls reset() on all managed cursors.
	 */
	public void reset() {
		for(Cursor<T> c : cursors)
			c.reset();
	}

	/**
	 * Calls close() on all managed cursors.
	 */
	public void close() {
		for(Cursor<T> c : cursors)
			c.close();
	}

	/**
	 * Gets the Type of the specified cursor.
	 */
	public T getType(int index) {
		return cursors.get(index).getType();
	}
}
