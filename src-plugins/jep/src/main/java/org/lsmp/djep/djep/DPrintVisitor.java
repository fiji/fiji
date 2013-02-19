/* @author rich
 * Created on 26-Feb-2004
 */
package org.lsmp.djep.djep;
import org.nfunk.jep.*;
import org.lsmp.djep.xjep.PrintVisitor;

/**
 * An extension of PrintVisitor which will print the equations of a variable if required.
 * The behaviours of this class is determined by two modes
 * PRINT_PARTIAL_EQNS and PRINT_VARIABLE_EQNS.
 * When a variable or partial derivative is encountered then
 * its equation may be printed.
 * By default equations for PartialDerivatives are printed
 * but equations for normal derivatives are not.
 * TODO might want to print eqn for y=sin(x) but not x=3
 *  
 * @author Rich Morris
 * Created on 26-Feb-2004
 */
public class DPrintVisitor extends PrintVisitor {
	public static final int PRINT_PARTIAL_EQNS = 16;
	public static final int PRINT_VARIABLE_EQNS = 32;
	
	/**
	 * 
	 */
	public DPrintVisitor() {
		super();
		setMode(PRINT_PARTIAL_EQNS,true);
	}

	/** Prints the variable or its equation.
	 * Depends on the state of the flags and whether the variable has an equation.
	 */
	public Object visit(ASTVarNode node, Object data) throws ParseException
	{
		Variable var = node.getVar();
		if(var instanceof PartialDerivative)
		{
			PartialDerivative deriv = (PartialDerivative) var;
			if(((mode & PRINT_PARTIAL_EQNS)!=0) && deriv.hasEquation())
				deriv.getEquation().jjtAccept(this,null);
			else
				sb.append(node.getName());
		}
		else if(var instanceof DVariable)
		{
			DVariable dvar = (DVariable) var;
			if(((mode & PRINT_VARIABLE_EQNS)!=0) && dvar.hasEquation())
				dvar.getEquation().jjtAccept(this,null);
			else
				sb.append(node.getName());
		}
		else
			sb.append(node.getName());

	  return data;
	}
}
