package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxStyleUtils;

import fiji.plugin.trackmate.visualization.TrackColorGenerator;

public class TrackSchemeStylist {

	private TrackColorGenerator colorGenerator;
	private final JGraphXAdapter graphx;

	public TrackSchemeStylist(TrackScheme trackScheme, TrackColorGenerator colorGenerator) {
		this.graphx = trackScheme.getGraph();
		this.colorGenerator = colorGenerator;
	}

	public void setColorGenerator(TrackColorGenerator colorGenerator) {
		this.colorGenerator = colorGenerator;
	}

	/**
	 * Change the style of the edge cells to reflect the currently set color generator.
	 * @param edgeMap the {@link mxCell} ordered by the track IDs they belong to.
	 */
	public void execute(Map<Integer, Set<mxCell>> edgeMap) {
		graphx.getModel().beginUpdate();
		try {

			for (Integer trackID : edgeMap.keySet()) {
				colorGenerator.setCurrentTrackID(trackID);
				
				Set<mxCell> edgesToUpdate = edgeMap.get(trackID);
				for (mxCell cell : edgesToUpdate) {

					// The edge itself
					DefaultWeightedEdge edge = graphx.getEdgeFor(cell);
					Color color = colorGenerator.color(edge);
					String colorstr = Integer.toHexString(color.getRGB()).substring(2);
					String style = cell.getStyle();
					style = mxStyleUtils.setStyle(style , mxConstants.STYLE_STROKECOLOR, colorstr);
					cell.setStyle(style);

					// Its target
					mxICell target = cell.getTarget();
					String targetStyle = cell.getStyle();
					targetStyle = mxStyleUtils.setStyle(targetStyle , mxConstants.STYLE_STROKECOLOR, colorstr);
					target.setStyle(targetStyle);
				}
			}
		} finally {
			graphx.getModel().endUpdate();
		}
	}

}
