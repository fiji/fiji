package fiji.plugin.trackmate.util;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableRealInterval;
import net.imglib2.Positionable;
import net.imglib2.RealPositionable;
import net.imglib2.algorithm.region.localneighborhood.AbstractNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.EllipseNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.EllipsoidNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.meta.ImgPlus;
import net.imglib2.outofbounds.OutOfBoundsMirrorExpWindowingFactory;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;

public class SpotNeighborhood<T extends RealType<T>> implements Neighborhood<T> {

	/*
	 * FIELDS
	 */
	
	protected final double[] calibration;
	protected final AbstractNeighborhood<T, ImgPlus<T>> neighborhood;
	protected final long[] center;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public SpotNeighborhood(final Spot spot, final ImgPlus<T> img) {
		this.calibration = TMUtils.getSpatialCalibration(img);
		// Center
		this.center = new long[img.numDimensions()];
		for (int d = 0; d < center.length; d++) {
			center[d] = Math.round( spot.getFeature(Spot.POSITION_FEATURES[d]).doubleValue() / calibration[d]);
		}
		// Span
		final long[] span = new long[img.numDimensions()];
		for (int d = 0; d < span.length; d++) {
			span[d] = Math.round(spot.getFeature(Spot.RADIUS) / calibration[d]);
		}
		// Neighborhood
		OutOfBoundsMirrorExpWindowingFactory<T, ImgPlus<T>> oob = new OutOfBoundsMirrorExpWindowingFactory<T, ImgPlus<T>>();
		if (img.numDimensions() == 2) {
			this.neighborhood = new EllipseNeighborhood<T, ImgPlus<T>>(img, center, span, oob);
		} else if (img.numDimensions() == 3) {
			this.neighborhood = new EllipsoidNeighborhood<T, ImgPlus<T>>(img, center, span, oob);
		} else {
			throw new IllegalArgumentException("Source input must be 2D or 3D, got nDims = "+img.numDimensions());
		}
		
	}
	
	/*
	 * METHODS
	 * We delegate everything to the wrapped neighborhood
	 */

	@Override
	public final SpotNeighborhoodCursor<T> cursor() {
		return new SpotNeighborhoodCursor<T>(this);
	}

	@Override
	public SpotNeighborhoodCursor<T> localizingCursor() {
		return cursor();
	}

	@Override
	public long size() {
		return neighborhood.size();
	}

	@Override
	public T firstElement() {
		return neighborhood.firstElement();
	}

	@Override
	public Object iterationOrder() {
		return neighborhood.iterationOrder();
	}

	@Override
	@Deprecated
	public boolean equalIterationOrder(IterableRealInterval<?> f) {
		return neighborhood.equalIterationOrder(f);
	}

	@Override
	public double realMin(int d) {
		return neighborhood.realMax(d);
	}

	@Override
	public void realMin(double[] min) {
		neighborhood.realMin(min);
		
	}

	@Override
	public void realMin(RealPositionable min) {
		neighborhood.realMin(min);
		
	}

	@Override
	public double realMax(int d) {
		return neighborhood.realMax(d);
	}

	@Override
	public void realMax(double[] max) {
		neighborhood.realMax(max);
	}

	@Override
	public void realMax(RealPositionable max) {
		neighborhood.realMax(max);
	}

	@Override
	public int numDimensions() {
		return neighborhood.numDimensions();
	}

	@Override
	public SpotNeighborhoodCursor<T> iterator() {
		return cursor();
	}

	@Override
	public long min(int d) {
		return neighborhood.min(d);
	}

	@Override
	public void min(long[] min) {
		neighborhood.min(min);
	}

	@Override
	public void min(Positionable min) {
		neighborhood.min(min);
	}

	@Override
	public long max(int d) {
		return neighborhood.max(d);
	}

	@Override
	public void max(long[] max) {
		neighborhood.max(max);
	}

	@Override
	public void max(Positionable max) {
		neighborhood.max(max);
	}

	@Override
	public void dimensions(long[] dimensions) {
		neighborhood.dimensions(dimensions);
	}

	@Override
	public long dimension(int d) {
		return neighborhood.dimension(d);
	}

	@Override
	public void localize(int[] position) {
		for (int d = 0; d < position.length; d++) {
			position[d] = (int) center[d];
		}
	}

	@Override
	public void localize(long[] position) {
		for (int d = 0; d < position.length; d++) {
			position[d] = center[d];
		}
	}

	@Override
	public int getIntPosition(int d) {
		return (int) center[d];
	}

	@Override
	public long getLongPosition(int d) {
		return center[d];
	}

	@Override
	public void localize(float[] position) {
		for (int d = 0; d < position.length; d++) {
			position[d] = center[d];
		}
	}

	@Override
	public void localize(double[] position) {
		for (int d = 0; d < position.length; d++) {
			position[d] = center[d];
		}
	}

	@Override
	public float getFloatPosition(int d) {
		return center[d];
	}

	@Override
	public double getDoublePosition(int d) {
		return center[d];
	}

	@Override
	public Interval getStructuringElementBoundingBox() {
		long[] min = new long[numDimensions()];
		long[] max = new long[numDimensions()];
		min(min);
		max(max);
		FinalInterval interval = new FinalInterval(min , max );
		return interval;
	}

}
