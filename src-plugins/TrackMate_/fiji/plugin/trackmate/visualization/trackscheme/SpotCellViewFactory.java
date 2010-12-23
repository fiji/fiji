package fiji.plugin.trackmate.visualization.trackscheme;

import org.jgraph.graph.DefaultCellViewFactory;
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
	
	
}
