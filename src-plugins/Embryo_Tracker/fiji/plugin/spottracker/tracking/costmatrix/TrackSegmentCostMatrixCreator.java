package fiji.plugin.spottracker.tracking.costmatrix;

import java.util.ArrayList;

import Jama.Matrix;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.Utils;
import fiji.plugin.spottracker.tracking.costfunction.GapClosingCostFunction;
import fiji.plugin.spottracker.tracking.costfunction.MergingCostFunction;
import fiji.plugin.spottracker.tracking.costfunction.SplittingCostFunction;

/**
 * <p>Creates the cost matrix <b>roughly</b> described in Figure 1c in the paper.
 * 
 * <p>Jaqaman, K. et al. "Robust single-particle tracking in live-cell time-lapse sequences."
 * Nature Methods, 2008.
 * 
 * <p>This matrix is notably different than the one described in the paper. This is because
 * the cost matrix described in 1c in the paper <b>is not </b> the cost matrix they use in their
 * matlab implementation.
 * 
 * <p>The cost matrix used in the matlab implementation by the authors, and used here, is described
 * as follows:
 * 
 * <p> The overall matrix can be divided into <u>four quadrants</u>, outlined below. Each 
 * quadrant has dimensions (number of track segments + number of middle points) x (number of track segments + number of middle points).
 * A middle point is defined as a Spot in a track segment that is neither the start nor end. So a 
 * track segment of length two has no middle points.
 * 
 * <ul>
 * <li>
 * <p><b>Top left</b>: Contains scores for gap closing, merging, splitting, and "blank" region.
 * <br><br>
 * <p>This quadrant can also be further subdivided into four smaller submatrices:
 *
 * <ul>
 * <li><i>Gap closing (top left)</i>: has dimensions (number of track segments) x (number of track segments), 
 * and contains the scores for linking the ends of track segments to the starts of other track segments
 * as described in the paper.</li>
 * 
 * <li><i>Merging (top right)</i>: has (number of track segment) rows and (number of middle
 * points) columns. Contains scores for linking the end of a track segment into the
 * middle of another (a merge).</li>
 * 
 * <li><i>Splitting (top left)</i>: has (number of middle points) rows and (number of track
 * segments columns). Contains scores for linking the start of a track segment into the
 * middle of another (a split).</li>
 * 
 * <li><i>Empty (Bottom right)</i>: has dimensions (number of middle points) x (number of middle points), and is blocked, 
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
 * the top left quadrant is inverted, and all non-blocked values are replaced with the cutoff
 * value used in the bottom left and top right quadrants.
 * <br><br>
 * </li>
 * </ul>
 *
 * @author Nicholas Perry
 *
 */
public class TrackSegmentCostMatrixCreator extends LAPTrackerCostMatrixCreator {

	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_SEGMENTS = 1.0d;	// TODO make user input
	/** The factor used to create d and b in the paper, the alternative costs to linking
	 * objects. */
	protected static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;	// TODO make user input
	/** The maximum number of frames apart two segments can be 'gap closed.' */
	protected static final int GAP_CLOSING_TIME_WINDOW = 2; // TODO make user input.
	/** The minimum and maximum allowable intensity ratio cutoffs for merging and splitting. */
	protected static final double[] INTENSITY_RATIO_CUTOFFS = new double[] {0.5d, 4d}; // TODO make user input.
	/** The percentile used to calculate d and b cutoffs in the paper. */
	protected static final double CUTOFF_PERCENTILE = 0.9d;	// TODO make user input.
	
	/** The track segments. */
	protected ArrayList< ArrayList<Spot> > trackSegments;
	/** Holds the Spots in the middle of track segments (not at end or start). */
	protected ArrayList<Spot> middlePoints;
	
	protected ArrayList<Spot> mergingMiddlePoints;
	
