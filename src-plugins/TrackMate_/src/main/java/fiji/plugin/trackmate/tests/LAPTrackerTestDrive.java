package fiji.plugin.trackmate.tests;

import java.io.File;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class LAPTrackerTestDrive {
	
//	private static final File SPLITTING_CASE_3 = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
	private static final File SPLITTING_CASE_3 = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");

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
		TrackerKeys<T> settings = new TrackerKeys<T>();
		settings.linkingDistanceCutOff = 10;
		settings.allowGapClosing = false;
		settings.gapClosingDistanceCutoff = 15;
		settings.gapClosingTimeCutoff = 10;
		settings.allowMerging = false;
		settings.mergingDistanceCutoff = 10;
		settings.mergingTimeCutoff = 2;
		settings.mergingFeaturePenalties.clear();
		settings.allowSplitting = false;
		settings.splittingDistanceCutoff = 10;
		settings.splittingTimeCutoff = 2;
		settings.splittingFeaturePenalties.clear();
		System.out.println("Tracker settings:");
		System.out.println(settings.toString());
		model.getSettings().trackerSettings = settings;
		
		// 2 - Track the test spots
		long start = System.currentTimeMillis();
		LAPTracker<T> lap = new LAPTracker<T>();
		lap.setModel(model);
		lap.setLogger(Logger.DEFAULT_LOGGER);

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
