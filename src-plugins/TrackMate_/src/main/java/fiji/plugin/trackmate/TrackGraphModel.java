package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.AsUnweightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.ClosestFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * A component of {@link TrackMateModel} specialized for tracks
 * @author Jean-Yves Tinevez
 */
public class TrackGraphModel {

	private static final boolean DEBUG = false;

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
	
	/*
	 * BULK TRACKS MODIFICATION
	 */
	


	/**
	 * Set the graph resulting from the tracking process, and fire a
	 * {@link TrackMateModelChangeEvent#TRACKS_COMPUTED} event. The {@link #filteredTrackKeys}
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
		final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.TRACKS_COMPUTED);
		for (TrackMateModelChangeListener listener : model.modelChangeListeners)
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
	

	public boolean addSpot(Spot spotToAdd) {
		return graph.addVertex(spotToAdd);
	}

	public boolean removeSpot(Spot spotToRemove) {
		return graph.removeVertex(spotToRemove);
		
	}
	
	public DefaultWeightedEdge addEdge(final Spot source, final Spot target, final double weight) {
		// Mother graph
		DefaultWeightedEdge edge = graph.addEdge(source, target);
		graph.setEdgeWeight(edge, weight);
		if (DEBUG)
			System.out.println("[TrackGraphModel] Adding edge between " + source + " and " + target + " with weight " + weight);
		return edge;
	}

	public DefaultWeightedEdge removeEdge(final Spot source, final Spot target) {
		// Other graph
		DefaultWeightedEdge edge = graph.removeEdge(source, target);
		if (DEBUG)
			System.out.println("[TrackGraphModel] Removing edge between " + source + " and " + target);
		return edge;
	}

	public boolean removeEdge(final DefaultWeightedEdge edge) {
		// Mother graph
		boolean removed = graph.removeEdge(edge);
		model.edgeSelection.remove(edge);
		if (DEBUG)
			System.out.println("[TrackGraphModel] Removing edge " + edge + " between " + graph.getEdgeSource(edge) + " and " + graph.getEdgeTarget(edge));
		return removed;
	}

	/*
	 * TRACK FILTERING
	 */

