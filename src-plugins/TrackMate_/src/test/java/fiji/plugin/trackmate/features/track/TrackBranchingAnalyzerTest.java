package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateModelChangeListener;

public class TrackBranchingAnalyzerTest {

	private static final int N_LINEAR_TRACKS = 5;
	private static final int N_TRACKS_WITH_GAPS = 7;
	private static final int N_TRACKS_WITH_SPLITS = 9;
	private static final int N_TRACKS_WITH_MERGES = 11;
	private static final int DEPTH = 9;
	private TrackMateModel model;

	@Before
	public void setUp() {
		model = new TrackMateModel();
		model.beginUpdate();
		try {
			// linear tracks
			for (int i = 0; i < N_LINEAR_TRACKS; i++) {
				Spot previous = null;
				for (int j = 0; j < DEPTH; j++) {
					Spot spot = new SpotImp(new double[3]);
					model.addSpotTo(spot, j);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					}
					previous = spot;
				}
			}
			// tracks with gaps
			for (int i = 0; i < N_TRACKS_WITH_GAPS; i++) {
				Spot previous = null;
				for (int j = 0; j < DEPTH; j++) {
					if (j == DEPTH/2) {
						continue;
					}
					Spot spot = new SpotImp(new double[3]);
					model.addSpotTo(spot, j);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					}
					previous = spot;
				}
			}
			// tracks with splits
			for (int i = 0; i < N_TRACKS_WITH_SPLITS; i++) {
				Spot previous = null;
				Spot split = null;
				for (int j = 0; j < DEPTH; j++) {
					Spot spot = new SpotImp(new double[3]);
					if (j == DEPTH/2) {
						split = spot;
					}
					model.addSpotTo(spot, j);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					}
					previous = spot;
				}
				previous = split;
				for (int j = DEPTH/2+1; j < DEPTH; j++) {
					Spot spot = new SpotImp(new double[3]);
					model.addSpotTo(spot, j);
					model.addEdge(previous, spot, 1);
					previous = spot;
				}
			}
			// tracks with merges
			for (int i = 0; i < N_TRACKS_WITH_MERGES; i++) {
				Spot previous = null;
				Spot merge = null;
				for (int j = 0; j < DEPTH; j++) {
					Spot spot = new SpotImp(new double[3]);
					if (j == DEPTH/2) {
						merge = spot;
					}
					model.addSpotTo(spot, j);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					}
					previous = spot;
				}
				previous = null;
				for (int j = 0; j < DEPTH/2; j++) {
					Spot spot = new SpotImp(new double[3]);
					model.addSpotTo(spot, j);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					}
					previous = spot;
				}
				model.addEdge(previous, merge, 1);
			}

		} finally {
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess() {
		// Process model
		TrackBranchingAnalyzer analyzer = new TrackBranchingAnalyzer(model);
		analyzer.process(model.getTrackModel().getFilteredTrackIDs());

		// Collect features
		int nTracksWithGaps = 0;
		int nTracksWithSplits = 0;
		int nTracksWithMerges = 0;
		int nTracksWithNothing = 0;
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
			nTracksWithGaps 	+= model.getFeatureModel().getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_GAPS);
			nTracksWithMerges 	+= model.getFeatureModel().getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_MERGES);
			nTracksWithSplits 	+= model.getFeatureModel().getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_SPLITS);
			if (0 == model.getFeatureModel().getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_GAPS) 
					&& 0 == model.getFeatureModel().getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_MERGES) 
					&&0 == model.getFeatureModel().getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_SPLITS)) {
				nTracksWithNothing++;
			}
		}
		assertEquals(N_LINEAR_TRACKS, nTracksWithNothing);
		assertEquals(N_TRACKS_WITH_GAPS, nTracksWithGaps);
		assertEquals(N_TRACKS_WITH_MERGES, nTracksWithMerges);
		assertEquals(N_TRACKS_WITH_SPLITS, nTracksWithSplits);
	}

	@Test
	public final void testModelChanged() {
		// Copy old keys
		HashSet<Integer> oldKeys = new HashSet<Integer>(model.getTrackModel().getFilteredTrackIDs());
		
		// First analysis
		final TestTrackBranchingAnalyzer analyzer = new TestTrackBranchingAnalyzer(model);
		analyzer.process(oldKeys);
		
		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;
		
		// Prepare listener for model change
		TrackMateModelChangeListener listener = new TrackMateModelChangeListener() {
			@Override
			public void modelChanged(TrackMateModelChangeEvent event) {
				analyzer.modelChanged(event);
			}
		};
		model.addTrackMateModelChangeListener(listener);
		
		// Add a new track to the model - the old tracks should not be affected
		model.beginUpdate();
		try {
			Spot spot1 = model.addSpotTo(new SpotImp(new double[3]), 0);
			Spot spot2 = model.addSpotTo(new SpotImp(new double[3]), 1);
			model.addEdge(spot1, spot2, 1);
			
		} finally {
			model.endUpdate();
		}
		
		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);
		
		// Check the track IDs the analyzer received - none of the old keys must be in it
		for (Integer calledKey : analyzer.keys) {
			if (oldKeys.contains(calledKey)) {
				fail("Track with ID " + calledKey + " should not have been re-analyzed.");
			}
		}
		
		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;
		
		// New change: graft a new spot on the first track - it should be re-analyzed
		Integer firstKey = oldKeys.iterator().next();
		Spot firstSpot = model.getTrackModel().getTrackSpots(firstKey).iterator().next();
		Spot newSpot = null;
		int firstFrame = firstSpot.getFeature(Spot.FRAME).intValue();
		model.beginUpdate();
		try {
			newSpot = model.addSpotTo(new SpotImp(new double[3]), firstFrame + 1);
			model.addEdge(firstSpot, newSpot, 1);
		} finally {
			model.endUpdate();
		}
		
		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);
		
		// Check the track IDs: must be of size 1, and tkey to the track with firstSpot and newSpot in it
		assertEquals(1, analyzer.keys.size());
		assertTrue(model.getTrackModel().getTrackSpots(analyzer.keys.iterator().next()).contains(firstSpot));
		assertTrue(model.getTrackModel().getTrackSpots(analyzer.keys.iterator().next()).contains(newSpot));
		
		
	}
	

	
	/**
	 *  Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackBranchingAnalyzer extends TrackBranchingAnalyzer {
		
		private boolean hasBeenCalled = false;
		private Collection<Integer> keys;
		
		public TestTrackBranchingAnalyzer(TrackMateModel model) {
			super(model);
		}
		
		@Override
		public void process(Collection<Integer> trackIDs) {
			hasBeenCalled = true;
			keys = trackIDs;
			super.process(trackIDs);
		}
	}
	

}
