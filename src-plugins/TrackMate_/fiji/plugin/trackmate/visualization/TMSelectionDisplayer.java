package fiji.plugin.trackmate.visualization;

import java.util.Collection;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;

/**
 * This interface specifies capabilities of displaying classes that can highlight 
 * specifics spot or/and track selection.
 */
public interface TMSelectionDisplayer {

	/**
	 * Highlight visually the spot given in argument. Do nothing if the given spot is not in {@link #spotsToShow}.
	 */
	public void highlightSpots(final Collection<Spot> spots);
	
	/**
	 * Highlight visually the edges given in argument.
	 */
	public void highlightEdges(final Set<DefaultWeightedEdge> edges);
	
	/**
	 * Center the view on the given spot.
	 */
	public void centerViewOn(final Spot spot);
	
}
