package fiji.plugin.trackmate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.VertexFactory;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.AsUnweightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedGraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import fiji.plugin.trackmate.graph.Function1;
import fiji.plugin.trackmate.graph.SortedDepthFirstIterator;
import fiji.plugin.trackmate.graph.TimeDirectedDepthFirstIterator;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;
import fiji.plugin.trackmate.graph.TimeDirectedSortedDepthFirstIterator;
import fiji.plugin.trackmate.util.AlphanumComparator;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * A component of {@link Model} specialized for tracks
 * @author Jean-Yves Tinevez
 */
public class TrackModel {

	/**
	 * The mother graph, from which all subsequent fields are calculated. This
	 * graph is not made accessible to the outside world. Editing it must be
	 * trough the model methods {@link #addEdge(Spot, Spot, double)},
	 * {@link #removeEdge(DefaultWeightedEdge)}, {@link #removeEdge(Spot, Spot)}
	 * .
	 */
	private ListenableUndirectedGraph<Spot,DefaultWeightedEdge> graph;
	private final MyGraphListener mgl;

	/*
	 * TRANSACTION FIELDS
	 */

	/**
	 * The edges that have been added to this model by {@link #addEdge(Spot, Spot, double)}.
	 * <p>It is the parent instance responsibility to clear this field when it is fit to do so. 
	 */
	final Set<DefaultWeightedEdge> edgesAdded = new HashSet<DefaultWeightedEdge>();
	/**
	 * The edges that have removed from this model by {@link #removeEdge(DefaultWeightedEdge)} or
	 * {@link #removeEdge(Spot, Spot)}.
	 * <p>It is the parent instance responsibility to clear this field when it is fit to do so. 
	 */
	final Set<DefaultWeightedEdge> edgesRemoved = new HashSet<DefaultWeightedEdge>();
	/**
	 * The edges that have been modified in this model by changing its cost using 
	 * {@link #setEdgeWeight(DefaultWeightedEdge, double)} or modifying the spots it links
	 * elsewhere.
	 * <p>It is the parent instance responsibility to clear this field when it is fit to do so. 
	 */
	final Set<DefaultWeightedEdge> edgesModified = new HashSet<DefaultWeightedEdge>();
	/**
	 * The track IDs that have been modified, updated or created, <b>solely</b> by removing or adding
	 * an edge. Possibly after the removal of a spot. Tracks having edges that are <b>modified</b>,
	 * for instance by modifying a spot it contains, will not be listed here, but must be sought
	 * from the {@link #edgesModified} field.
	 * <p>
	 * It is the parent instance responsibility to clear this field when it is fit to do so. 
	 */
	final Set<Integer> tracksUpdated = new HashSet<Integer>();

	private static final Boolean DEFAULT_VISIBILITY = Boolean.TRUE;

	//~ Instance fields --------------------------------------------------------

	private int IDcounter = 0;
	private Map<Integer, Set<DefaultWeightedEdge>> connectedEdgeSets;
	 Map<DefaultWeightedEdge, Integer> edgeToID;
	private Map<Integer, Set<Spot>> connectedVertexSets;
	 Map<Spot, Integer> vertexToID;
	private Map<Integer, Boolean> visibility;
	private Map<Integer, String> names;
	private final Iterator<String> nameGenerator = new DefaultNameGenerator();



	/*
	 * Constructors -----------------------------------------------------------
	 */
	
