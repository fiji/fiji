package fiji.plugin.trackmate.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

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
	
	private TrackMateModel model; 
	
	public TrackSplitter(final TrackMateModel model) {
		this.model = model;
	}
	
	
	
	public ArrayList<ArrayList<Spot>> splitTrackInBranches(Set<Spot> track) {
		
		SortedSet<Spot> sortedTrack = new TreeSet<Spot>(Spot.timeComparator);
		sortedTrack.addAll(track);
		Spot first = sortedTrack.first();

		ArrayList<ArrayList<Spot>> branches = new ArrayList<ArrayList<Spot>>();
		ArrayList<Spot> currentParent = null;
		
		DepthFirstIterator<Spot, DefaultWeightedEdge> iterator = model.getTrackModel().getDepthFirstIterator(first, false);
		Spot previousSpot = null;
		while (iterator.hasNext()) {
			Spot spot = iterator.next();
			
			int type = getVertexType(model, spot);
			
			if (type == BRANCH_START) {
				// This can be a real branch start, unless we are iterating backward in time. 
				// Then this event should be a branch stop. We discriminate between the 2 using
				//	the previous spot: if it is connected to this one, then we are moving backward
				// and it is a branch stop.
				if (model.getTrackModel().containsEdge(spot, previousSpot)) {
					// branch stop
					currentParent.add(spot);
					branches.add(currentParent);
					currentParent = new ArrayList<Spot>(); // make a new branch
				} else {
					// branch start
					branches.add(currentParent);
					currentParent = new ArrayList<Spot>();
					currentParent.add(spot);
				}
			
			} else if (type == SPLITTING_POINT || type == MERGING_POINT) {
				branches.add(currentParent); // finish current branch
				currentParent = new ArrayList<Spot>(); // make a new branch
				
			
			} else if (type == BRANCH_END) {
				// See BRANCH_START comment
				if (model.getTrackModel().containsEdge(spot, previousSpot)) {
					currentParent.add(spot);
					branches.add(currentParent); // Finish this one
					currentParent = new ArrayList<Spot>(); // Create a new branch for the next spot
				} else {
					// branch start
					branches.add(currentParent);
					currentParent = new ArrayList<Spot>();
					currentParent.add(spot);
				}
				
			} else if (!model.getTrackModel().containsEdge(spot, previousSpot)) {
				branches.add(currentParent);
				currentParent = new ArrayList<Spot>(); // make a new branch
				currentParent.add(spot);
					
			} else { 
				currentParent.add(spot);
			} 
			
			previousSpot = spot;
			
		}
		
		// In the general case, there might be empty branches. We prune them here
		ArrayList<ArrayList<Spot>> prunedBranches = new ArrayList<ArrayList<Spot>>();
		for (ArrayList<Spot> branch : branches) {
			if (branch == null || branch.size() == 0)
				continue;
			prunedBranches.add(branch);
				
		}
		
		return prunedBranches;
	}
	
	public static final int getVertexType(final TrackMateModel model, final Spot spot) {
		if (!model.getFilteredSpots().getAllSpots().contains(spot))
			return NOT_IN_GRAPH;
		
		Set<DefaultWeightedEdge> edges = model.getTrackModel().edgesOf(spot);
		int nConnections = edges.size();
		
		if (nConnections == 0) 
			return LONE_VERTEX;
		
		int t0 = model.getSpots().getFrame(spot);

		if (nConnections == 1) {
			DefaultWeightedEdge edge = edges.iterator().next();
			Spot other = model.getTrackModel().getEdgeSource(edge);
			if (other == spot)
				other = model.getTrackModel().getEdgeTarget(edge);
			int t1 = model.getSpots().getFrame(other);
			if (t1 > t0)
				return BRANCH_START;
			else
				return BRANCH_END; // What if t0 == t1?
		}
		
		if (nConnections == 2) {
			Iterator<DefaultWeightedEdge> it = edges.iterator();
			DefaultWeightedEdge edge1 = it.next();
			Spot other1 = model.getTrackModel().getEdgeSource(edge1);
			if (other1 == spot)
				other1 = model.getTrackModel().getEdgeTarget(edge1);
//			double t1 = other1.getFeature(SpotFeature.POSITION_T);
			int t1 = model.getSpots().getFrame(other1);
			DefaultWeightedEdge edge2 = it.next();
			Spot other2 = model.getTrackModel().getEdgeSource(edge2);
			if (other2 == spot)
				other2 = model.getTrackModel().getEdgeTarget(edge2);
//			double t2 = other2.getFeature(SpotFeature.POSITION_T);
			int t2 = model.getSpots().getFrame(other2);
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
			Spot other = model.getTrackModel().getEdgeSource(edge);
			if (other == spot)
				other = model.getTrackModel().getEdgeTarget(edge);
			double t = other.getFeature(Spot.POSITION_T);
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
