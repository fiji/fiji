package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackSpeedStatisticsAnalyzerTest {

	private static final int N_TRACKS = 10;
	private static final int DEPTH = 9;
	private TrackMateModel model;
	private HashMap<Integer, Double> expectedVmean;
	private HashMap<Integer, Double> expectedVmax;

	@Before
	public void setUp() {
		model = new TrackMateModel();
		model.beginUpdate();
		try {

			expectedVmean 		= new HashMap<Integer, Double>(N_TRACKS); 
			expectedVmax 		= new HashMap<Integer, Double>(N_TRACKS); 

			// Linear movement
			for (int i = 1; i < N_TRACKS+1; i++) {

				Spot previous = null;

				HashSet<Spot> track = new HashSet<Spot>();
				for (int j = 0; j <= DEPTH; j++) {
					// We use deterministic locations
					double[] location = new double[] { j * i, i, i }; 
					Spot spot = new Spot(location);
					spot.putFeature(Spot.POSITION_T, j);
					model.addSpotTo(spot, j);
					track.add(spot);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					}
					previous = spot;
				}

				int key = track.hashCode(); // a hack: the track ID will be the hash of the spot set
				double speed = i; 
				expectedVmean.put(key, Double.valueOf(speed));
				expectedVmax.put(key, Double.valueOf(speed));
			}

		} finally {
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess() {
		// Process model
		TrackSpeedStatisticsAnalyzer analyzer = new TrackSpeedStatisticsAnalyzer(model);
		analyzer.process(model.getTrackModel().getFilteredTrackIDs());

		// Collect features
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {

			assertEquals(expectedVmax.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackSpeedStatisticsAnalyzer.TRACK_MAX_SPEED));
			assertEquals(expectedVmean.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED));

		}
	}

	@Test
	public final void testProcess2() {
		// Build parabolic model
		TrackMateModel model2 = new TrackMateModel();
		model2.beginUpdate();
		try {

			// Parabolic movement
			Spot previous = null;
			HashSet<Spot> track = new HashSet<Spot>();
			for (int j = 0; j <= DEPTH; j++) {
				// We use deterministic locations
				double[] location = new double[] { j * j, 0, 0 }; 
				Spot spot = new Spot(location);
				spot.putFeature(Spot.POSITION_T, j);
				model2.addSpotTo(spot, j);
				track.add(spot);
				if (null != previous) {
					model2.addEdge(previous, spot, 1);
				}
				previous = spot;
			}

		} finally {
			model2.endUpdate();
		}
		
		// Expected values
		double meanV = 9;
		double stdV = 5.477225575051661;
		double minV = 1;
		double maxV = 17;
		double medianV = 9;

		// Process model
		TrackSpeedStatisticsAnalyzer analyzer = new TrackSpeedStatisticsAnalyzer(model2);
		analyzer.process(model2.getTrackModel().getFilteredTrackIDs());

		// Collect features
		for (Integer trackID : model2.getTrackModel().getFilteredTrackIDs()) {

			assertEquals(meanV, model2.getFeatureModel().getTrackFeature(trackID, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED), Double.MIN_VALUE);
			assertEquals(stdV, model2.getFeatureModel().getTrackFeature(trackID, TrackSpeedStatisticsAnalyzer.TRACK_STD_SPEED), 1e-6);
			assertEquals(minV, model2.getFeatureModel().getTrackFeature(trackID, TrackSpeedStatisticsAnalyzer.TRACK_MIN_SPEED), Double.MIN_VALUE);
			assertEquals(maxV, model2.getFeatureModel().getTrackFeature(trackID, TrackSpeedStatisticsAnalyzer.TRACK_MAX_SPEED), Double.MIN_VALUE);
			assertEquals(medianV, model2.getFeatureModel().getTrackFeature(trackID, TrackSpeedStatisticsAnalyzer.TRACK_MEDIAN_SPEED), Double.MIN_VALUE);

		}
	}

	@Test
	public final void testModelChanged() {
		// Copy old keys
		HashSet<Integer> oldKeys = new HashSet<Integer>(model.getTrackModel().getFilteredTrackIDs());

		// First analysis
		final TestTrackSpeedStatisticsAnalyzer analyzer = new TestTrackSpeedStatisticsAnalyzer(model);
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
			spot1.putFeature(Spot.POSITION_T, 0);
			Spot spot2 = model.addSpotTo(new Spot(new double[3]), 1);
			spot2.putFeature(Spot.POSITION_T, 1);
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
	}

	@Test
	public final void testModelChanged2() {
		// Copy old keys
		HashSet<Integer> oldKeys = new HashSet<Integer>(model.getTrackModel().getFilteredTrackIDs());

		// First analysis
		final TestTrackSpeedStatisticsAnalyzer analyzer = new TestTrackSpeedStatisticsAnalyzer(model);
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

		// New change: remove the first spot on the first track - the new track emerging should be re-analyzed
		Integer firstKey = oldKeys.iterator().next();
		TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.frameComparator);
		sortedTrack.addAll( model.getTrackModel().getTrackSpots(firstKey));
		Iterator<Spot> it = sortedTrack.iterator();
		Spot firstSpot = it.next();
		Spot secondSpot = it.next();

		model.beginUpdate();
		try {
			model.removeSpotFrom(firstSpot, firstSpot.getFeature(Spot.FRAME).intValue());
		} finally {
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);

		// Check the track IDs: must be of size 1 since we removed the first spot of a track
		assertEquals(1, analyzer.keys.size());
		Integer newKey = analyzer.keys.iterator().next();
		assertEquals(model.getTrackModel().getTrackIDOf(secondSpot).longValue(), newKey.longValue());

		// That did not affect speed values )was a constant speed track)
		assertEquals(expectedVmean.get(firstKey).doubleValue(), model.getFeatureModel().getTrackFeature(newKey, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED).doubleValue(), Double.MIN_VALUE);
		assertEquals(expectedVmax.get(firstKey).doubleValue(), model.getFeatureModel().getTrackFeature(newKey, TrackSpeedStatisticsAnalyzer.TRACK_MAX_SPEED).doubleValue(), Double.MIN_VALUE);
	}

	@Test
	public final void testModelChanged3() {
		// Copy old keys
		HashSet<Integer> oldKeys = new HashSet<Integer>(model.getTrackModel().getFilteredTrackIDs());

		// First analysis
		final TestTrackSpeedStatisticsAnalyzer analyzer = new TestTrackSpeedStatisticsAnalyzer(model);
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

		// New change: we displace the last spot of first track, making the edge faster
		Integer firstKey = oldKeys.iterator().next();
		TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.frameComparator);
		sortedTrack.addAll( model.getTrackModel().getTrackSpots(firstKey));
		Iterator<Spot> it = sortedTrack.descendingIterator();
		Spot lastSpot = it.next();
		Spot penultimateSpot = it.next();

		model.beginUpdate();
		try {
			lastSpot.putFeature(Spot.POSITION_X, 2 * lastSpot.getFeature(Spot.POSITION_X));
			model.updateFeatures(lastSpot);
		} finally {
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);

		// Check the track IDs: must be of size 1 since we removed the first spot of a track
		assertEquals(1, analyzer.keys.size());
		Integer newKey = analyzer.keys.iterator().next();
		assertEquals(model.getTrackModel().getTrackIDOf(lastSpot).longValue(), newKey.longValue());

		// Track must be faster now
		assertTrue(expectedVmean.get(firstKey).doubleValue() < model.getFeatureModel().getTrackFeature(newKey, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED).doubleValue());
		// max speed is the one on this edge
		double maxSpeed = lastSpot.getFeature(Spot.POSITION_X).doubleValue() - penultimateSpot.getFeature(Spot.POSITION_X).doubleValue(); 
		assertEquals(maxSpeed, model.getFeatureModel().getTrackFeature(newKey, TrackSpeedStatisticsAnalyzer.TRACK_MAX_SPEED).doubleValue(), Double.MIN_VALUE);
	}

	/**
	 *  Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackSpeedStatisticsAnalyzer extends TrackSpeedStatisticsAnalyzer {

		private boolean hasBeenCalled = false;
		private Collection<Integer> keys;

		public TestTrackSpeedStatisticsAnalyzer(TrackMateModel model) {
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
