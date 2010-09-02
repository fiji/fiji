package fiji.plugin.nperry.tracking;

import java.util.ArrayList;

import Jama.Matrix;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;
import fiji.plugin.nperry.Utils;

public class TrackSegmentCostMatrixCreator implements CostMatrixCreator {

	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_SEGMENTS = 2.0f;	// TODO make user input
	/** The factor used to create d and b in the paper, the alternative costs to linking
	 * objects. */
	protected static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05f;	// TODO make user input
	/** Used to prevent this assignment from being made during Hungarian Algorithm. */
	protected static final double BLOCKED = Double.POSITIVE_INFINITY;
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
		if (!inputChecked) {
			errorMessage = "You must run checkInput() before running process().";
			return false;
		}
		
		// 1 - Set various variables to help in the subsequent steps.
		final int numTrackSegments = trackSegments.size();										// Number of track segments
		middlePoints = getTrackSegmentMiddlePoints(trackSegments);		// A list of track segment middle points
		final int numMiddlePoints = middlePoints.size();										// Number of middle track segment Spots (not including start and end Spot)
		final int length = 2 * numTrackSegments + numMiddlePoints;								// The total length of the final cost matrix
		final int[] cols = new int[] {0, numTrackSegments, numTrackSegments + numMiddlePoints, 2 * numTrackSegments + numMiddlePoints - 1};
		final int[] rows = new int[] {0, numTrackSegments, numTrackSegments + numMiddlePoints, 2 * numTrackSegments + numMiddlePoints - 1};

		// 2 - Initialize overall cost matrix
		costs = new Matrix(length, length);
		
		// 3 - Fill in Gap closing (top left), merging (top middle), and splitting scores (middle left).
		fillInTopLeft(numTrackSegments, rows, cols);
		fillInTopMiddle(numTrackSegments, numMiddlePoints, rows, cols, middlePoints);
		fillInMiddleLeft(numTrackSegments, numMiddlePoints, rows, cols, middlePoints);
		double cutoff = getEndPointCutoff();	// the score (a cutoff) for other matrices depends on these three
		
		// 4 - Fill in terminating (top right), initiating alternatives (bottom left)
		fillInTopRight(cutoff, numTrackSegments, rows, cols);
		fillInBottomLeft(cutoff, numTrackSegments, rows, cols);
		
		// 5 - Fill in the bottom right and middle (either blank, or mathematically required by LAP, respectively)
		Matrix topLeft = costs.getMatrix(rows[0], rows[1] - 1, cols[0], cols[1] - 1);
		fillInBottomRight(topLeft, numTrackSegments, rows, cols);
		fillInMiddle(numMiddlePoints, rows, cols);
		
		// 6 - Fill in Merging and Splitting alternatives (bottom middle and middle right, respectively)
		//Matrix middleLeft = costs.getMatrix(rows[1], rows[2] - 1, cols[0], cols[1] - 1);
		//Matrix topMiddle = costs.getMatrix(rows[0], rows[1] - 1, cols[1], cols[2] - 1);
		//fillInMiddleRight(middleLeft, numMiddlePoints, numTrackSegments, rows, cols, middlePoints);
		//fillInBottomMiddle(topMiddle, numMiddlePoints, numTrackSegments, rows, cols, middlePoints);
		fillInMiddleRight(numMiddlePoints, numTrackSegments, rows, cols, middlePoints);
		fillInBottomMiddle(numMiddlePoints, numTrackSegments, rows, cols, middlePoints);
		
