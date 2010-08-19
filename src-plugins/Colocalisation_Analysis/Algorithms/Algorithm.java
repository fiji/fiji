import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * An algorithm is an abstraction of techniques like the
 * calculation of the Persons coefficient or Li'S ICQ. It
 * allows to separate initialization and execution of
 * such an algorithm.
 */
public abstract class Algorithm {

	/* a list of warnings that can be filled by the
	 *  execute method
	 */
	Dictionary<String, String> warnings = new Hashtable<String, String>();

	/**
	 * Executes the previously initialized {@link Algorithm}.
	 */
	public abstract void execute(DataContainer container) throws MissingPreconditionException;

	/**
	 * Gets a reference to the warnings.
	 *
	 * @return A reference to the warnings list
	 */
	public Dictionary<String, String> getWarningns() {
		return warnings;
	}
}
