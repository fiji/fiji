/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep;

import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;


/****** Classes to implement the differentation rules **********/

/**
 * Holds a set of rules describing how to differentiate a function.
 * Each function to be differentiated should have a object which implements 
 * this interface. 
 * @author R Morris
 * Created on 18-Jun-2003
 */
public interface DiffRulesI {

	/**
	 * Returns the top node of of the derivative of this function 
	 * wrt to variable var.
	 * @param var The name of variable to differentiate wrt to.
	 * @param children the arguments of the function
	 * @param dchildren the derivatives of each argument of the function.
	 * @return top node of and expression tree for the derivative.
	 * @throws ParseException if there is some problem in compiling the derivative.
	 */
	public Node differentiate(ASTFunNode node,String var,Node [] children,Node [] dchildren,DJep djep) throws ParseException;

	/**
	 * Returns a string representation of the rule.
	 */
	public String toString();

	/**
	 * Returns the name of the function.
	 * Used as index in hashtable and as way of linking with standard JEP functions.
	 * You probably want to specify the in the constructors. 
	 */
	public String getName();

}
