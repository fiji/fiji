package fiji.plugin.trackmate.tracking.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import mpicbg.imglib.util.Util;

import org.jdom.JDOMException;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.TrackerType;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
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
		settings.allowMerging = false;
		settings.allowSplitting = true;
		settings.splittingDistanceCutoff = 5;
		settings.splittingTimeCutoff = 2;
		settings.splittingFeatureCutoffs.clear();
		System.out.println("Tracker settings:");
		System.out.println(settings.toString());
		model.getSettings().trackerSettings = settings;
		
		// 2 - Track the test spots
		LAPTracker lap;
		lap = new LAPTracker(model.getFilteredSpots(), model.getSettings().trackerSettings);
		lap.setLogger(Logger.DEFAULT_LOGGER);
		if (!lap.checkInput())
			System.err.println("Error checking input: "+lap.getErrorMessage());
		if (!lap.process())
			System.err.println("Error in process: "+lap.getErrorMessage());
		
		// Print out track segments
		List<SortedSet<Spot>> trackSegments = lap.getTrackSegments();
		System.out.println("Found "+trackSegments.size()+" track segments:");
		for (SortedSet<Spot> trackSegment : trackSegments) {
			System.out.println("\n-*-*-*-*-* New Segment *-*-*-*-*-");
			for (Spot spot : trackSegment)
				System.out.println(Util.printCoordinates(spot.getPosition(null)) + ", Frame [" + spot.getFeature(Feature.POSITION_T) + "]");	
		}


		// 3 - Print out results for testing		
		System.out.println();
		System.out.println();
		System.out.println();
		SimpleWeightedGraph<Spot,DefaultWeightedEdge> graph = lap.getTrackGraph();
		ConnectivityInspector<Spot, DefaultWeightedEdge> inspector = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph);
		List<Set<Spot>> tracks = inspector.connectedSets();
		System.out.println("Found " + tracks.size() + " final tracks:");
		System.out.println();
		int counter = 0;
		for(Set<Spot> track : tracks) {
			System.out.println("Track "+counter);
			System.out.print("Spots in frames: \n");
			for(Spot spot : track)
				System.out.println(Util.printCoordinates(spot.getPosition(null)) + ", Frame [" + spot.getFeature(Feature.POSITION_T) + "]");
			System.out.println();
			counter++;
		}
		
		// 4 - Detailed info
//		System.out.println("Segment costs:");
//		LAPUtils.echoMatrix(lap.getSegmentCosts());
		
//		System.out.println();
//		System.out.println("Fragment costs for 1st frame:");
//		LAPUtils.echoMatrix(lap.getLinkingCosts().get(0));
		
		// 5 - Display tracks
		// Load Image
		ij.ImageJ.main(args);
		
		AbstractTrackMateModelView sd2d = new HyperStackDisplayer(model);
		sd2d.render();
//		sd2d.setSpots(model.getSpots());
//		sd2d.setSpotsToShow(model.getFilteredSpots());
//		sd2d.setTrackGraph(graph);
		sd2d.setDisplayTrackMode(AbstractTrackMateModelView.TrackDisplayMode.ALL_WHOLE_TRACKS, 1);
	}

}
