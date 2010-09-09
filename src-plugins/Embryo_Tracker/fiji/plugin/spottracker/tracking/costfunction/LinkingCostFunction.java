package fiji.plugin.spottracker.tracking.costfunction;

import java.util.ArrayList;

import Jama.Matrix;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.Utils;
import fiji.plugin.spottracker.tracking.LAPTracker;

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
	
	/** The cost matrix. */
	protected Matrix m;
	/** The objects belonging to frame t and frame t+1, respectively. */
	protected ArrayList<Spot> t0, t1;
	/** The maximum distance away objects can be in order to be linked. */
	protected double maxDist;
	/** The value to use to block an assignment in the cost matrix. */
	protected double blocked;
	
	public LinkingCostFunction(Matrix m, ArrayList<Spot> t0, ArrayList<Spot> t1, double maxDist, double blocked) {
		this.m = m;
		this.t0 = t0;
		this.t1 = t1;
		this.maxDist = maxDist;
		this.blocked = blocked;
	}
	
	@Override
	public void applyCostFunction() {
		Spot s0 = null;			// Spot in t0
		Spot s1 = null;			// Spot in t1
		double d = 0;			// Holds Euclidean distance between s0 and s1
		double score;			// Holds the score
		
		for (int i = 0; i < t0.size(); i++) {
			s0 = t0.get(i);
			for (int j = 0; j < t1.size(); j++) {
				s1 = t1.get(j);
				d = Utils.euclideanDistance(s0, s1);

				// Distance threshold
				if (d > maxDist) {
					m.set(i, j, blocked);
					continue;
				}

				// Set score
				score = d*d + 2*Double.MIN_VALUE;	// score cannot be 0 in order to solve LAP, so add a small amount
				m.set(i, j, score);
			}
		}
	}
}
