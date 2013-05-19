/* @author rich
 * Created on 04-May-2004
 */
package org.lsmp.djep.mrpe;

import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.lsmp.djep.vectorJep.values.Tensor;
import org.nfunk.jep.ParseException;

/**
 * The base type for values returned by evaluate.
 * 
 * @author Rich Morris
 * Created on 04-May-2004
 */
public abstract class MRpRes {
	/** The Dimension of the object */
	public abstract Dimensions getDims();
	/** 
	 * Copy the value into res.
	 * 
	 * @param res The object values will be copied into, must be of corect type.
	 * @throws ParseException if the res is not of the same type.
	 */
	public abstract void copyToVecMat(MatrixValueI res) throws ParseException;
	/**
	 * Converts to a MatrixValueI object. 
	 * @return a new MatrixValueI with values filled in.
	 * @throws ParseException should not happen!
	 */
	public final MatrixValueI toVecMat()  throws ParseException {
		MatrixValueI res = Tensor.getInstance(getDims());
		copyToVecMat(res);
		return res;
	}
	/**
	 * Returns an array of doubles with the values filled in. 
	 * @return the array either double[] or double[][]
	 */
	public abstract Object toArray();

}
