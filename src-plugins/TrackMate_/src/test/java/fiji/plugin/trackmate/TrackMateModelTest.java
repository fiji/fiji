package fiji.plugin.trackmate;

import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateModelChangeListener;

public class TrackMateModelTest <T extends RealType<T> & NativeType<T>>   {


	/**
	 * Test if the track visibility is followed correctly.
	 */
	@Test
	public void testTrackVisibility() {
		TrackMateModel<T> model = new TrackMateModel<T>();
		// Build track 1 with 5 spots
		final Spot s1 = new SpotImp(new double[3], "S1");
		final Spot s2 = new SpotImp(new double[3], "S2");
		final Spot s3 = new SpotImp(new double[3], "S3");
		final Spot s4 = new SpotImp(new double[3], "S4");
		final Spot s5 = new SpotImp(new double[3], "S5");
		// Build track 2 with 2 spots
		final Spot s6 = new SpotImp(new double[3], "S6");
		final Spot s7 = new SpotImp(new double[3], "S7");
		// Build track 3 with 2 spots
		final Spot s8 = new SpotImp(new double[3], "S8");
		final Spot s9 = new SpotImp(new double[3], "S9");

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

			model.addSpotTo(s6, 0);
			model.addSpotTo(s7, 1);
			model.addEdge(s6, s7, 0);

			model.addSpotTo(s8, 0);
			model.addSpotTo(s9, 1);
			model.addEdge(s8, s9, 0);

		} finally {
			model.endUpdate();
		}

		Set<Integer> visibleTracks = model.getFilteredTrackKeys();

		// These must be 3 tracks visible
		assertEquals(3, visibleTracks.size());
		// with indices 0, 1, 2
		assertTrue(visibleTracks.contains(0));
		assertTrue(visibleTracks.contains(1));
		assertTrue(visibleTracks.contains(2));

		// Delete spot s3, make 2 tracks of the first one
		model.beginUpdate();
		try {
			model.removeSpotFrom(s3, null);
		} finally {
			model.endUpdate();
		}
		
		// These must be 4 tracks visible
		visibleTracks = model.getFilteredTrackKeys();
		assertEquals(4, visibleTracks.size());
		// with indices 0, 1, 2 & 3
		assertTrue(visibleTracks.contains(0));
		assertTrue(visibleTracks.contains(1));
		assertTrue(visibleTracks.contains(2));
		assertTrue(visibleTracks.contains(3));

		// Check in what track is the spot s4
		int track2 = model.getTrackIndexOf(s4);
//		System.out.println("The spot "+s4+" is in track "+track2);
		
		// Make it invisible
		boolean modified = model.setTrackVisible(track2, false, false);
		
		// We must have modified something: it was visible, now it is invisible
		assertTrue(modified);
		
		// These must be now 3 tracks visible
		visibleTracks = model.getFilteredTrackKeys();
		assertEquals(3, visibleTracks.size());
		// out of 4
		assertEquals(4, model.getNTracks());
		// with indices different from track2
		for(int index : visibleTracks) {
			assertTrue( track2 != index ); 
		}
		
		// Reconnect s2 and s4
		model.beginUpdate();
		try {
			model.addEdge(s2, s4, 0);
		} finally {
			model.endUpdate();
		}
		
