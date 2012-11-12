package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_HEIGHT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_WIDTH;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_COLOR;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.X_COLUMN_SIZE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.Y_COLUMN_SIZE;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxStyleUtils;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.TrackSplitter;

/**
 * This {@link mxGraphLayout} arranges cells on a graph in lanes corresponding to tracks. 
 * It also sets the style of each cell so that they have a coloring depending on the lane
 * they belong to.
 * Each lane's width and color is available to other classes for further exploitation.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Mar 2011 - 2012
 *
 */
public class mxTrackGraphLayout <T extends RealType<T> & NativeType<T>> extends mxGraphLayout {

	private static final int SWIMLANE_HEADER_SIZE = 30;
	private static final boolean DEBUG = false;

	private int[] columnWidths;
	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	/** The style to use to apply to cells, can be changed by the user. */
	private String layoutStyle = TrackScheme.DEFAULT_STYLE_NAME;
	
	private Color[] trackColorArray;
	/**  Do we group branches and display branch cells.
	 * False by default. */
	private boolean doBranchGrouping = false;
	/**  Do we display costs along edges? Default set by mother frame. */
	private boolean doDisplayCosts = TrackScheme.DEFAULT_DO_DISPLAY_COSTS_ON_EDGES;
	/**  Used to keep a reference to the branch cell which will contain spot cells.
	 * We need this to be able to purge them from the graph when we redo a layout.	 */
	private ArrayList<mxCell> branchCells = new ArrayList<mxCell>();
	/** The target model to draw spot from. */
	private final TrackMateModel<T> model;
	private final JGraphXAdapter<T> graph;
	/**
	 * Hold the current row length for each frame.
	 * That is, for frame <code>i</code>, the number of cells on the row
	 * corresponding to frame <code>i</code> is <code>rowLength.get(i)</code>.
	 * This field is regenerated after each call to {@link #execute(Object)}.
	 */
	private Map<Integer, Integer> rowLengths;
	
	/*
	 * CONSTRUCTOR
	 */

	public mxTrackGraphLayout(final JGraphXAdapter<T> graph, final TrackMateModel<T> model) {
		super(graph);
		this.graph = graph;
		this.model = model;
	}

	/*
	 * PUBLIC METHODS
	 */

	public void setDoDisplayCosts(boolean doDisplayCosts) {
		this.doDisplayCosts = doDisplayCosts;
	}

	public boolean isDoDisplayCosts() {
		return doDisplayCosts;
	}

