package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gadgets.MaskFactory;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.PredicateCursor;
import net.imglib2.RandomAccess;
import net.imglib2.TwinCursor;
import net.imglib2.algorithm.math.ImageStatistics;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.predicate.MaskPredicate;
import net.imglib2.predicate.Predicate;
import net.imglib2.roi.RectangleRegionOfInterest;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

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
		final Img<UnsignedByteType> img = positiveCorrelationImageCh1;
		final long[] roiOffset = createRoiOffset(img);
		final long[] roiSize = createRoiSize(img);
		final long[] dim = new long[ img.numDimensions() ];
		img.dimensions(dim);
		final Img<BitType> mask = MaskFactory.createMask(dim,
				roiOffset, roiSize);

		// create cursor to walk an image with respect to a mask
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				img.randomAccess(), img.randomAccess(), mask.localizingCursor());

		// create an image for the "clipped ROI"
		ImgFactory<UnsignedByteType> maskFactory =
				new ArrayImgFactory<UnsignedByteType>();
		Img<UnsignedByteType> clippedRoiImage =
				maskFactory.create( roiSize, new UnsignedByteType() ); //  "Clipped ROI" );
		RandomAccess<UnsignedByteType> outputCursor =
				clippedRoiImage.randomAccess();

		// copy ROI data to new image
		long[] pos = new long[ clippedRoiImage.numDimensions() ];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			// shift position by offset
			for (int i=0; i<pos.length; i++) {
				pos[i] = pos[i] - roiOffset[i];
			}
			outputCursor.setPosition(pos);
			outputCursor.get().set( cursor.getChannel1() );
		}

		/* go through the clipped ROI and compare the date to offset values
		 * of the original data.
		 */
		Cursor<UnsignedByteType> roiCopyCursor =
				clippedRoiImage.localizingCursor();
		RandomAccess<UnsignedByteType> imgCursor =
				img.randomAccess();
		// create variable for summing up and set it to zero
		double sum = 0;
		pos = new long [ clippedRoiImage.numDimensions() ];
		while (roiCopyCursor.hasNext()) {
			roiCopyCursor.fwd();
			roiCopyCursor.localize(pos);
			// shift position by offset
			for (int i=0; i<pos.length; i++) {
				pos[i] = pos[i] + roiOffset[i];
			}
			// set position in original image
			imgCursor.setPosition(pos);
			// get ROI and original image data
			double roiData = roiCopyCursor.get().getRealDouble();
			double imgData = imgCursor.get().getRealDouble();
			// sum up the difference
			double diff = roiData - imgData;
			sum += diff * diff;
		}

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
		Img<UnsignedByteType> img = positiveCorrelationImageCh1;
		long[] roiOffset = createRoiOffset(img);
		long[] roiSize = createRoiSize(img);
		long[] dim = new long[ img.numDimensions() ];
		img.dimensions(dim);
		Img<BitType> mask = MaskFactory.createMask(dim,
				roiOffset, roiSize);

		// create cursor to walk an image with respect to a mask
		final Predicate<BitType> predicate = new MaskPredicate();
		Cursor<BitType> roiCursor
			= new PredicateCursor<BitType>(mask.localizingCursor(), predicate);

		// test if all visited voxels are "true"
		while (roiCursor.hasNext()) {
			roiCursor.fwd();
			assertTrue(roiCursor.get().get());
		}
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
		Img<UnsignedByteType> img = positiveCorrelationImageCh1;
		final long[] roiOffset = createRoiOffset(img);
		final long[] roiSize = createRoiSize(img);
		final long[] dim = new long[ img.numDimensions() ];
		img.dimensions(dim);
		Img<BitType> mask = MaskFactory.createMask(dim,
				roiOffset, roiSize);

		// is the number of dimensions the same as in the image?
		final long[] dimMask = new long[ mask.numDimensions() ];
		mask.dimensions(dimMask);
		assertTrue( Arrays.equals(dim, dimMask) );

		// go through the mask and check if all valid points are in the ROI
		long[] pos = new long[ img.numDimensions() ];
		final Cursor<BitType> cursor = mask.localizingCursor();
		while ( cursor.hasNext() ) {
			cursor.fwd();
			cursor.localize(pos);
			// get values in mask image
			boolean onInMask = cursor.get().get();
			// calculate value that the current point *should* have
			boolean onInROI = true;
			for(int i=0; i<pos.length; ++i)
				onInROI &= pos[i] >= roiOffset[i] && pos[i] < (roiOffset[i] + roiSize[i]);
			// both values must match
			assertTrue(onInMask == onInROI);
		}

		/* go once more trough the image wrt. the mask to build a
		 * bounding box
		 */
		// create cursor to walk an image with respect to a mask
		final Predicate<BitType> predicate = new MaskPredicate();
		Cursor<BitType> roiCursor
			= new PredicateCursor<BitType>(mask.localizingCursor(), predicate);
		long[] min = new long[ mask.numDimensions() ];
		long[] max = new long[ mask.numDimensions() ];
		Arrays.fill(min, Integer.MAX_VALUE);
		Arrays.fill(max, Integer.MIN_VALUE);
		while (roiCursor.hasNext()) {
			roiCursor.fwd();
			roiCursor.localize(pos);
			for (int i=0; i<pos.length; i++) {
				if (pos[i] < min[i])
					min[i] = pos[i];
				if (pos[i] > max[i])
					max[i] = pos[i];
			}
		}
		// the bounding box min should equal the ROI offset
		assertTrue(Arrays.equals(min, roiOffset));
		// create theoretical bounding box max and check it
		long[] roiMax = roiOffset.clone();
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
		final Img<UnsignedByteType> img = positiveCorrelationImageCh1;
		// first, create an always true mask
		final long[] dim = new long[ img.numDimensions() ];
		Img<BitType> mask = MaskFactory.createMask(dim, true);
		final Predicate<BitType> predicate = new MaskPredicate();
		Cursor<BitType> cursor
			= new PredicateCursor<BitType>(mask.localizingCursor(),
					predicate);
		// iterate over mask and count values
		long count = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			count++;
			assertTrue(cursor.get().get());
		}
		assertEquals(ImageStatistics.getNumPixels(mask), count);

		// second, create an always false mask
		mask = MaskFactory.createMask(dim, false);
		cursor = new PredicateCursor<BitType>(
				mask.localizingCursor(), predicate);
		// iterate over mask and count values
		count = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			count++;
		}
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
		Img<UnsignedByteType> img =
			TestImageAccessor.produceSticksNoiseImage(300, 300, 50, 2, 10);
		final long[] dim = new long[ img.numDimensions() ];
		img.dimensions(dim);
		
		/* first test - using itself as a mask */
		Img<BitType> mask = MaskFactory.createMask(dim, img);
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				img.randomAccess(),
				img.randomAccess(),
				mask.localizingCursor());
		while (cursor.hasNext()) {
			cursor.fwd();
			assertTrue( cursor.getChannel1().getInteger() != 0 );
		}

		/* second test - using inverted image */
		Img<UnsignedByteType> invImg = TestImageAccessor.invertImage(img);
		Img<BitType> invMask = MaskFactory.createMask(dim, invImg);
		cursor = new TwinCursor<UnsignedByteType>(
				img.randomAccess(),
				img.randomAccess(),
				invMask.localizingCursor());
		while (cursor.hasNext()) {
			cursor.fwd();
			assertEquals( 0, cursor.getChannel1().getInteger() );
		}
	}

	/**
	 * This test makes sure that a mask that is based on a lower dimension
	 * image has the correct dimensionality.
	 */
	@Test
	public void irregularRoiDimensionTest() {
		// load a 3D test image
		Img<UnsignedByteType> img = positiveCorrelationImageCh1;
		final long width = img.dimension(0);
		final long height = img.dimension(1);
		final long slices = img.dimension(2);
		final long[] dimImg = new long[ img.numDimensions() ];
		img.dimensions(dimImg);
		// create a random noise 2D image -- set roiWidh/roiSize accordingly
		Img<UnsignedByteType> maskSlice =
			TestImageAccessor.produceSticksNoiseImage( (int) width, (int) height, 50, 2, 10);
		Img<BitType> mask = MaskFactory.createMask(dimImg, maskSlice);
		final long[] dimMask = new long[ mask.numDimensions() ];
		mask.dimensions(dimMask);
		// check the dimensions of the mask
		org.junit.Assert.assertArrayEquals(dimImg, dimMask);
		// make sure the mask actually got the same content on every slice
		final double[] offset = new double[ mask.numDimensions() ];
		Arrays.fill(offset, 0);
		double[] size = new double[ mask.numDimensions() ];
		size[0] = width;
		size[1] = height;
		size[2] = 1;
		RandomAccess<BitType> maskCursor = mask.randomAccess();
		RectangleRegionOfInterest roi = new RectangleRegionOfInterest( offset, size);
		Cursor<BitType> firstSliceCursor = roi.getIterableIntervalOverROI(mask).cursor();
		
		final long[] pos = new long[ mask.numDimensions() ];
		while (firstSliceCursor.hasNext()) {
			firstSliceCursor.fwd();
			firstSliceCursor.localize(pos);
			BitType maskValue = firstSliceCursor.get();
			// go through all slices
			for (int i=1; i<slices; ++i) {
				pos[2] = i;
				maskCursor.setPosition(pos);
				// compare the values and assume they are the same
				int cmp = maskCursor.get().compareTo(maskValue);
				assertEquals(0, cmp);
			}
		}
	}

	@Test
	public void regularRoiPixelCountTest() {
		// load a 3D test image
		Img<UnsignedByteType> img = positiveCorrelationImageCh1;
		final long[] roiOffset = createRoiOffset(img);
		final long[] roiSize = createRoiSize(img);
		final long width = img.dimension(0);
		final long height = img.dimension(1);

		Img<UnsignedByteType> maskImg
			= TestImageAccessor.createRectengularMaskImage(width, height, roiOffset, roiSize);
		final long[] dim = new long[ img.numDimensions() ];
		img.dimensions(dim);
		Img<BitType> mask = MaskFactory.createMask(dim, maskImg);

		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				img.randomAccess(),
				img.randomAccess(),
				mask.localizingCursor());
		// calculate volume of mask bounding box
		long roiVolume = roiSize[0] * roiSize[1] * img.dimension(2);
		long count = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			count++;
		}

		assertEquals(roiVolume, count);
	}
}
