/* @author rich
 * Created on 18-Nov-2003
 */
package org.lsmp.djep.xjep.function;

import java.util.Stack;

import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * Base class for functions like Sum(x^2,x,1,10) which finds the sum of x^2 with x running from 1 to 10.
 * The first argument should be an equation, the second argument is a variable name,
 * the third argument is the min value, the forth is the max value and the 
 * fifth argument (if present, default 1) is the increment to use.
 * Sub classes should implement the 
 * <pre>public abstract Object evaluate(Object elements[]) throws ParseException;</pre>
 * method, which is passed an array of the value 

 * @author Rich Morris
 * Created on 10-Sept-2004
 */
public abstract class SumType extends PostfixMathCommand implements CallbackEvaluationI {
	/** The name of the function, use in error reporting. */
	protected String name;

	public SumType(String funName)
	{
		numberOfParameters = -1;
		name = funName;
	}

	public SumType()
	{
		numberOfParameters = -1;
	}

	public boolean checkNumberOfParameters(int n) {
		return (n==4||n==5);
	}

	/**
	 * Evaluates the operator in given context. 
	 * Typically does not need to be sub-classed as the other evaluate methods are more useful. This method just checks the arguments.
	 */
	public Object evaluate(Node node,EvaluatorI pv) throws ParseException {

		int numParams =  node.jjtGetNumChildren();
		if(!checkNumberOfParameters(numParams))
			throw new ParseException(name+": called with invalid number of parameters: "+numParams+" it should be either 4 or 5.");
			
		Node varNode = node.jjtGetChild(1);
		Variable var=null;
		if(varNode instanceof ASTVarNode)
			var = ((ASTVarNode) varNode).getVar();
		else
			throw new ParseException(name+": second argument should be a variable");
			
		Object minObj = pv.eval(node.jjtGetChild(2));
		double min;
		if(minObj instanceof Number)
			min = ((Number ) minObj).doubleValue();
		else throw new ParseException(name+": third argument (min) should evaluate to a number it is "+minObj.toString());
			
		Object maxObj = pv.eval(node.jjtGetChild(3));
		double max;
		if(maxObj instanceof Number)
			max = ((Number ) maxObj).doubleValue();
		else throw new ParseException(name+": forth argument (max) should evaluate to a number it is "+minObj.toString());

		if(min>max) throw new ParseException(name+": min value should be smaller than max value they are "+min+" and "+max+".");
		
		if(numParams == 5)
		{
			//node.jjtGetChild(3).jjtAccept(pv,data);	
			//checkStack(stack); // check the stack
			//Object incObj = stack.pop();
			Object incObj = pv.eval(node.jjtGetChild(4));
			double inc;
			if(incObj instanceof Number)
				inc = ((Number ) incObj).doubleValue();
			else throw new ParseException(name+": fifth argument (steps) should evaluate to a number it is "+minObj.toString());
			
			return evaluate(node.jjtGetChild(0),var,min,max,inc,pv);
		}
		return evaluate(node.jjtGetChild(0),var,min,max,1.0,pv);
	}

	/** Evaluates the node by repeatibly setting the value of the variable from min to max, and calculating the value of the first argument.
	 * Sub classes generally do not need to implement this method as
	 * {@link #evaluate(Object[])}
	 * is more useful. If they do they should follow the pattern used here. 
	 * 
	 * @param node
	 * @param var
	 * @param min
	 * @param max
	 * @param inc
	 * @param pv
	 * @return the result of evaluation
	 * @throws ParseException
	 */ 

	public Object evaluate(
		Node node,
		Variable var,
		double min, double max, double inc,
		EvaluatorI pv)
		throws ParseException {
			
			int i=0;
			double val;
			Object[] res=new Object[(int) ((max-min)/inc)+1];	
			for(i=0,val=min;val<=max;++i,val=min+i*inc)
			{
				var.setValue(new Double(val));
				
				res[i] = pv.eval(node);
			}
			Object ret = evaluate(res);
			return ret;
	}
		
	/** Evaluates the function given the set of y values.
	 * For example for Sum(x^2,x,1,5) the function will be passed the array [1,4,9,16,25].
	 * 
	 * @param elements the y values
	 * @return the result of the function
	 * @throws ParseException
	 */
	public abstract Object evaluate(Object elements[]) throws ParseException;
	
	/**
	 * run method. Should not be called.
	 */
	public void run(Stack s) throws ParseException {
		throw new ParseException(name+": run method called should not normally happen.");
	}

}
