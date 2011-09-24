package gadgets;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.MaskCursor;
import mpicbg.imglib.cursor.special.MaskLocalizableCursor;
import mpicbg.imglib.cursor.special.MaskLocalizableByDimCursor;
import mpicbg.imglib.cursor.special.RoiShiftingLocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.type.numeric.NumericType;

/**
 * A MaskedImage decorates an ImgLib Image in a way that cursors on it
 * will only walk through the region of interest specified. Since not
 * all cursors can handle that in an easy way, some are not usable
 * with that class yet.
 *
 * For now the mask must be of the same type of the image and is bound
 * to numeric types.
 */
public class MaskedImage<T extends NumericType<T> & Comparable<T>> extends RoiImage<T> {
	// the image to operate on
	Image<T> image;
	// the mask to use for the image
	final Image<T> mask;
	// the offValue of the image (see MaskCursor)
	T offValue;
	// a factory to create MaskImage objects
	MaskedImageFactory<T> maskedImageFactory;

	/**
	 * Creates a new MaskedImage to decorate the passed image. Cursors
	 * created through that class will refer only to the ROI.
	 *
	 * @param img The image to decorate.
	 * @param mask The mask for the image.
	 */
	public MaskedImage( Image<T> img, final Image<T> mask, int[] offset, int size[] ) {
		super(img, offset, size);

		this.image = img;
		this.mask = mask;

		// create the offValue of the mask
		offValue = mask.createType();
		offValue.setZero();

		init();
	}

	/**
	 * Init the mask factory and the off value.
	 */
	protected void init() {
		// create a new factory
		maskedImageFactory = new MaskedImageFactory<T>(mask, roiOffset, roiSize, image.createType(),
			image.getContainerFactory());
	}

	@Override
	public Cursor<T> createCursor() {
		LocalizableCursor<T> cursor = image.createLocalizableCursor();
		LocalizableByDimCursor<T> maskCursor = new RoiShiftingLocalizableByDimCursor<T>(
			mask.createLocalizableByDimCursor(), roiOffset, true);

		return new MaskCursor<T>(cursor, maskCursor, offValue);
	}

	@Override
	public LocalizableCursor<T> createLocalizableCursor() {
		LocalizableCursor<T> cursor = image.createLocalizableCursor();
		LocalizableByDimCursor<T> maskCursor = new RoiShiftingLocalizableByDimCursor<T>(
			mask.createLocalizableByDimCursor(), roiOffset, true);

		return new MaskLocalizableCursor<T>(cursor, maskCursor, offValue, roiOffset);
	}

	@Override
	public LocalizableByDimCursor<T> createLocalizableByDimCursor() {
		LocalizableByDimCursor<T> cursor = image.createLocalizableByDimCursor();
		LocalizableByDimCursor<T> maskCursor = new RoiShiftingLocalizableByDimCursor<T>(
			mask.createLocalizableByDimCursor(), roiOffset, true);

		return new MaskLocalizableByDimCursor<T>(cursor, maskCursor, offValue, roiOffset);
	}

	/* Not implemented/needed methods follow */

	@Override
	public Interpolator<T> createInterpolator(InterpolatorFactory<T> factory) {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
}
