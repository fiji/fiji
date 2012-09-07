package fiji.plugin.trackmate.tests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class LAPTrackerTestDrive implements TrackerKeys {
	
	private static final File SPLITTING_CASE_3 = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
//	private static final File SPLITTING_CASE_3 = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");

	/*
	 * MAIN METHOD
	 */
	
	public static <T extends RealType<T> & NativeType<T>> void main(String args[]) {
		
		File file = SPLITTING_CASE_3;
		
		// 1 - Load test spots
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader<T> reader = new TmXmlReader<T>(file, Logger.DEFAULT_LOGGER);
		TrackMateModel<T> model = null;
		// Parse
		reader.parse();
		model = reader.getModel();
		
		System.out.println("All spots: "+ model.getSpots());
		System.out.println("Filtered spots: "+ model.getFilteredSpots());
		System.out.println("Found "+model.getNTracks()+" tracks in the file:");
		for(int i=0; i<model.getNTracks(); i++)
			System.out.println('\t'+model.trackToString(i));
		System.out.println();
		
		// 1.5 - Set the tracking settings
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put(KEY_LINKING_MAX_DISTANCE, 10d);
		settings.put(KEY_ALLOW_GAP_CLOSING, false);
		settings.put(KEY_GAP_CLOSING_MAX_DISTANCE, 15);
		settings.put(KEY_ALLOW_TRACK_MERGING, false);
		settings.put(KEY_MERGING_MAX_DISTANCE, 10);
		settings.put(KEY_ALLOW_TRACK_SPLITTING, false);
		settings.put(KEY_SPLITTING_MAX_DISTANCE, 10);
		System.out.println("Tracker settings:");
		model.getSettings().trackerSettings = settings;
		
		// 2 - Track the test spots
		long start = System.currentTimeMillis();
		LAPTracker lap = new LAPTracker(model.getFilteredSpots(), settings, Logger.DEFAULT_LOGGER);

		if (!lap.checkInput())
			System.err.println("Error checking input: "+lap.getErrorMessage());
		if (!lap.process())
			System.err.println("Error in process: "+lap.getErrorMessage());
		long end = System.currentTimeMillis();
		model.setGraph(lap.getResult());
		
		// 3 - Print out results for testing		
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Found " + model.getNTracks() + " final tracks.");
		System.out.println("Whole tracking done in "+(end-start)+" ms.");
		System.out.println();


		// 4 - Detailed info
//		System.out.println("Segment costs:");
//		LAPUtils.echoMatrix(lap.getSegmentCosts());
		
		System.out.println("Track features: ");
		for (int i = 0; i < model.getNTracks(); i++) {
			System.out.println(model.trackToString(i));
		}
		
		
		// 5 - Display tracks
		// Load Image
		ij.ImageJ.main(args);
		
		TrackMateModelView<T> sd2d = new HyperStackDisplayer<T>();
		sd2d.setModel(model);
		sd2d.render();
		sd2d.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE);
	}

}

