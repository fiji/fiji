package gadgets;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.cursor.special.RoiShiftingLocalizableByDimCursor;
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
	final int[] roiOffset, roiSize;
	// total number of pixels
	final int numPixels;
	// a factory for creating RoiImages like this
	RoiImageFactory<T> roiImageFactory;

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

		// copy offset data, and pad with zero if necessary
		roiOffset = new int[img.getNumDimensions()];
		for (int d=0; d < roiOffset.length; d++) {
			roiOffset[d] = (d<offset.length)? offset[d] : 0;
		}

		/* copy size data, and pad with original images size in
		 * the missing dimension if necessary
		 */
		this.roiSize = new int[img.getNumDimensions()];
		int count = 1;
		for (int d=0; d < roiSize.length; d++) {
			roiSize[d] = (d<size.length)? size[d] : img.getDimension(d);
			count *= roiSize[d];
		}
		numPixels = count;

		roiImageFactory = new RoiImageFactory<T>(offset, size, img.createType(),
			img.getContainerFactory());
	}

	public int[] getOffset() {
		return roiOffset.clone();
	}

	@Override
	public int[] getDimensions() {
		return roiSize.clone();
	}

	@Override
	public void getDimensions( int[] position ) {
		for (int d=0; d < getNumDimensions(); d++) {
			position[d] = roiSize[d];
		}
	}

	@Override
	public int getDimension( int dim ) {
		if ( dim < getNumDimensions() && dim > -1 )
			return this.roiSize[ dim ];
		else
			return 1;
	}

	@Override
	public int getNumPixels() {
		return numPixels;
	}

	@Override
	public Cursor<T> createCursor() {
		LocalizableByDimCursor<T> cursor = super.createLocalizableByDimCursor();
		return new RegionOfInterestCursor<T>(cursor, roiOffset, roiSize);
	}

	@Override
	public LocalizableCursor<T> createLocalizableCursor() {
		LocalizableByDimCursor<T> cursor = super.createLocalizableByDimCursor();
		return new RegionOfInterestCursor<T>(cursor, roiOffset, roiSize);
	}

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor() {
		LocalizableByDimCursor<T> cursor = super.createLocalizableByDimCursor();
		return new RoiShiftingLocalizableByDimCursor<T>(cursor, roiOffset, true);
	}

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor(
			OutOfBoundsStrategyFactory<T> factory) {
		LocalizableByDimCursor<T> cursor = super.createLocalizableByDimCursor( factory );
		return new RoiShiftingLocalizableByDimCursor<T>(cursor, roiOffset, true);
	}


	/* Not implemented/needed methods follow */

	@Override
	public Interpolator<T> createInterpolator(InterpolatorFactory<T> factory) {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
}
