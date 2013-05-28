package fiji.plugin.trackmate.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexSetListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;


/**
 * Allows obtaining various connectivity aspects of a graph. The <i>inspected
 * graph</i> is specified at construction time and cannot be modified.
 * Currently, the inspector supports connected components for an undirected
 * graph and weakly connected components for a directed graph. To find strongly
 * connected components, use {@link StrongConnectivityInspector} instead.
 *
 * <p>The inspector methods work in a lazy fashion: no computation is performed
 * unless immediately necessary. Computation are done once and results and
 * cached within this class for future need.</p>
 *
 * <p>The inspector is also a {@link org.jgrapht.event.GraphListener}. If added
 * as a listener to the inspected graph, the inspector will amend internal
 * cached results instead of recomputing them. It is efficient when a few
 * modifications are applied to a large graph. If many modifications are
 * expected it will not be efficient due to added overhead on graph update
 * operations. If inspector is added as listener to a graph other than the one
 * it inspects, results are undefined.</p>
 * 
 * <p>This is an updated version from Barak & John {@link ConnectivityInspector},
 * because I (JYT) needed changes to be amended, which was not done in their
 * class.
 *
 * @author Barak Naveh
 * @author John V. Sichi
 * @author Jean-Yves Tinevez
 * @since 2013
 */
public class IncrementalConnectivityInspector<V, E> implements GraphListener<V, E> {

	/** The source for track IDs. */
	private final AtomicInteger IDcounter = new AtomicInteger(0);
	
	//~ Instance fields --------------------------------------------------------

	private Map<Integer, Set<E>> connectedEdgeSets;
	private Map<E, Integer> edgeToID;
	private Map<Integer, Set<V>> connectedVertexSets;
	private Map<V, Integer> vertexToID;
	private Graph<V, E> graph;

	//~ Constructors -----------------------------------------------------------

	/**
	 * Creates a connectivity inspector for the specified undirected graph.
	 *
	 * @param g the graph for which a connectivity inspector to be created.
	 */
	public IncrementalConnectivityInspector(UndirectedGraph<V, E> g) {
		init();
		this.graph = g;
	}

	/**
	 * Creates a connectivity inspector for the specified directed graph.
	 *
	 * @param g the graph for which a connectivity inspector to be created.
	 */
	public IncrementalConnectivityInspector(DirectedGraph<V, E> g) {
		init();
		this.graph = new AsUndirectedGraph<V, E>(g);
	}

	//~ Methods ----------------------------------------------------------------

	/**
	 * Returns a list of <code>Set</code> s, where each set contains all
	 * vertices that are in the same maximally connected component. All graph
	 * vertices occur in exactly one set. For more on maximally connected
	 * component, see <a
	 * href="http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html">
	 * http://www.nist.gov/dads/HTML/maximallyConnectedComponent.html</a>.
	 *
	 * @return Returns a list of <code>Set</code> s, where each set contains all
	 * vertices that are in the same maximally connected component.
	 */
	public Map<Integer, Set<V>> connectedVertexSets() {
		lazyFindConnectedSets();
		return connectedVertexSets;
	}
	
	public Map<Integer, Set<E>> connectedEdgeSets() {
		lazyFindConnectedSets();
		return connectedEdgeSets;
	}
	
