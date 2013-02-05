package fiji.plugin.trackmate.tracking.costmatrix;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import Jama.Matrix;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.costfunction.GapClosingCostFunction;
import fiji.plugin.trackmate.tracking.costfunction.MergingCostFunction;
import fiji.plugin.trackmate.tracking.costfunction.SplittingCostFunction;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * <p>Creates the cost matrix <b><u>roughly</u></b> described in Figure 1c in the paper:
 * 
 * <p>Jaqaman, K. et al. "Robust single-particle tracking in live-cell time-lapse sequences."
 * Nature Methods, 2008.
 * 
 * <p>This matrix is notably <u>different</u> than the one described in the paper. This is because
 * the cost matrix described in 1c in the paper <b>is not</b> the cost matrix they use in their
 * matlab implementation.
 * 
 * <p>The cost matrix used in the matlab implementation by the authors, and used here, is described
 * as follows:
 * 
 * <p> The overall matrix can be divided into <u>four quadrants</u>, outlined below. Each 
 * quadrant has dimensions <code>(number of track segments + number of M/S candidate points) x 
 * (number of track segments + number of M/S candidate points)</code>.
 * A merging or splitting (M/S) candidate point is defined as a Spot in a track segment 
 * with at least two spots.
 * 
 * <ul>
 * <li>
 * <p><b>Top left</b>: Contains scores for gap closing, merging, splitting, and "blank" region.
 * <br><br>
 * <p>This quadrant can also be further subdivided into four smaller sub-matrices:
 *
 * <ul>
 * <li><i>Gap closing (top left)</i>: has dimensions <code>(number of track segments) x (number of track segments)</code>, 
 * and contains the scores for linking the ends of track segments to the starts of other track segments
 * as described in the paper.</li>
 * 
 * <li><i>Merging (top right)</i>: has (number of track segment) rows and (number of M/S candidate points)
 * columns. Contains scores for linking the end of a track segment into the
 * middle of another (a merge).</li>
 * 
 * <li><i>Splitting (bottom left)</i>: has (number of M/S candidate points) rows and (number of track
 * segments columns). Contains scores for linking the start of a track segment into the
 * middle of another (a split).</li>
 * 
 * <li><i>Empty (bottom right)</i>: has dimensions (number of middle points) x (number of middle points), and is blocked, 
 * so no solutions lie in this region.</li>
 * </ul>
 * <br>
 * 
 * </li>
 * <li>Top right: Terminating and splitting alternatives.
 * <br><br>
 * <p>This quadrant contains alternative scores, and the scores are arranged along the
 * diagonal (top left to bottom right). The score is a cutoff value, and is the same cutoff
 * value used in the bottom left and bottom right quadrants.
 * <br><br>
 * </li>
 * <li>Bottom left: Initiating and merging alternatives.
 * <br><br>
 * <p>This quadrant contains alternative scores, and the scores are arranged along the
 * diagonal (top left to bottom right). The score is a cutoff value, and is the same cutoff
 * value used in the top right and bottom right quadrants.
 * <br><br>
 * </li>
 * <li>Bottom right: Mathematically required to solve LAP problems.
 * <br><br>
 * <p>This quadrant requires special formatting to allow the LAP to be solved. Essentially,
 * the top left quadrant is transposed, and all non-blocked values are replaced with the cutoff
 * value used in the bottom left and top right quadrants.
 * <br><br>
 * </li>
 * </ul>
 *
 * @author Nicholas Perry
 *
 */

public class TrackSegmentCostMatrixCreator extends LAPTrackerCostMatrixCreator {


	private static final boolean PRUNING_OPTIMIZATION = true;

	private static final boolean DEBUG = false;
	/** The track segments. */
	protected List<SortedSet<Spot>> trackSegments;
	/** Holds the Spots in the middle of track segments (not at end or start). */
	protected List<Spot> middlePoints;
	/** The list of middle Spots which can participate in merge events. */
	protected List<Spot> mergingMiddlePoints;
	/** The list of middle Spots which can participate in splitting events. */
	protected List<Spot> splittingMiddlePoints;

