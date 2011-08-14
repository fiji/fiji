package fiji.plugin.trackmate.tracking.costmatrix;

import java.util.List;

import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.costfunction.LinkingCostFunction;

/**
 * <p>Creates the cost matrix described in Figure 1b in the paper.
 * 
 * <p>Jaqaman, K. et al. "Robust single-particle tracking in live-cell time-lapse sequences."
 * Nature Methods, 2008.
 * 
 * <p>The top left quadrant contains the costs to links objects between frames,
 * the bottom left and top right quadrants correspond to alternative costs for linking
 * (allows no links to be made between objects), and the bottom right corner is mathematically
 * required for solving an LAP.
 * 
 * @author Nicholas Perry
 *
 */
public class LinkingCostMatrixCreator extends LAPTrackerCostMatrixCreator {

	/** The Spots belonging to time frame t. */
	protected final List<Spot> t0;
	/** The Spots belonging to time frame t+1. */
	protected final List<Spot> t1;
	/** The total number of Spots in time frames t and t+1. */
	protected int numSpots;

	/*
	 * CONSTRUCTOR
	 */


	public LinkingCostMatrixCreator(final List<Spot> t0, final List<Spot> t1, final TrackerSettings settings) {
		super(settings);
		this.t0 = t0;
		this.t1 = t1;
		this.numSpots = t0.size() + t1.size();
		this.costs = new Matrix(numSpots, numSpots);
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput() {
		inputChecked = true;
		return true;
	}


	@Override
	public boolean process() {
		if (!inputChecked) {
			errorMessage = "You must run checkInput() before running process().";
			return false;
		}

		// Deal with special cases:

		if (numSpots == 0) {
			// 0.0 - No spots -> nothing to do
			costs = new Matrix(0, 0);
			return true;
		}

		if (t1.size() == 0) {
			// 0.1 - No spots in late frame -> termination only.
			costs = new Matrix(t0.size(), t0.size(), settings.blockingValue);
			for (int i = 0; i < t0.size(); i++) {
				costs.set(i, i, 0);
			}
			return true;
		}

		if (t0.size() == 0) {
			// 0.1 - No spots in early frame -> initiation only.
			costs = new Matrix(t1.size(), t1.size(), settings.blockingValue);
			for (int i = 0; i < t1.size(); i++) {
				costs.set(i, i, 0);
			}
			return true;
		}


		// 1 - Fill in quadrants
		Matrix topLeft = getLinkingCostSubMatrix();
		final double cutoff = settings.alternativeObjectLinkingCostFactor * getMaxScore(topLeft);
		Matrix topRight = getAlternativeScores(t0.size(), cutoff);
		Matrix bottomLeft = getAlternativeScores(t1.size(), cutoff);
		Matrix bottomRight = getLowerRight(topLeft, cutoff);

		// 2 - Fill in complete cost matrix by quadrant
		costs.setMatrix(0, t0.size() - 1, 0, t1.size() - 1, topLeft);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, t1.size(), costs.getColumnDimension() - 1, bottomRight);
		costs.setMatrix(0, t0.size() - 1, t1.size(), costs.getColumnDimension() - 1, topRight);
		costs.setMatrix(t0.size(), costs.getRowDimension() - 1, 0, t1.size() - 1, bottomLeft);

		//printMatrix(costs, "linking costs");

		return true;
	}

	/**
	 * Gets the max score in a matrix m.
	 */
	private double getMaxScore(Matrix m) {
		double max = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < m.getRowDimension(); i++) {
			for (int j = 0; j < m.getColumnDimension(); j++) {
				if (m.get(i, j) > max && m.get(i, j) < settings.blockingValue) {
					max = m.get(i, j);
				}
			}
		}

		return max;
	}

	/**
	 * Creates a sub-matrix which holds the linking scores between objects, and returns it.
	 */
	private Matrix getLinkingCostSubMatrix() {
		LinkingCostFunction linkingCosts = new LinkingCostFunction(settings);
		return linkingCosts.getCostFunction(t0, t1);
	}
}
