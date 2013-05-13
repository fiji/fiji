package fiji.plugin.trackmate.tracking.costfunction;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

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
 * <p>Splitting cost function used with {@link LAPTracker}.
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
public class SplittingCostFunction {

	private static final boolean DEBUG = false;

	/** The distance threshold. */
	protected final double maxDist;
	/** The value used to block an assignment in the cost matrix. */
	protected final double blockingValue;
	/** Thresholds for the feature ratios. */
	protected final Map<String, Double> featurePenalties;
	private boolean allowSplitting;
	/** A flag stating if we should use multi--threading for some calculations. */ // FIXME THIS IS LAME
	protected boolean useMultithreading = fiji.plugin.trackmate.TrackMate_.DEFAULT_USE_MULTITHREADING;


	/*
	 * CONSTRUCTOR
	 */


	@SuppressWarnings("unchecked")
	public SplittingCostFunction(final Map<String, Object> settings) {
		this.maxDist 			= (Double) settings.get(KEY_SPLITTING_MAX_DISTANCE);
		this.blockingValue		= (Double) settings.get(KEY_BLOCKING_VALUE);
		this.featurePenalties	= (Map<String, Double>) settings.get(KEY_SPLITTING_FEATURE_PENALTIES);
		this.allowSplitting		= (Boolean) settings.get(KEY_ALLOW_TRACK_SPLITTING);
	}

	/*
	 * METHODS
	 */


	public Matrix getCostFunction(final List<SortedSet<Spot>> trackSegments, final List<Spot> middlePoints) {

		if (DEBUG)
			System.out.println("-- DEBUG information from SplittingCostFunction --");

		if (!allowSplitting)
			return new Matrix(middlePoints.size(), trackSegments.size(), blockingValue);

		// Prepare threads
		final Thread[] threads;
		if (useMultithreading) {
			threads = SimpleMultiThreading.newThreads();
		} else {
			threads = SimpleMultiThreading.newThreads(1);
		}

		final Matrix m = new Matrix(middlePoints.size(), trackSegments.size());

		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger(0);
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("LAPTracker splitting cost thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int i = ai.getAndIncrement(); i < middlePoints.size(); i = ai.getAndIncrement()) {

						Spot middle = middlePoints.get(i);

						for (int j = 0; j < trackSegments.size(); j++) {

							SortedSet<Spot> track = trackSegments.get(j);
							Spot start = track.first();

							if (DEBUG)
								System.out.println("Segment "+j);
							if (track.contains(middle)) {	
								m.set(i, j, blockingValue);
								continue;
							}

							// Frame threshold - middle Spot must be one frame behind of the start Spot
							int startFrame = start.getFeature(Spot.FRAME).intValue();
							int middleFrame = middle.getFeature(Spot.FRAME).intValue();
							if (startFrame - middleFrame != 1 ) {
								m.set(i, j, blockingValue);
								continue;
							}

							double cost = LAPUtils.computeLinkingCostFor(start, middle, maxDist, blockingValue, featurePenalties);
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