	/**
	 * @see GraphListener#edgeAdded(GraphEdgeChangeEvent)
	 */
	@Override
	public void edgeAdded(GraphEdgeChangeEvent<V, E> event) {
		/*
		 * This is the tricky part: when we add an edge to our set model,
		 * first we need to find to what existing set it has been added.
		 * Then a new edge sometime come with 1 or 2 vertices that might
		 * be new or belonging to an existing set.
		 */
		System.out.println(toString(event));//DEBUG 
		E e = event.getEdge();
		
		// Was it added to known tracks?
		V sv = graph.getEdgeSource(e);
		Integer sid = vertexToID.get(sv);
		V tv = graph.getEdgeTarget(e);
		Integer tid = vertexToID.get(tv);
		
		if (null != tid && null != sid) {
			// Case 1: it was added between two existing sets. We connect them, therefore
			// and take the id of the largest one. The other id, disappear.
			
			// Vertices:
			Set<V> svs = connectedVertexSets.get(sid);
			Set<V> tvs = connectedVertexSets.get(tid);
			HashSet<V> nvs = new HashSet<V>( svs.size() + tvs.size() );
			nvs.addAll(svs);
			nvs.addAll(tvs);
			
			// Edges:
			Set<E> ses = connectedEdgeSets.get(sid);
			Set<E> tes = connectedEdgeSets.get(tid);
			HashSet<E> nes = new HashSet<E>(ses.size() + tes.size() + 1);
			nes.addAll(ses);
			nes.addAll(tes);
			nes.add(e);
			
			// ID
			Integer nid, rid;
			if (nvs.size() > tvs.size()) {
				nid = sid;
				rid = tid;
				for (V v : tvs) {
					// Vertices of target set change id
					vertexToID.put(v, nid);
				}
				for (E te : tes) {
					edgeToID.put(te, nid);
				}
			} else {
				nid = tid;
				rid = sid;
				for (V v : svs) {
					// Vertices of source set change id
					vertexToID.put(v, nid);
				}
				for (E se : ses) {
					edgeToID.put(se, nid);
				}
			}
			edgeToID.put(e, nid);
			connectedVertexSets.put(nid, nvs);
			connectedVertexSets.remove(rid);
			connectedEdgeSets.put(nid, nes);
			connectedEdgeSets.remove(rid);
			
		} else if (null == sid) {
			// Case 2: the edge was added to the target set. No source set, but there is a source vertex.
			// Add it, with the source vertex, to the target id.
			connectedEdgeSets.get(tid).add(e);
			edgeToID.put(e, tid);
			connectedVertexSets.get(tid).add(sv);
			vertexToID.put(sv, tid);
			
		} else if (null == tid) {
			// Case 3: the edge was added to the source set. No target set, but there is a target vertex.
			// Add it, with the target vertex, to the source id.
			connectedEdgeSets.get(sid).add(e);
			edgeToID.put(e, sid);
			connectedVertexSets.get(sid).add(tv);
			vertexToID.put(tv, sid);
			
		} else {
			// Case 4: the edge was added between two lonely vertices.
			// Create a new set id from this
			HashSet<V> nvs = new HashSet<V>(2);
			nvs.add(graph.getEdgeSource(e));
			nvs.add(graph.getEdgeTarget(e));
			
			HashSet<E> nes = new HashSet<E>(1);
			nes.add(e);
			
			int nid = IDcounter.getAndIncrement();
			connectedEdgeSets.put(nid, nes);
			connectedVertexSets.put(nid, nvs);
			
		}
	}

