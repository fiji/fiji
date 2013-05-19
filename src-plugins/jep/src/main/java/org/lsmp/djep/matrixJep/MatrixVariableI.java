/* @author rich
 * Created on 19-Dec-2003
 */
package org.lsmp.djep.matrixJep;
import org.nfunk.jep.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.MatrixValueI;

/**
 * Matrix aware variables should implement this interface. 
 * @author Rich Morris
 * Created on 19-Dec-2003
 */
public interface MatrixVariableI {
	/** The dimension of the variable. */
	public abstract Dimensions getDimensions();
	/** Sets the dimension of the variable. 
	 * Will also allocate appropriate space for value container. 
	 */
	public abstract void setDimensions(Dimensions dims);
	/** Returns the value container of this variable.
	 * There is no setMValue as the value can be changed by
	 * setting the individual elements of value container.
	 */
	public abstract MatrixValueI getMValue();
	/** Sets the value of the variable (matrix aware). */
	public abstract void setMValue(MatrixValueI val);
	/** Is the value of this variable meaningful? */
	public abstract boolean hasValidValue();
	/** makes the vaule valid. */
	public abstract void setValidValue(boolean b);
	/** The equation represented by this variable. */
	public abstract Node getEquation();
	/** Whether this variable has an equation. */
	public abstract boolean hasEquation();
	/** The name of variable */
	public abstract String getName();
	/** Is it constant? */
	public abstract boolean isConstant();
}
