package fiji.plugin.trackmate.tracking;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.junit.Test;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;


public class LAPTrackerTest {

	/**
	 * Standard tracking
	 */
	@Test
	public void  testTracking() {

		final int nFrames = 100;

		// Create 2 "lines" of spots, keeping track of the manual tracks for later testing
		List<Spot> group1 = new ArrayList<Spot>(nFrames);
		List<Spot> group2 = new ArrayList<Spot>(nFrames);
		SpotCollection spotCollection = new SpotCollection();
		for (int i = 0; i < nFrames; i++) {
			double[] coords1 = new double[] { 1d, 1d * i, 0 } ;
			double[] coords2 = new double[] { 2d, 1d * i, 0 } ;

			Spot spot1 = new Spot(coords1);
			Spot spot2 = new Spot(coords2);
			spot1.putFeature(Spot.POSITION_T, i);
			spot2.putFeature(Spot.POSITION_T, i);
			spot1.setName("G1T"+i);
			spot2.setName("G2T"+i);

			group1.add(spot1);
			group2.add(spot2);

			List<Spot> spots = new ArrayList<Spot>(2);
			spots.add(spot1);
			spots.add(spot2);
			spotCollection.put(i, spots);
		}
		List<List<Spot>> groups = new ArrayList<List<Spot>>(2);
		groups.add(group1);
		groups.add(group2);

		// Set the tracking settings
		Map<String, Object> trackerSettings = LAPUtils.getDefaultLAPSettingsMap();
		trackerSettings.put(KEY_LINKING_MAX_DISTANCE, 2d);
		trackerSettings.put(KEY_ALLOW_GAP_CLOSING, false);

		// Instantiate tracker
		LAPTracker tracker = new LAPTracker(spotCollection, Logger.VOID_LOGGER);
		tracker.setSettings(trackerSettings);

		// Check process
		if (!tracker.checkInput() || !tracker.process()) {
			fail(tracker.getErrorMessage());
		}
		
		// Check results
		SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = tracker.getResult();
		verifyTracks(graph, groups, nFrames);
	}


	/**
	 * This time we try to track spots with different intensities and see if we can put 
	 * them back right
	 */
	@Test
	public void  testTrackingWithFeature() {

		final int nFrames = 100;

		// Create 2 "lines" of spots, keeping track of the manual tracks for later testing
		List<Spot> group1 = new ArrayList<Spot>(nFrames);
		List<Spot> group2 = new ArrayList<Spot>(nFrames);
		SpotCollection spotCollection = new SpotCollection();
		for (int i = 0; i < nFrames; i++) {
			double[] coords1 = new double[] {  (i % 2), 1d * i, 0 } ;
			double[] coords2 = new double[] { ( (i+1) % 2), 1d * i, 0 } ;

			Spot spot1 = new Spot(coords1);
			Spot spot2 = new Spot(coords2);
			spot1.putFeature(Spot.POSITION_T, i);
			spot2.putFeature(Spot.POSITION_T, i);
			spot1.setName("G1T"+i);
			spot2.setName("G2T"+i);
			// For this test, we need to put a different feature for each track
			spot1.putFeature(SpotIntensityAnalyzerFactory.MEAN_INTENSITY, 100);
			spot2.putFeature(SpotIntensityAnalyzerFactory.MEAN_INTENSITY, 200);

			group1.add(spot1);
			group2.add(spot2);

			List<Spot> spots = new ArrayList<Spot>(2);
			spots.add(spot1);
			spots.add(spot2);
			spotCollection.put(i, spots);
		}
		List<List<Spot>> groups = new ArrayList<List<Spot>>(2);
		groups.add(group1);
		groups.add(group2);

		// Set the tracking settings
		Map<String, Object> trackerSettings = LAPUtils.getDefaultLAPSettingsMap();
		trackerSettings.put(KEY_LINKING_MAX_DISTANCE, 2.);
		trackerSettings.put(KEY_ALLOW_GAP_CLOSING, false);
		StringBuilder errorHolder = new StringBuilder();
		boolean ok = LAPUtils.addFeaturePenaltyToSettings(trackerSettings, KEY_LINKING_FEATURE_PENALTIES, SpotIntensityAnalyzerFactory.MEAN_INTENSITY, 1d, errorHolder );
		if (!ok) {
			fail(errorHolder.toString());
		}

		// Instantiate tracker
		LAPTracker tracker = new LAPTracker(spotCollection, Logger.VOID_LOGGER);
		tracker.setSettings(trackerSettings);

		// Check process
		if (!tracker.checkInput() || !tracker.process()) {
			fail(tracker.getErrorMessage());
		}
		
		// Check results
		SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = tracker.getResult();
		verifyTracks(graph, groups, nFrames);
	}



	private static void verifyTracks(final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph, List<List<Spot>> groups, int nFrames) {

		// Check that we have the right number of vertices
		assertEquals("The tracking result graph has the wrong number of vertices, ", 
				2 * nFrames,
				graph.vertexSet().size());

		// Check that we have the right number of tracks
		ConnectivityInspector<Spot, DefaultWeightedEdge> inspector = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph);
		int nTracks = inspector.connectedSets().size();
		assertEquals("Did not get the right number of tracks, ", 2, nTracks);

		// Check that the tracks are right: the group1 must contain exactly the spot of one track
		for(List<Spot> group : groups) {
			//			System.out.print("\nTrack: ");
			Set<Spot> track1 = inspector.connectedSetOf(group.get(0));
			for(Spot spot : group) {
				boolean removed = track1.remove(spot);
				assertTrue("Failed to find spot "+spot+" in track.", removed);
				//				System.out.print(spot+"-");
			}
			assertEquals("Track has some unexpected spots", 0, track1.size());
		}

	}
}
