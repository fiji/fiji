/* @author rich
 * Created on 01-Oct-2004
 */
package org.lsmp.djep.rewrite;

import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;

/**
 * @author Rich Morris
 * Created on 01-Oct-2004
 */
public class ExpandPower implements RewriteRuleI {

	private NodeFactory nf;
	private OperatorSet opSet;
	private TreeUtils tu;
	private XJep xj;

	/**
	 * 
	 */
	public ExpandPower(XJep xj) {
		opSet = xj.getOperatorSet();
		tu = xj.getTreeUtils();
		nf = xj.getNodeFactory();
		this.xj = xj;
	}
	private ExpandPower() {}
	/* (non-Javadoc)
	 * @see org.lsmp.djep.xjep.RewriteRuleI#test(org.nfunk.jep.Node, org.nfunk.jep.Node[])
	 */
	public boolean test(ASTFunNode node, Node[] children) {
		if(!node.isOperator())	return false;
		XOperator op= (XOperator) node.getOperator();

		if(opSet.getPower() == op)
		{
			if(tu.getOperator(children[0]) == opSet.getAdd()
			 || tu.getOperator(children[0]) == opSet.getSubtract())
			{
				return tu.isInteger(children[1]) && (tu.isPositive(children[1]) || tu.isZero(children[1]));
			}
			return false;
		}
		return false;
	}

	public Node apply(ASTFunNode node, Node[] children) throws ParseException {
		Operator lhsOp = tu.getOperator(children[0]); 
		int n = tu.intValue(children[1]);
		Node sub1 = children[0].jjtGetChild(0);
		Node sub2 = children[0].jjtGetChild(1);
		
		if(lhsOp == opSet.getAdd() || lhsOp == opSet.getSubtract())
		{ /* (a+b)^n --> (a^n+nC1 a^(n-1) b + ....) */
			if(n == 0) return nf.buildConstantNode(new Double(1));
			if(n == 1) return children[0];
			
			Node vals[] = new Node[n+1];
			/* a^n */
			vals[0] = nf.buildOperatorNode(
				opSet.getPower(),
				xj.deepCopy(sub1),
				nf.buildConstantNode(new Double(n))
				);
			if(n==2)
			{
				vals[1]=nf.buildOperatorNode(
					opSet.getMultiply(),
					nf.buildConstantNode(new Double(2)),
					nf.buildOperatorNode(
						opSet.getMultiply(),
						xj.deepCopy(sub1),
						xj.deepCopy(sub2)));
			}
			else
			{
				/* n * a^(n-1) * b */
				vals[1]=nf.buildOperatorNode(
					opSet.getMultiply(),
					nf.buildConstantNode(new Double(n)),
					nf.buildOperatorNode(
						opSet.getMultiply(),
						nf.buildOperatorNode(
							opSet.getPower(),
							xj.deepCopy(sub1),
							nf.buildConstantNode(new Double(n-1))),
						xj.deepCopy(sub2)));
			}
			/* n * a * b^(n-1) */
			if(n>=3)
			{
				vals[n-1] = nf.buildOperatorNode(
				opSet.getMultiply(),
				nf.buildConstantNode(new Double(n)),
				nf.buildOperatorNode(
					opSet.getMultiply(),
					xj.deepCopy(sub1),
					nf.buildOperatorNode(
						opSet.getPower(),
						xj.deepCopy(sub2),
						nf.buildConstantNode(new Double(n-1)))));
			}
			/* a^n */
			vals[n] = nf.buildOperatorNode(
				opSet.getPower(),
				xj.deepCopy(sub2),
				nf.buildConstantNode(new Double(n))
				);
			for(int i=2;i<n-1;++i)
			{
				/* (n,i) * a^(n-i) * b^i */ 
				vals[i]=nf.buildOperatorNode(
					opSet.getMultiply(),
					nf.buildConstantNode(new Double(XMath.binomial(n,i))),
					nf.buildOperatorNode(
						opSet.getMultiply(),
						nf.buildOperatorNode(
							opSet.getPower(),
							xj.deepCopy(sub1),
							nf.buildConstantNode(new Double(n-i))),
						nf.buildOperatorNode(
							opSet.getPower(),
							xj.deepCopy(sub2),
							nf.buildConstantNode(new Double(i)))));
			}

			Node sums[] = new Node[n+1];
			sums[n]=vals[n];
			for(int i=n-1;i>=0;--i)
			{
				sums[i] = nf.buildOperatorNode(
					lhsOp,
					vals[i],
					sums[i+1]);
			}
			return sums[0];
		}
		throw new ParseException("ExpandBrackets at least one child must be + or -");
	}

}
