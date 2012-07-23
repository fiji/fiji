package fiji.plugin.trackmate;

import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.features.track.TrackFeatureAnalyzer;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.WizardController;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateSelectionView;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;

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
public class TrackMate_<T extends RealType<T> & NativeType<T>>  implements PlugIn {

	public static final String PLUGIN_NAME_STR = "TrackMate";
	public static final String PLUGIN_NAME_VERSION = "1.3";
	public static final boolean DEFAULT_USE_MULTITHREADING = true;

	protected TrackMateModel<T> model;
	protected boolean useMultithreading = DEFAULT_USE_MULTITHREADING;

	protected SpotFeatureAnalyzerFactory<T> spotFeatureFactory;
	protected TrackFeatureAnalyzerFactory<T> trackFeatureFactory;
	/** The factory that provides this plugin with available {@link TrackMateModelView}s. */
	protected ViewFactory<T> viewFactory;
	/** The factory that provides this plugin with available {@link TrackMateAction}s. */
	protected ActionFactory<T> actionFactory;
	/** The list of {@link SpotTracker} that will be offered to choose amongst to the user. */
	protected TrackerFactory<T> trackerFactory;
	/** The {@link SpotDetector} factory that provide the GUI with the list of available detectors. */
	protected DetectorFactory<T> detectorFactory;

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
		this.detectorFactory		= createDetectorFactory();
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
	
	
	protected List<Spot> execSingleFrameDetection(final ImgPlus<T> img, Settings<T> settings) {
		
		SpotDetector<T> detector = detectorFactory.getDetector(settings.detector);
		detector.setTarget(img, settings.detectorSettings);

		if (detector.checkInput() && detector.process()) {
			List<Spot> spotsThisFrame = detector.getResult();
			return translateAndPruneSpots(spotsThisFrame, settings);
			
		} else {
			model.getLogger().error(detector.getErrorMessage()+'\n');
			return null;
		}

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
	 * Create detector factory containing available spot {@link SpotDetector}s.
	 */
	protected DetectorFactory<T> createDetectorFactory() {
		return new DetectorFactory<T>();
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
	protected TrackerFactory<T> createTrackerFactory() {
		return new TrackerFactory<T>();
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
	public DetectorFactory<T> getDetectorFactory() {
		return detectorFactory;
	}
	
	/**
	 * Return a list of the {@link SpotTracker} that are currently registered in this plugin.
	 */
	public TrackerFactory<T> getTrackerFactory() {
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
		model.getFeatureModel().computeSpotFeatures(model.getSpots());
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
		String trackerName = model.getSettings().tracker;
		SpotTracker<T> tracker = trackerFactory.getTracker(trackerName);
		if (null == tracker) {
			model.getLogger().error("Tracker named "+trackerName+" is not available in "+toString());
		}
		tracker.setModel(model);
		tracker.setLogger(model.getLogger());
		if (tracker.checkInput() && tracker.process()) {
			model.setGraph(tracker.getResult());
		} else
			model.getLogger().error("Problem occured in tracking:\n"+tracker.getErrorMessage()+'\n');
	}

	/** 
	 * Execute the detection part.
	 * <p>
	 * This method looks for bright blobs: bright object of approximately spherical shape, whose expected 
	 * diameter is given in argument. The method used for segmentation depends on the {@link SpotDetector} 
	 * chosen, and set in {@link #settings};
	 * <p>
	 * This gives us a collection of spots, which at this stage simply wrap a physical center location.
	 * These spots are stored in a {@link SpotCollection} field, {@link #spots}, but listeners of this model
	 * are <b>not</b> notified when the process is over.  
	 * 
	 * @see #getSpots()
	 */
	public void execDetection() {
		final Settings<T> settings = model.getSettings();
		final int detectionChannel = settings.detectionChannel;
		final Logger logger = model.getLogger();
		final ImagePlus imp = settings.imp;
		
		if (null == imp) {
			logger.error("No image to operate on.\n");
			return;
		}
		if (null == settings.detector) {
			logger.error("No detector selected.\n");
			return;
		}
		if (null == settings.detectorSettings) {
			logger.error("No detector settings set.\n");
			return;
		}
		if (!settings.detectorSettings.getClass().equals( detectorFactory.getDefaultSettings(settings.detector).getClass())) {
			logger.error(String.format("Detector settings class does not match detector class: %s vs %s.\n", 
					settings.detectorSettings.getClass().getSimpleName(),
					detectorFactory.getDefaultSettings(settings.detector).getClass().getSimpleName()));
			return;
		}
		
		final int numFrames = settings.tend - settings.tstart + 1;
		final SpotCollection spots = new SpotCollection();
		final AtomicInteger spotFound = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);

		final ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(imp);
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img , detectionChannel-1); // channel index is 1-based

		final Thread[] threads;
		if (useMultithreading) {
			threads = SimpleMultiThreading.newThreads();
		} else {
			threads = SimpleMultiThreading.newThreads(1);
		}

		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger(settings.tstart);
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("TrackMate spot detection thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int frame = ai.getAndIncrement(); frame <= settings.tend; frame = ai.getAndIncrement()) {
						
						// Extract frame of interest
						ImgPlus<T> imgT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);
						
						// Segment it
						List<Spot> s = execSingleFrameDetection(imgT, settings);
						
						// Add detection feature other than position
						for (Spot spot : s) {
							spot.putFeature(Spot.POSITION_T, frame * settings.dt);
						}
						spots.put(frame, s);

						spotFound.addAndGet(s.size());
						logger.setProgress(progress.incrementAndGet() / (float)numFrames );

					} // Finished looping over frames
				}
			};
		}

		logger.setStatus("Detection...");
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
	public void execInitialSpotFiltering() {
		Double initialSpotFilterValue = model.getSettings().initialSpotFilterValue;
		FeatureFilter featureFilter = new FeatureFilter(Spot.QUALITY, initialSpotFilterValue, true);
		model.setSpots(model.getSpots().filter(featureFilter), true);
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
	public void execSpotFiltering() {
		model.setFilteredSpots(model.getSpots().filter(model.getSettings().getSpotFilters()), true);
	}

	public void execTrackFiltering() {
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
	}

	
	public String toString() {
		return PLUGIN_NAME_STR + "v" + PLUGIN_NAME_VERSION;
	};
}
