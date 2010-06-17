import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

	// The source images that the results are based on
	Image<T> sourceImage1, sourceImage2;
	// The channels of the source images that the result relate to
	int ch1, ch2;
	/* The thresholds for both image channels. Pixels below a lower
	 * threshold do NOT include the threshold and pixels above an upper
	 * one will NOT either. Pixels "in between (and including)" thresholds
	 * do include the threshold values.
	 */
	T ch1MinThreshold, ch1MaxThreshold, ch2MinThreshold, ch2MaxThreshold;
	// The container of the results
	List<Result> resultsObjectList = new ArrayList<Result>();

	/**
	 * Creates a new {@link DataContainer} for a specific set of image and
	 * channel combination.
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
	}

	/**
	 * Adds a {@link Result} to the container.
	 *
	 * @param result The result to add.
	 */
	public void add(Result result){
		resultsObjectList.add(result);
	}

	/**
	 * Gets an iterator over the contained results.
	 */
	public Iterator<Result> iterator() {
		return resultsObjectList.iterator();
	}
}