package fiji.plugin.trackmate.tracking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class TrackSplitter {

	
	public static final int LONE_VERTEX			= 0;
	public static final int BRANCH_START		= 1;
	public static final int BRANCH_END			= 2;
	public static final int BRIDGE				= 4;
	public static final int SPLITTING_START 	= 8;
	public static final int MERGING_END		 	= 16;
	public static final int SPLITTING_POINT		= 32;
	public static final int MERGING_POINT 		= 64;
	public static final int COMPLEX_POINT		= 128;
	public static final int NOT_IN_GRAPH		= 256; 
	
	public static final int MERGING_EVENT		= MERGING_POINT + MERGING_END;
	public static final int SPLITTING_EVENT		= SPLITTING_POINT + SPLITTING_START;
	public static final int TERMINATION_EVENT	= BRANCH_END + BRANCH_START;
	public static final int BRANCHING_EVENT 	= BRANCH_START + SPLITTING_EVENT;
	
	
	private UndirectedGraph<Spot, DefaultWeightedEdge> graph; 
	
	public TrackSplitter(UndirectedGraph<Spot, DefaultWeightedEdge> graph) {
		this.graph = graph;
	}
	
	
	
	public ArrayList<ArrayList<Spot>> splitTrackInBranches(Set<Spot> track) {
		
		SortedSet<Spot> sortedTrack = new TreeSet<Spot>(SpotImp.frameComparator);
		sortedTrack.addAll(track);
		Spot first = sortedTrack.first();

		ArrayList<ArrayList<Spot>> branches = new ArrayList<ArrayList<Spot>>();
		ArrayList<Spot> currentParent = null;
		
		DepthFirstIterator<Spot, DefaultWeightedEdge> iterator = new DepthFirstIterator<Spot, DefaultWeightedEdge>(graph, first);
		Spot previousSpot = null;
		while (iterator.hasNext()) {
			Spot spot = iterator.next();
			
			int type = getVertexType(graph, spot);
			
			if (type == BRANCH_START) {
				branches.add(currentParent);
				currentParent = new ArrayList<Spot>();
				currentParent.add(spot);
			
			} else if (type == SPLITTING_POINT || type == MERGING_POINT) {
				branches.add(currentParent); // finish current branch
				currentParent = new ArrayList<Spot>(); // make a new branch
				
			} else if (type == BRANCH_END) {
				currentParent.add(spot);
				branches.add(currentParent); // Finish this one
				currentParent = new ArrayList<Spot>(); // Create a new branch for the next spot
				
			} else if (!graph.containsEdge(spot, previousSpot)) {
				branches.add(currentParent);
				currentParent = new ArrayList<Spot>(); // make a new branch
				currentParent.add(spot);
					
			} else { 
				currentParent.add(spot);
			} 
			
			previousSpot = spot;
			
		}
				
		return branches;
	}
	
	
	
	
	
	
	public static final int getVertexType(UndirectedGraph<Spot, DefaultWeightedEdge> graph, Spot spot) {
		if (!graph.containsVertex(spot))
			return NOT_IN_GRAPH;
		
		Set<DefaultWeightedEdge> edges = graph.edgesOf(spot);
		int nConnections = edges.size();
		
		if (nConnections == 0) 
			return LONE_VERTEX;
		
		float t0 = spot.getFeature(Feature.POSITION_T);

		if (nConnections == 1) {
			DefaultWeightedEdge edge = edges.iterator().next();
			Spot other = graph.getEdgeSource(edge);
			if (other == spot)
				other = graph.getEdgeTarget(edge);
			float t1 = other.getFeature(Feature.POSITION_T);
			if (t1 > t0)
				return BRANCH_START;
			else
				return BRANCH_END; // What if t0 == t1?
		}
		
		if (nConnections == 2) {
			Iterator<DefaultWeightedEdge> it = edges.iterator();
			DefaultWeightedEdge edge1 = it.next();
			Spot other1 = graph.getEdgeSource(edge1);
			if (other1 == spot)
				other1 = graph.getEdgeTarget(edge1);
			float t1 = other1.getFeature(Feature.POSITION_T);
			DefaultWeightedEdge edge2 = it.next();
			Spot other2 = graph.getEdgeSource(edge2);
			if (other2 == spot)
				other2 = graph.getEdgeTarget(edge2);
			float t2 = other2.getFeature(Feature.POSITION_T);
			if ( (t2>t0 && t0>t1) || (t2<t0 && t0<t1) )
				return BRIDGE;
			else if (t0 > t1 && t0 >t2)
				return MERGING_END;
			else 
				return SPLITTING_START;
		}
		
		int before = 0;
		int after = 0;
		for(DefaultWeightedEdge edge : edges) {
			Spot other = graph.getEdgeSource(edge);
			if (other == spot)
				other = graph.getEdgeTarget(edge);
			float t = other.getFeature(Feature.POSITION_T);
			if (t > t0)
				after++;
			if (t < t0) 
				before++;
		}
		
		if (before == 0 && after > 0)
			return SPLITTING_START;
		
		if (after == 0 && before > 0)
			return MERGING_END;
		
		if (before == 1 && after > 0)
			return SPLITTING_POINT;
		
		if (after == 1 && before > 0)
			return MERGING_POINT;
		
		return COMPLEX_POINT;
	}
	
	
	
}
