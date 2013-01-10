/**
 * 
 */
package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

/**
 * @author Jean-Yves Tinevez
 */
public class TrackIndexAnalyzerTest {

	private static final int N_TRACKS = 10;
	private static final int DEPTH = 5;
	private TrackMateModel model;

	/** Create a simple linear graph with {@value #N_TRACKS} tracks. */
	@Before
	public void setUp() {
		model = new TrackMateModel();
		model.beginUpdate();
		try {
			for (int i = 0; i < N_TRACKS; i++) {
				Spot previous = null;
				for (int j = 0; j < DEPTH; j++) {
					Spot spot = new Spot(new double[3]);
					model.addSpotTo(spot, j);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					}
					previous = spot;
				}
			}
		} finally {
			model.endUpdate();
		}
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.features.track.TrackIndexAnalyzer#process(java.util.Collection)}.
	 */
	@Test
	public final void testProcess() {
		// Compute track index
		Set<Integer> trackIDs = model.getTrackModel().getFilteredTrackIDs();
		TrackIndexAnalyzer analyzer = new TrackIndexAnalyzer(model);
		analyzer.process(trackIDs);

		// Collect track indices
		ArrayList<Integer> trackIndices = new ArrayList<Integer>(trackIDs.size());
		for (Integer trackID : trackIDs) {
			trackIndices.add(model.getFeatureModel().getTrackFeature(trackID, TrackIndexAnalyzer.TRACK_INDEX).intValue());
		}

		//Check values: they must be 0, 1, 2, ... in the order of the filtered track IDs (which reflect track names order)
		for (int i = 0; i < N_TRACKS; i++) {
			assertEquals("Bad track index:", (long) i, trackIndices.get(i).longValue());
		}
	}

	/**
	 * Test method for {@link fiji.plugin.trackmate.features.track.TrackIndexAnalyzer#modelChanged(fiji.plugin.trackmate.ModelChangeEvent)}.
	 */
	@Test
	public final void testModelChanged() {


		// Compute track index
		Set<Integer> trackIDs = model.getTrackModel().getFilteredTrackIDs();
		final TestTrackIndexAnalyzer analyzer = new TestTrackIndexAnalyzer(model);
		analyzer.process(trackIDs);
		assertTrue(analyzer.hasBeenCalled);

		// Collect track indices
		ArrayList<Integer> trackIndices = new ArrayList<Integer>(trackIDs.size());
		for (Integer trackID : trackIDs) {
			trackIndices.add(model.getFeatureModel().getTrackFeature(trackID, TrackIndexAnalyzer.TRACK_INDEX).intValue());
		}

		// Reset analyzer
		analyzer.hasBeenCalled = false;

		// Prepare listener -> forward to analyzer
		ModelChangeListener listener = new ModelChangeListener() {
			@Override
			public void modelChanged(ModelChangeEvent event) {
				if (analyzer.isLocal()) {
					analyzer.process(event.getTrackUpdated());
				} else {
					analyzer.process(model.getTrackModel().getFilteredTrackIDs());
				}
			}
		};

		/*
		 *  Modify the model a first time:
		 *  We attach a new spot to an existing track. It must not modify the 
		 *  track indices, nor generate a call to recalculate them. 
		 */
		model.addTrackMateModelChangeListener(listener);
		model.beginUpdate();
		try {
			Spot targetSpot = model.getFilteredSpots().get(0).iterator().next();
			Spot newSpot = model.addSpotTo(new Spot(new double[3]), 1);
			model.addEdge(targetSpot, newSpot, 1);
		} finally {
			model.endUpdate();
		}

		// Reset analyzer
		analyzer.hasBeenCalled = false;

		/*
		 * Second modification: we create a new track by cutting one track in the middle
		 */
		model.addTrackMateModelChangeListener(listener);
		model.beginUpdate();
		try {
			Spot targetSpot = model.getFilteredSpots().get(DEPTH/2).iterator().next();
			model.removeSpotFrom(targetSpot, DEPTH/2);
		} finally {
			model.endUpdate();
		}

		// Process method must have been called
		assertTrue(analyzer.hasBeenCalled);

		// There must N_TRACKS+1 indices now
		trackIDs = model.getTrackModel().getFilteredTrackIDs();
		assertEquals((long) N_TRACKS+1,	(long) trackIDs.size());

		// With correct indices
		Iterator<Integer> it = trackIDs.iterator();
		for (int i = 0; i < trackIDs.size(); i++) {
			assertEquals((long) i, model.getFeatureModel().getTrackFeature(it.next(), TrackIndexAnalyzer.TRACK_INDEX).longValue());
		}
		// FIXME
		// FAILS BECAUSE TRANCK INDEX IS A GLOBAL TRACK ANALYZER AND NEEDS TO RECOMPUTE FOR THE WHOLE MODEL
		// C:EST LA VIE

	}

	/**
	 *  Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackIndexAnalyzer extends TrackIndexAnalyzer {

		private boolean hasBeenCalled = false;

		public TestTrackIndexAnalyzer(TrackMateModel model) {
			super(model);
		}

		@Override
		public void process(Collection<Integer> trackIDs) {
			hasBeenCalled = true;
			super.process(trackIDs);
		}
	}


}