	@Override
	public void execute(Object parent) {

		/*
		 * To be able to deal with lonely cells later (i.e. cells that are not part of a track),
		 * we retrieve the list of all cells.
		 */
		Object[] objs = graph.getChildVertices(graph.getDefaultParent());
		final ArrayList<mxCell> lonelyCells = new ArrayList<mxCell>(objs.length);
		for (Object obj : objs) {
			lonelyCells.add((mxCell) obj);
		}
		
		graph.getModel().beginUpdate();
		try {

			// Generate colors
			int ntracks = model.getNFilteredTracks();
			HashMap<Integer, Color> trackColors = new HashMap<Integer, Color>(ntracks, 1);
			int colorIndex = 0;
			for (int i : model.getVisibleTrackIndices()) { 				
				trackColors.put(i, colorMap.getPaint( (double) colorIndex / (ntracks-1)));
				colorIndex++;
			}

			if (DEBUG) {
				System.out.println("[mxTrackGraphLayout] execute: Found "+ntracks+" visible tracks.");
			}

			/* 
			 * The columns array hold the column count for a given frame number
			 */
			int maxFrame = 0;
			for(Spot spot : model.getFilteredSpots()) {
				int frame = spot.getFeature(Spot.FRAME).intValue();
				if (maxFrame < frame) {
					maxFrame = frame;
				}
			}
			int[] columns = new int[maxFrame+1];

			int currentColumn = 2;
			int previousColumn = 0;
			int columnIndex = 0;
			int trackIndex = -1;
			double dx = model.getSettings().dx;
			if (dx <=0) {
				dx = 1;
			}

			columnWidths = new int[ntracks];
			trackColorArray = new Color[ntracks];

			String trackColorStr = null;
			Object currentParent = graph.getDefaultParent();

			// To keep a reference of branch cells, if any
			ArrayList<mxCell> newBranchCells = new ArrayList<mxCell>();

			int spotIndex = 0; // Index with which the cells will be added to the root.

			for (int i : model.getVisibleTrackIndices()) {
				trackIndex++;
				
				// Init track variables
				Spot previousSpot = null;

				// Get track color
				final Color trackColor = trackColors.get(i);
				trackColorStr =  Integer.toHexString(trackColor.getRGB()).substring(2);

				// Get Tracks
				final Set<Spot> track = model.getTrackSpots(i);

				if (track.isEmpty()) {
					if (DEBUG) {
						System.out.println("[mxTrackGraphLayout] execute: Track nbr "+i+" is empty, skipping.");
					}

					continue;
				}

				if (DEBUG) {
					System.out.println("[mxTrackGraphLayout] execute: Track nbr "+i+": "+model.trackToString(i));
				}

				// Sort by ascending order
				SortedSet<Spot> sortedTrack = new TreeSet<Spot>(SpotImp.timeComparator);
				sortedTrack.addAll(track);
				Spot first = sortedTrack.first();

				// First loop: Loop over spots in good order
				DepthFirstIterator<Spot, DefaultWeightedEdge> iterator = model.getUndirectedDepthFirstIterator(first);
				while(iterator.hasNext()) {

					Spot spot = iterator.next();
					
					// Get corresponding JGraphX cell, add it if it does not exist in the JGraphX yet
					mxICell cell = graph.getCellFor(spot);
					if (null == cell) {
						if (DEBUG) {
							System.out.println("[mxTrackGraphLayout] execute: creating cell for invisible spot "+spot);
						}
						cell = graph.addJGraphTVertex(spot);
					}
					
					// This is cell is in a track, remove it from the list of lonely cells
					lonelyCells.remove(cell);

					// Determine in what column to put the spot
					int frame = spot.getFeature(Spot.FRAME).intValue();
					int freeColumn = columns[frame] + 1;

					// If we have no direct edge with the previous spot, we add 1 to the current column
					if (previousSpot != null && ! (model.containsEdge(previousSpot, spot) || model.containsEdge(spot, previousSpot) ) ) { // direction does not matter
						currentColumn = currentColumn + 1;
					}
					previousSpot = spot;

					int targetColumn = Math.max(freeColumn, currentColumn);
					currentColumn = targetColumn;

					// Keep track of column filling
					columns[frame] = targetColumn;

					// Cell size, position and style
					setCellGeometryAndStyle(cell, spot, targetColumn, dx, trackColorStr);

					// Add it to its root cell holder.
					// Not needed, but if we do not do it, some cells with modified geometry 
					// are not put back to the imposed geometry.
					graph.getModel().add(currentParent, cell, spotIndex++); 

				}

				// Second pass: we now iterate over each spot's edges
				for(Spot spot : track) {

					for(final DefaultWeightedEdge edge : model.edgesOf(spot)) {
						mxICell edgeCell = graph.getCellFor(edge);
						if (null == edgeCell) {
							if (DEBUG) {
								System.out.println("[mxTrackGraphLayout] execute: creating cell for invisible edge "+edge);
							}
							edgeCell = graph.addJGraphTEdge(edge);
						}

						graph.getModel().add(currentParent, edgeCell, 0);
						String edgeStyle = edgeCell.getStyle();
						edgeStyle = mxStyleUtils.setStyle(edgeStyle, mxConstants.STYLE_STROKECOLOR, trackColorStr);
						edgeStyle = mxStyleUtils.setStyle(edgeStyle, mxSideTextShape.STYLE_DISPLAY_COST, ""+doDisplayCosts);
						graph.getModel().setStyle(edgeCell, edgeStyle);
					}
				}

				// When done with a track, move all columns to the next free column
				for (int j = 0; j < columns.length; j++) {
					columns[j] = currentColumn + 1;
				}

				columnWidths[columnIndex] = currentColumn - previousColumn + 1;
				trackColorArray[columnIndex] = trackColor;
				columnIndex++;
				previousColumn = currentColumn;	

				// Change the parent of some spots to add them to branches

				if (doBranchGrouping ) {

					ArrayList<ArrayList<Spot>> branches = new TrackSplitter<T>(model).splitTrackInBranches(track);

					int partIndex = 1;

					for (ArrayList<Spot> branch : branches) {

						mxCell branchParent = makeParentCell(trackColorStr, trackIndex, partIndex++);
						newBranchCells.add(branchParent);

						double minX = Double.MAX_VALUE;
						double minY = Double.MAX_VALUE;
						double maxX = 0;
						double maxY = 0;

						for (Spot spot : branch) {

							mxICell cell = graph.getCellFor(spot);

							mxGeometry geom = graph.getModel().getGeometry(cell);
							if (minX > geom.getX()) 
								minX = geom.getX(); 
							if (minY > geom.getY()) 
								minY = geom.getY(); 
							if (maxX < geom.getX() + geom.getWidth()) 
								maxX = geom.getX() + geom.getWidth(); 
							if (maxY < geom.getY() + geom.getHeight()) 
								maxY = geom.getY() + geom.getHeight();

							graph.getModel().add(branchParent, cell, 0);

						}

						minY -= SWIMLANE_HEADER_SIZE;
						mxGeometry branchGeometry = new mxGeometry(minX, minY, maxX-minX, maxY-minY);
						branchGeometry.setAlternateBounds(new mxRectangle(minX, minY, DEFAULT_CELL_WIDTH, SWIMLANE_HEADER_SIZE));
						graph.getModel().setGeometry(branchParent, branchGeometry);

						for (Spot spot : branch) {
							mxICell cell = graph.getCellFor(spot);
							graph.getModel().getGeometry(cell).translate(-branchGeometry.getX(), -branchGeometry.getY());
						}

					}

				}

			}  // loop over tracks

			// Clean previous branch cells
			for (mxCell branchCell : branchCells )
				graph.getModel().remove(branchCell);
			branchCells = newBranchCells;
			
			// Deal with lonely cells
			for (mxCell cell : lonelyCells) {
				Spot spot = graph.getSpotFor(cell);
				int frame = spot.getFeature(Spot.FRAME).intValue();
				setCellGeometryAndStyle(cell, spot, ++columns[frame], dx, DEFAULT_COLOR);
			}
			
			// Before we leave, we regenerate the row length, for our brothers
			rowLengths = new HashMap<Integer, Integer>(columns.length);
			for (int i = 0; i < columns.length; i++) {
				rowLengths.put(i, columns[i]+1); // we add 1 so that we do not report the track lane limit
			}

		} finally {
			graph.getModel().endUpdate();
		}
		
		
	}


