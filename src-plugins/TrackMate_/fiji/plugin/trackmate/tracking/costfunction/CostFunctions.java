package fiji.plugin.trackmate.tracking.costfunction;

import Jama.Matrix;

/**
 * Interface for cost function classes, which take a {@link Matrix} and
 * fill in values according to the cost function.
 * 
 * @author Nicholas Perry
 *
 */
public interface CostFunctions {

	/** Fills in the supplied matrix using the information given at construction
	 * and the cost function. */
	public void applyCostFunction();
	
}
