/* @author rich
 * Created on 14-Dec-2004
 */
package org.lsmp.djep.sjep;
import org.lsmp.djep.xjep.*;
import org.nfunk.jep.*;
/**
 * Represents a variable.
 * 
 * @author Rich Morris
 * Created on 14-Dec-2004
 */
public class PVariable extends AbstractPNode {

	XVariable variable;
	/**
	 * 
	 */
	public PVariable(PolynomialCreator pc,XVariable var) {
		super(pc);
		this.variable = var;
	}

	public boolean equals(PNodeI node)
	{
		if(node instanceof PVariable)
			if(variable.equals(((PVariable)node).variable))
				return true;	

		return false;
	}

	/**
	this < arg ---> -1
	this > arg ---> 1
	*/
	public int compareTo(PVariable vf)
	{
			return variable.getName().compareTo(vf.variable.getName());
	}
	
	public String toString()
	{
		return variable.getName();
	}
	
	public Node toNode() throws ParseException
	{
		return pc.nf.buildVariableNode(variable);
	}
	
	public PNodeI expand()	{ return this;	}
}
