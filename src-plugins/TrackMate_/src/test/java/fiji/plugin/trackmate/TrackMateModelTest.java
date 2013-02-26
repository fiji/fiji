package fiji.plugin.trackmate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;

public class TrackMateModelTest {


	/**
	 * Test if the track visibility is followed correctly.
	 */
	@Test 
	public void testTrackVisibility() {
		TrackMateModel model = new TrackMateModel();
		// Build track 1 with 5 spots
		final Spot s1 = new Spot(new double[3], "S1");
		final Spot s2 = new Spot(new double[3], "S2");
		final Spot s3 = new Spot(new double[3], "S3");
		final Spot s4 = new Spot(new double[3], "S4");
		final Spot s5 = new Spot(new double[3], "S5");
		// Build track 2 with 2 spots
		final Spot s6 = new Spot(new double[3], "S6");
		final Spot s7 = new Spot(new double[3], "S7");
		// Build track 3 with 2 spots
		final Spot s8 = new Spot(new double[3], "S8");
		final Spot s9 = new Spot(new double[3], "S9");

		model.beginUpdate();
		try {

			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 1);
			model.addSpotTo(s3, 2);
			model.addSpotTo(s4, 3);
			model.addSpotTo(s5, 4);
			model.getTrackModel().addEdge(s1, s2, 0);
			model.getTrackModel().addEdge(s2, s3, 0);
			model.getTrackModel().addEdge(s3, s4, 0);
			model.getTrackModel().addEdge(s4, s5, 0);

			model.addSpotTo(s6, 0);
			model.addSpotTo(s7, 1);
			model.getTrackModel().addEdge(s6, s7, 0);

			model.addSpotTo(s8, 0);
			model.addSpotTo(s9, 1);
			model.getTrackModel().addEdge(s8, s9, 0);

		} finally {
			model.endUpdate();
		}

		Set<Integer> visibleTracks = model.getTrackModel().getFilteredTrackIDs();

