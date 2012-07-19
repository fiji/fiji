package fiji.plugin.trackmate.tests;

import java.io.File;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerSettings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class NNTrackerTest {

	
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
		NearestNeighborTrackerSettings<T> settings = new NearestNeighborTrackerSettings<T>();
		settings.maxLinkingDistance = 15;
		
		System.out.println("Tracker settings:");
		System.out.println(settings.toString());
		model.getSettings().trackerSettings = settings;
		
		// 2 - Track the test spots
		long start = System.currentTimeMillis();
		NearestNeighborTracker<T> tracker = new NearestNeighborTracker<T>();
		tracker.setModel(model);
		tracker.setLogger(Logger.DEFAULT_LOGGER);

		if (!tracker.checkInput())
			System.err.println("Error checking input: "+tracker.getErrorMessage());
		if (!tracker.process())
			System.err.println("Error in process: "+tracker.getErrorMessage());
		long end = System.currentTimeMillis();
		model.setGraph(tracker.getResult());
		
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
