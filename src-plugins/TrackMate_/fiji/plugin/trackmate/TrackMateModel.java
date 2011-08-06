package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.RealType;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotFeatureFacade;
import fiji.plugin.trackmate.features.track.TrackFeatureFacade;
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
	private boolean useMultithreading = TrackMate_.DEFAULT_USE_MULTITHREADING;

	
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
	 * must be trough the model methods {@link #addEdge(Spot, Spot, double)},
	 * {@link #removeEdge(DefaultWeightedEdge)}, {@link #removeEdge(Spot, Spot)}.
	 */
	protected SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = null;
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
	/** 
	 * The filtered track indices. Is a set made of the indices of tracks (in {@link #trackEdges} and
	 * {@link #trackSpots}) that are retained after filtering.
	 */
	protected Set<Integer> filteredTrackIndices;

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
	protected Settings settings = new Settings();;

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
	 * DEAL WITH TRACK GRAPH 
	 */

	// Questing graph 

	/**
	 * Return the number of filtered tracks in the model.
	 */
	public int getNFilteredTracks() {
		if (filteredTrackIndices == null)
			return 0;
		else
			return filteredTrackIndices.size();
	}

	/**
	 * Return the number of <b>un-filtered</b> tracks in the model.
	 */
	public int getNTracks() {
		if (trackSpots == null)
			return 0;
		else
			return trackSpots.size();
	}

	/**
	 * Return the track index of the given edge. Return <code>null</code> if the edge 
	 * is not in any track.
	 */
	public Integer getTrackIndexOf(final DefaultWeightedEdge edge) {
		for (int i = 0; i < trackEdges.size(); i++) {
			Set<DefaultWeightedEdge> edges = trackEdges.get(i);
			if (edges.contains(edge)) {
				return i;
			}
		}
		return null;
		
	}

	/**
	 * Return the track index of the given edge. Return <code>null</code> if the edge 
	 * is not in any track.
	 */
	public Integer getTrackIndexOf(final Spot spot) {
		for (int i = 0; i < trackSpots.size(); i++) {
			Set<Spot> edges = trackSpots.get(i);
			if (edges.contains(spot)) {
				return i;
			}
		}
		return null;
	}

	/**
	 * Return true if the track with the given index is within the set of filtered tracks.
	 */
	public boolean isTrackVisible(final int index) {
		if (filteredTrackIndices.contains(index)) { // work because based on hash
			return true;
		} else {
			return false;
		}
		
	}
	
	
	/**
	 * Return the indices of the tracks that result from track feature filtering.
	 * @see #execTrackFiltering()  
	 */
	public Set<Integer> getFilteredTrackIndices() {
		return filteredTrackIndices;
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

	public DefaultWeightedEdge getEdge(final Spot source, final Spot target) {
		return graph.getEdge(source, target);
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

	public Float getTrackFeature(final int trackIndex, final TrackFeature feature) {
		return trackFeatures.get(trackIndex).get(feature);
	}

	public EnumMap<TrackFeature, double[]> getTrackFeatureValues() {
		final EnumMap<TrackFeature, double[]> featureValues = new  EnumMap<TrackFeature, double[]>(TrackFeature.class);
		Float val;
		int nTracks = getNTracks();
		for(TrackFeature feature : TrackFeature.values()) {
			// Make a double array to comply to JFreeChart histograms
			boolean noDataFlag = true;
			final double[] values = new double[nTracks];
			for (int i = 0; i < nTracks; i++) {
				val = getTrackFeature(i, feature);
				if (null == val)
					continue;
				values[i] = val; 
				noDataFlag = false;
			}

			if (noDataFlag)
				featureValues.put(feature, null);
			else 
				featureValues.put(feature, values);
		}
		return featureValues;
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

	/**
	 * Return the <b>un-filtered</b> list of tracks as a list of spots.
	 */
	public List<Set<Spot>> getTrackSpots() {
		return trackSpots;
	}

	/**
	 * Return the <b>un-filtered</b> list of tracks as a list of edges.
	 */
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
	 * @param doNotify  if true, will fire a {@link TrackMateModelChangeEvent#SPOTS_FILTERED} event.
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
	 * Overwrite the {@link #filteredTrackIndices} field, resulting normally from the {@link #execTrackFiltering()} process.
	 * @param doNotify  if true, will fire a {@link TrackMateModelChangeEvent#TRACKS_FILTERED} event.
	 */
	public void setFilteredTrackIndices(Set<Integer> filteredTrackIndices, boolean doNotify) {
		this.filteredTrackIndices = filteredTrackIndices;
		if (doNotify) {
			final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.TRACKS_FILTERED);
			for (TrackMateModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Set the graph resulting from the tracking process, and
	 * fire a {@link TrackMateModelChangeEvent#TRACKS_COMPUTED} event.
	 */
	public void setGraph(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph) {
		this.graph = graph;
		computeTracksFromGraph();
		computeTrackFeatures();
		//
		filteredTrackIndices = new HashSet<Integer>(getNTracks());
		for (int i = 0; i < getNTracks(); i++) 
			filteredTrackIndices.add(i);
		//
		final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.TRACKS_COMPUTED);
		for (TrackMateModelChangeListener listener : modelChangeListeners)
			listener.modelChanged(event);
	}

	public void clearTracks() {
		this.graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		this.trackEdges = null;
		this.trackSpots = null;
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
	public EnumMap<SpotFeature, double[]> getSpotFeatureValues() {
		return TMUtils.getSpotFeatureValues(spots.values());
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
		// Mother graph
		DefaultWeightedEdge edge = graph.addEdge(source, target);
		graph.setEdgeWeight(edge, weight);
		// Transaction
		edgesAdded.add(edge);
		if (DEBUG)
			System.out.println("[TrackMateModel] Adding edge between "+source+" and "+ target + " with weight "+weight);
		return edge;
	}

	public DefaultWeightedEdge removeEdge(final Spot source, final Spot target) {
		// Other graph
		DefaultWeightedEdge edge = graph.removeEdge(source, target);
		if (null == edge)
			System.out.println("Problem removing edge "+ edge);
		// Transaction
		edgesRemoved.add(edge); // TRANSACTION
		if (DEBUG)
			System.out.println("[TrackMateModel] Removing edge between "+source+" and "+ target);
		return edge;
	}

	public boolean removeEdge(final DefaultWeightedEdge edge) {
		// Mother graph
		boolean removed = graph.removeEdge(edge);
		if (!removed)
			System.out.println("Problem removing edge "+edge);
		// Transaction
		edgesRemoved.add(edge);
		if (DEBUG)
			System.out.println("[TrackMateModel] Removing edge "+edge+" between "+graph.getEdgeSource(edge)+" and "+ graph.getEdgeTarget(edge));
		return removed;
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
	 * Calculate all features for the given spot collection.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw image. See the {@link SpotFeatureFacade} class
	 * for details. 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void computeSpotFeatures(final SpotCollection toCompute) {

		final List<Integer> frameSet = new ArrayList(toCompute.keySet());
		final int numFrames = frameSet.size();
		final float[] calibration = settings.getCalibration();
		final AtomicInteger ai = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);

		final Thread[] threads;
		if (useMultithreading) {
			threads = SimpleMultiThreading.newThreads();
		} else {
			threads = SimpleMultiThreading.newThreads(1);
		}

		/* Prepare stack for use with Imglib.
		 * This time, since the spot coordinates are with respect to the top-left corner of the image, 
		 * we must not generate a cropped version of the image, but a full snapshot. 	 */
		final Settings uncroppedSettings = new Settings();
		uncroppedSettings.xstart = 1;
		uncroppedSettings.xend   = settings.imp.getWidth();
		uncroppedSettings.ystart = 1;
		uncroppedSettings.yend   = settings.imp.getHeight();
		uncroppedSettings.zstart = 1;
		uncroppedSettings.zend   = settings.imp.getNSlices();
		
		// Prepare the thread array
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("TrackMate spot feature calculating thread "+(1+ithread)+"/"+threads.length) {  

				public void run() {

					for (int index = ai.getAndIncrement(); index < numFrames; index = ai.getAndIncrement()) {

						int frame = frameSet.get(index);
						Image<? extends RealType> img = TMUtils.getSingleFrameAsImage(settings.imp, frame, uncroppedSettings); 
						SpotFeatureFacade featureCalculator = new SpotFeatureFacade(img, calibration);
						List<Spot> spotsThisFrame = toCompute.get(frame);
						featureCalculator.processAllFeatures(spotsThisFrame);

						logger.setProgress(progress.incrementAndGet() / (float)numFrames);
					} // Finished looping over frames
				}
			};
		}
		logger.setStatus("Calculating features...");
		logger.setProgress(0);
		
		SimpleMultiThreading.startAndJoin(threads);
		
		logger.setProgress(1);
		logger.setStatus("");
		return;
	}



	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Fire events.
	 * Regenerate fields derived from the filtered graph.
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
			computeTracksFromGraph();
			computeTrackFeatures();
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
	 * Compute the two track lists {@link #trackSpots} and {@link #trackSpots} 
	 * from the {@link #graph}. These two track lists are the only objects reflecting the 
	 * tracks visible from outside the model.
	 * <p>
	 * The guts of this method are a bit convoluted: we must make sure that tracks that were visible
	 * previous to the changes that called for this method are still visible after, event if 
	 * some tracks are merge, deleted or split.
	 */
	private void computeTracksFromGraph() {
		if (DEBUG)
			System.out.println("[TrackMateModel] #computeTracksFromGraph()");

		// Retain old values
		final List<Set<Spot>> oldTrackSpots = trackSpots;

		// Build new track lists
		this.trackSpots = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph).connectedSets();
		this.trackEdges = new ArrayList<Set<DefaultWeightedEdge>>(trackSpots.size());

		for(Set<Spot> spotTrack : trackSpots) {
			Set<DefaultWeightedEdge> spotEdge = new HashSet<DefaultWeightedEdge>();
			for(Spot spot : spotTrack) {
				spotEdge.addAll(graph.edgesOf(spot));
			}
			trackEdges.add(spotEdge);
		}

		// Try to infer correct visibility 
		if (filteredTrackIndices == null || filteredTrackIndices.isEmpty())
			return;

		if (DEBUG) {
			System.out.println("[TrackMateModel] computeTrackFromGraph: old track visibility is "+filteredTrackIndices);
		}
		final int ntracks = trackSpots.size();
		final int noldtracks = oldTrackSpots.size();
		final Set<Integer> oldTrackVisibility = filteredTrackIndices;
		filteredTrackIndices = new HashSet<Integer>(noldtracks); // Approx 
		// How to know if a new track should be visible or not?
		// We can say this: the new track should be visible if it has at least one spot
		// that can be found in a visible old track.
		for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {

			boolean shouldBeVisible = false;
			for(final Spot spot : trackSpots.get(trackIndex)) {

				for (int oldTrackIndex : oldTrackVisibility) { // we iterate over only old VISIBLE tracks
					if (oldTrackSpots.get(oldTrackIndex).contains(spot)) {
						shouldBeVisible = true;
						break;
					}
				}
				if (shouldBeVisible) {
					break;
				}
			}

			if (shouldBeVisible) {
				filteredTrackIndices.add(trackIndex);
			}

		}

		if (DEBUG) {
			System.out.println("[TrackMateModel] computeTrackFromGraph: new track visibility is "+filteredTrackIndices);
		}


	}

	private void computeTrackFeatures() {
		initFeatureMap();
		trackFeatureFacade.processAllFeatures(this);
	}

	/**
	 * Instantiate an empty feature 2D map.
	 */
	private void initFeatureMap() {
		this.trackFeatures = new ArrayList<EnumMap<TrackFeature,Float>>(getNTracks());
		for (int i = 0; i < getNTracks(); i++) {
			EnumMap<TrackFeature, Float> featureMap = new EnumMap<TrackFeature, Float>(TrackFeature.class);
			trackFeatures.add(featureMap);
		}
	}


}
