package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.DEFAULT_CELL_HEIGHT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.DEFAULT_CELL_WIDTH;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.X_COLUMN_SIZE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.Y_COLUMN_SIZE;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * This {@link mxGraphLayout} arranges cells on a graph in lanes corresponding to tracks. 
 * It also sets the style of each cell so that they have a coloring depending on the lane
 * they belong to.
 * Each lane's width and color is available to other classes for further exploitation.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Mar 13, 2011
 *
 */
public class mxTrackGraphLayout extends mxGraphLayout {

	private static final int SWIMLANE_HEADER_SIZE = 30;
	
	private JGraphXAdapter<Spot, DefaultWeightedEdge> graph;
	private int[] columnWidths;
	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	private Color[] trackColorArray;
	private TreeMap<Float, Integer> rows;
	private UndirectedGraph<Spot, DefaultWeightedEdge> jGraphT;
	/**
	 * The spatial calibration in X. We need it to compute cell's height from spot radiuses.
	 */
	private float dx;
	/**
	 * We store swimlane cells (roots) so that we can purge them from the graph when redoing the layout.
	 */
	private ArrayList<Object> parentCells = new ArrayList<Object>();

	/*
	 * CONSTRUCTOR
	 */

	public mxTrackGraphLayout(UndirectedGraph<Spot, DefaultWeightedEdge> jGraphT, JGraphXAdapter<Spot, DefaultWeightedEdge> graph, float dx) {
		super(graph);
		this.graph = graph;
		this.jGraphT = jGraphT;
		this.dx = dx;
	}

	@Override
	public void execute(Object parent) {
		
		ArrayList<Object> newParents = new ArrayList<Object>();
		
		// Compute tracks
		List<Set<Spot>> tracks = new ConnectivityInspector<Spot, DefaultWeightedEdge>(jGraphT).connectedSets();

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

			int currentColumn = 2;
			int previousColumn = 0;
			int columnIndex = 0;
			int trackIndex = 0;
			int partIndex;
			boolean createNewRoot = false;
			
			columnWidths = new int[tracks.size()];
			trackColorArray = new Color[tracks.size()];
			Color trackColor = null;
			String trackColorStr = null;
			Object currentParent = graph.getDefaultParent();
			mxGeometry geometry = null;
			boolean branchIsNew = false; // Used to make sure we add edges to the right parent
						
			for (Set<Spot> track : tracks) {
				
				// Init track variables
				double maxX = 0;
				double maxY = 0;
				double laneTopX = 0;
				double laneTopY = 0;
				partIndex = 1;
				trackIndex++;
				Spot previousSpot = null;
				
				// Get track color
				trackColor = trackColors.get(track);
				trackColorStr =  Integer.toHexString(trackColor.getRGB()).substring(2);

				// Sort by ascending order
				SortedSet<Spot> sortedTrack = new TreeSet<Spot>(SpotImp.frameComparator);
				sortedTrack.addAll(track);
				Spot first = sortedTrack.first();
								
				// Loop over track child
				DepthFirstIterator<Spot, DefaultWeightedEdge> iterator = new DepthFirstIterator<Spot, DefaultWeightedEdge>(jGraphT, first);
				while(iterator.hasNext()) {
					
					Spot spot = iterator.next();
					int vertexType = TMUtils.getVertexType(jGraphT, spot);
					
					// Get corresponding JGraphX cell 
					mxCell cell = graph.getVertexToCellMap().get(spot);
					cell.setValue(spot.toString());
					
					// Get default style					
					String style = cell.getStyle();

					// Determine in what column to put the spot
					Float instant = spot.getFeature(Feature.POSITION_T);
					int freeColumn = columns.get(instant) + 1;

					// If we have no direct edge with the previous spot, we add 1 to the current column
					if (previousSpot != null && !jGraphT.containsEdge(spot, previousSpot)) {
						currentColumn = currentColumn + 1;
						createNewRoot = true;
						branchIsNew = true;
					}
					previousSpot = spot;
					
					int targetColumn = Math.max(freeColumn, currentColumn);
					currentColumn = targetColumn;

					// Keep track of column filling
					columns.put(instant, targetColumn);
				
					// Compute cell position in absolute coords 
					double x = (targetColumn) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
					double y = (0.5 + rows.get(instant)) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2;
					
					// Cell size
					int height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Feature.RADIUS) / dx));
					height = Math.max(height, 12);
					geometry = new mxGeometry(x, y, DEFAULT_CELL_WIDTH, height);

					if ((vertexType & TMUtils.BRANCH_START) > 0) {
						createNewRoot = true;
					}
					
