/* @author rich
 * Created on 18-Nov-2003
 */
package org.nfunk.jep.function;
import org.nfunk.jep.*;
import java.util.Stack;
/**
 * Functions which require greater control over their evaluation should implement this interface.
 *
 * @author Rich Morris
 * @deprecated The interface CallbackEvaluationI should generally be used instead as its simpler and allows different evaluation schemes to be used.
 * @see CallbackEvaluationI
 * Created on 18-Nov-2003
 */
public interface SpecialEvaluationI {

	/**
	 * Performs some special evaluation on the node.
	 * This method has the responsibility for evaluating the children of the node
	 * and it should generally call
	 * <pre>
	 * node.jjtGetChild(i).jjtAccept(pv,data);	
	 * </pre>
	 * for each child. Briefly the symbol table was removed as arguments to this method, it is now reinserted.
	 *
	 * @param node	The current node
	 * @param data	The data passed to visitor, typically not used
	 * @param pv	The visitor, can be used evaluate the children
	 * @param stack	The stack of the evaluator
	 * @param symTab The symbol table
	 * @return the value after evaluation
	 * @throws ParseException
	 */
	public Object evaluate(Node node,Object data,ParserVisitor pv,Stack stack,SymbolTable symTab) throws ParseException;
}
