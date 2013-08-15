package fiji.plugin.trackmate.visualization;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_COLOR;
import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel;

/**
 * A dummy track color generator that always return the default color.
 * @author Jean-Yves Tinevez - 2013
 */
public class DummyTrackColorGenerator implements TrackColorGenerator {

	@Override
	public Color color(DefaultWeightedEdge obj) {
		return DEFAULT_TRACK_COLOR;
	}

	@Override
	public void setFeature(String feature) {}

	@Override
	public void terminate() {}

	@Override
	public void setCurrentTrackID(Integer trackID) {}

	@Override
	public String getFeature() {
		return ColorByFeatureGUIPanel.UNIFORM_KEY;
	}

}
