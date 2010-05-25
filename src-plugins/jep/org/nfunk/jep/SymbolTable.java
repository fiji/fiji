/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/
package org.nfunk.jep;
import java.util.*;

/** A Hashtable which holds a list of all variables.
 * Heavily changed from Jep-2.24 which was just a Hashtable which stored
 * the values of each variable. Here the Hashtable contains
 * elements of type {@link Variable Variable} which contain
 * information about that variable.
 * Rather than using {@link #get get} the methods
 * {@link #getValue getValue(String)}, {@link #getVar getVar(String)}
 * should be used to return the value or variable.
 * The {@link #put put} method is deprecated and should be replace by one of
 * <ul>
 * <li>{@link #addVariable addVariable(String,Object)} adds a variable with a given name and value, returns null if variable already exists.
 * <li>{@link #addConstant addConstant(String,Object)} adds a 'constant' variable whos value cannot be changed.
 * <li>{@link #setVarValue setVarValue(String,Object)} sets the value of an existing variable. Returns false if variable does not exist.
 * <li>{@link #makeVarIfNeeded(String,Object)} if necessary creates a variable and set its value.
 * <li>{@link #makeVarIfNeeded(String)} if necessary creates a variable. Does not change the value.
 * </ul>
 * <p>
 * Variables which do not have a value set are deemed to be invalid.
 * When Variables need to be constructed then methods in the {@link VariableFactory}
 * should be called, which allows different types of variables to be used.
 * 
 * <p>
 * Both SymbolTable and Variable implement the Observer/Observable pattern.
 * This allows objects to be informed whenever a new variable is created
 * or when its value has been changed. The member class StObservable is used to implement
 * Observer. An example of use is
 * <pre>
 * public class MyObserver implements Observer
 * {
 * 	public void initialise()
 * 	{
 * 		SymbolTable st = j.getSymbolTable();
 * 		st.addObserver(this);
 * 		st.addObserverToExistingVariables(this);
 * 	}
 * 
 * 	public void update(Observable arg0, Object arg1)
 * 	{
 * 		if(arg0 instanceof Variable)
 * 			println("Var changed: "+arg0);
 * 		else if(arg0 instanceof SymbolTable.StObservable)
 * 		{
 * 			println("New var: "+arg1);
 * 
 * 			// This line is vital to ensure that 
 * 			// any new variable created will be observed. 
 * 			((Variable) arg1).addObserver(this);
 * 		}
 * 	}
 * }
 * </pre>

 *  
 * @author Rich Morris
 * Created on 28-Feb-2004
 */ 
public class SymbolTable extends Hashtable
{
	private static final long serialVersionUID = 1127787896437151144L;
	private VariableFactory vf;
	/** SymbolTable should always be constructed an associated variable factory. */
	public SymbolTable(VariableFactory varFac)
	{
		vf = varFac;
	}
	/** Private default constructors, SymbolTable should always be constructed with an explicit variable factory. */
	private SymbolTable() {}
	
	/**
	 * @deprecated The getValue or getVar methods should be used instead. 
	 */
	public Object get(Object key) { return getValue(key); }
	

	/** Finds the value of the variable with the given name. 
	 * Returns null if variable does not exist. */
	public Object getValue(Object key)
	{
		Variable var = (Variable) super.get(key);
		if(var==null) return null;
		return var.getValue();
	}
	
	/** Finds the variable with given name. 
	* Returns null if variable does not exist. */
	public Variable getVar(String name)
	{
		return (Variable) super.get(name);
	}

	/**
	 * @deprecated The setVarValue or makeVar methods should be used instead.
	 */
	public Object put(Object key,Object val)
	{
		return makeVarIfNeeded((String) key,val);
	}

	/**
	 * Sets the value of variable with the given name.
	 * @throws NullPointerException if the variable has not been previously created
	 * with {@link #addVariable(String,Object)} first.
	 */
	public void setVarValue(String name,Object val)
	{
		Variable var = (Variable) super.get(name);
		if(var != null) {
			var.setValue(val); 
			return; 
		}
		throw new NullPointerException("Variable "+name+" does not exist.");
	}

	/**
	 * Returns a new variable fro the variable factory. Notifies observers
	 * when a new variable is created. If a subclass need to create a new variable it should call this method.
	 * 
	 * @param name
	 * @param val
	 * @return an new Variable object.
	 */
	protected Variable createVariable(String name,Object val)
	{
		Variable var = vf.createVariable(name,val);
		obeservable.stSetChanged();
		obeservable.notifyObservers(var);
		return var;
	}

