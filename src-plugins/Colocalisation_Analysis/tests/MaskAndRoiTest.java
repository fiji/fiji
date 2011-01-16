package tests;

import static org.junit.Assert.assertTrue;
import gadgets.MaskedImage;
import gadgets.RoiImage;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.junit.Test;

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
	public void regularRoiTest() {
		// create a random noise 2D image -- set roiWidh/roiSize accordingly
		//Image<FloatType> img = TestImageAccessor.produceNoiseImage(new FloatType(), 200, 300);
		// load a 3D test image
		Image<UnsignedByteType> img = positiveCorrelationImageCh1;
		int width = img.getDimension(0);
		int height = img.getDimension(1);
		int z = img.getDimension(2);
		int[] roiOffset = new int[] {width / 4, height / 4, 0};
		int[] roiSize = new int[] {width / 2, height / 2, z};
		RoiImage<UnsignedByteType> roiImage
			= new RoiImage<UnsignedByteType>(img, roiOffset, roiSize);

		// clip the actual ROI, so that the non-ROI area is removed
		ImageFactory<UnsignedByteType> maskFactory
			= new ImageFactory<UnsignedByteType>(img.createType(),
				new ArrayContainerFactory());
		Image<UnsignedByteType> clippedRoiImage
			= maskFactory.createImage( roiSize, "Clipped ROI" );
		LocalizableCursor<UnsignedByteType> roiCursor
			= roiImage.createLocalizableCursor();
		LocalizableByDimCursor<UnsignedByteType> outputCursor
			= clippedRoiImage.createLocalizableByDimCursor();

		// copy ROI data to new image
		while (roiCursor.hasNext()) {
			roiCursor.fwd();
			outputCursor.setPosition(roiCursor);
			outputCursor.getType().set( roiCursor.getType() );
		}

		roiCursor.close();
		outputCursor.close();

		/* go through the clipped ROI and compare the date to offset values
		 * of the original data.
		 */
		LocalizableCursor<UnsignedByteType> roiCopyCursor
			= clippedRoiImage.createLocalizableCursor();
		LocalizableByDimCursor<UnsignedByteType> imgCursor
		= img.createLocalizableByDimCursor();
		// create variable for summing up and set it to zero
		double sum = 0;
		while (roiCopyCursor.hasNext()) {
			roiCopyCursor.fwd();
			int[] pos = roiOffset.clone();
			for (int d=0; d<clippedRoiImage.getNumDimensions(); d++) {
				pos[d] = pos[d] + roiCopyCursor.getPosition(d);
			}
			// set position in original image
			imgCursor.setPosition(pos);
			// get ROI and original image data
			double roiData = roiCopyCursor.getType().getRealDouble();
			double imgData = imgCursor.getType().getRealDouble();
			// sum up the difference
			sum += (roiData - imgData);
		}

		// check if sum is zero
		assertTrue(Math.abs(sum) < 0.00001);
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
		// invert the image
		Image<UnsignedByteType> invImg = TestImageAccessor.invertImage(img);
		// some general mask data
		int[] maskOffset = new int[] {0, 0};
		int[] maskSize = new int[] {invImg.getDimension(0), invImg.getDimension(1)};

		/* first test - using itself as a mask */
		MaskedImage<UnsignedByteType> maskedImg1 =
			new MaskedImage<UnsignedByteType>(img, img, maskOffset, maskSize);
		Cursor<UnsignedByteType> maskedCursor = maskedImg1.createCursor();

		while (maskedCursor.hasNext()) {
			maskedCursor.fwd();
			assertTrue( maskedCursor.getType().getInteger() != 0 );
		}

		/* second test - using inverted image */
		MaskedImage<UnsignedByteType> invMaskedImg =
			new MaskedImage<UnsignedByteType>(img, invImg, maskOffset, maskSize);
		Cursor<UnsignedByteType> invMaskedCursor = invMaskedImg.createCursor();

		while (invMaskedCursor.hasNext()) {
			invMaskedCursor.fwd();
			assertTrue( invMaskedCursor.getType().getInteger() == 0 );
		}
	}
}
