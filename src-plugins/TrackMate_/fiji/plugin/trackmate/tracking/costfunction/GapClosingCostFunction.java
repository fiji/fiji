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
 * <p>Gap closing cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is:
 * 
 * <p><code>d^2</code>, where d is the euclidean distance between two objects.
 * 
 * <p>The <b>thresholds</b> used are:
 * <ul>
 * <li>Must be within a certain time.</li>
 * <li>Must be within a certain distance.</li>
 * </ul>
 * 
 * See equation (4) in the paper.
 * 
 * @author Nicholas Perry
 */
public class GapClosingCostFunction {

	/** If false, gap closing will be prohibited. */
	private boolean allowed;
	/** The time cutoff, and distance cutoff, respectively */
	protected double timeCutoff, maxDist;
	/** The value to use to block an assignment in the cost matrix. */
	protected double blocked;
	/** Thresholds for the feature ratios. */
	protected Map<SpotFeature, Double> featureCutoffs;
	/** A flag stating if we should use multi--threading for some calculations. */
	protected boolean useMultithreading = fiji.plugin.trackmate.TrackMate_.DEFAULT_USE_MULTITHREADING;


	public GapClosingCostFunction(TrackerSettings settings) {
		this.timeCutoff 		= settings.gapClosingTimeCutoff;
		this.maxDist 			= settings.gapClosingDistanceCutoff;
		this.blocked 			= settings.blockingValue;
		this.featureCutoffs		= settings.gapClosingFeatureCutoffs;
		this.allowed 			= settings.allowGapClosing;
	}

	public Matrix getCostFunction(final List<SortedSet<Spot>> trackSegments) {
		final int n = trackSegments.size();
		final Matrix m = new Matrix(n, n);

		// If we are not allow to make gap-closing, simply fill the matrix with blocking values.
		if (!allowed) {
			return new Matrix(n, n, blocked);
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
						Float tend = end.getFeature(SpotFeature.POSITION_T); // we want at least tstart > tend

						// Set the gap closing scores for each segment start and end pair
						for (int j = 0; j < n; j++) {

							// If i and j are the same track segment, block it
							if (i == j) {
								m.set(i, j, blocked);
								continue;
							}

							SortedSet<Spot> seg2 = trackSegments.get(j);
							Spot start = seg2.first();			// get first Spot of seg2
							Float tstart = start.getFeature(SpotFeature.POSITION_T);

							// Frame cutoff
							if (tstart - tend > timeCutoff || tend >= tstart) {
								m.set(i, j, blocked);
								continue;
							}

							// Radius cutoff
							double d2 = start.squareDistanceTo(end);
							if (d2 > maxDist*maxDist) {
								m.set(i, j, blocked);
								continue;
							}

							// Initial cost
							double s = d2;

							// Update cost with feature costs
							for (SpotFeature feature : featureCutoffs.keySet()) {

								// Larger than 0, equals 0 is the 2 intensities are the same
								Float iRatio = start.normalizeDiffTo(end, feature);
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
