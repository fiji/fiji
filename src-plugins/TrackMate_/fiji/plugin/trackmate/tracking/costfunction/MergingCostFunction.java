package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.multithreading.SimpleMultiThreading;
import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPTrackerSettings;
import fiji.plugin.trackmate.tracking.LAPUtils;

/**
 * <p>Merging cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is determined by the default equation in the
 * TrackMate plugin, see below.
 * <p>  
 *  It slightly differs from the Jaqaman article, see equation (5) and (6) in the paper.
 * <p>
 * The <b>thresholds</b> used are:
 * <ul>
 * <li>Must be within a certain number of frames.</li>
 * <li>Must be within a certain distance.</li>
 * </ul>
 * 
 * @see LAPUtils#computeLinkingCostFor(Spot, Spot, double, double, Map)
 * @author Nicholas Perry
 * @author Jean-Yves Tinevez
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
	protected double blockingValue;
	/** Thresholds for the feature ratios. */
	protected Map<String, Double> featurePenalties;
	/** A flag stating if we should use multi--threading for some calculations. */
	protected boolean useMultithreading = fiji.plugin.trackmate.TrackMate_.DEFAULT_USE_MULTITHREADING;

	public MergingCostFunction(LAPTrackerSettings settings) {
		this.maxDist 			= settings.mergingDistanceCutoff;
		this.timeCutoff 		= settings.mergingTimeCutoff;
		this.blockingValue		= settings.blockingValue;
		this.featurePenalties 	= settings.mergingFeaturePenalties;
		this.allowed 			= settings.allowMerging;
	}

	public Matrix getCostFunction(final List<SortedSet<Spot>> trackSegments, final List<Spot> middlePoints) {
		final Matrix m = new Matrix(trackSegments.size(), middlePoints.size());

		// If we are not allow to make gap-closing, simply fill the matrix with blocking values.
		if (!allowed) {
			return new Matrix(trackSegments.size(), middlePoints.size(), blockingValue);
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
							Float tend = end.getFeature(Spot.POSITION_T);
							Float tmiddle = middle.getFeature(Spot.POSITION_T);
							if (tmiddle - tend > timeCutoff || tmiddle - tend <= 0) {
								m.set(i, j, blockingValue);
								continue;
							}

							// Initial cost
							double cost = LAPUtils.computeLinkingCostFor(end, middle, maxDist, blockingValue, featurePenalties);
							m.set(i, j, cost);
						}
					}
				}
			};
		}
		
		SimpleMultiThreading.startAndJoin(threads);
		return m;
	}
}
