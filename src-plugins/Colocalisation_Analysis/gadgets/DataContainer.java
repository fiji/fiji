package gadgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import algorithms.Algorithm;
import algorithms.AutoThresholdRegression;
import algorithms.InputCheck;

/**
 * The DataContainer keeps all the source data, pre-processing results and
 * algorithms that have been executed. It allows a client to get most its
 * content and makes the source image and channel information available
 * to a client.

 * @param <T>
 */
public class DataContainer<T extends RealType<T>> {

	// some general image statistics
	double meanCh1, meanCh2, minCh1, maxCh1, minCh2, maxCh2, integralCh1, integralCh2;
	// The source images that the results are based on
	Image<T> sourceImage1, sourceImage2;
	// The mask for the images
	Image<BitType> mask;

	// The channels of the source images that the result relate to
	int ch1, ch2;
	// The mask, clipped to its bounding box, if irregular ROI or a mask is use
	protected Image<T> maskBB = null;
	protected int[] maskBBSize = null;
	protected int[] maskBBOffset = null;
	// indicates if a regular ROI is in use
	protected boolean regularRoiInUse = false;

	InputCheck<T> inputCheck = null;
	AutoThresholdRegression<T> autoThreshold = null;

	// a list that contains all added algorithms
	List< Algorithm<T> > algorithms = new ArrayList< Algorithm<T> >();

	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination.
	 * We create default thresholds here that are the max and min of the
	 * type of the source image channels.
	 *
	 * @param src1 The channel one image source
	 * @param src2 The channel two image source
	 * @param ch1 The channel one image channel
	 * @param ch2 The channel two image channel
	 */
	public DataContainer(Image<T> src1, Image<T> src2, int ch1, int ch2) {
		sourceImage1 = src1;
		sourceImage2 = src2;
		mask = MaskFactory.createMask(src1.getDimensions(), true);
		this.ch1 = ch1;
		this.ch2 = ch2;

		meanCh1 = ImageStatistics.getImageMean(sourceImage1);
		meanCh2 = ImageStatistics.getImageMean(sourceImage2);
		minCh1 = ImageStatistics.getImageMin(sourceImage1).getRealDouble();
		minCh2 = ImageStatistics.getImageMin(sourceImage2).getRealDouble();
		maxCh1 = ImageStatistics.getImageMax(sourceImage1).getRealDouble();
		maxCh2 = ImageStatistics.getImageMax(sourceImage2).getRealDouble();
		integralCh1 = ImageStatistics.getImageIntegral(sourceImage1);
		integralCh2 = ImageStatistics.getImageIntegral(sourceImage2);
	}

	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination. It will give access to the image according to
	 * the misk passed. It is expected that the mask is of the same size
	 * as an image slice. Default thresholds, min, max and mean will be set
	 * according to the mask as well.
	 *
	 * @param src1 The channel one image source
	 * @param src2 The channel two image source
	 * @param ch1 The channel one image channel
	 * @param ch2 The channel two image channel
	 * @param mask The mask to use
	 * @param offset The offset of the ROI in each dimension
	 * @param size The size of the ROI in each dimension
	 */
	public DataContainer(Image<T> src1, Image<T> src2, int ch1, int ch2,
			final Image<T> mask, final Image<T> maskBB,
			final int[] offset, final int[] size) {
		this(new MaskedImage<T>(src1, mask, offset, size), new MaskedImage<T>(src2, mask, offset, size),
			 ch1, ch2);
		this.maskBB = maskBB;
		/* The maskBBOffset will just be zero for all directions.
		 * That is because we later need it to create a MaskImage
		 * with only the size of the mask and therefore need no
		 * offset.
		 */
		maskBBOffset = offset.clone();
		Arrays.fill(maskBBOffset, 0);

		maskBBSize = size.clone();
	}

	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination. It will give access to the image according to
	 * the region of interest (ROI) passed. Default thresholds, min, max and
	 * mean will be set according to the ROI as well.
	 *
	 * @param src1 The channel one image source
	 * @param src2 The channel two image source
	 * @param ch1 The channel one image channel
	 * @param ch2 The channel two image channel
	 * @param offset The offset of the ROI in each dimension
	 * @param size The size of the ROI in each dimension
	 */
	public DataContainer(Image<T> src1, Image<T> src2, int ch1, int ch2,
			final int[] offset, final int size[]) {
		this(new RoiImage<T>(src1, offset, size), new RoiImage<T>(src2, offset, size),
			 ch1, ch2);
		regularRoiInUse = true;
	}

	/**
	 * This method will build a new image object that is based on
	 * the type of source image. If source images don't have a ROI
	 * or a mask, the image will be returned as is. The same is
	 * true for a regular ROI. Otherwise a MaskImage is returned
	 * that contains tha same mask as the one used during container
	 * creation.
	 */
	public Image<T> maskImageIfNeeded(Image<T> image) {
		// return the image on normal image or reg. ROI
		if (maskBB == null)
			return image;

		return new MaskedImage<T>(image, maskBB, maskBBOffset.clone(), maskBBSize.clone());
	}

	/**
	 * Indicates if a regular ROI is in use.
	 */
	public boolean isRoiInUse() {
		return regularRoiInUse;
	}

	/**
	 * Gets if a mask or irregular ROI is in use.
	 */
	public boolean isMaskInUse() {
		return (maskBB != null);
	}

	public Image<T> getSourceImage1() {
		return sourceImage1;
	}

	public Image<T> getSourceImage2() {
		return sourceImage2;
	}

	public Image<BitType> getMask() {
		return mask;
	}

	public int getCh1() {
		return ch1;
	}

	public int getCh2() {
		return ch2;
	}
	public double getMeanCh1() {
		return meanCh1;
	}

	public double getMeanCh2() {
		return meanCh2;
	}

	public double getMinCh1() {
		return minCh1;
	}

	public double getMaxCh1() {
		return maxCh1;
	}

	public double getMinCh2() {
		return minCh2;
	}

	public double getMaxCh2() {
		return maxCh2;
	}

	public double getIntegralCh1() {
		return integralCh1;
	}

	public double getIntegralCh2() {
		return integralCh2;
	}

	public InputCheck<T> getInputCheck() {
		return inputCheck;
	}

	public Algorithm<T> setInputCheck(InputCheck<T> inputCheck) {
		this.inputCheck = inputCheck;
		return inputCheck;
	}

	public AutoThresholdRegression<T> getAutoThreshold() {
		return autoThreshold;
	}

	public Algorithm<T> setAutoThreshold(AutoThresholdRegression<T> autoThreshold) {
		this.autoThreshold = autoThreshold;
		return autoThreshold;
	}
}
