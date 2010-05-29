/* @author rich
 * Created on 25-Oct-2003
 */
package org.lsmp.djep.vectorJep;


/**
 * A class to represent a set of dimensions.
 * Might be 1 for 0-dimensional numbers.
 * [3] for a 3D vector
 * [3,3] for a matrix
 * @author rich
 * Created on 25-Oct-2003
 */
public class Dimensions
{
	private int dims[];
	public static final Dimensions UNKNOWN = new Dimensions(-1);
	public static final Dimensions ONE = new Dimensions(1);
	public static final Dimensions TWO = new Dimensions(2);
	public static final Dimensions THREE = new Dimensions(3);
	
	private Dimensions() {}	
	/** Sets the dimension to a single number. Implies its a Scaler or MVector. */
	private Dimensions(int d) {	dims = new int[] {d};	}
	/** Use this method for matrices. */
	private Dimensions(int d1,int d2) {	dims = new int[] {d1,d2}; }
	/** Construct a dimension set from an array. */
	private Dimensions(int d[])	
	{
		dims = new int[d.length];
		System.arraycopy(d,0,dims,0,d.length);
	}
	/**
	 * Factory method returns a Dimension for vector of given length.
	 * For 1,2,3 returns ONE,TWO or THREE otherwise create a new object.
	 */ 
	public static Dimensions valueOf(int d)
	{ 
		switch(d) {
			case 1: return Dimensions.ONE;
			case 2: return Dimensions.TWO;
			case 3: return Dimensions.THREE;
			default:
				return new Dimensions(d);
		}
	}
	/** returns dimensions for a matrix. **/
	public static Dimensions valueOf(int rows,int cols)
	{ 
		return new Dimensions(rows,cols);
	}
	/** return a dimension [d,inDim[0],...,inDim[n]] */
	public static Dimensions valueOf(int d,Dimensions inDim)
	{ 
		Dimensions res = new Dimensions();
		res.dims = new int[inDim.rank()+1];
		res.dims[0]=d;
		for(int i=0;i<inDim.rank();++i)
			res.dims[i+1]=inDim.dims[i];
		return res;
	}

	/** return a dimension [inDim[0],...,inDim[n],d] */
	public static Dimensions valueOf(Dimensions inDim,int d)
	{ 
		Dimensions res = new Dimensions();
		res.dims = new int[inDim.rank()+1];
		for(int i=0;i<inDim.rank();++i)
			res.dims[i]=inDim.dims[i];
		res.dims[inDim.rank()+1]=d;
		return res;
	}

	/** returns a dimensions with given dimensions. */
	public static Dimensions valueOf(int dims[])
	{
		if(dims.length == 1) return valueOf(dims[0]);
		if(dims.length == 2) return valueOf(dims[0],dims[1]);
		return new Dimensions(dims);
	}

	/** get the first dimension, 1 for numbers, 
	    or the length of a vector. 
	    for a matrix [[1,2,3],[4,5,6]] first dim is number of rows eg 2 */
	public int getFirstDim() {	return dims[0];	}
	/** get the last dimension, 1 for numbers, 
		or the length of a vector. 
	    for a matrix [[1,2,3],[4,5,6]] last dim is number of cols eg 3 
	 */
	public int getLastDim() {	return dims[dims.length-1];	}
	public int getIthDim(int i) { return dims[i]; }
	
	/** Is it 0D, ie a simple number. **/
	public boolean is0D() { return dims.length == 1 && dims[0] == 1; }
	/** Is it 1D, ie a vector [1,2,3]. **/
	public boolean is1D() {	return dims.length == 1 && dims[0] != 1; }
	/** Is it 2D, ie a matrix [[1,2,3],[4,5,6]]. **/
	public boolean is2D() {	return dims.length == 2; }
	/**
	 * The total number of elements.
	 * Produce of all the dimensions.
	 */
	public int numEles()
	{
		int res=1;
		for(int i=0;i<dims.length;++i) res *= dims[i];
		return res; 	
	}
	/** rank of dimensions 0 for numbers, 1 for vectors, 2 for matrices */
	public int rank()
	{
		if(is0D()) return 0;
		return dims.length; 
	}
	/** A string representation.
	 * Either 1,n,[m,n],[l,m,n] etc.
	 */
	public String toString()
	{
		if(is0D()) return String.valueOf(dims[0]);
		if(is1D()) return String.valueOf(dims[0]);
		StringBuffer sb = new StringBuffer("["+dims[0]);
		for(int i=1;i<dims.length;++i)
			sb.append(","+dims[i]);
		sb.append("]");
		return sb.toString();
	}

	/** Two dimensions are equal if the element of dims are the same. */
	public boolean equals(Dimensions dims2)
	{
		if(dims2 == null) return false;
		if( dims.length != dims2.dims.length) return false;
		for(int i=0;i<dims.length;++i)
		{ if(dims[i] != dims2.dims[i]) return false;} 
		return true;  
	}

	/** apparently your should always override hashcode when you
	 * override equals (Effective Java, Bloch).
	 */
	public int hashcode()
	{
		int res =17;
		for(int i=0;i<dims.length;++i)
			res = 37*res + dims[i];
		return res;
	}
	
	public boolean equals(Object arg)
	{
		if(arg == null) return false;
		if(arg instanceof Dimensions) return equals((Dimensions) arg);
		return false;
	}

}
