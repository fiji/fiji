package fiji.plugin.trackmate.tracking.test;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.TrackerType;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class LAPTrackerTestDrive {
	
	private static final File SPLITTING_CASE_3 = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String args[]) {
		
		File file = SPLITTING_CASE_3;
		
		// 1 - Load test spots
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = null;
		// Parse
		try {
			reader.parse();
			model = reader.getModel();
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		// 1.5 - Set the tracking settings
		TrackerSettings settings = new TrackerSettings();
		settings.trackerType = TrackerType.LAP_TRACKER;
		settings.linkingDistanceCutOff = 10;
		settings.allowGapClosing = false; //true;
		settings.gapClosingDistanceCutoff = 100;
		settings.gapClosingTimeCutoff = 10;
		settings.allowMerging = true;
		settings.mergingDistanceCutoff = 10;
		settings.mergingTimeCutoff = 2;
		settings.mergingFeatureCutoffs.clear();
		settings.allowSplitting = true;
		settings.splittingDistanceCutoff = 10;
		settings.splittingTimeCutoff = 2;
		settings.splittingFeatureCutoffs.clear();
		System.out.println("Tracker settings:");
		System.out.println(settings.toString());
		model.getSettings().trackerSettings = settings;
		
		// 2 - Track the test spots
		long start = System.currentTimeMillis();
		LAPTracker lap;
		lap = new LAPTracker(model.getFilteredSpots(), settings); //model.getSettings().trackerSettings);
		lap.setLogger(Logger.DEFAULT_LOGGER);
		if (!lap.checkInput())
			System.err.println("Error checking input: "+lap.getErrorMessage());
		if (!lap.process())
			System.err.println("Error in process: "+lap.getErrorMessage());
		long end = System.currentTimeMillis();
		model.setTracks(lap.getTracks(), false);
		
	
		// 3 - Print out results for testing		
		System.out.println();
		System.out.println();
		System.out.println();
		TrackCollection tracks = lap.getTracks();
		System.out.println("Found " + tracks.size() + " final tracks.");
		System.out.println("Whole tracking done in "+(end-start)+" ms.");
		System.out.println();


		// 4 - Detailed info
//		System.out.println("Segment costs:");
//		LAPUtils.echoMatrix(lap.getSegmentCosts());
		
		System.out.println("Track features: ");
		for (int i = 0; i < tracks.size(); i++) {
			System.out.println(tracks.toString(i));
		}
		
		
		// 5 - Display tracks
		// Load Image
		ij.ImageJ.main(args);
		
		TrackMateModelView sd2d = new HyperStackDisplayer(model);
		sd2d.render();
		sd2d.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE);
	}

}
