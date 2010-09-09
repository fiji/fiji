package fiji.plugin.spottracker.tracking.costfunction;

import java.util.ArrayList;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.Utils;
import fiji.plugin.spottracker.tracking.LAPTracker;

import Jama.Matrix;

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
public class MergingCostFunction implements CostFunctions {
	
	/** The cost matrix. */
	protected Matrix m;
	/** The distance threshold. */
	protected double maxDist;
	/** The value used to block an assignment in the cost matrix. */
	protected double blocked;
	/** The list of track segments. */
	protected ArrayList< ArrayList<Spot> > trackSegments;
	/** The list of middle points. */
	protected ArrayList<Spot> middlePoints;
	/** Thresholds for the intensity ratios. */
	protected double[] intensityThresholds;
	
	public MergingCostFunction(Matrix m, ArrayList< ArrayList<Spot> > trackSegments, ArrayList<Spot> middlePoints, double maxDist, double blocked, double[] intensityThresholds) {
		this.m = m;
		this.trackSegments = trackSegments;
		this.middlePoints = middlePoints;
		this.maxDist = maxDist;
		this.blocked = blocked;
		this.intensityThresholds = intensityThresholds;
	}
	
	@Override
	public void applyCostFunction() {
		double iRatio, d, s;
		int segLength;
		Spot end, middle;
		
		for (int i = 0; i < trackSegments.size(); i++) {
			for (int j = 0; j < middlePoints.size(); j++) {
				segLength = trackSegments.get(i).size();
				end = trackSegments.get(i).get(segLength - 1);
				middle = middlePoints.get(j);
				
				// Frame threshold - middle Spot must be one frame ahead of the end Spot
				if (middle.getFrame() != end.getFrame() + 1) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Radius threshold
				d = Utils.euclideanDistance(end, middle);
				if (d > maxDist) {
					m.set(i, j, blocked);
					continue;
				}

				iRatio = middle.getFeature(Feature.MEAN_INTENSITY) / (middle.getPrev().get(0).getFeature(Feature.MEAN_INTENSITY) + end.getFeature(Feature.MEAN_INTENSITY));
				
				// Intensity threshold -  must be within INTENSITY_RATIO_CUTOFFS ([min, max])
				if (iRatio > intensityThresholds[1] || iRatio < intensityThresholds[0]) {
					m.set(i, j, blocked);
					continue;
				}
				
				if (iRatio >= 1) {
					s = d * d * iRatio;
				} else {
					s = d * d * ( 1 / (iRatio * iRatio) );
				}

				// Set score
				m.set(i, j, s);
			}
		}
	}
}
