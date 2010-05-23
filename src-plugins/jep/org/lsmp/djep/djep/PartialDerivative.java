/* @author rich
 * Created on 29-Oct-2003
 */
package org.lsmp.djep.djep;
import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;

import java.util.Observable;
import java.util.Observer;
/**
 * Contains infomation about a PartialDerivative of a variable.
 * Should  
 * @author Rich Morris
 * Created on 29-Oct-2003
 */
public class PartialDerivative extends XVariable  implements Observer {

	private DVariable root;
	private String dnames[] = null;
	private String printString;
	/**
	 * Protected constructor, should only be constructed
	 * through the findDerivative method in {@link DVariable DVariable}.
	**/ 
	protected PartialDerivative(DVariable var, String derivnames[])
	{
		super(var.getName());
		root = var;
		dnames = derivnames;
		printString = DVariable.makeDerivString(root.getName(),derivnames);
		root.addObserver(this);
	}
	
	protected PartialDerivative(DVariable var, String derivnames[],Node deriv)
	{
		this(var,derivnames);
		/*TODO could be a little cleverer just have a 
		 * partial derivative which is a constant dy/dx = 1
		 * don't use an equation, instead use a value.
		 * 
		if(deriv instanceof ASTConstant) {
			System.out.println("Warning constant derivative "+printString+"="+((ASTConstant) deriv).getValue());
			this.setValue(((ASTConstant) deriv).getValue());
		}
		else
		*/
			setEquation(deriv);
	}
	
	public String getName() { return printString; }
	
	/**
	 * Every partial derivative has a root variable
	 * for instance the root variable of dy/dx is y.
	 * This method returns than variable.
	 */
	public DVariable getRoot() { return root; }
	public String[] getDnames() { return dnames; }

	public String toString()
	{
		return printString;
	}
	
	public PartialDerivative findDerivative(String dname,DJep jep)
		throws ParseException
	{
		return root.findDerivative(this,dname,jep);
	}
	
	
	/**
	 * When the value of the root object is changed
	 * makes the value of this partial derivative invalid.
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable arg0, Object arg1) {
		if(root.equals(arg0))
		{
			setValidValue(false);
		}
	}

}
