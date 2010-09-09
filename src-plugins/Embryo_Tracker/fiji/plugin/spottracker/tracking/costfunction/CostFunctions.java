package fiji.plugin.spottracker.tracking.costfunction;

import Jama.Matrix;

/**
 * Interface for cost function classes, which take a {@link Matrix} and
 * fill in values according to the cost function.
 * 
 * @author Nicholas Perry
 *
 */
public interface CostFunctions {

	public void applyCostFunction();
	
}
