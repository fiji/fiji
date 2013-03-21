package fiji.plugin.trackmate.tracking.costfunction;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.multithreading.SimpleMultiThreading;
import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.LAPTracker;
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
	private final boolean allowed;
	/** The distance threshold. */
	protected final double maxDist;
	/** The value used to block an assignment in the cost matrix. */
	protected final double blockingValue;
	/** Thresholds for the feature ratios. */
	protected final Map<String, Double> featurePenalties;
	/** A flag stating if we should use multi--threading for some calculations. */
	protected boolean useMultithreading = fiji.plugin.trackmate.TrackMate_.DEFAULT_USE_MULTITHREADING; // FIXME this is lame

	@SuppressWarnings("unchecked")
	public MergingCostFunction(Map<String, Object> settings) {
		this.maxDist 			= (Double) settings.get(KEY_MERGING_MAX_DISTANCE);
		this.blockingValue		= (Double) settings.get(KEY_BLOCKING_VALUE);
		this.featurePenalties	= (Map<String, Double>) settings.get(KEY_MERGING_FEATURE_PENALTIES);
		this.allowed 			= (Boolean) settings.get(KEY_ALLOW_TRACK_MERGING);
	}

	public Matrix getCostFunction(final List<SortedSet<Spot>> trackSegments, final List<Spot> middlePoints) {
		final Matrix m = new Matrix(trackSegments.size(), middlePoints.size());

		// If we are not allow to catch merging events, simply fill the matrix with blocking values.
		if (!allowed) {
			return new Matrix(trackSegments.size(), 0, blockingValue);
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
							int endFrame = end.getFeature(Spot.FRAME).intValue();
							int middleFrame = middle.getFeature(Spot.FRAME).intValue();
							// We only merge from one frame to the next one, no more
							if (middleFrame - endFrame != 1) {
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
