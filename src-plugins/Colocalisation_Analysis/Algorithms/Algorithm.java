/**
 * An algorithm is an abstraction of techniques like the
 * calculation of the Persons coefficient or Li'S ICQ. It
 * allows to separate initialization and execution of
 * such an algorithm.
 */
public abstract class Algorithm {

	/**
	 * Executes the previously initialized {@link Algorithm}.
	 */
	public abstract void execute(DataContainer container) throws MissingPreconditionException;
}
