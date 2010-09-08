package fiji.plugin.spottracker.tracking;

import java.util.ArrayList;

import Jama.Matrix;

import fiji.plugin.spottracker.Spot;

/**
 * <p>Creates the cost matrix described in Figure 1b in the paper.
 * 
 * <p>Jaqaman, K. et al. "Robust single-particle tracking in live-cell time-lapse sequences."
 * Nature Methods, 2008.
 * 
 * <p>The top left quadrant corresponds to the cost of links objects between frames,
 * the bottom left and top right quadrants correspond to alternative costs for linking
 * (allows no links to be made for objects), and the bottom right corner is mathematically
 * required for solving an LAP.
 * 
 * @author nperry
 *
 */
public class LinkingCostMatrixCreator extends AbstractCostMatrixCreator {

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
	/** The highest link score made across all frames. */
	protected double MAX_SCORE = Double.NEGATIVE_INFINITY;
	
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
		
		// Fill in scoring submatrices
		Matrix linkingScores = getLinkingScores();
		Matrix t0LinkingAlternatives = getAlternativeScores(t0.size(), ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * MAX_SCORE);
		Matrix t1LinkingAlternatives = getAlternativeScores(t1.size(), ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * MAX_SCORE);
		Matrix lowerRight = getLowerRight(t0.size(), t1.size(), ALTERNATIVE_OBJECT_LINKING_COST_FACTOR * MAX_SCORE);

		// Fill in complete cost matrix using the submatrices just calculated
		costs.setMatrix(0, t0.size() - 1, 0, t1.size() - 1, linkingScores);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, t1.size(), costs.getColumnDimension() - 1, lowerRight);
		costs.setMatrix(0, t0.size() - 1, t1.size(), costs.getColumnDimension() - 1, t0LinkingAlternatives);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, 0, t1.size() - 1, t1LinkingAlternatives);

		return true;
	}
	
	private Matrix getLinkingScores() {
		Matrix linkingScores = new Matrix(t0.size(), t1.size());
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
					linkingScores.set(i, j, score);
					if (score > MAX_SCORE) {
						MAX_SCORE = score;
					}
				} else {
					linkingScores.set(i, j, BLOCKED);
				}
			}
		}
		
		return linkingScores;
	}
}
