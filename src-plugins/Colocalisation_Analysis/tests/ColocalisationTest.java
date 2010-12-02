package tests;

import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.junit.After;
import org.junit.Before;


public abstract class ColocalisationTest {

	// images and meta data for zero correlation
	Image<FloatType> zeroCorrelationImageCh1;
	Image<FloatType> zeroCorrelationImageCh2;
	double zeroCorrelationImageCh1Mean;
	double zeroCorrelationImageCh2Mean;

	// images and meta data for positive correlation
	Image<FloatType> positiveCorrelationImageCh1;
	Image<FloatType> positiveCorrelationImageCh2;
	double positiveCorrelationImageCh1Mean;
	double positiveCorrelationImageCh2Mean;

	/**
	 * This method is run before every single test is run and is meant to set up
	 * the images and meta data needed for testing image colocalisation.
	 */
	@Before
	public void setup() {
		zeroCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("Data/greenZstack.tif");
		zeroCorrelationImageCh1Mean = ImageStatistics.getImageMean(zeroCorrelationImageCh1);

		zeroCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("Data/redZstack.tif");
		zeroCorrelationImageCh2Mean = ImageStatistics.getImageMean(zeroCorrelationImageCh2);

		positiveCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("Data/colocsample1b-green.tif");
		positiveCorrelationImageCh1Mean = ImageStatistics.getImageMean(positiveCorrelationImageCh1);

		positiveCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("Data/colocsample1b-red.tif");
		positiveCorrelationImageCh2Mean = ImageStatistics.getImageMean(positiveCorrelationImageCh2);
	}

	/**
	 * This method is run after every single test and is meant to clean up.
	 */
	@After
	public void cleanup() {
		zeroCorrelationImageCh1.closeAllCursors();
		zeroCorrelationImageCh2.closeAllCursors();
		positiveCorrelationImageCh1.closeAllCursors();
		positiveCorrelationImageCh2.closeAllCursors();
	}
}