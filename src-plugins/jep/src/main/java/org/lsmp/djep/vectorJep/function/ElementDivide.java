/* @author rich
 * Created on 10-Dec-2004
 */
package org.lsmp.djep.vectorJep.function;
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
public class ElementDivide extends Divide implements BinaryOperatorI {

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
				res.setEle(i,super.div(lhs.getEle(i),rhs.getEle(i)));
			}
			return res;
	}

	/**
	 * Multiply arguments element by element. Returns result.
	 */
	public Object div(Object param1, Object param2) throws ParseException {

		if(param1 instanceof MatrixValueI && param2 instanceof MatrixValueI)
		{
			return div((MatrixValueI) param1,(MatrixValueI) param2);
		}
		else if(param1 instanceof MatrixValueI)
		{
			MatrixValueI l = (MatrixValueI) param1;
			MatrixValueI res = Tensor.getInstance(l.getDim());
			for(int i=0;i<res.getNumEles();++i)
				res.setEle(i,super.div(l.getEle(i),param2));
			return res;
		}
		else if(param2 instanceof MatrixValueI)
		{
			MatrixValueI r = (MatrixValueI) param2;
			MatrixValueI res = Tensor.getInstance(r.getDim());
			for(int i=0;i<res.getNumEles();++i)
				res.setEle(i,super.div(param1,r.getEle(i)));
			return res;
		}
		return super.div(param1,param2);
	}

	public Object div(MatrixValueI param1, MatrixValueI param2) throws ParseException 
	{
		Dimensions dims = this.calcDim(param1.getDim(),param2.getDim());
		MatrixValueI res = Tensor.getInstance(dims);
		return this.calcValue(res,param1,param2);
	}

}
