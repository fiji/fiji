/**
 * 
 */
package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackGraphModel;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateModelChangeListener;
import fiji.plugin.trackmate.features.FeatureModel;

/**
 * A {@link TrackPartsColorGenerator} that generate colors based on the whole
 * track feature.
 * @author Jean-Yves Tinevez
 *
 */
public class TrackFeatureColorGenerator implements TrackPartsColorGenerator, TrackMateModelChangeListener {

	/** Default color used when a feature value is missing. */
	private static final Color DEFAULT_COLOR = Color.WHITE;
	private HashMap<Integer,Color> colorMap;
	private final TrackMateModel model;
	private String feature;

	public TrackFeatureColorGenerator(TrackMateModel model) {
		this.model = model;
		model.addTrackMateModelChangeListener(this);
	}

	/**
	 * Set the track feature to set the color with. 
	 * <p>
	 * First, the track features are <b>re-calculated</b> for the target
	 * feature values to be accurate. We rely on the {@link #model} instance 
	 * for that. Then colors are calculated for all
	 * tracks when this method is called, and cached. 
	 * @param feature  the track feature that will control coloring.
	 * @throws IllegalArgumentException if the specified feature is unknown to the feature model.
	 */
	public void selectFeature(String feature) {
		this.feature = feature;
		refresh();
	}

	private void refresh() {

		TrackGraphModel trackModel = model.getTrackModel();
		Set<Integer> trackIDs = trackModel.getFilteredTrackIDs();


		// Get min & max & all values
		FeatureModel fm = model .getFeatureModel();
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		HashMap<Integer, Double> values = new HashMap<Integer, Double>(trackIDs.size());
		for (Integer trackID : trackIDs) {
			Double val = fm.getTrackFeature(trackID, feature);
			values.put(trackID, val);
			if (val < min) {
				min = val;
			}
			if (val > max) {
				max = val;
			}
		}

		// Get color scale
		InterpolatePaintScale generator = InterpolatePaintScale.Jet;

		// Create value->color map
		colorMap = new HashMap<Integer, Color>(trackIDs.size());
		for (Integer trackID : values.keySet()) {
			Double val = values.get(trackID);
			Color color;
			if (null == val) {
				color = DEFAULT_COLOR;
			} else {
				color = generator.getPaint( (val-min)/(max-min) );
			}
			colorMap.put(trackID, color);
		}
	}


	@Override
	public Color color(Spot spot, Integer trackID) {
		return colorMap.get(trackID);
	}

	@Override
	public Color color(DefaultWeightedEdge edge, Integer trackID) {
		return colorMap.get(trackID);
	}

	@Override
	public void modelChanged(TrackMateModelChangeEvent event) {
		if (event.getEventID() ==  TrackMateModelChangeEvent.MODEL_MODIFIED) {
			List<DefaultWeightedEdge> edges = event.getEdges();
			if (edges  != null && edges.size() > 0) {
				refresh();
			} 
		}		
	}

}
