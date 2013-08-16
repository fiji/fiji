package fiji.plugin.trackmate;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;

public class TrackModelTest {

	private static final int N_TRACKS = 3;
	private static final int DEPTH = 5;

	@Test
	public void testBuildingTracks() {
		TrackModel model = new TrackModel();
		for (int i = 0; i < N_TRACKS; i++) {
			Spot previous = null;
			for (int j = 0; j < DEPTH; j++) {
				Spot spot = new Spot(new double[3]);
				model.addSpot(spot);
				if (null != previous) {
					model.addEdge(previous, spot, 1);
				}
				previous = spot;
			}
		}
		
		// The must be N_TRACKS visible tracks in total
		assertEquals(N_TRACKS, model.nTracks(false));
		assertEquals(N_TRACKS, model.nTracks(true));
		// They must be made of DEPTH spots and DEPTH-1 edges
		for (Integer id : model.trackIDs(true)) {
			assertEquals(DEPTH, model.trackSpots(id).size());
			assertEquals(DEPTH-1, model.trackEdges(id).size());
		}
	}
	
	@Test
	public void testConnectingTracks() {
		// Build segments
		TrackModel model = new TrackModel();
		List<Spot> trackEnds = new ArrayList<Spot>();
		List<Spot> trackStarts = new ArrayList<Spot>();
		for (int i = 0; i < N_TRACKS; i++) {
			Spot previous = null;
			Spot spot = null;
			for (int j = 0; j < DEPTH; j++) {
				spot = new Spot(new double[3]);
				model.addSpot(spot);
				if (null != previous) {
					model.addEdge(previous, spot, 1);
				} else {
					trackStarts.add(spot);
				}
				previous = spot;
			}
			trackEnds.add(spot);
		}
		// Connect segments
		for (int i = 0; i < trackStarts.size()-1; i++) {
			Spot end = trackEnds.get(i);
			Spot start = trackStarts.get(i+1);
			model.addEdge(end, start, 2);
		}
		
		// There must be one visible track in total
		assertEquals(1, model.nTracks(false));
		assertEquals(1, model.nTracks(true));
		// It must be long
		Integer id = model.trackIDs(true).iterator().next();
		assertEquals(N_TRACKS * DEPTH, model.trackSpots(id).size());
		assertEquals(N_TRACKS * DEPTH - 1, model.trackEdges(id).size());
	}
	
	@Test
	public void testBreakingTracksBySpots() {
		// Build 1 long track
		TrackModel model = new TrackModel();
		List<Spot> trackBreaks = new ArrayList<Spot>();
		Spot previous = null;
		for (int i = 0; i < N_TRACKS; i++) {
			for (int j = 0; j < DEPTH; j++) {
				Spot spot = new Spot(new double[3]);
				model.addSpot(spot);
				if (null != previous) {
					model.addEdge(previous, spot, 1);
				}
				previous = spot;
			}
			trackBreaks.add(previous);
		}
		// Break it
		for (Spot spot : trackBreaks) {
			model.removeSpot(spot);
		}

		// There must be N_TRACKS visible tracks in total
		assertEquals(N_TRACKS, model.nTracks(false));
		assertEquals(N_TRACKS, model.nTracks(true));
		// They must be DEPTH-1 long in spots
		for (Integer id : model.trackIDs(false)) {
			assertEquals(DEPTH-1, model.trackSpots(id).size());
			assertEquals(DEPTH-2, model.trackEdges(id).size());
		}
	}
	
	@Test
	public void testBreakingTracksByEdges() {
		// Build 1 long track
		TrackModel model = new TrackModel();
		List<DefaultWeightedEdge> trackBreaks = new ArrayList<DefaultWeightedEdge>();
		Spot previous = new Spot(new double[3]);
		model.addSpot(previous);
		for (int i = 0; i < N_TRACKS; i++) {
			DefaultWeightedEdge edge = null;
			for (int j = 0; j < DEPTH; j++) {
				Spot spot = new Spot(new double[3]);
				model.addSpot(spot);
				edge = model.addEdge(previous, spot, 1);
				previous = spot;
			}
			trackBreaks.add(edge);
		}
		// Break it
		for (DefaultWeightedEdge edge : trackBreaks) {
			model.removeEdge(edge);
		}

		// There must be N_TRACKS visible tracks in total
		assertEquals(N_TRACKS, model.nTracks(false));
		assertEquals(N_TRACKS, model.nTracks(true));
		// They must be DEPTH long in spots
		for (Integer id : model.trackIDs(false)) {
			assertEquals(DEPTH, model.trackSpots(id).size());
			assertEquals(DEPTH-1, model.trackEdges(id).size());
		}
	}
	
	@Test
	public void testVisibility() {
		TrackModel model = new TrackModel();
		for (int i = 0; i < N_TRACKS; i++) {
			Spot previous = null;
			for (int j = 0; j < DEPTH; j++) {
				Spot spot = new Spot(new double[3]);
				model.addSpot(spot);
				if (null != previous) {
					model.addEdge(previous, spot, 1);
				}
				previous = spot;
			}
		}
		// Make some of them invisible
		Set<Integer> toHide = new HashSet<Integer>(N_TRACKS);
		for (Integer id : model.trackIDs(true)) {
			if (new Random().nextBoolean()) {
				toHide.add(id);
			}
		}
		for (Integer id : toHide) {
			model.setVisibility(id, false);
		}
		
		// Test if visibility is reported correctly
		assertEquals(N_TRACKS - toHide.size(), model.nTracks(true));
		for (Integer id : model.trackIDs(false)) {
			if (toHide.contains(id)) {
				assertEquals(false, model.isVisible(id));
			} else {
				assertEquals(true, model.isVisible(id));
			}
		}
	}
	
	@Test
	public void testVisibilityMerge() {
		TrackModel model = new TrackModel();
		for (int i = 0; i < 2; i++) {
			Spot previous = null;
			for (int j = 0; j < DEPTH; j++) {
				Spot spot = new Spot(new double[3]);
				model.addSpot(spot);
				if (null != previous) {
					model.addEdge(previous, spot, 1);
				}
				previous = spot;
			}
		}
		// Make one of them invisible
		Integer toHide = model.trackIDs(true).iterator().next();
		model.setVisibility(toHide, false);
		
		// Get the id of the other one
		Set<Integer> ids = new HashSet<Integer>(model.trackIDs(false));
		ids.remove(toHide);
		Integer shown = ids.iterator().next();
		
		// Connect the two
		Spot source = model.trackSpots(shown).iterator().next();
		Spot target = model.trackSpots(toHide).iterator().next();
		model.addEdge(source, target, 1);
		
		// Test if visibility is reported correctly
		assertEquals(1, model.nTracks(false));
		Integer id = model.trackIDs(false).iterator().next();
		assertTrue(model.isVisible(id));
	}

}