	protected Variable createVariable(String name)
	{
		Variable var = vf.createVariable(name);
		obeservable.stSetChanged();
		obeservable.notifyObservers(var);
		return var;
	}

	/** Creates a new variable with given value.
	 * 
	 * @param name name of variable
	 * @param val initial value of variable
	 * @return a reference to the created variable.
	 */
	public Variable addVariable(String name,Object val)
	{
		Variable var = (Variable) super.get(name);
		if(var != null)
		    throw new IllegalStateException("Variable "+name+" already exists.");
		
		var = createVariable(name,val);
		super.put(name,var);
		var.setValidValue(true);
		return var;
	}

	/** Create a constant variable with the given name and value.
	 * Returns null if variable already exists.
	 */
	public Variable addConstant(String name,Object val)
	{
		Variable var = addVariable(name,val);
		var.setIsConstant(true);
		return var;
	}

	/** Create a variable with the given name and value.
	 * It silently does nothing if the value cannot be set.
	 * @return the Variable.
	 */
	public Variable makeVarIfNeeded(String name,Object val)
	{
		Variable var = (Variable) super.get(name);
		if(var != null)
		{
		    if(var.isConstant())
		        throw new IllegalStateException("Attempt to change the value of constant variable "+name);
			var.setValue(val);
			return var; 
		}
		var = createVariable(name,val);
		super.put(name,var);
		return var;
	}

	/** If necessary create a variable with the given name.
	 * If the variable exists its value will not be changed.
	 * @return the Variable.
	 */
	public Variable makeVarIfNeeded(String name)
	{
		Variable var = (Variable) super.get(name);
		if(var != null)	return var; 

		var = createVariable(name);
		super.put(name,var);
		return var;
	}

	/**
	 * Returns a list of variables, one per line.
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for(Enumeration e = this.elements(); e.hasMoreElements(); ) 
		{
			Variable var = (Variable) e.nextElement();
			sb.append(var.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Clears the values of all variables.
	 * Finer control is available through the
	 * {@link Variable#setValidValue Variable.setValidValue} method.
	 */
	public void clearValues()
	{
		for(Enumeration e = this.elements(); e.hasMoreElements(); ) 
		{
			Variable var = (Variable) e.nextElement();
			var.setValidValue(false);
		}
	}
	/**
	 * Returns the variable factory of this instance.
	 */
	public VariableFactory getVariableFactory() {
		return vf;
	}

	public class StObservable extends Observable {
		protected synchronized void stSetChanged() {
			this.setChanged();
		}
		public SymbolTable getSymbolTable() {
			return SymbolTable.this;
		}
	}
	protected StObservable obeservable = new StObservable();
	/**
	 * Adds an observer which will be notified when a new variable is created.
	 * The observer's update method will be called whenever a new
	 * variable is created, the second argument of this will be
	 * a reference to the new variable.
	 * 
	 * <p>
	 * To find out if the values of variables are changed
	 * the Variable.addObserver method should be used.
	 * 
	 * @param arg the observer
	 * @see Variable#addObserver(Observer)
	 */	
	public synchronized void addObserver(Observer arg)	{
		obeservable.addObserver(arg);
	}

	public synchronized int countObservers() {
		return obeservable.countObservers();
	}

	public synchronized void deleteObserver(Observer arg)	{
		obeservable.deleteObserver(arg);
	}

	public synchronized void deleteObservers()	{
		obeservable.deleteObservers();
	}

	public synchronized boolean hasChanged()	{
		return obeservable.hasChanged();
	}

	/** Adds an observer to all variables currently in the SymbolTable.
	 * 
	 * @param arg the object to be notified when a variable changes.
	 */
	public synchronized void addObserverToExistingVariables(Observer arg)	{
		for(Enumeration en = this.elements();en.hasMoreElements();) {
			Variable var = (Variable) en.nextElement();
			var.addObserver(arg);
		}
	}
	/** Remove all non constant elements */
	public void clearNonConstants() {
		Vector tmp = new Vector();
		for(Enumeration en = this.elements();en.hasMoreElements();) {
			Variable var = (Variable) en.nextElement();
			if(var.isConstant()) tmp.add(var);
		}
		this.clear();
		for(Enumeration en = tmp.elements();en.hasMoreElements();) {
			Variable var = (Variable) en.nextElement();
			super.put(var.getName(),var);
		}
	}
}