	protected ArrayList<Spot> splittingMiddlePoints;
	
	
	/**
	 * 
	 * @param trackSegments A list of track segments (each track segment is 
	 * an <code>ArrayList</code> of <code>Spots</code>.
	 */
	public TrackSegmentCostMatrixCreator(ArrayList< ArrayList<Spot> > trackSegments) {
		this.trackSegments = trackSegments;
		this.mergingMiddlePoints = new ArrayList<Spot>();
		this.splittingMiddlePoints = new ArrayList<Spot>();
	}
	
	
	@Override
	public boolean checkInput() {
		if (trackSegments.isEmpty()) {
			errorMessage = "There are no track segments.";
			return false;
		}
		
		inputChecked = true;
		return true;
	}

	
	/**
	 * Returns an Arraylist which holds references to all the splitting middle points
	 * within the track segments. Notably, these middle points are in the
	 * same order here as they are referenced in the cost matrix (so, the 
	 * first column index in the merging section, index 0, corresponds to
	 * the Spot at index 0 in this ArrayList.
	 * @return The <code>ArrayList</code> of middle <code>Spots</code>
	 */
	public ArrayList<Spot> getSplittingMiddlePoints() {
		return splittingMiddlePoints;
	}
	
	
	/**
	 * Returns an Arraylist which holds references to all the merging middle points
	 * within the track segments. Notably, these middle points are in the
	 * same order here as they are referenced in the cost matrix (so, the 
	 * first column index in the merging section, index 0, corresponds to
	 * the Spot at index 0 in this ArrayList.
	 * @return The <code>ArrayList</code> of middle <code>Spots</code>
	 */
	public ArrayList<Spot> getMergingMiddlePoints() {
		return mergingMiddlePoints;
	}

	
	@Override
	public boolean process() {
		
		// 1 - Confirm that checkInput() has been executed already.
		if (!inputChecked) {
			errorMessage = "You must run checkInput() before running process().";
			return false;
		}
		
		// 2 - Set various variables to help in the subsequent steps.
		final int numTrackSegments = trackSegments.size();						// Number of track segments
		middlePoints = getTrackSegmentMiddlePoints(trackSegments);				// A list of track segment middle points

		
		// Create top left quadrant
		//Matrix topLeftQuadrant = createTopLeftSubmatrix();
		
		// Create submatrices for top left quadrant
		Matrix gapClosingScores = getGapClosingCostSubMatrix(numTrackSegments);
		Matrix mergingScores = getMergingScores(numTrackSegments);
		Matrix splittingScores = getSplittingScores(numTrackSegments);
		double cutoff = getCutoff(gapClosingScores, mergingScores, splittingScores); //TODO fix
		Matrix middle = new Matrix(splittingMiddlePoints.size(), mergingMiddlePoints.size(), BLOCKED);
		
		final int numEndSplit = numTrackSegments + splittingMiddlePoints.size();			// Number of end points + number of points considered for splitting
		final int numStartMerge = numTrackSegments + mergingMiddlePoints.size();			// Number of start points + number of points considered for merging
		
		// Create submatrices for top right quadrant
		Matrix terminatingSplittingAlternativeScores = getAlternativeScores(numEndSplit, cutoff);
		
		// Create submatrices for bottom left quadrant
		Matrix initiatingMergingAlternativeScores = getAlternativeScores(numStartMerge, cutoff);

		printMatrix(terminatingSplittingAlternativeScores, "term/split");
		printMatrix(initiatingMergingAlternativeScores, "init/merge");
		printMatrix(middle, "middle");
		printMatrix(mergingScores, "merging");
		printMatrix(splittingScores, "splitting");
		printMatrix(gapClosingScores, "gap closing");
		
		// Fill in complete cost matrix using the submatrices just calculated
		final int numCols = 2 * trackSegments.size() + splittingMiddlePoints.size() + mergingMiddlePoints.size();
		final int numRows = 2 * trackSegments.size() + splittingMiddlePoints.size() + mergingMiddlePoints.size();
		System.out.println(String.format("num cols = %d, num rows = %d", numCols, numRows));
		System.out.println(String.format("tracksegments = %d, merging points = %d, splitting points = %d", trackSegments.size(), mergingMiddlePoints.size(), splittingMiddlePoints.size()));

		costs = new Matrix(numRows, numCols, 0);
		printMatrix(costs, "costs");

		costs.setMatrix(0, numTrackSegments - 1, 0, numTrackSegments - 1, gapClosingScores);							// Gap closing
		costs.setMatrix(0, numTrackSegments - 1, numTrackSegments, numStartMerge - 1, mergingScores);					// Merging
		costs.setMatrix(numTrackSegments, numEndSplit - 1, 0, numTrackSegments - 1, splittingScores);					// Splitting
		costs.setMatrix(numTrackSegments, numEndSplit - 1, numTrackSegments, numStartMerge - 1, middle);				// Middle (empty)
		costs.setMatrix(numEndSplit, numRows - 1, 0, numStartMerge - 1, initiatingMergingAlternativeScores);		// Initiating and merging alternative
		costs.setMatrix(0, numEndSplit - 1, numStartMerge, numCols - 1, terminatingSplittingAlternativeScores);		// Terminating and splitting alternative
		
		
		
		// Create submatrices for bottom right quadrant (need costs to be filled in with other submatrices already)
		Matrix lowerRight = getLowerRight(numEndSplit, numStartMerge, cutoff);
		//costs.setMatrix(numEndSplit, totalLength - 1, numStartMerge, totalLength - 1, lowerRight);						// Lower right (transpose of gap closing, mathematically required for LAP)
		
		return true;
	}
	
