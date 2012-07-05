/* @author rich
 * Created on 19-Dec-2003
 */
package org.lsmp.djep.groupJep;

import org.nfunk.jep.*;
import org.nfunk.jep.type.Complex;
import org.lsmp.djep.groupJep.values.*;
/**
 * An extension of JEP which allows calculations over arbitary groups,
 * such as the integers(exact answers) and rationals.
 *  
 * @author Rich Morris
 * Created on 19-Dec-2003
 */
public class GroupJep extends JEP {
	protected GroupI group;
	/** 
	 * Create a new GroupJep instance with calculations over the given group.
	 * 
	 * @param group The group to calculate over.
	 */
	public GroupJep(GroupI group) {
		super();
		this.group = group;
		super.numberFactory = group.getNumberFactory();
		opSet = new GOperatorSet(group);
	}
	private GroupJep() {}
	public void addStandardFunctions()
	{
//		Certinally don't want the jep standard functions
//		super.addStandardFunctions();
		group.addStandardFunctions(this);
	}

	public void addStandardConstants()
	{
		group.addStandardConstants(this);
	}

	public GroupJep(JEP j) {
		super(j);
	}

	public GroupI getGroup()
	{
		return group;
	}

	/** 
	 * Calcuates the value of the expression and returns the 
	 * result as a complex number.
	 */
	public Complex getComplexValue() {
		Object num = this.getValueAsObject();
		if(num instanceof Complex) return (Complex) num;
		else if(num instanceof HasComplexValueI)
			return ((HasComplexValueI) num).getComplexValue();
		else if(num instanceof Number)
			return new Complex((Number) num);
		return super.getComplexValue();
	}

	/**
	 * A utility function which returns the complex aproximation of a number.
	 * @see HasComplexValueI
	 * @param num the object to be converted
	 * @return the complex aproximation or null if conversion to complex is not posible. 
	 **/
	public static Complex complexValueOf(Object num)
	{
		if(num instanceof Complex) return (Complex) num;
		else if(num instanceof HasComplexValueI)
			return ((HasComplexValueI) num).getComplexValue();
		else if(num instanceof Number)
			return new Complex((Number) num);
		else return null;
	}

}
