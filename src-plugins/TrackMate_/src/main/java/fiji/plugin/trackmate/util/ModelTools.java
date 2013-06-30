package fiji.plugin.trackmate.util;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;

/**
 * A collection of static utilities made to ease the manipulation of a 
 * TrackMate {@link Model} and {@link SelectionModel}.
 * @author Jean-Yves Tinevez - 2013
 *
 */
public class ModelTools {

	private ModelTools() { }


	/**
	 * Sets the content of the specified selection model to be the whole tracks 
	 * the selected spots belong to. Other selected edges are removed from the 
	 * selection. 
	 * @param selectionModel  the {@link SelectionModel} that will be updated by this
	 * call.
	 */
	public static void selectTrack(SelectionModel selectionModel) {
		selectionModel.clearEdgeSelection();
		selectionModel.selectTrack(selectionModel.getSpotSelection(), Collections.<DefaultWeightedEdge> emptyList(), 0);
	}

	/**
	 * Sets the content of the specified selection model to be the whole tracks 
	 * the selected spots belong to, but searched for only forward in time (downward). 
	 * Other selected edges are removed from the selection. 
	 * @param selectionModel  the {@link SelectionModel} that will be updated by this
	 * call.
	 */
	public static void selectTrackDownward(SelectionModel selectionModel) {
		selectionModel.clearEdgeSelection();
		selectionModel.selectTrack(selectionModel.getSpotSelection(), Collections.<DefaultWeightedEdge> emptyList(), -1);
	}

	/**
	 * Sets the content of the specified selection model to be the whole tracks 
	 * the selected spots belong to, but searched for only backward in time (backward). 
	 * Other selected edges are removed from the selection. 
	 * @param selectionModel  the {@link SelectionModel} that will be updated by this
	 * call.
	 */
	public static void selectTrackUpward(SelectionModel selectionModel) {
		selectionModel.clearEdgeSelection();
		selectionModel.selectTrack(selectionModel.getSpotSelection(), Collections.<DefaultWeightedEdge> emptyList(), 1);
	}

	/**
	 * Links all the spots in the selection, in time-forward order.
	 * @param model  the model to modify.
	 * @param selectionModel  the selection that contains the spots to link.
	 */
	public static void linkSpots(Model model, SelectionModel selectionModel) {
		
		/*
		 * Configure tracker
		 */
		
		SpotCollection spots = SpotCollection.fromCollection(selectionModel.getSpotSelection());
		NearestNeighborTracker tracker = new NearestNeighborTracker(spots);
		tracker.setNumThreads(1);
		Map<String, Object> settings = new HashMap<String, Object>(1);
		settings.put(KEY_LINKING_MAX_DISTANCE, Double.POSITIVE_INFINITY);
		tracker.setSettings(settings);
		
		/*
		 * Execute tracking
		 */
		
		if (!tracker.checkInput() || !tracker.process()) {
			System.err.println("Problem while computing spot links: " + tracker.getErrorMessage());
			return;
		}
		SimpleWeightedGraph<Spot,DefaultWeightedEdge> graph = tracker.getResult();
		
		/*
		 * Copy found links in source model
		 */
		
		model.beginUpdate();
		try {
			for (DefaultWeightedEdge edge : graph.edgeSet()) {
				Spot source = graph.getEdgeSource(edge);
				Spot target = graph.getEdgeTarget(edge);
				model.addEdge(source, target, graph.getEdgeWeight(edge));
			}
		} finally {
			model.endUpdate();
		}	
	}

}
