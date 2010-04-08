/* @author rich
 * Created on 19-Jun-2003
 */
package org.lsmp.djep.xjep;
import org.nfunk.jep.*;

/**
 * Interface defining the special actions performed during the preprocess 
 * stage. This interface should be implemented by PostFixMath Commands
 * which wish to perform a special action during the XJep.preprocess() method. 
 */
public interface CommandVisitorI {

	/**
	 * Performs the specified action on an expression tree.
	 * @param node top node of the tree
	 * @param children the children of the node after they have been preprocessed.
	 * @param xjep a reference to the current XJep interface.
	 * @return top node of the results.
	 * @throws ParseException
	 */
	public Node process(Node node,Node children[],XJep xjep) throws ParseException;
}
