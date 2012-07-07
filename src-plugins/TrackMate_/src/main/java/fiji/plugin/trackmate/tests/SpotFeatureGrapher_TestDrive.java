package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.trackscheme.SpotFeatureGrapher;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class SpotFeatureGrapher_TestDrive {

	public static void main(String[] args) throws JDOMException, IOException {

		// Load objects 
		File file = new File("/Users/tinevez/Desktop/Data/Tree.xml");
		TmXmlReader reader = new TmXmlReader(file, Logger.DEFAULT_LOGGER);
		reader.parse();
		final TrackMateModel model = reader.getModel();
		List<Spot> spots = model.getFilteredSpots().getAllSpots();
		
		HashSet<String> Y = new HashSet<String>(1);
		Y.add(Spot.POSITION_T);
		SpotFeatureGrapher grapher = new SpotFeatureGrapher(Spot.POSITION_X, Y, spots, model);
		grapher.setVisible(true);
		
		TrackSchemeFrame trackScheme = new TrackSchemeFrame(model);
		trackScheme.setVisible(true);
		
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
			float[] coordinates = new float[3];
			coordinates[0] = (float) (100 + 100 * i / 100. * Math.cos(i / 100. * 5 * 2*Math.PI)); 
			coordinates[1] = (float) (100 + 100 * i / 100. * Math.sin(i / 100. * 5 * 2*Math.PI));
			coordinates[2] = 0;
			Spot spot = new SpotImp(coordinates);
			spot.putFeature(Spot.POSITION_T, i);
			spot.putFeature(Spot.RADIUS, 2);
			
			spots.add(spot);
			
			List<Spot> ts = new ArrayList<Spot>(1);
			ts.add(spot);
			sc.put(i, ts);
		}
		
		TrackMateModel model = new TrackMateModel();
		model.setSpots(sc, false);
		model.setFilteredSpots(sc, false);
		
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
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
		model.setGraph(graph);
		
		return model;
	}
}
