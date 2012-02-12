package fiji.plugin.trackmate;

import fiji.plugin.trackmate.action.CaptureOverlayAction;
import fiji.plugin.trackmate.action.CopyOverlayAction;
import fiji.plugin.trackmate.action.GrabSpotImageAction;
import fiji.plugin.trackmate.action.LinkNew3DViewerAction;
import fiji.plugin.trackmate.action.PlotNSpotsVsTimeAction;
import fiji.plugin.trackmate.action.RadiusToEstimatedAction;
import fiji.plugin.trackmate.action.RecalculateFeatureAction;
import fiji.plugin.trackmate.action.ResetRadiusAction;
import fiji.plugin.trackmate.action.ResetSpotTimeFeatureAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.features.spot.BlobContrastAndSNR;
import fiji.plugin.trackmate.features.spot.BlobDescriptiveStatistics;
import fiji.plugin.trackmate.features.spot.BlobMorphology;
import fiji.plugin.trackmate.features.spot.RadiusEstimator;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackFeatureAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;
import fiji.plugin.trackmate.gui.WizardController;
import fiji.plugin.trackmate.segmentation.DogSegmenter;
import fiji.plugin.trackmate.segmentation.DownSampleLogSegmenter;
import fiji.plugin.trackmate.segmentation.LogSegmenter;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SimpleLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
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
 * <p>The TrackMate_ class runs on the currently active time-lapse image (2D or 3D) 
 * and both identifies and tracks bright spots over time.</p>
 * 
 * <p><b>Required input:</b> A 2D or 3D time-lapse image with bright blobs.</p>
 *
 * @author Nicholas Perry, Jean-Yves Tinevez - Institut Pasteur - July 2010 - 2011 - 2012
 *
 */
public class TrackMate_ implements PlugIn {

	public static final String PLUGIN_NAME_STR = "Track Mate";
	public static final String PLUGIN_NAME_VERSION = ".beta_2012-01-13";
	public static final boolean DEFAULT_USE_MULTITHREADING = true;

	protected TrackMateModel model;
	protected boolean useMultithreading = DEFAULT_USE_MULTITHREADING;

