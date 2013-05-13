package fiji.plugin.trackmate.tests;

import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.graph.GraphUtils;

public class Graph_Test {

	public static void main(String[] args) {

		TrackMateModel model = getExampleModel();
//		TrackMateModel model = getComplicatedExample();
		countOverallLeaves(model);
		pickLeavesOfOneTrack(model);

		System.out.println(GraphUtils.toString(model.getTrackModel()));


	}


	private static void pickLeavesOfOneTrack(final TrackMateModel model) {
		DirectedNeighborIndex<Spot, DefaultWeightedEdge> cache = model.getTrackModel().getDirectedNeighborIndex();
		TreeSet<Spot> spots = new TreeSet<Spot>(Spot.frameComparator);
		spots.addAll(model.getTrackModel().vertexSet());
		Spot first = spots.first();
		DepthFirstIterator<Spot, DefaultWeightedEdge> iterator = model.getTrackModel().getDepthFirstIterator(first, true);

		while (iterator.hasNext()) {
			Spot spot = iterator.next();
			boolean isBranching = cache.successorsOf(spot).size() > 1;
			if (isBranching) {
				System.out.println(" - " + spot + " is branching to " + cache.successorsOf(spot).size() + " children.");
			} else {
				boolean isLeaf = cache.successorsOf(spot).size() == 0;
				if (isLeaf) {
					System.out.println(" - " + spot + " is a leaf.");
				} else {
					System.out.println(" - " + spot);
				}
			}
		}
	}	

	private static void countOverallLeaves(final TrackMateModel model) {
		DirectedNeighborIndex<Spot, DefaultWeightedEdge> cache = model.getTrackModel().getDirectedNeighborIndex();
		int nleaves = 0;
		Set<Spot> spots = model.getTrackModel().vertexSet();
		for (Spot spot : spots) {
			if (cache.successorsOf(spot).size() == 0) {
				nleaves++;
			}
		}
		System.out.println("Iterated over " + spots.size() + " spots.");
		System.out.println("Found " + nleaves +" leaves.");
	}

	public static final TrackMateModel getExampleModel() {

		TrackMateModel model = new TrackMateModel();

		// Create spots

		Spot root 	= new Spot(new double[] { 	3d,  	0d, 	0d }, 	"Zygote");

		Spot AB 	= new Spot(new double[] { 	0d,  	1d, 	0d }, 	"AB");
		Spot P1 	= new Spot(new double[] { 	3d,  	1d, 	0d }, 	"P1");

		Spot P2 	= new Spot(new double[] { 	4d,  	2d, 	0d }, 	"P2");
		Spot EMS 	= new Spot(new double[] { 	2d,  	2d, 	0d }, 	"EMS");

		Spot P3 	= new Spot(new double[] { 	5d,  	3d, 	0d }, 	"P3");
		Spot C 		= new Spot(new double[] { 	3d,  	3d, 	0d }, 	"C");
		Spot E 		= new Spot(new double[] { 	1d,  	3d, 	0d }, 	"E");
		Spot MS		= new Spot(new double[] { 	2d,  	3d, 	0d }, 	"MS");
		Spot AB3 	= new Spot(new double[] { 	0d,  	3d, 	0d }, 	"AB");

		Spot D 		= new Spot(new double[] { 	4d,  	4d, 	0d }, 	"D");
		Spot P4 	= new Spot(new double[] { 	5d,  	4d, 	0d }, 	"P4");

		// Add them to the graph

		model.beginUpdate();
		try {

			model.addSpotTo(root, 0);

			model.addSpotTo(AB, 1);
			model.addSpotTo(P1, 1);

			model.addSpotTo(P2, 2);
			model.addSpotTo(EMS, 2);

			model.addSpotTo(P3, 3);
			model.addSpotTo(C, 3);
			model.addSpotTo(E, 3);
			model.addSpotTo(MS, 3);
			model.addSpotTo(AB3, 3);

			model.addSpotTo(D, 4);
			model.addSpotTo(P4, 4);

			// Create links

			model.addEdge(root, AB, 1);
			model.addEdge(root, P1, 1);

			model.addEdge(P1, P2, 1);
			model.addEdge(P1, EMS, 1);

			model.addEdge(AB, AB3 , 1);

			model.addEdge(EMS, E, 1);
			model.addEdge(EMS, MS, 1);

			model.addEdge(P2, P3, 1);
			model.addEdge(P2, C, 1);

			model.addEdge(P3, P4, 1);
			model.addEdge(P3, D, 1);

		} finally {
			model.endUpdate();
		}

		// Done!

		return model;
	}

	public static final TrackMateModel getComplicatedExample() {
		TrackMateModel model = getExampleModel();

		// Retrieve target spot by name
		Spot P3 = null;
		for (Spot spot : model.getSpots().getAllSpots()) {
			if (spot.getName().equals("P3")) {
				P3 = spot;
				break;
			}
		}
		
		// Update model
		model.beginUpdate();
		try {
			// new spots
			Spot Q1 = model.addSpotTo(new Spot(new double[3], "Q1"), 0);
			Spot Q2 = model.addSpotTo(new Spot(new double[3], "Q2"), 1);
			Spot Q3 = model.addSpotTo(new Spot(new double[3], "Q3"), 2);
			// new links
			model.addEdge(Q1, Q2, -1);
			model.addEdge(Q2, Q3, -1);
			model.addEdge(Q3, P3, -1);
		} finally {
			model.endUpdate();
		}

		return model;
	}

}