	private void printMatrix (Matrix m, String s) {
		Matrix n = m.copy();
		System.out.println(s);
		for (int i = 0; i < n.getRowDimension(); i++) {
			for (int j = 0; j < n.getColumnDimension(); j++) {
				if (n.get(i, j) == Double.MAX_VALUE) {
					n.set(i, j, Double.NaN);
				}
			}
		}
		n.print(4,2);
	}
	
	/**
	 * Creates ArrayList of middle points in all track segments. A middle point is a point
	 * in a track segment that is neither a start nor an end point. So, track segments
	 * of length 1 and 2 can have no middle points. Thus, we add middle points only for 
	 * track segments of length 3 or larger. 
	 * 
	 * @param trackSegments An ArrayList of track segments, where each segment is its own ArrayList of Spots.
	 * @return An ArrayList containing references to all the middle Spots in the track segments.
	 */
	public ArrayList<Spot> getTrackSegmentMiddlePoints(ArrayList< ArrayList<Spot> > trackSegments) {
		ArrayList<Spot> middlePoints = new ArrayList<Spot>();
		for (ArrayList<Spot> trackSegment : trackSegments) {
			if (trackSegment.size() >= 3) {
				for (int i = 1; i < trackSegment.size() - 1; i++) { 	// Extract middle Spots only.
					middlePoints.add(trackSegment.get(i));
				}
			}
		}
		return middlePoints;
	}

	
	/*
	 * Uses a gap closing cost function to fill in the gap closing costs submatrix.
	 */
	private Matrix getGapClosingCostSubMatrix(int n) {
		final Matrix gapClosingScores = new Matrix(n, n);
		GapClosingCostFunction gapClosing = new GapClosingCostFunction(gapClosingScores, GAP_CLOSING_TIME_WINDOW, MAX_DIST_SEGMENTS, BLOCKED, trackSegments);
		gapClosing.applyCostFunction();
		return gapClosingScores;
	}
	
	
	/*
	 * Uses a merging cost function to fill in the merging costs submatrix.
	 */
	private Matrix getMergingScores(int n) {
		//final Matrix mergingScores = new Matrix(n, m);	
		final Matrix mergingScores = new Matrix(n, middlePoints.size());
		//findParticipatingMergingMiddlePoints();	
		//final Matrix mergingScores = new Matrix(n, mergingMiddlePoints.size());	
		//MergingCostFunction merging = new MergingCostFunction(mergingScores, trackSegments, mergingMiddlePoints, MAX_DIST_SEGMENTS, BLOCKED, INTENSITY_RATIO_CUTOFFS);
		MergingCostFunction merging = new MergingCostFunction(mergingScores, trackSegments, middlePoints, MAX_DIST_SEGMENTS, BLOCKED, INTENSITY_RATIO_CUTOFFS);
		merging.applyCostFunction();
		return pruneColumns(mergingScores, mergingMiddlePoints);
		//return mergingScores;
	}

