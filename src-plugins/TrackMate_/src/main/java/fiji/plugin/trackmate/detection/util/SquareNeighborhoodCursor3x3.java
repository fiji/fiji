package fiji.plugin.trackmate.detection.util;

import java.util.NoSuchElementException;

import net.imglib2.Bounded;
import net.imglib2.Cursor;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.outofbounds.OutOfBounds;

public class SquareNeighborhoodCursor3x3<T> implements Cursor<T>, Bounded {

	private final ExtendedRandomAccessibleInterval<T,RandomAccessibleInterval<T>> source;
	private final long[] center;
	private final OutOfBounds<T> ra;
	private int index = -1;
	private boolean hasNext;
	
	/*
	 * CONSTRUCTOR
	 */

	public SquareNeighborhoodCursor3x3(ExtendedRandomAccessibleInterval<T,RandomAccessibleInterval<T>> extendedSource,	long[] center) {
		this.source = extendedSource;
		this.center = center;
		this.ra = extendedSource.randomAccess();
		reset();
	}
	
	/*
	 * METHODS
	 */

	@Override
	public void localize(float[] position) {
		ra.localize(position);
	}

	@Override
	public void localize(double[] position) {
		ra.localize(position);
	}

	@Override
	public float getFloatPosition(int d) {
		return ra.getFloatPosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return ra.getDoublePosition(d);
	}

	@Override
	public int numDimensions() {
		return source.numDimensions();
	}

	@Override
	public T get() {
		return ra.get();
	}

	@Override
	public Sampler<T> copy() {
		return ra.copy();
	}

	@Override
	public void jumpFwd(long steps) {
		for (int i = 0; i < steps; i++) {
			fwd();
		}
	}

	@Override
	public void fwd() {
		index++;
				
		switch (index) {
		case 0:
			// already in place
			break;
			
		case 1:
			ra.bck(1);
			break;
			
		case 2:
			ra.bck(0);
			break;
			
		case 3:
			ra.fwd(1);
			break;
			
		case 4:
			ra.fwd(1);
			break;
			
		case 5:
			ra.fwd(0);
			break;
			
		case 6:
			ra.fwd(0);
			break;
			
		case 7:
			ra.bck(1);
			break;
			
		case 8:
			ra.bck(1);
			hasNext = false;
			break;

		default:
			throw new NoSuchElementException("SquareNeighborhood3x3 exhausted");
		}
	}

	@Override
	public void reset() {
		index = -1;
		hasNext = true;
		ra.setPosition(center);
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public T next() {
		fwd();
		return ra.get();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove() is not implemented for SquareNeighborhoodCursor");
	}

	@Override
	public void localize(int[] position) {
		ra.localize(position);
	}

	@Override
	public void localize(long[] position) {
		ra.localize(position);
	}

	@Override
	public int getIntPosition(int d) {
		return ra.getIntPosition(d);
	}

	@Override
	public long getLongPosition(int d) {
		return ra.getLongPosition(d);
	}

	@Override
	public Cursor<T> copyCursor() {
		return new SquareNeighborhoodCursor3x3<T>(source, center);
	}

	@Override
	public boolean isOutOfBounds() {
		return ra.isOutOfBounds();
	}

}
