/* @author rich
 * Created on 05-Oct-2004
 */
package org.lsmp.djep.groupJep;

import org.nfunk.jep.*;
import org.lsmp.djep.groupJep.values.*;
import org.lsmp.djep.groupJep.groups.*;
import org.lsmp.djep.xjep.*;
/**
 * Constructs a polynomial from a JEP equation.
 * 
 * @author Rich Morris
 * Created on 05-Oct-2004
 */
public class PolynomialVisitor extends DoNothingVisitor {
	private OperatorSet opSet;
	private FreeGroup fg;
	/**
	 * 
	 */
	public PolynomialVisitor(JEP j) {
		super();
		opSet = j.getOperatorSet();
	}

	/**
	 * calculates a polynomial representing the node.
	 * @param node The top node of the expression
	 * @param fg The group the polynomial is an element of.
	 * @return the polynomial representing the equation
	 * @throws ParseException if the node cannot be converted to a Polynomial
	 */
	public Polynomial calcPolynomial(Node node,FreeGroup fg) throws ParseException
	{
		this.fg = fg;
		return (Polynomial) node.jjtAccept(this,null);
	}

	public Object visit(ASTFunNode node, Object data) throws ParseException {
		int nchild = node.jjtGetNumChildren();
		Polynomial children[] = new Polynomial[nchild];
		for(int i=0;i<nchild;++i)
			children[i]= (Polynomial) node.jjtGetChild(i).jjtAccept(this,data);
			
		Operator op = node.getOperator();
		if(op == null) throw new ParseException("Function "+node.getName()+" cannot be converted to a polynomial");
		if(op == opSet.getAdd())
		{
			return fg.add(children[0],children[1]);
		}
		if(op == opSet.getSubtract())
		{
			return fg.sub(children[0],children[1]);
		}
		if(op == opSet.getMultiply())
		{
			return fg.mul(children[0],children[1]);
		}
		throw new ParseException("Operator "+op.getName()+" not supported");
	}

	public Object visit(ASTVarNode node, Object data) throws ParseException {
		if(fg.getSymbol().equals(node.getName()))
		{
			return fg.getTPoly();
		}
		Variable var = node.getVar();
		if(!var.hasValidValue())
			throw new ParseException("Variable "+var.getName()+" does not have a valid value");
	
		return fg.valueOf(new Number[]{(Number) var.getValue()});
	}

	public Object visit(ASTConstant node, Object data) throws ParseException {
		return fg.valueOf(new Number[]{(Number) node.getValue()});
	}

}
