package fiji.plugin.trackmate;

import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.WizardController;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.CropImgView;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.ImgPlus;
import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.SimpleWeightedGraph;


/**
 * <p>The TrackMate_ class runs on the currently active time-lapse image (2D or 3D) 
 * and both identifies and tracks bright spots over time.</p>
 * 
 * <p><b>Required input:</b> A 2D or 3D time-lapse image with bright blobs.</p>
 *
 * @author Nicholas Perry, Jean-Yves Tinevez - Institut Pasteur - July 2010 - 2011 - 2012 - 2013
 *
 */
public class TrackMate_ implements PlugIn, Benchmark, MultiThreaded, Algorithm {

	public static final String PLUGIN_NAME_STR = "TrackMate";
	public static final String PLUGIN_NAME_VERSION = "2.0.4";
	public static final boolean DEFAULT_USE_MULTITHREADING = true;

	/** 
	 * The model this plugin will shape.
	 */
	protected TrackMateModel model;

	protected SpotAnalyzerProvider spotFeatureAnalyzerProvider;
	protected EdgeAnalyzerProvider edgeFeatureAnalyzerProvider;
	protected TrackAnalyzerProvider trackFeatureProvider;
	/** The factory that provides this plugin with available {@link TrackMateModelView}s. */
	protected ViewProvider viewProvider;
	/** The factory that provides this plugin with available {@link TrackMateAction}s. */
	protected ActionProvider actionProvider;
	/** The list of {@link SpotTracker} that will be offered to choose amongst to the user. */
	protected TrackerProvider trackerProvider;
	/** The {@link DetectorProvider} that provides the GUI with the list of available detectors. */
	protected DetectorProvider detectorProvider;
	protected long processingTime;
	protected String errorMessage;
	protected int numThreads = Runtime.getRuntime().availableProcessors();

	/*
	 * CONSTRUCTORS
	 */

	public TrackMate_(Settings settings) {
		this();
		model.setSettings(settings);
	}

	public TrackMate_() {
		this(new TrackMateModel());
	}

	public TrackMate_(TrackMateModel model) {
		this.model = model;
	}


	/*
	 * RUN METHOD
	 */

