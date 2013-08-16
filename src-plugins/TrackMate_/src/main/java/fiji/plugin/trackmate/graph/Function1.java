package fiji.plugin.trackmate.graph;

/**
 * Interface for function that compute values from a single input and store it in 
 * an output. 
 * @author Jean-Yves Inevez 
 *
 * @param <T1> type of input instances
 * @param <T2> type of output instances
 */
public interface Function1<T1, T2> {
	
	/**
	 * Compute a value from the data in input, and store in output.
	 * @param input  the input instance to compute on
	 * @param output  the output instance that will store the computation result
	 */
	public void compute(final T1 input, final T2 output);

}
