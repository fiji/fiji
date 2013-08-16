/* @author rich
 * Created on 14-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.matrixJep.function;

import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.function.*;
import org.nfunk.jep.*;

/**
 * An overloaded Power function compatible with MatrixJep.
 * 
 * @author Rich Morris
 * Created on 14-Feb-2005
 */
public class MPower extends VPower implements SpecialPreProcessorI
{
	/** During preprocessing sets the function to the Cross function if necessary. */ 
	public MatrixNodeI preprocess(
		ASTFunNode node,
		MatrixPreprocessor visitor,
		MatrixJep jep,
		MatrixNodeFactory nf)
		throws ParseException
	{
		MatrixNodeI children[] = visitor.visitChildrenAsArray(node,null);

		if(node.jjtGetNumChildren()!=2) throw new ParseException("Operator "+node.getOperator().getName()+" must have two elements, it has "+children.length);
		Dimensions lhsDim = children[0].getDim();
		Dimensions rhsDim = children[1].getDim();
		if(rhsDim.equals(Dimensions.ONE))
		{
			Dimensions dim = lhsDim; 
			return (ASTMFunNode) nf.buildOperatorNode(
					node.getOperator(),children,dim);
		}
		Operator op = jep.getOperatorSet().getCross();
		BinaryOperatorI bin = (BinaryOperatorI) op.getPFMC();
		Dimensions dim = bin.calcDim(lhsDim,rhsDim);
		return (ASTMFunNode) nf.buildOperatorNode(op,children,dim);
	}
}
