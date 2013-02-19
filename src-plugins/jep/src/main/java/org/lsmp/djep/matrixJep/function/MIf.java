/* @author rich
 * Created on 18-Nov-2003
 */
package org.lsmp.djep.matrixJep.function;
import org.nfunk.jep.function.*;
import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.function.NaryOperatorI;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.*;

/**
 * The if(condExpr,posExpr,negExpr) function.
 * The value of trueExpr will be returned if condExpr is >0 (true)
 * and value of negExpr will be returned if condExpr is &lt;= 0 (false).
 * <p>
 * This function performs lazy evaluation so that
 * only posExpr or negExpr will be evaluated.
 * For Complex numbers only the real part is used.
 * <p>
 * An alternate form if(condExpr,posExpr,negExpr,zeroExpr)
 * is also available. Note most computations
 * are carried out over floating point doubles so
 * testing for zero can be dangerous.
 * <p>
 * This function implements the SpecialEvaluationI interface
 * so that it handles setting the value of a variable. 
 * @author Rich Morris
 * Created on 18-Nov-2003
 */
public class MIf extends PostfixMathCommand implements NaryOperatorI, MatrixSpecialEvaluationI 
{
	public MIf() {
		super();
		numberOfParameters = -1;
	}

	/** Find the dimension of this node. */
	public Dimensions calcDim(Dimensions dims[]) throws ParseException
	{
		int num =dims.length; 
		if( num < 3 || num > 4)
			throw new ParseException("If operator must have 3 or 4 arguments.");

		Dimensions condDim = dims[0];
		if(!condDim.equals(Dimensions.ONE))
			throw new ParseException("First argument of if opperator must be 0 dimensional");
		Dimensions posDim = dims[1];
		for(int i=2;i<num;++i)
			if(!posDim.equals(dims[i]))
				throw new ParseException("Dimensions for each argument of if must be equal");		
		return posDim;
	}

	/** This method should not be called.
	 * Use {@link #evaluate} instead.
	 */
	public MatrixValueI calcValue(MatrixValueI res,
		MatrixValueI inputs[]) throws ParseException
	{
		throw new ParseException("Called calc value for If");
	}

	/**
	 * Evaluate the node, uses lazy evaluation.
	 */
	public MatrixValueI evaluate(MatrixNodeI node,MatrixEvaluator visitor,MatrixJep j) throws ParseException
	{
		int num =node.jjtGetNumChildren(); 
		if( num < 3 || num > 4)
			throw new ParseException("If operator must have 3 or 4 arguments.");

		// get value of argument
		
		Scaler cond = (Scaler) node.jjtGetChild(0).jjtAccept(visitor,null);	
//		Object condVal = cond.getEle(0);		
		// convert to double
		double val = cond.doubleValue();
		MatrixValueI res;
		if(val>0.0)
		{
			res = (MatrixValueI) node.jjtGetChild(1).jjtAccept(visitor,null);
		}
		else if(num ==3 || val <0.0)
		{
			res = (MatrixValueI) node.jjtGetChild(2).jjtAccept(visitor,null);
		}
		else
		{
			res = (MatrixValueI) node.jjtGetChild(3).jjtAccept(visitor,null);
		}
		MatrixValueI mvalue = node.getMValue();
		mvalue.setEles(res);
		return mvalue;
	}

	public boolean checkNumberOfParameters(int n) {
		return (n == 3 || n == 4);
	}
	
	
}
