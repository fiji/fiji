package net.imglib2.cursor.special;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import net.imglib2.Cursor;
import net.imglib2.Sampler;
import net.imglib2.img.Img;
import net.imglib2.outofbounds.OutOfBoundsRandomAccess;
import net.imglib2.type.Type;

/**
 * This abstract cursor offer facilities for specialized cursor that are based 
 * on a {@link LocalizableByDimCursor} whose iteration domain is imposed.
 * This abstract class itself is not really interesting, see sub-classes.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 9, 2010
 *
 * @param <T>
 */
public abstract class AbstractSpecialCursor <T extends Type<T>> implements Cursor<T> {

	/*
	 * FIELDS
	 */
	
	/** The cursor that will be used internally to iterate in the domain. */ 
	protected OutOfBoundsRandomAccess<T> cursor;
	/** The Image this cursors operates on. */
	protected Img<T> img;
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
	public T get() {
		return cursor.get();
	}

	@Override
	public final boolean hasNext() {
		return hasNext;
	}

	@Override
	public final T next() {
		fwd();
		return cursor.get();
	}
	
	/*
	 * LOCALIZABLE METHODS
	 */
	
	@Override
	public void localize(int[] position) {
		cursor.localize(position);
	}

	@Override
	public void localize(float[] position) {
		cursor.localize(position);
	}

	@Override
	public void localize(double[] position) {
		cursor.localize(position);
	}

	@Override
	public float getFloatPosition(int d) {
		return cursor.getFloatPosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return cursor.getDoublePosition(d);
	}

	@Override
	public int numDimensions() {
		return cursor.numDimensions();
	}

	@Override
	public Sampler<T> copy() {
		return cursor.copy();
	}

	@Override
	public void jumpFwd(long steps) {
		new RuntimeException("Not implemented.");
	}

	@Override
	public void remove() {
	}

	@Override
	public void localize(long[] position) {
		cursor.localize(position);
	}

	@Override
	public int getIntPosition(int d) {
		return cursor.getIntPosition(d);
	}

	@Override
	public long getLongPosition(int d) {
		return cursor.getLongPosition(d);
	}

}
