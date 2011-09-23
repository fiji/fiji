package fiji.plugin.trackmate;

import fiji.plugin.trackmate.features.spot.SpotFeatureFacade;
import fiji.plugin.trackmate.gui.TrackMateFrameController;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.PlugIn;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.RealType;

import org.jgrapht.graph.SimpleWeightedGraph;


/**
 * <p>The TrackMate_ class runs on the currently active time lapse image (2D or 3D) 
 * and both identifies and tracks bright blobs over time.</p>
 * 
 * <p><b>Required input:</b> A 2D or 3D time-lapse image with bright blobs.</p>
 *
 * @author Nicholas Perry, Jean-Yves Tinevez - Institut Pasteur - July 2010 - 2011
 *
 */
public class TrackMate_ implements PlugIn {

	public static final String PLUGIN_NAME_STR = "Track Mate";
	public static final String PLUGIN_NAME_VERSION = ".beta_2011-09-23";
	public static final boolean DEFAULT_USE_MULTITHREADING = true;

	private TrackMateModel model;
	private boolean useMultithreading = DEFAULT_USE_MULTITHREADING;



	/*
	 * CONSTRUCTORS
	 */

	public TrackMate_() {
		this(new TrackMateModel());
	}

	public TrackMate_(TrackMateModel model) {
		this.model = model;
	}
	
	public TrackMate_(Settings settings) {
		this();
		model.setSettings(settings);
	}


	/*
	 * RUN METHOD
	 */

	/** 
	 * Launch the GUI.
	 */
	public void run(String arg) {
		model.getSettings().imp = WindowManager.getCurrentImage();
		new TrackMateFrameController(this);
	}

	/*
	 * METHODS
	 */

	public TrackMateModel getModel() {
		return model;
	}

	public void setLogger(Logger logger) {
		model.setLogger(logger);
	}


	/*
	 * PROCESSES
	 */


	/**
	 * Calculate all features for all segmented spots.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link SpotFeatureFacade} class
	 * for details. 
	 */
	public void computeSpotFeatures() {
		model.computeSpotFeatures(model.getSpots());
	}

	/**
	 * Calculate all features for all tracks.
	 */
	public void computeTrackFeatures() {
		model.computeTrackFeatures();
	}



	/**
	 * Execute the tracking part.
	 * <p>
	 * This method links all the selected spots from the thresholding part using the selected tracking algorithm.
	 * This tracking process will generate a graph (more precisely a {@link SimpleWeightedGraph}) made of the spot 
	 * election for its vertices, and edges representing the links.
	 * <p>
	 * The {@link TrackMateModelChangeListener}s of this model will be notified when the successful process is over.
	 * @see #getTrackGraph()
	 */ 
	public void execTracking() {
		SpotTracker tracker = model.getSettings().getSpotTracker(model);
		tracker.setLogger(model.getLogger());
		if (tracker.checkInput() && tracker.process()) {
			model.setGraph(tracker.getResult());
		} else
			model.getLogger().error("Problem occured in tracking:\n"+tracker.getErrorMessage()+'\n');
	}

	/** 
	 * Execute the segmentation part.
	 * <p>
	 * This method looks for bright blobs: bright object of approximately spherical shape, whose expected 
	 * diameter is given in argument. The method used for segmentation depends on the {@link SpotSegmenter} 
	 * chosen, and set in {@link #settings};
	 * <p>
	 * This gives us a collection of spots, which at this stage simply wrap a physical center location.
	 * These spots are stored in a {@link SpotCollection} field, {@link #spots}, but listeners of this model
	 * are <b>not</b> notified when the process is over.  
	 * 
	 * @see #getSpots()
	 */
	@SuppressWarnings("unchecked")
	public void execSegmentation() {
		final Settings settings = model.getSettings();
		final float[] calibration = settings.getCalibration();
		final Logger logger = model.getLogger();
		final ImagePlus imp = settings.imp;
		if (null == imp) {
			model.getLogger().error("No image to operate on.\n");
			return;
		}
		final int numFrames = settings.tend - settings.tstart + 1;
		Roi roi = imp.getRoi();
		final Polygon polygon;
		if (roi != null)
			polygon = roi.getPolygon();
		else 
			polygon = null;
		final SpotCollection spots = new SpotCollection();
		final AtomicInteger spotFound = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);

