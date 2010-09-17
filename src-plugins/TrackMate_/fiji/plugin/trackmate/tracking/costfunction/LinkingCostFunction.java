package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;
import Jama.Matrix;
import fiji.plugin.trackmate.Spot;

/**
 * <p>Linking cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is:
 * 
 * <p><code>d^2</code> , where d is the euclidean distance between two objects.
 * 
 * <p>The <b>thresholds</b> used are:
 * <ul>
 * <li>Must be within a certain distance.</li>
 * </ul>
 *  
 *  See equation (3) in the paper.
 *  
 * @author Nicholas Perry
 *
 */
public class LinkingCostFunction implements CostFunctions {
	
	/** The maximum distance away objects can be in order to be linked. */
	protected double maxDist;
	/** The value to use to block an assignment in the cost matrix. */
	protected double blocked;
	
	public LinkingCostFunction(double maxDist, double blocked) {
		this.maxDist = maxDist;
		this.blocked = blocked;
	}
	
	@Override
	public Matrix getCostFunction(final List<Spot> t0, final List<Spot> t1) {
		Spot s0 = null;			// Spot in t0
		Spot s1 = null;			// Spot in t1
		double d2;				// Holds Euclidean distance between s0 and s1
		double score;			// Holds the score
		final Matrix m = new Matrix(t0.size(), t1.size());
		
		for (int i = 0; i < t0.size(); i++) {
			
			s0 = t0.get(i);
			
			for (int j = 0; j < t1.size(); j++) {
				
				s1 = t1.get(j);
				d2 = s0.squareDistanceTo(s1);

				// Distance threshold
				if (d2 > maxDist*maxDist) {
					m.set(i, j, blocked);
					continue;
				}

				// Set score
				score = d2 + 2*Double.MIN_VALUE;	// score cannot be 0 in order to solve LAP, so add a small amount
				m.set(i, j, score);
			}
		}
		
		return m;
	}

}
