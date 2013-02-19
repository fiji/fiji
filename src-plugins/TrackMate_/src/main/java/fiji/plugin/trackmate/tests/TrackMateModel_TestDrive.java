package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateModelChangeListener;

public class TrackMateModel_TestDrive {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Create a model with 5 spots, that forms a single branch track
		TrackMateModel model = new TrackMateModel();

		// Add an event listener now
		model.addTrackMateModelChangeListener(new EventLogger());

		Spot s1 = new SpotImp(new float[3], "S1");
		Spot s2 = new SpotImp(new float[3], "S2");
		Spot s3 = new SpotImp(new float[3], "S3");
		Spot s4 = new SpotImp(new float[3], "S4");
		Spot s5 = new SpotImp(new float[3], "S5");

		
		System.out.println("Create the graph in one update:");
		System.out.println();
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
		System.out.println(model.getVisibleTrackIndices());

		

		System.out.println();
		System.out.println();
		
		
		System.out.println("Making the second track invisible:");
		model.setTrackVisible(1, false, true);
		
		System.out.println("Track visibility is:");
		System.out.println(model.getVisibleTrackIndices());



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
		System.out.println(model.getVisibleTrackIndices());

	}



	private static class EventLogger implements TrackMateModelChangeListener {

		@Override
		public void modelChanged(TrackMateModelChangeEvent event) {
			// Simply append it to sysout
			System.out.println(event);

		}

	}
}
