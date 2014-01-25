package net.imglib2;

/**
 * An iterator over pairs of types.
 *  
 * @author "Johannes Schindelin"
 *
 * @param <T>
 */
public interface PairIterator<T> {

	/**
	 * Returns whether there are pairs left.
	 * 
	 * @return true if there are pairs left.
	 */
	boolean hasNext();

	/**
	 * Resets the iterator to just before the first element.
	 */
	void reset();

	/**
	 * Go to the next pair.
	 */
	void fwd();

	/**
	 * Return the first value of the pair.
	 * 
	 * @return the first value of the pair
	 */
	T getFirst();

	/**
	 * Return the second value of the pair.
	 * 
	 * @return the second value of the pair
	 */
	T getSecond();

}