		final Thread[] threads;
		if (useMultithreading) {
			threads = SimpleMultiThreading.newThreads();
		} else {
			threads = SimpleMultiThreading.newThreads(1);
		}


		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger(settings.tstart-1);
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("TrackMate spot feature calculating thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int i = ai.getAndIncrement(); i < settings.tend; i = ai.getAndIncrement()) {

						/* 0 -- Initialize local variables */

						@SuppressWarnings("rawtypes")
						SpotSegmenter<? extends RealType> segmenter = settings.getSpotSegmenter();
						segmenter.setCalibration(calibration);

						/* 1 - Prepare stack for use with Imglib. */
						@SuppressWarnings("rawtypes")
						Image img = TMUtils.getSingleFrameAsImage(imp, i, settings); // will be cropped according to settings

						/* 2 Segment it */
						segmenter.setImage(img);
						if (segmenter.checkInput() && segmenter.process()) {
							List<Spot> spotsThisFrame = segmenter.getResult(settings);
							List<Spot> prunedSpots;
							// Prune if outside of ROI
							if (null != polygon) {
								prunedSpots = new ArrayList<Spot>();
								for (Spot spot : spotsThisFrame) {
									if (polygon.contains(spot.getFeature(SpotFeature.POSITION_X)/calibration[0], spot.getFeature(SpotFeature.POSITION_Y)/calibration[1])) 
										prunedSpots.add(spot);
								}
							} else {
								prunedSpots = spotsThisFrame;
							}
							// Add segmentation feature other than position
							for (Spot spot : prunedSpots) {
								spot.putFeature(SpotFeature.POSITION_T, i * settings.dt);
							}
							spots.put(i, prunedSpots);
							spotFound.addAndGet(prunedSpots.size());
						} else {
							logger.error(segmenter.getErrorMessage()+'\n');
							return;
						}

						logger.setProgress(progress.incrementAndGet() / (float)numFrames );

					} // Finished looping over frames
				}
			};
		}

		logger.setStatus("Segmenting...");
		logger.setProgress(0);

		SimpleMultiThreading.startAndJoin(threads);
		model.setSpots(spots, true);

		logger.log("Found "+spotFound.get()+" spots.\n");
		logger.setProgress(1);
		logger.setStatus("");
		return;
	}

	/**
	 * Execute the initial spot filtering part.
	 *<p>
	 * Because of the presence of noise, it is possible that some of the regional maxima found in the segmenting step have
	 * identified noise, rather than objects of interest. This can generates a very high number of spots, which is
	 * inconvenient to deal with when it comes to  computing their features, or displaying them.
	 * <p>
	 * Any {@link SpotSegmenter} is expected to at least compute the {@link SpotFeature#QUALITY} value for each spot
	 * it creates, so it is possible to set up an initial filtering on this Feature, prior to any other operation. 
	 * <p>
	 * This method simply takes all the segmented spots, and discard those whose quality value is below the threshold set 
	 * by {@link #setInitialSpotFilter(Float)}. The spot field is overwritten, and discarded spots can't be recalled.
	 * <p>
	 * The {@link TrackMateModelChangeListener}s of this model will be notified with a {@link TrackMateModelChangeEvent#SPOTS_COMPUTED}
	 * event.
	 * 
	 * @see #getSpots()
	 * @see #setInitialFilter(Float)
	 */
	public void execInitialSpotFiltering() {
		Float initialSpotFilterValue = model.getInitialSpotFilterValue();
		FeatureFilter<SpotFeature> featureFilter = new FeatureFilter<SpotFeature>(SpotFeature.QUALITY, initialSpotFilterValue, true);
		model.setSpots(model.getSpots().filter(featureFilter), true);
	}

	/**
	 * Execute the spot feature filtering part.
	 *<p>
	 * Because of the presence of noise, it is possible that some of the regional maxima found in the segmenting step have
	 * identified noise, rather than objects of interest. A filtering operation based on the calculated features in this
	 * step should allow to rule them out.
	 * <p>
	 * This method simply takes all the segmented spots, and store in the field {@link #filteredSpots}
	 * the spots whose features satisfy all of the filters entered with the method {@link #addFilter(SpotFilter)}.
	 * <p>
	 * The {@link TrackMateModelChangeListener}s of this model will be notified with a {@link TrackMateModelChangeEvent#SPOTS_FILTERED}
	 * event.
	 * 
	 * @see #getFilteredSpots()
	 */
	public void execSpotFiltering() {
		model.setFilteredSpots(model.getSpots().filter(model.getSpotFilters()), true);
	}

	public void execTrackFiltering() {
		HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(); // will work, for the hash of Integer is its int

		for (int trackIndex = 0; trackIndex < model.getNTracks(); trackIndex++) {
			boolean trackIsOk = true;
			for(FeatureFilter<TrackFeature> filter : model.getTrackFilters()) {
				Float tval = filter.value;
				Float val = model.getTrackFeature(trackIndex, filter.feature);
				if (null == val)
					continue;

				if (filter.isAbove) {
					if (val < tval) {
						trackIsOk = false;
						break;
					}
				} else {
					if (val > tval) {
						trackIsOk = false;
						break;
					}
				}
			}
			if (trackIsOk)
				filteredTrackIndices.add(trackIndex);
		}
		model.setVisibleTrackIndices(filteredTrackIndices, true);
	}



	/*
	 * MAIN METHOD
	 */

	public static void main(String[] args) {
		ij.ImageJ.main(args);
		IJ.open("/Users/tinevez/Desktop/Data/FakeTracks.tif");
		TrackMate_ model = new TrackMate_();
		model.run(null);
	}

}
