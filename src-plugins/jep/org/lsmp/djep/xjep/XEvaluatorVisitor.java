package org.lsmp.djep.xjep;

import org.nfunk.jep.*;

/**
 * This class is used for the evaluation of an expression. It uses the Visitor
 * design pattern to traverse the function tree and evaluate the expression
 * using a stack.
 * <p>
 * Function nodes are evaluated by first evaluating all the children nodes,
 * then applying the function class associated with the node. Variable and
 * constant nodes are evaluated by pushing their value onto the stack.

 * <p>
 * Some changes implemented by rjm. Nov 03.
 * Added hook to SpecialEvaluationI.
 * Clears stack before evaluation.
 * Simplifies error handling by making visit methods throw ParseException.
 * Changed visit(ASTVarNode node) so messages not calculated every time. 
 */
public class XEvaluatorVisitor extends EvaluatorVisitor {

	/**
	 * Visit a variable node. The value of the variable is obtained from the
	 * symbol table (symTab) and pushed onto the stack.
	 */
	public Object visit(ASTVarNode node, Object data) throws ParseException {

		Variable var = node.getVar();
		if (var == null) {
			String message = "Could not evaluate " + node.getName() + ": ";
			throw new ParseException(message + " variable not set");
		}
		Object val = null;
		if(var.hasValidValue()) {
			val = var.getValue();
			if (trapNullValues && val == null) {
				String message = "Could not evaluate " + node.getName() + ": null value";
				throw new ParseException(message);
			}
			stack.push(val);
		} 
		else if(var instanceof XVariable)
		{
			Node equation = ((XVariable) var).getEquation();
			if(equation==null)
				throw new ParseException("Cannot find value of "+var.getName()+" no equation.");
			// TODO causes stack overflow if recursive eqn with undefined value is used: recurse = recurse+1
			equation.jjtAccept(this,data);
			val = stack.peek();
			if (trapNullValues && val == null) {
				String message = "Could not evaluate " + node.getName() + ": null value";
				throw new ParseException(message);
			}
		}
		else
		{
			throw new ParseException("Could not evaluate " + node.getName() + ": value not set");
		}

		return data;
	}
}
