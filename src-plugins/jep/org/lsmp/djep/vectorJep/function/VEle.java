/* @author rich
 * Created on 15-Nov-2003
 */
package org.lsmp.djep.vectorJep.function;

import java.util.*;
import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * ele(x,i) returns the i-th element of a vector x.
 * ele(m,[i,j]) returns the (i-th,j-th) element of a matrix m. 
 * Note this follows the mathematical indexing convention with indices starting from 1
 * rather than the computer science convention with indices starting from 0.
 * Hence
 * <code>
 * a = [1,2,3,4];
 * ele(a,1); // returns 1
 * m = [[1,2],[3,4]];
 * ele(m,[2,2]); // return 4
 * </code>
 * 
 * New parser feature allow a[] notation to be used.
 * <code>
 * a=[1,2,3,4];
 * a[3]; // returns 3
 * b=[[1,2],[3,4]];
 * b[1,2]; // returns 2
 * </code>
 * 
 * @author Rich Morris
 * Created on 15-Nov-2003
 */
public class VEle extends PostfixMathCommand implements BinaryOperatorI {

	public VEle() {
		super();
		numberOfParameters = 2;
	}

	public Dimensions calcDim(Dimensions ldim, Dimensions rdim)
		throws ParseException {
		return Dimensions.ONE;
	}

	public MatrixValueI calcValue(MatrixValueI res,
		MatrixValueI param1,MatrixValueI param2) throws ParseException
	{
//		Number num = (Number) rhs.getEle(0);
//		res.setEle(0,lhs.getEle(num.intValue()-1));		

		if(param1 instanceof MVector)
		{
			if(param2 instanceof Scaler)
			{
				int index = ((Number) param2.getEle(0)).intValue()-1;
				Object val = ((MVector) param1).getEle(index);
				res.setEle(0,val); 
			}
			else if(param2 instanceof MVector)
			{
				MVector vec = (MVector) param2;
				if(vec.getDim().equals(Dimensions.ONE))
				{
					int d1 = ((Number) vec.getEle(0)).intValue();
					if( d1<1 || d1 > ((MVector) param1).getNumEles())
						throw new ParseException("ArrayAccess: array indices "+d1+" out of range 1.."+param1.getDim());
					Object val = ((MVector) param1).getEle(d1-1);
					res.setEle(0,val);
				}
			}
			else throw new ParseException("Bad second argument to ele, expecting a double "+param2.toString());
		}
		else if(param1 instanceof Matrix)
		{
			if(param2 instanceof MVector)
			{
				MVector vec = (MVector) param2;
				if(vec.getDim().equals(Dimensions.TWO))
				{
					int d1 = ((Number) vec.getEle(0)).intValue();
					int d2 = ((Number) vec.getEle(1)).intValue();
					if( d1<1 || d1 > ((Matrix) param1).getNumRows()
					 ||	d2<1 || d2 > ((Matrix) param1).getNumCols() )
						throw new ParseException("ArrayAccess: array indices "+d1+", "+d2+" out of range 1.."+param1.getDim());

					Object val = ((Matrix) param1).getEle(d1-1,d2-1);
					res.setEle(0,val);
				}
			}
			else throw new ParseException("Bad second argument to ele, expecting [i,j] "+param2.toString());
		}
		else if(param1 instanceof Tensor)
		{
			throw new ParseException("Sorry don't know how to find elements for a tensor");
		}
		else
			throw new ParseException("ele requires a vector matrix or tensor for first argument it has "+param1.toString());
		return res;
	}
	
	public void run(Stack stack) throws ParseException 
	{
		checkStack(stack); // check the stack
	
		Object param1,param2;
	 
		// get the parameter from the stack
	        
		param2 = stack.pop();
		param1 = stack.pop();
	            
		if(param1 instanceof MVector)
		{
			if(param2 instanceof Number)
			{
				Object val = ((MVector) param1).getEle(((Number) param2).intValue()-1);
				stack.push(val);
				return; 
			}
			else if(param2 instanceof MVector)
			{
				MVector vec = (MVector) param2;
				if(vec.getDim().equals(Dimensions.ONE))
				{
					int d1 = ((Number) vec.getEle(0)).intValue();
					if( d1<1 || d1 > ((MVector) param1).getNumEles())
						throw new ParseException("ArrayAccess: array indices "+d1+" out of range 1.."+((MVector) param1).getDim());
					Object val = ((MVector) param1).getEle(d1-1);
					stack.push(val);
					return; 
				}
			}
			throw new ParseException("Bad second argument to ele, expecting a double "+param2.toString());
		}
		else if(param1 instanceof Matrix)
		{
			if(param2 instanceof MVector)
			{
				MVector vec = (MVector) param2;
				if(vec.getDim().equals(Dimensions.TWO))
				{
					int d1 = ((Number) vec.getEle(0)).intValue();
					int d2 = ((Number) vec.getEle(1)).intValue();
					if( d1<1 || d1 > ((Matrix) param1).getNumRows()
					 ||	d2<1 || d2 > ((Matrix) param1).getNumCols() )
						throw new ParseException("ArrayAccess: array indices "+d1+", "+d2+" out of range 1.."+((Matrix) param1).getDim());
					Object val = ((Matrix) param1).getEle(d1-1,d2-1);
					stack.push(val);
					return; 
				}
			}
			else throw new ParseException("Bad second argument to ele, expecting [i,j] "+param2.toString());
		}
		else if(param1 instanceof Tensor)
		{
			throw new ParseException("Sorry don't know how to find elements for a tensor");
		}
		throw new ParseException("ele requires a vector matrix or tensor for first argument it has "+param1.toString());
	}
}
