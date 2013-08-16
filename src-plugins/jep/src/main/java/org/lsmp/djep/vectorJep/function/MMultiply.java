/* @author rich
 * Created on 27-Jul-2003
 */
package org.lsmp.djep.vectorJep.function;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import java.util.*;

/**
 * An extension of the Multiply to with vectors and matricies.
 * Must faster (1/3) if used with MatrixJep and calcValue routines.
 * Note vector * vector treated as col_vec * row_vec -> matrix.
 *  
 * @author Rich Morris
 * Created on 27-Jul-2003
 * TODO add handeling of tensors
 * @since 2.3.2 Improved error reporting
 */
public class MMultiply extends Multiply implements BinaryOperatorI {
	
	protected Add add = new Add();
	protected Subtract sub = new Subtract();
	
	public MMultiply()
	{
		//add = (Add) Operator.OP_ADD.getPFMC();
		//sub = (Subtract) Operator.OP_SUBTRACT.getPFMC();
		numberOfParameters = 2;
	}




	/**
	 *  need to redo this as the standard jep version assumes commutivity.
	 */	
	public void run(Stack stack) throws ParseException 
	{
		checkStack(stack); // check the stack
		//if(this.curNumberOfParameters!=2) throw new ParseException("Multiply: should have two children its got "+stack.size());
		Object param2 = stack.pop();
		Object param1 = stack.pop();
		Object product = mul(param1, param2);
		stack.push(product);
		return;
	}

	/**
	 * Multiply two objects.
	 */

