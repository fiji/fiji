package fiji.plugin.spottracker.tracking.costmatrix;

import java.util.ArrayList;

import Jama.Matrix;

import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.tracking.costfunction.LinkingCostFunction;

/**
 * <p>Creates the cost matrix described in Figure 1b in the paper.
 * 
 * <p>Jaqaman, K. et al. "Robust single-particle tracking in live-cell time-lapse sequences."
 * Nature Methods, 2008.
 * 
 * <p>The top left quadrant contains the costs to links objects between frames,
 * the bottom left and top right quadrants correspond to alternative costs for linking
 * (allows no links to be made betwen objects), and the bottom right corner is mathematically
 * required for solving an LAP.
 * 
 * @author Nicholas Perry
 *
 */
public class LinkingCostMatrixCreator extends LAPTrackerCostMatrixCreator {

	/** The maximum distance away two Spots in consecutive frames can be in order 
	 * to be linked. */
	protected static final double MAX_DIST_OBJECTS = 1.0d;	// TODO make user input
	/** The factor used to create d and b in the paper, the alternative costs to linking
	 * objects. */
	protected static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;	// TODO make user input

	/** The Spots belonging to time frame t. */
	protected ArrayList<Spot> t0;
	/** The Spots belonging to time frame t+1. */
	protected ArrayList<Spot> t1;
	/** The total number of Spots in time frames t and t+1. */
	protected int numSpots;
	
	/**
	 * 
	 * @param t0 The spots in frame t
	 * @param t1 The spots in frame t+1
	 */
	public LinkingCostMatrixCreator(ArrayList<Spot> t0, ArrayList<Spot> t1) {
		this.t0 = t0;
		this.t1 = t1;
		this.numSpots = t0.size() + t1.size();
		this.costs = new Matrix(numSpots, numSpots);
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
	public boolean process() {
		if (!inputChecked) {
			errorMessage = "You must run checkInput() before running process().";
			return false;
		}
		
		// 1 - Fill in quadrants
		Matrix topLeft = getLinkingCostSubMatrix();
		final double cutoff = ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * getMaxScore(topLeft);
		Matrix topRight = getAlternativeScores(t0.size(), cutoff);
		Matrix bottomLeft = getAlternativeScores(t1.size(), cutoff);
		Matrix bottomRight = getLowerRight(topLeft, cutoff);

		// 2 - Fill in complete cost matrix by quadrant
		costs.setMatrix(0, t0.size() - 1, 0, t1.size() - 1, topLeft);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, t1.size(), costs.getColumnDimension() - 1, bottomRight);
		costs.setMatrix(0, t0.size() - 1, t1.size(), costs.getColumnDimension() - 1, topRight);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, 0, t1.size() - 1, bottomLeft);

		return true;
	}
	
	/*
	 * Gets the max score in a matrix m.
	 */
	private double getMaxScore(Matrix m) {
		double max = Double.NEGATIVE_INFINITY;
		
		for (int i = 0; i < m.getRowDimension(); i++) {
			for (int j = 0; j < m.getColumnDimension(); j++) {
				if (m.get(i, j) > max && m.get(i, j) < BLOCKED) {
					max = m.get(i, j);
				}
			}
		}
		
		return max;
	}
	
	/*
	 * Creates a submatrix which holds the linking scores between objects, and returns it.
	 */
	private Matrix getLinkingCostSubMatrix() {
		Matrix linkingScores = new Matrix(t0.size(), t1.size());
		//CostFunctions.linkingScores(linkingScores, t0, t1, MAX_DIST_OBJECTS);
		LinkingCostFunction linkingCosts = new LinkingCostFunction(linkingScores, t0, t1, MAX_DIST_OBJECTS, BLOCKED);
		linkingCosts.applyCostFunction();
		return linkingScores;
	}
}
