package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_HEIGHT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_WIDTH;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.X_COLUMN_SIZE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.Y_COLUMN_SIZE;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.imglib2.algorithm.Benchmark;

import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxStyleUtils;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;

/**
 * This {@link mxGraphLayout} arranges cells on a graph in lanes corresponding to tracks. 
 * It also sets the style of each cell so that they have a coloring depending on the lane
 * they belong to.
 * Each lane's width and color is available to other classes for further exploitation.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Mar 2011 - 2012
 *
 */
public class mxTrackGraphLayout extends mxGraphLayout implements Benchmark {

	private static final int SWIMLANE_HEADER_SIZE = 30;
	private static final boolean DEBUG = false;
	private static final int START_COLUMN = 2;


	private boolean doDisplayCosts = TrackScheme.DEFAULT_DO_DISPLAY_COSTS_ON_EDGES;
	/** The target model to draw spot from. */
	private final TrackMateModel model;
	private final JGraphXAdapter graph;
	/**
	 * Hold the current row length for each frame.
	 * That is, for frame <code>i</code>, the number of cells on the row
	 * corresponding to frame <code>i</code> is <code>rowLength.get(i)</code>.
	 * This field is regenerated after each call to {@link #execute(Object)}.
	 */
	private Map<Integer, Integer> rowLengths;
	private int[] columnWidths;
	private long processingTime;

	/*
	 * CONSTRUCTOR
	 */

	public mxTrackGraphLayout(final JGraphXAdapter graph, final TrackMateModel model) {
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

		long start = System.currentTimeMillis();

		/*
		 * To be able to deal with lonely cells later (i.e. cells that are not part of a track),
		 * we retrieve the list of all cells.
		 */
		Object[] objs = graph.getChildVertices(graph.getDefaultParent());
		final ArrayList<mxCell> lonelyCells = new ArrayList<mxCell>(objs.length);
		for (Object obj : objs) {
			lonelyCells.add((mxCell) obj);
		}

		// Get a neighborcache
		final DirectedNeighborIndex<Spot, DefaultWeightedEdge> neighborCache = model.getTrackModel().getDirectedNeighborIndex();
		
		graph.getModel().beginUpdate();
		try {

			final int ntracks = model.getTrackModel().getNFilteredTracks();

			/* 
			 * The columns array hold the column count for a given frame number
			 */
			int maxFrame = 0;
			for (Spot spot : model.getFilteredSpots()) {
				Integer frame = safeFindFrame(spot, model.getFilteredSpots(), model.getSettings().dt);
				if (null == frame) {
					continue;
				}
				int intframe = frame.intValue();
				if (maxFrame < intframe) {
					maxFrame = intframe;
				}
			}

			/*
			 * Initialize the column occupancy array
			 */
			final int[] columns = new int[maxFrame+1];
			for (int i = 0; i < columns.length; i++) {
				columns[i] = START_COLUMN;
			}

			columnWidths = new int[ntracks];

			Object currentParent = graph.getDefaultParent();

			int trackIndex = 0;

			for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {

				// Get Tracks
				final Set<Spot> track = model.getTrackModel().getTrackSpots(trackID);

				// Sort by ascending order
				SortedSet<Spot> sortedTrack = new TreeSet<Spot>(SpotImp.timeComparator);
				sortedTrack.addAll(track);
				Spot first = sortedTrack.first();

				// First loop: Loop over spots in good order
				DepthFirstIterator<Spot, DefaultWeightedEdge> iterator = model.getTrackModel().getUndirectedDepthFirstIterator(first);
				
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

					// Determine in what row to put the spot
					Integer frame = safeFindFrame(spot, model.getFilteredSpots(), model.getSettings().dt);
					if (null == frame) {
						continue;
					}

					// Cell size, position and style
					setCellGeometry(cell, frame, columns[frame]++);

				}

				// Second pass: we now iterate over each spot's edges
				for(final DefaultWeightedEdge edge : model.getTrackModel().getTrackEdges(trackID)) {

					mxICell edgeCell = graph.getCellFor(edge);

					if (null == edgeCell) {
						if (DEBUG) {
							System.out.println("[mxTrackGraphLayout] execute: creating cell for invisible edge "+edge);
						}
						edgeCell = graph.addJGraphTEdge(edge);
					}

					graph.getModel().add(currentParent, edgeCell, 0);
					String edgeStyle = edgeCell.getStyle();
					edgeStyle = mxStyleUtils.setStyle(edgeStyle, mxSideTextShape.STYLE_DISPLAY_COST, ""+doDisplayCosts);
					graph.getModel().setStyle(edgeCell, edgeStyle);
				}


				// When done with a track, move all columns to the next free column
				int maxCol = 0;
				for (int j = 0; j < columns.length; j++) {
					if (columns[j] > maxCol) {
						maxCol = columns[j];
					}
				}
				for (int i = 0; i < columns.length; i++) {
					columns[i] = maxCol + 1;
				}

				int sumWidth = 0;
				for (int i = 0; i < trackIndex; i++) {
					sumWidth += columnWidths[i];
				}
				columnWidths[trackIndex] = maxCol - sumWidth + 1;
				trackIndex++;

			}  // loop over tracks

			// Ensure we do not start at 0 for the first column of lonely cells
			for (int i = 0; i < columns.length; i++) {
				if (columns[i] < 1) {
					columns[i] = 1;
				}
			}

			// Deal with lonely cells
			for (mxCell cell : lonelyCells) {
				Spot spot = graph.getSpotFor(cell);
				int frame = safeFindFrame(spot, model.getFilteredSpots(), model.getSettings().dt);
				setCellGeometry(cell, frame, ++columns[frame]);
			}

			// Before we leave, we regenerate the row length, for our brothers
			rowLengths = new HashMap<Integer, Integer>(columns.length);
			for (int i = 0; i < columns.length; i++) {
				rowLengths.put(i, columns[i]+1); // we add 1 so that we do not report the track lane limit
			}

		} finally {
			graph.getModel().endUpdate();
		}

		long end = System.currentTimeMillis();
		processingTime = end - start;

		System.out.println("Layout done in " + processingTime + " ms."); // DEBUG

	}


	private final void setCellGeometry(final mxICell cell, final int row, final int targetColumn) {

		double height = DEFAULT_CELL_HEIGHT;  // TODO FIXME we should read the size from the style and center x y accordingly
		double width  = DEFAULT_CELL_WIDTH;

		mxGeometry geometry = cell.getGeometry();
		geometry.setHeight(height);
		geometry.setWidth(width);

		double x = (targetColumn) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
		double y = (0.5 + row  + 1) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2;
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

	/**
	 * Retrieve the frame a spot belong to, with failsafe. We return <code>null</code> when every possibility 
	 * is exhausted.
	 */
	private static final Integer safeFindFrame(final Spot spot, final SpotCollection spots, double dt) {
		Number frame = spot.getFeature(Spot.FRAME);
		if (null == frame) {
			// Damn, no info on the frame I belong to
			frame = spots.getFrame(spot);
			if (null == frame) {
				// Still no info?!? Ok then try with the POSITION_T feature
				Double post = spot.getFeature(Spot.POSITION_T);
				if (null == post || dt <= 0) {
					return null; // I give up
				}
				frame = Math.round(post / dt);
			}
		}
		return frame.intValue();
	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	}
}
