package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.VertexFactory;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.AsUnweightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.graph.Function1;
import fiji.plugin.trackmate.graph.SortedDepthFirstIterator;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * A component of {@link TrackMateModel} specialized for tracks
 * @author Jean-Yves Tinevez
 */
public class TrackGraphModel {

	private static final boolean DEBUG = false;

	private static int currentNameIndex;

	/**
	 * The model this component belongs to. This reference is used to pull listeners.
	 */
	private final TrackMateModel model;


	/**
	 * The mother graph, from which all subsequent fields are calculated. This
	 * graph is not made accessible to the outside world. Editing it must be
	 * trough the model methods {@link #addEdge(Spot, Spot, double)},
	 * {@link #removeEdge(DefaultWeightedEdge)}, {@link #removeEdge(Spot, Spot)}
	 * .
	 */
	private ListenableDirectedGraph<Spot,DefaultWeightedEdge> graph =
			new ListenableDirectedGraph<Spot, DefaultWeightedEdge>(new SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class));
	/** The tracks stored as a set of their edges. They are indexed by the hash of the matching spot set. 
	 * @see #trackSpots */
	private Map<Integer, Set<DefaultWeightedEdge>> trackEdges = new HashMap<Integer, Set<DefaultWeightedEdge>>();
	/** The tracks stored as a set of their spots. They are indexed by the hash of the target spot set. */
	private Map<Integer, Set<Spot>> trackSpots = new HashMap<Integer, Set<Spot>>();
	/** The map of track names, indexed by track ID. */
	private Map<Integer, String> trackNames = new HashMap<Integer, String>();

	/**
	 * The filtered track keys. Is a set made of the keys in the two maps
	 * {@link #trackEdges} and {@link #trackSpots} for the tracks that are retained after
	 * filtering, and set visible. The user can manually add to or remove from
	 * this list.	 */
	private Set<Integer> filteredTrackKeys = new HashSet<Integer>();

	/*
	 * TRANSACTION FIELDS
	 */
	List<DefaultWeightedEdge> edgesAdded = new ArrayList<DefaultWeightedEdge>();
	List<DefaultWeightedEdge> edgesRemoved = new ArrayList<DefaultWeightedEdge>();
	List<DefaultWeightedEdge> edgesModified = new ArrayList<DefaultWeightedEdge>();

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Default visibility constructor: can be instantiated only from {@link TrackMateModel}.
	 */
	TrackGraphModel(final TrackMateModel parentModel) {
		this.model = parentModel;
		graph.addGraphListener(new MyGraphListener());
	}

	/*
	 * METHODS
	 */

	/**
	 * @return a new graph with the same structure as the one wrapped here, and with vertices generated
	 * by the given {@link Function1}. Edges are copied in direction and weight.
	 * @param factory the vertex factory used to instantiate new vertices in the new graph
	 * @param function the function used to set values of a new vertex in the new graph, from the matching spot
	 * @param mappings a map that will receive mappings from {@link Spot} to the new vertices. Can be <code>null</code>
	 * if you do not want to get the mappings
	 */
	public <V> SimpleDirectedWeightedGraph<V, DefaultWeightedEdge> copy(final VertexFactory<V> factory, final Function1<Spot, V> function, Map<Spot, V> mappings) {
		SimpleDirectedWeightedGraph<V, DefaultWeightedEdge> copy = new SimpleDirectedWeightedGraph<V, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		Set<Spot> spots = graph.vertexSet();
		// To store mapping of old graph vs new graph
		Map<Spot, V> map;
		if (null == mappings) {
			map = new HashMap<Spot, V>(spots.size());
		} else {
			map = mappings;
		}

		// Generate new vertices
		for (Spot spot : spots) {
			V vertex = factory.createVertex();
			function.compute(spot, vertex);
			map.put(spot, vertex);
			copy.addVertex(vertex);
		}

		// Generate new edges
		for(DefaultWeightedEdge edge : graph.edgeSet()) {
			DefaultWeightedEdge newEdge = copy.addEdge(map.get(graph.getEdgeSource(edge)), map.get(graph.getEdgeTarget(edge)));
			copy.setEdgeWeight(newEdge, graph.getEdgeWeight(edge));
		}

		return copy;
	}



	/*
	 * BULK TRACKS MODIFICATION
	 */



	/**
	 * Set the graph resulting from the tracking process, and fire a
	 * {@link ModelChangeEvent#TRACKS_COMPUTED} event. The {@link #filteredTrackKeys}
	 * field is set to make all new tracks visible by default.
	 * <p>
	 * Calling this method <b>overwrites<b> the current graph. Calling this method does <b>not</b>
	 * trigger the calculation of the track features. 
	 */
	public void setGraph(final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph) {
		this.graph = new ListenableDirectedGraph<Spot, DefaultWeightedEdge>(graph);
		this.graph.addGraphListener(new MyGraphListener());
		//
		computeTracksFromGraph();
		//
		filteredTrackKeys = new HashSet<Integer>(getNTracks());
		for (Integer trackID : trackSpots.keySet())
			filteredTrackKeys.add(trackID);
		//
		final ModelChangeEvent event = new ModelChangeEvent(this, ModelChangeEvent.TRACKS_COMPUTED);
		for (ModelChangeListener listener : model.modelChangeListeners)
			listener.modelChanged(event);
	}

	public void clearTracks() {
		this.graph = new ListenableDirectedGraph<Spot, DefaultWeightedEdge>(
				new SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class));
		this.graph.addGraphListener(new MyGraphListener());
		this.trackEdges = new HashMap<Integer, Set<DefaultWeightedEdge>>();
		this.trackSpots = new HashMap<Integer, Set<Spot>>();
	}



	/*
	 * MANUAL TRACK MODIFICATION
	 */


	boolean addSpot(Spot spotToAdd) {
		return graph.addVertex(spotToAdd);
	}

	boolean removeSpot(Spot spotToRemove) {
		return graph.removeVertex(spotToRemove);

	}

	/**
	 * Creates a new edge in this graph, going from the source spot to the
	 * target spot, and returns the created edge. This model does not allow
	 * edge-multiplicity. If the graph already contains an edge
	 * from the specified source to the specified target, than this method does
	 * not change the graph and returns <code>null</code>.
	 *
	 * <p>The source and target spots must already be contained in this
	 * model. If they are not found in graph IllegalArgumentException is
	 * thrown.</p>
	 *
	 * @param source source spot of the edge.
	 * @param target target spot of the edge.
	 *
	 * @return The newly created edge if added to the graph, otherwise <code>
	 * null</code>.
	 *
	 * @throws IllegalArgumentException if source or target spots are not
	 * found in the graph.
	 * @throws NullPointerException if any of the specified spots is <code>
	 * null</code>.
	 */
	DefaultWeightedEdge addEdge(final Spot source, final Spot target, final double weight) {
		// Mother graph
		DefaultWeightedEdge edge = graph.addEdge(source, target);
		if (null != edge) {
			edgesAdded.add(edge);
			graph.setEdgeWeight(edge, weight);
			if (DEBUG)
				System.out.println("[TrackGraphModel] Adding edge between " + source + " and " + target + " with weight " + weight);
		}
		return edge;
	}

	/**
	 * Removes an edge going from source spot to target spot, if such
	 * spots and such edge exist in this graph. Returns the edge if removed
	 * or <code>null</code> otherwise.
	 *
	 * @param sourceVertex source spot of the edge.
	 * @param targetVertex target spot of the edge.
	 *
	 * @return The removed edge, or <code>null</code> if no edge removed.
	 */
	DefaultWeightedEdge removeEdge(final Spot source, final Spot target) {
		// Other graph
		DefaultWeightedEdge edge = graph.removeEdge(source, target);
		if (null != edge) {
			edgesRemoved.add(edge);
			if (DEBUG)
				System.out.println("[TrackGraphModel] Removing edge between " + source + " and " + target);
		}
		return edge;
	}

	/**
	 * Removes the specified edge from the graph. Removes the specified edge
	 * from this graph if it is present. More formally, removes an edge <code>
	 * e2</code> such that <code>e2.equals(e)</code>, if the graph contains such
	 * edge. Returns <tt>true</tt> if the graph contained the specified edge.
	 * (The graph will not contain the specified edge once the call returns).
	 *
	 * <p>If the specified edge is <code>null</code> returns <code>
	 * false</code>.</p>
	 *
	 * @param edge edge to be removed from this graph, if present.
	 *
	 * @return <code>true</code> if and only if the graph contained the
	 * specified edge.
	 */
	boolean removeEdge(final DefaultWeightedEdge edge) {
		// Mother graph
		boolean removed = graph.removeEdge(edge);
		if (removed) {
			edgesRemoved.add(edge);
			model.getSelectionModel().removeEdgeFromSelection(edge);
			if (DEBUG)
				System.out.println("[TrackGraphModel] Removing edge " + edge + " between " + graph.getEdgeSource(edge) + " and " + graph.getEdgeTarget(edge));
		}
		return removed;
	}

	/**
	 * Assigns a weight to an edge.
	 *
	 * @param e edge on which to set weight
	 * @param weight new weight for edge
	 */
	void setEdgeWeight(final DefaultWeightedEdge edge, double weight) {
		graph.setEdgeWeight(edge, weight);
		// mark for update
		edgesModified.add(edge);
	}

	/*
	 * TRACK FILTERING
	 */

	/**
	 * Overwrite the {@link #filteredTrackKeys} field, resulting normally from the 
	 * {@link #execTrackFiltering()} process.
	 * 
	 * @param doNotify if true, will fire a {@link ModelChangeEvent#TRACKS_VISIBILITY_CHANGED} 
	 * event.
	 */
	public void setFilteredTrackIDs(Set<Integer> visibleTrackIndices, boolean doNotify) {
		this.filteredTrackKeys = visibleTrackIndices;
		if (doNotify) {
			final ModelChangeEvent event = new ModelChangeEvent(this, ModelChangeEvent.TRACKS_VISIBILITY_CHANGED);
			for (ModelChangeListener listener : model.modelChangeListeners)
				listener.modelChanged(event);
		}
	}

	/**
	 * Change the visibility of a given track, whose ID is given. 
	 * 
	 * @param trackID  the index of the track whose visibility is to change. If <code>null</code>,
	 * nothing is done.
	 * @param visible  if true, the track will be made visible, and invisible otherwise. If the track
	 * was already visible, or respectively invisible, nothing is done.
	 * @param doNotify  if true, and if some changes occurred, an event with the ID 
	 * {@link ModelChangeEvent#TRACKS_VISIBILITY_CHANGED} will be fired.
	 * @return  true if and only if the call to this method actually changed the current visible 
	 * settings of tracks.
	 */
	public boolean setFilteredTrackID(Integer trackID, boolean visible, boolean doNotify) {
		if (trackID == null)
			return false;

		boolean modified = false;
		if (visible) {
			modified = filteredTrackKeys.add(trackID);
		} else {
			modified = filteredTrackKeys.remove(trackID);
		}

		if (doNotify && modified) {
			ModelChangeEvent event = new ModelChangeEvent(this, ModelChangeEvent.TRACKS_VISIBILITY_CHANGED);
			for (ModelChangeListener listener : model.modelChangeListeners) 
				listener.modelChanged(event);
		}

		return modified;
	}

	/*
	 * QUERYING TRACKS
	 */


	/**
	 * Return the number of filtered tracks in the model.
	 */
	public int getNFilteredTracks() {
		if (filteredTrackKeys == null)
			return 0;
		else
			return filteredTrackKeys.size();
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
	 * @return the track index of the given edge. Return <code>null</code> if the
	 * edge is not in any track.
	 */
	public Integer getTrackIDOf(final DefaultWeightedEdge edge) {
		for (Integer trackID : trackSpots.keySet()) {
			Set<DefaultWeightedEdge> edges = trackEdges.get(trackID);
			if (edges.contains(edge)) {
				return trackID;
			}
		}
		return null;
	}

	/**
	 * @return the track index of the given spot. Return <code>null</code> if the
	 * spot is not in any track.
	 */
	public Integer getTrackIDOf(final Spot spot) {
		for (Integer trackID : trackSpots.keySet()) {
			Set<Spot> spots = trackSpots.get(trackID);
			if (spots.contains(spot)) {
				return trackID;
			}
		}
		return null;
	}

	/**
	 * @return the name of the track with the given ID.
	 */
	public String getTrackName(Integer trackID) {
		return trackNames.get(trackID);
	}

	/**
	 * Set the name of the track with the given ID.
	 * @return the given name
	 */
	public String setTrackName(Integer trackID, String name) {
		return trackNames.put(trackID, name);
	}


	/**
	 * @return  the track indexed by the given key as a set of spot.
	 * The tracks returned here are guaranteed to have at least 2 spots in it. 
	 * There is no empty track, nor a track made of a single spot. 
	 */
	public Set<Spot> getTrackSpots(Integer trackID) {
		return trackSpots.get(trackID);
	}

	/**
	 * @return  the track indexed by the given key as a set of spot.
	 * The track returned here are guaranteed to have at least 1 edge in it. 
	 * There is no empty track. 
	 */
	public Set<DefaultWeightedEdge> getTrackEdges(Integer trackID) {
		return trackEdges.get(trackID);
	}

	/**
	 * @return the <b>un-filtered</b> map of tracks as a set of spots.
	 * The tracks returned here are guaranteed to have at least 2 spots in it. 
	 * There is no empty track, nor a track made of a single spot. 
	 * <p>
	 * <b>Note:</b> the actual map object return by this method is 
	 * re-instantiated every time a change is made to the model that affects
	 * tracks. So this method needs to be called again after each change for
	 * the map to be accurate.
	 */
	public Map<Integer,Set<Spot>> getTrackSpots() {
		return trackSpots;
	}

	/**
	 * return the <b>un-filtered</b> map of tracks as a set of edges.
	 * The tracks returned here are guaranteed to have at least 1 edge in it. 
	 * There is no empty track. 
	 * <p>
	 * <b>Note:</b> the actual map object return by this method is 
	 * re-instantiated every time a change is made to the model that affects
	 * tracks. So this method needs to be called again after each change for
	 * the map to be accurate.
	 */
	public Map<Integer,Set<DefaultWeightedEdge>> getTrackEdges() {
		return trackEdges;
	}

	/**
	 * Return the track index of the given edge. Return <code>null</code> if the
	 * edge is not in any track.
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
	 * @return true if the track with the given ID is within the set of
	 * filtered tracks.
	 */
	public boolean isTrackFiltered(final Integer trackID) {
		return filteredTrackKeys.contains(trackID);
	}

	/**
	 * @return the keys of the tracks that are filtered.
	 * <b>The set is ordered by the corresponding track names.</b>
	 * <p>
	 * <b>Note:</b> the actual {@link Set} object return by this method is 
	 * re-instantiated every time a change is made to the model that affects
	 * tracks. So this method needs to be called again after each change for
	 * the indices to be accurate.
	 * 
	 * @see #execTrackFiltering()
	 */
	public Set<Integer> getFilteredTrackIDs() {
		HashMap<Integer, String> names = new HashMap<Integer, String>(filteredTrackKeys.size());
		for (Integer trackID : filteredTrackKeys) {
			names.put(trackID, trackNames.get(trackID));
		}
		Map<Integer, String> sortedMap = TMUtils.sortByValue(names);
		return sortedMap.keySet();
	}

	/**
	 * @return a set of integer keys for all the tracks contained in this model.
	 * <b>The set is ordered by the corresponding track names.</b>
	 * These keys can then be used to iterate or retrieve individual tracks using
	 * {@link #getTrackEdges(int)}, {@link #getTrackSpots(int)}, etc...
	 * <p>
	 * <b>Note:</b> a track ID is just the {@link Object#hashCode()} of the track 
	 * as a {@link Set<Spot>}.
	 * <p>
	 * <b>Note:</b> the actual {@link Set} object return by this method is 
	 * re-instantiated at every method call, so that any change in names is
	 * reflected at this call.
	 */
	public Set<Integer> getTrackIDs() { 
		Map<Integer, String> sortedMap = TMUtils.sortByValue(trackNames);
		return sortedMap.keySet();
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

	/**
	 * @return true if and only if this model contains a link going from the source spot 
	 * to the target spot. Careful: this methods takes into consideration the direction
	 * of the edge. The result might be different if source and target are permuted since
	 * we use a directed graph. If any of the specified vertices does not exist 
	 * in the graph, or if is null, returns false.
	 * @param source  the spot the edge to find starts from 
	 * @param target  the spot the edge to find goes to
	 */
	public boolean containsEdge(final Spot source, final Spot target) {
		return graph.containsEdge(source, target);
	}

	/**
	 * @return an link connecting source spot to target spot if such
	 * spots and such link exist in the model. Otherwise returns <code>
	 * null</code>. If any of the specified spots is <code>null</code>
	 * returns <code>null</code>. This method takes into account the direction
	 * of the link, and will return <code>null</code> even if there is an
	 * existing link, but in the opposite direction.
	 *
	 * @param source source spot of the link.
	 * @param target target spot of the link.
	 */
	public DefaultWeightedEdge getEdge(final Spot source, final Spot target) {
		return graph.getEdge(source, target);
	}

	/**
	 * @return a set of all links touching the specified spot. If no links are
	 * touching the specified spot, return an empty set. If the spot does not belong
	 * in the graph model, return <code>null</code>.
	 */
	public Set<DefaultWeightedEdge> edgesOf(final Spot spot) {
		if (!graph.containsVertex(spot)) {
			return null;
		}
		return graph.edgesOf(spot);
	}

	/**
	 * @return a set of the edges contained in this graph. The set is backed by
	 * the graph, so changes to the graph are reflected in the set. If the graph
	 * is modified while an iteration over the set is in progress, the results
	 * of the iteration are undefined.
	 */
	public Set<DefaultWeightedEdge> edgeSet() {
		return graph.edgeSet();
	}

	/**
	 * Returns a set of the spots contained in the tracks. The set is backed
	 * by the graph, so changes to the tracks are reflected in the set. If the
	 * tracks are modified while an iteration over the set is in progress, the
	 * results of the iteration are undefined.
	 *
	 * <p>The graph implementation may maintain a particular set ordering (e.g.
	 * via {@link java.util.LinkedHashSet}) for deterministic iteration, but
	 * this is not required. It is the responsibility of callers who rely on
	 * this behavior to only use graph implementations which support it.</p>
	 *
	 * @return a set view of the spots contained in this graph model.
	 */
	public Set<Spot> vertexSet() {
		return graph.vertexSet();
	}

	/**
	 * @return a new depth first iterator over the spots connected by links in this model.
	 * A boolean flag allow to set whether the returned iterator does take into account 
	 * the edge direction. If true, the iterator will not be able to iterate backward in time.
	 * @param start  the spot to start iteration with. Can be <code>null</code>, then the start will be taken
	 * randomly and will traverse all the links.
	 * @param directed  if true returns a directed iterator, undirected if false.
	 */
	public DepthFirstIterator<Spot, DefaultWeightedEdge> getDepthFirstIterator(Spot start, boolean directed) {
		if (directed) {
			return new DepthFirstIterator<Spot, DefaultWeightedEdge>(graph, start);			
		} else {
			return new DepthFirstIterator<Spot, DefaultWeightedEdge>(new AsUndirectedGraph<Spot, DefaultWeightedEdge>(graph), start);
		}
	}

	/**
	 * @return a new depth first iterator over the spots connected by links in this model.
	 * This iterator is sorted: when branching, it chooses the next vertex according to a specified comparator. 
	 * A boolean flag allow to set whether the returned iterator does take into account 
	 * the edge direction. If true, the iterator will not be able to iterate backward in time.
	 * @param start  the spot to start iteration with. Can be <code>null</code>, then the start will be taken
	 * randomly and will traverse all the links.
	 * @param directed  if true returns a directed iterator, undirected if false.
	 * @param comparator the comparator to use to pick children in order when branching.
	 */
	public SortedDepthFirstIterator<Spot, DefaultWeightedEdge> getSortedDepthFirstIterator(Spot start, Comparator<Spot> comparator, boolean directed) {
		if (directed) {
			return new SortedDepthFirstIterator<Spot, DefaultWeightedEdge>(graph, start, comparator);			
		} else {
			return new SortedDepthFirstIterator<Spot, DefaultWeightedEdge>(new AsUndirectedGraph<Spot, DefaultWeightedEdge>(graph), start, comparator);
		}
	}




	public BreadthFirstIterator<Spot, DefaultWeightedEdge> getBreadthFirstIterator(Spot start, boolean directed) {
		if (directed) {
			return new BreadthFirstIterator<Spot, DefaultWeightedEdge>(graph, start);
		} else {
			return new BreadthFirstIterator<Spot, DefaultWeightedEdge>(new AsUndirectedGraph<Spot, DefaultWeightedEdge>(graph), start);
		}
	}

	/** @see DirectedNeighborIndex */
	public DirectedNeighborIndex<Spot, DefaultWeightedEdge> getDirectedNeighborIndex() {
		return new DirectedNeighborIndex<Spot, DefaultWeightedEdge>(graph);
	}

	public String trackToString(Integer trackID) {
		String str = trackNames.get(trackID) + ": ";
		for (String feature : model.getFeatureModel().getTrackFeatures())
			str += feature + " = "	+ model.getFeatureModel().getTrackFeature(trackID, feature)+ ", ";
		return str;
	}

	/**
	 * @return shortest path between two connected spot, using Dijkstra's algorithm.
	 * The edge weights, if any, are ignored here, meaning that the returned path is
	 * the shortest in terms of number of edges.
	 * <p>
	 * Return <code>null</code> if the two spots are not connected by a track, or if 
	 * one of the spot do not belong to the graph, or if the {@link #graph} field is
	 * <code>null</code>.
	 *  
	 * @param source  the spot to start the path with
	 * @param target  the spot to stop the path with
	 */
	public List<DefaultWeightedEdge> dijkstraShortestPath(final Spot source, final Spot target) {
		if (null == graph) {
			return null;
		}
		AsUnweightedGraph<Spot, DefaultWeightedEdge> unWeightedGrah = new  AsUnweightedGraph<Spot, DefaultWeightedEdge>(graph);
		DijkstraShortestPath<Spot, DefaultWeightedEdge> pathFinder = new DijkstraShortestPath<Spot, DefaultWeightedEdge>(unWeightedGrah, source, target);
		List<DefaultWeightedEdge> path = pathFinder.getPathEdgeList();
		return path;
	}

	/*
	 * PRIVATE METHODS
	 */


	/**
	 * Compute the two track lists {@link #trackSpots} and {@link #trackSpots}
	 * from the {@link #graph}. These two track lists are the only objects
	 * reflecting the tracks visible from outside the model.
	 * <p>
	 * The guts of this method are a bit convoluted: we must make sure that
	 * tracks that were visible previous to the changes that called for this
	 * method are still visible after, event if some tracks are merge, deleted
	 * or split.
	 * 
	 * @return  the map of the old track parts ID vs new ID: That is, all spots and edges 
	 * in the old track with ID oldID are now parts of the new tracks with new 
	 * IDs contained in the matching value set.
	 */
	void computeTracksFromGraph() {

		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph()");
		}

		// Retain old values
		Map<Integer, Set<Spot>> oldTrackSpots = trackSpots;
		Map<Integer, String> oldNames = trackNames;

		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph(): storing " + oldTrackSpots.size() + " old spot tracks.");
		}

		// Build new track lists
		List<Set<Spot>> connectedSets = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph).connectedSets();
		this.trackSpots = new HashMap<Integer, Set<Spot>>(connectedSets.size());
		this.trackEdges = new HashMap<Integer, Set<DefaultWeightedEdge>>(connectedSets.size());
		this.trackNames = new HashMap<Integer, String>(connectedSets.size());

		for(Set<Spot> track : connectedSets) {

			// We DO NOT WANT tracks made of a single spot. They will reside on the side, 
			// as lonely spots
			if (track.size() < 2) {
				continue;
			}

			Integer uniqueKey = track.hashCode();
			// Add to spot set collection
			trackSpots.put(uniqueKey, track);
			// Add to edge set collection, using the same hash as a key
			Set<DefaultWeightedEdge> spotEdge = new HashSet<DefaultWeightedEdge>();
			for (Spot spot : track) {
				spotEdge.addAll(graph.edgesOf(spot));
			}
			trackEdges.put(uniqueKey, spotEdge);

		}

		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph(): found " + trackSpots.size() + " new spot tracks.");
		}

		// Try to infer correct visibility
		final int noldtracks = oldTrackSpots.size();
		final Map<Integer, Boolean> oldTrackVisibility = new HashMap<Integer, Boolean>(noldtracks);
		for (Integer oldKey : oldTrackSpots.keySet()) {
			oldTrackVisibility.put(oldKey, filteredTrackKeys.contains(oldKey));
		}
		filteredTrackKeys = new HashSet<Integer>(noldtracks); // Approx

		/* A special case: if there is some completely new track appearing, it 
		 * should be visible by default. It also needs a default name.
		 * How do we know that some tracks are "de novo"?
		 * For de novo tracks, there is no spot in the Set<Spot> that can be found in 
		 * oldTrackSpots.	 */

		/*
		 * Map the new track IDs to the old ones. That is, the new track with ID newID
		 * is built from parts of the old tracks with old IDs contained in the matching value set.
		 */
		Map<Integer, Set<Integer>> newToOldKeyMap = new HashMap<Integer, Set<Integer>>(trackSpots.size());
		for (Integer trackID : trackSpots.keySet()) {
			newToOldKeyMap.put(trackID, new HashSet<Integer>());
		}

		// Loop over old tracks to get where they can be found in the new tracks
		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph(): inspecting old tracks.");
		}

		for (Integer oldKey : oldTrackSpots.keySet()) {
			Set<Spot> oldTrack = oldTrackSpots.get(oldKey);

			for (Integer trackKey : trackSpots.keySet()) { // Iterate over new tracks
				Set<Spot> track = trackSpots.get(trackKey);

				boolean found = false;
				for (Spot spot : track) {
					if (oldTrack.contains(spot)) {
						found = true;
						break;
					}
				}

				if (found) {
					// There were common elements. We store this old track ID as part of the new track, and skip the rest
					if (DEBUG) {
						System.out.println("[TrackGraphModel] #computeTracksFromGraph(): old track " + oldKey + " parts were found in new track " + trackKey);
					}
					newToOldKeyMap.get(trackKey).add(oldKey);
					// FIXME can we skip once we have found one?
				}

			} // Finished iterating over new tracks

		}




		// Assemble visibility and name from old parts

		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph(): assembling new tracks visibility and name.");
		}


		// Build this map
		for (Integer trackKey : trackSpots.keySet()) {

			if (newToOldKeyMap.get(trackKey).isEmpty()) {
				// Is new, so we make it visible and give it a default name.
				filteredTrackKeys.add(trackKey);
				trackNames.put(trackKey, generateDefaultTrackName() );
				if (DEBUG) {
					System.out.println("[TrackGraphModel] #computeTracksFromGraph(): track " + trackKey + " is completely new. Making it visible with name " + trackNames.get(trackKey));
				}

			} else {
				/* Is made of old tracks, so we can pick its name and visibility from there.
				 * 
				 * We copy the name from the largest old track that is part of this one now.
				 * 
				 * How to know if a new track should be visible or not?
				 * We can say this: the new track should be visible if it has at least
				 * one spot that can be found in a visible old track. */
				Iterator<Integer> it = newToOldKeyMap.get(trackKey).iterator();
				Integer keyOfLargestOldTracks = it.next();
				boolean shouldBeVisible = oldTrackVisibility.get(keyOfLargestOldTracks);
				while (it.hasNext()) {
					Integer oldKey = it.next();
					if (oldTrackSpots.get(oldKey).size() > oldTrackSpots.get(keyOfLargestOldTracks).size()) {
						keyOfLargestOldTracks = oldKey;	
					}
					shouldBeVisible = shouldBeVisible | oldTrackVisibility.get(oldKey);
				}
				trackNames.put(trackKey, oldNames.get(keyOfLargestOldTracks));
				if (shouldBeVisible) {
					filteredTrackKeys.add(trackKey);
				}
				if (DEBUG) {
					System.out.println("[TrackGraphModel] #computeTracksFromGraph(): track " + trackKey + " is not new; it is made in parts of old tracks " +  newToOldKeyMap.get(trackKey));
					System.out.println("[TrackGraphModel] #computeTracksFromGraph():  - giving it the name: " + trackNames.get(trackKey));
					System.out.println("[TrackGraphModel] #computeTracksFromGraph():  - making it visible? " + shouldBeVisible);

				}
			}
		}


		// Clean track feature value map
		HashSet<Integer> trackIDsToRemove = new HashSet<Integer>(oldTrackSpots.keySet());
		trackIDsToRemove.removeAll(trackSpots.keySet());
		for (Integer toRemove : trackIDsToRemove) {
			model.getFeatureModel().trackFeatureValues.remove(toRemove);
		}


		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph(): the end; found " + trackSpots.size() + " new spot tracks.");
		}
	}




	private static final String generateDefaultTrackName() {
		String columnString = "";
		int number = ++currentNameIndex; 
		while (number > 0) {
			int currentLetterNumber = (number - 1) % 26;
			char currentLetter = (char)(currentLetterNumber + 65);
			columnString = currentLetter + columnString;
			number = (number - (currentLetterNumber + 1)) / 26;
		}
		return "Track_" + columnString;
	}


	/**
	 * This listener class is made to deal with complex changes in the track graph.
	 * <p>
	 * By complex change, we mean the changes occurring in the graph caused by 
	 * another change that was initiated manually by the user. For instance, imagine
	 * we have a simple track branch made of 5 spots that link linearly, like this:
	 * <pre>
	 * 	S1 - S2 - S3 - S4 - S5
	 * </pre>
	 * The user might want to remove the S3 spot, in the middle of the track. On top
	 * of the track rearrangement, that is dealt with elsewhere in the model 
	 * class, this spot removal also triggers 2 edges removal: the links S2-S3 and S3-S4
	 * disappear. The only way for the {@link TrackGraphModel} to be aware of that, 
	 * and to forward these events to its listener, is to listen itself to the
	 * {@link #graph} that store links. 
	 * <p>
	 * This is done through this class. This class is notified every time a change occur in 
	 * the {@link #graph}:
	 * <ul>
	 * 	<li>It ignores events triggered by spots being added
	 * or removed, because they can't be triggered automatically, and are dealt with 
	 * in the {@link TrackGraphModel#addSpotTo(Spot, Integer)} and 
	 * {@link TrackGraphModel#removeSpotFrom(Spot, Integer)} methods.
	 * 	<li> It catches all events triggered by a link being added or removed in the graph,
	 * whether they are triggered manually through a call to a model method such as 
	 * {@link TrackGraphModel#addEdge(Spot, Spot, double)}, or triggered by another call.
	 * They are used to build the {@link TrackGraphModel#edgesAdded} and {@link TrackGraphModel#edgesRemoved}
	 * fields, that will be used to notify listeners of the model.    
	 * 
	 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Aug 12, 2011
	 *
	 */
	private class MyGraphListener implements GraphListener<Spot, DefaultWeightedEdge> {

		@Override
		public void vertexAdded(GraphVertexChangeEvent<Spot> e) {}

		@Override
		public void vertexRemoved(GraphVertexChangeEvent<Spot> e) {}

		@Override
		public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
			edgesAdded.add(e.getEdge());
		}

		@Override
		public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
			edgesRemoved.add(e.getEdge());
		}

	}



}
