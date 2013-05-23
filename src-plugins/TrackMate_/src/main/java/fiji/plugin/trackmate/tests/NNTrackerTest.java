package fiji.plugin.trackmate.tests;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import ij.ImagePlus;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class NNTrackerTest {

	/*
	 * MAIN METHOD
	 */
	
	public static void main(String args[]) {
		

		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		
		// 1 - Load test spots
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = reader.getModel();
		Settings gs = new Settings();
		reader.readSettings(gs, null, null, null, null, null);
		
		System.out.println("Spots: "+ model.getSpots());
		System.out.println("Found "+model.getTrackModel().getNTracks()+" tracks in the file:");
		System.out.println("Track features: ");
		for (Integer trackID : model.getTrackModel().getTrackIDs()) {
			System.out.println(model.getTrackModel().trackToString(trackID));
		}
		System.out.println();
		
		// 2 - Track the test spots
		long start = System.currentTimeMillis();
		NearestNeighborTracker tracker = new NearestNeighborTracker(model.getSpots(), Logger.DEFAULT_LOGGER);
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

		
		// 5 - Display tracks
		// Load Image
		ij.ImageJ.main(args);
		
		ImagePlus imp = gs.imp;
		TrackMateModelView sd2d = new HyperStackDisplayer(model, new SelectionModel(model), imp);
		sd2d.render();
		sd2d.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE);
	}

	
}
