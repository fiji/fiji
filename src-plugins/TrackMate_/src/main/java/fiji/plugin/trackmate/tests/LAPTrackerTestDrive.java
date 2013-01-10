package fiji.plugin.trackmate.tests;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.io.File;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class LAPTrackerTestDrive {
	
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
			return;
		}
		TrackMateModel model = plugin.getModel();
		
		System.out.println("All spots: "+ model.getSpots());
		System.out.println("Filtered spots: "+ model.getFilteredSpots());
		plugin.computeTrackFeatures(true);
		System.out.println("Found "+model.getTrackModel().getNTracks()+" tracks in the file:");
		for (Integer trackID : model.getTrackModel().getTrackEdges().keySet())
			System.out.println('\t'+model.getTrackModel().trackToString(trackID));
		System.out.println();
		
		// 1.5 - Set the tracking settings
		Map<String, Object> settings = LAPUtils.getDefaultLAPSettingsMap();
		settings.put(KEY_LINKING_MAX_DISTANCE, 10d);
		settings.put(KEY_ALLOW_GAP_CLOSING, false);
		settings.put(KEY_GAP_CLOSING_MAX_DISTANCE, 15d);
		settings.put(KEY_ALLOW_TRACK_MERGING, false);
		settings.put(KEY_MERGING_MAX_DISTANCE, 10d);
		settings.put(KEY_ALLOW_TRACK_SPLITTING, false);
		settings.put(KEY_SPLITTING_MAX_DISTANCE, 10d);
		System.out.println("Tracker settings:");
		model.getSettings().trackerSettings = settings;
		
		// 2 - Track the test spots
		long start = System.currentTimeMillis();
		LAPTracker lap = new LAPTracker(model.getFilteredSpots(), Logger.DEFAULT_LOGGER);
		lap.setSettings(settings);

		if (!lap.checkInput())
			System.err.println("Error checking input: "+lap.getErrorMessage());
		if (!lap.process())
			System.err.println("Error in process: "+lap.getErrorMessage());
		long end = System.currentTimeMillis();
		
		
		// 2.5 check the track visibility prior and after
		System.out.println("Track visibility before new graph allocation:");
		System.out.println("On the following tracks ID:");
		for (Integer trackID : model.getTrackModel().getTrackIDs()) 
			System.out.print(trackID + ", ");
		System.out.println("\nthe following were filtered:");
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) 
			System.out.print(trackID + ", ");
		System.out.println();
		
		// Pass the new graph
		model.getTrackModel().setGraph(lap.getResult());

		System.out.println("Track visibility after new graph allocation:");
		System.out.println("On the following tracks ID:");
		for (Integer trackID : model.getTrackModel().getTrackIDs()) 
			System.out.print(trackID + ", ");
		System.out.println("\nthe following were filtered:");
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) 
			System.out.print(trackID + ", ");
		System.out.println();

		
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
		for (Integer trackID : model.getTrackModel().getTrackEdges().keySet()) {
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

