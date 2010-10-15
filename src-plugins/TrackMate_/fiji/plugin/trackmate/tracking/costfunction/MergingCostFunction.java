package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;
import java.util.SortedSet;

import Jama.Matrix;
import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

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
	
	/** The time cutoff */
	protected double timeCutoff;
	/** The distance threshold. */
	protected double maxDist;
	/** The value used to block an assignment in the cost matrix. */
	protected double blocked;
	/** Thresholds for the intensity ratios. */
	protected double maxIntensityRatio;
	
	public MergingCostFunction(double maxDist, double timeCutOff, double blocked, double maxIntensityRatio) {
		this.maxDist = maxDist;
		this.timeCutoff = timeCutOff;
		this.blocked = blocked;
		this.maxIntensityRatio = maxIntensityRatio;
	}
	
	public Matrix getCostFunction(List<SortedSet<Spot>> trackSegments, List<Spot> middlePoints) {
		final Matrix m = new Matrix(trackSegments.size(), middlePoints.size());
		double iRatio, d2, s;
		Spot end, middle;
		float tend, tmiddle;
		
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
				
				// Intensity threshold -  must be within max intensity threshold
				iRatio = middle.normalizeDiffTo(end, Feature.MEAN_INTENSITY);				
				if (iRatio > maxIntensityRatio) {
					m.set(i, j, blocked);
					continue;
				}
				
				s = d2 * ( 1 + iRatio );
				
				// Set score
				m.set(i, j, s);
			}
		}
		
		return m;
	}
}
