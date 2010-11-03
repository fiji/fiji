package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import Jama.Matrix;
import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.TrackerSettings;

/**
 * <p>Merging cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is:
 * 
 * <p><code>d^2 * p</code>, p > 1
 * <p><code>d^2 * (1/(p^2))</code>, p < 1
 * 
 * <p>d = euclidean distance between two objects
 * <p>p = (intensity of middle point, frame t+1) / ((intensity of end point, frame t) + (intensity of middle point, frame t))
 * 
 * <p>The <b>thresholds</b> used are:
 * <ul>
 * <li>Must be within a certain number of frames.</li>
 * <li>Must be within a certain distance.</li>
 * <li>p, the intensity ratio, must be within a certain range</li>
 * </ul>
 * 
 * See equations (5) and (6) in the paper.
 * 
 * @author Nicholas Perry
 *
 */
public class MergingCostFunction {
	
	/** If false, gap closing will be prohibited. */
	private boolean allowed;
	/** The time cutoff */
	protected double timeCutoff;
	/** The distance threshold. */
	protected double maxDist;
	/** The value used to block an assignment in the cost matrix. */
	protected double blocked;
	/** Thresholds for the feature ratios. */
	protected Map<Feature, Double> featureCutoffs;
	
	public MergingCostFunction(TrackerSettings settings) {
		this.maxDist 			= settings.mergingDistanceCutoff;
		this.timeCutoff 		= settings.mergingTimeCutoff;
		this.blocked 			= settings.blockingValue;
		this.featureCutoffs 	= settings.mergingFeatureCutoffs;
		this.allowed 			= settings.allowMerging;
	}
	
	public Matrix getCostFunction(List<SortedSet<Spot>> trackSegments, List<Spot> middlePoints) {
		final Matrix m = new Matrix(trackSegments.size(), middlePoints.size());
		double iRatio, d2, s;
		Spot end, middle;
		float tend, tmiddle;
		
		// If we are not allow to make gap-closing, simply fill the matrix with blocking values.
		if (!allowed) {
			return new Matrix(trackSegments.size(), middlePoints.size(), blocked);
		}
		
		for (int i = 0; i < trackSegments.size(); i++) {
			end = trackSegments.get(i).last();

			for (int j = 0; j < middlePoints.size(); j++) {
				middle = middlePoints.get(j);
				
				// Frame threshold - middle Spot must be one frame ahead of the end Spot
				tend = end.getFeature(Feature.POSITION_T);
				tmiddle = middle.getFeature(Feature.POSITION_T);
				if (tmiddle - tend > timeCutoff || tmiddle <= tend) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Radius threshold
				d2 = end.squareDistanceTo(middle);
				if (d2 > maxDist*maxDist) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Initial cost
				s = d2;

				// Update cost with feature costs
				for (Feature feature : featureCutoffs.keySet()) {

					// Larger than 0, equals 0 is the 2 intensities are the same
					iRatio = middle.normalizeDiffTo(end, feature);
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
