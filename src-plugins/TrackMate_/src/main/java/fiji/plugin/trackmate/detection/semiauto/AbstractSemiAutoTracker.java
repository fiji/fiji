package fiji.plugin.trackmate.detection.semiauto;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.meta.ImgPlus;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.tracking.SpotTracker;

/**
 * A class made to perform semi-automated tracking of spots in TrackMate &
 * friends
 * <p>
 * The user has to select one spot, one a meaningful location. The spot location
 * and its radius are then used to extract a small rectangular neighborhood in
 * the next frame around the spot. The neighborhood is then passed to a
 * {@link SpotDetector} that returns the spot it found. If a spot of
 * {@link Spot#QUALITY} high enough is found near enough to the first spot
 * center, then it is added to the model and linked with the first spot.
 * <p>
 * The process is then repeated, taking the newly found spot as a source for the
 * next neighborhood. The model is updated live for every spot found.
 * <p>
 * The process halts when:
 * <ul>
 * <li>no spots of quality high enough are found;
 * <li>spots of high quality are found, but too far from the initial spot;
 * <li>the source has no time-point left.
 * </ul>
 * 
 * @author Jean-Yves Tinevez - 2013
 * @param <T>
 *            the type of the source. Must extend {@link RealType} and
 *            {@link NativeType} to use with most TrackMate {@link SpotDetector}
 *            s.
 */
public abstract class AbstractSemiAutoTracker<T extends RealType<T> & NativeType<T>> implements Algorithm, MultiThreaded {

	/** Minimal size of neighborhoods, in spot diameter units. */
	protected static final double NEIGHBORHOOD_FACTOR = 2d;
	protected static final String BASE_ERROR_MESSAGE = "[SemiAutoTracker] ";
	private static final double QUALITY_THRESHOLD = 0.2d;
	private static final double DISTANCE_TOLERANCE = 1.1d;
	private final Model model;
	private final SelectionModel selectionModel;
	protected String errorMessage;
	private int numThreads;
	protected boolean ok;
	protected final Logger logger;
	/** How close must be the new spot found to be accepted, in radius units. */
	protected double distanceTolerance = DISTANCE_TOLERANCE;
	/**
	 * The fraction of the initial quality above which we keep new spots. The
	 * highest, the more intolerant.
	 */
	protected double qualityThreshold = QUALITY_THRESHOLD;

	/*
	 * CONSTRUCTOR
	 */

	public AbstractSemiAutoTracker(final Model model, final SelectionModel selectionModel, final Logger logger) {
		this.model = model;
		this.selectionModel = selectionModel;
		this.logger = logger;
	}

	/*
	 * METHODS
	 */

	/**
	 * Configures this semi-automatic tracker.
	 * 
	 * @param qualityThreshold
	 *            the fraction of the initial quality above which we keep new
	 *            spots. The highest, the more intolerant.
	 * @param distanceTolerance
	 *            how close must be the new spot found to be accepted, in radius
	 *            units.
	 */
	public void setParameters(final double qualityThreshold, final double distanceTolerance) {
		this.qualityThreshold = qualityThreshold;
		this.distanceTolerance = distanceTolerance;
	}

	@Override
	public boolean process() {
		final Set<Spot> spots = new HashSet<Spot>(selectionModel.getSpotSelection());
		if (spots.isEmpty()) {
			errorMessage = BASE_ERROR_MESSAGE + "No spots in selection.\n";
			return false;
		}
		selectionModel.clearSelection();

		final int nThreads = Math.min(numThreads, spots.size());
		final ArrayBlockingQueue<Spot> queue = new ArrayBlockingQueue<Spot>(spots.size(), false, spots);

		ok = true;
		final ThreadGroup semiAutoTrackingThreadgroup = new ThreadGroup("Semi-automatic tracking threads");
		final Thread[] threads = SimpleMultiThreading.newThreads(nThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(semiAutoTrackingThreadgroup, new Runnable() {
				@Override
				public void run() {
					Spot spot;
					while ((spot = queue.poll()) != null) {
						processSpot(spot);
					}
				}
			});
		}
		SimpleMultiThreading.startAndJoin(threads);
		return ok;
	}

