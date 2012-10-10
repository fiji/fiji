package fiji.plugin.trackmate.features.spot;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import fiji.plugin.trackmate.Spot;

/**
 * Interface for a class that can compute feature on a collection of spots.
 * <p>
 * The spot collection to operate on is given at construction by the {@link SpotFeatureAnalyzerFactory}
 * that instantiated and configured this instance. Calling the {@link #process()} method 
 * result in updating the feature map of each spot directly, calling {@link Spot#putFeature(String, double)}.
 */
public interface SpotFeatureAnalyzer<T> extends Algorithm, Benchmark {

}
