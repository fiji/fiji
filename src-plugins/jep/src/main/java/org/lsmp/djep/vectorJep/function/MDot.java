/*****************************************************************************

JEP - Java Math Expression Parser 2.24
	  December 30 2002
	  (c) Copyright 2002, Nathan Funk
	  See LICENSE.txt for license information.

*****************************************************************************/
package org.lsmp.djep.vectorJep.function;

import java.util.*;

import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;

import org.nfunk.jep.*;

/**
 * The MDot operator. 
 * @author Rich Morris
 * Created on 23-Feb-2004
 */
public class MDot extends MMultiply implements BinaryOperatorI
{
	public MDot() {
		numberOfParameters = 2;
	}
	public Dimensions calcDim(Dimensions l,Dimensions r) {
		if(l.equals(r) && l.is1D()) return Dimensions.ONE;
		return null;
	}
	
	/** calculates the value.
	 * @param res - results will be stored in this object
	 * @param lhs - lhs value
	 * @param rhs - rhs value
	 * @return res
	 */
	public MatrixValueI calcValue(MatrixValueI res,MatrixValueI lhs,MatrixValueI rhs) throws ParseException
	{
		return calcValue((Scaler) res,(MVector) lhs,(MVector) rhs);
	}

	public Scaler calcValue(Scaler res,MVector lhs,MVector rhs) throws ParseException
	{
		int len = lhs.getNumEles();
		Object val = mul(lhs.getEle(0),rhs.getEle(0));
		for(int i=1;i<len;++i)
			val = add.add(val,mul(lhs.getEle(i),rhs.getEle(i)));
		res.setEle(0,val);
		return res;
	}

	public void run(Stack stack) throws ParseException 
	{
		checkStack(stack); // check the stack

		Object param1,param2;
 
		// get the parameter from the stack
        
		param2 = stack.pop();
		param1 = stack.pop();
            
			// multiply it with the product
                
		stack.push(dot(param1, param2));

		return;
	}

	/** returns param1 . param2. Defaults to scaler multiplication if parameters are not vectors. */
	public Object dot(Object param1, Object param2)	throws ParseException
	{
		if(param1 instanceof MVector && param2 instanceof MVector)
			return dot((MVector) param1,(MVector) param2);
		return super.mul(param1,param2);
	}
	
	/** returns lhs . rhs */
	public Object dot(MVector lhs, MVector rhs) throws ParseException
	{
		if(!lhs.getDim().equals(rhs.getDim())) throw new ParseException("Dot: Miss match in sizes ("+lhs.getDim()+","+rhs.getDim()+")");
		Scaler res = (Scaler) Scaler.getInstance(new Double(0.0));
		calcValue(res,lhs,rhs);
		return res.getEle(0);
	}
}
