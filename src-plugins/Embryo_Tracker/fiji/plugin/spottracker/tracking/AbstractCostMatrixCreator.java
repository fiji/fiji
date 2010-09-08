package fiji.plugin.spottracker.tracking;

import fiji.plugin.spottracker.Spot;
import Jama.Matrix;

/**
 * Contains the mutually shared fields, private functions used by the two 
 * cost matrix classes {@link LinkingCostMatrixCreator} and {@link TrackSegmentCostMatrixCreator}.
 * 
 * @author nperry
 *
 */
public abstract class AbstractCostMatrixCreator implements CostMatrixCreator {

	/** Used to prevent this assignment from being made during Hungarian Algorithm. */
	protected static final double BLOCKED = Double.MAX_VALUE;
	
	/** The cost matrix created by the class. */
	protected Matrix costs;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	
	
	@Override
	public double[][] getCostMatrix() {
		return costs.getArray();
	}

	
	@Override
	public String getErrorMessage() {
		return errorMessage;
	}
	
	
	/*
	 * Determines the Euclidean distance between two Spots.
	 */
	protected double euclideanDistance(Spot i, Spot j) {
		final float[] coordsI = i.getCoordinates();
		final float[] coordsJ = j.getCoordinates();
		double eucD = 0;

		for (int k = 0; k < coordsI.length; k++) {
			eucD += (coordsI[k] - coordsJ[k]) * (coordsI[k] - coordsJ[k]);
		}
		eucD = Math.sqrt(eucD);

		return eucD;
	}
	
	
	protected Matrix getLowerRight(int numEndSplit, int numStartMerge, double cutoff) {
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
	
	
	protected Matrix getAlternativeScores(int n, double cutoff) {
		final Matrix alternativeScores = new Matrix(n, n, BLOCKED);
		setDiagonalValue(alternativeScores, cutoff);
		return alternativeScores;
	}

	
	protected void setDiagonalValue(Matrix m, Double v) {
		for (int i = 0; i < m.getRowDimension(); i++) {
			m.set(i, i, v);
		}
	}
}
