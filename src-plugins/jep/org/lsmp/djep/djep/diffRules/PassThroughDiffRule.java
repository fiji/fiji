/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.djep.DiffRulesI;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommandI;


/**
   * Rules like Sum where diff(sum(a,b,c),x) -> sum(da/dx,db/dx,dc/dx) are instance of this class.
   **/
  public class PassThroughDiffRule implements DiffRulesI
  {
	private String name;
	private PostfixMathCommandI pfmc;

	private PassThroughDiffRule() {}
	public PassThroughDiffRule(DJep djep,String inName)
	{	  
	  name = inName;
	  pfmc = djep.getFunctionTable().get(name);
	}
	public PassThroughDiffRule(String inName,PostfixMathCommandI inPfmc)
	{
		name = inName;
		pfmc = inPfmc; 
	}
	public String toString()
	{
		if(pfmc==null)
		{
			return "" + name +"\t\tPassthrough but no math command!"; 
		}
		switch(pfmc.getNumberOfParameters())
		{
		case 0:
			return name + "  \t\tdiff("+name+",x) -> "+name;
		case 1:
			return name + "  \tdiff("+name+"(a),x) -> "+name+"(da/dx)";
		case 2:
			return name + "  \tdiff("+name+"(a,b),x) -> "+name+"(da/dx,db/dx)";
		default:
			return name + "  \tdiff("+name+"(a,b,...),x) -> "+name+"(da/dx,db/dx,...)";
		}
	}
	public String getName() { return name; }
  	  	
	public Node differentiate(ASTFunNode node,String var,Node [] children,Node [] dchildren,DJep djep) throws ParseException
	{
		return djep.getNodeFactory().buildFunctionNode(node,dchildren);
	}
  }
