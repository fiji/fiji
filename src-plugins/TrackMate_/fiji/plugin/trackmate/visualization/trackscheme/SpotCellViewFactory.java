package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.Color;

import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.VertexView;

public class SpotCellViewFactory extends DefaultCellViewFactory {

	private static final long serialVersionUID = 1L;


	/*
	 * CONSTRUCTORS
	 */

	public SpotCellViewFactory() {}

	
	/*
	 * METHODS
	 */
	
	@Override
	protected VertexView createVertexView(Object cell) {
		SpotCell spotCell = (SpotCell) cell;
		return new SpotView(spotCell);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected EdgeView createEdgeView(Object cell) {
		EdgeView eView = super.createEdgeView(cell);
		eView.getAttributes().put(GraphConstants.FONT, SMALL_FONT);
		eView.getAttributes().put(GraphConstants.FOREGROUND, Color.BLACK);
		eView.getAttributes().put(GraphConstants.LABELALONGEDGE, true);
		eView.getAttributes().put(GraphConstants.LINEWIDTH, 2f);
		return eView;
	}
	
}
