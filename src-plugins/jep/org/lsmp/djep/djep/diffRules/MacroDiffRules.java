/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.nfunk.jep.*;
import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.xjep.*;
import org.nfunk.jep.function.PostfixMathCommandI;


/**
   * Rules are specfied by a set of strings or trees of nodes.
   * The standard chain rule is applied
   * <pre>diff(f(g(x),h(x)),x) -> df/dg dg/dx + df/dh dh/dx</pre> 
   * for example 
   * <pre>
   * DifferentiationVisitor dv = new DifferentiationVisitor(new TreeUtils(jep));
   * DiffRulesI rule = new MacroDiffRules(dv,"sin","cos(x)");
   * </pre>
   **/
  public class MacroDiffRules extends ChainRuleDiffRules
  {
	/**
	 * Create a differention rule for function with 1 argument
	 * @param inName	name of function
	 * @param node		a tree represention differation of function wrt "x"
	 * @throws ParseException
	 */
	
	public MacroDiffRules(DJep djep,String inName,Node node) throws ParseException
	{
		name = inName;
		pfmc = djep.getFunctionTable().get(inName);
		if(pfmc!=null)
		{
			int nParam = pfmc.getNumberOfParameters();
			if(nParam != 1)
				throw new ParseException("Number of rules must match number of parameters for "+inName+" which is "+nParam);
		}
		rules = new Node[1];
		rules[0] = node;
		//fixVarNames();
	}
  	
	/**
	 * Create a differention rule for function with 1 argument
	 * @param inName	name of function
	 * @param rule		a string represention differation of a function wrt "x"
	 * @throws ParseException
	 */
	public MacroDiffRules(DJep djep,String inName,String rule) throws ParseException
	{
		this(djep,inName,djep.getFunctionTable().get(inName),rule);
	} 	

	/**
	 * Create a differention rule for function with 1 argument
	 * @param inName	name of function
	 * @param inPfmc	PostfixMathCommandI for function
	 * @param rule		a string represention differation of function wrt "x"
	 * @throws ParseException
	 */
	public MacroDiffRules(DJep djep,String inName,PostfixMathCommandI inPfmc,String rule) throws ParseException
	{
		//super(dv);
		name = inName;
		pfmc = inPfmc;
		if(pfmc!=null)
		{
			int nParam = pfmc.getNumberOfParameters();
			if(nParam != 1)
				throw new ParseException("Number of rules must match number of parameters for "+inName+" which is "+nParam);
		}
		XSymbolTable localSymTab = (XSymbolTable) ((XSymbolTable) djep.getSymbolTable()).newInstance(); //new SymbolTable();
		localSymTab.copyConstants(djep.getSymbolTable());
		XJep localJep = djep.newInstance(localSymTab);
		Node node = localJep.parse(rule);
		rules = new Node[1];
		rules[0] = node;
		//fixVarNames();
	}

	/**
	 * Create a differention rule for function with 2 arguments.
	 * The rules must be in terms of "x" and "y"
	 * @param inName	name of function
	 * @param inPfmc	PostfixMathCommandI for function
	 * @param rule1		a string represention differation of function wrt "x"
	 * @param rule2		a string represention differation of function wrt "y"
	 * @throws ParseException
	 */
	public MacroDiffRules(DJep djep,String inName,PostfixMathCommandI inPfmc,String rule1,String rule2) throws ParseException
	{
		//super(dv);
		name = inName;
		pfmc = inPfmc;
		if(pfmc!=null)
		{
			int nParam = pfmc.getNumberOfParameters();
			if(nParam != 2)
			throw new ParseException("Number of rules must match number of parameters for "+inName+" which is "+nParam);
		}
		XSymbolTable localSymTab = (XSymbolTable) ((XSymbolTable) djep.getSymbolTable()).newInstance(); //new SymbolTable();
		localSymTab.copyConstants(djep.getSymbolTable());
		XJep localJep = djep.newInstance(localSymTab);
		Node node1 = localJep.parse(rule1);
		Node node2 = localJep.parse(rule2);
		rules = new Node[2];
		rules[0] = node1;
		rules[1] = node2;
		//fixVarNames();
	}
	
	/**
	 * Create a differention rule for function with 2 arguments.
	 * The rules must be in terms of "x" and "y"
	 * @param inName	name of function
	 * @param rule1		a string represention differation of function wrt "x"
	 * @param rule2		a string represention differation of function wrt "y"
	 * @throws ParseException
	 */
	public MacroDiffRules(DJep djep,String inName,String rule1,String rule2) throws ParseException
	{
		this(djep,inName,djep.getFunctionTable().get(inName),rule1,rule2);
	}

	/**
	 * Create a differention rule for function with 2 arguments.
	 * The rules must be in terms of "x" and "y"
	 * @param inName	name of function
	 * @param inPfmc	PostfixMathCommandI for function
	 * @param node1		a expression tree represention differation of function wrt "x"
	 * @param node2		a expression tree represention differation of function wrt "y"
	 * @throws ParseException
	 */
/*	public MacroDiffRules(DJep djep,String inName,PostfixMathCommandI inPfmc,Node node1,Node node2) throws ParseException
	{
	  //super(dv);
		name = inName;
		pfmc = inPfmc;
		if(pfmc!=null)
		{
			int nParam = pfmc.getNumberOfParameters();
			if(nParam != 2)
			throw new ParseException("Number of rules must match number of parameters for "+inName+" which is "+nParam);
		}
		rules = new Node[2];
		rules[0] = node1;
		rules[1] = node2;
		//fixVarNames();
	}
*/	
	/**
	 * Create a differention rule for function with 2 arguments.
	 * The rules must be in terms of "x" and "y"
	 * @param inName	name of function
	 * @param node1		a expression tree represention differation of function wrt "x"
	 * @param node2		a expression tree represention differation of function wrt "y"
	 * @throws ParseException
	 */
/*	public MacroDiffRules(DJep djep,String inName,Node node1,Node node2) throws ParseException
	{
		this(djep,inName,djep.getFunctionTable().get(inName),node1,node2);
	}
*/	/**
	 * Create a differentation rule for function with n arguments.
	 * The rules must be in terms of "x1", "x2", ... "xn"
	 * @param inName	name of function
	 * @param inPfmc	PostfixMathCommandI for function
	 * @throws ParseException
	 */
	public MacroDiffRules(DJep djep,String inName,PostfixMathCommandI inPfmc,String[] inRules) throws ParseException
	{
		name = inName;
		pfmc = inPfmc;
		if(pfmc!=null)
		{
			int nParam = pfmc.getNumberOfParameters();
			if(nParam != inRules.length)
			throw new ParseException("Number of rules must match number of parameters for "+inName+" which is "+nParam);
		}
		
		XSymbolTable localSymTab = (XSymbolTable) ((XSymbolTable) djep.getSymbolTable()).newInstance(); //new SymbolTable();
		localSymTab.copyConstants(djep.getSymbolTable());
		XJep localJep = djep.newInstance(localSymTab);

		rules = new Node[inRules.length];
		for(int i=0;i<inRules.length;++i)
		{
			rules[i] = localJep.parse(inRules[i]);
		}
		//fixVarNames();
	}

	/**
	 * Create a differentation rule for function with n arguments.
	 * The rules must be in terms of "x1", "x2", ... "xn"
	 * @param inName	name of function
	 * @param inRules	an array of strings representation differentation of function wrt "x1",...
	 * @throws ParseException
	 */
	public MacroDiffRules(DJep djep,String inName,String[] inRules) throws ParseException
	{
		this(djep,inName,djep.getFunctionTable().get(inName),inRules);
	}
	/**
	 * Create a differentation rule for function with n arguments.
	 * The rules must be in terms of "x1", "x2", ... "xn"
	 * @param inName	name of function
	 * @param inPfmc	PostfixMathCommandI for function
	 * @param inRule	an array of expression trees representation differentation of function wrt "x1",...
	 * @throws ParseException
	 */
/*	public MacroDiffRules(DJep djep,String inName,PostfixMathCommandI inPfmc,Node[] inRules) throws ParseException
	{
		//super(dv);
		name = inName;
		pfmc = inPfmc;
		if(pfmc!=null)
		{
			int nParam = pfmc.getNumberOfParameters();
			if(nParam != inRules.length)
				throw new ParseException("Number of rules must match number of parameters for "+inName+" which is "+nParam);
		}
		rules = inRules;
		//fixVarNames();
	}
*/	
	/**
	 * Create a differentation rule for function with n arguments.
	 * The rules must be in terms of "x1", "x2", ... "xn"
	 * @param inName	name of function
	 * @param inRules	an array of expression trees representation differentation of function wrt "x1",...
	 * @throws ParseException
	 */
/*	public MacroDiffRules(DJep djep,String inName,Node[] inRules) throws ParseException
	{
		this(djep,inName,djep.getFunctionTable().get(inName),inRules);
	}
*/	
  } /* end MacroDiffRules */