	/**
	 * Recursively investigates a neighborhood to find the most likely successor
	 * of a spot, starting with the specified spot and operating recursively.
	 * The found spots are added to the {@link Model} given at construction and
	 * linked to the previous spot, until:
	 * <ul>
	 * <li>a spot of sufficient quality cannot be found in the close vicinity of
	 * the previous spot;
	 * <li>the raw image has exhausted all its time-points.
	 * </ul>
	 * This method can be called concurrently on several spots.
	 * 
	 * @param initialSpot
	 *            the spot to start detection with.
	 */
	public void processSpot(final Spot initialSpot) {

		/*
		 * Initial spot
		 */
		Spot spot = initialSpot;

		while (true) {

			/*
			 * Extract spot & features
			 */

			final int frame = spot.getFeature(Spot.FRAME).intValue() + 1; // We want to segment in the next frame
			final double radius = spot.getFeature(Spot.RADIUS);
			final double quality = spot.getFeature(Spot.QUALITY);

			/*
			 * Get neighborhood
			 */

			final SpotNeighborhood<T> sn = getNeighborhood(spot, frame);
			if (null == sn) {
				return;
			}

			final ImgPlus<T> neighborhood = sn.neighborhood;
			final AffineTransform3D transform = sn.transform;

			/*
			 * Detect spots
			 */

			final SpotDetector<T> detector = createDetector(neighborhood, radius, quality * qualityThreshold);

			if (!detector.checkInput() || !detector.process()) {
				ok = false;
				errorMessage = detector.getErrorMessage();
				return;
			}

			/*
			 * Get results
			 */

			final List<Spot> detectedSpots = detector.getResult();
			if (detectedSpots.isEmpty()) {
				logger.log("Spot: " + initialSpot + ": No suitable spot found.\n");
				return;
			}

			/*
			 * Translate spots
			 */

			final String[] features = new String[] { Spot.POSITION_X, Spot.POSITION_Y, Spot.POSITION_Z };
			for (final Spot ds : detectedSpots) {
				final double[] coords = new double[3];
				ds.localize(coords);
				final double[] target = new double[3];
				transform.apply(coords, target);
				for (int i = 0; i < target.length; i++) {
					ds.putFeature(features[i], target[i]);
				}
			}

			// Sort then by ascending quality
			final TreeSet<Spot> sortedSpots = new TreeSet<Spot>(Spot.featureComparator(Spot.QUALITY));
			sortedSpots.addAll(detectedSpots);

			boolean found = false;
			Spot target = null;
			for (final Iterator<Spot> iterator = sortedSpots.descendingIterator(); iterator.hasNext();) {
				final Spot candidate = iterator.next();
				if (candidate.squareDistanceTo(spot) < distanceTolerance * distanceTolerance * radius * radius) {
					found = true;
					target = candidate;
					break;
				}
			}

			if (!found) {
				logger.log("Spot: " + initialSpot + ": Suitable spot found, but outside the tolerance radius.\n");
				return;
			}

			/*
			 * Expose new spot
			 */

			exposeSpot(target, spot);

			/*
			 * Update model
			 */

			// spot
			target.putFeature(Spot.RADIUS, radius);
			target.putFeature(Spot.POSITION_T, Double.valueOf(frame));

			model.beginUpdate();
			try {
				model.addSpotTo(target, frame);
				model.addEdge(spot, target, spot.squareDistanceTo(target));
			} finally {
				model.endUpdate();
			}

			/*
			 * Loop
			 */

			spot = target;

		}
	}

	/**
	 * This method is a hook for subclassers. It exposes the newly found spot
	 * just before it is added to the {@link Model}. This method allows concrete
	 * implementation to add some specific post-treatment to detected spots.
	 * 
	 * @param newSpot
	 *            the spot that just has been found by the detection mechanism.
	 * @param previousSpot
	 *            the spot in the previous frame whose neighborhood has been
	 *            investigated to find the new spot. Already part of the model.
	 */
	protected abstract void exposeSpot(Spot newSpot, Spot previousSpot);

	/**
	 * Returns a small neighborhood around the specified spot, but taken at the
	 * specified frame. Implementations have to decide what is the right size
	 * for the neighborhood, given the specified spot radius and location.
	 * <p>
	 * Implementations can return <code>null</code> if for instance, the number
	 * of time frames in the raw source has been exhausted, or if the specified
	 * spot misses some information. This will be dealt with gracefully in the
	 * {@link #process()} method.
	 * 
	 * @param spot
	 *            the spot the desired neighborhood is centered on.
	 * @param frame
	 *            the frame in the source image the desired neighborhood as to
	 *            be taken.
	 * @return the neighborhood, as a {@link SpotNeighborhood}. Concrete
	 *         implementations have to specify the neighborhood location and
	 *         calibration, so that the found spots can have their coordinates
	 *         put back in the raw source coordinate system.
	 */
	protected abstract SpotNeighborhood<T> getNeighborhood(Spot spot, int frame);

	/**
	 * Returns a new instance of a {@link SpotDetector} that will inspect the
	 * neighborhood.
	 * 
	 * @param img
	 *            the neighborhood to inspect.
	 * @param radius
	 *            the expected spot radius.
	 * @param quality
	 *            the quality threshold below which found spots will be
	 *            discarded.
	 * @return a new {@link SpotTracker}.
	 */
	protected SpotDetector<T> createDetector(final ImgPlus<T> img, final double radius, final double quality) {
		final LogDetector<T> detector = new LogDetector<T>(img, radius, quality, true, false);
		detector.setNumThreads(1);
		return detector;
	}

	@Override
	public boolean checkInput() {
		if (null == model) {
			errorMessage = BASE_ERROR_MESSAGE + "model is null.\n";
			return false;
		}
		if (null == selectionModel) {
			errorMessage = BASE_ERROR_MESSAGE + "selectionModel is null.\n";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads(final int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}

	/**
	 * A utility class made to return the information on a neighborhood
	 * generated from a source around a {@link Spot}.
	 */
	public static class SpotNeighborhood<R> {
		/** The neighborhood, as a calibrated {@link ImgPlus}. */
		public ImgPlus<R> neighborhood;
		/**
		 * Affine transform that will convert the spot coordinates in the
		 * neighborhood reference to the global coordinate system.
		 */
		public AffineTransform3D transform;
	}

}
