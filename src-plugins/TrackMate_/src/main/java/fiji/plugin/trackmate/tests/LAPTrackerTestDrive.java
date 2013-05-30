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

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class LAPTrackerTestDrive {
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String args[]) {

		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		
		// 1 - Load test spots
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader reader = new TmXmlReader(file);
		Model model = reader.getModel();
		Settings settings = new Settings();
		reader.readSettings(settings, null, new TrackerProvider(model), null, null, null);
		
		System.out.println("Spots: "+ model.getSpots());
		System.out.println("Found "+model.getTrackModel().nTracks(false)+" tracks in the file:");
		System.out.println();
		
		// 1.5 - Set the tracking settings
		Map<String, Object> ts = LAPUtils.getDefaultLAPSettingsMap();
		ts.put(KEY_LINKING_MAX_DISTANCE, 10d);
		ts.put(KEY_ALLOW_GAP_CLOSING, true);
		ts.put(KEY_GAP_CLOSING_MAX_DISTANCE, 15d);
		ts.put(KEY_ALLOW_TRACK_MERGING, true);
		ts.put(KEY_MERGING_MAX_DISTANCE, 10d);
		ts.put(KEY_ALLOW_TRACK_SPLITTING, true);
		ts.put(KEY_SPLITTING_MAX_DISTANCE, 10d);
		settings.trackerSettings = ts;
		System.out.println("Tracker settings:");
		System.out.println(settings);
		
		// 2 - Track the test spots
		long start = System.currentTimeMillis();
		LAPTracker lap = new LAPTracker(model.getSpots(), Logger.DEFAULT_LOGGER);
		lap.setSettings(ts);

		if (!lap.checkInput())
			System.err.println("Error checking input: "+lap.getErrorMessage());
		if (!lap.process())
			System.err.println("Error in process: "+lap.getErrorMessage());
		long end = System.currentTimeMillis();
		
		
		// 2.5 check the track visibility prior and after
		System.out.println("Track visibility before new graph allocation:");
		System.out.println("On the following tracks ID:");
		for (Integer trackID : model.getTrackModel().trackIDs(false)) 
			System.out.print(trackID + ", ");
		System.out.println("\nthe following were filtered:");
		for (Integer trackID : model.getTrackModel().trackIDs(true)) 
			System.out.print(trackID + ", ");
		System.out.println();
		System.out.println();
		
		// Pass the new graph
		model.getTrackModel().setGraph(lap.getResult());

		System.out.println("Track visibility after new graph allocation:");
		System.out.println("On the following tracks ID:");
		for (Integer trackID : model.getTrackModel().trackIDs(false)) 
			System.out.print(trackID + ", ");
		System.out.println("\nthe following were filtered:");
		for (Integer trackID : model.getTrackModel().trackIDs(true)) 
			System.out.print(trackID + ", ");
		System.out.println();

		
		// 3 - Print out results for testing		
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Found " + model.getTrackModel().nTracks(false) + " final tracks.");
		System.out.println("Whole tracking done in "+(end-start)+" ms.");
		System.out.println();


		// 4 - Detailed info
//		System.out.println("Segment costs:");
//		LAPUtils.echoMatrix(lap.getSegmentCosts());
		
		// 5 - Display tracks
		// Load Image
		ij.ImageJ.main(args);
		
		TrackMateModelView sd2d = new HyperStackDisplayer(model, new SelectionModel(model), settings.imp);
		sd2d.render();
		sd2d.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE);
	}

}

