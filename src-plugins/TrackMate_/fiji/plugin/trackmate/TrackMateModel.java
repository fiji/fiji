package fiji.plugin.trackmate;

import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotFeatureFacade;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * 
 */
public class TrackMateModel {		

	private static final boolean DEBUG = true;

	/** Contain the segmentation result, un-filtered.*/
	protected SpotCollection spots;
	/** Contain the spots retained for tracking, after filtering by features. */
	protected SpotCollection filteredSpots;
	/** The feature filter list that is used to generate {@link #filteredSpots} from {@link #spots}. */
	protected List<SpotFilter> spotFilters = new ArrayList<SpotFilter>();
	/** The initial quality filter value that is used to clip spots of low quality from {@link #spots}. */
	protected Float initialSpotFilterValue;

	/** The tracks as a track collection. */
	protected TrackCollection tracks;

	/** The spot current selection. */
	protected Set<Spot> spotSelection = new HashSet<Spot>();
	/** The edge current selection. */
	protected Set<DefaultWeightedEdge> edgeSelection = new HashSet<DefaultWeightedEdge>();

	/** The logger to append processes messages */
	protected Logger logger = Logger.DEFAULT_LOGGER;

	/** The settings that determine processes actions */
	protected Settings settings;


	/** The list of listeners listening to model content change, that is, changes in 
	 * {@link #spots}, {@link #filteredSpots} and {@link #trackGraph}. */
	protected List<TrackMateModelChangeListener> modelChangeListeners = new ArrayList<TrackMateModelChangeListener>();
	/** The list of listener listening to change in selection.  */
	protected List<TrackMateSelectionChangeListener> selectionChangeListeners = new ArrayList<TrackMateSelectionChangeListener>();

	/**
	 * A private listener that listens to modifications made in the {@link TrackCollection} {@link #tracks},
	 * and forward them to the listener of this model.
	 */
	private TrackListener trackListener;

	/*
	 * DEAL WITH MODEL CHANGE LISTENER
	 */

	public void addTrackMateModelChangeListener(TrackMateModelChangeListener listener) {
		modelChangeListeners.add(listener);
	}

	public boolean removeTrackMateModelChangeListener(TrackMateModelChangeListener listener) {
		return modelChangeListeners.remove(listener);
	} 

	public List<TrackMateModelChangeListener> getTrackMateModelChangeListener(TrackMateModelChangeListener listener) {
		return modelChangeListeners;
	}

	/*
	 * DEAL WITH SELECTION CHANGE LISTENER
	 */

	public void addTrackMateSelectionChangeListener(TrackMateSelectionChangeListener listener) {
		selectionChangeListeners.add(listener);
	}

	public boolean removeTrackMateSelectionChangeListener(TrackMateSelectionChangeListener listener) {
		return selectionChangeListeners.remove(listener);
	}

	public List<TrackMateSelectionChangeListener> getTrackMateSelectionChangeListener() {
		return selectionChangeListeners;
	}

	/*
	 * PROCESSES
	 */

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
		SpotTracker tracker = settings.getSpotTracker(filteredSpots);
		tracker.setLogger(logger);
		if (tracker.checkInput() && tracker.process())
			setTracks(tracker.getTracks(), true);
		else
			logger.error("Problem occured in tracking:\n"+tracker.getErrorMessage()+'\n');
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
		final ImagePlus imp = settings.imp;
		if (null == imp) {
			logger.error("No image to operate on.\n");
			return;
		}

		Roi roi = imp.getRoi();
		Polygon polygon = null;
		if (roi != null)
			polygon = roi.getPolygon();

		int numFrames = settings.tend - settings.tstart + 1;

		/* 0 -- Initialize local variables */
		final float[] calibration = new float[] {(float) imp.getCalibration().pixelWidth, (float) imp.getCalibration().pixelHeight, (float) imp.getCalibration().pixelDepth};

		@SuppressWarnings("rawtypes")
		SpotSegmenter<? extends RealType> segmenter = settings.getSpotSegmenter();
		segmenter.setCalibration(calibration);

		spots = new SpotCollection();

