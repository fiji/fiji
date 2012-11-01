/* @author rich
 * Created on 01-Oct-2004
 */
package org.lsmp.djep.rewrite;

//import org.lsmp.djep.xjep.RewriteRuleI;
import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;

/**
 * @author Rich Morris
 * Created on 01-Oct-2004
 */
public class ExpandBrackets extends AbstractRewrite {

	/**
	 * TODO cope with a * uminus(x+x)
	 */
	public ExpandBrackets(XJep xj) {
		super(xj);
	}
	/* (non-Javadoc)
	 * @see org.lsmp.djep.xjep.RewriteRuleI#test(org.nfunk.jep.Node, org.nfunk.jep.Node[])
	 */
	public boolean test(ASTFunNode node, Node[] children) {
		if(!node.isOperator())	return false;
		XOperator op= (XOperator) node.getOperator();

		if(opSet.getMultiply() == op)
		{
			if(tu.getOperator(children[0]) == opSet.getAdd())
				return true;
			if(tu.getOperator(children[0]) == opSet.getSubtract())
				return true;
			if(tu.getOperator(children[1]) == opSet.getAdd())
				return true;
			if(tu.getOperator(children[1]) == opSet.getSubtract())
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.lsmp.djep.xjep.RewriteRuleI#apply(org.nfunk.jep.Node, org.nfunk.jep.Node[])
	 */
	public Node apply(ASTFunNode node, Node[] children) throws ParseException {
		
		Operator lhsOp = tu.getOperator(children[0]); 
		Operator rhsOp = tu.getOperator(children[1]); 
		if(lhsOp == opSet.getAdd() || lhsOp == opSet.getSubtract())
		{ /* (a+b)*c --> (a*c)+(b*c) */
			return nf.buildOperatorNode(
				lhsOp,
				nf.buildOperatorNode(
					opSet.getMultiply(),
						children[0].jjtGetChild(0),
						xj.deepCopy(children[1])),
				nf.buildOperatorNode(
					opSet.getMultiply(),
						children[0].jjtGetChild(1),
						xj.deepCopy(children[1]))
						);
	
		}
		if(rhsOp == opSet.getAdd() || rhsOp == opSet.getSubtract())
		{	/* a*(b+c) -> (a*b)+(a*c) */
			return nf.buildOperatorNode(
				rhsOp,
				nf.buildOperatorNode(
					opSet.getMultiply(),
						xj.deepCopy(children[0]),
						children[1].jjtGetChild(0)),
				nf.buildOperatorNode(
					opSet.getMultiply(),
						xj.deepCopy(children[0]),
						children[1].jjtGetChild(1))
						);
		}
		throw new ParseException("ExpandBrackets at least one child must be + or -");
	}

}
