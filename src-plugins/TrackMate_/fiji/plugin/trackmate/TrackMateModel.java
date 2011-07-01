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

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotFeatureFacade;
import fiji.plugin.trackmate.features.track.TrackFeatureFacade;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * 
 */
public class TrackMateModel {		

	/*
	 * CONSTANTS
	 */

	private static final boolean DEBUG = false;
	private static final boolean DEBUG_SELECTION = false;

	/*
	 * FIELDS
	 */

	// SPOTS

	/** Contain the segmentation result, un-filtered.*/
	protected SpotCollection spots;
	/** Contain the spots retained for tracking, after filtering by features. */
	protected SpotCollection filteredSpots;
	/** The feature filter list that is used to generate {@link #filteredSpots} from {@link #spots}. */
	protected List<FeatureFilter<SpotFeature>> spotFilters = new ArrayList<FeatureFilter<SpotFeature>>();
	/** The initial quality filter value that is used to clip spots of low quality from {@link #spots}. */
	protected Float initialSpotFilterValue;

	// TRACKS

	/**
	 * The mother graph, from which all subsequent fields are calculated. 
	 * This graph is not made accessible to the outside world. Editing it
	 * must be trough the 
	 */
	protected SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
	/** The edges contained in the list of tracks. */
	protected List<Set<DefaultWeightedEdge>> trackEdges;
	/** The spots contained in the list of spots. */
	protected List<Set<Spot>> trackSpots;
	/** The feature facade that will be used to compute track features. */
	private TrackFeatureFacade trackFeatureFacade = new TrackFeatureFacade();
	/**
	 * Feature storage. We use a List of Map as a 2D Map. The list maps each track to its feature map.
	 * We use the same index that for {@link #trackEdges} and {@link #trackSpots}.
	 * The feature map maps each {@link TrackFeature} to its float value for the selected track. 
	 */
	protected List<EnumMap<TrackFeature, Float>> trackFeatures;
	/** The track filter list that is used to prune track and spots. */
	protected List<FeatureFilter<TrackFeature>> trackFilters = new ArrayList<FeatureFilter<TrackFeature>>();


	// TRANSACTION MODEL

	/**
	 * Counter for the depth of nested transactions. Each call to beginUpdate
	 * increments this counter and each call to endUpdate decrements it. When
	 * the counter reaches 0, the transaction is closed and the respective
	 * events are fired. Initial value is 0.
	 */
	private int updateLevel = 0;

	private List<Spot> spotsAdded = new ArrayList<Spot>();
	private List<Spot> spotsRemoved = new ArrayList<Spot>();
	private List<Spot> spotsMoved = new ArrayList<Spot>();
	private List<Spot> spotsUpdated = new ArrayList<Spot>();
	private List<DefaultWeightedEdge> edgesAdded = new ArrayList<DefaultWeightedEdge>();
	private List<DefaultWeightedEdge> edgesRemoved = new ArrayList<DefaultWeightedEdge>();

	// SELECTION

	/** The spot current selection. */
	protected Set<Spot> spotSelection = new HashSet<Spot>();
	/** The edge current selection. */
	protected Set<DefaultWeightedEdge> edgeSelection = new HashSet<DefaultWeightedEdge>();

	// OTHERS

	/** The logger to append processes messages */
	protected Logger logger = Logger.DEFAULT_LOGGER;

	/** The settings that determine processes actions */
	protected Settings settings;

	// LISTENERS


