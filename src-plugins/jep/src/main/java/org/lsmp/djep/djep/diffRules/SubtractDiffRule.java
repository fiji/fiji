/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.djep.DiffRulesI;
import org.nfunk.jep.ASTFunNode;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;


/**
	 * Diffrentiates a subtaction with respect to var.
	 * diff(y-z,x) -> diff(y,x)-diff(z,x)
	 */
  public class SubtractDiffRule implements DiffRulesI
  {
	private String name;

	private SubtractDiffRule() {}
	public SubtractDiffRule(String inName)
	{	  
	  name = inName;
	}

	public String toString()
	{	  return name + "  \t\tdiff(f-g,x) -> diff(f,x)-diff(g,x)";  }
	public String getName() { return name; }
  	
	public Node differentiate(ASTFunNode node,String var,Node [] children,Node [] dchildren,DJep djep) throws ParseException
	{
	  int nchild = node.jjtGetNumChildren();
	  if(nchild==2) 
		  return djep.getNodeFactory().buildOperatorNode(djep.getOperatorSet().getSubtract(),dchildren[0],dchildren[1]);
	  else if(nchild==1)
		  return djep.getNodeFactory().buildOperatorNode(djep.getOperatorSet().getUMinus(),dchildren[0]);
	  else
		  throw new ParseException("Too many children "+nchild+" for "+node+"\n");
	}
  } /* end SubtractDiffRule */
