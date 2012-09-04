package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.spot.BlobMorphology;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class HangingTracking_TestDrive {

//	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/Scoobidoo.xml");
	private static final File file = new File("/Users/tinevez/Desktop/Data/Scoobidoo.xml");

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws JDOMException, IOException {

		TmXmlReader<T> reader = new TmXmlReader<T>(file, Logger.DEFAULT_LOGGER);
		reader.parse();
		@SuppressWarnings("unused")
		SpotCollection spots = reader.getAllSpots();
		SpotCollection filteredSpots = reader.getFilteredSpots();
		TrackMateModel<T> model = reader.getModel();

		int frame = 1;

		System.out.println();
		System.out.println("Without feature condition:");
		Settings<T> settings = new Settings<T>();
		reader.getTrackerSettings(settings);
		TrackerKeys<T> trackerSettings = (TrackerKeys<T>) settings.trackerSettings;
		trackerSettings.linkingDistanceCutOff = 60;
		model.getSettings().trackerSettings = trackerSettings;
		
		LAPTracker<T> tracker = new LAPTracker<T>();
		tracker.setModel(model);
		
		System.out.println("For frame pair "+frame+" -> "+(frame+1)+":");
		System.out.println("There are "+filteredSpots.getNSpots(frame)+" spots to link to "+filteredSpots.getNSpots(frame+1));

		System.out.println();
		System.out.println("With feature condition:");
		trackerSettings.linkingFeaturePenalties.put(BlobMorphology.MORPHOLOGY, (double) 1);
		
		tracker = new LAPTracker<T>();
		tracker.setModel(model);
		
		System.out.println("For frame pair "+frame+" -> "+(frame+1)+":");
		System.out.println("There are "+filteredSpots.getNSpots(frame)+" spots to link to "+filteredSpots.getNSpots(frame+1));

		tracker.solveLAPForTrackSegments();
		model.setGraph(tracker.getResult());
		
		TrackMateModelView<T> view = new HyperStackDisplayer<T>();
		view.setModel(model);
		view.render();
		
	}

}
