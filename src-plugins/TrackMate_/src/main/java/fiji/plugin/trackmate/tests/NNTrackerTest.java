package fiji.plugin.trackmate.tests;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class NNTrackerTest {

	
	private static final File SPLITTING_CASE_3 = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
//	private static final File SPLITTING_CASE_3 = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");

	/*
	 * MAIN METHOD
	 */
	
	public static void main(String args[]) {
		
		File file = SPLITTING_CASE_3;
		
		// 1 - Load test spots
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TrackMate_ plugin = new TrackMate_();
		plugin.initModules();
		TmXmlReader reader = new TmXmlReader(file, plugin);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		TrackMateModel model = plugin.getModel();
		
		System.out.println("All spots: "+ model.getSpots());
		System.out.println("Filtered spots: "+ model.getFilteredSpots());
		System.out.println("Found "+model.getTrackModel().getNTracks()+" tracks in the file:");
		System.out.println("Track features: ");
		plugin.computeTrackFeatures(true);
		for (Integer trackID : model.getTrackModel().getTrackIDs()) {
			System.out.println(model.getTrackModel().trackToString(trackID));
		}
		System.out.println();
		
		// 2 - Track the test spots
		long start = System.currentTimeMillis();
		NearestNeighborTracker tracker = new NearestNeighborTracker(model.getFilteredSpots(), Logger.DEFAULT_LOGGER);
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put(KEY_LINKING_MAX_DISTANCE, 15d);
		tracker.setSettings(settings );

		if (!tracker.checkInput())
			System.err.println("Error checking input: "+tracker.getErrorMessage());
		if (!tracker.process())
			System.err.println("Error in process: "+tracker.getErrorMessage());
		long end = System.currentTimeMillis();
		model.getTrackModel().setGraph(tracker.getResult());
		
		// 3 - Print out results for testing		
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Found " + model.getTrackModel().getNTracks() + " final tracks.");
		System.out.println("Whole tracking done in "+(end-start)+" ms.");
		System.out.println();


		// 4 - Detailed info
//		System.out.println("Segment costs:");
//		LAPUtils.echoMatrix(lap.getSegmentCosts());
		
		System.out.println("Track features: ");
		plugin.computeTrackFeatures(true);
		for (Integer trackID : model.getTrackModel().getTrackIDs()) {
			System.out.println(model.getTrackModel().trackToString(trackID));
		}
		
		
		// 5 - Display tracks
		// Load Image
		ij.ImageJ.main(args);
		
		TrackMateModelView sd2d = new HyperStackDisplayer(model);
		sd2d.render();
		sd2d.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE);
	}

	
}
