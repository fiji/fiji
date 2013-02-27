package fiji.plugin.trackmate.features.track;

import java.util.Collection;

import net.imglib2.algorithm.Benchmark;
import fiji.plugin.trackmate.TrackMateModel;

/**
 * Mother interface for the classes that can compute the feature of tracks.
 * Target tracks are given through their IDs, which means that concrete implementations
 * must be instantiated with the model that stores the tracks. 
 * <p>
 * Note: ideally concrete implementation should work in a multi-threaded fashion
 * for performance reason, when possible.
 * <p>
 * For {@link TrackAnalyzer}s, there is a mechanism intended to maintain
 * the model integrity against manual, small changes. Something as simple as removing
 * a spot in the middle of a track will generate two new tracks, which will invalidate 
 * all feature values for the old track. Analyzers are notified of such events, so
 * that they can recompute track features after the change.
 * <p>
 * A simple way would be to recompute all track features at once, but this might be too
 * long and overkill for changes that do not affect all tracks (<i>e.g.</i> adding 
 * a lonely spot, or a new track is likely not to affect all tracks in some case).
 * <p>
 * So the {@link #process(Collection)} will be called selectively on new or modified
 * tracks every time a change happens. It will be called from the {@link TrackMateModel}
 * after a {@link TrackMateModel#endUpdate()}, before any listener gets notified.
 * 
 * @author Jean-Yves Tinevez
 */
public interface TrackAnalyzer extends Benchmark {

	/**
	 * Score the track whose ID is given.
	 */
	public void process(final Collection<Integer> trackIDs);
	
	/**
	 * @return <code>true</code> if this analyzer is a local analyzer. That is: a modification that
	 * affects only one track requires the track features to be re-calculated only for
	 * this track. If <code>false</code>, any model modification involving edges will trigger
	 * a recalculation over all the visible tracks of the model.
	 */
	public boolean isLocal();

}
