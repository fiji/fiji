package fiji.plugin.trackmate.visualization.trackscheme;

import org.jgraph.graph.DefaultGraphCell;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

public class SpotCell extends DefaultGraphCell {
	
	private static final long serialVersionUID = 1L;
	private Spot spot;
	
	public SpotCell(Spot spot) {
		this.spot = spot;
	}
	
	public Spot getSpot() {
		return spot;
	}
	
	@Override
	public String toString() {
		return "t = " + spot.getFeature(Feature.POSITION_T);
	}
}