		costs.print(4,2);
		return true;
	}

	/*
	 *  Gap closing submatrix.
	 */
	private void fillInTopLeft(int numTrackSegments, int[] rows, int[] cols) {
		final Matrix topLeft = new Matrix(numTrackSegments, numTrackSegments);
		setGapClosingScores(topLeft, numTrackSegments);
		costs.setMatrix(rows[0], rows[1] - 1, cols[0], cols[1] - 1, topLeft);
	}
	
	/*
	 * Merging submatrix.
	 */
	private void fillInTopMiddle(int numTrackSegments, int numMiddlePoints, int[] rows, int[] cols, ArrayList<Spot> middlePoints) {
		final Matrix topMiddle = new Matrix(numTrackSegments, numMiddlePoints);	
		setMergingScores(topMiddle, middlePoints);
		costs.setMatrix(rows[0], rows[1] - 1, cols[1], cols[2] - 1, topMiddle);
	}
	
	/*
	 * Splitting submatrix.
	 */
	private void fillInMiddleLeft(int numTrackSegments, int numMiddlePoints, int[] rows, int[] cols, ArrayList<Spot> middlePoints) {
		final Matrix middleLeft = new Matrix(numMiddlePoints, numTrackSegments);
		setSplittingScores(middleLeft, middlePoints);
		costs.setMatrix(rows[1], rows[2] - 1, cols[0], cols[1] - 1, middleLeft);
	}
	
	/*
	 * Top right alternatives (d)
	 */
	private void fillInTopRight(double cutoff, int numTrackSegments, int[] rows, int[] cols) {
		final Matrix topRight = new Matrix (numTrackSegments, numTrackSegments);
		setStartsEndsAlternative(topRight, cutoff);
		costs.setMatrix(rows[0], rows[1] - 1, cols[2], cols[3], topRight);
	}
	
	/*
	 * Bottom left alternatives (b)
	 */
	private void fillInBottomLeft(double cutoff, int numTrackSegments, int[] rows, int[] cols) {
		final Matrix bottomLeft = new Matrix(numTrackSegments, numTrackSegments);	
		setStartsEndsAlternative(bottomLeft, cutoff);
		costs.setMatrix(rows[2], rows[3], cols[0], cols[1] - 1, bottomLeft);
	}
	
	/*
	 * Lower right block
	 */
	private void fillInBottomRight(Matrix topLeft, int numTrackSegments, int[] rows, int[] cols) {
		final Matrix bottomRight = new Matrix(numTrackSegments, numTrackSegments);						
		setLowerRightBlock(bottomRight, topLeft);
		costs.setMatrix(rows[2], rows[3], cols[2], cols[3], bottomRight);
	}

	/*
	 * Middle
	 */
	private void fillInMiddle(int numMiddlePoints, int[] rows, int[] cols) {
		final Matrix middle = new Matrix(numMiddlePoints, numMiddlePoints, Double.POSITIVE_INFINITY);
		costs.setMatrix(rows[1], rows[2] - 1, cols[1], cols[2] - 1, middle);
	}
	
	/*
	 * Splitting alternatives
	 */
	private void fillInMiddleRight(int numMiddlePoints, int numTrackSegments, int[] rows, int[] cols, ArrayList<Spot> middlePoints) {
		final Matrix middleRight = new Matrix(numMiddlePoints, numTrackSegments);						
		//setSplittingAlternatives(middleRight, middleLeft, middlePoints);
		setSplittingAlternatives(middleRight, middlePoints);
		costs.setMatrix(rows[1], rows[2] - 1, cols[2], cols[3], middleRight);
	}
	
	/*
	 * Merging alternatives
	 */
	private void fillInBottomMiddle(int numMiddlePoints, int numTrackSegments, int[] rows, int[] cols, ArrayList<Spot> middlePoints) {
		final Matrix bottomMiddle = new Matrix(numTrackSegments, numMiddlePoints);				
		//setMergingAlternatives(bottomMiddle, topMiddle, middlePoints);
		setMergingAlternatives(bottomMiddle, middlePoints);
		costs.setMatrix(rows[2], rows[3], cols[1], cols[2] - 1, bottomMiddle);
		
	}
	
	/*
	 * Fills in the Gap Closing submatrix described in the paper (upper left).
	 * The score used in this matrix is the same as the one described in the paper:
	 * If the start of a track and the end of another are within the frame distance
	 * maximum, as well as a radius distance, their score is the square of the distance
	 * between the two.
	 */
	private void setGapClosingScores(Matrix topLeft, int numTrackSegments) {
		ArrayList<Spot> seg1, seg2;
		Spot end, start;
		double d, score;
		
		for (int i = 0; i < numTrackSegments; i++) {
			for (int j = 0; j < numTrackSegments; j++) {
				
				/* If i and j are the same track segment, block it (undefined to close a 
				 * gap between the starts and ends of the same segment) */
				if (i == j) {
					topLeft.set(i, j, BLOCKED);
					continue;
				}
				
				seg1 = trackSegments.get(i);
				seg2 = trackSegments.get(j);
				end = seg1.get(seg1.size() - 1);	// get last Spot of seg1
				start = seg2.get(0);				// get first Spot of seg2
				
				// Frame cutoff
				if (Math.abs(end.getFrame() - start.getFrame()) > GAP_CLOSING_TIME_WINDOW ) {
					topLeft.set(i, j, BLOCKED);
					continue;
				}
				
				// Radius cutoff
				d = euclideanDistance(end, start);
				if (d > MAX_DIST_SEGMENTS) {
					topLeft.set(i, j, BLOCKED);
					continue;
				}
				
				score = d*d;
				scores.add(score);
				topLeft.set(i, j, score);
			}
		}
	}
	
	
	/*
	 * Fills in the top middle submatrix of the paper (merging). The score used is
	 * the same as described in equation (5) in the paper. The cutoffs are also described
	 * in the paper, but are reproduced here:
	 * 1. The end of a segment must be one time frame before the middle spot.
	 * 2. The two spots must be within a certain radius.
	 * 3. There is an intensity threshold.
	 */
	private void setMergingScores(Matrix topMiddle, ArrayList<Spot> middlePoints) {
		double iRatio, d, m;
		int segLength;
		Spot end, middle;
		for (int i = 0; i < trackSegments.size(); i++) {
			for (int j = 0; j < middlePoints.size(); j++) {
				segLength = trackSegments.get(i).size();
				end = trackSegments.get(i).get(segLength - 1);
				middle = middlePoints.get(j);
				
				// Frame threshold - middle Spot must be one frame ahead of the end Spot
				if (middle.getFrame() != end.getFrame() + 1) {
					topMiddle.set(i, j, BLOCKED);
					continue;
				}
				
				// Radius threshold
				d = euclideanDistance(end, middle);
				if (d > MAX_DIST_SEGMENTS) {
					topMiddle.set(i, j, BLOCKED);
					continue;
				}

				iRatio = middle.getFeature(Feature.MEAN_INTENSITY) / (middle.getPrev().get(0).getFeature(Feature.MEAN_INTENSITY) + end.getFeature(Feature.MEAN_INTENSITY));
				
				// Intensity threshold -  must be within INTENSITY_RATIO_CUTOFFS ([min, max])
				if (iRatio > INTENSITY_RATIO_CUTOFFS[1] || iRatio < INTENSITY_RATIO_CUTOFFS[0]) {
					topMiddle.set(i, j, BLOCKED);
					continue;
				}
				
				if (iRatio >= 1) {
					m = d * d * iRatio;
				} else {
					m = d * d * ( 1 / (iRatio * iRatio) );
				}
				scores.add(m);
				topMiddle.set(i, j, m);
			}
		}
	}
	
	
	/*
	 * Fills in the middle left submatrix of the paper (splitting). The score used is
	 * the same as described in equation (5) in the paper. The cutoffs are also described
	 * in the paper, but are reproduced here:
	 * 1. The end of a segment must be one time frame before the middle spot.
	 * 2. The two spots must be within a certain radius.
	 * 3. There is an intensity threshold.
	 */
	private void setSplittingScores(Matrix middleLeft, ArrayList<Spot> middlePoints) {
		double iRatio, d, s;
		Spot start, middle;
		for (int i = 0; i < middlePoints.size(); i++) {
			for (int j = 0; j < trackSegments.size(); j++) {
				start = trackSegments.get(j).get(0);
				middle = middlePoints.get(i);
				
				// Frame threshold - middle Spot must be one frame behind of the start Spot
				if (middle.getFrame() != start.getFrame() - 1) {
					middleLeft.set(i, j, BLOCKED);
					continue;
				}
				
				// Radius threshold
				d = euclideanDistance(start, middle);
				if (d > MAX_DIST_SEGMENTS) {
					middleLeft.set(i, j, BLOCKED);
					continue;
				}

				iRatio = middle.getPrev().get(0).getFeature(Feature.MEAN_INTENSITY) / (middle.getFeature(Feature.MEAN_INTENSITY) + start.getFeature(Feature.MEAN_INTENSITY));
				
				// Intensity threshold -  must be within INTENSITY_RATIO_CUTOFFS ([min, max])
				if (iRatio > INTENSITY_RATIO_CUTOFFS[1] || iRatio < INTENSITY_RATIO_CUTOFFS[0]) {
					middleLeft.set(i, j, BLOCKED);
					continue;
				}
				
				if (iRatio >= 1) {
					s = d * d * iRatio;
				} else {
					s = d * d * ( 1 / (iRatio * iRatio) );
				}
				scores.add(s);
				middleLeft.set(i, j, s);
			}
		}
	}
	
	private void setStartsEndsAlternative(Matrix m, double cutoff) {
		for (int i = 0; i < m.getRowDimension(); i++) {
			for (int j = 0; j < m.getColumnDimension(); j++) {
				if (i==j) {
					m.set(i, j, cutoff);
				} else {
					m.set(i, j, BLOCKED);
				}
			}
		}
	}
	
	private void setLowerRightBlock(Matrix bottomRight, Matrix topLeft) {
		Matrix temp = topLeft.transpose().copy();
		for (int i = 0; i < temp.getRowDimension(); i++) {
			for (int j = 0; j < temp.getColumnDimension(); j++) {
				if (temp.get(i, j) < BLOCKED) {
					bottomRight.set(i, j, Double.MIN_VALUE);
				} else {
					bottomRight.set(i, j, BLOCKED);
				}
			}
		}
	}
	
	private void setSplittingAlternatives(Matrix middleRight, ArrayList<Spot> middlePoints) {
		double iRatio, avgDisp;
		Spot curr;
		for (int i = 0; i < middleRight.getRowDimension(); i++) {
			for (int j = 0; j < middleRight.getColumnDimension(); j++) {
				//if (i == j) {
					curr = middlePoints.get(i);
					iRatio = curr.getPrev().get(0).getFeature(Feature.MEAN_INTENSITY) / curr.getFeature(Feature.MEAN_INTENSITY);
					avgDisp = getAverateDisplacement(curr); 
					if (iRatio >= 1) {
						middleRight.set(i, j, avgDisp * avgDisp * iRatio);
					} else {
						middleRight.set(i, j, avgDisp * avgDisp * (1 / (iRatio * iRatio)));
					}
				//} else {
				//	middleRight.set(i, j, BLOCKED);
				//}
			}
		}
	}
	
	private void setMergingAlternatives(Matrix bottomMiddle, ArrayList<Spot> middlePoints) {
		double iRatio, avgDisp;
		Spot curr;
		for (int i = 0; i < bottomMiddle.getRowDimension(); i++) {
			for (int j = 0; j < bottomMiddle.getColumnDimension(); j++) {
				//if (i == j) {
					curr = middlePoints.get(i);
					iRatio = curr.getFeature(Feature.MEAN_INTENSITY) / curr.getPrev().get(0).getFeature(Feature.MEAN_INTENSITY);
					avgDisp = getAverateDisplacement(curr); 
					if (iRatio >= 1) {
						bottomMiddle.set(i, j, avgDisp * avgDisp * iRatio);
					} else {
						bottomMiddle.set(i, j, avgDisp * avgDisp * (1 / (iRatio * iRatio)));
					}
				//} else {
				//	bottomMiddle.set(i, j, BLOCKED);
				//}
			}
		}
	}
	
	/*
	 * Gets the average frame-to-frame displacement of the Spots in a track segment.
	 */
	private double getAverateDisplacement(Spot s) {
		double d = 0;
		int count = 0;
		Spot next, prev, curr;
		
		// all previous
		curr = s;
		while (curr.getPrev().size() != 0) {
			prev = curr.getPrev().get(0);
			d += euclideanDistance(curr, prev);
			curr = prev;
			count++;
		}
		
		// all subsequent
		curr = s;
		while(curr.getNext().size() != 0) {
			next = curr.getNext().get(0);
			d += euclideanDistance(curr, next);
			curr = next;
			count++;
		}
		
		return d / count;
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
		return cutoff;
	}
	
}
