package mpicbg.imglib.cursor.special;

import java.util.Iterator;

import mpicbg.imglib.container.Container;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * This abstract cursor offer facilities for specialized cursor that are based 
 * on a {@link LocalizableByDimCursor} whose iteration domain is imposed.
 * This abstract class itself is not really interesting, see sub-classes.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 9, 2010
 *
 * @param <T>
 */
public abstract class AbstractSpecialCursor <T extends Type<T>> implements LocalizableCursor<T> {

	/*
	 * FIELDS
	 */
	
	/** The cursor that will be used internally to iterate in the domain. */ 
	protected LocalizableByDimCursor<T> cursor;
	/** The Image this cursors operates on. */
	protected Image<T> img;
	/** True if the iteration is not done yet. */
	protected boolean hasNext;
	
	/**
	 * Return the number of pixels this cursor will iterate on (or, the number of iterations
	 * it will do before exhausting). This is useful when one needs to know 
	 * the number of pixel iterated on in advance. For instance:
	 * <pre>
	 * DiscCursor<T> dc = new DiscCursor(img, center, 5);
	 * int arrraySize = sc.getNPixels();
	 * float[] pixelVal = new float[arraySize];
	 * int index = 0;
	 * while (sc.hasNext()) {
	 * 	sc.fwd();
	 * 	pixelVal[index] = sc.getType().getRealFloat();
	 * 	index++;
	 * }
	 * </pre>
	 */
	public abstract int getNPixels();
	
	
	/*
	 * CURSOR METHODS
	 * We simply forward them to the internal cursor
	 */
	
	
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
		return img;
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
	public void setDebug(boolean debug) {
		cursor.setDebug(debug);
	}

	@Override
	public final boolean hasNext() {
		return hasNext;
	}

	@Override
	public final T next() {
		fwd();
		return cursor.getType();
	}

	@Override
	public void remove() {
		cursor.remove();
	}

	@Override
	public Iterator<T> iterator() {
		reset();
		return this;
	}

	@Override
	public void fwd(long steps) {
		for (int i = 0; i < steps; i++) 
			fwd();
	}

	@Override
	public int[] getDimensions() {
		return cursor.getDimensions();
	}

	@Override
	public void getDimensions(int[] position) {
		cursor.getDimensions(position);
	}

	@Override
	public int getNumDimensions() {
		return 3;
	}

	/*
	 * LOCALIZABLE METHODS
	 */
	
	@Override
	public int[] getPosition() {
		return cursor.getPosition();
	}

	@Override
	public void getPosition(int[] position) {
		cursor.getPosition(position);
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
