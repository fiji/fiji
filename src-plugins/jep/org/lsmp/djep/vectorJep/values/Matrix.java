/* @author rich
 * Created on 07-Jul-2003
 */
package org.lsmp.djep.vectorJep.values;
import org.lsmp.djep.vectorJep.*;

//import JSci.maths.DoubleMatrix;
//import JSci.physics.relativity.Rank1Tensor;

/**
 * Represents a matrix.
 * 
 * @author Rich Morris
 * Created on 07-Jul-2003
 * @version 2.3.0.2 now extends number
 * @version 2.3.1.1 Bug with non square matrices fixed.
 * @since 2.3.2 Added equals method.
 */
public class Matrix implements MatrixValueI 
{
	public MatrixValueI copy() {
		Matrix tmp = new Matrix(this.rows,this.cols);
		tmp.setEles(this);
		return tmp;
	}
	// want package access to simplify addition of matrices
	int rows=0;
	int cols=0;
	Object data[][] = null;
	Dimensions dims;
	
	private Matrix() {}
	/** Construct a matrix with given rows and cols. */
	protected Matrix(int rows,int cols)
	{
		this.rows = rows;
		this.cols = cols;
		data = new Object[rows][cols];
		dims = Dimensions.valueOf(rows,cols);
	}
	public static MatrixValueI getInstance(int rows,int cols) {
		return new Matrix(rows,cols);
	}
	/**
	 * Construct a Matrix from a set of row vectors.
	 * @param vecs
	 */
/*
	public Matrix(MVector[] vecs) throws ParseException
	{
		if(vecs==null) { throw new ParseException("Tried to create a matrix with null row vectors"); } 
		rows = vecs.length;
		if(rows==0) {  throw new ParseException("Tried to create a matrix with zero row vectors"); }		
		
		// now check that each vector has the same size.
		
		cols = vecs[0].size();
		for(int i = 1;i<rows;++i)
			if(cols != vecs[i].size())
				throw new ParseException("Each vector must be of the same size");
	
		data = new Object[rows][cols];
		for(int i = 0;i<rows;++i)
			for(int j=0;j<cols;++j)
			{
				data[i][j]= vecs[i].elementAt(j);
				if(data[i][j] == null)
					throw new ParseException("Null element in vector");
			}
	}
*/
	/**
	 * Returns a string representation of matrix. Uses [[a,b],[c,d]] syntax.  
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for(int i = 0;i<rows;++i)
		{
			if(i>0) sb.append(',');
			sb.append('[');
			for(int j=0;j<cols;++j)
			{
				if(j>0)sb.append(',');
				sb.append(data[i][j]);
			}
			sb.append(']');
		}
		sb.append(']');
		return sb.toString();
	}
	public Dimensions getDim() { return dims; }
	public int getNumEles() { return rows*cols; }
	public int getNumRows() { return rows; }
	public int getNumCols() { return cols; }

	public void setEle(int n,Object value) 
	{
		int i = n / cols;
		int j = n % cols;
		data[i][j] = value;
	}
	public void setEle(int i,int j,Object value) 
	{
		data[i][j] = value;
	}
	public Object getEle(int n)
	{
		int i = n / cols;
		int j = n % cols;
		return data[i][j];
	}
	public Object getEle(int i,int j) 
	{
		return data[i][j];
	}
	
	public Object[][] getEles()
	{
		return data;
	}
	/** sets the elements to those of the arguments. */
	public void setEles(MatrixValueI val)
	{
		if(!dims.equals(val.getDim())) return;
		for(int i=0;i<rows;++i)
			System.arraycopy(((Matrix) val).data[i],0,data[i],0,cols);
	}

	/** value of ele(1,1). */	
	//public int intValue() {return ((Number) data[0][0]).intValue();	}
	/** value of ele(1,1). */	
	//public long longValue() {return ((Number) data[0][0]).longValue();	}
	/** value of ele(1,1). */	
	//public float floatValue() {	return ((Number) data[0][0]).floatValue();	}
	/** value of ele(1,1). */	
	//public double doubleValue() {return ((Number) data[0][0]).doubleValue();	}
	/** Are two matrices equal, element by element 
	 * Overrides Object.
	 */
	public boolean equals(Object obj) {
		if(!(obj instanceof Matrix)) return false;
		Matrix mat = (Matrix) obj;
		if(!mat.getDim().equals(getDim())) return false;
		for(int i=0;i<rows;++i)
			for(int j=0;j<cols;++j)
				if(!data[i][j].equals(mat.data[i][j])) return false;
		return true;
	}
	
	/**
	 * Always override hashCode when you override equals.
	 * Effective Java, Joshua Bloch, Sun Press
	 */
	public int hashCode() {
		int result = 17;
//		long xl = Double.doubleToLongBits(this.re);
//		long yl = Double.doubleToLongBits(this.im);
//		int xi = (int)(xl^(xl>>32));
//		int yi = (int)(yl^(yl>>32));
		for(int i=0;i<rows;++i)
			for(int j=0;j<cols;++j)
			result = 37*result+ data[i][j].hashCode();
		return result;
	}
	

}
