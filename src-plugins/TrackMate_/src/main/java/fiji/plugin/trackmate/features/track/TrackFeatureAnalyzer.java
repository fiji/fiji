package fiji.plugin.trackmate.features.track;

import java.util.Collection;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Mother interface for the classes that can compute the feature of tracks.
 * Target tracks are given through their IDs, which means that concrete implementations
 * must be instantiated with the model that stores the tracks. 
 * <p>
 * Note: ideally concrete implementation should work in a multi-threaded fashion
 * for performance reason, when possible.
 * 
 * @author Jean-Yves Tinevez
 */
public interface TrackFeatureAnalyzer<T extends RealType<T> & NativeType<T>> extends Benchmark {
	
	/**
	 * Score the track whose ID is given.
	 */
	public void process(final Collection<Integer> trackIDs);
	
}
