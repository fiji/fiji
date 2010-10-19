package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import Jama.Matrix;
import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.TrackerSettings;

/**
 * <p>Splitting cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is:
 * 
 * <p><code>d^2 * p</code>, p > 1
 * <p><code>d^2 * (1/(p^2))</code>, p < 1
 * 
 * <p>d = euclidean distance between two objects
 * <p>p = (intensity of middle point, frame t) / ((intensity of start point, frame t+1) + (intensity of middle point, frame t + 1))
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
public class SplittingCostFunction {

	/** The time cutoff */
	protected double timeCutoff;
	/** The distance threshold. */
	protected double maxDist;
	/** The value used to block an assignment in the cost matrix. */
	protected double blocked;
	/** Thresholds for the feature ratios. */
	protected Map<Feature, Double> featureCutoffs;
	
	/*
	 * CONSTRUCTOR
	 */
	
	
	public SplittingCostFunction(TrackerSettings settings) {
		this.maxDist 			= settings.splittingDistanceCutoff;
		this.timeCutoff 		= settings.splittingTimeCutoff;
		this.blocked 			= settings.blockingValue;
		this.featureCutoffs 	= settings.splittingFeatureCutoffs;
	}
	
	/*
	 * METHODS
	 */
	
	
	public Matrix getCostFunction(final List<SortedSet<Spot>> trackSegments, final List<Spot> middlePoints) {
		double iRatio, d2, s;
		Spot start, middle;
		float tstart, tmiddle;
		Matrix m = new Matrix(middlePoints.size(), trackSegments.size());
		
		// Fill in splitting scores
		for (int i = 0; i < middlePoints.size(); i++) {
			for (int j = 0; j < trackSegments.size(); j++) {
				start = trackSegments.get(j).first();
				middle = middlePoints.get(i);
				
				// Frame threshold - middle Spot must be one frame behind of the start Spot
				tstart = start.getFeature(Feature.POSITION_T);
				tmiddle = middle.getFeature(Feature.POSITION_T);
				if (tmiddle - tstart <  - timeCutoff) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Radius threshold
				d2 = start.squareDistanceTo(middle);
				if (d2 > maxDist*maxDist) {
					m.set(i, j, blocked);
					continue;
				}

				// Initial cost
				s = d2;

				// Update cost with feature costs
				for (Feature feature : featureCutoffs.keySet()) {

					// Larger than 0, equals 0 is the 2 intensities are the same
					iRatio = start.normalizeDiffTo(middle, feature);
					if (iRatio > featureCutoffs.get(feature)) {
						s = blocked;
						break;
					}

					// Set score
					s *= (1 + iRatio);
				}
				
				m.set(i, j, s);
			}
		}
		
		return m;
	}
}
