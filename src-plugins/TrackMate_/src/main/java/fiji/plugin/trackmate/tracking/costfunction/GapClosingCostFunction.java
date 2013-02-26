package fiji.plugin.trackmate.tracking.costfunction;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;

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
	protected final boolean allowed;
	/** The distance cutoff: no gap is closed if the two spots are further than this distance. */
	protected final double maxDist;
	/** The max frame gap above which gap to close are not sought. */
	protected final int frameCutoff;
	/** The value to use to block an assignment in the cost matrix. */
	protected final double blockingValue;
	/** Feature penalties. */
	protected final Map<String, Double> featurePenalties;
	/** A flag stating if we should use multi--threading for some calculations. */ // FIXME THIS IS LAME
	protected boolean useMultithreading = fiji.plugin.trackmate.TrackMate_.DEFAULT_USE_MULTITHREADING;


	@SuppressWarnings("unchecked")
	public GapClosingCostFunction(final Map<String, Object> settings) {
		this.frameCutoff 		= (Integer) settings.get(KEY_GAP_CLOSING_MAX_FRAME_GAP);
		this.maxDist 			= (Double) settings.get(KEY_GAP_CLOSING_MAX_DISTANCE);
		this.blockingValue		= (Double) settings.get(KEY_BLOCKING_VALUE);
		this.featurePenalties	= (Map<String, Double>) settings.get(KEY_GAP_CLOSING_FEATURE_PENALTIES);
		this.allowed 			= (Boolean) settings.get(KEY_ALLOW_GAP_CLOSING);
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
						int endFrame = end.getFeature(Spot.FRAME).intValue(); // we want at least tstart > tend

						// Set the gap closing scores for each segment start and end pair
						for (int j = 0; j < n; j++) {

							// If i and j are the same track segment, block it
							if (i == j) {
								m.set(i, j, blockingValue);
								continue;
							}

							SortedSet<Spot> seg2 = trackSegments.get(j);
							Spot start = seg2.first();			// get first Spot of seg2
							int startFrame = start.getFeature(Spot.FRAME).intValue();

							// Frame cutoff. A value of 1 means a gap of 1 frame. If the end spot 
							// is in frame 10, the start spot in frame 12, and if the max gap is 1
							// then we should sought to bridge this gap (12 to 10 is a gap of 1 frame).
							if (startFrame - endFrame > (frameCutoff+1) || endFrame >= startFrame) {
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
