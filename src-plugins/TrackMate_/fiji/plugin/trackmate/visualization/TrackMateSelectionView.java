package fiji.plugin.trackmate.visualization;

import java.util.Collection;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;

public interface TrackMateSelectionView {

	/**
	 * Highlight visually the spot given in argument. Do nothing if the given spot is not in {@link #spotsToShow}.
	 */
	public void highlightSpots(final Collection<Spot> spots);

	/**
	 * Highlight visually the edges given in argument.
	 */
	public void highlightEdges(final Collection<DefaultWeightedEdge> edges);

	/**
	 * Center the view on the given spot.
	 */
	public void centerViewOn(final Spot spot);
}
