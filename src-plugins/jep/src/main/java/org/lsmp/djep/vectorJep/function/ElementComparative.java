/* @author rich
 * Created on 10-Dec-2004
 */
package org.lsmp.djep.vectorJep.function;
import java.util.Stack;

import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * Multiplies any number of Vectors or Matrices element by element.
 * TODO could be much more efficient when called through run.
 * 
 * @author Rich Morris
 * Created on 10-Dec-2004
 */
public class ElementComparative extends Comparative implements BinaryOperatorI {

	public ElementComparative(int index) {super(index);}
	public Dimensions calcDim(Dimensions ldim,Dimensions rdim)
	{
		if(ldim.equals(rdim)) return ldim;
		return null;
	}

	/**
	 * Multiply the inputs element by element putting the results in res.
	 */
	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI lhs,MatrixValueI rhs)
		throws ParseException {

			int len = res.getNumEles();
			for(int i=0;i<len;++i)
			{
				boolean val=false;
				switch(id)
				{
				case LT: val = lt(lhs.getEle(i),rhs.getEle(i)); break;
				case GT: val = gt(lhs.getEle(i),rhs.getEle(i)); break;
				case LE: val = le(lhs.getEle(i),rhs.getEle(i)); break;
				case GE: val = ge(lhs.getEle(i),rhs.getEle(i)); break;
				case NE: val = ne(lhs.getEle(i),rhs.getEle(i)); break;
				case EQ: val = eq(lhs.getEle(i),rhs.getEle(i)); break;
				}
				res.setEle(i,val?new Double(1):new Double(0));
			}
			return res;
	}
	public void run(Stack inStack) throws ParseException {
		Object rhsObj = inStack.pop(); 
		Object lhsObj = inStack.pop();
		if(lhsObj instanceof MatrixValueI && rhsObj instanceof MatrixValueI)
		{
			MatrixValueI lhs = (MatrixValueI) lhsObj;
			MatrixValueI rhs = (MatrixValueI) rhsObj;
			if(!lhs.getDim().equals(rhs.getDim()))
				throw new ParseException("ElementComparative: dimensions of both sides must be equal");
			Dimensions dims = this.calcDim(lhs.getDim(),lhs.getDim());
			MatrixValueI res = Tensor.getInstance(dims);
			calcValue(res,lhs,rhs);
			inStack.push(res);
		}
		else
			throw new ParseException("ElementComparative: arguments must be a Matric or Vector type");
	}


}
