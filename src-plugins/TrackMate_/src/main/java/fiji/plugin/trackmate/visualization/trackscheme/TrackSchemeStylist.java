package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_HEIGHT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_WIDTH;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_COLOR;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxStyleUtils;
import com.mxgraph.view.mxPerimeter;
import com.mxgraph.view.mxStylesheet;

import fiji.plugin.trackmate.visualization.TrackColorGenerator;

public class TrackSchemeStylist {

	private TrackColorGenerator colorGenerator;
	private final JGraphXAdapter graphx;
	private String globalStyle = DEFAULT_STYLE_NAME;

	static final Map<String, Map<String, Object>> VERTEX_STYLES;
	static final String 			DEFAULT_STYLE_NAME = "full";
	static final String 			SIMPLE_STYLE_NAME = "simple";

	private static final HashMap<String, Object> DEFAULT_VERTEX_STYLE = new HashMap<String, Object>();
	private static final HashMap<String, Object> SIMPLE_VERTEX_STYLE = new HashMap<String, Object>();
	private static final HashMap<String, Object> BASIC_EDGE_STYLE = new HashMap<String, Object>();
	static {
		DEFAULT_VERTEX_STYLE.put(mxConstants.STYLE_FILLCOLOR, "white");
		DEFAULT_VERTEX_STYLE.put(mxConstants.STYLE_FONTCOLOR, "black");
		DEFAULT_VERTEX_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_RIGHT);
		DEFAULT_VERTEX_STYLE.put(mxConstants.STYLE_SHAPE, mxScaledLabelShape.SHAPE_NAME);
		DEFAULT_VERTEX_STYLE.put(mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT);
		DEFAULT_VERTEX_STYLE.put(mxConstants.STYLE_ROUNDED, true);
		DEFAULT_VERTEX_STYLE.put(mxConstants.STYLE_PERIMETER, mxPerimeter.RectanglePerimeter);
		DEFAULT_VERTEX_STYLE.put(mxConstants.STYLE_STROKECOLOR, DEFAULT_COLOR);

		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_FILLCOLOR, "white");
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_FONTCOLOR, "black");
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_RIGHT);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_ROUNDED, true);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_PERIMETER, mxPerimeter.EllipsePerimeter);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_STROKECOLOR, DEFAULT_COLOR);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_NOLABEL, true);

		BASIC_EDGE_STYLE.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STARTARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKEWIDTH, 2.0f);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKECOLOR, DEFAULT_COLOR);

		VERTEX_STYLES = new HashMap<String, Map<String, Object> >(2);
		VERTEX_STYLES.put(DEFAULT_STYLE_NAME, DEFAULT_VERTEX_STYLE);
		VERTEX_STYLES.put(SIMPLE_STYLE_NAME, SIMPLE_VERTEX_STYLE);

	}


	public TrackSchemeStylist(JGraphXAdapter graphx, TrackColorGenerator colorGenerator) {
		this.graphx = graphx;
		this.colorGenerator = colorGenerator;

		// Prepare styles
		mxStylesheet styleSheet = graphx.getStylesheet();
		styleSheet.setDefaultEdgeStyle(BASIC_EDGE_STYLE);
		styleSheet.setDefaultVertexStyle(DEFAULT_VERTEX_STYLE);
		styleSheet.putCellStyle(DEFAULT_STYLE_NAME, DEFAULT_VERTEX_STYLE);
		styleSheet.putCellStyle(SIMPLE_STYLE_NAME, SIMPLE_VERTEX_STYLE);
	}

	public void setColorGenerator(TrackColorGenerator colorGenerator) {
		this.colorGenerator = colorGenerator;
	}

	public void setStyle(String styleName) {
		if (!graphx.getStylesheet().getStyles().containsKey(styleName)) {
			throw new IllegalArgumentException("Unknwon TrackScheme style: " + styleName);
		}
		this.globalStyle = styleName;
	}

	/**
	 * Change the style of the edge cells to reflect the currently set color generator.
	 * @param edgeMap the {@link mxCell} ordered by the track IDs they belong to.
	 */
	public synchronized Set<mxICell> execute(Map<Integer, Set<mxCell>> edgeMap) {

		HashSet<mxICell> verticesChanged = new HashSet<mxICell>(edgeMap.size());
		graphx.getModel().beginUpdate();
		try {

			for (Integer trackID : edgeMap.keySet()) {
				colorGenerator.setCurrentTrackID(trackID);

				Set<mxCell> edgesToUpdate = edgeMap.get(trackID);
				for (mxCell cell : edgesToUpdate) {

					// The edge itself
					DefaultWeightedEdge edge = graphx.getEdgeFor(cell);
					Color color = colorGenerator.color(edge);
					String colorstr;
					if (null == color) {
						colorstr = DEFAULT_COLOR;
					} else {
						colorstr = Integer.toHexString(color.getRGB()).substring(2);
					}
					String style = cell.getStyle();
					style = mxStyleUtils.setStyle(style , mxConstants.STYLE_STROKECOLOR, colorstr);
					graphx.getModel().setStyle(cell, style);

					// Its target
					mxICell target = cell.getTarget();
					verticesChanged.add(target);
					// Set its style
					setVertexStyleFromEdge(target, cell);

				}

			}
		} finally {
			graphx.getModel().endUpdate();
		}
		return verticesChanged;
	}

	public void updateVertexStyle(Collection<mxCell> vertices) {

		for (mxCell vertex : vertices) {

			int nedges = vertex.getEdgeCount();
			if (nedges == 0) {
				continue;
			}
			mxICell edge;
			for (int i = 0; i < vertex.getEdgeCount(); i++) {
				edge = vertex.getEdgeAt(i);
				if (null != edge.getStyle()) {
					setVertexStyleFromEdge(vertex, edge);
					break;
				}
			}

		}
	}

	private final void setVertexStyleFromEdge(final mxICell vertex, final mxICell edge) {
		String targetStyle = vertex.getStyle();
		targetStyle = mxStyleUtils.removeAllStylenames(targetStyle);
		String colorstr = getStyleValue(edge.getStyle(), mxConstants.STYLE_STROKECOLOR);
		targetStyle = mxStyleUtils.setStyle(targetStyle , mxConstants.STYLE_STROKECOLOR, colorstr );

		// Style specifics
		int width, height;
		if (globalStyle.equals(SIMPLE_STYLE_NAME)) {
			targetStyle = mxStyleUtils.setStyle(targetStyle, mxConstants.STYLE_FILLCOLOR, colorstr);
			width = DEFAULT_CELL_HEIGHT;
			height = width;
		} else {
			targetStyle = mxStyleUtils.setStyle(targetStyle, mxConstants.STYLE_FILLCOLOR, "white");
			width = DEFAULT_CELL_WIDTH;
			height = DEFAULT_CELL_HEIGHT;
		}
		targetStyle = globalStyle + ";" + targetStyle;

		graphx.getModel().setStyle(vertex, targetStyle);
		graphx.getModel().getGeometry(vertex).setWidth(width);
		graphx.getModel().getGeometry(vertex).setHeight(height);
	}


	private static final String getStyleValue(String style, String key) {
		int index = style.indexOf(key + "=");

		if (index < 0) {
			return "";
		} else {
			int start = style.indexOf("=", index) + 1;
			int cont = style.indexOf(";", start);
			if (cont < 0) {
				cont = style.length();
			}
			return style.substring(start, cont);
		}
	}

}
