package fiji.plugin.trackmate.graph;

import fiji.plugin.trackmate.Spot;

/**
 * Interface for functions that can associate a new object with a spot.
 * @author Jean-Yves Tinevez
 *
 * @param <V>  the type of the new object created by the function
 */
public interface SpotFunction<V> {

	/**
	 * @return a new value computed from the given {@link Spot}.
	 */
	public V apply(final Spot spot);
	
}
