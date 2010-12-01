package results;

import algorithms.Histogram2D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.IntegerType;
import mpicbg.imglib.type.numeric.RealType;

/**
 * A result handler offers different methods to process results
 * of algorithms. Algorithms get passed such a result handler and
 * can let the handler process whatever information they like.
 *
 * @param <T> The source images value type
 */
public interface ResultHandler<T extends RealType<T>> {

	void handleImage(Image<T> image);

	void handleHistogram(Histogram2D<T> histogram);

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
