import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * The DataContainer keeps all the source data, pre-processing results and
 * algorithm results. It allows a client to iterate over its
 * contents and makes the source image and channel information available
 * to a client.

 * @param <T>
 */
public class DataContainer<T extends RealType<T>> implements Iterable<Result> {

	double meanCh1, meanCh2, minCh1, maxCh1, minCh2, maxCh2;

	// The source images that the results are based on
	Image<T> sourceImage1, sourceImage2;
	// The channels of the source images that the result relate to
	int ch1, ch2;
	/* The thresholds for both image channels. Pixels below a lower
	 * threshold do NOT include the threshold and pixels above an upper
	 * one will NOT either. Pixels "in between (and including)" thresholds
	 * do include the threshold values.
	 */
	double ch1MinThreshold, ch1MaxThreshold, ch2MinThreshold, ch2MaxThreshold;
	// The container of the results
	List<Result> resultsObjectList = new ArrayList<Result>();

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
		this.ch1 = ch1;
		this.ch2 = ch2;

		// get min and max value of image1's data type
		T dummyT = src1.createType();
		ch1MinThreshold = dummyT.getMinValue();
		ch1MaxThreshold = dummyT.getMaxValue();

		// get min and max value of image2's data type
		dummyT = src2.createType();
		ch2MinThreshold = dummyT.getMinValue();
		ch2MaxThreshold = dummyT.getMaxValue();

		meanCh1 = BasicImageStats.getImageMean(sourceImage1);
		meanCh2 = BasicImageStats.getImageMean(sourceImage2);
		minCh1 = BasicImageStats.getImageMin(sourceImage1);
		minCh2 = BasicImageStats.getImageMin(sourceImage2);
		maxCh1 = BasicImageStats.getImageMax(sourceImage1);
		maxCh2 = BasicImageStats.getImageMax(sourceImage2);
	}

	/**
	 * Adds a {@link Result} to the container.
	 *
	 * @param result The result to add.
	 */
	public void add(Result result) {
		resultsObjectList.add(result);
	}

	/**
	 * Gets an iterator over the contained results.
	 */
	public Iterator<Result> iterator() {
		return resultsObjectList.iterator();
	}

	public Image<T> getSourceImage1() {
		return sourceImage1;
	}

	public Image<T> getSourceImage2() {
		return sourceImage2;
	}

	public int getCh1() {
		return ch1;
	}

	public int getCh2() {
		return ch2;
	}

	public synchronized double getCh1MinThreshold() {
		return ch1MinThreshold;
	}

	public synchronized void setCh1MinThreshold(double ch1MinThreshold) {
		this.ch1MinThreshold = ch1MinThreshold;
	}

	public synchronized double getCh1MaxThreshold() {
		return ch1MaxThreshold;
	}

	public synchronized void setCh1MaxThreshold(double ch1MaxThreshold) {
		this.ch1MaxThreshold = ch1MaxThreshold;
	}

	public synchronized double getCh2MinThreshold() {
		return ch2MinThreshold;
	}

	public synchronized void setCh2MinThreshold(double ch2MinThreshold) {
		this.ch2MinThreshold = ch2MinThreshold;
	}

	public synchronized double getCh2MaxThreshold() {
		return ch2MaxThreshold;
	}

	public synchronized void setCh2MaxThreshold(double ch2MaxThreshold) {
		this.ch2MaxThreshold = ch2MaxThreshold;
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
}
