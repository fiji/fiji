/* @author rich
 * Created on 14-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.matrixJep.function;

import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * @author Rich Morris
 * Created on 14-Feb-2005
 */
public class MDiff extends PostfixMathCommand implements SpecialPreProcessorI
{

	public MDiff()
	{
		super();
		this.numberOfParameters = 2;
	}

	public MatrixNodeI preprocess(
		ASTFunNode node,
		MatrixPreprocessor visitor,
		MatrixJep jep,
		MatrixNodeFactory nf)
		throws ParseException
	{
		MatrixNodeI children[] = visitor.visitChildrenAsArray(node,null);
		if(children.length != 2)
			throw new ParseException("Diff opperator should have two children, it has "+children.length);
		// TODO need to handle diff(x,[x,y])
		if(!(children[1] instanceof ASTMVarNode))
			throw new ParseException("rhs of diff opperator should be a variable.");
		ASTMVarNode varNode = (ASTMVarNode) children[1];
		MatrixNodeI diff = (MatrixNodeI) jep.differentiate(children[0],varNode.getName());
		return diff;
	}

}