		// These must be 3 tracks visible
		assertEquals(3, visibleTracks.size());
		// all of the tracks must be visible
		Iterator<Integer> it = model.getTrackModel().getTrackIDs().iterator();
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));

		// Delete spot s3, make 2 tracks of the first one
		model.beginUpdate();
		try {
			model.removeSpotFrom(s3, null);
		} finally {
			model.endUpdate();
		}

		// These must be 4 tracks visible
		visibleTracks = model.getTrackModel().getFilteredTrackIDs();
		assertEquals(4, visibleTracks.size());
		// all of the tracks must be visible
		it = model.getTrackModel().getTrackIDs().iterator();
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));

		// Check in what track is the spot s4
		int track2 = model.getTrackModel().getTrackIDOf(s4);
		//		System.out.println("The spot "+s4+" is in track "+track2);

		// Make it invisible
		boolean modified = model.getTrackModel().setFilteredTrackID(track2, false, false);

		// We must have modified something: it was visible, now it is invisible
		assertTrue(modified);

		// These must be now 3 tracks visible
		visibleTracks = model.getTrackModel().getFilteredTrackIDs();
		assertEquals(3, visibleTracks.size());
		// out of 4
		assertEquals(4, model.getTrackModel().getNTracks());
		// with indices different from track2
		for(int index : visibleTracks) {
			assertTrue( track2 != index ); 
		}

		// Reconnect s2 and s4
		model.beginUpdate();
		try {
			model.getTrackModel().addEdge(s2, s4, 0);
		} finally {
			model.endUpdate();
		}

		// These must be now 3 tracks visible: connecting a visible track with an invisible makes
		// it all visible
		visibleTracks = model.getTrackModel().getFilteredTrackIDs();
		assertEquals(3, visibleTracks.size());
		// out of 3
		assertEquals(3, model.getTrackModel().getNTracks());
	}


	/**
	 * Test the track number reported by the model as we modify it.
	 */
	@Test
	public void testTrackNumber() {
		TrackMateModel model = new TrackMateModel();

		// Empty model, should get 0 tracks
		assertEquals(0, model.getTrackModel().getNTracks());

		// Build track with 5 spots
		final Spot s1 = new Spot(new double[3], "S1");
		final Spot s2 = new Spot(new double[3], "S2");
		final Spot s3 = new Spot(new double[3], "S3");
		final Spot s4 = new Spot(new double[3], "S4");
		final Spot s5 = new Spot(new double[3], "S5");
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 0);
			model.addSpotTo(s3, 0);
			model.addSpotTo(s4, 0);
			model.addSpotTo(s5, 0);

			model.getTrackModel().addEdge(s1, s2, 0);
			model.getTrackModel().addEdge(s2, s3, 0);
			model.getTrackModel().addEdge(s3, s4, 0);
			model.getTrackModel().addEdge(s4, s5, 0);
		} finally {
			model.endUpdate();
		}

		// All spots are connected by edges, should build one track
		assertEquals(1, model.getTrackModel().getNTracks());

		// Remove middle spot
		model.beginUpdate();
		try {
			model.removeSpotFrom(s3, null);
		} finally {
			model.endUpdate();
		}

		// Track split in 2, should get 2 tracks
		assertEquals(2, model.getTrackModel().getNTracks());

		// Stitch back the two tracks
		model.beginUpdate();
		try {
			model.getTrackModel().addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}

		// Stitched, so we should get back one track again
		assertEquals(1, model.getTrackModel().getNTracks());

	}


	/**
	 * Test if manual adding spots and links in one update step is caught as a single 
	 * event, and that this event is well configured.
	 */
	@Test
	public void testTrackModelChangeEvent() {

		// Create a model with 5 spots, that forms a single branch track
		final TrackMateModel model = new TrackMateModel();

		// Add an event listener for that checks for adding spots and edges
		ModelChangeListener eventLogger = new ModelChangeListener() {
			@Override
			public void modelChanged(ModelChangeEvent event) {

				// Event must be of the right type
				assertEquals(ModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 5 new spots from this event
				assertEquals(5, event.getSpots().size());
				// I expect 4 new links from this event
				assertEquals(4, event.getEdges().size());
				// Check the correct flag type for spots
				for(Spot spot : event.getSpots()) {
					assertEquals(ModelChangeEvent.FLAG_SPOT_ADDED, event.getSpotFlag(spot));
				}
				// Check the correct flag type for edges
				for(DefaultWeightedEdge edge : event.getEdges()) {
					assertEquals(ModelChangeEvent.FLAG_EDGE_ADDED, event.getEdgeFlag(edge));
				}
			}
		};
		model.addTrackMateModelChangeListener(eventLogger);


		final Spot s1 = new Spot(new double[3], "S1");
		final Spot s2 = new Spot(new double[3], "S2");
		final Spot s3 = new Spot(new double[3], "S3");
		final Spot s4 = new Spot(new double[3], "S4");
		final Spot s5 = new Spot(new double[3], "S5");

		//		System.out.println("Create the graph in one update:");
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 1);
			model.addSpotTo(s3, 2);
			model.addSpotTo(s4, 3);
			model.addSpotTo(s5, 4);

			model.addEdge(s1, s2, 0);
			model.addEdge(s2, s3, 0);
			model.addEdge(s3, s4, 0);
			model.addEdge(s4, s5, 0);
		} finally {
			model.endUpdate();
		}


		// Remove old eventLogger
		model.removeTrackMateModelChangeListener(eventLogger);

		/*
		 * We will now remove the middle spot in the newly created track.
		 * This will generate an event where we will of course see the removal
		 * of the spot, but also the removal of 2 edges that were linking to 
		 * this spot. 
		 */


		// Add a new event logger that will monitor for a spot removal
		eventLogger = new ModelChangeListener() {
			@Override
			public void modelChanged(ModelChangeEvent event) {
				// Event must be of the right type
				assertEquals(ModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 1 modified spot from this event
				assertEquals(1, event.getSpots().size());
				// It must be s3
				assertEquals(s3, event.getSpots().iterator().next());
				// It must be the removed flag
				assertEquals(ModelChangeEvent.FLAG_SPOT_REMOVED, event.getSpotFlag(s3));

				// I expect 2 links to be affected by this event
				assertEquals(2, event.getEdges().size());
				// Check the correct flag type for edges: they must be removed
				for(DefaultWeightedEdge edge : event.getEdges()) {
					assertEquals(ModelChangeEvent.FLAG_EDGE_REMOVED, event.getEdgeFlag(edge));
				}
				// Check the removed edges identity
				for (DefaultWeightedEdge edge : event.getEdges()) {

					assertTrue( 
							( model.getTrackModel().getEdgeSource(edge).equals(s3) && model.getTrackModel().getEdgeTarget(edge).equals(s2) || 
									model.getTrackModel().getEdgeSource(edge).equals(s2) && model.getTrackModel().getEdgeTarget(edge).equals(s3)
									) || (
											model.getTrackModel().getEdgeSource(edge).equals(s3) && model.getTrackModel().getEdgeTarget(edge).equals(s4) 
											|| model.getTrackModel().getEdgeSource(edge).equals(s4) && model.getTrackModel().getEdgeTarget(edge).equals(s3)
											)
							);

				}
			}
		};

		model.addTrackMateModelChangeListener(eventLogger);

		model.beginUpdate();
		try {
			model.removeSpotFrom(s3, null);
		} finally {
			model.endUpdate();
		}

		/*
		 * We ended up in having 2 tracks. 
		 * We will now reconnect them by creating a new edge. This will generate an event
		 * with 1 edge and 0 spots. 
		 */

		model.removeTrackMateModelChangeListener(eventLogger);

		eventLogger = new ModelChangeListener() {
			@Override
			public void modelChanged(ModelChangeEvent event) {
				// Event must be of the right type
				assertEquals(ModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 0 modified spot from this event, so spot field must be empty
				assertTrue(event.getSpots().isEmpty());
				// It must be s3

				// I expect 1 new link in this event
				assertEquals(1, event.getEdges().size());
				// Check the correct flag type for edges: they must be removed
				for(DefaultWeightedEdge edge : event.getEdges()) {
					assertEquals(ModelChangeEvent.FLAG_EDGE_ADDED, event.getEdgeFlag(edge));
				}
				// Check the added edges identity
				for (DefaultWeightedEdge edge : event.getEdges()) {

					assertTrue( 
							( model.getTrackModel().getEdgeSource(edge).equals(s2) && model.getTrackModel().getEdgeTarget(edge).equals(s4) || 
									model.getTrackModel().getEdgeSource(edge).equals(s4) && model.getTrackModel().getEdgeTarget(edge).equals(s2)	) 
							);
				}
			}
		};

		model.addTrackMateModelChangeListener(eventLogger);

		model.beginUpdate();
		try {
			model.getTrackModel().addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}

	}






	/*
	 * EXAMPLE
	 */


	public void exampleManipulation() {

		// Create a model with 5 spots, that forms a single branch track
		TrackMateModel model = new TrackMateModel();

		// Add an event listener now
		model.addTrackMateModelChangeListener(new EventLogger());

		Spot s1 = new Spot(new double[3], "S1");
		Spot s2 = new Spot(new double[3], "S2");
		Spot s3 = new Spot(new double[3], "S3");
		Spot s4 = new Spot(new double[3], "S4");
		Spot s5 = new Spot(new double[3], "S5");


		System.out.println("Create the graph in one update:");
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 0);
			model.addSpotTo(s3, 0);
			model.addSpotTo(s4, 0);
			model.addSpotTo(s5, 0);

			model.getTrackModel().addEdge(s1, s2, 0);
			model.getTrackModel().addEdge(s2, s3, 0);
			model.getTrackModel().addEdge(s3, s4, 0);
			model.getTrackModel().addEdge(s4, s5, 0);
		} finally {
			model.endUpdate();
		}

		System.out.println();
		System.out.println("Tracks are:");
		for (Integer trackID : model.getTrackModel().getTrackIDs()) {
			System.out.println("\tTrack "+trackID+" with name: " + model.getTrackModel().getTrackName(trackID));
			System.out.println("\t\t"+model.getTrackModel().getTrackSpots().get(trackID));
			System.out.println("\t\t"+model.getTrackModel().getTrackEdges().get(trackID));
		}
		System.out.println();
		System.out.println();



		// Remove one spot and see what happens
		System.out.println("Removing a single spot in the middle of the track:");
		model.beginUpdate();
		try {
			model.removeSpotFrom(s3, null);
		} finally {
			model.endUpdate();
		}
		System.out.println();
		System.out.println("Tracks are:");
		for (Integer trackID : model.getTrackModel().getTrackIDs()) {
			System.out.println("\tTrack "+trackID+" with name: " + model.getTrackModel().getTrackName(trackID));
			System.out.println("\t\t"+model.getTrackModel().getTrackSpots().get(trackID));
			System.out.println("\t\t"+model.getTrackModel().getTrackEdges().get(trackID));
		}
		System.out.println("Track visibility is:");
		System.out.println(model.getTrackModel().getFilteredTrackIDs());



		System.out.println();
		System.out.println();


		System.out.println("Making the first track invisible:");
		model.getTrackModel().setFilteredTrackID(model.getTrackModel().getTrackIDs().iterator().next(), false, true);

		System.out.println("Track visibility is:");
		System.out.println(model.getTrackModel().getFilteredTrackIDs());



		System.out.println();
		System.out.println();


		System.out.println("Reconnect the 2 tracks:");
		model.beginUpdate();
		try {
			model.getTrackModel().addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}


		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Tracks are:");
		for (Integer trackID : model.getTrackModel().getTrackIDs()) {
			System.out.println("\tTrack "+trackID+" with name: " + model.getTrackModel().getTrackName(trackID));
			System.out.println("\t\t"+model.getTrackModel().getTrackSpots().get(trackID));
			System.out.println("\t\t"+model.getTrackModel().getTrackEdges().get(trackID));
		}
		System.out.println("Track visibility is:");
		System.out.println(model.getTrackModel().getFilteredTrackIDs());

	}



	public static void main(String[] args) {
		new TrackMateModelTest().exampleManipulation();
	}

	private static class EventLogger implements ModelChangeListener {

		@Override
		public void modelChanged(ModelChangeEvent event) {
			// Simply append it to sysout
			System.out.println(event);

		}

	}
}
