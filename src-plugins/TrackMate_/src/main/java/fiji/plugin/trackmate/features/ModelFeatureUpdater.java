package fiji.plugin.trackmate.features;

import java.util.ArrayList;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.Model;

/**
 * A utility class that listens to the change occurring in a model, and updates
 * its spot, edge and track features accordingly. Useful to keep the model in 
 * sync with manual editing.
 *    
 * @author Jean-Yves Tinevez - 2013
 */
public class ModelFeatureUpdater implements ModelChangeListener {

	private final SpotFeatureCalculator spotFeatureCalculator;
	private final EdgeFeatureCalculator edgeFeatureCalculator;
	private final TrackFeatureCalculator trackFeatureCalculator;
	private final Model model;

	/**
	 * Constructs and activate a {@link ModelFeatureUpdater}. The new instance is 
	 * registered to listen to model changes, and update its feature.
	 * @param model  the model to listen to. 
	 * @param settings the {@link Settings} the model is built against. Required 
	 * to access the raw data.
	 */
	public ModelFeatureUpdater(Model model, Settings settings) {
		this.model = model;
		this.spotFeatureCalculator = new SpotFeatureCalculator(model, settings);
		this.edgeFeatureCalculator = new EdgeFeatureCalculator(model, settings);
		this.trackFeatureCalculator = new TrackFeatureCalculator(model, settings);
		model.addModelChangeListener(this);
	}

	/**
	 * Updates the model features against the change notified here.
	 * If the event is not a {@link ModelChangeEvent#MODEL_MODIFIED},
	 * does nothing.
	 */
	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (event.getEventID() != ModelChangeEvent.MODEL_MODIFIED) {
			return;
		}

		// Build spot list
		ArrayList<Spot> spots = new ArrayList<Spot>(event.getSpots().size());
		for (Spot spot : event.getSpots()) {
			if (event.getSpotFlag(spot) != ModelChangeEvent.FLAG_SPOT_REMOVED) {
				spots.add(spot);
			}
		}
		SpotCollection sc = SpotCollection.fromCollection(spots);
		
		// Build edge list
		ArrayList<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>(event.getEdges().size());
		for (DefaultWeightedEdge edge : event.getEdges()) {
			if (event.getEdgeFlag(edge) != ModelChangeEvent.FLAG_EDGE_REMOVED) {
				edges.add(edge);
			}
		}

		// Update spot features
		spotFeatureCalculator.computeSpotFeatures(sc, true);
		
		// Update edge features
		edgeFeatureCalculator.computeSpotFeatures(edges, true);
		
		// Update track features
		trackFeatureCalculator.computeTrackFeatures(event.getTrackUpdated(), true);
	}
	
	/**
	 * Re-registers this instance from the listeners of the model, and stop
	 * updating its features.
	 */
	public void quit() {
		model.removeModelChangeListener(this);
	}

}
