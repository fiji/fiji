/**
 * 
 */
package fiji.plugin.trackmate.graph;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;

public class TimeDirectedSortedDepthFirstIterator extends SortedDepthFirstIterator<Spot, DefaultWeightedEdge> {

	public TimeDirectedSortedDepthFirstIterator(Graph<Spot, DefaultWeightedEdge> g, Spot startVertex, Comparator<Spot> comparator) {
		super(g, startVertex, comparator);
	}
	
	
	
    protected void addUnseenChildrenOf(Spot vertex) {
    	
    	// Retrieve target vertices, and sort them in a TreeSet
    	TreeSet<Spot> sortedChildren = new TreeSet<Spot>(comparator);
    	// Keep a map of matching edges so that we can retrieve them in the same order
    	Map<Spot, DefaultWeightedEdge> localEdges = new HashMap<Spot, DefaultWeightedEdge>();
    	
    	int ts = vertex.getFeature(Spot.FRAME).intValue();
        for (DefaultWeightedEdge edge : specifics.edgesOf(vertex)) {
        	
        	Spot oppositeV = Graphs.getOppositeVertex(graph, edge, vertex);
        	int tt = oppositeV.getFeature(Spot.FRAME).intValue();
        	if (tt <= ts) {
        		continue;
        	}
        	
        	if (!seen.containsKey(oppositeV)) {
        		sortedChildren.add(oppositeV);
        	}
        	localEdges.put(oppositeV, edge);
        }
        
        Iterator<Spot> it = sortedChildren.descendingIterator();
        while (it.hasNext()) {
			Spot child = it.next();
			
            if (nListeners != 0) {
                fireEdgeTraversed(createEdgeTraversalEvent(localEdges.get(child)));
            }

            if (seen.containsKey(child)) {
                encounterVertexAgain(child, localEdges.get(child));
            } else {
                encounterVertex(child, localEdges.get(child));
            }
        }
    }

	
	
}