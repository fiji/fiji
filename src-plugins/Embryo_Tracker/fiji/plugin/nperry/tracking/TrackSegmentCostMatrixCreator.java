package fiji.plugin.nperry.tracking;

import java.util.ArrayList;

import Jama.Matrix;

import fiji.plugin.nperry.Spot;

public class TrackSegmentCostMatrixCreator implements CostMatrixCreator {

	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_OBJECTS = 5.0f;	// TODO make user input
	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_SEGMENTS = 5.0f;	// TODO make user input
	/** The factor used to create d and b in the paper, the alternative costs to linking
	 * objects. */
	protected static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05f;	// TODO make user input
	/** Used to prevent this assignment from being made during Hungarian Algorithm. */
	protected static final double BLOCKED = Double.POSITIVE_INFINITY;
	/** The maximum number of frames apart two segments can be 'gap closed.' */
	protected static final int GAP_CLOSING_TIME_WINDOW = 3; // TODO make user input.
	
	/** The track segments. */
	protected ArrayList< ArrayList<Spot> > trackSegments;
	/** The cost matrix for the gap closing / merging / splitting step */
	protected Matrix costs;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	
	public TrackSegmentCostMatrixCreator(ArrayList< ArrayList<Spot> > trackSegments) {
		this.trackSegments = trackSegments;
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

	@Override
	public boolean process() {
		if (!inputChecked) {
			errorMessage = "You must run checkInput() before running process().";
			return false;
		}
		
		// 0 - Set various variables to help in the following steps.
		int numTrackSegmentPoints = countTrackSegmentPoints(trackSegments);
		int numTrackSegments = trackSegments.size();
		int length = 2 * numTrackSegments + numTrackSegmentPoints;
		int colOne = 0;
		int colTwo = numTrackSegments;
		int colThree = numTrackSegments + numTrackSegmentPoints;
		int colThreeEnd  = 2 * numTrackSegments + numTrackSegmentPoints - 1;
		int rowOne = 0;
		int rowTwo = numTrackSegments;
		int rowThree = numTrackSegments + numTrackSegmentPoints;
		int rowThreeEnd = 2 * numTrackSegments + numTrackSegmentPoints - 1;
		
		// 1 - Initialize parent matrix and submatrices
		costs = new Matrix(length, length);																		// Overall matrix
		Matrix topLeft = new Matrix(numTrackSegments, numTrackSegments);										// Gap closing
		Matrix topMiddle = new Matrix(numTrackSegments, numTrackSegmentPoints);									// Merging
		Matrix topRight = new Matrix (numTrackSegments, numTrackSegments);										// Terminating
		Matrix middleLeft = new Matrix(numTrackSegmentPoints, numTrackSegments);								// Splitting
		Matrix middle = new Matrix(numTrackSegmentPoints, numTrackSegmentPoints, Double.POSITIVE_INFINITY);		// Nothing
		Matrix middleRight = new Matrix(numTrackSegmentPoints, numTrackSegments);								// Alternative
		Matrix bottomLeft = new Matrix(numTrackSegments, numTrackSegments);										// Terminating
		Matrix bottomMiddle = new Matrix(numTrackSegments, numTrackSegmentPoints);								// Alternative
		Matrix bottomRight = new Matrix(numTrackSegments, numTrackSegments);									// Lower right block (nothing)
		
		// 2 - Fill in submatrices
		fillInTopLeft(topLeft, numTrackSegments);
		fillInTopMiddle(topMiddle);
		
		
		// 3 - Fill in cost matrix with submatrices

		// Set top row
		costs.setMatrix(rowOne, rowTwo - 1, colOne, colTwo - 1, topLeft);
		costs.setMatrix(rowOne, rowTwo - 1, colTwo, colThree - 1, topMiddle);
		costs.setMatrix(rowOne, rowTwo - 1, colThree, colThreeEnd, topRight);

		// Set middle row
		costs.setMatrix(rowTwo, rowThree - 1, colOne, colTwo - 1, middleLeft);
		costs.setMatrix(rowTwo, rowThree - 1, colTwo, colThree - 1, middle);
		costs.setMatrix(rowTwo, rowThree - 1, colThree, colThreeEnd, middleRight);
		
		// Set bottom row
		costs.setMatrix(rowThree, rowThreeEnd, colOne, colTwo - 1, bottomLeft);
		costs.setMatrix(rowThree, rowThreeEnd, colTwo, colThree - 1, bottomMiddle);
		costs.setMatrix(rowThree, rowThreeEnd, colThree, colThreeEnd, bottomRight);
		
		costs.print(4, 2);
		
		return true;
	}

	/*
	 * Fills in the Gap Closing submatrix described in the paper (upper left).
	 * The score used in this matrix is the same as the one described in the paper:
	 * If the start of a track and the end of another are within the frame distance
	 * maximum, as well as a radius distance, their score is the square of the distance
	 * between the two.
	 */
	private void fillInTopLeft(Matrix topLeft, int numTrackSegments) {
		ArrayList<Spot> seg1, seg2;
		Spot end, start;
		double d, score, intensityRatio;
		
		
		for (int i = 0; i < numTrackSegments; i++) {
			for (int j = 0; j < numTrackSegments; j++) {
				
				// If i and j are the same track segment, block it
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
				
				intensityRatio = 
				
				
				// Intensity cutoff
				
				
				score = d*d;
				topLeft.set(i, j, score);
			}
		}
	}
	
	
	private void fillInTopMiddle(Matrix topMiddle) {
		
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
	 * Counts the number of individual points making up the track segments. For example,
	 * if there are two track segments, one with length seven and the other with length
	 * ten, this method would return the value 17.
	 */
	private int countTrackSegmentPoints(ArrayList< ArrayList<Spot> > trackSegments) {
		int count = 0;
		for (ArrayList<Spot> trackSegment : trackSegments) {
			count += trackSegment.size();
		}
		return count;
	}
	
}
