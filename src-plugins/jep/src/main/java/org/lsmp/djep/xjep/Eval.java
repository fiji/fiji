/* @author rich
 * Created on 18-Jun-2003
 */
package org.lsmp.djep.xjep;
import org.nfunk.jep.function.PostfixMathCommand;
import org.nfunk.jep.*;
import java.util.*;

/**
 * Symbolic eval(x^3,x,2) operator.  
 * @author R Morris.
 * Created on 18-Jun-2003
 */
public class Eval extends PostfixMathCommand implements CommandVisitorI
{
	/**
	 * Create a function that evaluates the lhs with values given on rhs.
	 * e.g. eval(f,x,1,y,2) sets variable x to 1, y to 2 and evaluates f.
	 */
	public Eval()
	{
		super();
		numberOfParameters = -1;
	}
	//TODO probably broken
	public Node process(Node node,Node children[],XJep xjep) throws ParseException
	{
		Vector errorList = new Vector();
		int nchild = children.length;
		if(nchild %2 == 0)
			throw new ParseException("Number of parameters must be odd");
		XSymbolTable localSymTab = (XSymbolTable) ((XSymbolTable) xjep.getSymbolTable()).newInstance();
		XJep localJep = xjep.newInstance(localSymTab);

		for(Enumeration en = xjep.getSymbolTable().keys();en.hasMoreElements();)
		{
			String key = (String) en.nextElement();
			Object val = xjep.getSymbolTable().getValue(key);
			localSymTab.addVariable(key,val);
		}
		/** first evaluate the arguments **/
		for(int i=1;i<nchild;i+=2)
		{
			ASTVarNode var;
			Object value;
			try
			{
				var = (ASTVarNode) children[i];
				Node rhs = children[i+1];
				if( rhs instanceof ASTConstant)
					value = ((ASTConstant) rhs).getValue();
				else
				{
					value = localJep.evaluate(rhs);
					if(!errorList.isEmpty())
						throw new ParseException(errorList.toString());
				}
			}
			catch(ClassCastException e)
			{
				throw new ParseException("Format should be eval(f,x,1,y,2) where x,y are variables and 1,2 are constants");
			}
			catch(Exception e)	{ throw new ParseException(e.getMessage());	}
			localSymTab.setVarValue(var.getName(),value);
		}
		/** now evaluate the equation **/
		try
		{
			Object res = localJep.evaluate(node.jjtGetChild(0));
			if(!errorList.isEmpty())
				throw new ParseException(errorList.toString());
			return xjep.getNodeFactory().buildConstantNode(res);
		}
		catch(Exception e2) { throw new ParseException(e2.getMessage()); } 
	}
	
	/**
	 * Should not be called by evaluator
	 * @throws ParseException if run.
	 */
	public void run(Stack s) throws ParseException 
	{
		throw new ParseException("Eval should not be called by Evaluator"); 
	}
}
