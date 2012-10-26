package fiji.plugin.trackmate.tests;

import java.io.File;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v12;

public class TmXmlReaderV12TestDrive {

	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks_v12.xml");

	public static <T extends RealType<T> & NativeType<T>> void main(String args[]) {

		//		ij.ImageJ.main(args);

		System.out.println("Opening file: "+file.getAbsolutePath());		
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();
		TmXmlReader<T> reader = new TmXmlReader_v12<T>(file, plugin , Logger.DEFAULT_LOGGER);
		// Parse
		reader.parse();
		TrackMateModel<T> model = reader.getModel();

		System.out.println(model.getSettings());

		System.out.println();
		System.out.println("Detector was: "+model.getSettings().detectorFactory.toString());
		System.out.println("With settings:");
		System.out.println(model.getSettings().detectorSettings);
		
		System.out.println();
		System.out.println("Found "+model.getSettings().getSpotFilters().size()+" spot feature filters:");
		for (FeatureFilter filter : model.getSettings().getSpotFilters()) {
			System.out.println(" - "+filter);
		}

		System.out.println();
		System.out.println("Tracker was: "+model.getSettings().tracker.toString());
		System.out.println("With settings:");
		System.out.println(model.getSettings().trackerSettings);
		System.out.println();
		System.out.println("Found "+model.getSpots().getNSpots()+" spots in total.");
		System.out.println("Found "+model.getFilteredSpots().getNSpots()+" filtered spots.");
		System.out.println("Found "+model.getNTracks()+" tracks in total.");
		System.out.println("Found "+model.getNFilteredTracks()+" filtered tracks.");

		System.out.println();
		System.out.println("Track features:");
		System.out.println(model.getFeatureModel().getTrackFeatureValues());

		
		System.out.println();
		System.out.println();
		System.out.println("Second pass:");
		System.out.println();
		
		reader = new TmXmlReader_v12<T>(file, plugin , Logger.DEFAULT_LOGGER);
		// Parse
		reader.parse();
		Settings<T> settings = reader.getSettings();
		model.setSettings(settings);
		reader.getDetectorSettings(settings);
		SpotCollection spots = reader.getAllSpots();
		model.setSpots(spots, false);
		FeatureFilter initialThreshold = reader.getInitialFilter();
		model.getSettings().initialSpotFilterValue = initialThreshold.value;
		List<FeatureFilter> featureThresholds = reader.getSpotFeatureFilters();
		model.getSettings().setSpotFilters(featureThresholds);
		SpotCollection selectedSpots = reader.getFilteredSpots();
		model.setFilteredSpots(selectedSpots, false);
		reader.getTrackerSettings(settings);
		SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = reader.readTrackGraph();
		model.setGraph(graph);
		model.setTrackSpots(reader.readTrackSpots(graph));
		model.setTrackEdges(reader.readTrackEdges(graph));
		reader.readTrackFeatures(model.getFeatureModel());
		model.getSettings().setTrackFilters(reader.getTrackFeatureFilters());
		model.setVisibleTrackIndices(reader.getFilteredTracks(), false);
		

		System.out.println(model.getSettings());

		System.out.println();
		System.out.println("Detector was: "+model.getSettings().detectorFactory.toString());
		System.out.println("With settings:");
		System.out.println(model.getSettings().detectorSettings);

		System.out.println();
		System.out.println("Tracker was: "+model.getSettings().tracker.toString());
		System.out.println("With settings:");
		System.out.println(model.getSettings().trackerSettings);
		System.out.println();
		System.out.println("Found "+model.getSpots().getNSpots()+" spots in total.");
		System.out.println("Found "+model.getFilteredSpots().getNSpots()+" filtered spots.");
		System.out.println("Found "+model.getNTracks()+" tracks in total.");
		System.out.println("Found "+model.getNFilteredTracks()+" filtered tracks.");

		System.out.println();
		System.out.println("Track features:");
		System.out.println(model.getFeatureModel().getTrackFeatureValues());

	}


}
