/* @author rich
 * Created on 15-Feb-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djep.jama;
import org.lsmp.djep.vectorJep.values.*;
import org.nfunk.jep.*;
/**
 * Utility functions for adding Jama matrix functions.
 * To add these functions to a VectorJep or MatrixJep instance use
 * <pre>
 * VectorJep j = new VectorJep();
 * j.addStandardFunctions();
 * ...
 * JamaUtil.addStandardFunctions(j);
 * </pre>
 * 
 * @author Rich Morris
 * Created on 15-Feb-2005
 * @see <a href="http://math.nist.gov/javanumerics/jama/">http://math.nist.gov/javanumerics/jama/</a>
 */
public final class JamaUtil
{
	public static Jama.Matrix toJama(Matrix m) throws ParseException
	{
		int rows = m.getNumRows();
		int cols = m.getNumCols();
		Object data[][] = m.getEles();
		double A[][] = new double[rows][cols];
		for(int i=0;i<rows;++i)
			for(int j=0;j<cols;++j)
			{
				Object ele = data[i][j];
				if(ele instanceof Number)
					A[i][j] = ((Number) ele).doubleValue();
				else
					throw new ParseException("Only real matricies are supported");
			}
		return new Jama.Matrix(A);
	}

	public static Jama.Matrix toJamaCol(MVector m) throws ParseException
	{
		int rows =1;
		int cols = m.getNumEles();
		double A[][] = new double[rows][cols];
		
			for(int j=0;j<cols;++j)
			{
				Object ele = m.getEle(j);
				if(ele instanceof Number)
					A[0][j] = ((Number) ele).doubleValue();
				else
					throw new ParseException("Only real matricies are supported");
			}
		return new Jama.Matrix(A);
	}

	public static Jama.Matrix toJamaRow(MVector m) throws ParseException
	{
		int rows = m.getNumEles();
		int cols =1;
		double A[][] = new double[rows][cols];
		
			for(int i=0;i<rows;++i)
			{
				Object ele = m.getEle(i);
				if(ele instanceof Number)
					A[i][0] = ((Number) ele).doubleValue();
				else
					throw new ParseException("Only real matricies are supported");
			}
		return new Jama.Matrix(A);
	}

	public static Matrix fromJama(Jama.Matrix A,Matrix m)
	{
		int rows = A.getRowDimension();
		int cols = A.getRowDimension();
		double in[][] = A.getArray();
		Object out[][] = m.getEles();
		for(int i=0;i<rows;++i)
			for(int j=0;j<cols;++j)
				out[i][j] = new Double(in[i][j]);
		return m;
	}
	public static Matrix fromJama(Jama.Matrix A)
	{
		int rows = A.getRowDimension();
		int cols = A.getRowDimension();
		Matrix m = (Matrix) Matrix.getInstance(rows,cols);
		double in[][] = A.getArray();
		Object out[][] = m.getEles();
		for(int i=0;i<rows;++i)
			for(int j=0;j<cols;++j)
				out[i][j] = new Double(in[i][j]);
		return m;
	}
	
	public static void addStandardFunctions(JEP j)
	{
		j.addFunction("inverse",new Inverse());
		j.addFunction("rank",new Rank());
		j.addFunction("solve",new Solve());
	}
}
