package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gadgets.MaskFactory;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.PredicateCursor;
import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.cursor.special.predicate.MaskPredicate;
import mpicbg.imglib.cursor.special.predicate.Predicate;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

import algorithms.MissingPreconditionException;

/**
 * This class contains JUnit 4 test cases for the ROI and masks
 * implementation.
 *
 * @author Dan White & Tom Kazimiers
 */
public class MaskAndRoiTest extends ColocalisationTest {

	/**
	 * Tests if a RoiImage refers to the correct data by copying the
	 * ROI data to a separate image and then convert that result with
	 * the original image data. The position data in the original image
	 * is calculated for the ROI offset and the relative position in the
	 * copied ROI image.
	 */
	@Test
	public void regularRoiPredicateCursorTest() throws MissingPreconditionException {
		// create a random noise 2D image -- set roiWidh/roiSize accordingly
		//Image<FloatType> img = TestImageAccessor.produceNoiseImage(new FloatType(), 200, 300);
		// load a 3D test image
		Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		int width = img.getDimension(0);
		int height = img.getDimension(1);
		/* The size and dimensions are not of same dimension as the image.
		 * This is intended to make sure the RoiImage can figure it out
		 * on its own.
		 */
		int[] roiOffset = new int[] {width / 4, height / 4};
		int[] roiSize = new int[] {width / 2, height / 2};
		Image<BitType> mask = MaskFactory.createMask(img.getDimensions(),
				roiOffset, roiSize);

		// clip the actual ROI, so that the non-ROI area is removed
		final Predicate<BitType> predicate = new MaskPredicate();
		LocalizableCursor<BitType> roiCursor
			= new PredicateCursor<BitType>(mask.createLocalizableCursor(), predicate);

		// copy ROI data to new image
		while (roiCursor.hasNext()) {
			roiCursor.fwd();
			// assert that the value is true
			assert(roiCursor.getType().get());
		}

		roiCursor.close();
	}

	/**
	 * This test makes sure the RoiImage can figure out the
	 * correct dimensions of the ROI in case on passes a ROI
	 * with different dimensions than the image.
	 */
	@Test
	public void regularRoiDimensionsTest() throws MissingPreconditionException {
		// load a 3D test image
		Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		int width = img.getDimension(0);
		int height = img.getDimension(1);

		int[] roiOffset = new int[] {width / 4, height / 4};
		int[] roiSize = new int[] {width / 2, height / 2};
		Image<BitType> mask = MaskFactory.createMask(img.getDimensions(),
				roiOffset, roiSize);

		// is the number of dimensions the same as in the image?
		assertEquals(mask.getNumDimensions(), img.getNumDimensions());

		// Is the ROIs dimension information correct?
		for (int i=0; i<mask.getNumDimensions(); ++i)
			assertEquals(mask.getDimension(i), img.getDimension(i));

		// go through the mask and check if all valid points are in the ROI
		final int[] pos = mask.createPositionArray();
		final LocalizableCursor<BitType> cursor = mask.createLocalizableCursor();
		boolean everythingOkay = true;
		while ( cursor.hasNext() ) {
			cursor.getPosition(pos);
			boolean onInMask = cursor.getType().get();

			boolean onInROI = true;
			// test if the current position is contained in the ROI
			for(int i=0; i<pos.length; ++i)
				onInROI &= pos[i] > roiOffset[i] && pos[i] < (roiOffset[i] + roiSize[i]);
			// assume both values are the same
			everythingOkay &= (onInMask == onInROI);
		}
		assert(everythingOkay);
		cursor.close();
	}

	/**
	 * Tests against the implementation of irregular ROIs alias
	 * masks. Masks can also be produced by mask images open in
	 * another Fiji window.
	 *
	 * This test generates a random black/white noise image and
	 * uses first itself and then an inverted version of it as
	 * mask. While iterating over it, the pixel values are
	 * checked. Is the first version only non-zero values should
	 * be present, while only zeros should be there in the second
	 * one.
	 */
	@Test
	public void irregularRoiTest() {
		// create a random noise 2D image -- set roiWidh/roiSize accordingly
		Image<UnsignedByteType> img =
			TestImageAccessor.produceSticksNoiseImage(300, 300, 50, 2, 10);

		/* first test - using itself as a mask */
		Image<BitType> mask = MaskFactory.createMask(img.getDimensions(), img);
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				img.createLocalizableByDimCursor(),
				img.createLocalizableByDimCursor(),
				mask.createLocalizableCursor());
		while (cursor.hasNext()) {
			cursor.fwd();
			assertTrue( cursor.getType().getInteger() != 0 );
		}
		cursor.close();

		/* second test - using inverted image */
		Image<UnsignedByteType> invImg = TestImageAccessor.invertImage(img);
		Image<BitType> invMask = MaskFactory.createMask(img.getDimensions(), invImg);
		cursor = new TwinCursor<UnsignedByteType>(
				img.createLocalizableByDimCursor(),
				img.createLocalizableByDimCursor(),
				invMask.createLocalizableCursor());
		while (cursor.hasNext()) {
			cursor.fwd();
			assertEquals( 0, cursor.getType().getInteger() );
		}
		cursor.close();
	}

	@Test
	public void irregularRoiPixelCountTest() {
		// load a 3D test image
		Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		int width = img.getDimension(0);
		int height = img.getDimension(1);
		// define rectangular mask to be used as irregular mask
		int[] roiOffset = new int[] {width / 4, height / 4};
		int[] roiSize = new int[] {width / 2, height / 2};
		Image<UnsignedByteType> maskImg
			= TestImageAccessor.createRectengularMaskImage(width, height, roiOffset, roiSize);
		Image<BitType> mask = MaskFactory.createMask(img.getDimensions(), maskImg);

		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				img.createLocalizableByDimCursor(),
				img.createLocalizableByDimCursor(),
				mask.createLocalizableCursor());
		// calculate volume of mask bounding box
		int roiVolume = roiSize[0] * roiSize[1] * img.getDimension(2);
		int count = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			count++;
		}
		cursor.close();

		assertEquals(roiVolume, count);
	}
}
