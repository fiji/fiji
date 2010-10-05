package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;
import java.util.SortedSet;

import Jama.Matrix;
import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

/**
 * <p>Gap closing cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is:
 * 
 * <p><code>d^2</code>, where d is the euclidean distance between two objects.
 * 
 * <p>The <b>thresholds</b> used are:
 * <ul>
 * <li>Must be within a certain time.</li>
 * <li>Must be within a certain distance.</li>
 * </ul>
 * 
 * See equation (4) in the paper.
 * 
 * @author Nicholas Perry
 */
public class GapClosingCostFunction {

	/** The time cutoff, and distance cutoff, respectively */
	protected double timeCutoff, maxDist;
	/** The value to use to block an assignment in the cost matrix. */
	protected double blocked;
	
	public GapClosingCostFunction(double timeCutoff, double maxDist, double blocked) {
		this.timeCutoff = timeCutoff;
		this.maxDist = maxDist;
		this.blocked = blocked;
	}
	
	public Matrix getCostFunction(List<SortedSet<Spot>> trackSegments) {
		SortedSet<Spot> seg1, seg2;
		Spot end, start;
		float tend, tstart;
		double d2;
		int n = trackSegments.size();
		final Matrix m = new Matrix(n, n);
		
		// Set the gap closing scores for each segment start and end pair
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				
				// If i and j are the same track segment, block it
				if (i == j) {
					m.set(i, j, blocked);
					continue;
				}
				
				seg1 = trackSegments.get(i);
				seg2 = trackSegments.get(j);
				end = seg1.last();				// get last Spot of seg1
				start = seg2.first();			// get first Spot of seg2
				tend = end.getFeature(Feature.POSITION_T); // we want at least tstart > tend
				tstart = start.getFeature(Feature.POSITION_T);
				
				// Frame cutoff
				if (tstart - tend > timeCutoff || tend >= tstart) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Radius cutoff
				d2 = start.squareDistanceTo(end);
				if (d2 > maxDist*maxDist) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Set score
				m.set(i, j, d2);
			}
		}
		
		return m;
	}
}
