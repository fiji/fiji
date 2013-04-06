package fiji.plugin.trackmate.detection.util;

import java.util.Iterator;

import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.IterableInterval;
import net.imglib2.IterableRealInterval;
import net.imglib2.Localizable;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.view.Views;

public class SquareNeighborhood3x3 <T> implements Positionable, IterableInterval<T> {

	private RandomAccessibleInterval<T> source;
	private final long[] center;
	private final ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> extendedSource;
	
	/*
	 * CONSTRUCTOR
	 */
	

	public SquareNeighborhood3x3(RandomAccessibleInterval<T> source, OutOfBoundsFactory<T, RandomAccessibleInterval<T>> outOfBounds) {
		this.source = source;
		this.center = new long[source.numDimensions()];
		this.extendedSource = Views.extend(source, outOfBounds);
	}
	
	
	/*
	 * METHODS
	 */

	@Override
	public int numDimensions() {
		return source.numDimensions();
	}

	@Override
	public void fwd(int d) {
		center[ d ]++;
	}



	@Override
	public void bck(int d) {
		center[ d ]--;
	}

	@Override
	public void move(int distance, int d) {
		center[ d ] = center [ d ] + distance;
	}

	@Override
	public void move(long distance, int d) {
		center[ d ] = center [ d ] + distance;
	}

	@Override
	public void move(Localizable localizable) {
		for (int i = 0; i < source.numDimensions(); i++) {
			center [ i ] = center [ i ] + localizable.getLongPosition(i);
		}		
	}

	@Override
	public void move(int[] distance) {
		for (int i = 0; i < distance.length; i++) {
			center [ i ]  = center [ i ] + distance [ i ];
		}
	}

	@Override
	public void move(long[] distance) {
		for (int i = 0; i < distance.length; i++) {
			center [ i ]  = center [ i ] + distance [ i ];
		}
	}

	@Override
	public void setPosition(Localizable localizable) {
		localizable.localize(center);
	}

	@Override
	public void setPosition(int[] position) {
		for (int i = 0; i < position.length; i++) {
			center [ i ] = position[ i ];
		}
	}

	@Override
	public void setPosition(long[] position) {
		System.arraycopy(position, 0, center, 0, center.length);
	}

	@Override
	public void setPosition(int position, int d) {
		center [ d ] = position;
	}

	@Override
	public void setPosition(long position, int d) {
		center [ d ] = position;
	}

	@Override
	public long size() {
		return 9;
	}

	@Override
	public T firstElement() {
		RandomAccess<T> ra = source.randomAccess();
		ra.setPosition(center);
		return ra.get();
	}

	@Override
	public Object iterationOrder() {
		return this;
	}

	@Override
	@Deprecated
	public boolean equalIterationOrder(IterableRealInterval<?> f) {
		return (f instanceof SquareNeighborhood3x3);
	}

	@Override
	public double realMin(int d) {
		return center[ d ] - 1;
	}

	@Override
	public void realMin(double[] min) {
		for (int d = 0; d < min.length; d++) {
			min[ d ] = center [ d ] - 1;
		}
	}

	@Override
	public void realMin(RealPositionable min) {
		for (int d = 0; d < center.length; d++) {
			min.setPosition(center[ d ] - 1, d);
		}
	}

	@Override
	public double realMax(int d) {
		return center[ d ] + 1;
	}

	@Override
	public void realMax(double[] max) {
		for (int d = 0; d < max.length; d++) {
			max[ d ] = center [ d ] + 1;
		}
	}

	@Override
	public void realMax(RealPositionable max) {
		for (int d = 0; d < center.length; d++) {
			max.setPosition(center[ d ] + 1, d);
		}		
	}

	@Override
	public long min(int d) {
		return center [ d ] - 1;
	}

	@Override
	public void min(long[] min) {
		for (int d = 0; d < min.length; d++) {
			min [ d ] = center[ d ] - 1;
		}
	}

	@Override
	public void min(Positionable min) {
		for (int d = 0; d < center.length; d++) {
			min.setPosition(center[ d ] - 1, d);
		}
	}

	@Override
	public long max(int d) {
		return center[ d ] + 1;
	}

	@Override
	public void max(long[] max) {
		for (int d = 0; d < max.length; d++) {
			max[ d ] = center[ d ] + 1;
		}		
	}

	@Override
	public void max(Positionable max) {
		for (int d = 0; d < center.length; d++) {
			max.setPosition(center[ d ] + 1, d);
		}
	}

	@Override
	public void dimensions(long[] dimensions) {
		dimensions[0] = 3;
		dimensions[1] = 3;
		for (int d = 2; d < dimensions.length; d++) {
			dimensions[ d ] = 1;
		}
		
	}

	@Override
	public long dimension(int d) {
		if (d < 2 ) 
			return 3; 
		else
			return 1;
	}

	@Override
	public SquareNeighborhoodCursor3x3<T> cursor() {
		return  new SquareNeighborhoodCursor3x3<T>(extendedSource, center);
	}

	@Override
	public SquareNeighborhoodCursor3x3<T> localizingCursor() {
		return  new SquareNeighborhoodCursor3x3<T>(extendedSource, center);
	}
	
	@Override
	public Iterator<T> iterator() {
		return  new SquareNeighborhoodCursor3x3<T>(extendedSource, center);
	}

}
