package fiji.plugin.trackmate.graph;

/**
 * Interface for function that compute values from a two inputs and store it in 
 * an output. 
 * @author Jean-Yves Inevez 
 *
 * @param <T1> type of input instances
 * @param <T2> type of output instances
 */
public interface Function2<T1, T2> {
	
	/**
	 * Compute a value from the data in two inputs, and store in output.
	 * @param input1  the first input instance to compute on
	 * @param input2  the second input instance to compute on
	 * @param output  the output instance that will store the computation result
	 */
	public void compute(final T1 input1, final T1 input2, final T2 output);

}
