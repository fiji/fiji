/* @author rich
 * Created on 26-Oct-2003
 */
package org.lsmp.djep.matrixJep;
import org.nfunk.jep.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;
import org.lsmp.djep.xjep.*;
import org.lsmp.djep.djep.*;
/**
 * Holds all info about a variable.
 * Has a name, an equation, a dimension (or sent of dimensions if matrix or tensor)
 * and also a set of {@link MatrixPartialDerivative MatrixPartialDerivative}.
 * The derivatives are stored in a hashtable index by
 * the sorted names of derivatives.
 * i.e. d^2f/dxdy, and d^2f/dydx will both be indexed by {"x","y"}.
 * df/dx is indexed by {"x"}, d^2f/dx^2 is index by {"x","x"}.
 * Partial derivatives are calculated as required by the
 * findDerivative method.
 * @author Rich Morris
 * Created on 26-Oct-2003
 * @since 2.3.2 Added a setValue method overriding 
 */
public class MatrixVariable extends DVariable implements MatrixVariableI {

	private Dimensions dims;
	private MatrixValueI mvalue = null;

	protected PartialDerivative createDerivative(String derivnames[],Node eqn)
	{
		return new MatrixPartialDerivative(this,derivnames,eqn);
	}

	/**
	 * The constructor is package private. Variables should be created
	 * using the VariableTable.find(Sting name) method.
	 */
	MatrixVariable(String name) 
	{ 
		super(name);
		this.dims = Dimensions.ONE;
		this.mvalue = Scaler.getInstance(new Double(0.0));
		setValidValue(false);
	}

	MatrixVariable(String name,Object value) 
	{ 
		super(name);
		if(value == null)
			this.mvalue = Scaler.getInstance(new Double(0.0));
		else if(value instanceof MatrixValueI)
			this.mvalue = (MatrixValueI) value;
		else
			this.mvalue = Scaler.getInstance(value);
		
		this.dims = mvalue.getDim();
		setValidValue(true);
	}

	public Dimensions getDimensions() { return dims; }
	public void setDimensions(Dimensions dims) {
		this.dims = dims;
		this.mvalue=Tensor.getInstance(dims);
		this.invalidateAll();
	}

	/** returns the value, uses the Scaler type. */
	public MatrixValueI getMValue() { return mvalue; }	

	/** returns the value, unpacks Scalers so they just return its elements. */
	public Object getValue() { 
		if(mvalue instanceof Scaler)
			return mvalue.getEle(0);
		return mvalue;
	}	

	/**
	 * Sets the value of this variable.
	 * Needed when using macro functions in matrix calculations.
	 * TODO might be better to change macro function behaviour.
	 */
	protected boolean setValueRaw(Object val) {
		if(val instanceof MatrixValueI)
		{
			mvalue = (MatrixValueI) val;
			this.dims = mvalue.getDim();
		}
		else 
			mvalue.setEle(0,val);
		this.setValidValue(true);
		return true;
	}
	 
	public void setMValue(MatrixValueI val) {
		if(this.isConstant()) return;
		mvalue.setEles(val);
		setValidValue(true);
		setChanged();
		notifyObservers(val);
	}
	
//	public void setMValue(VectorMatrixTensorI value) 
//	{ this.mvalue = value; this.invalidateAll(); }
	
	public void print(PrintVisitor bpv)
	{
		StringBuffer sb = new StringBuffer(name);
		sb.append(": ");
		if(dims!=null) sb.append("dim "+dims.toString());
		if(hasValidValue() && mvalue != null) sb.append(" val "+getMValue() );
		else	sb.append(" null value");
		sb.append(" ");
		if(this.getEquation()!=null)
			sb.append("eqn "+bpv.toString(this.getEquation()));
		else
			sb.append("no equation");
		sb.append("\n");
		for(java.util.Enumeration e = this.derivatives.elements(); e.hasMoreElements(); ) 
		{
			MatrixPartialDerivative var = (MatrixPartialDerivative) e.nextElement();
			sb.append("\t"+var.toString()+": ");
			if(var.hasValidValue()) sb.append(" val "+var.getMValue() );
			else	sb.append(" null value");
			sb.append(" ");
			sb.append(bpv.toString(var.getEquation()));
			sb.append("\n");
		}
		System.out.print(sb.toString());
	}
	

}