	/**
	 * @see GraphListener#edgeRemoved(GraphEdgeChangeEvent)
	 */
	@Override
	public void edgeRemoved(GraphEdgeChangeEvent<V, E> event) {
		if (null == connectedEdgeSets) {
			return;
		}
		
		E e = event.getEdge();
		Integer id = edgeToID.get(e);
		if (null == id) {
			throw new RuntimeException("Edge is unkown to this model: " + e);
		}
		Set<E> set = connectedEdgeSets.get(id);
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
			connectedEdgeSets.remove(id); // we do not remove it from the vertex set, for vertices will be processed elsewhere
		} else {
			// So there are some edges remaining in the set.
			// Look at the connected component of its source and target.
			// Source
			HashSet<V> sourceVCS = new HashSet<V>();
			HashSet<E> sourceECS = new HashSet<E>();
			{
				V source = graph.getEdgeSource(e);
				if (source != null) {
					// Get its connected set
					BreadthFirstIterator<V, E> i = new BreadthFirstIterator<V, E>(graph, source);
					while (i.hasNext()) {
						V sv = i.next();
						sourceVCS.add(sv);
						sourceECS.addAll(graph.edgesOf(sv));
					}
				}
			}
			// Target
			HashSet<V> targetVCS = new HashSet<V>();
			HashSet<E> targetECS = new HashSet<E>();
			{
				V target = graph.getEdgeTarget(e);
				if (target != null) {
					// Get its connected set
					BreadthFirstIterator<V, E> i = new BreadthFirstIterator<V, E>(graph, target);
					while (i.hasNext()) {
						V sv = i.next();
						targetVCS.add(sv);
						targetECS.addAll(graph.edgesOf(sv));
					}
				}
			}
			// Re-attribute the found connected sets to the model.
			// The largest one (in vertices) gets the original id, the other
			// gets a new id.
			if (targetVCS.size() > sourceVCS.size()) {
				
				connectedEdgeSets.put(id, targetECS);
				connectedVertexSets.put(id, targetVCS); // they already have the right id in #vertexToId
				
				if (sourceVCS.size() > 0) {
					int newid = IDcounter.getAndIncrement();
					connectedEdgeSets.put(newid, sourceECS); // otherwise forget it
					for (E te : sourceECS) {
						edgeToID.put(te, newid);
					}
					connectedVertexSets.put(newid, sourceVCS); 
					for (V tv : sourceVCS) {
						vertexToID.put(tv, newid);
					}
				}
				
			} else {
				
				if (sourceVCS.size() > 0) {
					connectedEdgeSets.put(id, sourceECS);
					connectedVertexSets.put(id, sourceVCS); // otherwise forget it
				} else {
					// Nothing remains (maybe a solitary vertex) -> forget about it all.
					connectedEdgeSets.remove(id);
					connectedVertexSets.remove(id);
				}
				if (targetVCS.size() > 0) {
					int newid = IDcounter.getAndIncrement();
					connectedEdgeSets.put(newid, targetECS);
					for (E te : targetECS) {
						edgeToID.put(te, newid);
					}
					connectedVertexSets.put(newid, targetVCS);
					for (V v : targetVCS) {
						vertexToID.put(v, newid);
					}
				}
			}
		}
	}

	/**
	 * @see VertexSetListener#vertexAdded(GraphVertexChangeEvent)
	 */
	@Override
	public void vertexAdded(GraphVertexChangeEvent<V> e) {
		// " we do nothing when a vertex is added.");//DEBUG 
	}

	/**
	 * @see VertexSetListener#vertexRemoved(GraphVertexChangeEvent)
	 */
	@Override
	public void vertexRemoved(GraphVertexChangeEvent<V> event) {
		if (null == connectedEdgeSets) {
			return;
		}

		V v = event.getVertex();
		Integer id = vertexToID.get(v);
		Set<V> set = connectedVertexSets.get(id);
		if (null == set) {
			throw new RuntimeException("Unknown set ID: " + id);
		}
		boolean removed = set.remove(v);
		if (!removed) {
			throw new RuntimeException("Could not removed vertex " + v + " from set with ID: " + id);
		}
		vertexToID.remove(v);
		
		if (set.isEmpty()) {
			connectedEdgeSets.remove(id);
			connectedVertexSets.remove(id);
		}
	}

	private void init() {
		connectedVertexSets = null;
		vertexToID = new HashMap<V, Integer>();
		connectedEdgeSets = null;
		edgeToID = new HashMap<E, Integer>();
		IDcounter.set(0);
	}

	private void lazyFindConnectedSets() {
		if (connectedVertexSets == null) {
			connectedVertexSets = new HashMap<Integer, Set<V>>();
			connectedEdgeSets = new HashMap<Integer, Set<E>>();

			Set<V> vertexSet = graph.vertexSet();

			if (vertexSet.size() > 0) {
				BreadthFirstIterator<V, E> i = new BreadthFirstIterator<V, E>(graph, null);
				i.addTraversalListener(new MyTraversalListener());

				while (i.hasNext()) {
					i.next();
				}
			}
		}
	}

	//~ Inner Classes ----------------------------------------------------------

	/**
	 * A traversal listener that groups all vertices according to to their
	 * containing connected set.
	 *
	 * @author Barak Naveh
	 * @since Aug 6, 2003
	 */
	private class MyTraversalListener implements TraversalListener<V, E> {
		private Set<V> currentConnectedVertexSet;
		private Set<E> currentConnectedEdgeSet;
		private Integer ID;

		/**
		 * @see TraversalListenerAdapter#connectedComponentFinished(ConnectedComponentTraversalEvent)
		 */
		@Override
		public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
			connectedVertexSets.put(ID, currentConnectedVertexSet);
			connectedEdgeSets.put(ID, currentConnectedEdgeSet);
		}

		/**
		 * @see TraversalListenerAdapter#connectedComponentStarted(ConnectedComponentTraversalEvent)
		 */
		@Override
		public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			currentConnectedVertexSet = new HashSet<V>();
			currentConnectedEdgeSet = new HashSet<E>();
			ID = IDcounter.getAndIncrement();
		}

		/**
		 * @see TraversalListenerAdapter#vertexTraversed(VertexTraversalEvent)
		 */
		@Override
		public void vertexTraversed(VertexTraversalEvent<V> event) {
			V v = event.getVertex();
			currentConnectedVertexSet.add(v);
			vertexToID.put(v, ID);
		}
		
		@Override
		public void edgeTraversed(EdgeTraversalEvent<V, E> event) {
			E e = event.getEdge();
			currentConnectedEdgeSet.add(e);
			edgeToID.put(e, ID);
		}

		@Override
		public void vertexFinished(VertexTraversalEvent<V> e) { }
	}
	
	
	/*
	 * UTILS
	 */
	
	@SuppressWarnings("unused")
	private static final <V> String toString(GraphVertexChangeEvent<V> event) {
		String str = event.getVertex().toString() +": ";
		switch (event.getType()) {
		case GraphVertexChangeEvent.BEFORE_VERTEX_ADDED:
			str += "will be added.";
			break;
		case GraphVertexChangeEvent.BEFORE_VERTEX_REMOVED:
			str += "will be removed.";
			break;
		case GraphVertexChangeEvent.VERTEX_ADDED:
			str += "was added.";
			break;
		case GraphVertexChangeEvent.VERTEX_REMOVED:
			str += "was removed.";
			break;
		}
		return str;
	}
	
	private static final <V, E> String toString(GraphEdgeChangeEvent<V, E> event) {
		String str = event.getEdge().toString() + ": ";
		switch (event.getType()) {
		case GraphEdgeChangeEvent.BEFORE_EDGE_ADDED:
			str += "will be added.";
			break;
		case GraphEdgeChangeEvent.BEFORE_EDGE_REMOVED:
			str += "will be removed.";
			break;
		case GraphEdgeChangeEvent.EDGE_ADDED:
			str += "was added.";
			break;
		case GraphEdgeChangeEvent.EDGE_REMOVED:
			str += "was removed.";
			break;
		}
		return str;
	}
	
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
			Set<E> es = connectedEdgeSets.get(id);
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
}
