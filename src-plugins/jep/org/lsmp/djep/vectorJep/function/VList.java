/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.lsmp.djep.vectorJep.function;

import java.util.*;
import org.nfunk.jep.*;
import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.values.*;

/**
 * A enhanced version of List, allows matrices and tensors.
 * During evaluation this function converts lists of values into the appropriate 
 * @see org.lsmp.djep.vectorJep.values.MatrixValueI MatrixValueI
 * type.
 * 
 * @author Rich Morris
 * Created on 27-Nov-2003
 */
public class VList extends org.nfunk.jep.function.List 
	implements NaryOperatorI
{
	public VList()
	{
		numberOfParameters = -1;
	}

	/** Calculates the dimension of this node with given dimensions of children. */
	public Dimensions calcDim(Dimensions dims[]) throws ParseException
	{
		return Dimensions.valueOf(dims.length,dims[0]);
	}
	
	/** Calculates the value of this node.
	 * 
	 */
	public MatrixValueI calcValue(MatrixValueI res,
		MatrixValueI inputs[]) throws ParseException
	{
		int eleSize = inputs[0].getNumEles();
		for(int i=0;i<inputs.length;++i)
		{
			for(int j=0;j<eleSize;++j)
			{
				res.setEle(i*eleSize+j,inputs[i].getEle(j));
			}
		}
		return res;
	}
	
	public void run(Stack inStack) throws ParseException 
	{
		checkStack(inStack); // check the stack
		if(curNumberOfParameters <1)
			throw new ParseException("Empty list");
		Object param1 = inStack.pop();
		
		if(param1 instanceof Vector)
		{
			Vector vec1 = (Vector) param1;
			int rows = curNumberOfParameters;
			int cols = vec1.size();
			Matrix res = (Matrix) Matrix.getInstance(rows,cols);
			for(int j=0;j<cols;++j)
				res.setEle(rows-1,j,vec1.elementAt(j));					
			for(int i=rows-2;i>=0;--i)
			{
				Vector vec = (Vector) inStack.pop();
				for(int j=0;j<cols;++j)
					res.setEle(i,j,vec.elementAt(j));					
			}
			inStack.push(res);
			return;
		}
		else if(param1 instanceof MatrixValueI)
		{
			MatrixValueI mat1 = (MatrixValueI) param1;
			int rows = curNumberOfParameters;
			int neles = mat1.getNumEles();
			MatrixValueI res = Tensor.getInstance(Dimensions.valueOf(rows,mat1.getDim()));
			for(int j=0;j<neles;++j)
				res.setEle((rows-1)*neles+j,mat1.getEle(j));				
			for(int i=rows-2;i>=0;--i)
			{
				MatrixValueI mat = (MatrixValueI) inStack.pop();
				for(int j=0;j<neles;++j)
					res.setEle(i*neles+j,mat.getEle(j));				
			}
			inStack.push(res);
			return;
		}
		else
		{
			MVector res = new MVector(curNumberOfParameters);
			res.setEle(curNumberOfParameters-1,param1);
			for(int i=curNumberOfParameters-2;i>=0;--i)
			{
				Object param = inStack.pop();
				res.setEle(i,param);
			}
			inStack.push(res);
			return;
		}
	}
}
