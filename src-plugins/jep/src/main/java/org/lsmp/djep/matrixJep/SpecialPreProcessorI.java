/* @author rich
 * Created on 14-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.matrixJep;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.nfunk.jep.*;
/**
 * Applies a special preprocessing step for a function or operator.
 * 
 * @author Rich Morris
 * Created on 14-Feb-2005
 */
public interface SpecialPreProcessorI
{
	/**
	 * Subverts the preprocessing stage.
	 * Preprocessing performs a number of operations:
	 * <ol>
	 * <li>Converts each node into one of the subtypes of MatrixNodeI</li>
	 * <li>Calculate the dimensions for the results</li>
	 * <li>Performs any special symbolic operations such as differentation</li> 
	 * </ol>
	 * In general the first step in preprocessing is to run the preprocessor on the children of the node.
	 * This can be done using 
	 * <pre>
	 * MatrixNodeI children[] = visitor.visitChildrenAsArray(node,null);
	 * </pre>
	 * The final step is to construct a node of the correct type. The MatrixNodeFactory
	 * argument has a number of methods to do this. For example
	 * <pre>
	 * return (ASTMFunNode) nf.buildOperatorNode(node.getOperator(),children,rhsDim);
	 * </pre>
	 * Note how the dimension is specified.
	 * 
	 * @param node the top node of the tree representing the function and its arguments.
	 * @param visitor A reference to the preprocessing visitor.
	 * @param jep A reference of the MatrixJep instance.
	 * @param nf  A reference to the node factory object.
	 * @return A new MatrixNodeI representing the converted function.
	 * @throws ParseException if some error occurs.
	 */
	public MatrixNodeI preprocess(
		ASTFunNode node,
		MatrixPreprocessor visitor,
		MatrixJep jep,
		MatrixNodeFactory nf) throws ParseException;

}
