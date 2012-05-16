package tests;

import gadgets.MaskFactory;
import net.imglib2.algorithm.math.ImageStatistics;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.After;
import org.junit.Before;


public abstract class ColocalisationTest {

	// images and meta data for zero correlation
	Img<UnsignedByteType> zeroCorrelationImageCh1;
	Img<UnsignedByteType> zeroCorrelationImageCh2;
	Img<BitType> zeroCorrelationAlwaysTrueMask;
	double zeroCorrelationImageCh1Mean;
	double zeroCorrelationImageCh2Mean;

	// images and meta data for positive correlation
	Img<UnsignedByteType> positiveCorrelationImageCh1;
	Img<UnsignedByteType> positiveCorrelationImageCh2;
	Img<BitType> positiveCorrelationAlwaysTrueMask;
	double positiveCorrelationImageCh1Mean;
	double positiveCorrelationImageCh2Mean;

	// images and meta data for a synthetic negative correlation dataset
	Img<UnsignedByteType> syntheticNegativeCorrelationImageCh1;
	Img<UnsignedByteType> syntheticNegativeCorrelationImageCh2;
	Img<BitType> syntheticNegativeCorrelationAlwaysTrueMask;
	double syntheticNegativeCorrelationImageCh1Mean;
	double syntheticNegativeCorrelationImageCh2Mean;
	
	// images like in the manders paper
	Img<UnsignedByteType> mandersA, mandersB, mandersC, mandersD,
		mandersE, mandersF, mandersG, mandersH, mandersI;
	Img<BitType> mandersAlwaysTrueMask;

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

		final long[] dimZeroCorrCh1 = new long[ zeroCorrelationImageCh1.numDimensions() ];
		zeroCorrelationImageCh1.dimensions(dimZeroCorrCh1);
		zeroCorrelationAlwaysTrueMask = MaskFactory.createMask(dimZeroCorrCh1, true);

		positiveCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("Data/colocsample1b-green.tif");
		positiveCorrelationImageCh1Mean = ImageStatistics.getImageMean(positiveCorrelationImageCh1);

		positiveCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("Data/colocsample1b-red.tif");
		positiveCorrelationImageCh2Mean = ImageStatistics.getImageMean(positiveCorrelationImageCh2);

		final long[] dimPosCorrCh1 = new long[ positiveCorrelationImageCh1.numDimensions() ];
		positiveCorrelationImageCh1.dimensions(dimPosCorrCh1);
		positiveCorrelationAlwaysTrueMask = MaskFactory.createMask(dimPosCorrCh1, true);

		syntheticNegativeCorrelationImageCh1 = TestImageAccessor.loadTiffFromJar("Data/syntheticNegCh1.tif");
		syntheticNegativeCorrelationImageCh1Mean = ImageStatistics.getImageMean(syntheticNegativeCorrelationImageCh1);

		syntheticNegativeCorrelationImageCh2 = TestImageAccessor.loadTiffFromJar("Data/syntheticNegCh2.tif");
		syntheticNegativeCorrelationImageCh2Mean = ImageStatistics.getImageMean(syntheticNegativeCorrelationImageCh2);

		final long[] dimSynthNegCorrCh1 = new long[ syntheticNegativeCorrelationImageCh1.numDimensions() ];
		syntheticNegativeCorrelationImageCh1.dimensions(dimSynthNegCorrCh1);
		syntheticNegativeCorrelationAlwaysTrueMask = MaskFactory.createMask(dimSynthNegCorrCh1, true);
		
		mandersA = TestImageAccessor.loadTiffFromJar("Data/mandersA.tiff");
		mandersB = TestImageAccessor.loadTiffFromJar("Data/mandersB.tiff");
		mandersC = TestImageAccessor.loadTiffFromJar("Data/mandersC.tiff");
		mandersD = TestImageAccessor.loadTiffFromJar("Data/mandersD.tiff");
		mandersE = TestImageAccessor.loadTiffFromJar("Data/mandersE.tiff");
		mandersF = TestImageAccessor.loadTiffFromJar("Data/mandersF.tiff");
		mandersG = TestImageAccessor.loadTiffFromJar("Data/mandersG.tiff");
		mandersH = TestImageAccessor.loadTiffFromJar("Data/mandersH.tiff");
		mandersI = TestImageAccessor.loadTiffFromJar("Data/mandersI.tiff");

		final long[] dimMandersA = new long[ mandersA.numDimensions() ];
		mandersA.dimensions(dimMandersA);
		mandersAlwaysTrueMask = MaskFactory.createMask(dimMandersA, true);
	}

	/**
	 * This method is run after every single test and is meant to clean up.
	 */
	@After
	public void cleanup() {
		// nothing to do
	}

	/**
	 * Creates a ROI offset array with a distance of 1/4 to the origin
	 * in each dimension.
	 */
	protected <T extends RealType<T>> long[] createRoiOffset(Img<T> img) {
		final long[] offset = new long[ img.numDimensions() ];
		img.dimensions(offset);
		for (int i=0; i<offset.length; i++) {
			offset[i] = Math.max(1, img.dimension(i) / 4);
		}
		return offset;
	}

	/**
	 * Creates a ROI size array with a size of 1/2 of each
	 * dimension.
	 */
	protected <T extends RealType<T>> long[] createRoiSize(Img<T> img) {
		final long[] size = new long[ img.numDimensions() ];
		img.dimensions(size);
		for (int i=0; i<size.length; i++) {
			size[i] = Math.max(1, img.dimension(i) / 2);
		}
		return size;
	}
}