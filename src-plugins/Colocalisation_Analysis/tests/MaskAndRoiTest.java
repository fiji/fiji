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
	 * This test test if a regular mask is created by the MaskFactory
	 * correctly. First, the dimensions are checked, they must be the
	 * same as the original images ones. Then it is checked if all
	 * values in the mask image have the value they should have. For
	 * a regular ROI this is easy to tell as one can calculate it out
	 * of the position.
	 */
	@Test
	public void regularMaskCreationTest() throws MissingPreconditionException {
		// load a 3D test image
		Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		int[] roiOffset = createRoiOffset(img);
		int[] roiSize = createRoiSize(img);
		Image<BitType> mask = MaskFactory.createMask(img.getDimensions(),
				roiOffset, roiSize);

		// is the number of dimensions the same as in the image?
		assertTrue( Arrays.equals(img.getDimensions(), mask.getDimensions()) );

		// go through the mask and check if all valid points are in the ROI
		final int[] pos = mask.createPositionArray();
		final LocalizableCursor<BitType> cursor = mask.createLocalizableCursor();
		while ( cursor.hasNext() ) {
			cursor.fwd();
			cursor.getPosition(pos);
			// get values in mask image
			boolean onInMask = cursor.getType().get();
			// calculate value that the current point *should* have
			boolean onInROI = true;
			for(int i=0; i<pos.length; ++i)
				onInROI &= pos[i] >= roiOffset[i] && pos[i] < (roiOffset[i] + roiSize[i]);
			// both values must match
			assertTrue(onInMask == onInROI);
		}
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
			assertTrue( cursor.getChannel1().getInteger() != 0 );
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
			assertEquals( 0, cursor.getChannel1().getInteger() );
		}
		cursor.close();
	}

	@Test
	public void regularRoiPixelCountTest() {
		// load a 3D test image
		Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		int[] roiOffset = createRoiOffset(img);
		int[] roiSize = createRoiSize(img);
		int width = img.getDimension(0);
		int height = img.getDimension(1);

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
