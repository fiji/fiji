/* @author rich
 * Created on 03-Aug-2003
 */
package org.nfunk.jep;

import org.nfunk.jep.function.PostfixMathCommandI;

/**
 * A class containing information about an operator.
 *
 * @see OperatorSet 
 * @author Rich Morris
 * Created on 19-Oct-2003
 */
public class Operator {

	/** A unique name defining the operator. */
	private String name;
	/** The symbol for the operator, used for printing. */
	private String symbol;
	/** Postfix mathcommand */
	private PostfixMathCommandI pfmc;
	
	/** private default constructor, prevents calling with no arguments. */
	private Operator()
	{
	}

	/** construct a new operator.
	 * 
	 * @param name	printable name of operator
	 * @param pfmc  postfix math command for opperator
	 */
	public Operator(String name,PostfixMathCommandI pfmc)
	{
		this();
		this.name = name; this.pfmc = pfmc;
		this.symbol = name;
	}
	/** construct a new operator, with a different name and symbol
	 * 
	 * @param name	name of operator, must be unique, used when describing operator
	 * @param symbol printable name of operator, used for printing equations
	 * @param pfmc  postfix math command for opperator
	 */
	public Operator(String name,String symbol,PostfixMathCommandI pfmc)
	{
		this();
		this.name = name; this.pfmc = pfmc;
		this.symbol = symbol;
	}
	/** returns the symbol used by this operator. */
	public final String getSymbol() {return symbol;}
	/** returns a unique name definig this operator. */
	public final String getName() {return name;}
	public final PostfixMathCommandI getPFMC() { return pfmc;}
	public final void setPFMC(PostfixMathCommandI pfmc) { this.pfmc = pfmc;}
	/** returns a verbose representation of the operator. **/
	public String toString() { return "Operator: \""+name+"\""; }
}
