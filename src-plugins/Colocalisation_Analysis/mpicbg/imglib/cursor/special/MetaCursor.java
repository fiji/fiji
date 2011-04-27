package mpicbg.imglib.cursor.special;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.type.Type;

/**
 * Represents a cursor that could be used to drive an arbitrary number
 * of other cursors.
 *
 * @author Dan White & Tom Kazimiers
 */
public abstract class MetaCursor< T extends Type< T > > implements Cursor<T>
{
	// the list of cursors that is driven by this class
	List<Cursor<T>> cursors = new ArrayList<Cursor<T>>();

	public MetaCursor(Collection<Cursor<T>> cursors){
		if (cursors.contains(null))
			throw new IllegalArgumentException("At least one cursor is null");

		// save first cursor separately as driver
		this.cursors.addAll(cursors);
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

	public void setDebug( final boolean debug ) {
		for(Cursor<T> c : cursors)
			c.setDebug( debug );
	}

	public boolean isActive() {
		boolean isActive = true;
		for(Cursor<T> c : cursors)
			isActive &= c.isActive();

		return isActive;
	}

	public void remove()
	{
		for(Cursor<T> c : cursors)
			c.remove();
	}

	/**
	 * Gets the Type of the specified cursor.
	 */
	public T getType(int index) {
		return cursors.get(index).getType();
	}

	public T next() {
		fwd();
		return getType();
	}
}
