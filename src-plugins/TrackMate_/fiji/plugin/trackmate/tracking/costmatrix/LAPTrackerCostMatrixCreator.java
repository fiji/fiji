package fiji.plugin.trackmate.tracking.costmatrix;

import fiji.plugin.trackmate.tracking.TrackerSettings;
import Jama.Matrix;

/**
 * Contains the mutually shared fields and private functions used by the two 
 * cost matrix classes {@link LinkingCostMatrixCreator} and {@link TrackSegmentCostMatrixCreator}
 * that are used with the {@link LAPTracker} class..
 * 
 * @author Nicholas Perry
 *
 */
public abstract class LAPTrackerCostMatrixCreator implements CostMatrixCreator {

	/** The cost matrix created by the class. */
	protected Matrix costs;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	/** The settings to comply to create a cost matrix. */
	protected  TrackerSettings settings;
	
	/*
	 * CONSTRUCTOR
	 */
	
	protected LAPTrackerCostMatrixCreator(TrackerSettings settings) {
		this.settings = settings;
	}
	
	
	
	@Override
	public double[][] getCostMatrix() {
		return costs.getArray();
	}

	
	@Override
	public String getErrorMessage() {
		return errorMessage;
	}
	
	
	/**
	 * Takes the submatrix of costs defined by rows 0 to numRows - 1 and columns
	 * 0 to numCols - 1, inverts it, and sets any non-BLOCKED value to be cutoff.
	 * 
	 * The reasoning for this is explained in the supplementary notes of the paper,
	 * but basically it has to be made this way so that the LAP is solvable
	 * (mathematical requirement).
	 */
	protected Matrix getLowerRight(Matrix topLeft, double cutoff) {
		Matrix lowerRight = topLeft.copy();
		lowerRight = lowerRight.transpose();
		for (int i = 0; i < lowerRight.getRowDimension(); i++) {
			for (int j = 0; j < lowerRight.getColumnDimension(); j++) {
				if (lowerRight.get(i, j) < settings.blockingValue) {
					lowerRight.set(i, j, cutoff);
				}
			}
		}
		return lowerRight;
	}
	
	
	/**
	 * Sets alternative scores in a new matrix along a diagonal. The new
	 * matrix is n x n, and is set to BLOCKED everywhere except along
	 * the diagonal that runs from top left to bottom right.
	 */
	protected Matrix getAlternativeScores(int n, double cutoff) {
		final Matrix alternativeScores = new Matrix(n, n, settings.blockingValue);
		
		// Set the cutoff along the diagonal (top left to bottom right)
		for (int i = 0; i < alternativeScores.getRowDimension(); i++) {
			alternativeScores.set(i, i, cutoff);
		}
		
		return alternativeScores;
	}
}
