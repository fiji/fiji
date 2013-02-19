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
 * <p>Gap closing cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is determined by the default equation in the
 * TrackMate plugin, see below.
 * <p>  
 *  It slightly differs from the Jaqaman article, see equation (4) in the paper.
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
 */
public class GapClosingCostFunction {

	/** If false, gap closing will be prohibited. */
	private boolean allowed;
	/** The time cutoff, and distance cutoff, respectively */
	protected double timeCutoff, maxDist;
	/** The value to use to block an assignment in the cost matrix. */
	protected double blockingValue;
	/** Thresholds for the feature ratios. */
	protected Map<String, Double> featurePenalties;
	/** A flag stating if we should use multi--threading for some calculations. */
	protected boolean useMultithreading = fiji.plugin.trackmate.TrackMate_.DEFAULT_USE_MULTITHREADING;


	public GapClosingCostFunction(LAPTrackerSettings settings) {
		this.timeCutoff 		= settings.gapClosingTimeCutoff;
		this.maxDist 			= settings.gapClosingDistanceCutoff;
		this.blockingValue		= settings.blockingValue;
		this.featurePenalties	= settings.gapClosingFeaturePenalties;
		this.allowed 			= settings.allowGapClosing;
	}

	public Matrix getCostFunction(final List<SortedSet<Spot>> trackSegments) {
		final int n = trackSegments.size();
		final Matrix m = new Matrix(n, n);

		// If we are not allow to make gap-closing, simply fill the matrix with blocking values.
		if (!allowed) {
			return new Matrix(n, n, blockingValue);
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

			threads[ithread] = new Thread("LAPTracker gap closing cost thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int i = ai.getAndIncrement(); i < n; i = ai.getAndIncrement()) {

						SortedSet<Spot> seg1 = trackSegments.get(i);
						Spot end = seg1.last();				// get last Spot of seg1
						Float tend = end.getFeature(Spot.POSITION_T); // we want at least tstart > tend

						// Set the gap closing scores for each segment start and end pair
						for (int j = 0; j < n; j++) {

							// If i and j are the same track segment, block it
							if (i == j) {
								m.set(i, j, blockingValue);
								continue;
							}

							SortedSet<Spot> seg2 = trackSegments.get(j);
							Spot start = seg2.first();			// get first Spot of seg2
							Float tstart = start.getFeature(Spot.POSITION_T);

							// Frame cutoff
							if (tstart - tend > timeCutoff || tend >= tstart) {
								m.set(i, j, blockingValue);
								continue;
							}

							double cost = LAPUtils.computeLinkingCostFor(end, start, maxDist, blockingValue, featurePenalties);
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
