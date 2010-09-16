package fiji.plugin.trackmate.tracking.costfunction;

import java.util.ArrayList;

import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.TrackNode;
import fiji.plugin.trackmate.Utils;
import fiji.plugin.trackmate.tracking.LAPTracker;

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
public class GapClosingCostFunction<K extends Spot> implements CostFunctions {

	/** The cost matrix. */
	protected Matrix m;
	/** The frame cutoff, and distance cutoff, respectively */
	protected double frameCutoff, maxDist;
	/** The list of track segments, where each segment is a list of Spots. */
	protected ArrayList< ArrayList<TrackNode<K>> > trackSegments;
	/** The value to use to block an assignment in the cost matrix. */
	protected double blocked;
	
	public GapClosingCostFunction(Matrix m, double frameCutoff, double maxDist, double blocked, ArrayList< ArrayList<TrackNode<K>> > trackSegments) {
		this.m = m;
		this.frameCutoff = frameCutoff;
		this.maxDist = maxDist;
		this.blocked = blocked;
		this.trackSegments = trackSegments;
	}
	
	@Override
	public void applyCostFunction() {
		ArrayList<TrackNode<K>> seg1, seg2;
		TrackNode<K> end, start;
		K objEnd, objStart;
		float tend, tstart;
		double d2;
		int n = m.getRowDimension();
		
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
				end = seg1.get(seg1.size() - 1);	// get last Spot of seg1
				start = seg2.get(0);				// get first Spot of seg2
				objEnd = end.getObject();
				objStart = start.getObject();
				tend = objEnd.getFeature(Feature.POSITION_T);
				tstart = objStart.getFeature(Feature.POSITION_T);
				
				// Frame cutoff
				if (Math.abs(tend-tstart) > frameCutoff || tend > tstart) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Radius cutoff
				d2 = Utils.euclideanDistanceSquared(objEnd, objStart);
				if (d2 > maxDist*maxDist) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Set score
				m.set(i, j, d2);
			}
		}
	}
}
