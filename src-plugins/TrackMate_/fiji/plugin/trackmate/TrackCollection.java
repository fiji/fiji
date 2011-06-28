package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.features.track.TrackFeatureFacade;

public class TrackCollection {

	public static final boolean DEBUG = true;
	
	/**
	 * The mother graph, from which all subsequent fields are calculated. 
	 * This graph is not made accessible to the outside world. Editing it
	 * must be trough the 
	 */
	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	private List<Set<DefaultWeightedEdge>> trackEdges;
	private List<Set<Spot>> trackSpots;
	/**
	 * Feature storage. We use a List of Map as a 2D Map. The list maps each track to its feature map.
	 * We use the same index that for {@link #trackEdges} and {@link #trackSpots}.
	 * The feature map maps each {@link TrackFeature} to its float value for the selected track. 
	 */
	private List<EnumMap<TrackFeature, Float>> features;
	/**
	 * Counter for the depth of nested transactions. Each call to beginUpdate
	 * increments this counter and each call to endUpdate decrements it. When
	 * the counter reaches 0, the transaction is closed and the respective
	 * events are fired. Initial value is 0.
	 */
	private int updateLevel = 0;

	private TrackFeatureFacade featureFacade;

	private List<TrackMateModelChangeListener> listeners = new ArrayList<TrackMateModelChangeListener>();

	private List<Spot> spotsAdded = new ArrayList<Spot>();
	private List<Spot> spotsRemoved = new ArrayList<Spot>();
	private List<DefaultWeightedEdge> edgesAdded = new ArrayList<DefaultWeightedEdge>();
	private List<DefaultWeightedEdge> edgesRemoved = new ArrayList<DefaultWeightedEdge>();
	
	
	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Construct an empty {@link TrackCollection}
	 */
	public TrackCollection() {
		this.graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		this.featureFacade = new TrackFeatureFacade();
	}
	
	/**
	 * Construct a {@link TrackCollection} that contains all the tracks of the given graph.
	 */
	public TrackCollection(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph) {
		this.graph = graph;
		this.featureFacade = new TrackFeatureFacade();
		refresh();
	}
	
	/*
	 * LISTENERS
	 */
	
	public void addGraphListener(final TrackMateModelChangeListener listener) {
		listeners.add(listener);
	}

	public boolean removeGraphListener(final TrackMateModelChangeListener listener) {
		return listeners.remove(listener);
	}
	
	/*
	 * GRAPH MODIFICATION
	 */
	
	public void beginUpdate()	{
		updateLevel++;
		if (DEBUG)
			System.out.println("[TrackCollection] #beginUpdate: increasing update level to "+updateLevel+".");
	}
	
	public void endUpdate()	{
		updateLevel--;
		if (DEBUG)
			System.out.println("[TrackCollection] #endUpdate: decreasing update level to "+updateLevel+".");
		if (updateLevel == 0) {
			if (DEBUG)
				System.out.println("[TrackCollection] #endUpdate: update level is 0, calling refresh.");
			refresh();
		}
	}
	
	// Adding / Removing
	
	public void addVertex(final Spot spot) {
		graph.addVertex(spot);
		spotsAdded.add(spot);
	}
	
	public boolean removeVertex(final Spot spot) {
		spotsRemoved.add(spot);
		return graph.removeVertex(spot);
	}
	
	public DefaultWeightedEdge addEdge(final Spot source, final Spot target, final double weight) {
		DefaultWeightedEdge edge = graph.addEdge(source, target);
		graph.setEdgeWeight(edge, weight);
		edgesAdded.add(edge);
		return edge;
	}
	
	public DefaultWeightedEdge removeEdge(final Spot source, final Spot target) {
		DefaultWeightedEdge edge = graph.removeEdge(source, target);
		edgesRemoved.add(edge);
		return edge;
	}

	public boolean removeEdge(final DefaultWeightedEdge edge) {
		edgesRemoved.add(edge);
		return graph.removeEdge(edge);
	}
	
	// Questing graph 
	
	public Spot getEdgeSource(final DefaultWeightedEdge edge) {
		return graph.getEdgeSource(edge);
	}

	public Spot getEdgeTarget(final DefaultWeightedEdge edge) {
		return graph.getEdgeTarget(edge);
	}
	
	public double getEdgeWeight(final DefaultWeightedEdge edge) {
		return graph.getEdgeWeight(edge);
	}
	
