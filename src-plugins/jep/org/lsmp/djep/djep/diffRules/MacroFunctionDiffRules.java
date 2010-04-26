/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.xjep.*;
import org.nfunk.jep.*;

/**
   * If your really lazy, you don't even need to workout the derivatives
   * of a function defined by a macro yourself.
   * This class will automatically calculate the rules for you.
   */
  public class MacroFunctionDiffRules extends ChainRuleDiffRules
  {
	/**
	 * Calculates the rules for the given function.
	 */
	  public MacroFunctionDiffRules(DJep djep,MacroFunction fun)  throws ParseException
	  {
		  name = fun.getName();
		  pfmc = fun;
		  
		XSymbolTable localSymTab = (XSymbolTable) ((XSymbolTable) djep.getSymbolTable()).newInstance(); //new SymbolTable();
		localSymTab.copyConstants(djep.getSymbolTable());
		DJep localJep = (DJep) djep.newInstance(localSymTab);

		  int nargs = fun.getNumberOfParameters();
		  rules = new Node[nargs];
		  if(nargs == 1)
			  rules[0] = localJep.differentiate(fun.getTopNode(),"x");
		  else if(nargs == 2)
		  {
			  rules[0] = localJep.differentiate(fun.getTopNode(),"x");
			  rules[1] = localJep.differentiate(fun.getTopNode(),"y");
		  }
		  else
		  {
			  for(int i=0;i<nargs;++i)
				  rules[i] = localJep.differentiate(fun.getTopNode(),"x"+ String.valueOf(i));
		  }
		  for(int i=0;i<nargs;++i)
				rules[i] = localJep.simplify(rules[i]);
		  //fixVarNames();
	  }
  }
