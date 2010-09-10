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

	/** The maximum distance away two Segments can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_SEGMENTS = 10.0d;	// TODO make user input
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
	/** The list of middle Spots which can participate in merge events. */
	protected ArrayList<Spot> mergingMiddlePoints;
	/** The list of middle Spots which can participate in splitting events. */
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
		
		// 2 - Get a list of the middle points that can participate in merging and splitting
		middlePoints = getTrackSegmentMiddlePoints(trackSegments);

		// 3 - Create cost matrices by quadrant
		
		// Top left quadrant
		Matrix topLeft = createTopLeftQuadrant();
		double cutoff = getCutoff(topLeft);
		Matrix topRight = getAlternativeScores(topLeft.getRowDimension(), cutoff);
		Matrix bottomLeft = getAlternativeScores(topLeft.getColumnDimension(), cutoff);
		Matrix bottomRight = getLowerRight(topLeft, cutoff);

		// 4 - Fill in complete cost matrix by quadrant
		final int numCols = 2 * trackSegments.size() + splittingMiddlePoints.size() + mergingMiddlePoints.size();
		final int numRows = 2 * trackSegments.size() + splittingMiddlePoints.size() + mergingMiddlePoints.size();
		costs = new Matrix(numRows, numCols, 0);

		costs.setMatrix(0, topLeft.getRowDimension() - 1, 0, topLeft.getColumnDimension() - 1, topLeft);							// Gap closing
		costs.setMatrix(topLeft.getRowDimension(), numRows - 1, 0, topLeft.getColumnDimension() - 1, bottomLeft);		// Initiating and merging alternative
		costs.setMatrix(0, topLeft.getRowDimension() - 1, topLeft.getColumnDimension(), numCols - 1, topRight);		// Terminating and splitting alternative
		costs.setMatrix(topLeft.getRowDimension(), numRows - 1, topLeft.getColumnDimension(), numCols - 1, bottomRight);						// Lower right (transpose of gap closing, mathematically required for LAP)		
		
		//printMatrix(costs, "Step 2 costs");
		
		return true;
	}
	
	private Matrix createTopLeftQuadrant() {
		
		// Create submatrices of top left quadrant (gap closing, merging, splitting, and empty middle
		Matrix gapClosingScores = getGapClosingCostSubMatrix(trackSegments.size());
		Matrix mergingScores = getMergingScores(trackSegments.size());
		Matrix splittingScores = getSplittingScores(trackSegments.size());
		Matrix middle = new Matrix(splittingMiddlePoints.size(), mergingMiddlePoints.size(), BLOCKED);
		
		// Initialize the top left quadrant
		final int numRows = trackSegments.size() + splittingMiddlePoints.size();
		final int numCols = trackSegments.size() + mergingMiddlePoints.size();
		Matrix topLeft = new Matrix(numRows, numCols);
		
		// Fill in top left quadrant
		topLeft.setMatrix(0, trackSegments.size() - 1, 0, trackSegments.size() - 1, gapClosingScores);
		topLeft.setMatrix(trackSegments.size(), numRows - 1, 0, trackSegments.size() - 1, splittingScores);
		topLeft.setMatrix(0, trackSegments.size() - 1, trackSegments.size(), numCols - 1, mergingScores);
		topLeft.setMatrix(trackSegments.size(), numRows - 1, trackSegments.size(), numCols - 1, middle);
		
		return topLeft;
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
		final Matrix mergingScores = new Matrix(n, middlePoints.size());
		MergingCostFunction merging = new MergingCostFunction(mergingScores, trackSegments, middlePoints, MAX_DIST_SEGMENTS, BLOCKED, INTENSITY_RATIO_CUTOFFS);
		merging.applyCostFunction();
		return pruneColumns(mergingScores, mergingMiddlePoints);
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
		
		if (pruned.length == 0) {
			return new Matrix(0,0);
		}
		return new Matrix(pruned);
	}
	
	
	/*
	 * Uses a splitting cost function to fill in the splitting costs submatrix.
	 */
	private Matrix getSplittingScores(int n) {
		final Matrix splittingScores = new Matrix(middlePoints.size(), n);	
		SplittingCostFunction splitting = new SplittingCostFunction(splittingScores, trackSegments, middlePoints, MAX_DIST_SEGMENTS, BLOCKED, INTENSITY_RATIO_CUTOFFS);
		splitting.applyCostFunction();
		return pruneRows(splittingScores, splittingMiddlePoints);
	}
	
	
	/*
	 * Calculates the CUTOFF_PERCENTILE cost of all costs in gap closing, merging, and
	 * splitting matrices to assign the top right and bottom left score matrices.
	 */
	private double getCutoff(Matrix m) {
		
		// Get a list of all non-BLOCKED cost
		ArrayList<Double> scores = new ArrayList<Double>();
		for (int i = 0; i < m.getRowDimension(); i++) {
			for (int j = 0; j < m.getColumnDimension(); j++) {
				if (m.get(i, j) < BLOCKED) {
					scores.add(m.get(i, j));
				}
			}
		}
		
		// Get the correct percentile of all non-BLOCKED cost values
		double[] scoreArr = new double[scores.size()];
		for (int i = 0; i < scores.size(); i++) {
			scoreArr[i] = scores.get(i);
		}
		double cutoff = Utils.getPercentile(scoreArr, CUTOFF_PERCENTILE); 
		if (!(cutoff < BLOCKED)) {
			cutoff = 10.0d; // TODO how to fix this?
		}
		return ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * cutoff;
	}
}
