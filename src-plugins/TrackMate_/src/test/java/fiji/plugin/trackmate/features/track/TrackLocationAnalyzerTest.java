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

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;

public class TrackLocationAnalyzerTest {

	private static final int N_TRACKS = 10;
	private static final int DEPTH = 9;
	private Model model;
	private HashMap<Integer, Double> expectedX;
	private HashMap<Integer, Double> expectedY;
	private HashMap<Integer, Double> expectedZ;

	@Before
	public void setUp() {
		model = new Model();
		model.beginUpdate();
		try {
			
			expectedX 		= new HashMap<Integer, Double>(N_TRACKS); 
			expectedY 		= new HashMap<Integer, Double>(N_TRACKS); 
			expectedZ 		= new HashMap<Integer, Double>(N_TRACKS); 

			for (int i = 0; i < N_TRACKS; i++) {
				
				Spot previous = null;
				
				HashSet<Spot> track = new HashSet<Spot>();
				for (int j = 0; j <= DEPTH; j++) {
					// We use deterministic locations
					double[] location = new double[] { j + i, j + i, j + i }; 
					Spot spot = new Spot(location);
					model.addSpotTo(spot, j);
					track.add(spot);
					if (null != previous) {
						model.addEdge(previous, spot, 1);
					}
					previous = spot;
				}
				
				
				int key = model.getTrackModel().trackIDOf(previous);
				double mean = (double) DEPTH / 2 + (double) i;  
				expectedX.put(key, Double.valueOf(mean));
				expectedY.put(key, Double.valueOf(mean));
				expectedZ.put(key, Double.valueOf(mean));
			}
			
		} finally {
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess() {
		// Process model
		TrackLocationAnalyzer analyzer = new TrackLocationAnalyzer(model);
		analyzer.process(model.getTrackModel().trackIDs(true));

		// Collect features
		for (Integer trackID : model.getTrackModel().trackIDs(true)) {
			
			assertEquals(expectedX.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackLocationAnalyzer.X_LOCATION));
			assertEquals(expectedY.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackLocationAnalyzer.Y_LOCATION));
			assertEquals(expectedZ.get(trackID), model.getFeatureModel().getTrackFeature(trackID, TrackLocationAnalyzer.Z_LOCATION));
			
		}
	}

	@Test
	public final void testModelChanged() {
		// Copy old keys
		HashSet<Integer> oldKeys = new HashSet<Integer>(model.getTrackModel().trackIDs(true));
		
		// First analysis
		final TestTrackLocationAnalyzer analyzer = new TestTrackLocationAnalyzer(model);
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
		model.addModelChangeListener(listener);
		
		// Add a new track to the model - the old tracks should not be affected
		model.beginUpdate();
		try {
			Spot spot1 = model.addSpotTo(new Spot(new double[3]), 0);
			spot1.putFeature(Spot.POSITION_T, 0d);
			Spot spot2 = model.addSpotTo(new Spot(new double[3]), 1);
			spot2.putFeature(Spot.POSITION_T, 1d);
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
		
		// New change: remove the first spot on the first track - it should be re-analyzed
		Integer firstKey = oldKeys.iterator().next();
		TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.frameComparator);
		sortedTrack.addAll( model.getTrackModel().trackSpots(firstKey));
		Iterator<Spot> it = sortedTrack.iterator();
		Spot firstSpot = it.next();
		Spot secondSpot = it.next();
		
		model.beginUpdate();
		try {
			model.removeSpot(firstSpot);
		} finally {
			model.endUpdate();
		}
		
		// The analyzer must have done something:
		assertTrue(analyzer.hasBeenCalled);
		
		// Check the track IDs: must be of size 1 since we removed the first spot of a track
		assertEquals(1, analyzer.keys.size());
		Integer newKey = analyzer.keys.iterator().next();
		assertEquals(model.getTrackModel().trackIDOf(secondSpot).longValue(), newKey.longValue());
		
		// The location k features for this track must have changed by 0.5 with respect to previous calculation
		assertEquals(expectedX.get(firstKey)+0.5d, model.getFeatureModel().getTrackFeature(newKey, TrackLocationAnalyzer.X_LOCATION).doubleValue(), Double.MIN_VALUE);
		assertEquals(expectedY.get(firstKey)+0.5d, model.getFeatureModel().getTrackFeature(newKey, TrackLocationAnalyzer.Y_LOCATION).doubleValue(), Double.MIN_VALUE);
		assertEquals(expectedZ.get(firstKey)+0.5d, model.getFeatureModel().getTrackFeature(newKey, TrackLocationAnalyzer.Z_LOCATION).doubleValue(), Double.MIN_VALUE);
	}
	
	/**
	 *  Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackLocationAnalyzer extends TrackLocationAnalyzer {
		
		private boolean hasBeenCalled = false;
		private Collection<Integer> keys;
		
		public TestTrackLocationAnalyzer(Model model) {
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
