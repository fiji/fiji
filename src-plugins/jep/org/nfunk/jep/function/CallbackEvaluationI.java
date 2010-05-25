/* @author rich
 * Created on 18-Nov-2003
 */
package org.nfunk.jep.function;
import org.nfunk.jep.*;
/**
 * Functions which require greater control over their evaluation should implement this interface.
 *
 * @author Rich Morris
 * Created on 18-Nov-2003
 */
public interface CallbackEvaluationI {

	/**
	 * Performs some special evaluation on the node.
	 * This method has the responsibility for evaluating the children of the node
	 * and it should generally call
	 * <pre>
	 * pv.eval(node.jjtGetChild(i))	
	 * </pre>
	 * for each child.
	 * 
	 * The SymbolTable is not passed as an argument. This is because
	 * it is better practice to get and set variable values by using
	 * node.getVar().setValue() rather that through the SymbolTable with
	 * requires a hashtable lookup.
	 *
	 * @param node	The current node
	 * @param pv	The visitor, can be used evaluate the children
	 * @return the value after evaluation
	 * @throws ParseException
	 */
	public Object evaluate(Node node,EvaluatorI pv) throws ParseException;
}
