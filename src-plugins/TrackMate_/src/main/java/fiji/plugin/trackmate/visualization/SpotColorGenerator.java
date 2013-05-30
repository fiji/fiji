package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Model;

public class SpotColorGenerator implements FeatureColorGenerator<Spot>, ModelChangeListener {

	private final Map<Spot, Color> spotColorMap = new HashMap<Spot, Color>();
	private final Model model;
	private String feature = null;

	public SpotColorGenerator(Model model) {
		this.model = model;
		model.addModelChangeListener(this);
	}
	
	@Override
	public Color color(Spot spot) {
		return spotColorMap.get(spot);
	}

	@Override
	public void terminate() {
		model.removeModelChangeListener(this);
	}
	

	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (event.getEventID() ==  ModelChangeEvent.MODEL_MODIFIED) {
			Set<Spot> spots = event.getSpots();
			if (spots.size() > 0) {
				computeSpotColors(feature);
			} 
		} else if (event.getEventID() == ModelChangeEvent.SPOTS_COMPUTED) {
			computeSpotColors(feature);
		}
	}

	/**
	 * Sets the feature that will be used to color spots.
	 * <code>null</code> is accepted; it will color all the spot with the 
	 * same default color.
	 * @param feature the feature to color spots with.
	 */
	@Override
	public void setFeature(String feature) {
		if (null != feature && feature.equals(this.feature)) {
			return;
		}
		this.feature = feature;
		computeSpotColors(feature);
	}

	
	/*
	 * PRIVATE METHODS
	 */
	

	private void computeSpotColors(final String feature) {
		spotColorMap.clear();
		// Check null
		if (null == feature) {
			for(Spot spot : model.getSpots().iterable(false)) {
				spotColorMap.put(spot, TrackMateModelView.DEFAULT_COLOR);
			}
			return;
		}

		// Get min & max
		double min = Float.POSITIVE_INFINITY;
		double max = Float.NEGATIVE_INFINITY;
		Double val;
		for (int ikey : model.getSpots().keySet()) {
			for (Spot spot : model.getSpots().iterable(ikey, false)) {
				val = spot.getFeature(feature);
				if (null == val)
					continue;
				if (val > max) max = val;
				if (val < min) min = val;
			}
		}

		for(Spot spot : model.getSpots().iterable(false)) {
			val = spot.getFeature(feature);
			InterpolatePaintScale  colorMap = InterpolatePaintScale.Jet;
			if (null == feature || null == val)
				spotColorMap.put(spot, TrackMateModelView.DEFAULT_COLOR);
			else
				spotColorMap.put(spot, colorMap .getPaint((val-min)/(max-min)) );
		}
	}
}
