/*
Created 26-May-2006 - Richard Morris
*/
package org.nfunk.jep.function;

import java.util.Stack;
import java.util.Vector;

import org.nfunk.jep.ASTVarNode;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.Variable;

/**
 * Function which allows array access using the a[3] notation on left and right hand side.
 * <code>
 * a=[4,3,2,1];
 * a[2]; // returns 2
 * a[2]=5; // a is now [4,5,2,1]
 * </code>
 * @author Richard Morris
 */
public class Ele extends PostfixMathCommand implements LValueI {

	/**
	 * 
	 */
	public Ele() {
		numberOfParameters = 2;
	}

	public void set(EvaluatorI pv, Node node, Object value)
			throws ParseException {
		Node lhsNode = node.jjtGetChild(0);

		if(!(lhsNode instanceof ASTVarNode))
			throw new ParseException("Ele: lhs must be a variable");
		ASTVarNode vn = (ASTVarNode) lhsNode;
		Variable var = vn.getVar();
		Object rhs = pv.eval(node.jjtGetChild(1));
		int index=-1;
		if(rhs instanceof Number)
		{
			index=((Number) rhs).intValue()-1;
		}
		else if(rhs instanceof Vector)
		{
			Vector vec = (Vector) rhs;
			if(vec.size()!=1) throw new ParseException("Ele: only single dimension arrays supported in JEP");
			index = ((Number) vec.firstElement()).intValue()-1;
		}
		else throw new ParseException("Ele: rhs must be a number");
		Object oldVarVal = var.getValue();
		if(!(oldVarVal instanceof Vector))
			throw new ParseException("Ele: the value of the variable must be a Vector");
		Vector newVarVal = (Vector) ((Vector) oldVarVal).clone();
		newVarVal.set(index,value);
		var.setValue(newVarVal);
	}

	public void run(Stack s) throws ParseException {
		checkStack(s);// check the stack
		Object rhs = s.pop();
		Object lhs = s.pop();
		if(!(lhs instanceof Vector)) throw new ParseException("Ele: lhs must be an instance of Vector");

		if(rhs instanceof Number)
		{
			int index = ((Number) rhs).intValue();
			Object val = ((Vector) lhs).elementAt(index-1);
			s.push(val);
			return;
		}
		if(rhs instanceof Vector)
		{
			Vector vec = (Vector) rhs;
			if(vec.size()!=1) throw new ParseException("Ele: only single dimension arrays supported in JEP");
			int index = ((Number) vec.firstElement()).intValue();
			Object val = ((Vector) lhs).elementAt(index-1);
			s.push(val);
			return;
			
		}
		throw new ParseException("Ele: only single dimension arrays supported in JEP");
	}

}
