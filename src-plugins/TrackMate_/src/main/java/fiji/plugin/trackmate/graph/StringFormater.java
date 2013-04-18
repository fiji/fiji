package fiji.plugin.trackmate.graph;

/**
 * Interface for function that can build a human-readable string 
 * representation of an object
 * @author JeanYves
 *
 */
public interface StringFormater<V> {
	
	/**
	 * Convert the given instance to a string representation.
	 */
	public String toString(V instance);

}
