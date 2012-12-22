package fiji.plugin.trackmate.tests;

import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackGraphModel;
import fiji.plugin.trackmate.TrackMateModel;

public class Graph_Test {

	public static void main(String[] args) {

		TrackMateModel model = getExampleModel();
		countOverallLeaves(model);
		pickLeavesOfOneTrack(model);

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
		TrackGraphModel graph = model.getTrackModel();

		// Create spots

		Spot root 	= new SpotImp(new double[] { 	3d,  	0d, 	0d }, 	"Zygote");

		Spot AB 	= new SpotImp(new double[] { 	0d,  	1d, 	0d }, 	"AB");
		Spot P1 	= new SpotImp(new double[] { 	3d,  	1d, 	0d }, 	"P1");

		Spot P2 	= new SpotImp(new double[] { 	4d,  	2d, 	0d }, 	"P2");
		Spot EMS 	= new SpotImp(new double[] { 	2d,  	2d, 	0d }, 	"EMS");

		Spot P3 	= new SpotImp(new double[] { 	5d,  	3d, 	0d }, 	"P3");
		Spot C 		= new SpotImp(new double[] { 	3d,  	3d, 	0d }, 	"C");
		Spot E 		= new SpotImp(new double[] { 	1d,  	3d, 	0d }, 	"E");
		Spot MS		= new SpotImp(new double[] { 	2d,  	3d, 	0d }, 	"MS");

		Spot D 		= new SpotImp(new double[] { 	4d,  	4d, 	0d }, 	"D");
		Spot P4 	= new SpotImp(new double[] { 	5d,  	4d, 	0d }, 	"P4");

		// Set their "frame" - required

		root.putFeature(Spot.FRAME, 0);
		AB.putFeature(Spot.FRAME, 1);
		P1.putFeature(Spot.FRAME, 1);

		P2.putFeature(Spot.FRAME, 2);
		EMS.putFeature(Spot.FRAME, 2);

		P3.putFeature(Spot.FRAME, 3);
		C.putFeature(Spot.FRAME, 3);
		E.putFeature(Spot.FRAME, 3);
		MS.putFeature(Spot.FRAME, 3);

		P4.putFeature(Spot.FRAME, 4);
		D.putFeature(Spot.FRAME, 4);


		// Add them to the graph

		model.addSpotTo(root, 0);

		model.addSpotTo(AB, 1);
		model.addSpotTo(P1, 1);

		model.addSpotTo(P2, 2);
		model.addSpotTo(EMS, 2);

		model.addSpotTo(P3, 3);
		model.addSpotTo(C, 3);
		model.addSpotTo(E, 3);
		model.addSpotTo(MS, 3);

		model.addSpotTo(D, 4);
		model.addSpotTo(P4, 4);

		// Create links

		graph.addEdge(root, AB, 1);
		graph.addEdge(root, P1, 1);

		graph.addEdge(P1, P2, 1);
		graph.addEdge(P1, EMS, 1);

		graph.addEdge(EMS, E, 1);
		graph.addEdge(EMS, MS, 1);

		graph.addEdge(P2, P3, 1);
		graph.addEdge(P2, C, 1);

		graph.addEdge(P3, P4, 1);
		graph.addEdge(P3, D, 1);

		// Done!

		return model;
	}

}
