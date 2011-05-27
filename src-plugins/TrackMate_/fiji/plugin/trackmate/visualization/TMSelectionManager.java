package fiji.plugin.trackmate.visualization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

/**
 * A utility class that ensures coherent behavior when playing with spot selection across
 * a {@link TrackMateModelView} showing the spot and track data on the image and a {@link TrackSchemeFrame}
 * showing a map of tracks.
 * <p>
 * This class maintains a selection list (actually two; one for spots, one for track edges) and offer
 * to register multiple {@link TMSelectionDisplayer}s. It can be itself registered as a 
 * {@link TMSelectionChangeListener} that will listen for changes in selection. When such a 
 * change occur, the maintained selection lists are updated,
 * all {@link TMSelectionDisplayer} are notified and have their display updated 
 * with the current selection.
 * <p> 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - 2011
 *
 */
public class TMSelectionManager implements TMSelectionChangeListener {

	private static final boolean DEBUG = false;
	private HashSet<Spot> currentSpotSelection = new HashSet<Spot>();
	private HashSet<DefaultWeightedEdge> currentEdgeSelection = new HashSet<DefaultWeightedEdge>();
	private ArrayList<TMSelectionDisplayer> selectionDisplayers = new ArrayList<TMSelectionDisplayer>();

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Add the given displayer to the list managed by this class. 
	 * <p>
	 * The {@link SpotSelectionEvent}s caught by this class will cause the spot selection
	 * managed by this class to be highlighted in all {@link SpotSelectionDisplayer}s registered
	 * using this method.
	 */
	public void registerDisplayer(TMSelectionDisplayer displayer) {
		selectionDisplayers.add(displayer);
	}

	@Override
	public void selectionChanged(final TMSelectionChangeEvent event) {
		Object source = event.getSource();
		if (DEBUG)
			System.out.println("\n[TMSelectionManager] Selection change event, from source: "+source.getClass().getSimpleName());
		Map<Spot, Boolean> spots = event.getSpots();
		if (null != spots) {

			if (DEBUG)
				System.out.print("[TMSelectionManager] Spots: ");
			for(Spot spot : spots.keySet()) {
				if (spots.get(spot)) {
					currentSpotSelection.add(spot);
					if (DEBUG)
						System.out.print("+"+spot+", ");
				} else {
					currentSpotSelection.remove(spot);
					if (DEBUG)
						System.out.print("-"+spot+", ");
				}
			}
			if (DEBUG)
				System.out.println();
			
			Spot firstSpot = null;
			if (currentSpotSelection.size() > 0)
				firstSpot = currentSpotSelection.iterator().next();
			for (TMSelectionDisplayer displayer : selectionDisplayers) {
				if (source == displayer)
					continue; // Do not forward change to source of the change
				if (DEBUG)
					System.out.println("[TMSelectionManager] Forwarding spots highlighting to displayer: "+displayer.getClass().getSimpleName());
				displayer.highlightSpots(currentSpotSelection);
				if (null != firstSpot)
					displayer.centerViewOn(firstSpot);
			}
		} 

		Map<DefaultWeightedEdge, Boolean> edges = event.getEdges();
		if (null != edges) {
			
			if (DEBUG)
				System.out.print("[TMSelectionManager] Edges: ");
			for(DefaultWeightedEdge edge : edges.keySet()) {
				if (edges.get(edge)) {
					currentEdgeSelection.add(edge);
					if (DEBUG)
						System.out.print("+"+edge+", ");
				} else { 
					currentEdgeSelection.remove(edge);
					if (DEBUG)
						System.out.print("-"+edge+", ");
				}
			}
			if (DEBUG)
				System.out.println();
			
			for (TMSelectionDisplayer displayer : selectionDisplayers) {
				if (source == displayer)
					continue; // Do not forward change to source of the change
				if (DEBUG)
					System.out.println("[TMSelectionManager] Forwarding edges highlighting to displayer: "+displayer.getClass().getSimpleName());
				displayer.highlightEdges(currentEdgeSelection);
			}
		}

	}

}
