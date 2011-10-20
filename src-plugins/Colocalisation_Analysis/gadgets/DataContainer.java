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
import algorithms.MissingPreconditionException;

/**
 * The DataContainer keeps all the source data, pre-processing results and
 * algorithms that have been executed. It allows a client to get most its
 * content and makes the source image and channel information available
 * to a client.

 * @param <T>
 */
public class DataContainer<T extends RealType<T>> {
	// enumeration of different mask types
	public enum MaskType { Regular, Irregular, None };
	// some general image statistics
	double meanCh1, meanCh2, minCh1, maxCh1, minCh2, maxCh2, integralCh1, integralCh2;
	// The source images that the results are based on
	Image<T> sourceImage1, sourceImage2;
	// The mask for the images
	Image<BitType> mask;
	// Type of the used mask
	protected MaskType maskType;

	// The channels of the source images that the result relate to
	int ch1, ch2;
	// The mask, clipped to its bounding box, if irregular ROI or a mask is use
	protected Image<T> maskBB = null;
	protected int[] maskBBSize = null;
	protected int[] maskBBOffset = null;

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
		// create a mask that is everywhere valid
		mask = MaskFactory.createMask(src1.getDimensions(), true);
		this.ch1 = ch1;
		this.ch2 = ch2;
		// fill mask dimension information, here the whole image
		maskBBOffset = mask.createPositionArray();
		Arrays.fill(maskBBOffset, 0);
		maskBBSize = mask.getDimensions();
		// indicated that there is actually no mask
		maskType = MaskType.None;

		calculateStatistics();
	}

	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination. It will give access to the image according to
	 * the mask passed. It is expected that the mask is of the same size
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
	 * @throws MissingPreconditionException
	 */
	public DataContainer(Image<T> src1, Image<T> src2, int ch1, int ch2,
			final Image<T> mask, final Image<T> maskBB, final int[] offset,
			final int[] size) throws MissingPreconditionException {
		sourceImage1 = src1;
		sourceImage2 = src2;
		this.ch1 = ch1;
		this.ch2 = ch2;

		this.mask = MaskFactory.createMask(src1.getDimensions(), mask);
		this.maskBB = maskBB;

		final int[] dim = src1.getDimensions();
		maskBBOffset = src1.createPositionArray();
		maskBBSize = src1.createPositionArray();
		// this constructor supports irregular masks
		maskType = MaskType.Irregular;
		adjustRoiOffset(offset, maskBBOffset, dim);
		adjustRoiSize(size, maskBBSize, dim, maskBBOffset);

		calculateStatistics();
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
			final int[] offset, final int size[]) throws MissingPreconditionException {
		sourceImage1 = src1;
		sourceImage2 = src2;

		final int[] dim = src1.getDimensions();
		int[] roiOffset = src1.createPositionArray();
		int[] roiSize = src1.createPositionArray();

		adjustRoiOffset(offset, roiOffset, dim);
		adjustRoiSize(size, roiSize, dim, roiOffset);

		// create a mask that is everywhere valid
		mask = MaskFactory.createMask(dim, roiOffset, roiSize);
		maskBBOffset = roiOffset.clone();
		maskBBSize = roiSize.clone();
		// this constructor only supports regular masks
		maskType = MaskType.Regular;

		this.ch1 = ch1;
		this.ch2 = ch2;

		calculateStatistics();
	}

	protected void calculateStatistics() {
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
	 * 	Make sure that the ROI offset has the same dimensionality
	 *	as the image. The method fills it up with zeros if needed.
	 *
	 * @param oldOffset The offset with the original dimensionality
	 * @param newOffset The output array with the new dimensionality
	 * @param dimensions An array of the dimensions
	 * @throws MissingPreconditionException
	 */
	protected void adjustRoiOffset(int[] oldOffset, int[] newOffset, int[] dimensions)
			throws MissingPreconditionException {
		for (int i=0; i<newOffset.length; ++i) {
			if (i < oldOffset.length) {
				if (oldOffset[i] > dimensions[i])
					throw new MissingPreconditionException("Dimension " + i + " of ROI offset is larger than image dimension.");
				newOffset[i] = oldOffset[i];
			} else {
				newOffset[i] = 0;
			}
		}
	}

	/**
	 * Transforms a ROI size array to a dimensionality. The method
	 * fill up with image (dimension - offset in that dimension) if
	 * needed.
	 *
	 * @param oldSize Size array of old dimensionality
	 * @param newSize Output size array of new dimensionality
	 * @param dimensions Dimensions representing the new dimensionality
	 * @param offset Offset of the new dimensionality
	 * @throws MissingPreconditionException
	 */
	protected void adjustRoiSize(int[] oldSize, int[] newSize, int[] dimensions, int[] offset)
			throws MissingPreconditionException {
		for (int i=0; i<newSize.length; ++i) {
			if (i < oldSize.length) {
				if (oldSize[i] > (dimensions[i] - offset[i]))
					throw new MissingPreconditionException("Dimension " + i + " of ROI size is larger than what fits in.");
				newSize[i] = oldSize[i];
			} else {
				newSize[i] = dimensions[i] - offset[i];
			}
		}
	}

	public MaskType getMaskType() {
		return maskType;
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

	public int[] getMaskBBOffset() {
		return maskBBOffset.clone();
	}

	public int[] getMaskBBSize() {
		return maskBBSize.clone();
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
