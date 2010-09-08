package fiji.plugin.spottracker.tracking.costfunction;

import java.util.ArrayList;

import Jama.Matrix;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.tracking.LAPTracker;

/**
 * <p>Gap closing cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is:
 * 
 * <p><code>d^2</code> , where d is the euclidean distance between two objects.
 * 
 * <p>The <b>thresholds</b> used are:
 * <ul>
 * <li>Must be within a certain number of frames.</li>
 * <li>Must be within a certain distance.</li>
 * </ul>
 * 
 * See equation (4) in the paper.
 * 
 * @author Nicholas Perry
 *
 */
public class GapClosingCostFunction extends LAPTrackerCostFunction {

	/** The cost matrix. */
	protected Matrix m;
	/** The frame cutoff, and distance cutoff, respectively */
	protected double frameCutoff, maxDist;
	/** The list of track segments, where each segment is a list of Spots. */
	protected ArrayList< ArrayList<Spot> > trackSegments;
	
	public GapClosingCostFunction(Matrix m, double frameCutoff, double maxDist, ArrayList< ArrayList<Spot> > trackSegments) {
		this.m = m;
		this.frameCutoff = frameCutoff;
		this.maxDist = maxDist;
		this.trackSegments = trackSegments;
	}
	
	@Override
	public void applyCostFunction() {
		ArrayList<Spot> seg1, seg2;
		Spot end, start;
		double d, score;
		int n = m.getRowDimension();
		
		// Set the gap closing scores for each segment start and end pair
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				
				// If i and j are the same track segment, block it
				if (i == j) {
					m.set(i, j, BLOCKED);
					continue;
				}
				
				seg1 = trackSegments.get(i);
				seg2 = trackSegments.get(j);
				end = seg1.get(seg1.size() - 1);	// get last Spot of seg1
				start = seg2.get(0);				// get first Spot of seg2
				
				// Frame cutoff
				if (Math.abs(end.getFrame() - start.getFrame()) > frameCutoff || end.getFrame() > start.getFrame()) {
					m.set(i, j, BLOCKED);
					continue;
				}
				
				// Radius cutoff
				d = euclideanDistance(end, start);
				if (d > maxDist) {
					m.set(i, j, BLOCKED);
					continue;
				}
				
				score = d*d;
				m.set(i, j, score);
			}
		}
	}
}