	public Object mul(Object param1, Object param2) throws ParseException 
	{
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
		
	/**
	 * Multiply two objects.
	 */

	public Object mul(MatrixValueI param1, MatrixValueI param2) throws ParseException 
	{
		Dimensions dims = this.calcDim(param1.getDim(),param2.getDim());
		MatrixValueI res = Tensor.getInstance(dims);
		return this.calcValue(res,param1,param2);
	}

	public Dimensions calcDim(Dimensions l,Dimensions r) throws ParseException
	{
		int lrank = l.rank();
		int rrank = r.rank();
		
		switch(lrank)
		{
		case 0: // Scaler res
			return r;
		case 1: // Vector * ?
			switch(rrank)
			{
			case 0: // Vector * Scaler -> Vector
				return l;
			case 1: // Vector * Vector -> Matrix
				return Dimensions.valueOf(l.getFirstDim(),r.getFirstDim());
			case 2: // Vector * Matrix -> Vector
				if(l.getLastDim() == r.getFirstDim())
					 return Dimensions.valueOf(r.getLastDim());
				break;
			default: // Tensor res
				throw new ParseException("Sorry I don't know how to multiply a vector by a tensor");
			}
			break;
		case 2: // Matrix * ?
			switch(rrank)
			{
			case 0: // Matrix * Scaler -> Matrix
				return l;
			case 1: // Matrix * Vector -> Vector
				if(l.getLastDim() == r.getFirstDim())
					 return Dimensions.valueOf(l.getFirstDim());
				break;
			case 2: // Matrix * Matrix -> Matrix
				if(l.getLastDim() == r.getFirstDim()) return Dimensions.valueOf(l.getFirstDim(),r.getLastDim());
				break;
			default: // Tensor res
				//throw new ParseException("Sorry I don't know how to multiply a matrix by a tensor");
				
			}
			break;
		default: // Tensor res
			switch(rrank)
			{
			case 0: // Scaler res
				return l;
//			case 1: // Vector res
//				throw new ParseException("Sorry I don't know how to multiply a tensor by a vector");
//			case 2: // Matrix res
//				throw new ParseException("Sorry I don't know how to multiply a tensor by a matrix");
//			default: // Tensor res
//				throw new ParseException("Sorry I don't know how to multiply a tensor by a tensor");
			}
		}
		throw new ParseException("Dimensions for multiply do not match: "+l+" "+r);
	}

	public MatrixValueI calcValue(MatrixValueI res,MatrixValueI param1,MatrixValueI param2) throws ParseException
	{
		if(param1 instanceof Scaler)
		{	
			if(param2 instanceof Scaler)	
			{ // Scaler * Scaler -> Scaler
				res.setEle(0,super.mul(param1.getEle(0),param2.getEle(0)));
			}
			else if(param2 instanceof MVector)
			{
			 // Scaler * Vector -> Vector
				for(int i=0;i<param2.getDim().getFirstDim();++i)
					res.setEle(i,super.mul(param1.getEle(0),param2.getEle(i)));
			}
			else if(param2 instanceof Matrix) // Scaler * Matrix -> Matrix
			{
				Matrix r = (Matrix) param2;
				Matrix mres = (Matrix) res;
				for(int i=0;i<r.getNumRows();++i)
					for(int j=0;j<r.getNumCols();++j)
					mres.setEle(i,j,super.mul(param1.getEle(0),r.getEle(i,j)));
			}
			else
			{ // Tensor res
				for(int i=0;i<param2.getDim().numEles();++i)
					res.setEle(i,super.mul(param1.getEle(0),param2.getEle(i)));
			}
		}
		else if(param1 instanceof MVector)
		{
			if(param2 instanceof Scaler) // Vector * Scaler -> Vector
			{
				for(int i=0;i<param1.getDim().getFirstDim();++i)
					res.setEle(i,super.mul(param1.getEle(i),param2.getEle(0)));
			}
			else if(param2 instanceof MVector) // Vector * Vector -> Matrix
			{
				Matrix mat = (Matrix) res;
				for(int i=0;i<param1.getDim().getFirstDim();++i)
					for(int j=0;j<param2.getDim().getFirstDim();++j)
						mat.setEle(i,j,super.mul(param1.getEle(i),param2.getEle(j)));
			}
			else if(param2 instanceof Matrix) // Vector * Matrix -> Vector
			{
				MVector lhs = (MVector) param1;
				Matrix rhs = (Matrix) param2;
				if(lhs.getNumEles() != rhs.getNumRows()) throw new ParseException("Multiply Matrix , Vector: Miss match in sizes ("+lhs.getNumEles()+","+rhs.getNumRows()+")!");
				for(int i=0;i<rhs.getNumCols();++i)
				{
					Object val = super.mul(lhs.getEle(0),rhs.getEle(0,i));
					for(int j=1;j<rhs.getNumRows();++j)
						val = add.add(val,
								super.mul(lhs.getEle(j),rhs.getEle(j,i)));
					res.setEle(i,val);
				}
			}
			else
			{
				throw new ParseException("Sorry I don't know how to multiply a vector by a tensor");
			}
		}
		else if(param1 instanceof Matrix)
		{
			if(param2 instanceof Scaler) // Matrix * Scaler -> Matrix
			{
				Matrix l = (Matrix) param1;
				Matrix mres = (Matrix) res;
				for(int i=0;i<l.getNumRows();++i)
					for(int j=0;j<l.getNumCols();++j)
					mres.setEle(i,j,super.mul(l.getEle(i,j),param2.getEle(0)));
			}
			else if(param2 instanceof MVector) // Matrix * Vector -> Vector
			{	
				Matrix lhs = (Matrix) param1;
				MVector rhs = (MVector) param2;
				if(lhs.getNumCols() != rhs.getNumEles()) throw new ParseException("Mat * Vec: Miss match in sizes ("+lhs.getNumCols()+","+rhs.getNumEles()+") when trying to add vectors!");
				for(int i=0;i<lhs.getNumRows();++i)
				{
					Object val = super.mul(lhs.getEle(i,0),rhs.getEle(0));
					for(int j=1;j<lhs.getNumCols();++j)
						val = add.add(val,super.mul(lhs.getEle(i,j),rhs.getEle(j)));
					res.setEle(i,val);
				}
			}
			else if(param2 instanceof Matrix) // Matrix * Matrix -> Matrix
			{
				Matrix lhs = (Matrix) param1;
				Matrix rhs = (Matrix) param2;
				Matrix mres = (Matrix) res;
				if(lhs.getNumCols() != rhs.getNumRows()) throw new ParseException("Multiply matrix,matrix: Miss match in number of dims ("+lhs.getNumCols()+","+rhs.getNumRows()+")!");
				int lnr = lhs.getNumRows();
				int lnc = lhs.getNumCols();
				int rnc = rhs.getNumCols();
				Object ldata[][] = lhs.getEles();
				Object rdata[][] = rhs.getEles();
				Object resdata[][] = mres.getEles();
				for(int i=0;i<lnr;++i)
					for(int j=0;j<rnc;++j)
					{
						Object val = mul(ldata[i][0],rdata[0][j]);
						for(int k=1;k<lnc;++k)
							val = add.add(val,
								mul(ldata[i][k],rdata[k][j]));
						resdata[i][j] = val;
					}
			}
			else // Tensor res
				throw new ParseException("Sorry I don't know how to multiply a matrix by a tensor");
		}
		else
		{
			if(param2 instanceof Scaler)
			{
				for(int i=0;i<param1.getDim().numEles();++i)
					res.setEle(i,super.mul(param1.getEle(i),param2.getEle(0)));
			}
			else
				throw new ParseException("Sorry I don't know how to multiply a tensor by a vector");
		}
		return res;
	}
}
