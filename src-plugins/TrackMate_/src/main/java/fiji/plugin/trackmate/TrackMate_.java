package fiji.plugin.trackmate;

import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.features.track.TrackFeatureAnalyzer;
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
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jgrapht.graph.SimpleWeightedGraph;


/**
 * <p>The TrackMate_ class runs on the currently active time-lapse image (2D or 3D) 
 * and both identifies and tracks bright spots over time.</p>
 * 
 * <p><b>Required input:</b> A 2D or 3D time-lapse image with bright blobs.</p>
 *
 * @author Nicholas Perry, Jean-Yves Tinevez - Institut Pasteur - July 2010 - 2011 - 2012
 *
 */
public class TrackMate_<T extends RealType<T> & NativeType<T>>  implements PlugIn, Benchmark, MultiThreaded, Algorithm {

	public static final String PLUGIN_NAME_STR = "TrackMate";
	public static final String PLUGIN_NAME_VERSION = "1.3.0";
	public static final boolean DEFAULT_USE_MULTITHREADING = true;

	/** 
	 * The model this plugin will shape.
	 */
	protected TrackMateModel<T> model;

	protected SpotFeatureAnalyzerFactory<T> spotFeatureFactory;
	protected TrackFeatureAnalyzerFactory<T> trackFeatureFactory;
	/** The factory that provides this plugin with available {@link TrackMateModelView}s. */
	protected ViewFactory<T> viewFactory;
	/** The factory that provides this plugin with available {@link TrackMateAction}s. */
	protected ActionFactory<T> actionFactory;
	/** The list of {@link SpotTracker} that will be offered to choose amongst to the user. */
	protected TrackerProvider<T> trackerFactory;
	/** The {@link DetectorProvider} that provides the GUI with the list of available detectors. */
	protected DetectorProvider<T> detectorProvider;
	protected long processingTime;
	protected String errorMessage;
	protected int numThreads = Runtime.getRuntime().availableProcessors();

	/*
	 * CONSTRUCTORS
	 */

	public TrackMate_(Settings<T> settings) {
		this();
		model.setSettings(settings);
	}

	public TrackMate_() {
		this(new TrackMateModel<T>());
	}