	private Matrix pruneColumns (Matrix m, ArrayList<Spot> keptMiddleSpots) {

		// Find all columns that contain a cost (a value != BLOCKED)
		double[][] full = m.copy().getArray();
		double[] curCol = new double[m.getRowDimension()];
		ArrayList<double[]> usefulColumns = new ArrayList<double[]>();
		for (int j = 0; j < m.getColumnDimension(); j++) {
			boolean containsCost = false;
			for (int i = 0; i < m.getRowDimension(); i++) {
				curCol[i] = full[i][j];
				if (full[i][j] < BLOCKED) {
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

	private Matrix pruneRows (Matrix m, ArrayList<Spot> keptMiddleSpots) {

		// Find all rows that contain a cost (a value != BLOCKED)
		double[][] full = m.copy().getArray();
		double[] curRow;
		ArrayList<double[]> usefulRows = new ArrayList<double[]>();
		for (int i = 0; i < m.getRowDimension(); i++) {
			boolean containsCost = false;
			curRow = full[i];
			for (int j = 0; j < m.getColumnDimension(); j++) {
				if (curRow[j] < BLOCKED) {
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

		return new Matrix(pruned);
	}
	
	
	/*
	 * Uses a splitting cost function to fill in the splitting costs submatrix.
	 */
	private Matrix getSplittingScores(int n) {
		//final Matrix splittingScores = new Matrix(n, m);
		//findParticipatingSplittingMiddlePoints();	
		//final Matrix splittingScores = new Matrix(splittingMiddlePoints.size(), n);	
		final Matrix splittingScores = new Matrix(middlePoints.size(), n);	
		SplittingCostFunction splitting = new SplittingCostFunction(splittingScores, trackSegments, middlePoints, MAX_DIST_SEGMENTS, BLOCKED, INTENSITY_RATIO_CUTOFFS);
		//SplittingCostFunction splitting = new SplittingCostFunction(splittingScores, trackSegments, splittingMiddlePoints, MAX_DIST_SEGMENTS, BLOCKED, INTENSITY_RATIO_CUTOFFS);
		splitting.applyCostFunction();
		return pruneRows(splittingScores, splittingMiddlePoints);
		//return splittingScores;
	}
	
	
	/*
	 * Calculates the CUTOFF_PERCENTILE cost of all costs in gap closing, merging, and
	 * splitting matrices to assign the top right and bottom left score matrices.
	 */
	private double getCutoff(Matrix m, Matrix n, Matrix o) {
		
		// Get a list of all non-BLOCKED cost
		ArrayList<Double> scores = new ArrayList<Double>();
		Matrix[] matrices = new Matrix[] {m, n, o};
		for (Matrix matrix : matrices) {
			for (int i = 0; i < matrix.getRowDimension(); i++) {
				for (int j = 0; j < matrix.getColumnDimension(); j++) {
					if (matrix.get(i, j) < BLOCKED) {
						scores.add(matrix.get(i, j));
					}
				}
			}
		}
		
		// Get the correct percentile of all non-BLOCKED cost values
		double[] scoreArr = new double[scores.size()];
		for (int i = 0; i < scores.size(); i++) {
			scoreArr[i] = scores.get(i);
		}
		double cutoff = Utils.getPercentile(scoreArr, CUTOFF_PERCENTILE); 
		return ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * cutoff;
	}
}
