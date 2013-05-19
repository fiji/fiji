/* @author rich
 * Created on 30-Oct-2003
 */
package org.lsmp.djep.matrixJep;
import org.nfunk.jep.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.lsmp.djep.vectorJep.function.*;
import org.lsmp.djep.vectorJep.values.*;
import org.lsmp.djep.vectorJep.Dimensions;
import org.nfunk.jep.function.*;
import java.util.Stack;

/**
 * This visitor evaluates a the tree representing the equation.
 * 
 * @author Rich Morris
 * Created on 30-Oct-2003
 * @since 2.3.2 Hack so comparative operations work with vectors and matrices. 
 */
public class MatrixEvaluator implements ParserVisitor,EvaluatorI
{
//	private DimensionCalculator dimCalc;
	private Stack stack = new Stack();
	private MatrixJep mjep;
	public MatrixValueI evaluate(MatrixNodeI node,MatrixJep mj) throws ParseException
	{
		this.mjep=mj;
		return (MatrixValueI) node.jjtAccept(this,null);
	}

	public Object eval(Node node) throws ParseException {
		MatrixValueI val = (MatrixValueI) node.jjtAccept(this,null);
		return val.copy();
	}

	public Object visit(SimpleNode node, Object data)	{ return null;	}
	public Object visit(ASTStart node, Object data)	{ return null;	}

	/** constants **/
	public Object visit(ASTConstant node, Object data)
	{
		return ((ASTMConstant) node).getMValue();
	}
	/** multi dimensional differentiable variables */
	public Object visit(ASTVarNode node, Object data) throws ParseException
	{
		MatrixVariableI var = (MatrixVariableI) node.getVar();
		if(var.hasValidValue())
			return var.getMValue();
		if(!var.hasEquation())
			throw new ParseException("Tried to evaluate a variable with an invalid value but no equation");
		MatrixValueI res = (MatrixValueI) var.getEquation().jjtAccept(this,data);
		var.setMValue(res);
		return res;
	}
	
	/** other functions **/
	public Object visit(ASTFunNode node, Object data) throws ParseException
	{
		MatrixNodeI mnode = (MatrixNodeI) node;
		PostfixMathCommandI pfmc = node.getPFMC(); 
		if(pfmc instanceof MatrixSpecialEvaluationI)
		{
			MatrixSpecialEvaluationI se = (MatrixSpecialEvaluationI) pfmc;
			return se.evaluate(mnode,this,mjep);
		}
		else if (pfmc instanceof CallbackEvaluationI) {
			Object val = ((CallbackEvaluationI) pfmc).evaluate(node,this);
			if(val instanceof MatrixValueI)
				mnode.getMValue().setEles((MatrixValueI) val);
			else
				mnode.getMValue().setEle(0,val);
			return mnode.getMValue();
		}
		else if (pfmc instanceof SpecialEvaluationI) {
			
			throw new ParseException("Encountered an instance of SpecialEvaluationI");
		}
		else if(pfmc instanceof BinaryOperatorI)
		{
			BinaryOperatorI bin = (BinaryOperatorI) pfmc;
			MatrixValueI lhsval = (MatrixValueI) node.jjtGetChild(0).jjtAccept(this,data);
			MatrixValueI rhsval = (MatrixValueI) node.jjtGetChild(1).jjtAccept(this,data);
			return bin.calcValue(mnode.getMValue(),lhsval,rhsval);
		}
		else if(pfmc instanceof UnaryOperatorI)
		{
			UnaryOperatorI uni = (UnaryOperatorI) pfmc;
			MatrixValueI val = (MatrixValueI) node.jjtGetChild(0).jjtAccept(this,data);
			return uni.calcValue(mnode.getMValue(),val);
		}
		else if(pfmc instanceof NaryOperatorI)
		{
			NaryOperatorI uni = (NaryOperatorI) pfmc;
			MatrixValueI results[] = new MatrixValueI[node.jjtGetNumChildren()];
			for(int i=0;i<results.length;++i)
				results[i] = (MatrixValueI) node.jjtGetChild(i).jjtAccept(this,data);
			return uni.calcValue(mnode.getMValue(),results);
		}
		else if(pfmc instanceof Comparative) {
			Object lhsval = (MatrixValueI) node.jjtGetChild(0).jjtAccept(this,data);
			Object rhsval = (MatrixValueI) node.jjtGetChild(1).jjtAccept(this,data);
			stack.push(lhsval);
			stack.push(rhsval);
			pfmc.setCurNumberOfParameters(2);
			pfmc.run(stack);
			mnode.getMValue().setEle(0,stack.pop());
			return mnode.getMValue();
		}

		// not a clever op use old style call
		// assumes 
		int num = node.jjtGetNumChildren();
		for(int i=0;i<num;++i)
		{
			MatrixValueI vec = (MatrixValueI) node.jjtGetChild(i).jjtAccept(this,data);
			if(!vec.getDim().equals(Dimensions.ONE))
				throw new ParseException("Arguments of "+node.getName()+" must be scalers");			
			stack.push(vec.getEle(0));
		}
		pfmc.setCurNumberOfParameters(num);
		pfmc.run(stack);
		mnode.getMValue().setEle(0,stack.pop());
		return mnode.getMValue();
	}
}
