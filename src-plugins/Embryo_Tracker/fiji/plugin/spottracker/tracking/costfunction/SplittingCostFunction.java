package fiji.plugin.spottracker.tracking.costfunction;

import java.util.ArrayList;

import fiji.plugin.spottracker.Featurable;
import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.TrackNode;
import fiji.plugin.spottracker.Utils;
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
 * @author Nicholas Perry
 *
 */
public class SplittingCostFunction <K extends Featurable> implements CostFunctions {

	/** The cost matrix. */
	protected Matrix m;
	/** The distance threshold. */
	protected double maxDist;
	/** The value used to block an assignment in the cost matrix. */
	protected double blocked;
	/** The list of track segments. */
	protected ArrayList< ArrayList<TrackNode<K>> > trackSegments;
	/** The list of middle points. */
	protected ArrayList<TrackNode<K>> splittingMiddlePoints;
	/** Thresholds for the intensity ratios. */
	protected double minIntensityRatio;
	/** Thresholds for the intensity ratios. */
	protected double maxIntensityRatio;
	
	public SplittingCostFunction(
			Matrix m, 
			ArrayList< ArrayList<TrackNode<K>> > trackSegments, 
			ArrayList<TrackNode<K>> splittingMiddlePoints, 
			double maxDist, 
			double blocked, 
			double minIntensityRatio, 
			double maxIntensityRatio) {
		this.m = m;
		this.trackSegments = trackSegments;
		this.splittingMiddlePoints = splittingMiddlePoints;
		this.maxDist = maxDist;
		this.blocked = blocked;
		this.minIntensityRatio = minIntensityRatio;
		this.maxIntensityRatio = maxIntensityRatio;
	}
	
	@Override
	public void applyCostFunction() {
		double iRatio, d2, s;
		TrackNode<K> start, middle;
		float tstart, tmiddle;
		
		// Fill in splitting scores
		for (int i = 0; i < splittingMiddlePoints.size(); i++) {
			for (int j = 0; j < trackSegments.size(); j++) {
				start = trackSegments.get(j).get(0);
				middle = splittingMiddlePoints.get(i);
				
				// Frame threshold - middle Spot must be one frame behind of the start Spot
				tstart = start.getObject().getFeature(Feature.POSITION_T);
				tmiddle = middle.getObject().getFeature(Feature.POSITION_T);
				if (tmiddle - tstart <  - 1) {
					m.set(i, j, blocked);
					continue;
				}
				
				// Radius threshold
				d2 = Utils.euclideanDistanceSquared(start.getObject(), middle.getObject());
				if (d2 > maxDist*maxDist) {
					m.set(i, j, blocked);
					continue;
				}

				K middleSpot = middle.getChildren().iterator().next().getObject(); 
				iRatio = middle.getObject().getFeature(Feature.MEAN_INTENSITY) / (middleSpot.getFeature(Feature.MEAN_INTENSITY) + start.getObject().getFeature(Feature.MEAN_INTENSITY));
				
				// Intensity threshold -  must be within mix and max intensity thresholds
				if (iRatio > maxIntensityRatio || iRatio < minIntensityRatio) {
					m.set(i, j, blocked);
					continue;
				}

				
				// Set score
				if (iRatio >= 1)
					s = d2 * iRatio;
				else
					s = d2 * ( 1 / (iRatio * iRatio) );
				m.set(i, j, s);
			}
		}
	}
}
