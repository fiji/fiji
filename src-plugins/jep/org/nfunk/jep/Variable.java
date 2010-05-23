/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/
/* @author rich
 * Created on 18-Nov-2003
 */
package org.nfunk.jep;
import java.util.*;

/**
 * Information about a variable. 
 * Each variable has a name, a value.
 * There is a flag to indicate
 * whether it is a constant or not (constants cannot have their value changed).
 * There is also a flag to indicate whether the value of the
 * variable is valid, if the variable is initialised without a value
 * then its value is said to be invalid.
 * <p>
 * @author Rich Morris
 * Created on 18-Nov-2003
 * @version 2.3.0 beta 2 Now extends Observable so observers can track if the value has been changed.
 */
public class Variable extends Observable {
	protected String name;
	private Object value;
	private boolean isConstant = false;
	private boolean validValue = false;
//	private static final Double ZERO = new Double(0.0);

	/** Constructors are protected. Variables should only
	 * be created through the associated {@link VariableFactory}
	 * which are in turned called by {@link SymbolTable}.
	 */
	protected Variable(String name)
	{
		this.name = name;
		this.value = null;
		validValue = false;
	}
	/** Constructors are protected. Variables should only
	 * be created through the associated {@link VariableFactory}
	 * which are in turned called by {@link SymbolTable}.
	 */
	protected Variable(String name,Object value)
	{
		this.name = name;
		this.value = value;
		validValue = (value!=null);
	}
	public String getName() {return name;}
	//private void setName(String string) {name = string;	}
	public boolean isConstant() { return this.isConstant; }
	public void setIsConstant(boolean b) { this.isConstant = b; }
	public Object getValue() { return value; }
	/** Is the value of this variable valid? **/
	public boolean hasValidValue() { return validValue; }
	/** Sets whether the value of variable is valid. **/
	public void setValidValue(boolean val) {
		if(isConstant()) return; validValue = val; }

	/**
	 * Sets the value of the variable. Constant values cannot be changed.
	 * <p>
	 * This method call java.util.Observable.notifyObservers()
	 * to indicate to anyone interested that the value has been changed.
	 * Note subclasses should override setValueRaw rather than this
	 * method so they do not need to handle the Observable methods.
	 *  
	 * @return false if tried to change a constant value.
	 * @since 2.3.0 beta 2 added Observable
	 */
	public boolean setValue(Object object) {
		if(!setValueRaw(object)) return false;
		setChanged();
		notifyObservers(object);
		return true;
	}

	/**
	 * In general subclasses should override this method rather than
	 * setValue. This is because setValue notifies any observers
	 * and then calls this method.
	 * @param object
	 * @return false if tried to change a constant value.
	 * @since 2.3.0 beta 2
	 */
	protected boolean setValueRaw(Object object) {
		if(isConstant) return false;
		validValue = true;
		value = object;
		return true;
	}

	/**
	 * Returns a string with the variable name followed by it's value.
	 * For example for the variable "a" with the value 10, the following
	 * string is returned:
	 * <pre>a: 10</pre>
	 * If the variable is a constant the string " (Constant" is appended.
	 * @return A string with the variable name and value.
	 */
	public String toString() {
		if(!validValue || value == null)
			return name + ": null";
		else if(isConstant)
			return name + ": " + value.toString() + " (Constant)";
		else
			return name + ": " + value.toString(); 
	}
}
