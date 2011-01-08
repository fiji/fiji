package gadgets;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.LocalizablePlaneCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;

/**
 * A RoiImage decorates an ImgLib Image in a way that cursors on it
 * will only walk through the region of interest specified. Since not
 * all cursors can handle that in an easy way, some are not usable
 * with that class yet.
 */
public class RoiImage<T extends Type<T>> extends Image<T> {
	// location and dimensionality
	final int[] offset, size;
	// total number of pixels
	final int numPixels;

	/**
	 * Creates a new RoiImage to decorate the passed image. Cursors
	 * created through that class will refer only to the ROI.
	 *
	 * @param img The image to decorate
	 * @param offset The offset of the ROI
	 * @param size The size of the ROI
	 */
	public RoiImage( Image<T> img, final int[] offset, final int size[] ) {
		super(img.getContainer(), img.createType(), img.getName());

		this.offset = offset;
		this.size = size;

		int count = 1;
		for (int d=0; d < getNumDimensions(); d++) {
			count *= size[d];
		}
		numPixels = count;
	}

	@Override
	public int[] getDimensions() {
		return size.clone();
	}

	@Override
	public void getDimensions( int[] position ) {
		for (int d=0; d < getNumDimensions(); d++) {
			position[d] = size[d];
		}
	}

	@Override
	public int getDimension( int dim ) {
		return size[dim];
	}

	@Override
	public int getNumPixels() {
		return numPixels;
	}

	@Override
	public Cursor<T> createCursor() {
		LocalizableByDimCursor<T> cursor = super.createLocalizableByDimCursor();
		return new RegionOfInterestCursor<T>(cursor, offset, size);
	}

	@Override
	public LocalizableCursor<T> createLocalizableCursor() {
		LocalizableByDimCursor<T> cursor = super.createLocalizableByDimCursor();
		return new RegionOfInterestCursor<T>(cursor, offset, size);
	}


	/* Not implemented/needed methods follow */

	@Override
	public LocalizablePlaneCursor<T> createLocalizablePlaneCursor() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor(
			OutOfBoundsStrategyFactory<T> factory) {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public Interpolator<T> createInterpolator(InterpolatorFactory<T> factory) {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
}
