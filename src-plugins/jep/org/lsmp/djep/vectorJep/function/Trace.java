/* @author rich
 * Created on 13-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.vectorJep.function;

import java.util.Stack;

import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.*;

/**
 * Calculate the trace of a matrix
 * trace([[1,2],[3,4]]) -> 1+4 = 5
 * 
 * @author Rich Morris
 * Created on 13-Feb-2005
 */
public class Trace extends PostfixMathCommand implements UnaryOperatorI
{
	Add add = new Add();
	Multiply mul = new Multiply();
	
	public Trace()
	{
		super();
		this.numberOfParameters = 1;
	}

	public Dimensions calcDim(Dimensions ldim)
	{
		return Dimensions.ONE;
	}

	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI lhs)
		throws ParseException
	{
		if(!(res instanceof Scaler))
			throw new ParseException("trace: result must be a scaler");
		if(!(lhs instanceof Matrix))
			throw new ParseException("trace: argument must be a matrix");
		Matrix mat = (Matrix) lhs;
		if( mat.getNumRows()!= mat.getNumCols())
		 	throw new ParseException("trace: argument must be a square matrix "+mat);

		if(mat.getNumRows() == 2)
		{
			res.setEle(0,add.add(mat.getEle(0,0),mat.getEle(1,1)));
		}
		else if(mat.getNumRows() == 3)
		{	
			res.setEle(0,add.add(mat.getEle(0,0),add.add(mat.getEle(1,1),mat.getEle(2,2))));
		}
		else
		{
			Object val = mat.getEle(0,0);
			for(int i=1;i<mat.getNumRows();++i)
				val = add.add(val,mat.getEle(i,i));
			res.setEle(0,val);
		}
		return res;
	}
	public void run(Stack s) throws ParseException
	{
		MatrixValueI obj = (MatrixValueI) s.pop();
		MatrixValueI res = Scaler.getInstance(new Double(0.0));
		calcValue(res,obj);
		s.push(res);
	}

}
