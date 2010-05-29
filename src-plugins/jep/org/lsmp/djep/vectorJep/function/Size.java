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
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Returns the size of an Scaler, Vector or Matrix.
 * <pre>
 * size(7) -> 1
 * size([1,2,3]) -> 3
 * size([[1,2,3],[4,5,6]]) -> [2,3]
 * size([[[1,2],[3,4],[5,6]],[[7,8],[9,10],[11,12]]]) -> [2,3,2]
 * </pre>
 * @author Rich Morris
 * Created on 13-Feb-2005
 */
public class Size extends PostfixMathCommand implements UnaryOperatorI
{
	public Size()
	{
		super();
		this.numberOfParameters = 1;
	}

	public Dimensions calcDim(Dimensions ldim)
	{
		int rank = ldim.rank();
		if(rank == 0) return Dimensions.ONE;
		return Dimensions.valueOf(rank);
	}

	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI lhs)
		throws ParseException
	{
		Dimensions dims = lhs.getDim();
		if(dims.is0D())
		{
			res.setEle(0,new Integer(1));
			return res;
		}
		for(int i=0;i<dims.rank();++i)
		{
			res.setEle(i,new Integer(dims.getIthDim(i)));
		}
		return res;
	}

	public void run(Stack s) throws ParseException
	{
		Object obj = s.pop();
		MatrixValueI res = null;
		if(obj instanceof Scaler)
		{
			res = Scaler.getInstance(new Integer(1)); 
		}
		else if(obj instanceof MVector)
			res = Scaler.getInstance(new Integer(((MVector) obj).getNumEles()));
		else if(obj instanceof MatrixValueI)
		{
			Dimensions inDim = ((MatrixValueI) obj).getDim();
			res = MVector.getInstance(inDim.rank());
			for(int i=0;i<inDim.rank();++i)
				res.setEle(i,new Integer(inDim.getIthDim(i)));
		}
		else
			res = Scaler.getInstance(new Integer(1));
		s.push(res); 
	}

}
