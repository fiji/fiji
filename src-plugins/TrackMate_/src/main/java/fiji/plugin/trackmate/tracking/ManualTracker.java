package fiji.plugin.trackmate.tracking;

import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;


public class ManualTracker implements SpotTracker {

	public static final String TRACKER_KEY = "MANUAL_TRACKER";
	public static final String NAME = "Manual tracking";
	public static final String INFO_TEXT =  "<html>" +
			"Choosing this tracker skips the automated tracking step <br>" +
			"and keeps the current annotation.</html>";


	@Override
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getResult() {
		return null;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public String getKey() {
		return TRACKER_KEY;
	}

	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public void setTarget(final SpotCollection spots, final Map<String, Object> settings) {}

}