	private final void setCellGeometryAndStyle(final mxICell cell, final Spot spot, final int targetColumn, final double dx, final String colorStr) {

		// Add selected style to style string
		String style = cell.getStyle(); 
		style = mxStyleUtils.removeAllStylenames(style);
		style = layoutStyle + ";" + style;
		
		double height 	= DEFAULT_CELL_HEIGHT; 
		double width 	= DEFAULT_CELL_WIDTH;

		// Deal with styles
		if (layoutStyle.equals("Simple")) {
			width 	= DEFAULT_CELL_HEIGHT / 2;
			height 	= DEFAULT_CELL_HEIGHT / 2;
			style = mxStyleUtils.setStyle(style, mxConstants.STYLE_FILLCOLOR, colorStr);
		} else {
			height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Spot.RADIUS) / dx));
			height = Math.max(height, DEFAULT_CELL_HEIGHT/3);
			style = mxStyleUtils.setStyle(style, mxConstants.STYLE_FILLCOLOR, "white");
		}
		style = mxStyleUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR, colorStr);
		style = graph.getModel().setStyle(cell, style);
		
		mxGeometry geometry = cell.getGeometry();
		geometry.setHeight(height);
		geometry.setWidth(width);
		int frame = spot.getFeature(Spot.FRAME).intValue();
		double x = (targetColumn) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
		double y = (0.5 + frame  + 1) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2;
		geometry.setX(x);
		geometry.setY(y);	
	
	}
	
	/**
	 * @return  the current row length for each frame.
	 * That is, for frame <code>i</code>, the number of cells on the row
	 * corresponding to frame <code>i</code> is <code>rowLength.get(i)</code>.
	 * This field is regenerated after each call to {@link #execute(Object)}.
	 */
	public Map<Integer, Integer> getRowLengths() {
		return rowLengths;
	}

	/**
	 * Return the width in column units of each track after they are arranged by this GraphLayout.
	 */
	public int[] getTrackColumnWidths() {
		return columnWidths;
	}
	/**
	 * Return the color affected to each track.
	 */
	public Color[] getTrackColors() {
		return trackColorArray;
	}

	private mxCell makeParentCell(String trackColorStr, int trackIndex, int partIndex) {
		// Set this as parent for the coming track in JGraphX
		mxCell rootCell = (mxCell) graph.insertVertex(graph.getDefaultParent(), null, "Track "+trackIndex+"\nBranch "+partIndex, 100, 100, 100, 100);
		rootCell.setConnectable(false);

		// Set the root style
		String rootStyle = rootCell.getStyle();
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_STROKECOLOR, "black");
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_ROUNDED, "false");
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_FILLCOLOR, Integer.toHexString(Color.DARK_GRAY.brighter().getRGB()).substring(2));
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_DASHED, "true");
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_TOP);
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_FONTCOLOR, trackColorStr);
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_FONTSTYLE, ""+mxConstants.FONT_BOLD);
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_SHAPE, ""+mxConstants.SHAPE_SWIMLANE);
		rootStyle = mxStyleUtils.setStyle(rootStyle, mxConstants.STYLE_STARTSIZE, ""+SWIMLANE_HEADER_SIZE);
		graph.getModel().setStyle(rootCell, rootStyle);

		return rootCell;
	}

	public boolean isBranchGroupingEnabled() {
		return doBranchGrouping;
	}

	public void setBranchGrouping(boolean enable) {
		this.doBranchGrouping = enable;
	}

	public void setAllFolded(boolean collapsed) {
		graph.foldCells(collapsed, false, branchCells.toArray());
	}

	public void setLayoutStyle(String layoutStyle) {
		this.layoutStyle = layoutStyle;
	}

	public String getLayoutStyle() {
		return layoutStyle;
	}

}