	/** 
	 * Launch the GUI.
	 */
	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		Settings settings = new Settings(imp);
		model.setSettings(settings);
		initModules();
		launchGUI();
	}

	/**
	 * This method instantiate the fields that contain all currently possible choices
	 * offered to the user in the GUI of this plugin. It needs to be called solely in
	 * the GUI context, when instantiating a new {@link TrackMate_} plugin that will
	 * be used with the {@link TrackMateWizard} GUI. To use the plugin in batch mode
	 * or in scripts, you do not need to call this method, which will save you the 
	 * time needed to instantiate all the modules.
	 * This method to be called also if you are going to load a xml file and want the
	 * GUI and {@link Settings} object properly reflecting the saved state.  
	 * <p>
	 * More precisely, the modules and fields instantiated by this method are:
	 * <ul>
	 * 	<li> {@link #spotFeatureAnalyzerProvider}: the list of Spot feature analyzers that will
	 * be used when calling {@link #computeSpotFeatures()};
	 * 	<li> {@link #trackFeatureProvider}: the list of track feature analyzers that will
	 * be used when calling {@link #computeTrackFeatures()};
	 * 	<li> {@link #spotDetectors}: the list of {@link SpotDetector}s that will be offered 
	 * to the user to choose from;
	 * 	<li> {@link #trackerProvider}: the list of {@link SpotTracker}s that will be offered 
	 * to the user to choose from;
	 * 	<li> {@link #viewProvider}: the list of {@link TrackMateModelView}s (the "displayers")
	 * that will be offered to the user to choose from;
	 * 	<li> {@link #actionProvider}:  the list of {@link TrackMateAction}s that will be 
	 * offered to the user to choose from.
	 * </ul>
	 */
	public void initModules() {
		this.spotFeatureAnalyzerProvider 	= createSpotAnalyzerProvider();
		this.trackFeatureProvider 	= createTrackAnalyzerProvider();
		this.edgeFeatureAnalyzerProvider = createEdgeAnalyzerProvider();
		this.detectorProvider		= createDetectorProvider();
		this.trackerProvider 		= createTrackerProvider();
		this.viewProvider 			= createViewProvider();
		this.actionProvider 			= createActionProvider();
		model.getFeatureModel().setSpotFeatureFactory(spotFeatureAnalyzerProvider);
		model.getFeatureModel().setTrackAnalyzerProvider(trackFeatureProvider);
		model.getFeatureModel().setEdgeAnalyzerProvider(edgeFeatureAnalyzerProvider);
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * This method exists for the following reason:
	 * <p>
	 * The detector receives at each frame a cropped image to operate on, depending
	 * on the user specifying a ROI. It therefore returns spots whose coordinates are 
	 * with respect to the top-left corner of the ROI, not of the original image. 
	 * <p>
	 * This method modifies the given spots to put them back in the image coordinate
	 * system. Additionally, is a non-square ROI was specified (e.g. a polygon), it 
	 * prunes the spots that are not within the polygon of the ROI.
	 * @param spotsThisFrame  the spot list to inspect
	 * @param settings  the {@link Settings} object that will be used to retrieve the image ROI
	 * and cropping information
	 * @return  a list of spot. Depending on the presence of a polygon ROI, it might be a new, 
	 * pruned list. Or not.
	 */
	protected List<Spot> translateAndPruneSpots(final List<Spot> spotsThisFrame, final Settings settings) {

		// Put them back in the right referential 
		final double[] calibration = TMUtils.getSpatialCalibration(settings.imp);
		TMUtils.translateSpots(spotsThisFrame, 
				settings.xstart * calibration[0], 
				settings.ystart * calibration[1], 
				settings.zstart * calibration[2]);
		List<Spot> prunedSpots;
		// Prune if outside of ROI
		if (null != settings.polygon) {
			prunedSpots = new ArrayList<Spot>();
			for (Spot spot : spotsThisFrame) {
				if (settings.polygon.contains(spot.getFeature(Spot.POSITION_X)/calibration[0], spot.getFeature(Spot.POSITION_Y)/calibration[1])) 
					prunedSpots.add(spot);
			}
		} else {
			prunedSpots = spotsThisFrame;
		}
		return prunedSpots;
	}


	/**
	 * Hook for subclassers.
	 * <p>
	 * Create and launch the GUI that will control this plugin. You can override this method
	 * if you want to use another GUI, or use a the {@link WizardController} extended
	 * to suit your needs.
	 */
	protected void launchGUI() {
		new WizardController(this);
	}

	/*
	 * PROVIDERS
	 */
	
	/**
	 * Hook for subclassers.
	 * <p>
	 *  Create view factory containing available spot {@link TrackMateSelectionView}s.
	 */
	protected ViewProvider createViewProvider() {
		return new ViewProvider(model);
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create detector provider that manages available {@link SpotDetectorFactory}.
	 */
	protected DetectorProvider createDetectorProvider() {
		return new DetectorProvider();
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the spot feature analyzer provider.
	 */
	protected SpotAnalyzerProvider createSpotAnalyzerProvider() {
		return new SpotAnalyzerProvider(model);
	}
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the edge feature analyzer provider.
	 */
	protected EdgeAnalyzerProvider createEdgeAnalyzerProvider() {
		return new EdgeAnalyzerProvider(model);
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create detector factory containing available spot {@link TrackAnalyzer}s.
	 */
	protected TrackAnalyzerProvider createTrackAnalyzerProvider() {
		return new TrackAnalyzerProvider(model);
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create detector factory containing available spot {@link SpotTracker}s.
	 */
	protected TrackerProvider createTrackerProvider() {
		return new TrackerProvider(model);
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create action factory containing available spot {@link TrackMateAction}s.
	 */
	protected ActionProvider createActionProvider() {
		return new ActionProvider();
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

	/**
	 * @return the {@link SpotAnalyzerProvider} currently set.
	 * @see #createSpotAnalyzerProvider()
	 */
	public SpotAnalyzerProvider getSpotFeatureAnalyzerProvider() {
		return spotFeatureAnalyzerProvider;
	}

	/**
	 * @return the {@link TrackAnalyzerProvider} currently set.
	 */
	public TrackAnalyzerProvider getTrackFeatureProvider() {
		return trackFeatureProvider;
	}
	
	/**
	 * @return the {@link EdgeAnalyzerProvider} currently set.
	 */
	public EdgeAnalyzerProvider getEdgeFeatureAnalyzerProvider() {
		return edgeFeatureAnalyzerProvider;
	}
	
	/**
	 * @return the {@link DetectorProvider} currently set.
	 */
	public DetectorProvider getDetectorProvider() {
		return detectorProvider;
	}

	/**
	 * @return the {@link TrackerProvider} currently set.
	 */
	public TrackerProvider getTrackerProvider() {
		return trackerProvider;
	}


	/**
	 * @return the {@link ViewProvider} currently set.
	 */
	public ViewProvider getViewProvider() {
		return viewProvider;
	}

	/**
	 * @return a list of the {@link TrackMateAction} that are currently registered in this plugin.
	 */
	public ActionProvider getActionProvider() {
		return actionProvider;
	}


	/*
	 * PROCESSES
	 */

	/**
	 * Calculate all features for all detected spots.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. 
	 */
	public void computeSpotFeatures(boolean doLogIt) {
		final Logger logger = model.getLogger();
		logger.log("Computing spot features.\n");
		model.getFeatureModel().computeSpotFeatures(model.getSpots(), doLogIt);
	}

	public void computeEdgeFeatures(boolean doLogIt) {
		model.getFeatureModel().computeEdgeFeatures(model.getTrackModel().edgeSet(), doLogIt);
	}
	
	/**
	 * Calculate all features for all tracks.
	 */
	public void computeTrackFeatures(boolean doLogIt) {
		model.getFeatureModel().computeTrackFeatures(model.getTrackModel().getTrackIDs(), doLogIt);
	}

	/**
	 * Execute the tracking part.
	 * <p>
	 * This method links all the selected spots from the thresholding part using the selected tracking algorithm.
	 * This tracking process will generate a graph (more precisely a {@link SimpleWeightedGraph}) made of the spot 
	 * election for its vertices, and edges representing the links.
	 * <p>
	 * The {@link ModelChangeListener}s of this model will be notified when the successful process is over.
	 * @see #getTrackGraph()
	 */ 
	public boolean execTracking() {
		final Logger logger = model.getLogger();
		logger.log("Starting tracking process.\n");
		SpotTracker tracker =  model.getSettings().tracker;
		tracker.setSettings(model.getSettings().trackerSettings);
		if (tracker.checkInput() && tracker.process()) {
			model.getTrackModel().setGraph(tracker.getResult());
			return false;
		} else {
			errorMessage = "Tracking process failed:\n"+tracker.getErrorMessage();
			return false;
		}
	}

	/** 
	 * Execute the detection part.
	 * <p>
	 * This method configure the chosen {@link Settings#detectorFactory} with the source image 
	 * and the detectr settings and execute the detection process for all the frames set 
	 * in the {@link Settings} object of the target model.
	 * @return true if the whole detection step has exectued correctly.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean execDetection() {
		final Logger logger = model.getLogger();
		logger.log("Starting detection process.\n");
		
		final Settings settings = model.getSettings();
		final SpotDetectorFactory<?> factory = settings.detectorFactory;
		if (null == factory) {
			errorMessage = "Detector factory is null.\n";
			return false;
		}
		if (null == settings.detectorSettings) {
			errorMessage  = "Detector settings is null.\n";
			return false;
		}

		/*
		 *  Prepare cropped image
		 */
		ImgPlus rawImg = TMUtils.rawWraps(settings.imp);
		ImgPlus img;

		// Check if we indeed wish to crop the source image. To this, we check
		// the crop cube settings

		if (settings.xstart != 0 
				|| settings.ystart != 0
				|| settings.zstart != 0
				|| settings.xend != settings.imp.getWidth()-1
				|| settings.yend != settings.imp.getHeight()-1
				|| settings.zend != settings.imp.getNSlices()-1) {
			// Yes, we want to crop

			long[] max = new long[rawImg.numDimensions()];
			long[] min = new long[rawImg.numDimensions()];
			// X, we must have it
			int xindex = TMUtils.findXAxisIndex(rawImg);
			if (xindex < 0) {
				errorMessage = "Source image has no X axis.\n";
				return false;
			}
			min[xindex] = settings.xstart;
			max[xindex] = settings.xend;
			// Y, we must have it
			int yindex = TMUtils.findYAxisIndex(rawImg);
			if (yindex < 0) {
				errorMessage  = "Source image has no Y axis.\n";
				return false;
			}
			min[yindex] = settings.ystart;
			max[yindex] = settings.yend;
			// Z, we MIGHT have it
			int zindex = TMUtils.findZAxisIndex(rawImg);
			if (zindex >= 0) {
				min[zindex] = settings.zstart;
				max[zindex] = settings.zend;
			}
			// CHANNEL, we might have it 
			int cindex = TMUtils.findCAxisIndex(rawImg);
			if (cindex >= 0) {
				min[cindex] = 0;
				max[cindex] = settings.imp.getNChannels();
			}
			// TIME, we might have it, but anyway we leave the start & end management to the threads below  
			int tindex = TMUtils.findTAxisIndex(rawImg);
			if (tindex >= 0) {
				min[tindex] = 0;
				max[tindex] = settings.imp.getNFrames();
			}
			// crop: we now have a cropped view of the source image
			CropImgView cropView = new CropImgView(rawImg, min, max);
			// Put back metadata in a new ImgPlus 
			img = new ImgPlus(cropView, rawImg);
			
		} else {
			img = rawImg;
		}

		factory.setTarget(img, settings.detectorSettings);

		final int numFrames = settings.tend - settings.tstart + 1;
		// Final results holder, for all frames
		final SpotCollection spots = new SpotCollection();
		spots.setNumThreads(numThreads);
		// To report progress
		final AtomicInteger spotFound = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);
		// To translate spots, later
		final double[] calibration = TMUtils.getSpatialCalibration(settings.imp);
		final double dx = settings.xstart * calibration[0];
		final double dy = settings.ystart * calibration[1];
		final double dz = settings.zstart * calibration[2];

		final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		final AtomicBoolean ok = new AtomicBoolean(true);

		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger(settings.tstart);
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("TrackMate spot detection thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int frame = ai.getAndIncrement(); frame <= settings.tend; frame = ai.getAndIncrement()) {

						// Yield detector for target frame
						SpotDetector<?> detector = factory.getDetector(frame);

						// Execute detection
						if (ok.get() && detector.checkInput() && detector.process()) {
							// On success,
							// Get results,
							List<Spot> spotsThisFrame = detector.getResult();
							// Translate individual spots back to top-left corner of the image, if
							// the raw image was cropped.
							TMUtils.translateSpots(spotsThisFrame, dx, dy, dz);
							// Prune if outside of ROI
							List<Spot> prunedSpots;
							if (null != settings.polygon) {
								prunedSpots = new ArrayList<Spot>();
								for (Spot spot : spotsThisFrame) {
									if (settings.polygon.contains(spot.getFeature(Spot.POSITION_X)/calibration[0], spot.getFeature(Spot.POSITION_Y)/calibration[1])) 
										prunedSpots.add(spot);
								}
							} else {
								prunedSpots = spotsThisFrame;
							}
							// Add detection feature other than position
							for (Spot spot : prunedSpots) {
								spot.putFeature(Spot.POSITION_T, frame * settings.dt); // FRAME will be set upon adding to SpotCollection
							}
							// Store final results for this frame
							spots.put(frame, prunedSpots);
							// Report 
							spotFound.addAndGet(prunedSpots.size());
							logger.setProgress(progress.incrementAndGet() / (double)numFrames );

						} else {
							// Fail: exit and report error.
							ok.set(false);
							errorMessage = detector.getErrorMessage();
							return;
						}

					} // Finished looping over frames
				}
			};
		}

		logger.setStatus("Detection...");
		logger.setProgress(0);

		SimpleMultiThreading.startAndJoin(threads);
		model.setSpots(spots, true);

		if (ok.get()) {
			logger.log("Found "+spotFound.get()+" spots.\n");
		} else {
			logger.error("Detection failed after "+progress.get()+" frame:\n"+errorMessage);
		}
		logger.setProgress(1);
		logger.setStatus("");
		return ok.get();
	}

	/**
	 * Execute the initial spot filtering part.
	 *<p>
	 * Because of the presence of noise, it is possible that some of the regional maxima found in the detection step have
	 * identified noise, rather than objects of interest. This can generates a very high number of spots, which is
	 * inconvenient to deal with when it comes to  computing their features, or displaying them.
	 * <p>
	 * Any {@link SpotDetector} is expected to at least compute the {@link SpotFeature#QUALITY} value for each spot
	 * it creates, so it is possible to set up an initial filtering on this Feature, prior to any other operation. 
	 * <p>
	 * This method simply takes all the detected spots, and discard those whose quality value is below the threshold set 
	 * by {@link #setInitialSpotFilter(Float)}. The spot field is overwritten, and discarded spots can't be recalled.
	 * <p>
	 * The {@link ModelChangeListener}s of this model will be notified with a {@link ModelChangeEvent#SPOTS_COMPUTED}
	 * event.
	 * 
	 * @see #getSpots()
	 * @see #setInitialFilter(Float)
	 */
	public boolean execInitialSpotFiltering() {
		final Logger logger = model.getLogger();
		logger.log("Starting initial filtering process.\n");
		Double initialSpotFilterValue = model.getSettings().initialSpotFilterValue;
		FeatureFilter featureFilter = new FeatureFilter(Spot.QUALITY, initialSpotFilterValue, true);
		model.setSpots(model.getSpots().filter(featureFilter), true);
		return true;
	}

	/**
	 * Execute the spot feature filtering part.
	 *<p>
	 * Because of the presence of noise, it is possible that some of the regional maxima found in the detection step have
	 * identified noise, rather than objects of interest. A filtering operation based on the calculated features in this
	 * step should allow to rule them out.
	 * <p>
	 * This method simply takes all the detected spots, and store in the field {@link #filteredSpots}
	 * the spots whose features satisfy all of the filters entered with the method {@link #addFilter(SpotFilter)}.
	 * <p>
	 * The {@link ModelChangeListener}s of this model will be notified with a {@link ModelChangeEvent#SPOTS_FILTERED}
	 * event.
	 * @param doLogIt  if true, will send a message to the {@link TrackMateModel#logger}.
	 * @see #getFilteredSpots()
	 */
	public boolean execSpotFiltering(boolean doLogIt) {
		if (doLogIt) {
			final Logger logger = model.getLogger();
			logger.log("Starting spot filtering process.\n");
		}
		model.setFilteredSpots(model.getSpots().filter(model.getSettings().getSpotFilters()), true);
		return true;
	}

	public boolean execTrackFiltering(boolean doLogIt) {
		if (doLogIt) {
			Logger logger = model.getLogger();
			logger.log("Starting track filtering process.\n");
		}
		HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(); // will work, for the hash of Integer is its int

		for (Integer trackID : model.getTrackModel().getTrackIDs()) {
			boolean trackIsOk = true;
			for(FeatureFilter filter : model.getSettings().getTrackFilters()) {
				Double tval = filter.value;
				Double val = model.getFeatureModel().getTrackFeature(trackID, filter.feature);
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
				filteredTrackIndices.add(trackID);
		}
		model.getTrackModel().setFilteredTrackIDs(filteredTrackIndices, true);
		return true;
	}


	public String toString() {
		return PLUGIN_NAME_STR + "v" + PLUGIN_NAME_VERSION;
	}

	/*
	 * ALGORITHM METHODS
	 */
	
	@Override
	public boolean checkInput() {
		if (null == model) {
			errorMessage = "The model is null.\n";
			return false;
		}
		Settings settings = model.getSettings();
		if (null == settings) {
			errorMessage = "Settings in the model are null";
			return false;
		}
		if (!settings.checkValidity()) {
			errorMessage = settings.getErrorMessage();
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public boolean process() {
		if (!execDetection()) {
			return false;
		}
		if (!execInitialSpotFiltering()) {
			return false;
		}
		computeSpotFeatures(true);
		if (!execSpotFiltering(true)) {
			return false;
		}
		if (!execTracking()) {
			return false;
		}
		computeTrackFeatures(true);
		if (!execTrackFiltering(true)) {
			return false;
		}
		return true;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
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
	public long getProcessingTime() {
		return processingTime;
	};
	
}


