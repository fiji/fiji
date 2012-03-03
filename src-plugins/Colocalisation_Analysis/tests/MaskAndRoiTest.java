package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gadgets.MaskFactory;

import java.util.Arrays;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.PredicateCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.cursor.special.predicate.MaskPredicate;
import mpicbg.imglib.cursor.special.predicate.Predicate;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
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
	 * Tests if a masked walk over an image refers to the correct data
	 * by copying the data to a separate image and then compare it with
	 * the original image data. The position data in the original image
	 * is calculated based on the ROI offset and the relative position
	 * in the copied ROI image.
	 * @throws MissingPreconditionException
	 */
	@Test
	public void maskContentTest() throws MissingPreconditionException {
		// load a 3D test image
		final Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		final int[] roiOffset = createRoiOffset(img);
		final int[] roiSize = createRoiSize(img);
		final Image<BitType> mask = MaskFactory.createMask(img.getDimensions(),
				roiOffset, roiSize);

		// create cursor to walk an image with respect to a mask
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				img.createLocalizableByDimCursor(),
				img.createLocalizableByDimCursor(),
				mask.createLocalizableCursor());

		// create an image for the "clipped ROI"
		ImageFactory<UnsignedByteType> maskFactory =
				new ImageFactory<UnsignedByteType>(img.createType(),
						new ArrayContainerFactory());
		Image<UnsignedByteType> clippedRoiImage =
				maskFactory.createImage( roiSize, "Clipped ROI" );
		LocalizableByDimCursor<UnsignedByteType> outputCursor =
				clippedRoiImage.createLocalizableByDimCursor();

		// copy ROI data to new image
		int[] pos = clippedRoiImage.createPositionArray();
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getPosition(pos);
			// shift position by offset
			for (int i=0; i<pos.length; i++) {
				pos[i] = pos[i] - roiOffset[i];
			}
			outputCursor.setPosition(pos);
			outputCursor.getType().set( cursor.getChannel1() );
		}
		cursor.close();
		outputCursor.close();

		/* go through the clipped ROI and compare the date to offset values
		 * of the original data.
		 */
		LocalizableCursor<UnsignedByteType> roiCopyCursor =
				clippedRoiImage.createLocalizableCursor();
		LocalizableByDimCursor<UnsignedByteType> imgCursor =
				img.createLocalizableByDimCursor();
		// create variable for summing up and set it to zero
		double sum = 0;
		pos = clippedRoiImage.createPositionArray();
		while (roiCopyCursor.hasNext()) {
			roiCopyCursor.fwd();
			roiCopyCursor.getPosition(pos);
			// shift position by offset
			for (int i=0; i<pos.length; i++) {
				pos[i] = pos[i] + roiOffset[i];
			}
			// set position in original image
			imgCursor.setPosition(pos);
			// get ROI and original image data
			double roiData = roiCopyCursor.getType().getRealDouble();
			double imgData = imgCursor.getType().getRealDouble();
			// sum up the difference
			double diff = roiData - imgData;
			sum += diff * diff;
		}
		roiCopyCursor.close();
		imgCursor.close();

		// check if sum is zero
		assertTrue("The sum of squared differences was " + sum + ".", Math.abs(sum) < 0.00001);
	}

	/**
	 * Tests a PredicateCursor by checking if all visited values are "true".
	 * @throws MissingPreconditionException
	 */
	@Test
	public void predicateCursorTest() throws MissingPreconditionException {
		// load a 3D test image
		Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		int[] roiOffset = createRoiOffset(img);
		int[] roiSize = createRoiSize(img);
		Image<BitType> mask = MaskFactory.createMask(img.getDimensions(),
				roiOffset, roiSize);

		// create cursor to walk an image with respect to a mask
		final Predicate<BitType> predicate = new MaskPredicate();
		LocalizableCursor<BitType> roiCursor
			= new PredicateCursor<BitType>(mask.createLocalizableCursor(), predicate);

		// test if all visited voxels are "true"
		while (roiCursor.hasNext()) {
			roiCursor.fwd();
			assertTrue(roiCursor.getType().get());
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

		/* go once more trough the image wrt. the mask to build a
		 * bounding box
		 */
		// create cursor to walk an image with respect to a mask
		final Predicate<BitType> predicate = new MaskPredicate();
		LocalizableCursor<BitType> roiCursor
			= new PredicateCursor<BitType>(mask.createLocalizableCursor(), predicate);
		int[] min = mask.createPositionArray();
		int[] max = mask.createPositionArray();
		Arrays.fill(min, Integer.MAX_VALUE);
		Arrays.fill(max, Integer.MIN_VALUE);
		while (roiCursor.hasNext()) {
			roiCursor.fwd();
			roiCursor.getPosition(pos);
			for (int i=0; i<pos.length; i++) {
				if (pos[i] < min[i])
					min[i] = pos[i];
				if (pos[i] > max[i])
					max[i] = pos[i];
			}
		}
		roiCursor.close();
		// the bounding box min should equal the ROI offset
		assertTrue(Arrays.equals(min, roiOffset));
		// create theoretical bounding box max and check it
		int[] roiMax = roiOffset.clone();
		for (int i=0; i<roiMax.length; i++)
			roiMax[i] += roiSize[i] - 1;
		assertTrue(Arrays.equals(max, roiMax));
	}

	/**
	 * This test creates first an "always true" mask and count the data
	 * values. There should be as many as the number of vocels in total.
	 * After that an "always false" mask is created. The predicate cursor
	 * there should not return any values.
	 */
	@Test
	public void simpleMaskCreationTest() {
		final Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		// first, create an always true mask
		Image<BitType> mask = MaskFactory.createMask(img.getDimensions(), true);
		final Predicate<BitType> predicate = new MaskPredicate();
		LocalizableCursor<BitType> cursor
			= new PredicateCursor<BitType>(mask.createLocalizableCursor(),
					predicate);
		// iterate over mask and count values
		int count = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			count++;
			assertTrue(cursor.getType().get());
		}
		cursor.close();
		assertEquals(img.getNumPixels(), count);

		// second, create an always false mask
		mask = MaskFactory.createMask(img.getDimensions(), false);
		cursor = new PredicateCursor<BitType>(
				mask.createLocalizableCursor(), predicate);
		// iterate over mask and count values
		count = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			count++;
		}
		cursor.close();
		assertEquals(0, count);
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

	/**
	 * This test makes sure that a mask that is based on a lower dimension
	 * image has the correct dimensionality.
	 */
	@Test
	public void irregularRoiDimensionTest() {
		// load a 3D test image
		Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		int width = img.getDimension(0);
		int height = img.getDimension(1);
		int slices = img.getDimension(2);
		// create a random noise 2D image -- set roiWidh/roiSize accordingly
		Image<UnsignedByteType> maskSlice =
			TestImageAccessor.produceSticksNoiseImage(width, height, 50, 2, 10);
		Image<BitType> mask = MaskFactory.createMask(img.getDimensions(), maskSlice);
		// check the dimensions of the mask
		org.junit.Assert.assertArrayEquals(img.getDimensions(), mask.getDimensions());
		// make sure the mask actually got the same content on every slice
		int[] offset = mask.createPositionArray();
		Arrays.fill(offset, 0);
		int[] size = mask.createPositionArray();
		size[0] = width;
		size[1] = height;
		size[2] = 1;
		LocalizableByDimCursor<BitType> maskCursor =
				mask.createLocalizableByDimCursor();
		RegionOfInterestCursor<BitType> firstSliceCursor =
				mask.createLocalizableByDimCursor().createRegionOfInterestCursor(offset, size);
		int[] pos = mask.createPositionArray();
		while (firstSliceCursor.hasNext()) {
			firstSliceCursor.fwd();
			firstSliceCursor.getPosition(pos);
			BitType maskValue = firstSliceCursor.getType();
			// go through all slices
			for (int i=1; i<slices; ++i) {
				pos[2] = i;
				maskCursor.setPosition(pos);
				// compare the values and assume they are the same
				int cmp = maskCursor.getType().compareTo(maskValue);
				assertEquals(0, cmp);
			}
		}
		firstSliceCursor.close();
		maskCursor.close();
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
