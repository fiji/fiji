package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;

/**
 * Interface for functions that can generate track colors, <b>with a 
 * different color for each vertex and edge</b>. This allow more
 * detailed views, but is most likely more costly in memory and computation
 * time.
 * <p>
 * The spot coloring can seem to be redundant with individual spot coloring 
 * defined elsewhere. However, it must be noted that this interface is intended
 * for <b>track coloring</b>, and is applied to spots in tracks only. 
 * Concrete implementations of {@link TrackMateModelView} decide whether they
 * abide to individual spot coloring or spots within tracks coloring (this interface). 
 *  
 * @author Jean-Yves Tinevez
 */
public interface TrackPartsColorGenerator {

	/** @return a color for the given spot, that must belong to the 
	 * track with the specified ID.
	 * <p>
	 * Specifying the track ID allows save computations, but the
	 * ID must be accurate or inadequate rendering will happen.
	 */
	public Color color(final Spot spot, final Integer trakID);

	/** @return a color for the given edge, that must belong to the 
	 * track with the specified ID.
	 * <p>
	 * Specifying the track ID allows save computations, but the
	 * ID must be accurate or inadequate rendering will happen.
	 */
	public Color color(final DefaultWeightedEdge edge, final Integer trakID);

}