	public TrackMate_(TrackMateModel<T> model) {
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
		Settings<T> settings = new Settings<T>(imp);
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
	 * 	<li> {@link #spotFeatureFactory}: the list of Spot feature analyzers that will
	 * be used when calling {@link #computeSpotFeatures()};
	 * 	<li> {@link #trackFeatureFactory}: the list of track feature analyzers that will
	 * be used when calling {@link #computeTrackFeatures()};
	 * 	<li> {@link #spotDetectors}: the list of {@link SpotDetector}s that will be offered 
	 * to the user to choose from;
	 * 	<li> {@link #trackerFactory}: the list of {@link SpotTracker}s that will be offered 
	 * to the user to choose from;
	 * 	<li> {@link #viewFactory}: the list of {@link TrackMateModelView}s (the "displayers")
	 * that will be offered to the user to choose from;
	 * 	<li> {@link #actionFactory}:  the list of {@link TrackMateAction}s that will be 
	 * offered to the user to choose from.
	 * </ul>
	 */
	public void initModules() {
		this.spotFeatureFactory 	= createSpotFeatureAnalyzerFactory();
		this.trackFeatureFactory 	= createTrackFeatureAnalyzerFactory();
		this.detectorProvider		= createDetectorFactory();
		this.trackerFactory 		= createTrackerFactory();
		this.viewFactory 			= createViewFactory();
		this.actionFactory 			= createActionFactory();
		model.getFeatureModel().setSpotFeatureFactory(spotFeatureFactory);
		model.getFeatureModel().setTrackFeatureFactory(trackFeatureFactory);
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
	protected List<Spot> translateAndPruneSpots(final List<Spot> spotsThisFrame, final Settings<T> settings) {

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
		new WizardController<T>(this);
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 *  Create view factory containing available spot {@link TrackMateSelectionView}s.
	 */
	protected ViewFactory<T> createViewFactory() {
		return new ViewFactory<T>();
	}


	/**
	 * Hook for subclassers.
	 * <p>
	 * Create detector provider that manages available {@link SpotDetectorFactory}.
	 */
	protected DetectorProvider<T> createDetectorFactory() {
		return new DetectorProvider<T>();
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create detector factory containing available spot {@link SpotFeatureAnalyzer}s.
	 */
	protected SpotFeatureAnalyzerFactory<T> createSpotFeatureAnalyzerFactory() {
		return new SpotFeatureAnalyzerFactory<T>();
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create detector factory containing available spot {@link SpotTracker}s.
	 */
	protected TrackerProvider<T> createTrackerFactory() {
		return new TrackerProvider<T>(model);
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create detector factory containing available spot {@link TrackFeatureAnalyzer}s.
	 */
	protected TrackFeatureAnalyzerFactory<T> createTrackFeatureAnalyzerFactory() {
		return new TrackFeatureAnalyzerFactory<T>();
	}

	/**
	 * Hook for subclassers.
	 * <p>
	 * Create action factory containing available spot {@link TrackMateAction}s.
	 */
	protected ActionFactory<T> createActionFactory() {
		return new ActionFactory<T>();
	}

	/*
	 * METHODS
	 */

	public TrackMateModel<T> getModel() {
		return model;
	}

	public void setLogger(Logger logger) {
		model.setLogger(logger);
	}

	/**
	 * @return the {@link SpotFeatureAnalyzerFactory} currently registered in this plugin.
	 * @see #createSpotFeatureAnalyzerFactory()
	 */
	public SpotFeatureAnalyzerFactory<T> getAvailableSpotFeatureAnalyzers() {
		return spotFeatureFactory;
	}

	/**
	 * Return a list of the {@link SpotDetector} that are currently registered in this plugin.
	 */
	public DetectorProvider<T> getDetectorProvider() {
		return detectorProvider;
	}

	/**
	 * Return a list of the {@link SpotTracker} that are currently registered in this plugin.
	 */
	public TrackerProvider<T> getTrackerProvider() {
		return trackerFactory;
	}


	/**
	 * Return a list of the {@link TrackMateModelView} that are currently registered in this plugin.
	 */
	public ViewFactory<T> getViewFactory() {
		return viewFactory;
	}

	/**
	 * Return a list of the {@link TrackMateAction} that are currently registered in this plugin.
	 */
	public ActionFactory<T> getActionFactory() {
		return actionFactory;
	}


	/*
	 * PROCESSES
	 */

	/**
	 * Calculate all features for all detected spots.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. 
	 */
	public void computeSpotFeatures() {
		final Logger logger = model.getLogger();
		logger.log("Computing spot features.\n");
		model.getFeatureModel().computeSpotFeatures(model.getSpots());
	}

	/**
	 * Calculate all features for all tracks.
	 */
	public void computeTrackFeatures() {
		final Logger logger = model.getLogger();
		logger.log("Computing track features.\n");
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
	public boolean execTracking() {
		final Logger logger = model.getLogger();
		logger.log("Starting tracking process.\n");
		SpotTracker tracker =  model.getSettings().tracker;
		tracker.setSettings(model.getSettings().trackerSettings);
		if (tracker.checkInput() && tracker.process()) {
			model.setGraph(tracker.getResult());
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
	public boolean execDetection() {
		final Logger logger = model.getLogger();
		logger.log("Starting detection process.\n");
		
		final Settings<T> settings = model.getSettings();
		final SpotDetectorFactory<T> factory = settings.detectorFactory;
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
		ImgPlus<T> rawImg = ImagePlusAdapter.wrapImgPlus(settings.imp);
		ImgPlus<T> img;

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
			CropImgView<T> cropView = new CropImgView<T>(rawImg, min, max);
			// Put back metadata in a new ImgPlus 
			img = new ImgPlus<T>(cropView, rawImg);
			
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
						SpotDetector<T> detector = factory.getDetector(frame);

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
								spot.putFeature(Spot.POSITION_T, frame * settings.dt);
								spot.putFeature(Spot.FRAME, frame);
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
	 * The {@link TrackMateModelChangeListener}s of this model will be notified with a {@link TrackMateModelChangeEvent#SPOTS_COMPUTED}
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
	 * The {@link TrackMateModelChangeListener}s of this model will be notified with a {@link TrackMateModelChangeEvent#SPOTS_FILTERED}
	 * event.
	 * 
	 * @see #getFilteredSpots()
	 */
	public boolean execSpotFiltering() {
		final Logger logger = model.getLogger();
		logger.log("Starting spot filtering process.\n");
		model.setFilteredSpots(model.getSpots().filter(model.getSettings().getSpotFilters()), true);
		return true;
	}

	public boolean execTrackFiltering() {
		final Logger logger = model.getLogger();
		logger.log("Starting track filtering process.\n");
		HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(); // will work, for the hash of Integer is its int

		for (int trackIndex = 0; trackIndex < model.getNTracks(); trackIndex++) {
			boolean trackIsOk = true;
			for(FeatureFilter filter : model.getSettings().getTrackFilters()) {
				Double tval = filter.value;
				Double val = model.getFeatureModel().getTrackFeature(trackIndex, filter.feature);
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
		Settings<T> settings = model.getSettings();
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
		computeSpotFeatures();
		if (!execSpotFiltering()) {
			return false;
		}
		if (!execTracking()) {
			return false;
		}
		computeTrackFeatures();
		if (!execTrackFiltering()) {
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
