package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.Color;

import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.graph.DefaultWeightedEdge;

public class TrackEdgeView extends EdgeView {
	
	private static final long serialVersionUID = 8946025342509621517L;
	private static final Color DEFAULT_COLOR = Color.MAGENTA;

	@SuppressWarnings("unchecked")
	public TrackEdgeView(TrackEdgeCell edgeCell) {
		super(edgeCell);
		attributes.put(GraphConstants.FONT, SMALL_FONT);
		attributes.put(GraphConstants.FOREGROUND, Color.BLACK);
		attributes.put(GraphConstants.LABELALONGEDGE, true);
		attributes.put(GraphConstants.LINEWIDTH, 2f);
		attributes.put(GraphConstants.LINECOLOR, DEFAULT_COLOR);
	}
	
	public DefaultWeightedEdge getEdge() {
		return ((TrackEdgeCell)cell).getEdge();
	}
	
}
