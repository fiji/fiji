package fiji.plugin.spottracker.tracking.costfunction;

import java.util.ArrayList;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.tracking.LAPTracker;

import Jama.Matrix;

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
 * @author nperry
 *
 */
public class SplittingCostFunction extends LAPTrackerCostFunction {

	protected Matrix m;
	protected double maxDist;
	protected ArrayList< ArrayList<Spot> > trackSegments;
	protected ArrayList<Spot> middlePoints;
	protected double[] intensityThresholds;
	
	public SplittingCostFunction(Matrix m, ArrayList< ArrayList<Spot> > trackSegments, ArrayList<Spot> middlePoints, double maxDist, double[] intensityThresholds) {
		this.m = m;
		this.trackSegments = trackSegments;
		this.middlePoints = middlePoints;
		this.maxDist = maxDist;
		this.intensityThresholds = intensityThresholds;
	}
	
	@Override
	public void applyCostFunction() {
		double iRatio, d, s;
		Spot start, middle;
		
		// Fill in splitting scores
		for (int i = 0; i < middlePoints.size(); i++) {
			for (int j = 0; j < trackSegments.size(); j++) {
				start = trackSegments.get(j).get(0);
				middle = middlePoints.get(i);
				
				// Frame threshold - middle Spot must be one frame behind of the start Spot
				if (middle.getFrame() != start.getFrame() - 1) {
					m.set(i, j, BLOCKED);
					continue;
				}
				
				// Radius threshold
				d = euclideanDistance(start, middle);
				if (d > maxDist) {
					m.set(i, j, BLOCKED);
					continue;
				}

				iRatio = middle.getPrev().get(0).getFeature(Feature.MEAN_INTENSITY) / (middle.getFeature(Feature.MEAN_INTENSITY) + start.getFeature(Feature.MEAN_INTENSITY));
				
				// Intensity threshold -  must be within INTENSITY_RATIO_CUTOFFS ([min, max])
				if (iRatio > intensityThresholds[1] || iRatio < intensityThresholds[0]) {
					m.set(i, j, BLOCKED);
					continue;
				}

				if (iRatio >= 1) {
					s = d * d * iRatio;
				} else {
					s = d * d * ( 1 / (iRatio * iRatio) );
				}
				m.set(i, j, s);
			}
		}
	}
}