		// For each frame...
		int spotFound = 0;
		for (int i = settings.tstart-1; i < settings.tend; i++) {

			/* 1 - Prepare stack for use with Imglib. */
			@SuppressWarnings("rawtypes")
			Image img = TMUtils.getSingleFrameAsImage(imp, i, settings); // will be cropped according to settings

			/* 2 Segment it */
			logger.setStatus("Frame "+(i+1)+": Segmenting...");
			logger.setProgress((i-settings.tstart) / (float)numFrames );
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
					spot.putFeature(SpotFeature.RADIUS, settings.segmenterSettings.expectedRadius);
				}
				spots.put(i, prunedSpots);
				spotFound += prunedSpots.size();
			} else {
				logger.error(segmenter.getErrorMessage()+'\n');
				return;
			}

		} // Finished looping over frames
		logger.log("Found "+spotFound+" spots.\n");
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
		SpotFilter featureFilter = new SpotFilter(SpotFeature.QUALITY, initialSpotFilterValue, true);
		setSpots(spots.filter(featureFilter), true);
	}

	/**
	 * Calculate given features for the given spots, according to the {@link Settings} set in this model.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link SpotFeatureFacade} class
	 * for details. Since a {@link SpotFeatureAnalyzer} can compute more than a {@link SpotFeature} at once, spots might
	 * received more data than required.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void computeSpotFeatures(final SpotCollection toCompute, final List<SpotFeature> features) {

		int numFrames = settings.tend - settings.tstart + 1;
		List<Spot> spotsThisFrame;
		SpotFeatureFacade<?> featureCalculator;
		final float[] calibration = new float[] { settings.dx, settings.dy, settings.dz };

		for (int i = settings.tstart-1; i < settings.tend; i++) {
			logger.setProgress((2*(i-settings.tstart)) / (2f * numFrames + 1));
			logger.setStatus("Frame "+(i+1)+": Calculating features...");

			/* 1 - Prepare stack for use with Imglib.
			 * This time, since the spot coordinates are with respect to the top-left corner of the image, 
			 * we must not generate a cropped version of the image, but a full snapshot. 	 */
			Settings uncroppedSettings = new Settings();
			uncroppedSettings.xstart = 1;
			uncroppedSettings.xend   = settings.imp.getWidth();
			uncroppedSettings.ystart = 1;
			uncroppedSettings.yend   = settings.imp.getHeight();
			uncroppedSettings.zstart = 1;
			uncroppedSettings.zend   = settings.imp.getNSlices();
			Image<? extends RealType> img = TMUtils.getSingleFrameAsImage(settings.imp, i, uncroppedSettings); 

			/* 1.5 Determine what analyzers are needed */
			featureCalculator = new SpotFeatureFacade(img, calibration);
			HashSet<SpotFeatureAnalyzer> analyzers = new HashSet<SpotFeatureAnalyzer>();
			for (SpotFeature feature : features)
				analyzers.add(featureCalculator.getAnalyzerForFeature(feature));

			/* 2 - Compute features. */
			spotsThisFrame = toCompute.get(i);
			for (SpotFeatureAnalyzer analyzer : analyzers)
				analyzer.process(spotsThisFrame);

		} // Finished looping over frames
		logger.setProgress(1);
		logger.setStatus("");
		return;
	}

	/**
	 * Calculate given features for the all segmented spots of this model, 
	 * according to the {@link Settings} set in this model.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link SpotFeatureFacade} class
	 * for details. Since a {@link SpotFeatureAnalyzer} can compute more than a {@link SpotFeature} at once, spots might
	 * received more data than required.
	 */
	public void computeSpotFeatures(final List<SpotFeature> features) {
		computeSpotFeatures(spots, features);
	}

	/**
	 * Calculate given features for the all filtered spots of this model, 
	 * according to the {@link Settings} set in this model.
	 */
	public void computeSpotFeatures(final SpotFeature feature) {
		ArrayList<SpotFeature> features = new ArrayList<SpotFeature>(1);
		features.add(feature);
		computeSpotFeatures(features);
	}

	/**
	 * Calculate all features for all segmented spots.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link SpotFeatureFacade} class
	 * for details. 
	 */
	public void computeSpotFeatures() {
		computeSpotFeatures(spots);
	}

	/**
	 * Calculate all features for the given spot collection.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link SpotFeatureFacade} class
	 * for details. 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void computeSpotFeatures(final SpotCollection toCompute) {
		int numFrames = toCompute.keySet().size();
		List<Spot> spotsThisFrame;
		SpotFeatureFacade<?> featureCalculator;
		final float[] calibration = settings.getCalibration();

		for (int i : toCompute.keySet()) {
			logger.setProgress((2*(i-settings.tstart)) / (2f * numFrames + 1));
			logger.setStatus("Frame "+(i+1)+": Calculating features...");

			/* 1 - Prepare stack for use with Imglib.
			 * This time, since the spot coordinates are with respect to the top-left corner of the image, 
			 * we must not generate a cropped version of the image, but a full snapshot. 	 */
			Settings uncroppedSettings = new Settings();
			uncroppedSettings.xstart = 1;
			uncroppedSettings.xend   = settings.imp.getWidth();
			uncroppedSettings.ystart = 1;
			uncroppedSettings.yend   = settings.imp.getHeight();
			uncroppedSettings.zstart = 1;
			uncroppedSettings.zend   = settings.imp.getNSlices();
			Image<? extends RealType> img = TMUtils.getSingleFrameAsImage(settings.imp, i, uncroppedSettings); 

			/* 1.5 Determine what analyzers are needed */
			featureCalculator = new SpotFeatureFacade(img, calibration);
			spotsThisFrame = toCompute.get(i);
			featureCalculator.processAllFeatures(spotsThisFrame);

		} // Finished looping over frames
		logger.setProgress(1);
		logger.setStatus("");
		return;
	}


	/**
	 * Execute the feature filtering part.
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
		setFilteredSpots(spots.filter(spotFilters), true);
	}

	public void computeTrackFeatures(final TrackCollection trackCollection) {
		//TODO
	}

	public void computeTrackFeatures() {
		computeTrackFeatures(tracks);
	}



	/*
	 * GETTERS / SETTERS
	 */

	/**
	 * Return the tracks of this model. 
	 * This collection contains all tracks, un-filtered.
	 */
	public TrackCollection getTracks() {
		return tracks;
	}

	/**
	 * Return the spots generated by the segmentation part of this plugin. The collection are un-filtered and contain
	 * all spots. They are returned as a {@link SpotCollection}.
	 */
	public SpotCollection getSpots() {
		return spots;
	}

	/**
	 * Return the spots filtered by feature filters. 
	 * These spots will be used for subsequent tracking and display.
	 * <p>
	 * Feature thresholds can be set / added / cleared by 
	 * {@link #setSpotFilters(List)}, {@link #addSpotFilter(SpotFilter)} and {@link #clearSpotFilters()}.
	 */
	public SpotCollection getFilteredSpots() {
		return filteredSpots;
	}

	/**
	 * Overwrite the raw {@link #spots} field, resulting normally from the {@link #execSegmentation()} process.
	 * @param spots
	 * @param doNotify  if true, will file a {@link TrackMateModelChangeEvent#SPOTS_COMPUTED} event.
	 */
	public void setSpots(SpotCollection spots, boolean doNotify) {
		this.spots = spots;
		if (doNotify) {
			final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_COMPUTED);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Overwrite the {@link #filteredSpots} field, resulting normally from the {@link #execSpotFiltering()} process.
	 * @param doNotify  if true, will file a {@link TrackMateModelChangeEvent#SPOTS_FILTERED} event.
	 */
	public void setFilteredSpots(final SpotCollection filteredSpots, boolean doNotify) {
		this.filteredSpots = filteredSpots;
		if (doNotify) {
			final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_FILTERED);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Overwrite the {@link #tracks} field, resulting from the tracking step.
	 * @param doNotify  if true, will file a {@link TrackMateModelChangeEvent#TRACKS_COMPUTED} event.
	 */
	public void setTracks(final TrackCollection tracks, final boolean doNotify) {
		if (this.tracks != null) 
			this.tracks.removeTrackMateModelChangeListener(trackListener);
		this.tracks = tracks;
		this.trackListener = new TrackListener();
		this.tracks.addTrackMateModelChangeListener(trackListener);
		if (doNotify) {
			final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.TRACKS_COMPUTED);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}



	/*
	 * FEATURE FILTERS
	 */

	/**
	 * Add a filter to the list of spot filters to deal with when executing {@link #execFiltering()}.
	 */
	public void addSpotFilter(final SpotFilter filter) { spotFilters.add(filter); }


	public void removeSpotFilter(final SpotFilter filter) { spotFilters.remove(filter); }

	/**
	 * Remove all spot filters stored in this model.
	 */
	public void clearSpotFilters() { spotFilters.clear(); }

	public List<SpotFilter> getSpotFilters() { return spotFilters; }

	public void setSpotFilters(List<SpotFilter> spotFilters) { this.spotFilters = spotFilters; }

	/**
	 * Return the initial filter value on {@link SpotFeature#QUALITY} stored in this model.
	 */
	public Float getInitialSpotFilterValue() {
		return initialSpotFilterValue;
	}

	/**
	 * Set the initial filter value on {@link SpotFeature#QUALITY} stored in this model.
	 */
	public void setInitialSpotFilterValue(Float initialSpotFilterValue) {
		this.initialSpotFilterValue = initialSpotFilterValue;
	}

	/*
	 * LOGGER
	 */

	/**
	 * Set the logger that will receive the messages from the processes occurring within this plugin.
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Return the logger currently set for this model.
	 */
	public Logger getLogger() {
		return logger;
	}



	/*
	 * SETTINGS
	 */

	/**
	 * Return the {@link Settings} object that determines the behavior of this plugin.
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Set the {@link Settings} object that determines the behavior of this model's processes.
	 * @see #execSegmentation()
	 * @see #execTracking()
	 */

	public void setSettings(Settings settings) {
		this.settings = settings;
	}


	/*
	 * FEATURES
	 */

	/**
	 * Return a map of {@link SpotFeature} values for the spot collection held by this instance.
	 * Each feature maps a double array, with 1 element per {@link Spot}, all pooled
	 * together.
	 */
	public EnumMap<SpotFeature, double[]> getFeatureValues() {
		return TMUtils.getFeatureValues(spots.values());
	}

	/*
	 * SELECTION METHODSs
	 */

	public void clearSelection() {
		if (DEBUG)
			System.out.println("[TrackMateModel] Clearing selection");
		// Prepare event
		Map<Spot, Boolean> spotMap = new HashMap<Spot, Boolean>(spotSelection.size());
		for(Spot spot : spotSelection) 
			spotMap.put(spot, false);
		Map<DefaultWeightedEdge, Boolean> edgeMap = new HashMap<DefaultWeightedEdge, Boolean>(edgeSelection.size());
		for(DefaultWeightedEdge edge : edgeSelection) 
			edgeMap.put(edge, false);
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, spotMap, edgeMap);
		// Clear fields
		clearSpotSelection();
		clearEdgeSelection();
		// Fire event
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public void clearSpotSelection() {
		if (DEBUG)
			System.out.println("[TrackMateModel] Clearing spot selection");
		// Prepare event
		Map<Spot, Boolean> spotMap = new HashMap<Spot, Boolean>(spotSelection.size());
		for(Spot spot : spotSelection) 
			spotMap.put(spot, false);
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, spotMap, null);
		// Clear field
		spotSelection.clear();
		// Fire event
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public void clearEdgeSelection() {
		if (DEBUG)
			System.out.println("[TrackMateModel] Clearing edge selection");
		// Prepare event
		Map<DefaultWeightedEdge, Boolean> edgeMap = new HashMap<DefaultWeightedEdge, Boolean>(edgeSelection.size());
		for(DefaultWeightedEdge edge : edgeSelection) 
			edgeMap.put(edge, false);
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, null, edgeMap);
		// Clear field
		edgeSelection.clear();
		// Fire event
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public void addSpotToSelection(final Spot spot) {
		if (!spotSelection.add(spot))
			return; // Do nothing if already present in selection
		if (DEBUG)
			System.out.println("[TrackMateModel] Adding spot "+spot+" to selection");
		Map<Spot, Boolean> spotMap = new HashMap<Spot, Boolean>(1); 
		spotMap.put(spot, true);
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, spotMap, null);
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public void removeSpotFromSelection(final Spot spot) {
		if (!spotSelection.remove(spot))
			return; // Do nothing was not already present in selection
		if (DEBUG)
			System.out.println("[TrackMateModel] Removing spot "+spot+" from selection");
		Map<Spot, Boolean> spotMap = new HashMap<Spot, Boolean>(1); 
		spotMap.put(spot, false);
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, spotMap, null);
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public void addSpotToSelection(final Collection<Spot> spots) {
		Map<Spot, Boolean> spotMap = new HashMap<Spot, Boolean>(spots.size()); 
		for (Spot spot : spots) {
			if (spotSelection.add(spot)) {
				spotMap.put(spot, true);
				if (DEBUG)
					System.out.println("[TrackMateModel] Adding spot "+spot+" to selection");
			}
		}
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, spotMap, null);
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public void removeSpotFromSelection(final Collection<Spot> spots) {
		Map<Spot, Boolean> spotMap = new HashMap<Spot, Boolean>(spots.size()); 
		for (Spot spot : spots) {
			if (spotSelection.remove(spot)) {
				spotMap.put(spot, false);
				if (DEBUG)
					System.out.println("[TrackMateModel] Removing spot "+spot+" from selection");
			}
		}
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, spotMap, null);
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public void addEdgeToSelection(final DefaultWeightedEdge edge) {
		if (!edgeSelection.add(edge))
			return; // Do nothing if already present in selection
		if (DEBUG)
			System.out.println("[TrackMateModel] Adding edge "+edge+" to selection");
		Map<DefaultWeightedEdge, Boolean> edgeMap = new HashMap<DefaultWeightedEdge, Boolean>(1); 
		edgeMap.put(edge, true);
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, null, edgeMap);
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);

	}

	public void removeEdgeFromSelection(final DefaultWeightedEdge edge) {
		if (!edgeSelection.remove(edge))
			return; // Do nothing if already present in selection
		if (DEBUG)
			System.out.println("[TrackMateModel] Removing edge "+edge+" from selection");
		Map<DefaultWeightedEdge, Boolean> edgeMap = new HashMap<DefaultWeightedEdge, Boolean>(1); 
		edgeMap.put(edge, false);
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, null, edgeMap);
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);

	}

	public void addEdgeToSelection(final Collection<DefaultWeightedEdge> edges) {
		Map<DefaultWeightedEdge, Boolean> edgeMap = new HashMap<DefaultWeightedEdge, Boolean>(edges.size());
		for (DefaultWeightedEdge edge : edges) {
			if (edgeSelection.add(edge)) {
				edgeMap.put(edge, true);
				if (DEBUG)
					System.out.println("[TrackMateModel] Adding edge "+edge+" to selection");
			}
		}
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, null, edgeMap);
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public void removeEdgeFromSelection(final Collection<DefaultWeightedEdge> edges) {
		Map<DefaultWeightedEdge, Boolean> edgeMap = new HashMap<DefaultWeightedEdge, Boolean>(edges.size());
		for (DefaultWeightedEdge edge : edges) {
			if (edgeSelection.remove(edge)) {
				edgeMap.put(edge, false);
				if (DEBUG)
					System.out.println("[TrackMateModel] Removing edge "+edge+" from selection");
			}
		}
		TrackMateSelectionChangeEvent event = new TrackMateSelectionChangeEvent(this, null, edgeMap);
		for (TrackMateSelectionChangeListener listener : selectionChangeListeners)
			listener.selectionChanged(event);
	}

	public Set<Spot> getSpotSelection() {
		return spotSelection;
	}

	public Set<DefaultWeightedEdge> getEdgeSelection() {
		return edgeSelection;
	}

	/*
	 * SPOT UPDATING METHODS
	 */

	/**
	 * Move some spots from a frame to another, then update their features.
	 * @param spotsToMove  the list of spots to move
	 * @param fromFrame  the frame each spot originated from
	 * @param toFrame  the destination frame of each spot
	 * @param doNotify  if false, {@link TrackMateModelChangeListener}s will not be notified of this change
	 */
	public void moveSpotsFrom(List<Spot> spotsToMove, List<Integer> fromFrame, List<Integer> toFrame, boolean doNotify) {
		if (null != spots) 
			for (int i = 0; i < spotsToMove.size(); i++) {
				spots.add(spotsToMove.get(i), toFrame.get(i));
				spots.remove(spotsToMove.get(i), fromFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModel] Moving "+spotsToMove.get(i)+" from frame "+fromFrame.get(i)+" to frame "+toFrame.get(i));
			}
		if (null != filteredSpots) 
			for (int i = 0; i < spotsToMove.size(); i++) {
				filteredSpots.add(spotsToMove.get(i), toFrame.get(i));
				filteredSpots.remove(spotsToMove.get(i), fromFrame.get(i));
			}
		updateFeatures(spotsToMove, false);
		tracks.computeFeatures();

		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(spotsToMove.size());
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			for (int i = 0; i < spotFlags.size(); i++)
				spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_FRAME_CHANGED);

			event.setSpots(spotsToMove);
			event.setSpotFlag(spotFlags);
			event.setFromFrame(fromFrame);
			event.setToFrame(toFrame);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Move a single spot from a frame to another, then update its features.
	 * @param spotToMove  the spot to move
	 * @param fromFrame  the frame the spot originated from
	 * @param toFrame  the destination frame
	 * @param doNotify  if false, {@link TrackMateModelChangeListener}s will not be notified of this change
	 */
	public void moveSpotsFrom(Spot spotToMove, Integer fromFrame, Integer toFrame, boolean doNotify) {
		if (null != spots) {
			spots.add(spotToMove, toFrame);
			spots.remove(spotToMove, fromFrame);
			if (DEBUG)
				System.out.println("[TrackMateModel] Moving "+spotToMove+" from frame "+fromFrame+" to frame "+toFrame);
		}
		if (null != filteredSpots) {
			filteredSpots.add(spotToMove, toFrame);
			filteredSpots.remove(spotToMove, fromFrame);
		}
		updateFeatures(spotToMove, false);
		tracks.computeFeatures();

		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(1);
			List<Integer> toFrames = new ArrayList<Integer>(1);
			List<Integer> fromFrames = new ArrayList<Integer>(1);
			List<Spot> spotsToMove = new ArrayList<Spot>(1);
			spotsToMove .add(spotToMove);
			spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_FRAME_CHANGED);
			toFrames.add(toFrame);
			fromFrames.add(fromFrame);

			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			event.setSpots(spotsToMove);
			event.setSpotFlag(spotFlags);
			event.setFromFrame(fromFrames);
			event.setToFrame(toFrames);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}


	/**
	 * Add spots to the collections managed by this model, then update heir features.
	 * @param doNotify  if false, listeners of this class will not be notified of this change. 
	 */
	public void addSpotTo(List<Spot> spotsToAdd, List<Integer> toFrame, boolean doNotify) {
		if (null != spots) 
			for (int i = 0; i < spotsToAdd.size(); i++) {
				spots.add(spotsToAdd.get(i), toFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModel] Adding spot "+spotsToAdd.get(i)+" to frame "+ toFrame.get(i));
			}

		if (null != filteredSpots) 
			for (int i = 0; i < spotsToAdd.size(); i++) 
				filteredSpots.add(spotsToAdd.get(i), toFrame.get(i));

		if (null != tracks) {
			tracks.beginUpdate();
			try {
				for (Spot spot : spotsToAdd) 
					tracks.addVertex(spot);
			} finally {
				tracks.endUpdate();
			}
		}
		
		updateFeatures(spotsToAdd, false);

		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(spotsToAdd.size());
			for (int i = 0; i < spotFlags.size(); i++)
				spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_ADDED);

			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			event.setSpots(spotsToAdd);
			event.setSpotFlag(spotFlags);
			event.setFromFrame(null);
			event.setToFrame(toFrame);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Add a single spot to the collections managed by this model, then update its features.
	 * @param doNotify  if false, listeners of this class will not be notified of this change. 
	 */
	public void addSpotTo(Spot spotToAdd, Integer toFrame, boolean doNotify) {
		if (null != spots)  {
			spots.add(spotToAdd, toFrame);
			if (DEBUG)
				System.out.println("[TrackMateModel] Adding spot "+spotToAdd+" to frame "+ toFrame);
		}
		
		if (null != filteredSpots) 
			filteredSpots.add(spotToAdd, toFrame);

		if (null != tracks) {
			tracks.beginUpdate();
			try {
				tracks.addVertex(spotToAdd);
			} finally {
				tracks.endUpdate();
			}
		}
		
		updateFeatures(spotToAdd, false);

		if (doNotify) {
			List<Spot> spotsToAdd = new ArrayList<Spot>(1);
			List<Integer> spotFlags = new ArrayList<Integer>(1);
			List<Integer> toFrames = new ArrayList<Integer>(1);
			spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_ADDED);
			spotsToAdd.add(spotToAdd);
			toFrames.add(toFrame);

			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			event.setSpots(spotsToAdd);
			event.setSpotFlag(spotFlags);
			event.setFromFrame(null);
			event.setToFrame(toFrames);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}


	/**
	 * Remove given spots from the collections managed by this model.
	 * @param doNotify  if false, listeners of this class will not be notified of this change. 
	 */
	public void removeSpotFrom(List<Spot> spotsToRemove, List<Integer> fromFrame, boolean doNotify) {
		if (null != spots) 
			for (int i = 0; i < spotsToRemove.size(); i++) {
				spots.remove(spotsToRemove.get(i), fromFrame.get(i));
				if (DEBUG)
					System.out.println("[TrackMateModel] Removing spot "+spotsToRemove.get(i)+" from frame "+ fromFrame.get(i));
			}
		
		if (null != filteredSpots) 
			for (int i = 0; i < spotsToRemove.size(); i++) 
				filteredSpots.remove(spotsToRemove.get(i), fromFrame.get(i));
		
		if (null != tracks) {
			tracks.beginUpdate();
			try {
				for (Spot spot : spotsToRemove) 
					tracks.removeVertex(spot);
			} finally {
				tracks.endUpdate();
			}
		}

		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(spotsToRemove.size());
			for (int i = 0; i < spotsToRemove.size(); i++)
				spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_REMOVED);

			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			event.setSpots(spotsToRemove);
			event.setSpotFlag(spotFlags);
			event.setFromFrame(fromFrame);
			event.setToFrame(null);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Remove a single spot from the collections managed by this model.
	 * @param doNotify  if false, listeners of this class will not be notified of this change. 
	 */
	public void removeSpotFrom(Spot spotToRemove, Integer fromFrame, boolean doNotify) {
		if (null != spots) {
			spots.remove(spotToRemove, fromFrame);
			if (DEBUG)
				System.out.println("[TrackMateModel] Removing spot "+spotToRemove+" from frame "+ fromFrame);
		}
		
		if (null != filteredSpots) 
			filteredSpots.remove(spotToRemove, fromFrame);
		
		if (null != tracks) {
			tracks.beginUpdate();
			try {
				tracks.removeVertex(spotToRemove);
			} finally {
				tracks.endUpdate();
			}
		}

		if (doNotify) {
			List<Spot> spotsToRemove = new ArrayList<Spot>(1);
			List<Integer> spotFlags = new ArrayList<Integer>(1);
			List<Integer> fromFrames = new ArrayList<Integer>(1);
			spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_REMOVED);
			spotsToRemove.add(spotToRemove);
			fromFrames.add(fromFrame);

			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			event.setSpots(spotsToRemove);
			event.setSpotFlag(spotFlags);
			event.setFromFrame(fromFrames);
			event.setToFrame(null);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	public void updateFeatures(List<Spot> spotsToUpdate, boolean doNotify) {
		if (DEBUG)
			System.out.println("[TrackMateModel] Updating the features of spot "+spotsToUpdate.size());
		if (null == spots)
			return;

		// Find common frames
		SpotCollection toCompute = filteredSpots.subset(spotsToUpdate);
		computeSpotFeatures(toCompute);


		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(spotsToUpdate.size());
			for (int i = 0; i < spotFlags.size(); i++)
				spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_MODIFIED);

			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			event.setSpots(spotsToUpdate);
			event.setSpotFlag(spotFlags);
			event.setFromFrame(null);
			event.setToFrame(null);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	public void updateFeatures(Spot spotToUpdate, boolean doNotify) {
		if (DEBUG)
			System.out.println("[TrackMateModel] Updating the features of spot "+spotToUpdate);
		if (null == spots)
			return;

		// Find frame
		SpotCollection toCompute = new SpotCollection();
		int frame = spots.getFrame(spotToUpdate);
		toCompute.add(spotToUpdate, frame);

		// Calculate features
		computeSpotFeatures(toCompute);

		if (doNotify) {
			List<Integer> spotFlags = new ArrayList<Integer>(1);
			List<Spot> spotsToUpdate = new ArrayList<Spot>(1);
			spotsToUpdate.add(spotToUpdate);
			spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_MODIFIED);

			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			event.setSpots(spotsToUpdate);
			event.setSpotFlag(spotFlags);
			event.setFromFrame(null);
			event.setToFrame(null);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	
	/*
	 * INNER CLASS
	 */
	
	private class TrackListener implements TrackMateModelChangeListener {

		/**
		 * Simply forward events coming from the {@link TrackCollection} to the 
		 * listeners of this {@link TrackMateModel}.
		 */
		@Override
		public void modelChanged(final TrackMateModelChangeEvent event) {
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
		
	}


}
