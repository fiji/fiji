package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.DEFAULT_CELL_HEIGHT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.DEFAULT_CELL_WIDTH;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.X_COLUMN_SIZE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.Y_COLUMN_SIZE;

import java.awt.Color;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxStylesheet;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.visualization.trackscheme.SpotCellViewFactory.SpotCell;
import fiji.plugin.trackmate.visualization.trackscheme.SpotCellViewFactory.TrackEdgeCell;

public class JGraphTimeLayout extends mxGraphLayout {



	private JGraphXAdapter<Spot, DefaultWeightedEdge> graph;
	private List<Set<Spot>> tracks;
	private int[] columnWidths;
	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	private Color[] trackColorArray;
	private TreeMap<Float, Integer> rows;
	private UndirectedGraph<Spot, DefaultWeightedEdge> jGraphT;

	/*
	 * CONSTRUCTOR
	 */


	public JGraphTimeLayout(UndirectedGraph<Spot, DefaultWeightedEdge> jGraphT, JGraphXAdapter<Spot, DefaultWeightedEdge> graph) {
		super(graph);
		this.graph = graph;
		this.jGraphT = jGraphT;
		this.tracks = new ConnectivityInspector<Spot, DefaultWeightedEdge>(jGraphT).connectedSets();
	}

	@Override
	public void execute(Object parent) {

		// Create rounded style
		mxStylesheet stylesheet = graph.getStylesheet();
		Hashtable<String, Object> style = new Hashtable<String, Object>();
		style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
		style.put(mxConstants.STYLE_OPACITY, 50);
		style.put(mxConstants.STYLE_FONTCOLOR, "#774400");
		stylesheet.putCellStyle("ROUNDED", style);
		
		
		graph.getModel().beginUpdate();
		try {

			// Generate colors
			HashMap<Set<Spot>, Color> trackColors = new HashMap<Set<Spot>, Color>(tracks.size());
			int counter = 0;
			int ntracks = tracks.size();
			for(Set<Spot> track : tracks) {
				trackColors.put(track, colorMap.getPaint((float) counter / (ntracks-1)));
				counter++;
			}

			// Collect unique instants
			SortedSet<Float> instants = new TreeSet<Float>();
			for (Spot s : jGraphT.vertexSet())
				instants.add(s.getFeature(Feature.POSITION_T));

			TreeMap<Float, Integer> columns = new TreeMap<Float, Integer>();
			for(Float instant : instants)
				columns.put(instant, -1);

			// Build row indices from instants
			rows = new TreeMap<Float, Integer>();
			Iterator<Float> it = instants.iterator();
			int rowIndex = 1; // Start at 1 to let room for column headers
			while (it.hasNext()) {
				rows.put(it.next(), rowIndex);
				rowIndex++;
			}

			int currentColumn = 1;
			int previousColumn = 0;
			Spot previousSpot = null;
			int columnIndex = 0;
			columnWidths = new int[tracks.size()];
			trackColorArray = new Color[tracks.size()];
			Color trackColor = null;


			for (Set<Spot> track : tracks) {

				// Get track color
				trackColor = trackColors.get(track);

				// Sort by ascending order
				SortedSet<Spot> sortedTrack = new TreeSet<Spot>(SpotImp.frameComparator);
				sortedTrack.addAll(track);
				Spot root = sortedTrack.first();

				DepthFirstIterator<Spot, DefaultWeightedEdge> iterator = new DepthFirstIterator<Spot, DefaultWeightedEdge>(jGraphT, root);
				while (iterator.hasNext()) {
					Spot spot = iterator.next();

					// Determine in what column to put the spot
					Float instant = spot.getFeature(Feature.POSITION_T);
					int freeColumn = columns.get(instant) + 1;

					// If we have no direct edge with the previous spot, we add 1 to the current column
					if (!jGraphT.containsEdge(spot, previousSpot))
						currentColumn = currentColumn + 1;
					previousSpot = spot;

					int targetColumn = Math.max(freeColumn, currentColumn);
					currentColumn = targetColumn;

					// Keep track of column filling
					columns.put(instant, targetColumn);

					// Get corresponding JGraphX cell 
					SpotCell cell = (SpotCell) graph.getCellForVertex(spot);

					// Tune aspect of cell according to context

					// Move the corresponding cell in the facade
					double x = (targetColumn) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
					double y = (0.5 + rows.get(instant)) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2;
					int height = Math.min(DEFAULT_CELL_WIDTH, spot.getIcon().getIconHeight());
					height = Math.max(height, 12);
					mxGeometry geometry = new mxGeometry(x, y, DEFAULT_CELL_WIDTH, height);
					graph.getModel().setGeometry(cell, geometry);
					graph.getModel().setStyle(cell, "strokeColor="+Integer.toHexString(trackColor.getRGB()));

//					// Edges
					Object[] objEdges = graph.getEdges(cell);
					for(Object obj : objEdges) {
						TrackEdgeCell edge = (TrackEdgeCell) obj;
						graph.getModel().setStyle(edge, "startArrow=none;endArrow=none;strokeWidth=2;strokeColor="+Integer.toHexString(trackColor.getRGB())); //
					}
				}


				for(Float instant : instants)
					columns.put(instant, currentColumn+1);

				columnWidths[columnIndex] = currentColumn - previousColumn + 1;
				trackColorArray[columnIndex] = trackColor;
				columnIndex++;
				previousColumn = currentColumn;	


			}  // loop over tracks

		} finally {

			graph.getModel().endUpdate();
		}


	}

	/**
	 * Return the width in column units of each track after they are arranged by this GraphLayout.
	 */
	public int[] getTrackColumnWidths() {
		return columnWidths;
	}

	/**
	 * Return map linking the the row number for a given instant.
	 */
	public TreeMap<Float, Integer> getRowForInstant() {
		return rows;
	}

	/**
	 * Return the color affected to each track.
	 */
	public Color[] getTrackColors() {
		return trackColorArray;
	}

	@Override
	public void moveCell(Object cell, double x, double y) {
		System.out.println("Moving cell: "+cell+" to x="+x+" y="+y);// DEBUG
	}

}
