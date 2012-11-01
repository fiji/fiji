/* @author rich
 * Created on 26-Oct-2003
 */
package org.lsmp.djep.djep;
import java.util.*;
import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;

/**
 * Holds all info about a variable.
 * Has a name, an equation, a dimension (or sent of dimensions if matrix or tensor)
 * and also a set of {@link PartialDerivative PartialDerivative}.
 * The derivatives are stored in a hashtable index by
 * the sorted names of derivatives.
 * i.e. d^2f/dxdy, and d^2f/dydx will both be indexed by {"x","y"}.
 * df/dx is indexed by {"x"}, d^2f/dx^2 is index by {"x","x"}.
 * Partial derivatives are calculated as required by the
 * findDerivative method.
 * @author Rich Morris
 * Created on 26-Oct-2003
 */
public class DVariable extends XVariable 
{
	protected Hashtable derivatives = new Hashtable();
	
	protected PartialDerivative createDerivative(String derivnames[],Node eqn)
	{
		return new PartialDerivative(this,derivnames,eqn);
	}
	/**
	 * The constructor is package private. Variables should be created
	 * using the VariableTable.find(Sting name) method.
	 */
	protected DVariable(String name) 
	{ 
		super(name);
	}

	protected DVariable(String name,Object value) 
	{ 
		super(name,value);
	}

	/** sets the equation */
	public void setEquation(Node eqn)
	{
		super.setEquation(eqn);
		derivatives.clear(); 
	}

	/** makes value and values of all derivatives invalid. **/
	public void invalidateAll()
	{
		if(isConstant()) return;
		setValidValue(false);
		for(Enumeration e = derivatives.elements(); e.hasMoreElements(); ) 
		{
			PartialDerivative deriv = (PartialDerivative) e.nextElement();
			deriv.setValidValue(false);
		}
	}
	/** Produces a string to represent the derivative.
	 * The string will be of the form "dx^2/dxdy".
	 * This string is used to index the derivatives of a variable.
	 * @param rootname name of the variable we are calculating the derivative of.
	 * @param dnames An array of the names of each of the partial derivatives.
	 * @return the string representation
	 */	
	public static String makeDerivString(String rootname,String dnames[])
	{
		StringBuffer sb = new StringBuffer();
		sb.append('d');
		if( dnames.length!= 1) sb.append("^" + dnames.length);
		sb.append(rootname);
		sb.append('/');
		// TODO print d^2f/dxdx as d^2f/dx^2
		for(int i=0;i<dnames.length;++i)
		{
			sb.append('d');
			sb.append(dnames[i]);
		}
		return sb.toString();

	}
	/** returns a sorted copy of the input array of strings */		
	private String[] sortedNames(String names[])
	{
		String newnames[] = new String[names.length]; 
		System.arraycopy(names,0,newnames,0,names.length);
		Arrays.sort(newnames);
		return newnames;
	}
	/** Sets the derivative wrt the variables specified in 
	 * 	deriv names. Note the names are sorted so that
	 *  d^2z/dxdy = d^2z/dydx.
	 */
	void setDerivative(String derivnames[],PartialDerivative eqn)
	{
		String newnames[] = sortedNames(derivnames); 
		derivatives.put(makeDerivString(name,newnames),eqn);
	}

	void setDerivativeSorted(String derivnames[],PartialDerivative eqn)
	{
		derivatives.put(makeDerivString(name,derivnames),eqn);
	}

	PartialDerivative getDerivative(String derivnames[])
	{
		String newnames[] = sortedNames(derivnames);
		return (PartialDerivative) derivatives.get(makeDerivString(name,newnames));
	}

	PartialDerivative getDerivativeSorted(String derivnames[])
	{
		return (PartialDerivative) derivatives.get(makeDerivString(name,derivnames));
	}


/**
 * Finds the derivative of this variable wrt the names.
 * If the derivative already exists just return that.
 * Otherwise use the visitor to calculate the derivative.
 * If the derivative cannot be calculated (say if the equation is null)
 * then return null.
 * 
 * @param derivnames
 * @return The derivative or null if it cannot be calculated.
 * @throws ParseException
 */
	PartialDerivative findDerivativeSorted(String derivnames[],DJep jep)
		throws ParseException
	{
		if(getEquation()==null) return null;
		
		if(derivnames == null) throw new ParseException("findDerivativeSorted: Null array of names");
		PartialDerivative res = getDerivativeSorted(derivnames);
		if(res!=null) return res;
		
		// Deriv not found. Calculate from lower derivative
		int origlen = derivnames.length;
		Node lowereqn; // equation for lower derivative (or root equation)
		if(origlen < 1)
			throw new ParseException("findDerivativeSorted: Empty Array of names");
		else if(origlen == 1)
			lowereqn = getEquation();
		else
		{
			String newnames[] = new String[origlen-1];
			for(int i=0;i<origlen-1;++i)
				newnames[i]=derivnames[i];
			lowereqn = findDerivativeSorted(newnames,jep).getEquation();
		}
		if(lowereqn==null)
		{
			return null;
		}
		Node deriv = jep.differentiate(lowereqn,derivnames[origlen-1]);
		Node simp = jep.simplify(deriv);
		res = createDerivative(derivnames,simp); 
		setDerivative(derivnames,res);
		return res;	
	}

	PartialDerivative findDerivative(String derivnames[],DJep jep)
		throws ParseException
	{
		String newnames[] = sortedNames(derivnames); 
		return findDerivativeSorted(newnames,jep);
	}

	PartialDerivative findDerivative(String derivname,DJep jep)
		throws ParseException
	{
		String newnames[] = new String[1];
		newnames[0]=derivname;
		return findDerivativeSorted(newnames,jep);
	}

	PartialDerivative findDerivative(PartialDerivative deriv,String dname,DJep jep)
		throws ParseException
	{
		int len = deriv.getDnames().length;
		String newnames[] = new String[len+1];
		System.arraycopy(deriv.getDnames(),0,newnames,0,len);
		newnames[len]=dname;
		return findDerivative(newnames,jep);
	}
	
	public String toString(PrintVisitor bpv)
	{
		boolean mode = bpv.getMode(DPrintVisitor.PRINT_VARIABLE_EQNS);
		bpv.setMode(DPrintVisitor.PRINT_VARIABLE_EQNS,false);
		StringBuffer sb = new StringBuffer(name);
		sb.append(":\t");
		if(hasValidValue()) sb.append(getValue() );
		else	sb.append("NA");
		sb.append("\t");
		if(this.isConstant()) sb.append("constant");
		else if(getEquation()!=null) sb.append("eqn "+bpv.toString(getEquation()));
		else sb.append("no equation");
		for(Enumeration e = derivatives.elements(); e.hasMoreElements(); ) 
		{
			sb.append("\n");
			PartialDerivative var = (PartialDerivative) e.nextElement();
			sb.append("\t"+var.toString()+": ");
			if(var.hasValidValue()) sb.append(var.getValue() );
			else	sb.append("NA");
			sb.append("\t");
			sb.append(bpv.toString(var.getEquation()));
		}
		bpv.setMode(DPrintVisitor.PRINT_VARIABLE_EQNS,mode);
		return sb.toString();
	}

	/** Enumerate all the derivatives of this variable.
	 * 
	 * @return an Enumeration running through all the derivatives. 
	 */
	public Enumeration allDerivatives() {
		return derivatives.elements();
	}
}
