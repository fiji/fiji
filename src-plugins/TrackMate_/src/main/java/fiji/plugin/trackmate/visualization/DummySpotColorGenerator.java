package fiji.plugin.trackmate.visualization;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_SPOT_COLOR;

import java.awt.Color;

import fiji.plugin.trackmate.Spot;

/**
 * A dummy spot color generator that always return the default color.
 * @author Jean-Yves Tinevez - 2013
 */
public class DummySpotColorGenerator implements FeatureColorGenerator<Spot> {
	
	@Override
	public Color color(Spot obj) {
		return DEFAULT_SPOT_COLOR;
	}

	@Override
	public void setFeature(String feature) {}

	@Override
	public void terminate() {}

}
