package fiji.plugin.spottracker.tracking;

import java.util.ArrayList;

import Jama.Matrix;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.Utils;

/**
 * 
 * @author nperry
 *
 */
public class TrackSegmentCostMatrixCreator implements CostMatrixCreator {

	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_SEGMENTS = 1.0f;	// TODO make user input
	/** The factor used to create d and b in the paper, the alternative costs to linking
	 * objects. */
	protected static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05f;	// TODO make user input
	/** Used to prevent this assignment from being made during Hungarian Algorithm. */
	protected static final double BLOCKED = Double.MAX_VALUE;
	/** The maximum number of frames apart two segments can be 'gap closed.' */
	protected static final int GAP_CLOSING_TIME_WINDOW = 3; // TODO make user input.
	/** The minimum and maximum allowable intensity ratio cutoffs for merging and splitting. */
	protected static final double[] INTENSITY_RATIO_CUTOFFS = new double[] {0.5d, 4d}; // TODO make user input.
	/** The percentile used to calculate d and b cutoffs in the paper. */
	protected static final double CUTOFF_PERCENTILE = 0.9d;
	
	/** The track segments. */
	protected ArrayList< ArrayList<Spot> > trackSegments;
	/** The cost matrix for the gap closing / merging / splitting step */
	protected Matrix costs;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	/** Holds the scores for gap closing, merging, and splitting to calculate cutoffs
	 * for alternatives. */
	protected ArrayList<Double> scores;
	/** Holds the Spots in the middle of track segments (not at end or start). */
	protected ArrayList<Spot> middlePoints;
	
	public TrackSegmentCostMatrixCreator(ArrayList< ArrayList<Spot> > trackSegments) {
		this.trackSegments = trackSegments;
		scores = new ArrayList<Double>();
	}
	
	@Override
	public double[][] getCostMatrix() {
		return costs.getArray();
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

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}
	
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
		Matrix terminatingSplittingAlternativeScores = getTerminatingSplittingAlternativeScores(numStartMerge, cutoff);
		Matrix initiatingMergingAlternativeScores = getInitiatingMergingAlternativeScores(numEndSplit, cutoff);

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
	
	
	private Matrix getTerminatingSplittingAlternativeScores(int n, double cutoff) {
		final Matrix terminatingSplittingAlternativeScores = new Matrix(n, n, BLOCKED);
		setDiagonalValue(terminatingSplittingAlternativeScores, cutoff);
		return terminatingSplittingAlternativeScores;
	}
	
	private Matrix getInitiatingMergingAlternativeScores(int n, double cutoff) {
		final Matrix initiatingMergingAlternativeScores = new Matrix(n, n, BLOCKED);
		setDiagonalValue(initiatingMergingAlternativeScores, cutoff);
		return initiatingMergingAlternativeScores;
	}
	
	private void setDiagonalValue(Matrix m, Double v) {
		for (int i = 0; i < m.getRowDimension(); i++) {
			m.set(i, i, v);
		}
	}

	private Matrix getLowerRight(int numEndSplit, int numStartMerge, double cutoff) {
		Matrix lowerRight = costs.getMatrix(0, numEndSplit - 1, 0, numStartMerge - 1);
		lowerRight = lowerRight.transpose();
		for (int i = 0; i < lowerRight.getRowDimension(); i++) {
			for (int j = 0; j < lowerRight.getColumnDimension(); j++) {
				if (lowerRight.get(i, j) < BLOCKED) {
					lowerRight.set(i, j, cutoff);
				}
			}
		}
		return lowerRight;
	}
	
	/*
	 * This function can be used for equations (3) and (4) in the paper.
	 */
	private double euclideanDistance(Spot i, Spot j) {
		final float[] coordsI = i.getCoordinates();
		final float[] coordsJ = j.getCoordinates();
		double eucD = 0;

		for (int k = 0; k < coordsI.length; k++) {
			eucD += (coordsI[k] - coordsJ[k]) * (coordsI[k] - coordsJ[k]);
		}
		eucD = Math.sqrt(eucD);

		return eucD;
	}
	
	
	/*
	 * Creates ArrayList of middle points in all track segments. A middle point is a point
	 * in a track segment that is neither a start nor an end point. So, track segments
	 * of length 1 and 2 can have no middle points. Thus, we add middle points only for 
	 * track segments of length 3 or larger. 
	 */
	private ArrayList<Spot> getTrackSegmentMiddlePoints(ArrayList< ArrayList<Spot> > trackSegments) {
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
