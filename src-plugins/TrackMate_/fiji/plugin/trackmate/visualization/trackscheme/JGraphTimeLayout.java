package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class JGraphTimeLayout implements JGraphLayout {

	/*
	 * CONSTRUCTOR
	 */
	
	private static final float Y_ZOOM_FACTOR = 50;
	private static final float X_ZOOM_FACTOR = 100;
	
	
	private SimpleGraph<Spot, DefaultEdge> graph;
	private List<Set<Spot>> tracks;


	public JGraphTimeLayout(SimpleGraph<Spot,org.jgrapht.graph.DefaultEdge> graph) {
		this.graph = graph;
		this.tracks = new ConnectivityInspector<Spot, DefaultEdge>(graph).connectedSets();
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(JGraphFacade graphFacade) {
		
		SortedSet<Float> instants = new TreeSet<Float>();
		for (Spot s : graph.vertexSet())
			instants.add(s.getFeature(Feature.POSITION_T));
			
		TreeMap<Float, Integer> columns = new TreeMap<Float, Integer>();
		for(Float instant : instants)
			columns.put(instant, -1);

		int currentColumn = 0;
		Spot previousSpot = null;
		for (Set<Spot> track : tracks) {
			
			// Sort by ascending order
			SortedSet<Spot> sortedTrack = new TreeSet<Spot>(SpotImp.frameComparator);
			sortedTrack.addAll(track);
			Spot root = sortedTrack.first();
			
			DepthFirstIterator<Spot, DefaultEdge> iterator = new DepthFirstIterator<Spot, DefaultEdge>(graph, root);
			while (iterator.hasNext()) {
				Spot spot = iterator.next();
				
				// Determine in what column to put the spot
				Float instant = spot.getFeature(Feature.POSITION_T);
				int freeColumn = columns.get(instant) + 1;
				
				// If we have no direct edge with the previous spot, we add 1 to the current column
				if (!graph.containsEdge(spot, previousSpot))
					currentColumn = currentColumn + 1;
				previousSpot = spot;
				
				int targetColumn = Math.max(freeColumn, currentColumn);
				currentColumn = targetColumn;
				
				// Keep track of column filling
				columns.put(instant, targetColumn);
				
				// Move the corresponding object in the facade
				Collection facadeObjects = graphFacade.getVertices();
				Object facadeTarget = null;
				for(Object obj : facadeObjects) {
					SpotCell s = (SpotCell) obj;
					if (s.getSpot() == spot) {
						facadeTarget = obj;
						break;
					}
				}
				graphFacade.setLocation(facadeTarget, targetColumn * X_ZOOM_FACTOR, instant * Y_ZOOM_FACTOR);
			}

		
			for(Float instant : instants)
				columns.put(instant, currentColumn+1);
		}
		
	}

}
