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
   * Differentiates a addition with respect to var.
   * diff(y+z,x) -> diff(y,x)+diff(z,x)
   */
public class AdditionDiffRule implements DiffRulesI
{
  private String name;

  /** private default constructor, a name must always be specified. */
  private AdditionDiffRule() { /* nothing happening */ 	}
  
  public AdditionDiffRule(String inName)
  {	  
	name = inName;
  }

  public String toString()
  {	  return name + "  \t\tdiff(f+g,x) -> diff(f,x)+diff(g,x)";  }
  public String getName() { return name; }
  	
  public Node differentiate(ASTFunNode node,String var,Node [] children,Node [] dchildren,DJep djep) throws ParseException
  {
	int nchild = node.jjtGetNumChildren();
	if(nchild!=2) 
		throw new ParseException("Too many children "+nchild+" for "+node+"\n");
	return djep.getNodeFactory().buildOperatorNode(djep.getOperatorSet().getAdd(),dchildren[0],dchildren[1]);
  }
}
