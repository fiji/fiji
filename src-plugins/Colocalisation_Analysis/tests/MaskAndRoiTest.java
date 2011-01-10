package tests;

import static org.junit.Assert.assertTrue;
import gadgets.MaskedImage;
import gadgets.RoiImage;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursorFactory;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.junit.Test;

import com.lowagie.text.ImgTemplate;

import clojure.test.junit__init;

import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;

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
}
