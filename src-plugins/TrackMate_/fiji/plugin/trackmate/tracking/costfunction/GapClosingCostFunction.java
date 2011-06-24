package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import Jama.Matrix;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.TrackerSettings;

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

	/** If false, gap closing will be prohibited. */
	private boolean allowed;
	/** The time cutoff, and distance cutoff, respectively */
	protected double timeCutoff, maxDist;
	/** The value to use to block an assignment in the cost matrix. */
	protected double blocked;
	/** Thresholds for the feature ratios. */
	protected Map<SpotFeature, Double> featureCutoffs;
	
	public GapClosingCostFunction(TrackerSettings settings) {
		this.timeCutoff 		= settings.gapClosingTimeCutoff;
		this.maxDist 			= settings.gapClosingDistanceCutoff;
		this.blocked 			= settings.blockingValue;
		this.featureCutoffs		= settings.gapClosingFeatureCutoffs;
		this.allowed 			= settings.allowGapClosing;
	}
	
	public Matrix getCostFunction(List<SortedSet<Spot>> trackSegments) {
		SortedSet<Spot> seg1, seg2;
		Spot end, start;
		float tend, tstart;
		double d2, s, iRatio;
		int n = trackSegments.size();
		final Matrix m = new Matrix(n, n);

		// If we are not allow to make gap-closing, simply fill the matrix with blocking values.
		if (!allowed) {
			return new Matrix(n, n, blocked);
		}
		
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
				tend = end.getFeature(SpotFeature.POSITION_T); // we want at least tstart > tend
				tstart = start.getFeature(SpotFeature.POSITION_T);
				
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

				// Initial cost
				s = d2;

				// Update cost with feature costs
				for (SpotFeature feature : featureCutoffs.keySet()) {

					// Larger than 0, equals 0 is the 2 intensities are the same
					iRatio = start.normalizeDiffTo(end, feature);
					if (iRatio > featureCutoffs.get(feature)) {
						s = blocked;
						break;
					}

					// Set score
					s *= (1 + iRatio);
				}
				
				// Set score
				m.set(i, j, s);
			}
		}
		
		return m;
	}
}
