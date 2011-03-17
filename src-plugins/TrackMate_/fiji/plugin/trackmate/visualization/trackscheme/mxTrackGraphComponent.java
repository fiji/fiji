package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

public class mxTrackGraphComponent extends mxGraphComponent implements mxIEventListener {
	
	private static final long serialVersionUID = -281620557095353617L;
	private static final Color BACKGROUND_COLOR_1 	= Color.GRAY;
	private static final Color BACKGROUND_COLOR_2 	= Color.LIGHT_GRAY;
	private static final Color LINE_COLOR 			= Color.BLACK;
	
	private TreeSet<Float> instants;
	private TreeMap<Float, Integer> rows;
	private int[] columnWidths = null;
	private Color[] columnColors;
	private TrackSchemeFrame frame;

	public mxTrackGraphComponent(TrackSchemeFrame frame) {
		super(frame.getGraph());
		this.frame = frame;
		getViewport().setOpaque(true);
		getViewport().setBackground(BACKGROUND_COLOR_1);
		setZoomFactor(2.0);

		instants = new TreeSet<Float>();
		for (Spot s : frame.trackGraph.vertexSet())
			instants.add(s.getFeature(Feature.POSITION_T));
		
		connectionHandler.addListener(mxEvent.CONNECT, this);
		
		mxGraphics2DCanvas.putShape(mxScaledLabelShape.SHAPE_NAME, new mxScaledLabelShape());
	}

	@Override
	public void paintBackground(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Rectangle paintBounds = g.getClipBounds();
		
		int width = getViewport().getView().getSize().width;
		int height = getViewport().getView().getSize().height;
		float scale = (float) graph.getView().getScale();

		// Scaled sizes
		int xcs 			= Math.round(TrackSchemeFrame.X_COLUMN_SIZE*scale);
		int ycs 			= Math.round(TrackSchemeFrame.Y_COLUMN_SIZE*scale);

		// Alternating row color
		g.setColor(BACKGROUND_COLOR_2);
		int y = 0;
		while (y < height) {
			if (y > paintBounds.y - ycs && y < paintBounds.y + paintBounds.height)
				g.fillRect(0, y, width, ycs);
			y += 2*ycs;
		}

		// Header separator
		g.setColor(LINE_COLOR);
		if (ycs > paintBounds.y && ycs < paintBounds.y + paintBounds.height)
			g.drawLine(paintBounds.x, ycs, paintBounds.x + paintBounds.width, ycs);
		if (xcs > paintBounds.x && xcs < paintBounds.x + paintBounds.width)
			g.drawLine(xcs, paintBounds.y, xcs, paintBounds.y + paintBounds.height);

		// Row headers
		int x = xcs / 4;
		y = 3 * ycs / 2;
		g.setFont(FONT.deriveFont(12*scale).deriveFont(Font.BOLD));
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for(Float instant : instants) {
			if (xcs > paintBounds.x && y > paintBounds.y - ycs && y < paintBounds.y + paintBounds.height) {
				g.drawString(String.format("%.1f "+frame.settings.timeUnits, instant), x, y);
				g.drawString(String.format("frame %.0f", (instant+1)/frame.settings.dt), x, Math.round(y+12*scale));
			}
			y += ycs;
		}

		// Column headers
		if (null != columnWidths) {
			x = xcs;
			for (int i = 0; i < columnWidths.length; i++) {
				int cw = columnWidths[i]-1;
				g.setColor(columnColors[i]);
				g.drawString(String.format("Track %d", i+1), x+20, ycs/2);
				g.setColor(LINE_COLOR);					
				x += cw * xcs;
				g.drawLine(x, 0, x, height);
			}
		}
	}
	
	public void setRowForInstant(TreeMap<Float, Integer> rowForInstant) {
		rows = rowForInstant;
	}
	
	public TreeMap<Float, Integer> getRowForInstant() {
		return rows;
	}

	public void setColumnWidths(int[] columnWidths) {
		this.columnWidths  = columnWidths;
	}

	public void setColumnColor(Color[] columnColors) {
		this.columnColors = columnColors;
	}

	public int[] getColumnWidths() {
		return columnWidths;
	}

	/** 
	 * This listener method will be invoked when a new edge has been created interactively
	 * in the graph component. 
	 */
	@Override
	public void invoke(Object sender, mxEventObject evt) {
		Map<String, Object> props = evt.getProperties();
		Object obj = (Object) props.get("cell");
		mxCell cell = (mxCell) obj;
		DefaultWeightedEdge edge;
		if (cell.isEdge()) {
			frame.getGraph().getModel().beginUpdate();
			try {
				Spot source = frame.getGraph().getCellToVertexMap().get(cell.getSource());
				Spot target = frame.getGraph().getCellToVertexMap().get(cell.getTarget());
				// We add a new jGraphT edge to the underlying model
				edge = frame.lGraph.addEdge(source, target);
				frame.lGraph.setEdgeWeight(edge, -1);
				// Then, remove the old JGraphX edge.
				frame.getGraph().removeCells(new Object[] { cell });
				evt.consume();
			} finally {
				frame.getGraph().getModel().endUpdate();
			}
			// Then we do the update, and get the new JGraphX edge (through the map in the adapter) and change its value and style. Easy.
			frame.getGraph().getModel().beginUpdate();
			try {
				mxCell newEdgeCell = frame.getGraph().getEdgeToCellMap().get(edge);
				newEdgeCell.setValue("N");
				newEdgeCell.setStyle(mxTrackGraphLayout.BASIC_EDGE_STYLE);
			} finally {
				frame.getGraph().getModel().endUpdate();
			}
		}
	}

}
