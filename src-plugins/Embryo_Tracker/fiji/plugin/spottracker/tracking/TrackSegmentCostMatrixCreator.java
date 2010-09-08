package fiji.plugin.spottracker.tracking;

import java.util.ArrayList;

import Jama.Matrix;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.Utils;

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
 * @author nperry
 *
 */
public class TrackSegmentCostMatrixCreator extends AbstractCostMatrixCreator {

	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_SEGMENTS = 1.0d;	// TODO make user input
	/** The factor used to create d and b in the paper, the alternative costs to linking
	 * objects. */
	protected static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;	// TODO make user input
	/** The maximum number of frames apart two segments can be 'gap closed.' */
	protected static final int GAP_CLOSING_TIME_WINDOW = 3; // TODO make user input.
	/** The minimum and maximum allowable intensity ratio cutoffs for merging and splitting. */
	protected static final double[] INTENSITY_RATIO_CUTOFFS = new double[] {0.5d, 4d}; // TODO make user input.
	/** The percentile used to calculate d and b cutoffs in the paper. */
	protected static final double CUTOFF_PERCENTILE = 0.9d;	// TODO make user input.
	
	/** The track segments. */
	protected ArrayList< ArrayList<Spot> > trackSegments;
	/** Holds the scores for gap closing, merging, and splitting to calculate cutoffs
	 * for alternatives. */
	protected ArrayList<Double> scores;
	/** Holds the Spots in the middle of track segments (not at end or start). */
	protected ArrayList<Spot> middlePoints;
	
	
	/**
	 * 
	 * @param trackSegments A list of track segments (each track segment is 
	 * an <code>ArrayList</code> of <code>Spots</code>.
	 */
	public TrackSegmentCostMatrixCreator(ArrayList< ArrayList<Spot> > trackSegments) {
		this.trackSegments = trackSegments;
		scores = new ArrayList<Double>();
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
	 * Returns an Arraylist which holds references to all the middle points
	 * within the track segments. Notably, these middle points are in the
	 * same order here as they are referenced in the cost matrix (so, the 
	 * first column index in the merging section, index 0, corresponds to
	 * the Spot at index 0 in this ArrayList.
	 * @return The <code>ArrayList</code> of middle <code>Spots</code>
	 */
	public ArrayList<Spot> getMiddlePoints() {
		return middlePoints;
	}

	
	@Override
	public boolean process() {
		
		// Confirm that checkInput() has been executed already.
		if (!inputChecked) {
			errorMessage = "You must run checkInput() before running process().";
			return false;
		}
		
		// 1 - Set various variables to help in the subsequent steps.
		final int numTrackSegments = trackSegments.size();						// Number of track segments
		middlePoints = getTrackSegmentMiddlePoints(trackSegments);				// A list of track segment middle points
		final int numMiddlePoints = middlePoints.size();						// Number of middle track segment Spots (not including start and end Spot)
		final int numEndSplit = numTrackSegments + numMiddlePoints;				// Number of end points + number of points considered for splitting
		final int numStartMerge = numTrackSegments + numMiddlePoints;			// Number of start points + number of points considered for merging
		final int totalLength = 2 * (numTrackSegments + numMiddlePoints);		// The total length of the final cost matrix

		// 2 - Initialize overall cost matrix
		costs = new Matrix(totalLength, totalLength);
		
		// 3 - Fill in scoring submatrices
		Matrix gapClosingScores = getGapClosingScores(numTrackSegments);
		Matrix mergingScores = getMergingScores(numTrackSegments, numMiddlePoints);
		Matrix splittingScores = getSplittingScores(numMiddlePoints, numTrackSegments);
		double cutoff = getEndPointCutoff();
		Matrix middle = new Matrix(numMiddlePoints, numMiddlePoints, BLOCKED);
		Matrix terminatingSplittingAlternativeScores = getAlternativeScores(numStartMerge, cutoff);
		Matrix initiatingMergingAlternativeScores = getAlternativeScores(numEndSplit, cutoff);

		// 4 - Fill in complete cost matrix using the submatrices just calculated
		costs.setMatrix(0, numTrackSegments - 1, 0, numTrackSegments - 1, gapClosingScores);							// Gap closing
		costs.setMatrix(0, numTrackSegments - 1, numTrackSegments, numStartMerge - 1, mergingScores);					// Merging
		costs.setMatrix(numTrackSegments, numEndSplit - 1, 0, numTrackSegments - 1, splittingScores);					// Splitting
		costs.setMatrix(numTrackSegments, numEndSplit - 1, numTrackSegments, numStartMerge - 1, middle);				// Middle (empty)
		costs.setMatrix(numEndSplit, totalLength - 1, 0, numStartMerge - 1, initiatingMergingAlternativeScores);		// Initiating and merging alternative
		costs.setMatrix(0, numEndSplit - 1, numStartMerge, totalLength - 1, terminatingSplittingAlternativeScores);		// Terminating and splitting alternative
		
		// 5 - Set the lower right submatrix
		Matrix lowerRight = getLowerRight(numEndSplit, numStartMerge, cutoff);
		costs.setMatrix(numEndSplit, totalLength - 1, numStartMerge, totalLength - 1, lowerRight);						// Lower right (transpose of gap closing, mathematically required for LAP)
		
		return true;
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
	 * Sets the scores for gap closing. Iterates through each pair of starts and ends for all
	 * track segments, and determines if they are within thresholds for gap closing, and if
	 * so calculates a score. 
	 * 
	 * The thresholds used are:
	 * 1. Must be within a certain frame window
	 * 2. Must be within a certain distance of each other
	 * 
	 * If a pair passes the thresholds, the score used is the Euclidean distance between
	 * them squared.
	 */
	private Matrix getGapClosingScores(int n) {
		
		// Initialize local variables
		final Matrix gapClosingScores = new Matrix(n, n);
		ArrayList<Spot> seg1, seg2;
		Spot end, start;
		double d, score;
		
		// Set the gap closing scores for each segment start and end pair
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				
				// If i and j are the same track segment, block it
				if (i == j) {
					gapClosingScores.set(i, j, BLOCKED);
					continue;
				}
				
				seg1 = trackSegments.get(i);
				seg2 = trackSegments.get(j);
				end = seg1.get(seg1.size() - 1);	// get last Spot of seg1
				start = seg2.get(0);				// get first Spot of seg2
				
				// Frame cutoff
				if (Math.abs(end.getFrame() - start.getFrame()) > GAP_CLOSING_TIME_WINDOW || end.getFrame() > start.getFrame()) {
					gapClosingScores.set(i, j, BLOCKED);
					continue;
				}
				
				// Radius cutoff
				d = euclideanDistance(end, start);
				if (d > MAX_DIST_SEGMENTS) {
					gapClosingScores.set(i, j, BLOCKED);
					continue;
				}
				
				score = d*d;
				scores.add(score);
				gapClosingScores.set(i, j, score);
			}
		}
		
		return gapClosingScores;
	}
	
	
	/*
	 * Sets the scores for merging. Iterates through each pair of starts and all middle points for all
	 * track segments, and determines if they are within thresholds for merging, and if
	 * so calculates a score. A middle point is a point within a track segment that is not a start nor
	 * an end.
	 * 
	 * The thresholds used are:
	 * 1. Must be within a certain frame window
	 * 2. Must be within a certain distance of each other
	 * 3. Must have a similar threshold ratio
	 * 
	 * If a pair passes the thresholds, the score used is defined by equation (5) in
	 * the paper.
	 * 
	 * The intensity ratio is defined as equation (6) in the paper.
	 */
	private Matrix getMergingScores(int n, int m) {
		
		// Initialize local variables
		final Matrix mergingScores = new Matrix(n, m);	
		double iRatio, d, s;
		int segLength;
		Spot end, middle;
		
		for (int i = 0; i < trackSegments.size(); i++) {
			for (int j = 0; j < middlePoints.size(); j++) {
				segLength = trackSegments.get(i).size();
				end = trackSegments.get(i).get(segLength - 1);
				middle = middlePoints.get(j);
				
				// Frame threshold - middle Spot must be one frame ahead of the end Spot
				if (middle.getFrame() != end.getFrame() + 1) {
					mergingScores.set(i, j, BLOCKED);
					continue;
				}
				
				// Radius threshold
				d = euclideanDistance(end, middle);
				if (d > MAX_DIST_SEGMENTS) {
					mergingScores.set(i, j, BLOCKED);
					continue;
				}

				iRatio = middle.getFeature(Feature.MEAN_INTENSITY) / (middle.getPrev().get(0).getFeature(Feature.MEAN_INTENSITY) + end.getFeature(Feature.MEAN_INTENSITY));
				
				// Intensity threshold -  must be within INTENSITY_RATIO_CUTOFFS ([min, max])
				if (iRatio > INTENSITY_RATIO_CUTOFFS[1] || iRatio < INTENSITY_RATIO_CUTOFFS[0]) {
					mergingScores.set(i, j, BLOCKED);
					continue;
				}
				
				if (iRatio >= 1) {
					s = d * d * iRatio;
				} else {
					s = d * d * ( 1 / (iRatio * iRatio) );
				}
				scores.add(s);
				mergingScores.set(i, j, s);
			}
		}
		
		return mergingScores;
		
	}

	
	private Matrix getSplittingScores(int n, int m) {
		
		// Initialize local variables
		final Matrix splittingScores = new Matrix(n, m);
		double iRatio, d, s;
		Spot start, middle;
		
		// Fill in splitting scores
		for (int i = 0; i < middlePoints.size(); i++) {
			for (int j = 0; j < trackSegments.size(); j++) {
				start = trackSegments.get(j).get(0);
				middle = middlePoints.get(i);
				
				// Frame threshold - middle Spot must be one frame behind of the start Spot
				if (middle.getFrame() != start.getFrame() - 1) {
					splittingScores.set(i, j, BLOCKED);
					continue;
				}
				
				// Radius threshold
				d = euclideanDistance(start, middle);
				if (d > MAX_DIST_SEGMENTS) {
					splittingScores.set(i, j, BLOCKED);
					continue;
				}

				iRatio = middle.getPrev().get(0).getFeature(Feature.MEAN_INTENSITY) / (middle.getFeature(Feature.MEAN_INTENSITY) + start.getFeature(Feature.MEAN_INTENSITY));
				
				// Intensity threshold -  must be within INTENSITY_RATIO_CUTOFFS ([min, max])
				if (iRatio > INTENSITY_RATIO_CUTOFFS[1] || iRatio < INTENSITY_RATIO_CUTOFFS[0]) {
					splittingScores.set(i, j, BLOCKED);
					continue;
				}

				if (iRatio >= 1) {
					s = d * d * iRatio;
				} else {
					s = d * d * ( 1 / (iRatio * iRatio) );
				}
				scores.add(s);
				splittingScores.set(i, j, s);
			}
		}
		
		return splittingScores;
	}
	

	
	
	
	
	/*
	 * Calculates the CUTOFF_PERCENTILE score of all scores in gap closing, merging, and
	 * splitting matrices to assign the top right and bottom left score matrices. See
	 * b and d cutoffs in paper for gap closing / merging / splitting.
	 */
	private double getEndPointCutoff() {
		ArrayList<Double> realScores = new ArrayList<Double>();
		for (int i = 0; i < scores.size(); i++) {
			if (Double.compare(scores.get(i), Double.NEGATIVE_INFINITY) != 0) {
				realScores.add(scores.get(i));
			}
		}
		double[] scoreArr = new double[realScores.size()];
		for (int i = 0; i < realScores.size(); i++) {
			scoreArr[i] = realScores.get(i);
		}
		double cutoff = Utils.getPercentile(scoreArr, CUTOFF_PERCENTILE); 
		return ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * cutoff;
	}
}
