/**
 * 
 */
package fiji.plugin.trackmate.visualization;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_COLOR;

import java.awt.Color;
import java.util.HashMap;
import java.util.Set;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

/**
 * A {@link TrackColorGenerator} that generate colors based on the whole
 * track feature.
 * @author Jean-Yves Tinevez
 *
 */
public class PerTrackFeatureColorGenerator implements TrackColorGenerator, ModelChangeListener {

	private static final InterpolatePaintScale generator = InterpolatePaintScale.Jet;
	private HashMap<Integer,Color> colorMap;
	private final Model model;
	private String feature;
	private Integer trackID;

	public PerTrackFeatureColorGenerator(Model model, String feature) {
		this.model = model;
		model.addModelChangeListener(this);
		setFeature(feature);
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
	@Override
	public void setFeature(String feature) {
		// Special case: if null, then all tracks should be green
		if (null == feature) {
			this.feature = null;
			refreshNull();
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
		TrackModel trackModel = model.getTrackModel();
		Set<Integer> trackIDs = trackModel.trackIDs(true);

		// Create value->color map
		colorMap = new HashMap<Integer, Color>(trackIDs.size());
		for (Integer trackID : trackIDs) {
			colorMap.put(trackID, DEFAULT_TRACK_COLOR);
		}
	}

	/**
	 * A shortcut for the track index feature
	 */
	private synchronized void refreshIndex() {
		TrackModel trackModel = model.getTrackModel();
		Set<Integer> trackIDs = trackModel.trackIDs(true);

		// Create value->color map
		colorMap = new HashMap<Integer, Color>(trackIDs.size());
		int index = 0;
		for (Integer trackID : trackIDs) {
			Color color = generator.getPaint( (double) index++ / (trackIDs.size()-1) );
			colorMap.put(trackID, color);
		}
	}

	private synchronized void refresh() {
		TrackModel trackModel = model.getTrackModel();
		Set<Integer> trackIDs = trackModel.trackIDs(true);

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
				color = DEFAULT_TRACK_COLOR;
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
				if (null == feature) {
					refreshNull();
				} else if (feature.equals(TrackIndexAnalyzer.TRACK_INDEX)) {
					refreshIndex();
				} else {
					refresh();
				}
			} 
		}
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
		model.removeModelChangeListener(this);
	}
	
}
