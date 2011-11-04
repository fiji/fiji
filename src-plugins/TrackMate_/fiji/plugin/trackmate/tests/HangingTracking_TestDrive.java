package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.spot.BlobMorphology;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPTrackerSettings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class HangingTracking_TestDrive {

//	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/Scoobidoo.xml");
	private static final File file = new File("/Users/tinevez/Desktop/Data/Scoobidoo.xml");

	public static void main(String[] args) throws JDOMException, IOException {

		TmXmlReader reader = new TmXmlReader(file, Logger.DEFAULT_LOGGER);
		reader.parse();
		SpotCollection spots = reader.getAllSpots();
		SpotCollection filteredSpots = reader.getFilteredSpots(spots);
		TrackMateModel model = reader.getModel();

		int frame = 1;

		System.out.println();
		System.out.println("Without feature condition:");
		Settings settings = new Settings();
		reader.getTrackerSettings(settings);
		LAPTrackerSettings trackerSettings = (LAPTrackerSettings) settings.trackerSettings;
		trackerSettings.linkingDistanceCutOff = 60;
		model.getSettings().trackerSettings = trackerSettings;
		
		LAPTracker tracker = new LAPTracker();
		tracker.setModel(model);
		
		System.out.println("For frame pair "+frame+" -> "+(frame+1)+":");
		System.out.println("There are "+filteredSpots.getNSpots(frame)+" spots to link to "+filteredSpots.getNSpots(frame+1));

		System.out.println();
		System.out.println("With feature condition:");
		trackerSettings.linkingFeaturePenalties.put(BlobMorphology.MORPHOLOGY, (double) 1);
		
		tracker = new LAPTracker();
		tracker.setModel(model);
		
		System.out.println("For frame pair "+frame+" -> "+(frame+1)+":");
		System.out.println("There are "+filteredSpots.getNSpots(frame)+" spots to link to "+filteredSpots.getNSpots(frame+1));

		tracker.solveLAPForTrackSegments();
		model.setGraph(tracker.getResult());
		
		TrackMateModelView view = new HyperStackDisplayer();
		view.setModel(model);
		view.render();
		
	}

}