	private Logger logger = Logger.VOID_LOGGER;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * 
	 * @param trackSegments A list of track segments (each track segment is 
	 * an <code>ArrayList</code> of <code>Spots</code>.
	 */

	public TrackSegmentCostMatrixCreator(final List<SortedSet<Spot>> trackSegments, final Map<String, Object> settings) {
		super(settings);
		this.trackSegments = trackSegments;
	}

	/*
	 * METHODS
	 */

	public void setLogger(Logger logger) {
		this.logger  = logger;
	}

	@Override
	public boolean checkInput() {
		if (trackSegments.isEmpty()) {
			errorMessage = "There are no track segments.";
			return false;
		}
		StringBuilder errorHolder = new StringBuilder();;
		if (!LAPUtils.checkSettingsValidity(settings, errorHolder )) {
			errorMessage = errorHolder.toString();
			return false;
		}
		return true;
	}


	/**
	 * Returns a list which holds references to all the splitting middle points
	 * within all the track segments. Notably, these middle points are in the
	 * same order here as they are referenced in the cost matrix (so, the 
	 * first column index in the merging section, index 0, corresponds to
	 * the Spot at index 0 in this ArrayList.
	 * @return 
	 * @return The <code>List</code> of middle <code>Spots</code>
	 */
	public List<Spot> getSplittingMiddlePoints() {
		return splittingMiddlePoints;
	}


	/**
	 * Returns a list which holds references to all the merging middle points
	 * within all the track segments. Notably, these middle points are in the
	 * same order here as they are referenced in the cost matrix (so, the 
	 * first column index in the merging section, index 0, corresponds to
	 * the Spot at index 0 in this ArrayList.
	 * @return The <code>List</code> of middle <code>Spots</code>
	 */
	public List<Spot> getMergingMiddlePoints() {
		return mergingMiddlePoints;
	}


	@Override
	public boolean process() {
		
		// 1 - Get parameter values
		final boolean allowSplitting = (Boolean) settings.get(KEY_ALLOW_TRACK_SPLITTING);
		final boolean allowMerging = (Boolean) settings.get(KEY_ALLOW_TRACK_MERGING);

		try {
			// 2 - Get a list of the middle points that can participate in merging and splitting

			if (allowMerging || allowSplitting) {
				middlePoints = getTrackSegmentMiddlePoints(trackSegments); 
			} else {
				// If we can skip matrix creation for merging and splitting, 
				// we will not try to keep a list of merging and splitting candidates.
				middlePoints = new ArrayList<Spot>(0);
				splittingMiddlePoints = middlePoints;
				mergingMiddlePoints   = middlePoints;
				
			}

			// 3 - Create cost matrices by quadrant

			// Top left quadrant
			Matrix topLeft = createTopLeftQuadrant();
			logger.setStatus("Completing cost matrix...");
			logger.setProgress(0.7f);
			double cutoff = getCutoff(topLeft);
			Matrix topRight = getAlternativeScores(topLeft.getRowDimension(), cutoff);
			Matrix bottomLeft = getAlternativeScores(topLeft.getColumnDimension(), cutoff);
			Matrix bottomRight = getLowerRight(topLeft, cutoff);

			// 4 - Fill in complete cost matrix by quadrant
			final int numCols = 2 * trackSegments.size() + splittingMiddlePoints.size() + mergingMiddlePoints.size();
			final int numRows = 2 * trackSegments.size() + splittingMiddlePoints.size() + mergingMiddlePoints.size();
			costs = new Matrix(numRows, numCols, 0);

			costs.setMatrix(0, topLeft.getRowDimension() - 1, 0, topLeft.getColumnDimension() - 1, topLeft);						// Gap closing
			costs.setMatrix(topLeft.getRowDimension(), numRows - 1, 0, topLeft.getColumnDimension() - 1, bottomLeft);				// Initiating and merging alternative
			costs.setMatrix(0, topLeft.getRowDimension() - 1, topLeft.getColumnDimension(), numCols - 1, topRight);					// Terminating and splitting alternative
			costs.setMatrix(topLeft.getRowDimension(), numRows - 1, topLeft.getColumnDimension(), numCols - 1, bottomRight);		// Lower right (transpose of gap closing, mathematically required for LAP)		
			return true;

		} catch (OutOfMemoryError ome) {
			errorMessage = "Not enough memory.";
			costs = null;
			return false;
		}

	}


	/**
	 * Create a List of candidate spots for splitting or merging events. 
	 * A desirable candidate is a spot belonging to a track with at least 2 spots.
	 * 
	 * @param trackSegments A List of track segments, where each segment is its own List of Spots.
	 * @return A List containing references to all suitable candidate Spots in the track segments.
	 */
	public List<Spot> getTrackSegmentMiddlePoints(List<SortedSet<Spot>> trackSegments) {
		int n_spots = 0;
		for (SortedSet<Spot> trackSegment : trackSegments) {
			n_spots += trackSegment.size();
		}
		List<Spot> middlePoints = new ArrayList<Spot>(n_spots);
		for (SortedSet<Spot> trackSegment : trackSegments) {
			
			if (trackSegment.size() > 1) {
				middlePoints.addAll(trackSegment);
			}
		}
		return middlePoints;
	}


	/**
	 * Creates the top left quadrant of the overall cost matrix, which contains the costs 
	 * for gap closing, merging, and splitting (as well as the empty middle section).
	 */
	private Matrix createTopLeftQuadrant() {
		Matrix topLeft, mergingScores, splittingScores, middle;

		// Create sub-matrices of top left quadrant (gap closing, merging, splitting, and empty middle

		logger.setStatus("Computing gap-closing costs...");
		logger.setProgress(0.55f);
		Matrix gapClosingScores = getGapClosingCostSubMatrix();

		// Get parameter values
		final boolean allowSplitting = (Boolean) settings.get(KEY_ALLOW_TRACK_SPLITTING);
		final boolean allowMerging = (Boolean) settings.get(KEY_ALLOW_TRACK_MERGING);
		final double blockingValue = (Double) settings.get(KEY_BLOCKING_VALUE);
		
		if (!allowMerging && !allowSplitting) {

			// We skip the rest and only keep this modest matrix.
			topLeft = gapClosingScores;
			mergingScores 	= new Matrix(0, 0);
			splittingScores = new Matrix(0, 0);
			middle 			= new Matrix(0, 0);

		} else {

			logger.setStatus("Computing merging costs...");
			logger.setProgress(0.6f);
			mergingScores = getMergingScores();

			logger.setStatus("Computing splitting costs...");
			logger.setProgress(0.65f);
			splittingScores = getSplittingScores();

			middle = new Matrix(splittingMiddlePoints.size(), mergingMiddlePoints.size(), blockingValue);

			// Initialize the top left quadrant
			final int numRows = trackSegments.size() + splittingMiddlePoints.size();
			final int numCols = trackSegments.size() + mergingMiddlePoints.size();
			topLeft = new Matrix(numRows, numCols);

			// Fill in top left quadrant
			topLeft.setMatrix(0, trackSegments.size() - 1, 0, trackSegments.size() - 1, gapClosingScores);
			topLeft.setMatrix(trackSegments.size(), numRows - 1, 0, trackSegments.size() - 1, splittingScores);
			topLeft.setMatrix(0, trackSegments.size() - 1, trackSegments.size(), numCols - 1, mergingScores);
			topLeft.setMatrix(trackSegments.size(), numRows - 1, trackSegments.size(), numCols - 1, middle);
		}
		
		if(DEBUG) {
			System.out.println("-- DEBUG information from TrackSegmentCostMatrixCreator --");
			System.out.println("Gap closing scores for "+trackSegments.size()+" segments:");
			LAPUtils.echoMatrix(gapClosingScores.getArray());
			System.out.println("Merging scores for "+trackSegments.size()+" segments and "+mergingMiddlePoints.size() +" merging points:");
			LAPUtils.echoMatrix(mergingScores.getArray());
			System.out.println("Splitting scores for "+splittingMiddlePoints.size()+" splitting points and "+trackSegments.size()+" segments:");
			LAPUtils.echoMatrix(splittingScores.getArray());
			System.out.println("Middle block:");
			LAPUtils.echoMatrix(middle.getArray());
		}

		return topLeft;
	}

	/**
	 * Uses a gap closing cost function to fill in the gap closing costs sub-matrix.
	 */
	private Matrix getGapClosingCostSubMatrix() {
		GapClosingCostFunction gapClosing = new GapClosingCostFunction(settings);
		return gapClosing.getCostFunction(trackSegments);
	}


	/**
	 * Uses a merging cost function to fill in the merging costs sub-matrix.
	 */
	private Matrix getMergingScores() {
		MergingCostFunction merging = new MergingCostFunction(settings);
		Matrix mergingScores = merging.getCostFunction(trackSegments, middlePoints);
		if (PRUNING_OPTIMIZATION) {
			mergingMiddlePoints = new ArrayList<Spot>();
			return pruneColumns(mergingScores, mergingMiddlePoints);
		} else {
			mergingMiddlePoints = middlePoints;
			return mergingScores;
		}
	}


	/**
	 * Iterates through the complete cost matrix, and removes any columns
	 * that only contain BLOCKED (there are no useful columns with non-BLOCKED
	 * values). Returns the pruned matrix with the empty columns.
	 */
	private Matrix pruneColumns (Matrix m, List<Spot> keptMiddleSpots) {
		final double blockingValue = (Double) settings.get(KEY_BLOCKING_VALUE);
		// Find all columns that contain a cost (a value != BLOCKED)
		double[][] full = m.copy().getArray();
		ArrayList<double[]> usefulColumns = new ArrayList<double[]>();
		for (int j = 0; j < m.getColumnDimension(); j++) {
			boolean containsCost = false;
			double[] curCol = new double[m.getRowDimension()];
			for (int i = 0; i < m.getRowDimension(); i++) {
				curCol[i] = full[i][j];
				if (full[i][j] < blockingValue) {
					containsCost = true;
				}
			}
			if (containsCost) {
				usefulColumns.add(curCol);
				keptMiddleSpots.add(middlePoints.get(j));
			}
		}

		// Convert ArrayList<double[]> -> double[][] -> Matrix
		double[][] pruned = new double[m.getRowDimension()][usefulColumns.size()];
		double[] col;
		for (int i = 0; i < usefulColumns.size(); i++) {
			col = usefulColumns.get(i);
			for (int j = 0; j < col.length; j++) {
				pruned[j][i] = col[j];
			}

		}

		return new Matrix(pruned);
	}

	/**
	 * Iterates through the complete cost matrix, and removes any rows
	 * that only contain BLOCKED (there are no useful rows with non-BLOCKED
	 * values). Returns the pruned matrix with the empty rows.
	 */
	private Matrix pruneRows (Matrix m, List<Spot> keptMiddleSpots) {
		final double blockingValue = (Double) settings.get(KEY_BLOCKING_VALUE);
		// Find all rows that contain a cost (a value != BLOCKED)
		double[][] full = m.copy().getArray();
		double[] curRow;
		ArrayList<double[]> usefulRows = new ArrayList<double[]>();
		for (int i = 0; i < m.getRowDimension(); i++) {
			boolean containsCost = false;
			curRow = full[i];
			for (int j = 0; j < m.getColumnDimension(); j++) {
				if (curRow[j] < blockingValue) {
					containsCost = true;
				}
			}
			if (containsCost) {
				usefulRows.add(curRow);
				keptMiddleSpots.add(middlePoints.get(i));
			}
		}

		// Convert ArrayList<double[]> -> double[][] -> Matrix
		double[][] pruned = new double[usefulRows.size()][m.getColumnDimension()];
		for (int i = 0; i < usefulRows.size(); i++) {
			pruned[i] = usefulRows.get(i);
		}

		if (pruned.length == 0) {
			return new Matrix(0,0);
		}
		return new Matrix(pruned);
	}


	/*
	 * Uses a splitting cost function to fill in the splitting costs submatrix.
	 */
	private Matrix getSplittingScores() {
		SplittingCostFunction splitting = new SplittingCostFunction(settings); 
		Matrix splittingScores = splitting.getCostFunction(trackSegments, middlePoints);
		if (PRUNING_OPTIMIZATION) {
			splittingMiddlePoints = new ArrayList<Spot>();
			return pruneRows(splittingScores, splittingMiddlePoints);
		} else {
			splittingMiddlePoints = middlePoints;
			return splittingScores;
		}
	}


	/**
	 * Calculates the CUTOFF_PERCENTILE cost of all costs in gap closing, merging, and
	 * splitting matrices to assign the top right and bottom left score matrices.
	 */
	private double getCutoff(Matrix m) {
		final double blockingValue = (Double) settings.get(KEY_BLOCKING_VALUE);
		final double cutoffPercentile = (Double) settings.get(KEY_CUTOFF_PERCENTILE);
		final double alternativeLinkingCostFactor = (Double) settings.get(KEY_ALTERNATIVE_LINKING_COST_FACTOR);
		
		// Get a list of all non-BLOCKED cost
		ArrayList<Double> scores = new ArrayList<Double>();
		for (int i = 0; i < m.getRowDimension(); i++) {
			for (int j = 0; j < m.getColumnDimension(); j++) {
				if (m.get(i, j) < blockingValue) {
					scores.add(m.get(i, j));
				}
			}
		}

		// Get the correct percentile of all non-BLOCKED cost values
		double[] scoreArr = new double[scores.size()];
		for (int i = 0; i < scores.size(); i++) {
			scoreArr[i] = scores.get(i);
		}
		double cutoff = TMUtils.getPercentile(scoreArr, cutoffPercentile); 
		if (!(cutoff < blockingValue)) {
			cutoff = 10.0d; // TODO how to fix this? In this case, there are no costs in the matrix, so nothing to calculate the cutoff values from
		}
		return alternativeLinkingCostFactor * cutoff;
	}
}
