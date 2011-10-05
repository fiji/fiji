package mpicbg.imglib.cursor.special;

import java.util.Iterator;
import java.util.NoSuchElementException;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.predicate.Predicate;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * The PredicateCursor traverses a whole image but only returns
 * those pixels for which the Predicate returns true. There is
 * little sense to make this less than a LocalizableCursor
 */
public class PredicateCursor<T extends Type<T>> implements LocalizableCursor<T> {
	// the condition on which a position is valid
	final protected Predicate<T> predicate;
	// the cursor driven by the evaluation of the predicate
	final protected LocalizableCursor<T> cursor;
	// indicate if the next element has already been looked up
	protected boolean lookedForNext = false;
	// true if a next element was found after a look-up
	protected boolean hasNext = false;

	public PredicateCursor(final LocalizableCursor<T> cursor,
			final Predicate<T> predicate) {
		this.cursor = cursor;
		this.predicate = predicate;
	}

	/**
	 * Walks to the next valid elements and stores them in member
	 * variables cachedType1 and cachedType2.
	 *
	 * @return true if a next element was found, false otherwise
	 */
	protected boolean findNext() {
		boolean found = false;
		while( cursor.hasNext() ) {
			cursor.fwd();
			if ( predicate.test(cursor) ) {
				found = true;
				break;
			}
		}
		hasNext = found;
		return found;
	}

	@Override
	public boolean hasNext() {
		// did we already check for a next element without doing a fwd()?
		if ( lookedForNext )
			return hasNext;

		// indicate that we will already move the cursor to the next element
		lookedForNext = true;

		return findNext();
	}

	@Override
	public void fwd() {
		/* If we did not check for a next valid element before,
		 * walk to the next element now (if there is any).
		 */
		if ( ! lookedForNext )
			findNext();

		/* Since we have manually forwarded the cursor, the cached
		 * information is not valid any more
		 */
		lookedForNext = false;
	}

	@Override
	public void fwd(long num) {
		while (num > 0) {
			fwd();
		}
	}

	@Override
	public T next() {
		if ( hasNext() ) {
			fwd();
			return getType();
		} else {
			throw new NoSuchElementException();
		}
	}
	
	@Override
	public void remove() {
		cursor.remove();
	}

	@Override
	public Iterator<T> iterator() {
		return new PredicateCursor<T>(cursor, predicate);
	}
	
	@Override
	public void close() {
		cursor.close();
	}

	@Override
	public int[] createPositionArray() {
		return cursor.createPositionArray();
	}

	@Override
	public int getArrayIndex() {
		return cursor.getArrayIndex();
	}

	@Override
	public Image<T> getImage() {
		return cursor.getImage();
	}

	@Override
	public Container<T> getStorageContainer() {
		return cursor.getStorageContainer();
	}

	@Override
	public int getStorageIndex() {
		return cursor.getStorageIndex();
	}

	@Override
	public T getType() {
		return cursor.getType();
	}

	@Override
	public boolean isActive() {
		return cursor.isActive();
	}

	@Override
	public void reset() {
		cursor.reset();
		lookedForNext = false;
	}

	@Override
	public void setDebug(boolean debug) {
		cursor.setDebug(debug);
	}

	@Override
	public int[] getDimensions() {
		return cursor.getDimensions();
	}

	@Override
	public void getDimensions(int[] dim) {
		cursor.getDimensions(dim);
	}

	@Override
	public int getNumDimensions() {
		return cursor.getNumDimensions();
	}

	@Override
	public int[] getPosition() {
		return cursor.getPosition();
	}

	@Override
	public void getPosition(int[] pos) {
		cursor.getPosition(pos);
	}

	@Override
	public int getPosition(int dim) {
		return cursor.getPosition(dim);
	}

	@Override
	public String getPositionAsString() {
		return cursor.getPositionAsString();
	}
}