	TrackModel() {
		this(new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class));
	}

	private TrackModel(SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph) {
		this.mgl = new MyGraphListener();
		setGraph(graph);
	}
	
	
	/*
	 * CREATION METHODS
	 * Methods that wipes the current content and replace it in bulk.
	 */
	
	/**
	 * Clear the content of this model and replace it by the tracks found by inspecting 
	 * the specified graph. All new tracks found will be made visible and will be given
	 * a default name.
	 * @param graph  the graph to parse for tracks.
	 */
	public void setGraph(SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph) {
		if (null != this.graph) {
			this.graph.removeGraphListener(mgl);
		}
		this.graph = new ListenableUndirectedGraph<Spot, DefaultWeightedEdge>(graph);
		this.graph.addGraphListener(mgl);
		init(graph);
	}
	
	/**
	 * This method is meant to help building a model from a serialized source, such as 
	 * a saved file. It allows specifying the exact mapping of track IDs to 
	 * the connected sets. The model content is completely replaced by the specified parameters, 
	 * including the global graph, its connected components (both in spots and edges), 
	 * visibility and naming.
	 * <p>
	 * It is the caller responsibility to ensure that the graph and provided component
	 * are coherent. Unexpected behavior might result otherwise.
	 * @param graph the mother graph for the model.
	 * @param trackSpots  the mapping of track IDs vs the connected components as sets of spots.
	 * @param trackEdges the mapping of track IDs vs the connected components as sets of edges.
	 * @param trackVisibility  the track visibility.
	 * @param trackNames the track names.
	 */
	public void from(SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph, 
			Map<Integer, Set<Spot>> trackSpots,
			Map<Integer, Set<DefaultWeightedEdge>> trackEdges,
			Map<Integer, Boolean> trackVisibility,
			Map<Integer, String> trackNames) {
		
		if (null != this.graph) {
			this.graph.removeGraphListener(mgl);
		}
		this.graph = new ListenableUndirectedGraph<Spot, DefaultWeightedEdge>(graph);
		this.graph.addGraphListener(mgl);
		
		edgesAdded.clear();
		edgesModified.clear();
		edgesRemoved.clear();
		tracksUpdated.clear();

		visibility = trackVisibility;
		names = trackNames;
		connectedVertexSets = trackSpots;
		connectedEdgeSets = trackEdges;
		
		// Rebuild the id maps
		IDcounter = 0;
		vertexToID = new HashMap<Spot, Integer>();
		for (Integer id : trackSpots.keySet()) {
			for (Spot spot : trackSpots.get(id)) {
				vertexToID.put(spot, id);
			}
			if (id > IDcounter) {
				IDcounter = id;
			}
		}
		IDcounter++;
		
		edgeToID = new HashMap<DefaultWeightedEdge, Integer>();
		for (Integer id : trackEdges.keySet()) {
			for (DefaultWeightedEdge edge : trackEdges.get(id)) {
				edgeToID.put(edge, id);
			}
		}
		
	}
	
	
	/*
	 * DEFAULT VISIBILIT METHODS
	 * made to be called from the mother model.
	 */
	
	void addSpot(Spot spotToAdd) {
		graph.addVertex(spotToAdd);
	}

	void removeSpot(Spot spotToRemove) {
		graph.removeVertex(spotToRemove);
	}
	
	DefaultWeightedEdge addEdge(Spot source, Spot target, double weight) {
		if (!graph.containsVertex(source)) {
			graph.addVertex(source);
		}
		if (!graph.containsVertex(target)) {
			graph.addVertex(target);
		}
		DefaultWeightedEdge edge = graph.addEdge(source, target);
		graph.setEdgeWeight(edge, weight);
		return edge;
	}
	
	DefaultWeightedEdge removeEdge(Spot source, Spot target) {
		return graph.removeEdge(source, target);
	}
	
	boolean removeEdge(DefaultWeightedEdge edge) {
		return graph.removeEdge(edge);
	}

	void setEdgeWeight(DefaultWeightedEdge edge, double weight) {
		graph.setEdgeWeight(edge, weight);
		edgesModified.add(edge);
	}
	
	Boolean setVisibility(Integer trackID, boolean visible) {
		return visibility.put(trackID, Boolean.valueOf(visible));
	}

	
	/*
	 * PUBLIC METHODS
	 */
	
	
	/*
	 * GRAPH
	 */
	
	/**
	 * Returns a new graph with the same structure as the one wrapped here, and with vertices generated
	 * by the given {@link Function1}. Edges are copied in direction and weight.
	 * @param factory the vertex factory used to instantiate new vertices in the new graph
	 * @param function the function used to set values of a new vertex in the new graph, from the matching spot
	 * @param mappings a map that will receive mappings from {@link Spot} to the new vertices. Can be <code>null</code>
	 * if you do not want to get the mappings
	 * @return a new {@link SimpleDirectedWeightedGraph}.
	 */
	public <V> SimpleDirectedWeightedGraph<V, DefaultWeightedEdge> copy(VertexFactory<V> factory, Function1<Spot, V> function, Map<Spot, V> mappings) {
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
	
	/**
	 * @see Graph#containsEdge(Object, Object)
	 */
	public boolean containsEdge(Spot source, Spot target) {
		return graph.containsEdge(source, target);
	}
	
	/**
	 * @see Graph#getEdge(Object, Object)
	 */
	public DefaultWeightedEdge getEdge(Spot source, Spot target) {
		return graph.getEdge(source, target);
	}
	
	/**
	 * @see Graph#edgesOf(Object)
	 */
	public Set<DefaultWeightedEdge> edgesOf(Spot spot) {
		return graph.edgesOf(spot);
	}
	
	/**
	 * @see Graph#edgeSet()
	 */
	public Set<DefaultWeightedEdge> edgeSet() {
		return graph.edgeSet();
	}

	/**
	 * @see Graph#vertexSet()
	 */
	public Set<Spot> vertexSet() {
		return graph.vertexSet();
	}

	
	/**
	 * @see Graph#getEdgeSource(Object)
	 */
	public Spot getEdgeSource(DefaultWeightedEdge e) {
		return graph.getEdgeSource(e);
	}
	
	/**
	 * @see Graph#getEdgeTarget(Object)
	 */
	public Spot getEdgeTarget(DefaultWeightedEdge e) {
		return graph.getEdgeTarget(e);
	}
	
	/**
	 * @see Graph#getEdgeWeight(Object)
	 */
	public double getEdgeWeight(DefaultWeightedEdge edge) {
		return graph.getEdgeWeight(edge);
	}

	
	/*
	 * TRACKS
	 */
	
	/**
	 * Returns <code>true</code> if the track with the specified ID is marked as visible, 
	 * <code>false</code> otherwise. Throws a {@link NullPointerException} if the track
	 * ID is unknown to this model.
	 * @param ID  the track ID.
	 * @return the track visibility.
	 */
	public boolean isVisible(Integer ID) {
		return visibility.get(ID);
	}

	
	/**
	 * Returns the set of track IDs managed by this model, ordered
	 * by track names (alpha-numerically sorted).
	 * @param visibleOnly  if <code>true</code>, only visible track IDs will be returned.
	 * @return a new set of track IDs.
	 */
	public Set<Integer> trackIDs(boolean visibleOnly) {
		Set<Integer> ids = TMUtils.sortByValue(names, AlphanumComparator.instance).keySet();
		if (!visibleOnly) {
			return ids;
		} else {
			Set<Integer> vids = new HashSet<Integer>(ids.size());
			for (Integer id : ids) {
				if (visibility.get(id)) {
					vids.add(id);
				}
			}
			return vids;
		}
	}
	
	/**
	 * Returns the name of the track with the specified ID.
	 * @param id  the track ID.
	 * @return  the track name.
	 */
	public String name(Integer id) {
		return names.get(id);
	}

	/**
	 * Sets the name of the track with the specified ID.
	 * @param id  the track ID.
	 * @param name  the name for the track.
	 */
	public void setName(Integer id, String name) {
		names.put(id, name);
	}


	
	/**
	 * Returns the edges of the track with the specified ID.
	 * @param trackID  the track ID.
	 * @return the set of edges.
	 */
	public Set<DefaultWeightedEdge> trackEdges(Integer trackID) {
		return connectedEdgeSets.get(trackID);
	}
	
	/**
	 * Returns the spots of the track with the specified ID.
	 * @param trackID  the track ID.
	 * @return the set of spots.
	 */
	public Set<Spot> trackSpots(Integer trackID) {
		return connectedVertexSets.get(trackID);
	}
	
	public int nTracks(boolean visibleOnly) {
		if (!visibleOnly) {
			return connectedEdgeSets.size();
		} else {
			int ntracks = 0;
			for (Boolean visible : visibility.values()) {
				if (visible) {
					ntracks++;
				}
			}
			return ntracks;
		}
	}

	/**
	 * Returns the track ID the specified edge belong to, or <code>null</code>
	 * if the specified edge cannot be found in this model.
	 * @param edge  the edge to search for.
	 * @return  the track ID it belongs to.
	 */
	public Integer trackIDOf(DefaultWeightedEdge edge) {
		return edgeToID.get(edge);
	}
	
	/**
	 * Returns the track ID the specified spot belong to, or <code>null</code>
	 * if the specified spot cannot be found in this model.
	 * @param edge  the spot to search for.
	 * @return  the track ID it belongs to.
	 */
	public Integer trackIDOf(Spot spot) {
		return vertexToID.get(spot);
	}



	/*
	 * PRIVATE METHODS
	 */


	/**
	 * Generates initial connected sets in bulk, from a graph.
	 * All sets are created visible, and are give a default name.
	 */
	private void init(UndirectedGraph<Spot, DefaultWeightedEdge> graph) {
		vertexToID = new HashMap<Spot, Integer>();
		edgeToID = new HashMap<DefaultWeightedEdge, Integer>();
		IDcounter = 0;
		visibility = new HashMap<Integer, Boolean>();
		names = new HashMap<Integer, String>();
		connectedVertexSets = new HashMap<Integer, Set<Spot>>();
		connectedEdgeSets = new HashMap<Integer, Set<DefaultWeightedEdge>>();
		
		edgesAdded.clear();
		edgesModified.clear();
		edgesRemoved.clear();
		tracksUpdated.clear();

		Set<Spot> vertexSet = graph.vertexSet();
		if (vertexSet.size() > 0) {
			BreadthFirstIterator<Spot, DefaultWeightedEdge> i = new BreadthFirstIterator<Spot, DefaultWeightedEdge>(graph, null);
			i.addTraversalListener(new MyTraversalListener());

			while (i.hasNext()) {
				i.next();
			}
		}
	}
	
	

	
	/*
	 * UTILS
	 */
		
	public String echo() {
		if (null == connectedVertexSets) {
			return "Uninitialized.\n";
		}
		
		StringBuilder str = new StringBuilder();
		Set<Integer> vid = connectedVertexSets.keySet();
		HashSet<Integer> eid = new HashSet<Integer>(connectedEdgeSets.keySet());
		
		for (Integer id : vid) {
			str.append(id + ":\n");
			str.append(" - " + connectedVertexSets.get(id) + "\n");
			Set<DefaultWeightedEdge> es = connectedEdgeSets.get(id);
			if (es == null) {
				str.append(" - no matching edges!\n");
			} else {
				str.append(" - " + es + "\n");
			}
			eid.remove(id);
		}
		
		if (eid.isEmpty()) {
			str.append("No remaining edges ID.\n");
		} else {
			str.append("Found non-matching edge IDs!\n");
			for (Integer id : eid) {
				str.append(id + ":\n");
				str.append(" - " + connectedEdgeSets.get(id) + "\n");
			}
		}
		
		return str.toString();
	}
	
	
	/*
	 * ITERATORS
	 */
	
	
	/**
	 * Returns a new depth first iterator over the spots connected by links in this model.
	 * A boolean flag allow to set whether the returned iterator does take into account 
	 * the edge direction. If true, the iterator will not be able to iterate backward in time.
	 * @param start  the spot to start iteration with. Can be <code>null</code>, then the start will be taken
	 * randomly and will traverse all the links.
	 * @param directed  if true returns a directed iterator, undirected if false.
	 */
	public GraphIterator<Spot, DefaultWeightedEdge> getDepthFirstIterator(Spot start, boolean directed) {
		if (directed) {
			return new TimeDirectedDepthFirstIterator(graph, start);
		} else {
			return new DepthFirstIterator<Spot, DefaultWeightedEdge>(graph, start);			
		}
	}

	/**
	 * Returns a new depth first iterator over the spots connected by links in this model.
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
			return new TimeDirectedSortedDepthFirstIterator(graph, start, comparator);
		} else {
			return new SortedDepthFirstIterator<Spot, DefaultWeightedEdge>(graph, start, comparator);			
		}
	}

	public TimeDirectedNeighborIndex getDirectedNeighborIndex() {
		return new TimeDirectedNeighborIndex(graph);
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
	 *  Inner Classes 
	 */

	private class MyTraversalListener implements TraversalListener<Spot, DefaultWeightedEdge> {
		private Set<Spot> currentConnectedVertexSet;
		private Set<DefaultWeightedEdge> currentConnectedEdgeSet;
		private Integer ID;

		/**
		 * Called when after traversing a connected set. Stores it, gives
		 * it default visibility, and a default name. 
		 * Discard sets made of 1 vertices or 0 edges.
		 */
		@Override
		public void connectedComponentFinished(ConnectedComponentTraversalEvent event) {
			if (currentConnectedVertexSet.size() <= 1 || currentConnectedEdgeSet.size() == 0) {
				// Forget them
				for (DefaultWeightedEdge e : currentConnectedEdgeSet) {
					edgeToID.remove(e);
				}
				for (Spot v : currentConnectedVertexSet) {
					vertexToID.remove(v);
				}
				return;
			}
			// Adds them
			connectedVertexSets.put(ID, currentConnectedVertexSet);
			connectedEdgeSets.put(ID, currentConnectedEdgeSet);
			visibility.put(ID, DEFAULT_VISIBILITY);
			names.put(ID, nameGenerator.next());
		}

		/**
		 * @see TraversalListenerAdapter#connectedComponentStarted(ConnectedComponentTraversalEvent)
		 */
		@Override
		public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			currentConnectedVertexSet = new HashSet<Spot>();
			currentConnectedEdgeSet = new HashSet<DefaultWeightedEdge>();
			ID = IDcounter++;
		}

		/**
		 * @see TraversalListenerAdapter#vertexTraversed(VertexTraversalEvent)
		 */
		@Override
		public void vertexTraversed(VertexTraversalEvent<Spot> event) {
			Spot v = event.getVertex();
			currentConnectedVertexSet.add(v);
			vertexToID.put(v, ID);
		}
		
		@Override
		public void edgeTraversed(EdgeTraversalEvent<Spot, DefaultWeightedEdge> event) {
			DefaultWeightedEdge e = event.getEdge();
			currentConnectedEdgeSet.add(e);
			edgeToID.put(e, ID);
		}

		@Override
		public void vertexFinished(VertexTraversalEvent<Spot> e) { }
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
	 * disappear. The only way for the {@link TrackModel} to be aware of that, 
	 * and to forward these events to its listener, is to listen itself to the
	 * {@link #graph} that store links. 
	 * <p>
	 * This is done through this class. This class is notified every time a change occur in 
	 * the {@link #graph}:
	 * <ul>
	 * 	<li>It ignores events triggered by spots being added
	 * or removed, because they can't be triggered automatically, and are dealt with 
	 * in the {@link TrackModel#addSpotTo(Spot, Integer)} and 
	 * {@link TrackModel#removeSpot(Spot, Integer)} methods.
	 * 	<li> It catches all events triggered by a link being added or removed in the graph,
	 * whether they are triggered manually through a call to a model method such as 
	 * {@link TrackModel#addEdge(Spot, Spot, double)}, or triggered by another call.
	 * They are used to build the {@link TrackModel#edgesAdded} and {@link TrackModel#edgesRemoved}
	 * fields, that will be used to notify listeners of the model.    
	 * 
	 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Aug 12, 2011
	 *
	 */
	private class MyGraphListener implements GraphListener<Spot, DefaultWeightedEdge> {

		@Override
		public void vertexAdded(GraphVertexChangeEvent<Spot> event) {}

		@Override
		public void vertexRemoved(GraphVertexChangeEvent<Spot> event) {
			if (null == connectedEdgeSets) {
				return;
			}

			Spot v = event.getVertex();
			vertexToID.remove(v);
			Integer id = vertexToID.get(v);
			if (id != null) {
				Set<Spot> set = connectedVertexSets.get(id);
				if (null == set) {
					return; // it was removed when removing the last edge of a track, most likely.
				}
				set.remove(v);

				if (set.isEmpty()) {
					connectedEdgeSets.remove(id);
					connectedVertexSets.remove(id);
					names.remove(id);
					visibility.remove(id);
				}
			}
		}

		@Override
		public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> event) {
			// To signal to ModelChangeListener
			edgesAdded.add(event.getEdge());
			
			// To maintain connected sets coherence:
			/*
			 * This is the tricky part: when we add an edge to our set model,
			 * first we need to find to what existing set it has been added.
			 * Then a new edge sometime come with 1 or 2 vertices that might
			 * be new or belonging to an existing set.
			 */
			DefaultWeightedEdge e = event.getEdge();
			
			// Was it added to known tracks?
			Spot sv = graph.getEdgeSource(e);
			Integer sid = vertexToID.get(sv);
			Spot tv = graph.getEdgeTarget(e);
			Integer tid = vertexToID.get(tv);
			
			if (null != tid && null != sid) {
				// Case 1: it was added between two existing sets. We connect them, therefore
				// and take the id of the largest one. The other id, disappear.
				
				// Vertices:
				Set<Spot> svs = connectedVertexSets.get(sid);
				Set<Spot> tvs = connectedVertexSets.get(tid);
				HashSet<Spot> nvs = new HashSet<Spot>( svs.size() + tvs.size() );
				nvs.addAll(svs);
				nvs.addAll(tvs);
				
				// Edges:
				Set<DefaultWeightedEdge> ses = connectedEdgeSets.get(sid);
				Set<DefaultWeightedEdge> tes = connectedEdgeSets.get(tid);
				HashSet<DefaultWeightedEdge> nes = new HashSet<DefaultWeightedEdge>(ses.size() + tes.size() + 1);
				nes.addAll(ses);
				nes.addAll(tes);
				nes.add(e);
				
				// ID
				Integer nid, rid;
				if (nvs.size() > tvs.size()) {
					nid = sid;
					rid = tid;
					for (Spot v : tvs) {
						// Vertices of target set change id
						vertexToID.put(v, nid);
					}
					for (DefaultWeightedEdge te : tes) {
						edgeToID.put(te, nid);
					}
				} else {
					nid = tid;
					rid = sid;
					for (Spot v : svs) {
						// Vertices of source set change id
						vertexToID.put(v, nid);
					}
					for (DefaultWeightedEdge se : ses) {
						edgeToID.put(se, nid);
					}
				}
				edgeToID.put(e, nid);
				connectedVertexSets.put(nid, nvs);
				connectedVertexSets.remove(rid);
				connectedEdgeSets.put(nid, nes);
				connectedEdgeSets.remove(rid);
				
				// Transaction: we signal that the large id is to be updated, and forget about the small one
				tracksUpdated.add(nid);
				tracksUpdated.remove(rid);
				
				// Visibility: if at least one is visible, the new set is made visible.
				Boolean targetVisibility = visibility.get(sid) || visibility.get(tid);
				visibility.put(nid, targetVisibility);
				visibility.remove(rid);
				
				// Name: the new set gets the name of the largest one.
				names.remove(rid); // 'nid' already has the right name.
				
			} else if (null == sid && null == tid) {
				// Case 4: the edge was added between two lonely vertices.
				// Create a new set id from this
				HashSet<Spot> nvs = new HashSet<Spot>(2);
				nvs.add(graph.getEdgeSource(e));
				nvs.add(graph.getEdgeTarget(e));
				
				HashSet<DefaultWeightedEdge> nes = new HashSet<DefaultWeightedEdge>(1);
				nes.add(e);
				
				int nid = IDcounter++;
				connectedEdgeSets.put(nid, nes);
				connectedVertexSets.put(nid, nvs);
				vertexToID.put(sv, nid);
				vertexToID.put(tv, nid);
				edgeToID.put(e, nid);
				
				// Give it visibility
				visibility.put(nid, Boolean.TRUE);
				// and a default name.
				names.put(nid, nameGenerator.next());
				// Transaction: we mark the new track as updated
				tracksUpdated.add(nid);
				
			} else if (null == sid) {
				// Case 2: the edge was added to the target set. No source set, but there is a source vertex.
				// Add it, with the source vertex, to the target id.
				connectedEdgeSets.get(tid).add(e);
				edgeToID.put(e, tid);
				connectedVertexSets.get(tid).add(sv);
				vertexToID.put(sv, tid);
				// We do not change the visibility, nor the name.
				// Transaction: we mark the mother track as updated
				tracksUpdated.add(tid);
				
			} else if (null == tid) {
				// Case 3: the edge was added to the source set. No target set, but there is a target vertex.
				// Add it, with the target vertex, to the source id.
				connectedEdgeSets.get(sid).add(e);
				edgeToID.put(e, sid);
				connectedVertexSets.get(sid).add(tv);
				vertexToID.put(tv, sid);
				// We do not change the visibility, nor the name.
				// Transaction: we mark the mother track as updated
				tracksUpdated.add(sid);
				
			} 
				
			
		}

		@Override
		public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> event) {
			// To signal to ModelChangeListeners
			edgesRemoved.add(event.getEdge());
			
			// To maintain connected sets coherence

			DefaultWeightedEdge e = event.getEdge();
			Integer id = edgeToID.get(e);
			if (null == id) {
				throw new RuntimeException("Edge is unkown to this model: " + e);
			}
			Set<DefaultWeightedEdge> set = connectedEdgeSets.get(id);
			if (null == set) {
				throw new RuntimeException("Unknown set ID: " + id);
			}
			
			// Remove edge from set.
			boolean removed = set.remove(e);
			if (!removed) {
				throw new RuntimeException("Could not removed edge " + e + " from set with ID: " + id);
			}
			// Forget about edge.
			edgeToID.remove(e);
			
			/*
			 * Ok the trouble is that now we might be left with 2 sets if the edge
			 * "was in the middle". Or 1 if it was in the end. Or 0 if it was the last 
			 * edge of the set.
			 */
			
			if (set.size() == 0) {
				// The set is empty, remove it from the map.
				connectedEdgeSets.remove(id); 
				names.remove(id);
				visibility.remove(id);
				/*  We need to remove also the vertices */
				Set<Spot> vertexSet = connectedVertexSets.get(id);
				// Forget the vertices were in a set
				for (Spot spot : vertexSet) {
					vertexToID.remove(spot);
				}
				// Forget the vertex set
				connectedVertexSets.remove(id);
				/* We do not mark it as a track to update, for it disappeared.
				 * On the other hand, it might *have been* marked as a track to update.
				 * Since it just joined oblivion, we remove it from the list of tracks
				 * to update. */
				tracksUpdated.remove(id);
				
			} else {
				// So there are some edges remaining in the set.
				// Look at the connected component of its source and target.
				// Source 
				HashSet<Spot> sourceVCS = new HashSet<Spot>();
				HashSet<DefaultWeightedEdge> sourceECS = new HashSet<DefaultWeightedEdge>();
				{
					Spot source = graph.getEdgeSource(e);
					// Get its connected set
					BreadthFirstIterator<Spot, DefaultWeightedEdge> i = new BreadthFirstIterator<Spot, DefaultWeightedEdge>(graph, source);
					while (i.hasNext()) {
						Spot sv = i.next();
						sourceVCS.add(sv);
						sourceECS.addAll(graph.edgesOf(sv));
					}
				}
				// Target
				HashSet<Spot> targetVCS = new HashSet<Spot>();
				HashSet<DefaultWeightedEdge> targetECS = new HashSet<DefaultWeightedEdge>();
				{
					Spot target = graph.getEdgeTarget(e);
					// Get its connected set
					BreadthFirstIterator<Spot, DefaultWeightedEdge> i = new BreadthFirstIterator<Spot, DefaultWeightedEdge>(graph, target);
					while (i.hasNext()) {
						Spot sv = i.next();
						targetVCS.add(sv);
						targetECS.addAll(graph.edgesOf(sv));
					}
				}
				/*
				 * Re-attribute the found connected sets to the model.
				 * The largest one (in vertices) gets the original id, the other
				 * gets a new id. 
				 * As for names: the largest one keeps its name, the small one
				 * gets a new name. 
				 */
				if (targetVCS.size() > sourceVCS.size()) {
					
					connectedEdgeSets.put(id, targetECS);
					connectedVertexSets.put(id, targetVCS); // they already have the right id in #vertexToId
					tracksUpdated.add(id); // old track has changed
					
					if (sourceECS.size() > 0) {
						// the smaller part is still a track
						int newid = IDcounter++;
						connectedEdgeSets.put(newid, sourceECS); // otherwise forget it
						for (DefaultWeightedEdge te : sourceECS) {
							edgeToID.put(te, newid);
						}
						connectedVertexSets.put(newid, sourceVCS); 
						for (Spot tv : sourceVCS) {
							vertexToID.put(tv, newid);
						}
						Boolean targetVisibility = visibility.get(id);
						visibility.put(newid, targetVisibility);
						names.put(newid, nameGenerator.next());
						// Transaction: both children tracks are marked for update.
						tracksUpdated.add(newid);
						
					} else {
						/* Nothing remains from the smallest part. The remaining
						 * solitary vertex has no right to be called a track. */
						Spot solitary = sourceVCS.iterator().next();
						vertexToID.remove(solitary);
					}
					
				} else {
					
					if (sourceECS.size() > 0) {
						// There are still some pieces left. It is worth noting it.
						connectedEdgeSets.put(id, sourceECS);
						connectedVertexSets.put(id, sourceVCS); 
						tracksUpdated.add(id);
						
						if (targetECS.size() > 0) {
							// the small part is still a track
							int newid = IDcounter++;
							connectedEdgeSets.put(newid, targetECS);
							for (DefaultWeightedEdge te : targetECS) {
								edgeToID.put(te, newid);
							}
							connectedVertexSets.put(newid, targetVCS);
							for (Spot v : targetVCS) {
								vertexToID.put(v, newid);
							}
							Boolean targetVisibility = visibility.get(id);
							visibility.put(newid, targetVisibility);
							names.put(newid, nameGenerator.next());
							// Transaction: both children tracks are marked for update.
							tracksUpdated.add(newid);
						} else {
							/* Nothing remains from the smallest part. The remaining
							 * solitary vertex has no right to be called a track. */
							Spot solitary = targetVCS.iterator().next();
							vertexToID.remove(solitary);
						}
						
					} else {
						// Nothing remains (maybe a solitary vertex) -> forget about it all.
						connectedEdgeSets.remove(id);
						connectedVertexSets.remove(id);
						names.remove(id);
						visibility.remove(id);
						tracksUpdated.remove(id);
					}
					
				}
			}
		}

	}



	
	private static class DefaultNameGenerator implements Iterator<String> {

		private int nameID = 0;
		
		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public String next() {
			return "Track_" + nameID++;
		}

		@Override
		public void remove() {}

	}


}