					if ((vertexType & TMUtils.SPLITTING_POINT) > 0) {
						// Close previous parent
						makeRootGeometry(currentParent, laneTopX, laneTopY, maxX, maxY);
						currentParent = graph.getDefaultParent();
					}
					
					if ((vertexType & TMUtils.BRANCH_END) > 0) {
						// Close this parent
						makeRootGeometry(currentParent, laneTopX, laneTopY, maxX, maxY);
					}
					
					// Create a new root if needed
					if (createNewRoot) {
						currentParent = makeRootCell(trackColorStr, trackIndex, partIndex++);
						newParents.add(currentParent);
						maxX = 0;
						maxY = 0;
						laneTopX = x - DEFAULT_CELL_WIDTH/8;
						laneTopY = y - DEFAULT_CELL_HEIGHT/2;
						createNewRoot = false;
					}
					
					// Add it to its root cell holder
					graph.getModel().add(currentParent, cell, 0); //spotIndex++);
					if (currentParent == graph.getDefaultParent())
						createNewRoot = true;
					else
						geometry.translate(- laneTopX, - laneTopY);	// Translate geometry with respect to root cell
	
					graph.getModel().setGeometry(cell, geometry);
					
					// Store max & min position
					if (x > maxX)
						maxX = x;
					if (y > maxY)
						maxY = y;
					
					// Set cell style and image
					style = mxUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR, trackColorStr);
					style = graph.getModel().setStyle(cell, style);
					geometry.setRelative(false);
					
					// Edges
					Object[] objEdges = graph.getEdges(cell, null, true, false, false);
					for(Object obj : objEdges) {
						mxCell edgeCell = (mxCell) obj;
						
						if (branchIsNew) {
							graph.getModel().add(graph.getDefaultParent(), edgeCell, 0);
							branchIsNew = false;
						}
						else
							graph.getModel().add(currentParent, edgeCell, 0);
						
						DefaultWeightedEdge edge = graph.getCellToEdgeMap().get(edgeCell);
						edgeCell.setValue(String.format("%.1f", jGraphT.getEdgeWeight(edge)));
						String edgeStyle = edgeCell.getStyle();
						edgeStyle = mxUtils.setStyle(edgeStyle, mxConstants.STYLE_STROKECOLOR, trackColorStr);
						graph.getModel().setStyle(edgeCell, edgeStyle);
					}
					
				}
				
				makeRootGeometry(currentParent, laneTopX, laneTopY, maxX, maxY);
				
				for(Float instant : instants)
					columns.put(instant, currentColumn+1);

				columnWidths[columnIndex] = currentColumn - previousColumn + 1;
				trackColorArray[columnIndex] = trackColor;
				columnIndex++;
				previousColumn = currentColumn;	

			}  // loop over tracks

			// Purge previous root cells
			for (Object rc : parentCells) 
				graph.getModel().remove(rc);
			
			// Store root cells for next call to #execute
			parentCells = newParents;

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
	
	private mxCell makeRootCell(String trackColorStr, int trackIndex, int partIndex) {
		// Set this as parent for the coming track in JGraphX
		mxCell rootCell = (mxCell) graph.insertVertex(graph.getDefaultParent(), null, "Track "+trackIndex+"\nBranch "+partIndex, 100, 100, 100, 100);
		rootCell.setConnectable(false);
		
		// Set the root style
		String rootStyle = rootCell.getStyle();
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_STROKECOLOR, "black");
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_ROUNDED, "false");
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_FILLCOLOR, Integer.toHexString(Color.DARK_GRAY.brighter().getRGB()).substring(2));
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_DASHED, "true");
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP);
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_FONTCOLOR, trackColorStr);
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_FONTSTYLE, ""+mxConstants.FONT_BOLD);
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_SHAPE, ""+mxConstants.SHAPE_SWIMLANE);
		rootStyle = mxUtils.setStyle(rootStyle, mxConstants.STYLE_STARTSIZE, ""+SWIMLANE_HEADER_SIZE);
		graph.getModel().setStyle(rootCell, rootStyle);
		
		return rootCell;
	}
	
	private void makeRootGeometry(Object rootCell, double laneTopX, double laneTopY, double maxX, double maxY) {
		mxGeometry rootGeometry = new mxGeometry();
		rootGeometry.setX(laneTopX);
		rootGeometry.setY(laneTopY);
		rootGeometry.setWidth(maxX - laneTopX + 9 * DEFAULT_CELL_WIDTH / 8);
		rootGeometry.setHeight(maxY - laneTopY + DEFAULT_CELL_HEIGHT);
		rootGeometry.setAlternateBounds(new mxRectangle(laneTopX, laneTopY, rootGeometry.getWidth(), SWIMLANE_HEADER_SIZE));
		graph.getModel().setGeometry(rootCell, rootGeometry);

	}
}
