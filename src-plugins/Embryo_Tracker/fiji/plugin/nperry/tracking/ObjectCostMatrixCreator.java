package fiji.plugin.nperry.tracking;

import java.util.ArrayList;

import Jama.Matrix;

import fiji.plugin.nperry.Spot;

public class ObjectCostMatrixCreator implements CostMatrixCreator {

	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_OBJECTS = 1.0d;	// TODO make user input
	/** The factor used to create d and b in the paper, the alternative costs to linking
	 * objects. */
	protected static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;	// TODO make user input
	/** Used to prevent this assignment from being made during Hungarian Algorithm. */
	//protected static final double BLOCKED = Double.POSITIVE_INFINITY;
	protected static final double BLOCKED = Double.MAX_VALUE;
	/** The highest link score made across all frames. */
	protected static double MAX_SCORE = Double.NEGATIVE_INFINITY;

	/** The cost matrix. */
	protected Matrix costs;
	/** Stores a message describing an error incurred during use of the class. */
	protected String errorMessage;
	/** Stores whether the user has run checkInput() or not. */
	protected boolean inputChecked = false;
	/** The Spots belonging to time frame t. */
	protected ArrayList<Spot> t0;
	/** The Spots belonging to time frame t+1. */
	protected ArrayList<Spot> t1;
	/** The total number of Spots in time frames t and t+1. */
	protected int numSpots;
	
	public ObjectCostMatrixCreator(ArrayList<Spot> t0, ArrayList<Spot> t1) {
		this.t0 = t0;
		this.t1 = t1;
		this.numSpots = t0.size() + t1.size();
	}
	
	public double[][] getCostMatrix() {
		return this.costs.getArray();
	}

	@Override
	public boolean checkInput() {
		if (numSpots == 0) {
			errorMessage = "There are no objects!";
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
		
		// Initialize parent matrix and sub matrices
		costs = new Matrix(numSpots, numSpots);
		Matrix topLeft = new Matrix(t0.size(), t1.size());			// Linking
		Matrix topRight = new Matrix(t0.size(), t0.size());			// No linking (objects in t)
		Matrix bottomLeft =  new Matrix(t1.size(), t1.size());		// No linking (objects in t+1)
		Matrix bottomRight = new Matrix(t1.size(), t0.size());		// Nothing, but mathematically required for LAP
				
		// Top left quadrant
		Spot s0 = null;									// Spot in t0
		Spot s1 = null;									// Spot in t1
		double d = 0;									// Holds Euclidean distance between s0 and s1
		double score;
		for (int i = 0; i < t0.size(); i++) {
			s0 = t0.get(i);
			for (int j = 0; j < t1.size(); j++) {
				s1 = t1.get(j);
				d = euclideanDistance(s0, s1);

				if (d < MAX_DIST_OBJECTS) {
					score = d*d + 2*Double.MIN_VALUE;		// score cannot be 0 in order to solve LAP, so add a small amount
					topLeft.set(i, j, score);
					if (score > MAX_SCORE) {
						MAX_SCORE = score;
					}
				} else {
					topLeft.set(i, j, BLOCKED);
				}
			}
		}
		
		// Top right quadrant
		for (int i = 0; i < t0.size(); i++) {
			for (int j = 0; j < t0.size(); j++) {
				if (i == j) {
					topRight.set(i, j, ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * MAX_SCORE);
				} else {
					topRight.set(i, j, BLOCKED);
				}
				
			}
		}
		
		// Bottom left quadrant
		for (int i = 0; i < t1.size(); i++) {
			for (int j = 0; j < t1.size(); j++) {
				if (i == j) {
					bottomLeft.set(i, j, ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * MAX_SCORE);
				} else {
					bottomLeft.set(i, j, BLOCKED);
				}
				
			}
		}

		// Bottom right quadrant
		bottomRight = topLeft.transpose().copy();
		for (int i = 0; i < t1.size(); i++) {
			for (int j = 0; j < t0.size(); j++) {
				if (bottomRight.get(i, j) < BLOCKED) {
					bottomRight.set(i, j, Double.MIN_VALUE);
				}
			}
		}

		
		// Set submatrices of parent matrix with the submatrices we calculated separately
		costs.setMatrix(0, t0.size() - 1, 0, t1.size() - 1, topLeft);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, t1.size(), costs.getColumnDimension() - 1, bottomRight);
		costs.setMatrix(0, t0.size() - 1, t1.size(), costs.getColumnDimension() - 1, topRight);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, 0, t1.size() - 1, bottomLeft);
		
		return true;
	}
	
	/**
	 * This function can be used for equations (3) and (4) in the paper.
	 * 
	 * @param i Spot i
	 * @param j Spot j
	 * @return The Euclidean distance between Spots i and j.
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

}
