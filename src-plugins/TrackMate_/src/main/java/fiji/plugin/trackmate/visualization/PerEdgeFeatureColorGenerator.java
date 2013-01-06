package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.Set;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;

public class PerEdgeFeatureColorGenerator implements ModelChangeListener, TrackColorGenerator {

	private static final InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	private static final Color DEFAULT_COLOR = Color.WHITE;
	private final TrackMateModel model;
	private String feature;
	private double min;
	private double max;
	private DefaultWeightedEdge edgeMin;
	private DefaultWeightedEdge edgeMax;

	public PerEdgeFeatureColorGenerator(final TrackMateModel model, String feature) {
		this.model = model;
		model.addTrackMateModelChangeListener(this);
		setFeature(feature);
	}

	public void setFeature(String feature) {
		if (feature.equals(this.feature)) {
			return;
		}
		this.feature = feature;
		resetMinAndMax();
	}
	
	@Override
	public Color color(Spot spot) {
		Set<DefaultWeightedEdge> edges = model.getTrackModel().edgesOf(spot);
		if (edges.isEmpty()) {
			return DEFAULT_COLOR;
		} else {
			return color(edges.iterator().next());
		}
	}

	@Override
	public Color color(DefaultWeightedEdge edge) {
		double val = model.getFeatureModel().getEdgeFeature(edge, feature).doubleValue();
		return colorMap.getPaint( (val-min)/(max-min) );
	}
	
	@Override
	public void setCurrentTrackID(Integer trackID) { } // ignored

	/**
	 *  Monitor if the change induces some change in the colormap. Rescale it if so.
	 */
	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (event.getEventID() != ModelChangeEvent.MODEL_MODIFIED || event.getEdges().size() == 0) {
			return;
		}

		for (DefaultWeightedEdge edge : event.getEdges()) {
			
			if (event.getEdgeFlag(edge) == ModelChangeEvent.FLAG_EDGE_ADDED 
					|| event.getEdgeFlag(edge) == ModelChangeEvent.FLAG_EDGE_MODIFIED) {
				
				if (edge.equals(edgeMax) || edge.equals(edgeMin)) {
					resetMinAndMax();
					return;
				}
				
				double val = model.getFeatureModel().getEdgeFeature(edge, feature).doubleValue();
				if (val > max || val < min) {
					resetMinAndMax();
					return;
				}
				
			} else if (event.getEdgeFlag(edge) == ModelChangeEvent.FLAG_EDGE_REMOVED) {
				
				if (edge.equals(edgeMax) || edge.equals(edgeMin)) {
					resetMinAndMax();
					return;
				}
				
			}
		}
	}


	private void resetMinAndMax() {
		min = Double.POSITIVE_INFINITY;
		max = Double.NEGATIVE_INFINITY;
		// Only iterate over filtered edges
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
			for (DefaultWeightedEdge edge : model.getTrackModel().getTrackEdges(trackID)) {
				Double feat = model.getFeatureModel().getEdgeFeature(edge, feature);
//				if (null == feat) continue;
				double val = feat.doubleValue();
				if (val < min) {
					min = val;
					edgeMin = edge;
				}
				if (val > max) {
					max = val;
					edgeMax = edge;
				}
			}
		}
	}
	
	@Override
	public void terminate() {
		model.removeTrackMateModelChangeListener(this);
	}

}
