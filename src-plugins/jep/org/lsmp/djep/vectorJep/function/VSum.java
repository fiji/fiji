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
 * Adds the elements of a vector or matrix.
 * vsum([1,2,3]) -> 6
 * vsum([[1,2],[3,4]]) -> 10
 * 
 * @author Rich Morris
 * Created on 13-Feb-2005
 */
public class VSum extends PostfixMathCommand implements UnaryOperatorI
{
	Add add = new Add();
	
	public VSum()
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
			throw new ParseException("vsum: result must be a scaler");

		Object val = lhs.getEle(0);
			for(int i=1;i<lhs.getNumEles();++i)
				val = add.add(val,lhs.getEle(i));
			res.setEle(0,val);
		
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
