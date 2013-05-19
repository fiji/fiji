/* @author rich
 * Created on 18-Jun-2003
 */
package org.lsmp.djep.xjep;

import org.nfunk.jep.function.PostfixMathCommand;
import org.nfunk.jep.*;
import java.util.*;
/**
 * A function specified by a string.
 * For example
 * <pre>
 * XJepI jep = new XJep();
 * j.addFunction("zap",new MacroFunction("zap",1,"x*(x-1)/2",j));
 * Node node = j.parse("zap(10)");
 * System.out.println(j.evaluate(node)); // print 45
 * </pre>
 * The names of the variables used inside the function depends on the number of arguments:
 * <ul>
 * <li>One argument variable must be x: <tt>new MacroFunction("sec",1,"1/cos(x)",j)</tt></li>
 * <li>Two arguments variables must be x or y: <tt>new MacroFunction("myPower",2,"x^y",j)</tt></li>
 * <li>Three or more arguments variables must be x1, x2, x3,...: <tt>new MacroFunction("add3",3,"x1+x2+x3",j)</tt></li>
 * </ul>
 * @author R Morris.
 * Created on 18-Jun-2003
 */
public class MacroFunction extends PostfixMathCommand
{
	private String name;
	private Node topNode;
	private EvaluatorVisitor ev = new EvaluatorVisitor();
//	private XJep localJep;
	private XSymbolTable mySymTab;
	private Variable vars[];
	
	public String getName() { return name; }
	public Node getTopNode() { return topNode; }
	
	/**
	 * Create a function specified by a string.
	 * For example <tt>new MacroFunction("sec",1,"1/cos(x)",tu)</tt> creates the function for sec.
	 * Variable names must be x,y for 1 or 2 variables or x1,x2,x3,.. for 3 or more variables.
	 * @param inName name of function
	 * @param nargs number of arguments
	 * @param expression a string representing the expression.
	 * @param jep a reference to main XJep object.
	 */
	public MacroFunction(String inName,int nargs,String expression,XJep jep) throws IllegalArgumentException,ParseException
	{
		super();
		name = inName;

		XSymbolTable jepSymTab = (XSymbolTable) jep.getSymbolTable();
		mySymTab = (XSymbolTable) jepSymTab.newInstance(); 
		mySymTab.copyConstants(jepSymTab);
		XJep localJep = jep.newInstance(mySymTab);
		numberOfParameters = nargs;

		if(numberOfParameters == 0) {}
		else if(numberOfParameters == 1)
			vars = new Variable[]{mySymTab.addVariable("x",null)};
		else if(numberOfParameters == 2)
		{
			vars = new Variable[]{
					mySymTab.addVariable("x",null),
					mySymTab.addVariable("y",null)};
		}
		else
		{
			vars = new Variable[numberOfParameters];
			for(int i=numberOfParameters-1;i>0;)
				vars[i] = mySymTab.addVariable("x"+String.valueOf(i),null);
		}

		topNode = localJep.parse(expression);
	}
	
	/**
	 * Calculates the value of the expression.
	 * @throws ParseException if run.
	 */
	public void run(Stack stack) throws ParseException 
	{

		if(numberOfParameters == 0) {}
		else if(numberOfParameters == 1)
			vars[0].setValue(stack.pop());
		else if(numberOfParameters == 2)
		{
			vars[1].setValue(stack.pop());
			vars[0].setValue(stack.pop());
		}
		else
		{
			for(int i=numberOfParameters-1;i>0;)
				vars[i].setValue(stack.pop());
		}
		try
		{
			Object res = ev.getValue(topNode,mySymTab);
			stack.push(res);
		}
		catch(Exception e1) { throw new ParseException("MacroFunction eval: "+e1.getMessage()); }
	}
}
