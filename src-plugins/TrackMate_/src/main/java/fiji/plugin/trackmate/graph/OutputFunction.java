package fiji.plugin.trackmate.graph;

/**
 * Interface for functions that return a new object, computed from two input arguments.
 * @author Jean-Yves Tinevez
 */
public interface OutputFunction<E> {
	
	public E compute(E input1, E input2);

}
