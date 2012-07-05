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
import org.nfunk.jep.function.*;

/**
 * An overloaded operator, either cross product or power.
 * If the arguments are 3D vectors then treat as cross product.
 * Otherwise treet as power.
 * @author Rich Morris
 * Created on 27-Jul-2003
 */
public class ExteriorProduct extends PostfixMathCommand implements BinaryOperatorI
{
	private Subtract sub = new Subtract();
	private Multiply mul = new Multiply();
	
	public ExteriorProduct()
	{
		numberOfParameters = 2;
	}

	public Dimensions calcDim(Dimensions ldim,Dimensions rdim) throws ParseException
	{
		if(ldim.equals(Dimensions.THREE)&&rdim.equals(Dimensions.THREE))
			return Dimensions.THREE;
		throw new ParseException("^ only implemented for three dimensions vectors");
	}

	public MatrixValueI calcValue(
		MatrixValueI res,
		MatrixValueI lhs,
		MatrixValueI rhs) throws ParseException
	{
		res.setEle(0,sub.sub(
				mul.mul(lhs.getEle(1),rhs.getEle(2)),
				mul.mul(lhs.getEle(2),rhs.getEle(1))));
		res.setEle(1,sub.sub(
				mul.mul(lhs.getEle(2),rhs.getEle(0)),
				mul.mul(lhs.getEle(0),rhs.getEle(2))));
		res.setEle(2,sub.sub(
				mul.mul(lhs.getEle(0),rhs.getEle(1)),
				mul.mul(lhs.getEle(1),rhs.getEle(0))));
		return res;
		
	}

	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		inStack.push(crosspower(param1, param2));
	}
	
	public Object crosspower(Object param1, Object param2)
		throws ParseException
	{
		if(param1 instanceof MVector && param2 instanceof MVector)
			return exteriorProduct((MVector) param1,(MVector) param2);
		throw new ParseException("Sorry: can currently only do cross product on 3D vectors");
	}
	
	public Object exteriorProduct(MVector lhs, MVector rhs) throws ParseException
	{
		if(!lhs.getDim().equals(Dimensions.THREE)
		 || !lhs.getDim().equals(Dimensions.THREE) )
		 	throw new ParseException("Cross: Miss match in sizes ("+lhs.getDim()+","+rhs.getDim()+")");
		MVector res = new MVector(3);
		return calcValue(res,lhs,rhs);
	}
}
