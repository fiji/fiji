package fiji.plugin.trackmate.detection.semiauto;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.ImgPlus;
import net.imglib2.multithreading.SimpleMultiThreading;
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
 * A class made to perform semi-automated tracking of spots in TrackMate & friends
 * <p>
 * The user has to select one spot, one a meaningful location.
 * The spot location and its radius are then used to extract a small rectangular
 * neighborhood in the next frame around the spot. The neighborhood is then
 * passed to a {@link SpotDetector} that returns the spot it found. If a spot
 * of {@link Spot#QUALITY} high enough is found near enough to the first spot
 * center, then it is added to the model and linked with the first spot.
 * <p> 
 * The process is then repeated, taking the newly found spot as a source 
 * for the next neighborhood.
 * The model is updated live for every spot found.
 * <p>
 * The process halts when:
 * <ul>
 * 	<li> no spots of quality high enough are found;
 * 	<li> spots of high quality are found, but too far from the initial spot;
 * 	<li> the source has no time-point left. 
 * </ul>
 * 
 * @author Jean-Yves Tinevez - 2013
 * @param <T> the type of the source. Must extend {@link RealType} and {@link NativeType}
 * to use with most TrackMate {@link SpotDetector}s.
 */
public abstract class AbstractSemiAutoTracker<T extends RealType<T>  & NativeType<T>> implements Algorithm, MultiThreaded {

	protected static final String BASE_ERROR_MESSAGE = "[SemiAutoTracker] ";
	/** The size of the local neighborhood to inspect, in units of the source spot diameter. */
	protected static final double NEIGHBORHOOD_FACTOR = 3d;
	/** The quality drop we tolerate before aborting detection. The highest, the more intolerant. */
	protected static final double QUALITY_THRESHOLD = 0.2d;
	/** How close must be the new spot found to be accepted, in radius units. */
	protected static final double DISTANCE_TOLERANCE = 1.1d;
	private final Model model;
	private final SelectionModel selectionModel;
	protected String errorMessage;
	private int numThreads;
	protected boolean ok;
	protected final Logger logger;

	/*
	 * CONSTRUCTOR 
	 */

	public AbstractSemiAutoTracker(Model model, SelectionModel selectionModel, Logger logger) {
		this.model = model;
		this.selectionModel = selectionModel;
		this.logger = logger;
	}

	/*
	 * METHODS
	 */
	
	
	@Override
	public boolean process() {
		final Set<Spot> spots = new HashSet<Spot>(selectionModel.getSpotSelection());
		if (spots.isEmpty()) {
			errorMessage = BASE_ERROR_MESSAGE + "No spots in selection.";
			return false;
		}
		selectionModel.clearSelection();
		
		int nThreads = Math.min(numThreads, spots.size());
		final ArrayBlockingQueue<Spot> queue = new ArrayBlockingQueue<Spot>(spots.size(), false, spots);
		
			
		ok = true;
		final ThreadGroup semiAutoTrackingThreadgroup = new ThreadGroup( "Mamut semi-auto tracking threads" );
		Thread[] threads = SimpleMultiThreading.newThreads(nThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread( semiAutoTrackingThreadgroup, new Runnable() {
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

	public void processSpot(final Spot initialSpot) {

		/*
		 * Initial spot
		 */
		Spot spot = initialSpot;

		while (true) {


			/*
			 * Extract spot & features
			 */

			int frame = spot.getFeature(Spot.FRAME).intValue() + 1; // We want to segment in the next frame
			double radius = spot.getFeature(Spot.RADIUS);
			double quality = spot.getFeature(Spot.QUALITY);
			
			
			/* 
			 * Get neighborhood
			 */
			
			SpotNeighborhood<T> sn = getNeighborhood(spot, frame);
			if (null == sn) {
				return;
			}
			
			ImgPlus<T> neighborhood = sn.neighborhood;
			long[] min = sn.topLeftCorner;
			double[] cal = sn.calibration;

			/*
			 * Detect spots
			 */
			
			SpotDetector<T> detector = createDetector(neighborhood , radius, quality * QUALITY_THRESHOLD);

			if (!detector.checkInput() || !detector.process()) {
				ok = false;
				errorMessage = detector.getErrorMessage();
				logger.error("Spot: " + initialSpot + ": Detection problen: " + detector.getErrorMessage());
				return;
			}

			/*
			 * Get results
			 */

			List<Spot> detectedSpots = detector.getResult();
			if (detectedSpots.isEmpty()) {
				logger.log("Spot: " + initialSpot + ": No suitable spot found.");
				return;
			}
			
			/*
			 * Translate spots
			 */
			
			String[] features = new String[] { Spot.POSITION_X, Spot.POSITION_Y, Spot.POSITION_Z }; 
			for (Spot ds : detectedSpots) {
				for (int i = 0; i < features.length; i++) {
					Double val = ds.getFeature(features[i]);
					ds.putFeature(features[i], val + (double) min[i] * cal[i]);
				}
			}
			
			// Sort then by ascending quality
			TreeSet<Spot> sortedSpots = new TreeSet<Spot>(Spot.featureComparator(Spot.QUALITY));
			sortedSpots.addAll(detectedSpots);

			boolean found = false;
			Spot target = null;
			for (Iterator<Spot> iterator = sortedSpots.descendingIterator(); iterator.hasNext();) {
				Spot candidate = iterator.next();
				if (candidate.squareDistanceTo(spot) < DISTANCE_TOLERANCE * DISTANCE_TOLERANCE * radius * radius) {
					found = true;
					target = candidate;
					break;
				}
			}

			if (!found) {
				logger.log("Spot: " + initialSpot + ": Suitable spot found, but outside the tolerance radius.");
				return;
			}
			
			/*
			 * Update model
			 */

			// spot
			target.putFeature(Spot.RADIUS, radius );
			target.putFeature(Spot.POSITION_T, Double.valueOf(frame) );
			
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
	 * Returns a small neighborhood around the specified spot, but taken at the specified frame.
	 * Implementations have to decide what is the right size for the neighborhood, given the
	 * specified spot radius and location.
	 * <p>
	 * Implementations can return <code>null</code> if for instance, the number of time 
	 * frames in the raw source has been exhausted, or if the specified spot misses some information.
	 * This will be dealt with gracefully in the {@link #process()} method.  
	 * @param spot  the spot the desired neighborhood is centered on.
	 * @param frame  the frame in the source image the desired neighborhood as to be taken.
	 * @return  the neighborhood, as a {@link SpotNeighborhood}. Concrete implementations
	 * have to specify the neighborhood location and calibration, so that the found spots
	 * can have their coordinates put back in the raw source coordinate system.
	 */
	protected abstract SpotNeighborhood<T> getNeighborhood(Spot spot, int frame);

	/**
	 * Returns a new instance of a {@link SpotDetector} that will inspect the neighborhood.
	 * @param img  the neighborhood to inspect.
	 * @param radius  the expected spot radius. 
	 * @param quality  the quality threshold below which found spots will be discarded.
	 * @return  a new {@link SpotTracker}.
	 */
	protected SpotDetector<T> createDetector(ImgPlus<T> img, double radius, double quality) {
		LogDetector<T> detector = new LogDetector<T>(img, radius, quality, true, false);
		detector.setNumThreads(1);
		return detector;
	}

	@Override
	public boolean checkInput() {
		if (null == model) {
			errorMessage = BASE_ERROR_MESSAGE + "model is null.";
			return false;
		}
		if (null == selectionModel) {
			errorMessage = BASE_ERROR_MESSAGE + "selectionModel is null.";
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
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}

	
	/**
	 * A utility class made to return the information on a neighborhood generated
	 * from a source around a {@link Spot}.
	 */
	public static class SpotNeighborhood <R> {
		/** The neighborhood, as a calibrated {@link ImgPlus}. */
		public ImgPlus<R> neighborhood;
		/**  The location <b>in pixel units</b> of the top-left corner of the generated
		 * neighborhood. This is required to set the coordinates of the detected spots with the
		 * correct origin. */ 
		public long[] topLeftCorner;
		/** the physical calibration of the raw source. */
		public double[] calibration;
	}

}