		// These must be now 3 tracks visible: connecting a visible track with an invisible makes
		// it all visible
		visibleTracks = model.getFilteredTrackKeys();
		assertEquals(3, visibleTracks.size());
		// out of 3
		assertEquals(3, model.getNTracks());
	}


	/**
	 * Test the track number reported by the model as we modify it.
	 */
	@Test
	public void testTrackNumber() {
		TrackMateModel<T> model = new TrackMateModel<T>();

		// Empty model, should get 0 tracks
		assertEquals(0, model.getNTracks());

		// Build track with 5 spots
		final Spot s1 = new SpotImp(new double[3], "S1");
		final Spot s2 = new SpotImp(new double[3], "S2");
		final Spot s3 = new SpotImp(new double[3], "S3");
		final Spot s4 = new SpotImp(new double[3], "S4");
		final Spot s5 = new SpotImp(new double[3], "S5");
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 0);
			model.addSpotTo(s3, 0);
			model.addSpotTo(s4, 0);
			model.addSpotTo(s5, 0);

			model.addEdge(s1, s2, 0);
			model.addEdge(s2, s3, 0);
			model.addEdge(s3, s4, 0);
			model.addEdge(s4, s5, 0);
		} finally {
			model.endUpdate();
		}

		// All spots are connected by edges, should build one track
		assertEquals(1, model.getNTracks());

		// Remove middle spot
		model.beginUpdate();
		try {
			model.removeSpotFrom(s3, null);
		} finally {
			model.endUpdate();
		}

		// Track split in 2, should get 2 tracks
		assertEquals(2, model.getNTracks());

		// Stitch back the two tracks
		model.beginUpdate();
		try {
			model.addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}

		// Stitched, so we should get back one track again
		assertEquals(1, model.getNTracks());

	}


	/**
	 * Test if manual adding spots and links in one update step is caught as a single 
	 * event, and that this event is well configured.
	 */
	@Test
	public void testTrackModelChangeEvent() {

		// Create a model with 5 spots, that forms a single branch track
		final TrackMateModel<T> model = new TrackMateModel<T>();

		// Add an event listener for that checks for adding spots and edges
		TrackMateModelChangeListener eventLogger = new TrackMateModelChangeListener() {
			@Override
			public void modelChanged(TrackMateModelChangeEvent event) {
				// Event must be of the right type
				assertEquals(TrackMateModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 5 new spots from this event
				assertEquals(5, event.getSpots().size());
				// I expect 4 new links from this event
				assertEquals(4, event.getEdges().size());
				// Check the correct flag type for spots
				for(int eventFlag : event.getSpotFlags()) {
					assertEquals(TrackMateModelChangeEvent.FLAG_SPOT_ADDED, eventFlag);
				}
				// Check the correct flag type for edges
				for(int eventFlag : event.getEdgeFlags()) {
					assertEquals(TrackMateModelChangeEvent.FLAG_EDGE_ADDED, eventFlag);
				}
			}
		};
		model.addTrackMateModelChangeListener(eventLogger);


		final Spot s1 = new SpotImp(new double[3], "S1");
		final Spot s2 = new SpotImp(new double[3], "S2");
		final Spot s3 = new SpotImp(new double[3], "S3");
		final Spot s4 = new SpotImp(new double[3], "S4");
		final Spot s5 = new SpotImp(new double[3], "S5");

		//		System.out.println("Create the graph in one update:");
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 0);
			model.addSpotTo(s3, 0);
			model.addSpotTo(s4, 0);
			model.addSpotTo(s5, 0);

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
		eventLogger = new TrackMateModelChangeListener() {
			@Override
			public void modelChanged(TrackMateModelChangeEvent event) {
				// Event must be of the right type
				assertEquals(TrackMateModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 1 modified spot from this event
				assertEquals(1, event.getSpots().size());
				// It must be s3
				assertEquals(s3, event.getSpots().get(0));
				// It must be the removed flag
				assertEquals(TrackMateModelChangeEvent.FLAG_SPOT_REMOVED, event.getSpotFlag(s3).intValue());

				// I expect 2 links to be affected by this event
				assertEquals(2, event.getEdges().size());
				// Check the correct flag type for edges: they must be removed
				for(int eventFlag : event.getEdgeFlags()) {
					assertEquals(TrackMateModelChangeEvent.FLAG_EDGE_REMOVED, eventFlag);
				}
				// Check the removed edges identity
				for (DefaultWeightedEdge edge : event.getEdges()) {

					assertTrue( 
							( model.getEdgeSource(edge).equals(s3) && model.getEdgeTarget(edge).equals(s2) || 
									model.getEdgeSource(edge).equals(s2) && model.getEdgeTarget(edge).equals(s3)
									) || (
											model.getEdgeSource(edge).equals(s3) && model.getEdgeTarget(edge).equals(s4) 
											|| model.getEdgeSource(edge).equals(s4) && model.getEdgeTarget(edge).equals(s3)
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

		eventLogger = new TrackMateModelChangeListener() {
			@Override
			public void modelChanged(TrackMateModelChangeEvent event) {
				// Event must be of the right type
				assertEquals(TrackMateModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 0 modified spot from this event, so spot fiel must be null
				assertNull(event.getSpots());
				// It must be s3

				// I expect 1 new link in this event
				assertEquals(1, event.getEdges().size());
				// Check the correct flag type for edges: they must be removed
				for(int eventFlag : event.getEdgeFlags()) {
					assertEquals(TrackMateModelChangeEvent.FLAG_EDGE_ADDED, eventFlag);
				}
				// Check the added edges identity
				for (DefaultWeightedEdge edge : event.getEdges()) {

					assertTrue( 
							( model.getEdgeSource(edge).equals(s2) && model.getEdgeTarget(edge).equals(s4) || 
									model.getEdgeSource(edge).equals(s4) && model.getEdgeTarget(edge).equals(s2)	) 
							);
				}
			}
		};

		model.addTrackMateModelChangeListener(eventLogger);

		model.beginUpdate();
		try {
			model.addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}

	}






	/*
	 * EXAMPLE
	 */


	public void exampleManipulation() {

		// Create a model with 5 spots, that forms a single branch track
		TrackMateModel<T> model = new TrackMateModel<T>();

		// Add an event listener now
		model.addTrackMateModelChangeListener(new EventLogger());

		Spot s1 = new SpotImp(new double[3], "S1");
		Spot s2 = new SpotImp(new double[3], "S2");
		Spot s3 = new SpotImp(new double[3], "S3");
		Spot s4 = new SpotImp(new double[3], "S4");
		Spot s5 = new SpotImp(new double[3], "S5");


		System.out.println("Create the graph in one update:");
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 0);
			model.addSpotTo(s3, 0);
			model.addSpotTo(s4, 0);
			model.addSpotTo(s5, 0);

			model.addEdge(s1, s2, 0);
			model.addEdge(s2, s3, 0);
			model.addEdge(s3, s4, 0);
			model.addEdge(s4, s5, 0);
		} finally {
			model.endUpdate();
		}

		System.out.println();
		System.out.println("Tracks are:");
		for (int i = 0; i < model.getNTracks(); i++) {
			System.out.println("\tTrack "+i+":");
			System.out.println("\t\t"+model.getTrackSpots().get(i));
			System.out.println("\t\t"+model.getTrackEdges().get(i));
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
		for (int i = 0; i < model.getNTracks(); i++) {
			System.out.println("\tTrack "+i+":");
			System.out.println("\t\t"+model.getTrackSpots().get(i));
			System.out.println("\t\t"+model.getTrackEdges().get(i));
		}
		System.out.println("Track visibility is:");
		System.out.println(model.getFilteredTrackKeys());



		System.out.println();
		System.out.println();


		System.out.println("Making the second track invisible:");
		model.setTrackVisible(1, false, true);

		System.out.println("Track visibility is:");
		System.out.println(model.getFilteredTrackKeys());



		System.out.println();
		System.out.println();


		System.out.println("Reconnect the 2 tracks:");
		model.beginUpdate();
		try {
			model.addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}


		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Tracks are:");
		for (int i = 0; i < model.getNTracks(); i++) {
			System.out.println("\tTrack "+i+":");
			System.out.println("\t\t"+model.getTrackSpots().get(i));
			System.out.println("\t\t"+model.getTrackEdges().get(i));
		}
		System.out.println("Track visibility is:");
		System.out.println(model.getFilteredTrackKeys());

	}



	public static <T extends RealType<T> & NativeType<T>>  void main(String[] args) {
		new TrackMateModelTest<T>().exampleManipulation();
	}

	private static class EventLogger implements TrackMateModelChangeListener {

		@Override
		public void modelChanged(TrackMateModelChangeEvent event) {
			// Simply append it to sysout
			System.out.println(event);

		}

	}
}
