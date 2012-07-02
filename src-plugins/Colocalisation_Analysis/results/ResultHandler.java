package results;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import algorithms.Histogram2D;

/**
 * A result handler offers different methods to process results
 * of algorithms. Algorithms get passed such a result handler and
 * can let the handler process whatever information they like.
 *
 * @param <T> The source images value type
 */
public interface ResultHandler<T extends RealType<T>> {

	void handleImage(RandomAccessibleInterval<T> image, String name);

	void handleHistogram(Histogram2D<T> histogram, String name);

	void handleWarning(Warning warning);

	void handleValue(String name, double value);

	void handleValue(String name, double value, int decimals);

	/**
	 * The process method should start the processing of the
	 * previously collected results. E.g. it could show some
	 * windows or produce a final zip file.
	 */
	void process();
}
