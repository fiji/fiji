package algorithms;

import gadgets.DataContainer;

import java.util.ArrayList;
import java.util.List;

import results.ResultHandler;
import results.Warning;


import mpicbg.imglib.type.numeric.RealType;

/**
 * An algorithm is an abstraction of techniques like the
 * calculation of the Persons coefficient or Li'S ICQ. It
 * allows to separate initialization and execution of
 * such an algorithm.
 */
public abstract class Algorithm<T extends RealType<T>> {

	/* a list of warnings that can be filled by the
	 *  execute method
	 */
	List<Warning> warnings = new ArrayList<Warning>();

	/**
	 * Executes the previously initialized {@link Algorithm}.
	 */
	public abstract void execute(DataContainer<T> container) throws MissingPreconditionException;

	/**
	 * A method to give the algorithm the opportunity to let
	 * its results being processed by the passed handler.
	 * By default this methods passes the collected warnings to
	 * the handler and sub-classes should make use of this by
	 * adding custom behavior and call the super class.
	 *
	 * @param handler The ResultHandler to process the results.
	 */
	public void processResults(ResultHandler<T> handler) {
		for (Warning w : warnings)
			handler.handleWarning( w );
	}

	/**
	 * Gets a reference to the warnings.
	 *
	 * @return A reference to the warnings list
	 */
	public List<Warning> getWarnings() {
		return warnings;
	}

	/**
	 * Adds a warning to the list of warnings.
	 *
	 * @param shortMsg A short descriptive message
	 * @param longMsg A long message
	 */
	protected void addWarning(String shortMsg, String longMsg) {
		warnings.add( new Warning(shortMsg, longMsg) );
	}
}
