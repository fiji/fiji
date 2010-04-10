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
 * Calculate the Determinant of a matrix
 * det([[1,2],[3,4]]) -> 1*4-2*3 = -2
 * 
 * @author Rich Morris
 * Created on 13-Feb-2005
 */
public class Determinant extends PostfixMathCommand implements UnaryOperatorI
{
	Add add = new Add();
	Subtract sub = new Subtract();
	Multiply mul = new Multiply();
	
	public Determinant()
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
			throw new ParseException("det: result must be a scaler");
		if(!(lhs instanceof Matrix))
			throw new ParseException("det: argument must be a matrix");
		Matrix mat = (Matrix) lhs;
		if( mat.getNumRows()!= mat.getNumCols())
		 	throw new ParseException("det: argument must be a square matrix "+mat);

		if(mat.getNumRows() == 2)
		{
			res.setEle(0,sub.sub(
				mul.mul(mat.getEle(0,0),mat.getEle(1,1)),
				mul.mul(mat.getEle(1,0),mat.getEle(0,1))));
		}
		else if(mat.getNumRows() == 3)
		{	
			// | a b c |
			// | d e f | -> a e j + b f g + c d h - a f h - b d i - c e g
			// | g h i |
			Object r1 = 
				mul.mul(mat.getEle(0,0),mul.mul(mat.getEle(1,1),mat.getEle(2,2)));
			Object r2 = 
				mul.mul(mat.getEle(0,1),mul.mul(mat.getEle(1,2),mat.getEle(2,0)));
			Object r3 = 
				mul.mul(mat.getEle(0,2),mul.mul(mat.getEle(1,0),mat.getEle(2,1)));
			Object r4 = 
				mul.mul(mat.getEle(0,0),mul.mul(mat.getEle(1,2),mat.getEle(2,1)));
			Object r5 = 
				mul.mul(mat.getEle(0,1),mul.mul(mat.getEle(1,0),mat.getEle(2,2)));
			Object r6 = 
				mul.mul(mat.getEle(0,2),mul.mul(mat.getEle(1,1),mat.getEle(2,0)));

			Object r7 =	add.add(r1,add.add(r2,r3));
			Object r8 = add.add(r4,add.add(r5,r6));
			res.setEle(0,sub.sub(r7,r8));
		}
		else
		{
			Object[][] m = mat.getEles();
			res.setEle(0,det(m));
		}
//			throw new ParseException("Sorry can only calculate determinants for 2 by 2 and 3 by 3 matricies");
		return res;
	}

	/** returns a matrix excluding the specifyed row and column */
	public static Object[][] dropRowCol(Object[][] mat,int xrow,int xcol)
	{
		int nrows = mat.length;
		int ncols = mat[0].length;
		Object res[][] = new Object[nrows-1][ncols-1];
		int currow = 0;
		for(int i=0;i<nrows;++i)
		{
			if(i != xrow)
			{
				int curcol = 0;
				for(int j=0;j<ncols;++j)
				{
					if(j != xcol)
					{
						res[currow][curcol] = mat[i][j];
						++curcol;
					}
				}
				++currow;
			}
		}
		return res;
	}
	/** Calculates the determinant of an array 
	 * Uses the fact that
	 * | a b c |
	 * | d e f | = a | e f | - b | d f | + c | d e |
	 * | g h i |     | h i |     | g i |     | g i |  
	 */
	public Object det(Object[][] mat) throws ParseException
	{
		if(mat.length == 1) return mat[0][0];
		if(mat.length == 2) {
			return sub.sub(
				mul.mul(mat[0][0],mat[1][1]),
				mul.mul(mat[1][0],mat[0][1]));
		}
		Object res = new Double(0.0);
		for(int i=0;i<mat.length;++i)
		{
			Object[][] m = dropRowCol(mat,0,i);
			Object det = det(m);
			if(i%2 == 0)
				res = add.add(res,mul.mul(mat[0][i],det));
			else
				res = sub.sub(res,mul.mul(mat[0][i],det));
		}
		return res;
	}
	public void run(Stack s) throws ParseException
	{
		MatrixValueI obj = (MatrixValueI) s.pop();
		MatrixValueI res = Tensor.getInstance(calcDim(obj.getDim()));
		calcValue(res,obj);
		s.push(res);
	}

}