	/**
	 * Overwrite the {@link #filteredTrackKeys} field, resulting normally from the 
	 * {@link #execTrackFiltering()} process.
	 * 
	 * @param doNotify if true, will fire a {@link TrackMateModelChangeEvent#TRACKS_VISIBILITY_CHANGED} 
	 * event.
	 */
	public void setFilteredTrackIDs(Set<Integer> visibleTrackIndices, boolean doNotify) {
		this.filteredTrackKeys = visibleTrackIndices;
		if (doNotify) {
			final TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.TRACKS_VISIBILITY_CHANGED);
			for (TrackMateModelChangeListener listener : model.modelChangeListeners)
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
	 * {@link TrackMateModelChangeEvent#TRACKS_VISIBILITY_CHANGED} will be fired.
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
			TrackMateModelChangeEvent event = new TrackMateModelChangeEvent(this, TrackMateModelChangeEvent.TRACKS_VISIBILITY_CHANGED);
			for (TrackMateModelChangeListener listener : model.modelChangeListeners) 
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
	 * <p>
	 * <b>Note:</b> the actual {@link Set} object return by this method is 
	 * re-instantiated every time a change is made to the model that affects
	 * tracks. So this method needs to be called again after each change for
	 * the indices to be accurate.
	 * 
	 * @see #execTrackFiltering()
	 */
	public Set<Integer> getFilteredTrackIDs() {
		return filteredTrackKeys;
	}
	
	/**
	 * @return the set of integer keys for all the tracks contained in this model.
	 * These keys can then be used to iterate or retrieve individual tracks using
	 * {@link #getTrackEdges(int)}, {@link #getTrackSpots(int)}, etc...
	 * <p>
	 * <b>Note:</b> a track ID is just the {@link Object#hashCode()} of the track 
	 * as a {@link Set<Spot>}.
	 * <p>
	 * <b>Note:</b> the actual {@link Set} object return by this method is 
	 * re-instantiated every time a change is made to the model that affects
	 * tracks. So this method needs to be called again after each change for
	 * the indices to be accurate.
	 */
	public Set<Integer> getTrackIDs() {
		return trackSpots.keySet();
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
	 * touching the specified spot returns an empty set.
	 */
	public Set<DefaultWeightedEdge> edgesOf(final Spot spot) {
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
	 * @return a new undirected depth first iterator over the spots connected by links in this model.
	 * The returned iterator does not take into account edge direction, and will be able to iterate 
	 * backward in time if such links are met.
	 * @param start  the spot to start iteration with. Can be <code>null</code>, then the start will be taken
	 * randomly and will traverse all the links.
	 */
	public DepthFirstIterator<Spot, DefaultWeightedEdge> getUndirectedDepthFirstIterator(Spot start) {
		return new DepthFirstIterator<Spot, DefaultWeightedEdge>(new AsUndirectedGraph<Spot, DefaultWeightedEdge>(graph), start);
	}

	/**
	 * @return a new depth first iterator over the spots connected by links in this model.
	 * The returned iterator does take into account the edge direction, and will not be able 
	 * to iterate backward in time.
	 * @param start  the spot to start iteration with. Can be <code>null</code>, then the start will be taken
	 * randomly and will traverse all the links.
	 */
	public DepthFirstIterator<Spot, DefaultWeightedEdge> getDepthFirstIterator(Spot start) {
		return new DepthFirstIterator<Spot, DefaultWeightedEdge>(graph, start);
	}
	
	public BreadthFirstIterator<Spot, DefaultWeightedEdge> getBreadthFirstIterator(Spot start) {
		return new BreadthFirstIterator<Spot, DefaultWeightedEdge>(graph, start);
	}
	
	public BreadthFirstIterator<Spot, DefaultWeightedEdge> getUndirectedBreadthFirstIterator(Spot start) {
		return new BreadthFirstIterator<Spot, DefaultWeightedEdge>(new AsUndirectedGraph<Spot, DefaultWeightedEdge>(graph), start);
	}
	
	public ClosestFirstIterator<Spot, DefaultWeightedEdge> getUndirectedClosestFirstIterator(Spot start) {
		return new ClosestFirstIterator<Spot, DefaultWeightedEdge>(new AsUndirectedGraph<Spot, DefaultWeightedEdge>(graph), start);
	}
	
	public String trackToString(Integer trackID) {
		String str = "Track " + trackID + ": ";
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
	 */
	void computeTracksFromGraph() {
		
		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph()");
		}
		
		// Retain old values
		Map<Integer, Set<Spot>> oldTrackSpots = trackSpots;
		
		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph(): storing " + oldTrackSpots.size() + " old spot tracks.");
		}

		// Build new track lists
		List<Set<Spot>> connectedSets = new ConnectivityInspector<Spot, DefaultWeightedEdge>(graph).connectedSets();
		this.trackSpots = new HashMap<Integer, Set<Spot>>(connectedSets.size());
		this.trackEdges = new HashMap<Integer, Set<DefaultWeightedEdge>>(connectedSets.size());
		
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
		final Set<Integer> oldTrackVisibility = filteredTrackKeys;
		filteredTrackKeys = new HashSet<Integer>(noldtracks); // Approx

		/* Deal with a special case: of there were no tracks at all before this call,
		 * then oldTrackSpots is empty. To avoid that, we set it to the new value. Also,
		 * since the the visibility set is empty, we will not get any new track visible.
		 * So we seed it with all track indices, letting it propagate to new tracks. 
		 * So that manually added track will have a visibility to on. */
		if (oldTrackSpots.isEmpty()) {
			oldTrackSpots = trackSpots;
			for (int trackKey : trackSpots.keySet()) {
				filteredTrackKeys.add(trackKey);
			}
		}

		/* Another special case: if there is some completely new track appearing, it 
		 * should be visible by default. How do we know that some tracks are "de novo"?
		 * For de novo tracks, there is no spot in the Set<Spot> that can be found in 
		 * oldTrackSpots. Also, we want to avoid having visible tracks of 1 spot, so
		 * to be visible, de novo tracks must have more than 2 spots. 	 */

		// Pool all old spots together
		List<Spot> allSpotsInOldTrackSpots = new ArrayList<Spot>();
		for(Set<Spot> olTrack : oldTrackSpots.values()) {
			allSpotsInOldTrackSpots.addAll(olTrack);
		}

		// Interrogate each new track one by one
		for (Integer trackKey : trackSpots.keySet()) {

			Set<Spot> track = trackSpots.get(trackKey);

			boolean shouldBeVisible = true;
			for (final Spot spot : track) {
				if (allSpotsInOldTrackSpots.contains(spot)) {
					// At least one spot in the new track can be found in the old track list, so
					// it cannot be a de novo track. 
					shouldBeVisible = false;
					break;
				}
			}

			if (shouldBeVisible) {
				filteredTrackKeys.add(trackKey);
			}

		}

		// How to know if a new track should be visible or not?
		// We can say this: the new track should be visible if it has at least
		// one spot that can be found in a visible old track.
		for (int trackKey : trackSpots.keySet()) {

			boolean shouldBeVisible = false;
			for (final Spot spot : trackSpots.get(trackKey)) {

				for (Integer oldTrackIndex : oldTrackVisibility) { // we iterate over only old VISIBLE tracks
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
				filteredTrackKeys.add(trackKey);
			}
		}
		if (DEBUG) {
			System.out.println("[TrackGraphModel] #computeTracksFromGraph(): the end; found " + trackSpots.size() + " new spot tracks.");
		}
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
