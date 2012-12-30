package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateModelChangeListener;

public class TrackDurationAnalyzerTest {

	private static final int N_TRACKS = 10;
	private static final int DEPTH = 9;
	private TrackMateModel model;
	private HashMap<Integer, Double> expectedDuration;
	private HashMap<Integer, Double> expectedStart;
	private HashMap<Integer, Double> expectedStop;
	private HashMap<Integer, Double> expectedDisplacement;

	@Before
	public void setUp() {
		Random ran = new Random();
		model = new TrackMateModel();
		model.beginUpdate();
		try {
			
			expectedDuration 	= new HashMap<Integer, Double>(N_TRACKS); 
			expectedStart 		= new HashMap<Integer, Double>(N_TRACKS); 
			expectedStop 		= new HashMap<Integer, Double>(N_TRACKS); 
			expectedDisplacement = new HashMap<Integer, Double>(N_TRACKS); 

			for (int i = 0; i < N_TRACKS; i++) {
				
				Spot previous = null;
				
				int start = ran.nextInt(DEPTH);
				int stop = DEPTH + ran.nextInt(DEPTH);
				int duration = stop - start;
				double displacement = ran.nextDouble();
				
				HashSet<Spot> track = new HashSet<Spot>();
				for (int j = start; j <= stop ; j++) {
					Spot spot = new SpotImp(new double[3]);
					spot.putFeature(Spot.POSITION_T, Double.valueOf(j));
					model.addSpotTo(spot, j);
					track.add(spot);
					if (null != previous) {
						model.getTrackModel().addEdge(previous, spot, 1);
					}
					previous = spot;
				}
				previous.putFeature(Spot.POSITION_X, displacement);
				
				int key = track.hashCode(); // a hack: the track ID will be the hash of the spot set 
				expectedDuration.put(key, Double.valueOf(duration));
				expectedStart.put(key, Double.valueOf(start));
				expectedStop.put(key, Double.valueOf(stop));
				expectedDisplacement.put(key, displacement);
			}
			
		} finally {
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess() {
		// Process model
		TrackDurationAnalyzer analyzer = new TrackDurationAnalyzer(model);
		analyzer.process(model.getTrackModel().getFilteredTrackIDs());

		// Collect features
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
			
			assertEquals(expectedDisplacement.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackDurationAnalyzer.TRACK_DISPLACEMENT));
			assertEquals(expectedStart.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackDurationAnalyzer.TRACK_START));
			assertEquals(expectedStop.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackDurationAnalyzer.TRACK_STOP));
			assertEquals(expectedDuration.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackDurationAnalyzer.TRACK_DURATION));
			
		}
	}

	@Test
	public final void testModelChanged() {
		// Copy old keys
		HashSet<Integer> oldKeys = new HashSet<Integer>(model.getTrackModel().getFilteredTrackIDs());
		
		// First analysis
		final TestTrackDurationAnalyzer analyzer = new TestTrackDurationAnalyzer(model);
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
			spot1.putFeature(Spot.POSITION_T, 0);
			Spot spot2 = model.addSpotTo(new SpotImp(new double[3]), 1);
			spot2.putFeature(Spot.POSITION_T, 1);
			model.getTrackModel().addEdge(spot1, spot2, 1);
			
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
		TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.frameComparator);
		sortedTrack.addAll( model.getTrackModel().getTrackSpots(firstKey));
		Spot firstSpot = sortedTrack.first();
		Spot newSpot = null;
		int firstFrame = firstSpot.getFeature(Spot.FRAME).intValue();
		model.beginUpdate();
		try {
			newSpot = model.addSpotTo(new SpotImp(new double[3]), firstFrame + 1);
			newSpot.putFeature(Spot.POSITION_T, firstFrame + 1);
			model.getTrackModel().addEdge(firstSpot, newSpot, 1);
		} finally {
			model.endUpdate();
		}
		
		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);
		
		// Check the track IDs: must be of size 1, and they to the track with firstSpot and newSpot in it
		assertEquals(1, analyzer.keys.size());
		assertTrue(model.getTrackModel().getTrackSpots(analyzer.keys.iterator().next()).contains(firstSpot));
		assertTrue(model.getTrackModel().getTrackSpots(analyzer.keys.iterator().next()).contains(newSpot));
		
		// But the track features for this track should not have changed: the grafting did not affect
		// start nor stop nor displacement
		assertEquals(expectedDisplacement.get(firstKey), model.getFeatureModel().getTrackFeature(firstKey, TrackDurationAnalyzer.TRACK_DISPLACEMENT));
		assertEquals(expectedStart.get(firstKey), model.getFeatureModel().getTrackFeature(firstKey, TrackDurationAnalyzer.TRACK_START));
		assertEquals(expectedStop.get(firstKey), model.getFeatureModel().getTrackFeature(firstKey, TrackDurationAnalyzer.TRACK_STOP));
		assertEquals(expectedDuration.get(firstKey), model.getFeatureModel().getTrackFeature(firstKey, TrackDurationAnalyzer.TRACK_DURATION));
		
		
	}
	

	
	/**
	 *  Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackDurationAnalyzer extends TrackDurationAnalyzer {
		
		private boolean hasBeenCalled = false;
		private Collection<Integer> keys;
		
		public TestTrackDurationAnalyzer(TrackMateModel model) {
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
