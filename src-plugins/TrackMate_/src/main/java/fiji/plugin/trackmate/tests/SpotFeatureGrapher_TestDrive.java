package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jdom2.JDOMException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class SpotFeatureGrapher_TestDrive {

	public static void main(String[] args) throws JDOMException, IOException {

		// Load objects 
		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = reader.getModel();

		HashSet<String> Y = new HashSet<String>(1);
		Y.add(Spot.POSITION_T);
		List<Spot> spots = new ArrayList<Spot>(model.getSpots().getNSpots(true));
		for (Iterator<Spot> it = model.getSpots().iterator(true); it.hasNext();) {
			spots.add(it.next());
		}
		
		SpotFeatureGrapher grapher = new SpotFeatureGrapher(Spot.POSITION_X, Y, spots , model);
		grapher.render();
		
		TrackIndexAnalyzer analyzer = new TrackIndexAnalyzer(model);
		analyzer.process(model.getTrackModel().trackIDs(true)); // needed for trackScheme
		
		TrackScheme trackScheme = new TrackScheme(model, new SelectionModel(model));
		trackScheme.render();
		
	}
	
	/**
	 *  Another example: spots that go in spiral
	 */
	@SuppressWarnings("unused")
	private static TrackMateModel getSpiralModel() {
		
		final int N_SPOTS = 50;
		List<Spot> spots = new ArrayList<Spot>(N_SPOTS);
		SpotCollection sc = new SpotCollection();
		for (int i = 0; i < N_SPOTS; i++) {
			double[] coordinates = new double[3];
			coordinates[0] = 100 + 100 * i / 100. * Math.cos(i / 100. * 5 * 2*Math.PI); 
			coordinates[1] = 100 + 100 * i / 100. * Math.sin(i / 100. * 5 * 2*Math.PI);
			coordinates[2] = 0;
			Spot spot = new Spot(coordinates);
			spot.putFeature(Spot.POSITION_T, Double.valueOf(i));
			spot.putFeature(Spot.RADIUS, Double.valueOf(2));
			
			spots.add(spot);
			
			List<Spot> ts = new ArrayList<Spot>(1);
			ts.add(spot);
			sc.put(i, ts);
			spot.putFeature(SpotCollection.VISIBLITY, SpotCollection.ONE);
		}
		
		TrackMateModel model = new TrackMateModel();
		model.setSpots(sc, false);
		
		SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		for (Spot spot : spots) {
			graph.addVertex(spot);
		}
		Spot source = spots.get(0);
		for (int i = 1; i < N_SPOTS; i++) {
			Spot target = spots.get(i);
			DefaultWeightedEdge edge = graph.addEdge(source, target);
			graph.setEdgeWeight(edge, 1);
			source = target;
		}
		model.getTrackModel().setGraph(graph);
		
		return model;
	}
}
