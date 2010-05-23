/*
Created 16-May-2006 - Richard Morris
*/
package org.lsmp.djep.vectorJep.function;

import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.Matrix;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.lsmp.djep.vectorJep.values.Tensor;
import org.nfunk.jep.ASTVarNode;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.Variable;
import org.nfunk.jep.function.LValueI;

/**
 * A postfix MathCommand which facilitates the getting and setting of vector and matrix elements.
 * The class implements the set method of LValueI., read access is handled by parent VEle class.
 * For examples
 * <code>
 * a=[1,2,3];
 * a[2]=4;
 * b=[[1,2],[3,4]];
 * b[2,1]=5; 
 * </code>
 * @author Richard Morris
 * TODO implement setting slices a[3:5]=[3,4,5] a[1,]=[1,3]
 */
public class ArrayAccess extends VEle implements LValueI {

	public ArrayAccess() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Sets the LValue. For the equation a[2]=5
	 */
	public void set(EvaluatorI pv,Node node, Object value) throws ParseException {
		Node lhs = node.jjtGetChild(0);
		// TODO Auto-generated method stub
		if(! (lhs instanceof ASTVarNode))
			throw new ParseException("ArrayAccess: lhs of operator must be a Variable");
		Variable var = ((ASTVarNode) lhs).getVar();
		MatrixValueI varVal = (MatrixValueI) var.getValue();
		Object indicies = pv.eval(node.jjtGetChild(1));
		if(varVal instanceof MVector)
		{
			if(indicies instanceof Number)
			{
				((MVector) varVal).setEle(((Number) indicies).intValue()-1,value);
				return; 
			}
			else if(indicies instanceof MVector)
			{
				MVector vec = (MVector) indicies;
				if(vec.getDim().equals(Dimensions.ONE))
				{
					int d1 = ((Number) vec.getEle(0)).intValue();
					if(d1<1 || d1 > ((MVector) varVal).getNumEles())
						throw new ParseException("ArrayAccess: array index "+d1+" out of range 1.."+varVal.getDim());
					((MVector) varVal).setEle(d1-1,value);
					return;
				}
			}
			throw new ParseException("ArrayAccess: Bad second argument expecting a double "+indicies.toString());
		}
		else if(varVal instanceof Matrix)
		{
			if(indicies instanceof MVector)
			{
				MVector vec = (MVector) indicies;
				if(vec.getDim().equals(Dimensions.TWO))
				{
					int d1 = ((Number) vec.getEle(0)).intValue();
					int d2 = ((Number) vec.getEle(1)).intValue();
					if( d1<1 || d1 > ((Matrix) varVal).getNumRows()
					 ||	d2<1 || d2 > ((Matrix) varVal).getNumCols() )
						throw new ParseException("ArrayAccess: array indices "+d1+", "+d2+" out of range 1.."+varVal.getDim());
						((Matrix) varVal).setEle(d1-1,d2-1,value);
					return; 
				}
			}
			else throw new ParseException("ArrayAccess:Bad second argument, expecting [i,j] "+indicies.toString());
		}
		else if(indicies instanceof Tensor)
		{
			throw new ParseException("ArrayAccess: Sorry don't know how to set an elements for a tensor");
		}
		throw new ParseException("ArrayAccess: requires a vector matrix or tensor for first argument it has "+varVal.toString());

	}

}
