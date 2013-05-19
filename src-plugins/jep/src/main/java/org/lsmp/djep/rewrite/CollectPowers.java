/* @author rich
 * Created on 06-Oct-2004
 */
package org.lsmp.djep.rewrite;

import org.lsmp.djep.xjep.*;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

/**
 * Collect powers together so that x*x -> x^2 and x^n*x -> x^(n+1).
 * @author Rich Morris
 * Created on 06-Oct-2004
 */
public class CollectPowers extends AbstractRewrite {

	/**
	 * 
	 */
	public CollectPowers(XJep xj) {
		super(xj);
	}

	/* (non-Javadoc)
	 * @see org.lsmp.djep.xjep.RewriteRuleI#test(org.nfunk.jep.ASTFunNode, org.nfunk.jep.Node[])
	 */
	public boolean test(ASTFunNode node, Node[] children) {
		if(node.getOperator()==opSet.getMultiply())
		{
			// x * x -> true
			if(tu.isVariable(children[0]) && tu.isVariable(children[1]))
			{
				if(tu.getName(children[0]).equals(tu.getName(children[1])))
					return true;
				return false;
			}

			// x^n * x
			if(tu.getOperator(children[0]) == opSet.getPower() 
				&& tu.isVariable(children[0].jjtGetChild(0))
				&& tu.isVariable(children[1]))
			{
				if(tu.getName(children[0].jjtGetChild(0)).equals(tu.getName(children[1])))
					return true;
				return false;			
			}
			if(tu.isVariable(children[0])
				&& tu.getOperator(children[1]) == opSet.getPower() 
				&& tu.isVariable(children[1].jjtGetChild(0)))
			{
				if(tu.getName(children[0]).equals(tu.getName(children[1].jjtGetChild(0))))
					return true;
				return false;			
			}

		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.lsmp.djep.xjep.RewriteRuleI#apply(org.nfunk.jep.ASTFunNode, org.nfunk.jep.Node[])
	 */
	public Node apply(ASTFunNode node, Node[] children) throws ParseException {

		if(node.getOperator()==opSet.getMultiply())
		{
			if(tu.isVariable(children[0]) && tu.isVariable(children[1]))
			{
				if(tu.getName(children[0]).equals(tu.getName(children[1])))
				{
					return nf.buildOperatorNode(
						opSet.getPower(),
						children[0],
						nf.buildConstantNode(new Double(2.0))
						);
				}
			}
			if(tu.getOperator(children[0]) == opSet.getPower() 
				&& tu.isVariable(children[0].jjtGetChild(0))
				&& tu.isVariable(children[1]))
			{
				if(tu.getName(children[0].jjtGetChild(0)).equals(tu.getName(children[1])))
				{
					return nf.buildOperatorNode(
						opSet.getPower(),
						children[1],
						nf.buildOperatorNode(
							opSet.getAdd(),
							children[0].jjtGetChild(1),
							nf.buildConstantNode(tu.getONE())
							));
				}
			}
			if(tu.isVariable(children[0])
				&& tu.getOperator(children[1]) == opSet.getPower() 
				&& tu.isVariable(children[1].jjtGetChild(0)))
			{
				if(tu.getName(children[0]).equals(tu.getName(children[1].jjtGetChild(0))))
				{
					return nf.buildOperatorNode(
						opSet.getPower(),
						children[0],
						nf.buildOperatorNode(
							opSet.getAdd(),
							children[1].jjtGetChild(1),
							nf.buildConstantNode(tu.getONE())
							));
				}
			}
		}
		return null;
	}

}