	public boolean containsVertex(final Spot spot) {
		return graph.containsVertex(spot);
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
	
	public Set<Spot> vertexSet() {
		return graph.vertexSet();
	}


	public DepthFirstIterator<Spot, DefaultWeightedEdge> getDepthFirstIterator(Spot start) {
		return new DepthFirstIterator<Spot, DefaultWeightedEdge>(graph, start);
	}
	
	/*
	 * FEATURES
	 */
	
	public void putFeature(final int trackIndex, final TrackFeature feature, final Float value) {
		features.get(trackIndex).put(feature, value);
	}
	
	public void computeFeatures() {
		featureFacade.processAllFeatures(this);
	}
	
	/*
	 * GETTERS
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


	/*
	 * TRACK CONTENT METHODS
	 */
	
	public int size() {
		return trackSpots.size();
	}
	
	/**
	 * Return an iterator that iterates over the tracks as a set of spots. 
	 */
	public Iterator<Set<Spot>> spotIterator() {
		return trackSpots.iterator();
	}

	/**
	 * Return an iterator that iterates over the tracks as a set of edges. 
	 */
	public Iterator<Set<DefaultWeightedEdge>> edgeIterator() {
		return trackEdges.iterator();
	}

	
	/*
	 * UTILITIES
	 */
	
	/**
	 * Return an array of exactly 2 spots which are the 2 vertices of the given edge.
	 * If the {@link SpotFeature#POSITION_T} is calculated for both spot, the array 
	 * will be sorted by increasing time.
	 */
	public Spot[] getSpotsFor(final DefaultWeightedEdge edge) {
		Spot[] spots = new Spot[2];
		Spot spotA = graph.getEdgeSource(edge);
		Spot spotB = graph.getEdgeTarget(edge);
		Float tA = spotA.getFeature(SpotFeature.POSITION_T);
		Float tB = spotB.getFeature(SpotFeature.POSITION_T);
		if (tA != null && tB != null && tA > tB) {
			spots[0] = spotB;
			spots[1] = spotA;
		} else {
			spots[0] = spotA;
			spots[1] = spotB;
		}
		return spots;
	}
	
	public String toString(int i) {
		String str = "Track "+i+": ";
		for (TrackFeature feature : TrackFeature.values())
			str += feature.shortName() + " = " + features.get(i).get(feature) +", ";			
		return str;
	}

	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Regenerate fields derived from the mother graph.
	 */
	private void refresh() {
		if (DEBUG)
			System.out.println("[TrackCollection] #refresh(): building individual tracks.");
		this.trackSpots = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph).connectedSets();
		this.trackEdges = new ArrayList<Set<DefaultWeightedEdge>>(trackSpots.size());
		initFeatureMap();
		
		for(Set<Spot> spotTrack : trackSpots) {
			Set<DefaultWeightedEdge> spotEdge = new HashSet<DefaultWeightedEdge>();
			for(Spot spot : spotTrack)
				spotEdge.addAll(graph.edgesOf(spot));
			trackEdges.add(spotEdge);
		}

		if (DEBUG)
			System.out.println("[TrackCollection] #refresh(): re-calculating features.");
		initFeatureMap();
		featureFacade.processAllFeatures(this);
	
		if (DEBUG)
			System.out.println("[TrackCollection] #refresh(): firing events.");
		// TWO events are fired: one for the spots, one for the edges
		// Spots added & removed
		if (!spotsAdded.isEmpty() && !spotsRemoved.isEmpty()) {
			final List<Integer> spotFlags = new ArrayList<Integer>(spotsAdded.size() + spotsRemoved.size());
			for (int i = 0; i < spotsAdded.size(); i++) 
				spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_ADDED);
			for (int i = 0; i < spotsRemoved.size(); i++) 
				spotFlags.add(TrackMateModelChangeEvent.FLAG_SPOT_REMOVED);
			final List<Spot> spots = new ArrayList<Spot>(spotsAdded.size() + spotsRemoved.size());
			spots.addAll(spotsAdded);
			spots.addAll(spotsRemoved);
			
			final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.SPOTS_MODIFIED);
			event.setSpots(spots);
			event.setSpotFlag(spotFlags);
			for (final TrackMateModelChangeListener listener : listeners)
				listener.modelChanged(event);
			
			spotsAdded.clear();
			spotsRemoved.clear();
		}

		// Edges added & removed
		if (!edgesAdded.isEmpty() && !edgesRemoved.isEmpty()) {
			final List<Integer> edgeFlags = new ArrayList<Integer>(edgesAdded.size() + edgesRemoved.size());
			for (int i = 0; i < edgesAdded.size(); i++) 
				edgeFlags.add(TrackMateModelChangeEvent.FLAG_EDGE_ADDED);
			for (int i = 0; i < edgesRemoved.size(); i++) 
				edgeFlags.add(TrackMateModelChangeEvent.FLAG_EDGE_REMOVED);
			final List<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>(edgesAdded.size() + edgesRemoved.size());
			edges.addAll(edgesAdded);
			edges.addAll(edgesRemoved);
			
			final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.TRACKS_MODIFIED);
			event.setEdges(edges);
			event.setEdgeFlag(edgeFlags);
			for (final TrackMateModelChangeListener listener : listeners)
				listener.modelChanged(event);

			edgesAdded.clear();
			edgesRemoved.clear();
		}
		
		
		
	}
	
	/**
	 * Instantiate an empty feature 2D map.
	 */
	private void initFeatureMap() {
		this.features = new ArrayList<EnumMap<TrackFeature,Float>>(trackEdges.size());
		for (int i = 0; i < trackEdges.size(); i++) {
			EnumMap<TrackFeature, Float> featureMap = new EnumMap<TrackFeature, Float>(TrackFeature.class);
			features.add(featureMap);
		}
	}





	
}
