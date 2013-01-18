/**
 * 
 */
package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.HashMap;
import java.util.Set;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackGraphModel;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

/**
 * A {@link TrackColorGenerator} that generate colors based on the whole
 * track feature.
 * @author Jean-Yves Tinevez
 *
 */
public class PerTrackFeatureColorGenerator implements TrackColorGenerator, ModelChangeListener {

	/** Default color used when a feature value is missing. */
	private static final Color DEFAULT_COLOR = Color.GREEN;
	private static final InterpolatePaintScale generator = InterpolatePaintScale.Jet;
	private HashMap<Integer,Color> colorMap;
	private final TrackMateModel model;
	private String feature;
	private Integer trackID;

	public PerTrackFeatureColorGenerator(TrackMateModel model, String feature) {
		this.model = model;
		setFeature(feature);
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
	public void setFeature(String feature) {
		// Special case: if null, then all tracks should be green
		if (null == feature) {
			this.feature = null;
			refreshNull();
			return;
		}
		
		if (feature.equals(this.feature)) {
			return;
		}
		this.feature = feature;
		// A hack if we are asked for track index, which is the default and should never get caught to be null
		if (feature.equals(TrackIndexAnalyzer.TRACK_INDEX)) {
			refreshIndex();
		} else {
			refresh();
		}
	}

	private synchronized void refreshNull() {
		TrackGraphModel trackModel = model.getTrackModel();
		Set<Integer> trackIDs = trackModel.getFilteredTrackIDs();

		// Create value->color map
		colorMap = new HashMap<Integer, Color>(trackIDs.size());
		for (Integer trackID : trackIDs) {
			colorMap.put(trackID, DEFAULT_COLOR);
		}
	}

	/**
	 * A shortcut for the track index feature
	 */
	private synchronized void refreshIndex() {
		TrackGraphModel trackModel = model.getTrackModel();
		Set<Integer> trackIDs = trackModel.getFilteredTrackIDs();

		// Create value->color map
		colorMap = new HashMap<Integer, Color>(trackIDs.size());
		int index = 0;
		for (Integer trackID : trackIDs) {
			Color color = generator.getPaint( (double) index++ / trackIDs.size() );
			colorMap.put(trackID, color);
		}
	}

	private synchronized void refresh() {

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
	public void modelChanged(ModelChangeEvent event) {
		if (event.getEventID() ==  ModelChangeEvent.MODEL_MODIFIED) {
			Set<DefaultWeightedEdge> edges = event.getEdges();
			if (edges.size() > 0) {
				refresh();
			} 
		}		
	}

	@Override
	public Color color(Spot spot) {
		return colorMap.get(trackID);
	}

	@Override
	public Color color(DefaultWeightedEdge edge) {
		return colorMap.get(trackID);
	}

	@Override
	public synchronized void setCurrentTrackID(Integer trackID) {
		this.trackID = trackID;
	}

	@Override
	public void terminate() {
		model.removeTrackMateModelChangeListener(this);
	}
	
}
