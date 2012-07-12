package net.imglib2.cursor.special;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.AbstractNeighborhood;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsPeriodicFactory;

public class EllipsoidNeighborhood<T> extends AbstractNeighborhood<T> {

	/*
	 * CONSTRUCTORS
	 */
	
	public EllipsoidNeighborhood(RandomAccessibleInterval<T> source, OutOfBoundsFactory<T, RandomAccessibleInterval<T>> outOfBounds) {
		super(source, outOfBounds);
	}
	
	public EllipsoidNeighborhood(RandomAccessibleInterval<T> source) {
		this(source, new OutOfBoundsPeriodicFactory<T, RandomAccessibleInterval<T>>());
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public EllipsoidNeighborhoodCursor<T> cursor() {
		return new EllipsoidNeighborhoodCursor<T>(this);
	}

	@Override
	public EllipsoidNeighborhoodCursor<T> localizingCursor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EllipsoidNeighborhoodCursor<T> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
