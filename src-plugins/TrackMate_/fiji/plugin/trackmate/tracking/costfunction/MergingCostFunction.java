package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.multithreading.SimpleMultiThreading;

import Jama.Matrix;
import fiji.plugin.trackmate.SpotFeature;
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
	protected Map<SpotFeature, Double> featureCutoffs;
	/** A flag stating if we should use multi--threading for some calculations. */
	protected boolean useMultithreading = fiji.plugin.trackmate.TrackMate_.DEFAULT_USE_MULTITHREADING;

	public MergingCostFunction(TrackerSettings settings) {
		this.maxDist 			= settings.mergingDistanceCutoff;
		this.timeCutoff 		= settings.mergingTimeCutoff;
		this.blocked 			= settings.blockingValue;
		this.featureCutoffs 	= settings.mergingFeatureCutoffs;
		this.allowed 			= settings.allowMerging;
	}

	public Matrix getCostFunction(final List<SortedSet<Spot>> trackSegments, final List<Spot> middlePoints) {
		final Matrix m = new Matrix(trackSegments.size(), middlePoints.size());

		// If we are not allow to make gap-closing, simply fill the matrix with blocking values.
		if (!allowed) {
			return new Matrix(trackSegments.size(), middlePoints.size(), blocked);
		}

		// Prepare threads
		final Thread[] threads;
		if (useMultithreading) {
			threads = SimpleMultiThreading.newThreads();
		} else {
			threads = SimpleMultiThreading.newThreads(1);
		}

		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger(0);
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("LAPTracker merging cost thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int i = ai.getAndIncrement(); i < trackSegments.size(); i = ai.getAndIncrement()) {
						Spot end = trackSegments.get(i).last();

						for (int j = 0; j < middlePoints.size(); j++) {
							Spot middle = middlePoints.get(j);

							// Frame threshold - middle Spot must be one frame ahead of the end Spot
							Float tend = end.getFeature(SpotFeature.POSITION_T);
							Float tmiddle = middle.getFeature(SpotFeature.POSITION_T);
							if (tmiddle - tend > timeCutoff || tmiddle - tend <= 0) {
								m.set(i, j, blocked);
								continue;
							}

							// Radius threshold
							Float d2 = end.squareDistanceTo(middle);
							if (d2 > maxDist*maxDist) {
								m.set(i, j, blocked);
								continue;
							}

							// Initial cost
							double s = d2;

							// Update cost with feature costs
							for (SpotFeature feature : featureCutoffs.keySet()) {

								// Larger than 0, equals 0 is the 2 intensities are the same
								Float iRatio = middle.normalizeDiffTo(end, feature);
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
				}
			};
		}
		
		SimpleMultiThreading.startAndJoin(threads);
		return m;
	}
}
