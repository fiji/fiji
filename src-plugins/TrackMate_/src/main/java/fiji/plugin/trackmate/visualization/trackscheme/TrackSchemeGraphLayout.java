package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_HEIGHT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.DEFAULT_CELL_WIDTH;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.X_COLUMN_SIZE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.Y_COLUMN_SIZE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.imglib2.algorithm.Benchmark;

import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.graph.GraphUtils;
import fiji.plugin.trackmate.graph.SortedDepthFirstIterator;

/**
 * This {@link mxGraphLayout} arranges cells on a graph in lanes corresponding to tracks. 
 * It also sets the style of each cell so that they have a coloring depending on the lane
 * they belong to.
 * Each lane's width and color is available to other classes for further exploitation.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Mar 2011 - 2012
 *
 */
public class TrackSchemeGraphLayout extends mxGraphLayout implements Benchmark {

	private static final int START_COLUMN = 2;

	/** The target model to draw spot from. */
	private final TrackMateModel model;
	private final JGraphXAdapter graph;
	private final TrackSchemeGraphComponent component;
	/**
	 * Hold the current row length for each frame.
	 * That is, for frame <code>i</code>, the number of cells on the row
	 * corresponding to frame <code>i</code> is <code>rowLength.get(i)</code>.
	 * This field is regenerated after each call to {@link #execute(Object)}.
	 */
	private Map<Integer, Integer> rowLengths;
	private long processingTime;



	/*
	 * CONSTRUCTOR
	 */

	public TrackSchemeGraphLayout(final JGraphXAdapter graph, final TrackMateModel model, final TrackSchemeGraphComponent component) {
		super(graph);
		this.graph = graph;
		this.model = model;
		this.component = component;
	}

	/*
	 * PUBLIC METHODS
	 */

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

		/*
		 *  Get a neighbor cache
		 */
		DirectedNeighborIndex<Spot, DefaultWeightedEdge> neighborCache = model.getTrackModel().getDirectedNeighborIndex();

		/*
		 * Compute column width from recursive cumsum
		 */
		Map<Spot, Integer> cumulativeBranchWidth = GraphUtils.cumulativeBranchWidth(model.getTrackModel());

		/* 
		 * How many rows do we have to parse?
		 */
		int maxFrame = 0;
		for (Spot spot : model.getFilteredSpots()) {
			int frame = spot.getFeature(Spot.FRAME).intValue();
			if (maxFrame < frame) {
				maxFrame = frame;
			}
		}

		graph.getModel().beginUpdate();
		try {

			/*
			 * Pass n tracks info on component
			 */
			final int ntracks = model.getTrackModel().getNFilteredTracks();
			component.columnWidths = new int[ntracks];
			component.columnTrackIDs = new Integer[ntracks];

			/*
			 * Initialize the column occupancy array
			 */
			final int[] columns = new int[maxFrame+1];
			for (int i = 0; i < columns.length; i++) {
				columns[i] = START_COLUMN;
			}

			int trackIndex = 0;
			for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) { // will be sorted by track name

				// Get Tracks
				final Set<Spot> track = model.getTrackModel().getTrackSpots(trackID);

				// Pass name & trackID to component
				component.columnTrackIDs[trackIndex] = trackID;

				// Get first spot
				TreeSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.frameComparator);
				sortedTrack.addAll(track);
				Spot first = sortedTrack.first();

				/*
				 * A special case: our quick layout below fails for graph that are not trees. That
				 * is: if a track has at least a spot that has more than one predecessor. If we have 
				 * to deal with such a case, we revert to the old, slow scheme.
				 */

				boolean isTree = GraphUtils.isTree(track, neighborCache);

				if (isTree) {

					/*
					 * Quick layout for a tree-like track 
					 */


					// First loop: Loop over spots in good order
					SortedDepthFirstIterator<Spot,DefaultWeightedEdge> iterator = model.getTrackModel().getSortedDepthFirstIterator(first, Spot.nameComparator, false);

					while(iterator.hasNext()) {

						Spot spot = iterator.next();

						// Get corresponding JGraphX cell, add it if it does not exist in the JGraphX yet
						mxICell cell = graph.getCellFor(spot);

						// This is cell is in a track, remove it from the list of lonely cells
						lonelyCells.remove(cell);

						// Determine in what row to put the spot
						int frame = spot.getFeature(Spot.FRAME).intValue();

						// Cell size, position and style
						int cellPos = columns[frame] + cumulativeBranchWidth.get(spot)/2;
						setCellGeometry(cell, frame, cellPos);
						columns[frame] += cumulativeBranchWidth.get(spot);

						// If it is a leaf, we fill the remaining row below and above
						if (neighborCache.successorsOf(spot).size() == 0) {
							int target = columns[frame];
							for (int i = 0; i <= maxFrame; i++) {
								columns[i] = target;
							}
						}

					}



				} else {

					/*
					 * Old layout for merging tracks
					 */

					// Init track variables
					Spot previousSpot = null;
					int currentColumn = columns[0];
					boolean previousDirectionDescending = true;

					// First loop: Loop over spots 
					DepthFirstIterator<Spot, DefaultWeightedEdge> iterator = model.getTrackModel().getDepthFirstIterator(first, false);
					while(iterator.hasNext()) {

						Spot spot = iterator.next();

						// Get corresponding JGraphX cell, add it if it does not exist in the JGraphX yet
						mxICell cell = graph.getCellFor(spot);

						// This is cell is in a track, remove it from the list of lonely cells
						lonelyCells.remove(cell);

						// Determine in what column to put the spot
						int frame = spot.getFeature(Spot.FRAME).intValue();

						int freeColumn = columns[frame] + 1;

						int targetColumn;
						boolean currentDirectionDescending = previousDirectionDescending;
						if (previousSpot != null) currentDirectionDescending = Spot.frameComparator.compare(spot, previousSpot) > 0;

						if (previousSpot != null && ! (model.getTrackModel().containsEdge(previousSpot, spot) 
								|| model.getTrackModel().containsEdge(spot, previousSpot) ) ) { // direction does not matter
							// If we have no direct edge with the previous spot, we add 1 to the current column
							currentColumn = currentColumn + 1;
							targetColumn = Math.max(freeColumn, currentColumn);
							currentColumn = targetColumn;

						} else if (previousSpot != null && previousDirectionDescending != currentDirectionDescending) {
							// If we changed direction...
							currentColumn = currentColumn + 1;
							targetColumn = Math.max(freeColumn, currentColumn);
							currentColumn = targetColumn;

						} else {
							// Nothing special
							targetColumn = currentColumn;
						}

						previousDirectionDescending = currentDirectionDescending;
						previousSpot = spot;


						// Keep track of column filling
						columns[frame] = targetColumn;

						// Cell position
						setCellGeometry(cell, frame, targetColumn);

					}

					for (int j = 0; j < columns.length; j++) {
						columns[j]++;
					}
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

				// Store column widths for the panel background
				int sumWidth = START_COLUMN - 1;
				for (int i = 0; i < trackIndex; i++) {
					sumWidth += component.columnWidths[i];
				}
				component.columnWidths[trackIndex] = maxCol - sumWidth;


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
				int frame = spot.getFeature(Spot.FRAME).intValue();
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
	}


	private final void setCellGeometry(final mxICell cell, final int row, final int targetColumn) {

		double x = (targetColumn) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
		double y = (0.5 + row  + 1) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2;
		mxGeometry geometry = cell.getGeometry();
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


	@Override
	public long getProcessingTime() {
		return processingTime;
	}
}
