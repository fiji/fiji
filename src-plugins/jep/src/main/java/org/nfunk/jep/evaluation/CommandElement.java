/*
 * 
 * Created on 15-Aug-2003
 */
package org.nfunk.jep.evaluation;

import org.nfunk.jep.function.*;
/**
 * 
 * @author nathan
 */
public class CommandElement {
	public final static int VAR   = 0; 
	public final static int CONST = 1; 
	public final static int FUNC  = 2; 
	private int                 type;
	private String              varName;
	private PostfixMathCommandI pfmc;
	private int                 nParam;
	private Object              value;

	/**
	 * @return The function associated with this element.
	 */
	public final PostfixMathCommandI getPFMC() {
		return pfmc;
	}

	/**
	 * @return The type.
	 */
	public final int getType() {
		return type;
	}

	/**
	 * @return The value of this element.
	 */
	public final Object getValue() {
		return value;
	}

	/**
	 * @return The variable name.
	 */
	public final String getVarName() {
		return varName;
	}

	/**
	 * @return The number of parameters.
	 */
	public final int getNumParam() {
		return nParam;
	}

	/**
	 * @param commandI The function associated with this element.
	 */
	public final void setPFMC(PostfixMathCommandI commandI) {
		pfmc = commandI;
	}

	/**
	 * @param i The type identifier.
	 */
	public final void setType(int i) {
		type = i;
	}

	/**
	 * @param object The value of the element.
	 */
	public final void setValue(Object object) {
		value = object;
	}

	/**
	 * @param string The name of the variable.
	 */
	public final void setVarName(String string) {
		varName = string;
	}

	/**
	 * @param i The number of parameters.
	 */
	public final void setNumParam(int i) {
		nParam = i;
	}

}
