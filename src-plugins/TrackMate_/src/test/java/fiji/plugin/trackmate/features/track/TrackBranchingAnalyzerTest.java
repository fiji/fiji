package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackBranchingAnalyzerTest {

	private static final int N_LINEAR_TRACKS = 5;
	private static final int N_TRACKS_WITH_GAPS = 7;
	private static final int N_TRACKS_WITH_SPLITS = 9;
	private static final int N_TRACKS_WITH_MERGES = 11;
	private static final int DEPTH = 9;
	private TrackMateModel model;
	private Spot split;
	private Spot lastSpot1;
	private Spot lastSpot2;
	private Spot firstSpot;

	@Before
	public void setUp() {
		model = new TrackMateModel();
		model.beginUpdate();
		try {
			// linear tracks
			for (int i = 0; i < N_LINEAR_TRACKS; i++) {
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
			// tracks with gaps
			for (int i = 0; i < N_TRACKS_WITH_GAPS; i++) {
				Spot previous = null;
				for (int j = 0; j < DEPTH; j++) {
					if (j == DEPTH/2) {
						continue;
					}
					Spot spot = new Spot(new double[3]);
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
				split = null; // Store the spot at the branch split 
				for (int j = 0; j < DEPTH; j++) {
					Spot spot = new Spot(new double[3]);
					if (j == DEPTH/2) {
						split = spot;
					}
					model.addSpotTo(spot, j);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					} else {
						firstSpot = spot; // Store first spot of track
					}
					previous = spot;
				}
				lastSpot1 = previous; // Store last spot of 1st branch
				previous = split;
				for (int j = DEPTH/2+1; j < DEPTH; j++) {
					Spot spot = new Spot(new double[3]);
					model.addSpotTo(spot, j);
					model.addEdge(previous, spot, 1);
					previous = spot;
				}
				lastSpot2 = previous; // Store last spot of 2nd branch
			}
			// tracks with merges
			for (int i = 0; i < N_TRACKS_WITH_MERGES; i++) {
				Spot previous = null;
				Spot merge = null;
				for (int j = 0; j < DEPTH; j++) {
					Spot spot = new Spot(new double[3]);
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
					Spot spot = new Spot(new double[3]);
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
		ModelChangeListener listener = new ModelChangeListener() {
			@Override
			public void modelChanged(ModelChangeEvent event) {
				analyzer.process(event.getTrackUpdated());
			}
		};
		model.addTrackMateModelChangeListener(listener);

		// Add a new track to the model - the old tracks should not be affected
		model.beginUpdate();
		try {
			Spot spot1 = model.addSpotTo(new Spot(new double[3]), 0);
			Spot spot2 = model.addSpotTo(new Spot(new double[3]), 1);
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
			newSpot = model.addSpotTo(new Spot(new double[3]), firstFrame + 1);
			model.addEdge(firstSpot, newSpot, 1);
		} finally {
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);

		// Check the track IDs: must be of size 1, and key to the track with firstSpot and newSpot in it
		assertEquals(1, analyzer.keys.size());
		assertTrue(model.getTrackModel().getTrackSpots(analyzer.keys.iterator().next()).contains(firstSpot));
		assertTrue(model.getTrackModel().getTrackSpots(analyzer.keys.iterator().next()).contains(newSpot));


	}

	@Test
	public final void testModelChanged2() { 

		// Copy old keys
		HashSet<Integer> oldKeys = new HashSet<Integer>(model.getTrackModel().getFilteredTrackIDs());

		// First analysis
		final TestTrackBranchingAnalyzer analyzer = new TestTrackBranchingAnalyzer(model);
		analyzer.process(oldKeys);

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// Prepare listener for model change
		ModelChangeListener listener = new ModelChangeListener() {
			@Override
			public void modelChanged(ModelChangeEvent event) {
				analyzer.process(event.getTrackUpdated());
			}
		};
		model.addTrackMateModelChangeListener(listener);
		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		/*
		 * A nasty change: we move a spot from its frame to the first frame, for a track with a split:
		 * it should turn it in a track with a merge. 
		 */

		// Find a track with a split
		Integer splittingTrackID = null;
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
			if (model.getFeatureModel().getTrackFeature(trackID, TrackBranchingAnalyzer.NUMBER_SPLITS) > 0) {
				splittingTrackID = trackID;
				break;
			}
		}

		// Get the last spot in time
		TreeSet<Spot> track = new TreeSet<Spot>(Spot.frameComparator);
		track.addAll(model.getTrackModel().getTrackSpots(splittingTrackID));
		Spot lastSpot = track.last();

		// Move the spot to the first frame. We do it with beginUpdate() / endUpdate() 
		model.beginUpdate();
		try {
			model.moveSpotFrom(lastSpot, lastSpot.getFeature(Spot.FRAME).intValue(), 0);
		} finally {
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);

		// Check the track IDs: must be of size 1, and must be the track split
		assertEquals(1, analyzer.keys.size());
		assertEquals(splittingTrackID.longValue(), analyzer.keys.iterator().next().longValue());

		// Check that the features have been well calculated: it must now be a merging track
		assertEquals(1, model.getFeatureModel().getTrackFeature(splittingTrackID, TrackBranchingAnalyzer.NUMBER_SPLITS).intValue());
		assertEquals(1, model.getFeatureModel().getTrackFeature(splittingTrackID, TrackBranchingAnalyzer.NUMBER_MERGES).intValue());
	}

	@Test
	public final void testModelChanged3() { 

		// Copy old keys
		HashSet<Integer> oldKeys = new HashSet<Integer>(model.getTrackModel().getFilteredTrackIDs());

		// First analysis
		final TestTrackBranchingAnalyzer analyzer = new TestTrackBranchingAnalyzer(model);
		analyzer.process(oldKeys);

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// Prepare listener for model change
		ModelChangeListener listener = new ModelChangeListener() {
			@Override
			public void modelChanged(ModelChangeEvent event) {
				analyzer.process(event.getTrackUpdated());
			}
		};
		model.addTrackMateModelChangeListener(listener);
		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		/*
		 * A nasty change: we remove a spot from the frame where there is a split.
		 * Only the three generated tracks should get analyzed. 
		 */

		// Remove the branching spot 
		model.beginUpdate();
		try {
			model.removeSpotFrom(split, split.getFeature(Spot.FRAME).intValue());
		} finally {
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);

		// Check the track IDs: must be of size 3: the 3 tracks split
		assertEquals(3, analyzer.keys.size());
		for (Integer targetKey : analyzer.keys) {
			assertTrue(
					targetKey.equals(model.getTrackModel().getTrackIDOf(firstSpot))
					|| targetKey.equals(model.getTrackModel().getTrackIDOf(lastSpot1))
					|| targetKey.equals(model.getTrackModel().getTrackIDOf(lastSpot2))
					);
		}

		// Check that the features have been well calculated: we must have 3 linear tracks
		for (Integer targetKey : analyzer.keys) {
			assertEquals(0, model.getFeatureModel().getTrackFeature(targetKey, TrackBranchingAnalyzer.NUMBER_SPLITS).intValue());
			assertEquals(0, model.getFeatureModel().getTrackFeature(targetKey, TrackBranchingAnalyzer.NUMBER_MERGES).intValue());
			assertEquals(0, model.getFeatureModel().getTrackFeature(targetKey, TrackBranchingAnalyzer.NUMBER_COMPLEX).intValue());
		}
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