	protected List<SpotFeatureAnalyzer> spotFeatureAnalyzers;
	protected List<TrackFeatureAnalyzer> trackFeatureAnalyzers;
	/** The list of {@link SpotSegmenter} that will be offered to choose amongst to the user. */
	protected List<SpotSegmenter<? extends RealType<?>>> spotSegmenters;
	/** The list of {@link TrackMateModelView} that will be offered to choose amongst to the user. */
	protected List<TrackMateModelView> trackMateModelViews;
	/** The list of {@link TrackMateModelView} that will be offered to choose amongst to the user. */
	protected List<TrackMateAction> trackMateActions;
	/** The list of {@link SpotTracker} that will be offered to choose amongst to the user. */
	protected List<SpotTracker> spotTrackers;



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
		this.spotFeatureAnalyzers 	= createSpotFeatureAnalyzerList();
		this.trackFeatureAnalyzers 	= createTrackFeatureAnalyzerList();
		this.spotSegmenters 		= createSegmenterList();
		this.spotTrackers 			= createSpotTrackerList();
		this.trackMateModelViews 	= createTrackMateModelViewList();
		this.trackMateActions 		= createTrackMateActionList();
		model.getFeatureModel().setSpotFeatureAnalyzers(spotFeatureAnalyzers);
		model.getFeatureModel().setTrackFeatureAnalyzers(trackFeatureAnalyzers);
	}


	/*
	 * RUN METHOD
	 */

	/** 
	 * Launch the GUI.
	 */
	public void run(String arg) {
		model.getSettings().imp = WindowManager.getCurrentImage();
		launchGUI();
	}

	/*
	 * PROTECTED METHODS
	 */
	
	/**
	 * This method exists for the following reason:
	 * <p>
	 * The segmenter receives at each frame a cropped image to operate on, depending
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
		// Get Roi
		final Polygon polygon;
		if (null == settings.imp || null == settings.imp.getRoi()) {
			polygon = null;
		} else {
			polygon = settings.imp.getRoi().getPolygon();
		}		
		
		// Put them back in the right referential 
		final float[] calibration = settings.getCalibration();
		TMUtils.translateSpots(spotsThisFrame, 
				settings.xstart * calibration[0], 
				settings.ystart * calibration[1], 
				settings.zstart * calibration[2]);
		List<Spot> prunedSpots;
		// Prune if outside of ROI
		if (null != polygon) {
			prunedSpots = new ArrayList<Spot>();
			for (Spot spot : spotsThisFrame) {
				if (polygon.contains(spot.getFeature(Spot.POSITION_X)/calibration[0], spot.getFeature(Spot.POSITION_Y)/calibration[1])) 
					prunedSpots.add(spot);
			}
		} else {
			prunedSpots = spotsThisFrame;
		}
		return prunedSpots;
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected List<Spot> execSingleFrameSegmentation(final Image<?  extends RealType<?>> img, Settings settings, int frameIndex) {
		
		final float[] calibration = settings.getCalibration();
		SpotSegmenter segmenter = settings.segmenter.createNewSegmenter();
		segmenter.setTarget(img, calibration, settings.segmenterSettings);

		if (segmenter.checkInput() && segmenter.process()) {
			List<Spot> spotsThisFrame = segmenter.getResult();
			return translateAndPruneSpots(spotsThisFrame, settings);
			
		} else {
			model.getLogger().error(segmenter.getErrorMessage()+'\n');
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
		new WizardController(this);
	}
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the list of available {@link TrackMateModelView} the will be offered to choose from.
	 */
	protected List<TrackMateModelView> createTrackMateModelViewList() {
		List<TrackMateModelView> trackMateModelViews = new ArrayList<TrackMateModelView>(2);
		trackMateModelViews.add(new HyperStackDisplayer());
		trackMateModelViews.add(new SpotDisplayer3D());
		return trackMateModelViews;
	}
	
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the list of {@link SpotSegmenter} that will be offered to choose from. 
	 * Override it to add your own segmenter.
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected List<SpotSegmenter<? extends RealType<?>>> createSegmenterList() {
		List<SpotSegmenter<? extends RealType<?>>> spotSegmenters = new ArrayList<SpotSegmenter<? extends RealType<?>>>(4);
		spotSegmenters.add(new LogSegmenter());
		spotSegmenters.add(new DogSegmenter());
		spotSegmenters.add(new DownSampleLogSegmenter());
		spotSegmenters.add(new ManualSegmenter());
		return spotSegmenters;
	}
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the list of {@link SpotFeatureAnalyzer} that will be used to compute spot features.
	 * Overwrite this method if you want to add your {@link SpotFeatureAnalyzer}.
	 */
	protected List<SpotFeatureAnalyzer> createSpotFeatureAnalyzerList() {
		List<SpotFeatureAnalyzer> analyzers = new ArrayList<SpotFeatureAnalyzer>(5);
		analyzers.add(new BlobDescriptiveStatistics());
		analyzers.add(new BlobContrastAndSNR()); // must be after the statistics one
		analyzers.add(new RadiusEstimator());
		analyzers.add(new BlobMorphology());
//		analyzers.add(new SpotIconGrabber()); // Takes too long. And we need it only at the end, and we can do it with an action.
		return analyzers;
	}
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the list of {@link SpotTracker} that will be used to build tracks.
	 * Overwrite this method if you want to add your {@link SpotTracker}.
	 */
	protected List<SpotTracker> createSpotTrackerList() {
		List<SpotTracker> trackers = new ArrayList<SpotTracker>(5);
		trackers.add(new SimpleFastLAPTracker());
		trackers.add(new FastLAPTracker());
		trackers.add(new SimpleLAPTracker());
		trackers.add(new LAPTracker());
		trackers.add(new NearestNeighborTracker());
		return trackers;
		
	}
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the list of {@link TrackFeatureAnalyzer} that will be used to compute track features.
	 * Overwrite this method if you want to add your {@link TrackFeatureAnalyzer}.
	 */
	protected List<TrackFeatureAnalyzer> createTrackFeatureAnalyzerList() {
		List<TrackFeatureAnalyzer> analyzers = new ArrayList<TrackFeatureAnalyzer>(3);
		analyzers.add(new TrackBranchingAnalyzer());
		analyzers.add(new TrackDurationAnalyzer());
		analyzers.add(new TrackSpeedStatisticsAnalyzer());
		return analyzers;
	}
	
	/**
	 * Hook for subclassers.
	 * <p>
	 * Create the list of {@link TrackMateAction} that will be offered to use.
	 * Overwrite this method if you want to add your {@link TrackMateAction}.
	 */
	protected List<TrackMateAction> createTrackMateActionList() {
		List<TrackMateAction> actions = new ArrayList<TrackMateAction>(9);
		actions.add(new GrabSpotImageAction());
		actions.add(new LinkNew3DViewerAction());
		actions.add(new CopyOverlayAction());
		actions.add(new PlotNSpotsVsTimeAction());
		actions.add(new CaptureOverlayAction());
		actions.add(new ResetSpotTimeFeatureAction());
		actions.add(new RecalculateFeatureAction());
		actions.add(new ResetRadiusAction());
		actions.add(new RadiusToEstimatedAction());
		return actions;
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
	 * Return a list of the {@link SpotFeatureAnalyzer} that are currently registered in this plugin.
	 * <p>
	 * Note: the features that will be actually computed are a subset of this list and can be specified 
	 * to the model, using the {@link TrackMateModel#setSpotFeatureAnalyzers(List)} method. 
	 * @see #createSpotFeatureAnalyzerList()
	 */
	public List<SpotFeatureAnalyzer> getAvailableSpotFeatureAnalyzers() {
		return spotFeatureAnalyzers;
	}
	
	/**
	 * Return a list of the {@link SpotSegmenter} that are currently registered in this plugin.
	 */
	public List<SpotSegmenter<? extends RealType<?>>> getAvailableSpotSegmenters() {
		return spotSegmenters;
	}
	
	/**
	 * Return a list of the {@link SpotTracker} that are currently registered in this plugin.
	 */
	public List<SpotTracker> getAvailableSpotTrackers() {
		return spotTrackers;
	}

	
	/**
	 * Return a list of the {@link TrackMateModelView} that are currently registered in this plugin.
	 */
	public List<TrackMateModelView> getAvailableTrackMateModelViews() {
		return trackMateModelViews;
	}
	
	/**
	 * Return a list of the {@link TrackMateAction} that are currently registered in this plugin.
	 */
	public List<TrackMateAction> getAvailableActions() {
		return trackMateActions;
	}

	
	/*
	 * PROCESSES
	 */

	/**
	 * Calculate all features for all segmented spots.
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
		SpotTracker tracker = model.getSettings().tracker;
		tracker.setModel(model);
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
	public void execSegmentation() {
		final Settings settings = model.getSettings();
		final int segmentationChannel = settings.segmentationChannel;
		final Logger logger = model.getLogger();
		final ImagePlus imp = settings.imp;
		if (null == imp) {
			model.getLogger().error("No image to operate on.\n");
			return;
		}
		final int numFrames = settings.tend - settings.tstart + 1;
		
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

						Image<? extends RealType<?>> img = TMUtils.getSingleFrameAsImage(imp, i, segmentationChannel, settings); // will be cropped according to settings
						List<Spot> s = execSingleFrameSegmentation(img, settings, i);
						
						// Add segmentation feature other than position
						for (Spot spot : spots) {
							spot.putFeature(Spot.POSITION_T, i * settings.dt);
						}
						spots.put(i, s);
						spotFound.addAndGet(s.size());
						
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
		FeatureFilter featureFilter = new FeatureFilter(Spot.QUALITY, initialSpotFilterValue, true);
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
			for(FeatureFilter filter : model.getTrackFilters()) {
				Float tval = filter.value;
				Float val = model.getFeatureModel().getTrackFeature(trackIndex, filter.feature);
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
