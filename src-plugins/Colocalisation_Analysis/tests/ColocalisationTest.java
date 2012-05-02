package tests;

import gadgets.MaskFactory;
import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

import org.junit.After;
import org.junit.Before;


public abstract class ColocalisationTest {

	// images and meta data for zero correlation
	Image<UnsignedByteType> zeroCorrelationImageCh1;
	Image<UnsignedByteType> zeroCorrelationImageCh2;
	Image<BitType> zeroCorrelationAlwaysTrueMask;
	double zeroCorrelationImageCh1Mean;
	double zeroCorrelationImageCh2Mean;

	// images and meta data for positive correlation
	Image<UnsignedByteType> positiveCorrelationImageCh1;
	Image<UnsignedByteType> positiveCorrelationImageCh2;
	Image<BitType> positiveCorrelationAlwaysTrueMask;
	double positiveCorrelationImageCh1Mean;
	double positiveCorrelationImageCh2Mean;

	// images and meta data for a synthetic negative correlation dataset
	Image<UnsignedByteType> syntheticNegativeCorrelationImageCh1;
	Image<UnsignedByteType> syntheticNegativeCorrelationImageCh2;
	Image<BitType> syntheticNegativeCorrelationAlwaysTrueMask;
	double syntheticNegativeCorrelationImageCh1Mean;
	double syntheticNegativeCorrelationImageCh2Mean;
	
	// images like in the manders paper
	Image<UnsignedByteType> mandersA, mandersB, mandersC, mandersD,
		mandersE, mandersF, mandersG, mandersH, mandersI;
	Image<BitType> mandersAlwaysTrueMask;

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

		zeroCorrelationAlwaysTrueMask = MaskFactory.createMask(zeroCorrelationImageCh1.getDimensions(), true);

		positiveCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("Data/colocsample1b-green.tif");
		positiveCorrelationImageCh1Mean = ImageStatistics.getImageMean(positiveCorrelationImageCh1);

		positiveCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("Data/colocsample1b-red.tif");
		positiveCorrelationImageCh2Mean = ImageStatistics.getImageMean(positiveCorrelationImageCh2);

		positiveCorrelationAlwaysTrueMask = MaskFactory.createMask(positiveCorrelationImageCh1.getDimensions(), true);

		/*
		syntheticNegativeCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("Data/syntheticNegCh1.tif");
		syntheticNegativeCorrelationImageCh1Mean = ImageStatistics.getImageMean(syntheticNegativeCorrelationImageCh1);

		syntheticNegativeCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("Data/syntheticNegCh2.tif");
		syntheticNegativeCorrelationImageCh2Mean = ImageStatistics.getImageMean(syntheticNegativeCorrelationImageCh2);

		syntheticNegativeCorrelationAlwaysTrueMask = MaskFactory.createMask(syntheticNegativeCorrelationImageCh1.getDimensions(), true);
		*/
		
		mandersA = TestImageAccessor.loadTiffFromJar("Data/mandersA.tiff");
		mandersB = TestImageAccessor.loadTiffFromJar("Data/mandersB.tiff");
		mandersC = TestImageAccessor.loadTiffFromJar("Data/mandersC.tiff");
		mandersD = TestImageAccessor.loadTiffFromJar("Data/mandersD.tiff");
		mandersE = TestImageAccessor.loadTiffFromJar("Data/mandersE.tiff");
		mandersF = TestImageAccessor.loadTiffFromJar("Data/mandersF.tiff");
		mandersG = TestImageAccessor.loadTiffFromJar("Data/mandersG.tiff");
		mandersH = TestImageAccessor.loadTiffFromJar("Data/mandersH.tiff");
		mandersI = TestImageAccessor.loadTiffFromJar("Data/mandersI.tiff");

		mandersAlwaysTrueMask = MaskFactory.createMask(mandersA.getDimensions(), true);
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

		mandersA.closeAllCursors();
		mandersB.closeAllCursors();
		mandersC.closeAllCursors();
		mandersD.closeAllCursors();
		mandersE.closeAllCursors();
		mandersF.closeAllCursors();
		mandersG.closeAllCursors();
		mandersH.closeAllCursors();
		mandersI.closeAllCursors();
	}

	/**
	 * Creates a ROI offset array with a distance of 1/4 to the origin
	 * in each dimension.
	 */
	protected <T extends RealType<T>> int[] createRoiOffset(Image<T> img) {
		int[] offset = img.createPositionArray();
		for (int i=0; i<offset.length; i++) {
			offset[i] = Math.max(1, img.getDimension(i) / 4);
		}
		return offset;
	}

	/**
	 * Creates a ROI size array with a size of 1/2 of each
	 * dimension.
	 */
	protected <T extends RealType<T>> int[] createRoiSize(Image<T> img) {
		int[] size = img.createPositionArray();
		for (int i=0; i<size.length; i++) {
			size[i] = Math.max(1, img.getDimension(i) / 2);
		}
		return size;
	}
}