	/** The list of listeners listening to model content change, that is, changes in 
	 * {@link #spots}, {@link #filteredSpots} and {@link #trackGraph}. */
	protected List<TrackMateModelChangeListener> modelChangeListeners = new ArrayList<TrackMateModelChangeListener>();
	/** The list of listener listening to change in selection.  */
	protected List<TrackMateSelectionChangeListener> selectionChangeListeners = new ArrayList<TrackMateSelectionChangeListener>();



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
		SpotTracker tracker = settings.getSpotTracker(this);
		tracker.setLogger(logger);
		if (tracker.checkInput() && tracker.process()) {
			final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.TRACKS_COMPUTED);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		} else
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
		FeatureFilter<SpotFeature> featureFilter = new FeatureFilter<SpotFeature>(SpotFeature.QUALITY, initialSpotFilterValue, true);
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
		setFilteredSpots(spots.filter(spotFilters), true);
	}

	public void execTrackFiltering() {
		// Get the indices of the tracks to remove
		Float tval, val;
		Set<Integer> trackToRemove = new HashSet<Integer>();
		for (FeatureFilter<TrackFeature> filter : trackFilters) {

			tval = filter.value;
			if (null == tval)
				continue;
			trackToRemove.clear();

			if (filter.isAbove) {
				for (int i=0; i<getNTracks(); i++) {
					val = trackFeatures.get(i).get(filter.feature);
					if (null == val)
						continue;
					if ( val < tval)
						trackToRemove.add(i);
				}

			} else {
				for (int i=0; i<getNTracks(); i++) {
					val = trackFeatures.get(i).get(filter.feature);
					if (null == val)
						continue;
					if ( val > tval)
						trackToRemove.add(i);
				}
			}
		}

		// Remove these tracks from model, that is: 
		//  - remove all their edges from the graph;
		//  - remove all their spots from the graph BUT NOT from the filtered spots: they are still 
		// available for subsequent new tracking process.
		for (int i : trackToRemove) {
			graph.removeAllEdges(trackEdges.get(i));
			graph.removeAllVertices(trackSpots.get(i));
		}
	}


	/*
	 * DEAL WITH TRACK GRAPH 
	 */

	// Questing graph 

	public int getNTracks() {
		if (trackSpots == null)
			return 0;
		else
			return trackSpots.size();
	}

	public Spot getEdgeSource(final DefaultWeightedEdge edge) {
		return graph.getEdgeSource(edge);
	}

	public Spot getEdgeTarget(final DefaultWeightedEdge edge) {
		return graph.getEdgeTarget(edge);
	}

	public double getEdgeWeight(final DefaultWeightedEdge edge) {
		return graph.getEdgeWeight(edge);
	}

	public boolean containsEdge(final Spot source, final Spot target) {
		return graph.containsEdge(source, target);
	}

	public Set<DefaultWeightedEdge> edgesOf(final Spot spot) {
		return graph.edgesOf(spot); 
	}

	public Set<DefaultWeightedEdge> edgeSet() {
		return graph.edgeSet();
	}

	public DepthFirstIterator<Spot, DefaultWeightedEdge> getDepthFirstIterator(Spot start) {
		return new DepthFirstIterator<Spot, DefaultWeightedEdge>(graph, start);
	}

	public String trackToString(int i) {
		String str = "Track "+i+": ";
		for (TrackFeature feature : TrackFeature.values())
			str += feature.shortName() + " = " + trackFeatures.get(i).get(feature) +", ";			
		return str;
	}


	// Track features

	public void putTrackFeature(final int trackIndex, final TrackFeature feature, final Float value) {
		trackFeatures.get(trackIndex).put(feature, value);
	}

	/*
	 * GRAPH MODIFICATION
	 */

	public void beginUpdate()	{
		updateLevel++;
		if (DEBUG)
			System.out.println("[TrackMateModel] #beginUpdate: increasing update level to "+updateLevel+".");
	}

	public void endUpdate()	{
		updateLevel--;
		if (DEBUG)
			System.out.println("[TrackMateModel] #endUpdate: decreasing update level to "+updateLevel+".");
		if (updateLevel == 0) {
			if (DEBUG)
				System.out.println("[TrackMateModel] #endUpdate: update level is 0, calling flushUpdate().");
			flushUpdate();
		}
	}


	/*
	 * GETTERS / SETTERS
	 */

	public Set<Spot> getTrackSpots(int index) {
		return trackSpots.get(index);
	}

	public Set<DefaultWeightedEdge> getTrackEdges(int index) {
		return trackEdges.get(index);
	}

	public List<Set<Spot>> getTrackSpots() {
		return trackSpots;
	}

	public List<Set<DefaultWeightedEdge>> getTrackEdges() {
		return trackEdges;
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


	public void clearTracks() {
		this.graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		for(Spot spot : filteredSpots.getAllSpots())
			graph.addVertex(spot);
	}

	/*
	 * FEATURE FILTERS
	 */

	/** Add a filter to the list of spot filters to deal with when executing {@link #execFiltering()}. */
	public void addSpotFilter(final FeatureFilter<SpotFeature> filter) { spotFilters.add(filter); }

	public void removeSpotFilter(final FeatureFilter<SpotFeature> filter) { spotFilters.remove(filter); }

	/** Remove all spot filters stored in this model.  */
	public void clearSpotFilters() { spotFilters.clear(); }

	public List<FeatureFilter<SpotFeature>> getSpotFilters() { return spotFilters; }

	public void setSpotFilters(List<FeatureFilter<SpotFeature>> spotFilters) { this.spotFilters = spotFilters; }

	/** Return the initial filter value on {@link SpotFeature#QUALITY} stored in this model. */
	public Float getInitialSpotFilterValue() { return initialSpotFilterValue;	}

	/** Set the initial filter value on {@link SpotFeature#QUALITY} stored in this model.	 */
	public void setInitialSpotFilterValue(Float initialSpotFilterValue) { this.initialSpotFilterValue = initialSpotFilterValue; }

	/** Add a filter to the list of track filters. */
	public void addTrackFilter(final FeatureFilter<TrackFeature> filter) { trackFilters.add(filter); }

	public void removeTrackFilter(final FeatureFilter<TrackFeature> filter) { trackFilters.remove(filter); }

	/** Remove all track filters stored in this model. */
	public void clearTrackFilters() { trackFilters.clear(); }

	public List<FeatureFilter<TrackFeature>> getTrackFilters() { return trackFilters; }

	public void setTrackFilters(List<FeatureFilter<TrackFeature>> trackFilters) { this.trackFilters = trackFilters; }

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
		if (DEBUG_SELECTION)
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
		if (DEBUG_SELECTION)
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
		if (DEBUG_SELECTION)
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
		if (DEBUG_SELECTION)
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
		if (DEBUG_SELECTION)
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
				if (DEBUG_SELECTION)
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
				if (DEBUG_SELECTION)
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
		if (DEBUG_SELECTION)
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
		if (DEBUG_SELECTION)
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
				if (DEBUG_SELECTION)
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
				if (DEBUG_SELECTION)
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
	 * MODEL CHANGE METHODS
	 */

	/**
	 * Move a single spot from a frame to another, then update its features.
	 * @param spotToMove  the spot to move
	 * @param fromFrame  the frame the spot originated from
	 * @param toFrame  the destination frame
	 * @param doNotify  if false, {@link TrackMateModelChangeListener}s will not be notified of this change
	 */
	public void moveSpotFrom(Spot spotToMove, Integer fromFrame, Integer toFrame) {
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

		spotsMoved.add(spotToMove); // TRANSACTION

	}

	/**
	 * Add a single spot to the collections managed by this model, then update its features.
	 */
	public void addSpotTo(Spot spotToAdd, Integer toFrame) {
		if (null != spots)  {
			if (spots.add(spotToAdd, toFrame)) {
				spotsAdded.add(spotToAdd); // TRANSACTION
				if (DEBUG)
					System.out.println("[TrackMateModel] Adding spot "+spotToAdd+" to frame "+ toFrame);
			}
		}

		if (null != filteredSpots) 
			filteredSpots.add(spotToAdd, toFrame);

		graph.addVertex(spotToAdd);

	}

	/**
	 * Remove a single spot from the collections managed by this model.
	 * @param fromFrame  the frame the spot is in, if it is known. If <code>null</code> is given,
	 * then the adequate frame is retrieved from this model's collections.
	 */
	public void removeSpotFrom(final Spot spotToRemove, Integer fromFrame) {
		if (fromFrame == null)
			fromFrame = spots.getFrame(spotToRemove);
		if (null != spots) {
			if (spots.remove(spotToRemove, fromFrame)) {
				spotsRemoved.add(spotToRemove); // TRANSACTION
				if (DEBUG)
					System.out.println("[TrackMateModel] Removing spot "+spotToRemove+" from frame "+ fromFrame);
			}
		}

		if (null != filteredSpots) 
			filteredSpots.remove(spotToRemove, fromFrame);

		graph.removeVertex(spotToRemove);
	}



	// Modify graph

	public DefaultWeightedEdge addEdge(final Spot source, final Spot target, final double weight) {
		DefaultWeightedEdge edge = graph.addEdge(source, target);
		graph.setEdgeWeight(edge, weight);
		edgesAdded.add(edge); // TRANSACTION
		if (DEBUG)
			System.out.println("[TrackMateModel] Adding edge between "+source+" and "+ target + " with weight "+weight);
		return edge;
	}

	public DefaultWeightedEdge removeEdge(final Spot source, final Spot target) {
		DefaultWeightedEdge edge = graph.removeEdge(source, target);
		edgesRemoved.add(edge); // TRANSACTION
		if (DEBUG)
			System.out.println("[TrackMateModel] Removing edge between "+source+" and "+ target);
		return edge;
	}

	public boolean removeEdge(final DefaultWeightedEdge edge) {
		edgesRemoved.add(edge);
		if (DEBUG)
			System.out.println("[TrackMateModel] Removing edge "+edge+" between "+graph.getEdgeSource(edge)+" and "+ graph.getEdgeTarget(edge));
		return graph.removeEdge(edge);
	}


	/*
	 * MODIFY SPOT FEATURES
	 */

	/**
	 * Do the actual feature update for given spots.
	 */
	private void updateFeatures(final List<Spot> spotsToUpdate) {
		if (DEBUG)
			System.out.println("[TrackMateModel] Updating the features of "+spotsToUpdate.size()+" spots.");
		if (null == spots)
			return;

		// Find common frames
		SpotCollection toCompute = filteredSpots.subset(spotsToUpdate);
		computeSpotFeatures(toCompute);
	}

	public void updateFeatures(final Spot spotToUpdate) {
		spotsUpdated.add(spotToUpdate); // Enlist for feature update when transaction is marked as finished
	}


	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Fire events.
	 * Regenerate fields derived from the mother graph.
	 */
	private void flushUpdate() {

		if (DEBUG)
			System.out.println("[TrackMateModel] #flushUpdate().");


		// We recompute tracks only if some edges have been added or removed, and 
		// if some spots have been removed (equivalent to remove edges). We do NOT
		// recompute tracks if spots have been added: they will not result in new 
		// tracks made of single spots.
		int nEdgesToSignal = edgesAdded.size() + edgesRemoved.size();
		if (nEdgesToSignal + spotsRemoved.size() > 0) {

			if (DEBUG)
				System.out.println("[TrackMateModel] #flushUpdate(): recomputing tracks.");
			this.trackSpots = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph).connectedSets();
			this.trackEdges = new ArrayList<Set<DefaultWeightedEdge>>(trackSpots.size());

			for(Set<Spot> spotTrack : trackSpots) {
				Set<DefaultWeightedEdge> spotEdge = new HashSet<DefaultWeightedEdge>();
				for(Spot spot : spotTrack)
					spotEdge.addAll(graph.edgesOf(spot));
				trackEdges.add(spotEdge);
			}
			initFeatureMap();
			trackFeatureFacade.processAllFeatures(this);
		}

		// Deal with new or moved spots: we need to update their features.
		int nSpotsToUpdate = spotsAdded.size() + spotsMoved.size() + spotsUpdated.size();
		if (nSpotsToUpdate > 0) {
			ArrayList<Spot> spotsToUpdate = new ArrayList<Spot>(nSpotsToUpdate);
			spotsToUpdate.addAll(spotsAdded);
			spotsToUpdate.addAll(spotsMoved);
			spotsToUpdate.addAll(spotsUpdated);
			updateFeatures(spotsToUpdate);
		}

		// Initialize event
		TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.MODEL_MODIFIED);

		// Configure it with spots to signal.
		int nSpotsToSignal = nSpotsToUpdate + spotsRemoved.size();
		if (nSpotsToSignal > 0) {
			ArrayList<Spot> spotsToSignal = new ArrayList<Spot>(nSpotsToSignal);
			spotsToSignal.addAll(spotsAdded);
			spotsToSignal.addAll(spotsRemoved);
			spotsToSignal.addAll(spotsMoved);
			spotsToSignal.addAll(spotsUpdated);
			ArrayList<Integer> spotsFlag = new ArrayList<Integer>(nSpotsToSignal);
			for (int i = 0; i < spotsAdded.size(); i++) 
				spotsFlag.add(TrackMateModelChangeEvent.FLAG_SPOT_ADDED);
			for (int i = 0; i < spotsRemoved.size(); i++) 
				spotsFlag.add(TrackMateModelChangeEvent.FLAG_SPOT_REMOVED);
			for (int i = 0; i < spotsMoved.size(); i++) 
				spotsFlag.add(TrackMateModelChangeEvent.FLAG_SPOT_FRAME_CHANGED);
			for (int i = 0; i < spotsUpdated.size(); i++) 
				spotsFlag.add(TrackMateModelChangeEvent.FLAG_SPOT_MODIFIED);

			event.setSpots(spotsToSignal);
			event.setSpotFlags(spotsFlag);
		}

		// Configure it with edges to signal.
		if (nEdgesToSignal > 0) {
			ArrayList<DefaultWeightedEdge> edgesToSignal = new ArrayList<DefaultWeightedEdge>(nEdgesToSignal);
			edgesToSignal.addAll(edgesAdded);
			edgesToSignal.addAll(edgesRemoved);
			ArrayList<Integer> edgesFlag = new ArrayList<Integer>(nEdgesToSignal);
			for (int i = 0; i < edgesAdded.size(); i++)
				edgesFlag.add(TrackMateModelChangeEvent.FLAG_EDGE_ADDED);
			for (int i = 0; i < edgesRemoved.size(); i++)
				edgesFlag.add(TrackMateModelChangeEvent.FLAG_EDGE_REMOVED);

			event.setEdges(edgesToSignal);
			event.setEdgeFlags(edgesFlag);
		}

		try {
			if (nEdgesToSignal + nSpotsToSignal > 0) {
				if (DEBUG)
					System.out.println("[TrackMateModel] #flushUpdate(): firing event.");
				for (final TrackMateModelChangeListener listener : modelChangeListeners)
					listener.modelChanged(event);
			}
		} finally {
			spotsAdded.clear();
			spotsRemoved.clear();
			spotsMoved.clear();
			spotsUpdated.clear();
			edgesAdded.clear();
			edgesRemoved.clear();
		}


	}

	/**
	 * Instantiate an empty feature 2D map.
	 */
	private void initFeatureMap() {
		this.trackFeatures = new ArrayList<EnumMap<TrackFeature,Float>>(trackEdges.size());
		for (int i = 0; i < trackEdges.size(); i++) {
			EnumMap<TrackFeature, Float> featureMap = new EnumMap<TrackFeature, Float>(TrackFeature.class);
			trackFeatures.add(featureMap);
		}
	}



}
