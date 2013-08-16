/* @author rich
 * Created on 10-Dec-2004
 */
package org.lsmp.djep.vectorJep.function;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * Multiplies any number of Vectors or Matricies element by element.
 * TODO could be much more efficient when called through run.
 * 
 * @author Rich Morris
 * Created on 10-Dec-2004
 */
public class ElementMultiply extends Multiply implements NaryOperatorI {

	public Dimensions calcDim(Dimensions[] dims) throws ParseException {
		int len = dims.length;
		if(len==0) throw new ParseException("ElementMultiply called with 0 arguments");
		if(len==1) return dims[0];
		
		Dimensions firstDim = dims[0];
		for(int i=1;i<len;++i)
			if(!firstDim.equals(dims[i]))
				throw new ParseException("ElementMultiply: dimensions of each argument should be the same");
		
		return firstDim;
	}

	/**
	 * Multiply the inputs element by element putting the results in res.
	 */
	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI[] inputs)
		throws ParseException {

			int numArgs = inputs.length;
			int len = res.getNumEles();
			for(int i=0;i<len;++i)
			{
				Object ele = inputs[0].getEle(i);
				for(int j=1;j<numArgs;++j)
					ele = super.mul(ele,inputs[j].getEle(i));
				res.setEle(i,ele);
			}
			return res;
	}

	/**
	 * Multiply arguments element by element. Returns result.
	 */
	public Object mul(Object param1, Object param2) throws ParseException {

		if(param1 instanceof MatrixValueI && param2 instanceof MatrixValueI)
		{
			return mul((MatrixValueI) param1,(MatrixValueI) param2);
		}
		else if(param1 instanceof MatrixValueI)
		{
			MatrixValueI l = (MatrixValueI) param1;
			MatrixValueI res = Tensor.getInstance(l.getDim());
			for(int i=0;i<res.getNumEles();++i)
				res.setEle(i,super.mul(l.getEle(i),param2));
			return res;
		}
		else if(param2 instanceof MatrixValueI)
		{
			MatrixValueI r = (MatrixValueI) param2;
			MatrixValueI res = Tensor.getInstance(r.getDim());
			for(int i=0;i<res.getNumEles();++i)
				res.setEle(i,super.mul(param1,r.getEle(i)));
			return res;
		}
		return super.mul(param1,param2);
	}

	public Object mul(MatrixValueI param1, MatrixValueI param2) throws ParseException 
	{
		Dimensions dims = this.calcDim(new Dimensions[]{param1.getDim(),param2.getDim()});
		MatrixValueI res = Tensor.getInstance(dims);
		return this.calcValue(res,new MatrixValueI[]{param1,param2});
	}

}
