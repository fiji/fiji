/* @author rich
 * Created on 26-Nov-2003
 */
package org.lsmp.djep.vectorJep.function;

import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.*;
import java.util.*;
/**
 * An overloaded power function, if both arguments are vectors returns
 * the exteriour product, else return standard power.
 * @author Rich Morris
 * Created on 26-Nov-2003
 */
public class VPower extends PostfixMathCommand implements BinaryOperatorI 
{
	private static Power pow = new Power();
	private static ExteriorProduct cross = new ExteriorProduct();

	public VPower() {
		super();
		this.numberOfParameters = 2;
	}
	public Dimensions calcDim(Dimensions ldim,Dimensions rdim) throws ParseException
	{
		if(ldim.equals(Dimensions.ONE) && rdim.equals(Dimensions.ONE))
			return Dimensions.ONE;
		if(ldim.equals(Dimensions.THREE) && rdim.equals(Dimensions.THREE))
			return Dimensions.THREE;
		throw new ParseException("Power: both sides must be either 0 dimensional or 3D vectors");
	}

	public MatrixValueI calcValue(
		MatrixValueI res,
		MatrixValueI lhs,
		MatrixValueI rhs) throws ParseException
	{
		if(lhs.getDim().equals(Dimensions.ONE)
		 && rhs.getDim().equals(Dimensions.ONE))
		{
			res.setEle(0,pow.power(lhs.getEle(0),rhs.getEle(0)));
			return res;
		}
		if(lhs.getDim().equals(Dimensions.THREE)
		 && rhs.getDim().equals(Dimensions.THREE))
		{
			return cross.calcValue(res,lhs,rhs);
		}
		throw new ParseException("Power: both sides must be either 0 dimensional or 3D vectors");
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		if(param1 instanceof MVector && param2 instanceof MVector)
			inStack.push(cross.crosspower(param1, param2));
		else 
			inStack.push(pow.power(param1,param2));
	}

}
