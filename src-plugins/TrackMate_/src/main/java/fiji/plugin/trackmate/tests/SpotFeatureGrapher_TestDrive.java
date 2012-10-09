package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.trackscheme.SpotFeatureGrapher;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class SpotFeatureGrapher_TestDrive {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws JDOMException, IOException {

		// Load objects 
		File file = new File("/Users/tinevez/Desktop/Data/Tree.xml");
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();
		TmXmlReader<T> reader = new TmXmlReader<T>(file, plugin, Logger.DEFAULT_LOGGER);
		reader.parse();
		final TrackMateModel<T> model = reader.getModel();
		List<Spot> spots = model.getFilteredSpots().getAllSpots();
		
		HashSet<String> Y = new HashSet<String>(1);
		Y.add(Spot.POSITION_T);
		SpotFeatureGrapher<T> grapher = new SpotFeatureGrapher<T>(Spot.POSITION_X, Y, spots, model);
		grapher.setVisible(true);
		
		TrackScheme<T> trackScheme = new TrackScheme<T>(model);
		trackScheme.render();
		
	}
	
	/**
	 *  Another example: spots that go in spiral
	 */
	@SuppressWarnings("unused")
	private static <T extends RealType<T> & NativeType<T>> TrackMateModel<T> getSpiralModel() {
		
		final int N_SPOTS = 50;
		List<Spot> spots = new ArrayList<Spot>(N_SPOTS);
		SpotCollection sc = new SpotCollection();
		for (int i = 0; i < N_SPOTS; i++) {
			double[] coordinates = new double[3];
			coordinates[0] = 100 + 100 * i / 100. * Math.cos(i / 100. * 5 * 2*Math.PI); 
			coordinates[1] = 100 + 100 * i / 100. * Math.sin(i / 100. * 5 * 2*Math.PI);
			coordinates[2] = 0;
			Spot spot = new SpotImp(coordinates);
			spot.putFeature(Spot.POSITION_T, i);
			spot.putFeature(Spot.RADIUS, 2);
			
			spots.add(spot);
			
			List<Spot> ts = new ArrayList<Spot>(1);
			ts.add(spot);
			sc.put(i, ts);
		}
		
		TrackMateModel<T> model = new TrackMateModel<T>();
		model.setSpots(sc, false);
		model.setFilteredSpots(sc, false);
		
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
		model.setGraph(graph);
		
		return model;
	}
}
