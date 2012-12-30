package fiji.plugin.trackmate.features.track;

import java.util.Collection;

import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateModelChangeListener;

import net.imglib2.algorithm.Benchmark;

/**
 * Mother interface for the classes that can compute the feature of tracks.
 * Target tracks are given through their IDs, which means that concrete implementations
 * must be instantiated with the model that stores the tracks. 
 * <p>
 * Note: ideally concrete implementation should work in a multi-threaded fashion
 * for performance reason, when possible.
 * <p>
 * For {@link TrackFeatureAnalyzer}s, there is a mechanism intended to maintain
 * the model integrity against manual, small changes: the {@link #modelChanged(TrackMateModelChangeEvent)}
 * method. Something as simple as removing
 * a spot in the middle of a track will generate two new tracks, which will invalidate 
 * all feature values for the old track. Analyzers are notified of such events, so
 * that they can recompute track features after the change.
 * <p>
 * A simple way would be to recompute all track features at once, but this might be too
 * long and overkill for changes that do not affect all tracks (<i>e.g.</i> adding 
 * a lonely spot, or a new track is likely not to affect all tracks in some case). 
 * So with this method, concrete implementation are free to have convoluted techniques
 * to optimize computation time.
 * <p>
 * Though this method comes from the {@link TrackMateModelChangeListener} interface,
 * it will be called <b>before</b> the event is dispatched to other listeners 
 * (such as views, etc...), so that they can reflect the change in track feature 
 * value should they need it. 
 * 
 * @author Jean-Yves Tinevez
 */
public interface TrackFeatureAnalyzer extends Benchmark, TrackMateModelChangeListener {

	/**
	 * Score the track whose ID is given.
	 */
	public void process(final Collection<Integer> trackIDs);

	/**
	 * Notified when a model change event happens.
	 * <p>
	 * For {@link TrackFeatureAnalyzer}s, this is a mechanism intended to maintain
	 * the model integrity against manual, small changes. Something as simple as removing
	 * a spot in the middle of a track will generate two new tracks, which will invalidate 
	 * all feature values for the old track. Analyzers are notified of such events, so
	 * that they can recompute track features after the change.
	 * <p>
	 * A simple way would be to recompute all track features at once, but this might be too
	 * long and overkill for changes that do not affect all tracks (<i>e.g.</i> adding 
	 * a lonely spot, or a new track is likely not to affect all tracks in some case). 
	 * So with this method, concrete implementation are free to have convoluted techniques
	 * to optimize computation time.
	 * <p>
	 * Though this method comes from the {@link TrackMateModelChangeListener} interface,
	 * it will be called <b>before</b> the event is dispatched to other listeners 
	 * (such as views, etc...), so that they can reflect the change in track feature 
	 * value should they need it. 
	 */
	@Override
	public void modelChanged(TrackMateModelChangeEvent event);

}
