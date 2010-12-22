package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.X_COLUMN_SIZE;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.Y_COLUMN_SIZE;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class JGraphTimeLayout implements JGraphLayout {

	
	
	private SimpleGraph<Spot, DefaultEdge> graph;
	private List<Set<Spot>> tracks;
	private JGraphModelAdapter<Spot, DefaultEdge> adapter;

	/*
	 * CONSTRUCTOR
	 */
	

	public JGraphTimeLayout(SimpleGraph<Spot, DefaultEdge> graph, JGraphModelAdapter<Spot, DefaultEdge> adapter) {
		this.graph = graph;
		this.adapter = adapter;
		this.tracks = new ConnectivityInspector<Spot, DefaultEdge>(graph).connectedSets();
	}
	
	@Override
	public void run(JGraphFacade graphFacade) {
		
		SortedSet<Float> instants = new TreeSet<Float>();
		for (Spot s : graph.vertexSet())
			instants.add(s.getFeature(Feature.POSITION_T));
			
		TreeMap<Float, Integer> columns = new TreeMap<Float, Integer>();
		for(Float instant : instants)
			columns.put(instant, -1);
		
		TreeMap<Float, Integer> rows = new TreeMap<Float, Integer>();
		Iterator<Float> it = instants.iterator();
		int rowIndex = 1; // Start at 1 to let room for column headers
		while (it.hasNext()) {
			rows.put(it.next(), rowIndex);
			rowIndex++;
		}

		int currentColumn = 1; // Start at 1 to leave room for row header
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
				
				// Get corresponding JGraph cell 
				Object facadeTarget = adapter.getVertexCell(spot);
				// Move the corresponding cell in the facade
				graphFacade.setLocation(facadeTarget, targetColumn * X_COLUMN_SIZE, (0.5 + rows.get(instant)) * Y_COLUMN_SIZE);
			}
		
			for(Float instant : instants)
				columns.put(instant, currentColumn+1);
		}
		
	}

}
