/* @author rich
 * Created on 27-Jul-2003
 */
package org.lsmp.djep.vectorJep.function;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.Subtract;
/**
 * An extension of the Add command to allow it to add MVector's and Matrix's.
 * @author Rich Morris
 * Created on 27-Jul-2003
 */
public class MSubtract extends Subtract implements BinaryOperatorI {

	public Dimensions calcDim(Dimensions ldim,Dimensions rdim)
	{
		if(ldim.equals(rdim)) return ldim;
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
		int len = res.getNumEles();
		for(int i=0;i<len;++i)
			res.setEle(i,super.sub(lhs.getEle(i),rhs.getEle(i)));
		return res;
	}
	/**
	 * Adds two objects.
	 */
	
	public Object sub(Object param1, Object param2) throws ParseException 
	{
		if(param1 instanceof MVector && param2 instanceof MVector)
			return sub((MVector) param1,(MVector) param2);
		if(param1 instanceof Matrix && param2 instanceof Matrix)
			return sub((Matrix) param1,(Matrix) param2);
		else if(param1 instanceof Tensor && param2 instanceof Tensor)
			return sub((Tensor) param1,(Tensor) param2);
		else
			return super.sub(param1,param2);
	}
	
	/** Adds two vectors. */
	public MVector sub(MVector lhs,MVector rhs) throws ParseException
	{
		if(lhs.getNumEles() != rhs.getNumEles()) throw new ParseException("Miss match in sizes ("+lhs.getNumEles()+","+rhs.getNumEles()+") when trying to add vectors!");
		MVector res = new MVector(lhs.getNumEles());
		return (MVector) calcValue(res,lhs,rhs);			
	}

	/** Adds two matrices. */
	public Matrix sub(Matrix lhs,Matrix rhs) throws ParseException
	{
		if(lhs.getNumRows() != rhs.getNumRows()) throw new ParseException("Miss match in number of rows ("+lhs.getNumRows()+","+rhs.getNumRows()+") when trying to add vectors!");
		if(lhs.getNumCols() != rhs.getNumCols()) throw new ParseException("Miss match in number of cols ("+lhs.getNumCols()+","+rhs.getNumCols()+") when trying to add vectors!");
		Matrix res = (Matrix) Matrix.getInstance(lhs.getNumRows(),lhs.getNumCols());
		return (Matrix) calcValue(res,lhs,rhs);			
	}

	/** Adds two matrices. */
	public Tensor sub(Tensor lhs,Tensor rhs) throws ParseException
	{
		if(lhs.getNumEles() != rhs.getNumEles()) throw new ParseException("Miss match in sizes ("+lhs.getNumEles()+","+rhs.getNumEles()+") when trying to add vectors!");
		Tensor res = new Tensor(lhs.getDim());
		return (Tensor) calcValue(res,lhs,rhs);			
	}
}
