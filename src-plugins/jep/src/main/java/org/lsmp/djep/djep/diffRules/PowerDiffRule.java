/* @author rich
 * Created on 04-Jul-2003
 */
package org.lsmp.djep.djep.diffRules;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.djep.DiffRulesI;
import org.lsmp.djep.xjep.*;
import org.nfunk.jep.*;


/**
	* Diffrentiates a power with respect to var.
	* If m is a a number 
	* diff(y^m,x) -> m * y^(m-1)
	* otherwise
	* diff(y^z,x) -> z * y^(z-1) * diff(y,x) + y^z * ln(z) * diff(z,x) 
	*/

 public class PowerDiffRule implements DiffRulesI
 {
	private String name;
	private PowerDiffRule() {}
	public PowerDiffRule(String inName)
	{	  
	  name = inName;
	}

  public String toString()
  {	  return name + "  \t\tdiff(f*g,x) -> diff(f,x)*g+f*diff(g,x)";  }
  public String getName() { return name; }
  	
  public Node differentiate(ASTFunNode node,String var,Node [] children,Node [] dchildren,DJep djep) throws ParseException
  {
	OperatorSet op = djep.getOperatorSet();
	NodeFactory nf = djep.getNodeFactory();
	TreeUtils tu = djep.getTreeUtils();
	FunctionTable funTab = djep.getFunctionTable();
	
	int nchild = node.jjtGetNumChildren();
	if(nchild!=2) 
		throw new ParseException("Too many children "+nchild+" for "+node+"\n");
	//	x^y -> 	n*(pow(x,y-1)) x' + ln(y) pow(x,y) y'

	if(tu.isConstant(children[1]))
	{
	   ASTConstant c = (ASTConstant) children[1];
	   Object value = c.getValue();
	   if(value instanceof Double)
	   {	// x^m -> m * x^(m-1) * x'
//	   	Node  a = TreeUtils.deepCopy(children[1]);
//	   	Node b = TreeUtils.deepCopy(children[0]);
//	   	Node cc = TreeUtils.createConstant( ((Double) value).doubleValue()-1.0);
//	   	Node d = opSet.buildPowerNode(b,cc);
//	   	Node e = opSet.buildMultiplyNode(a,d);

		 return nf.buildOperatorNode(op.getMultiply(),
		   djep.deepCopy(children[1]),
		   nf.buildOperatorNode(op.getMultiply(),
			nf.buildOperatorNode(op.getPower(),
			   djep.deepCopy(children[0]),
		   nf.buildConstantNode( tu.getNumber(((Double) value).doubleValue()-1.0))),
			 dchildren[0]));
	   }
	   
		 return nf.buildOperatorNode(op.getMultiply(),
			djep.deepCopy(children[1]),
			nf.buildOperatorNode(op.getMultiply(),
				nf.buildOperatorNode(op.getPower(),
				   djep.deepCopy(children[0]),
				   nf.buildOperatorNode(op.getSubtract(),
				 	djep.deepCopy(children[1]),
				 	nf.buildConstantNode(tu.getONE()))),
			 dchildren[0]));
	   
   }
   // z * y^(z-1) * diff(y,x) + y^z * ln(z) * diff(z,x) 
   {
		return nf.buildOperatorNode(op.getAdd(),  
			nf.buildOperatorNode(op.getMultiply(), // z * y^(z-1) * diff(y,x)
				nf.buildOperatorNode(op.getMultiply(), // z * y^(z-1)
					djep.deepCopy(children[1]), // z
					nf.buildOperatorNode(op.getPower(), // y^(z-1)
						djep.deepCopy(children[0]), // y
						nf.buildOperatorNode(op.getSubtract(), // z-1
							djep.deepCopy(children[1]), // z
							djep.getNodeFactory().buildConstantNode(tu.getONE()) ))),
				dchildren[0]), // diff(y,x)
			nf.buildOperatorNode(op.getMultiply(), //  + y^z * ln(z) * diff(z,x)
				nf.buildOperatorNode(op.getMultiply(), 
					nf.buildOperatorNode(op.getPower(), // y^z
						djep.deepCopy(children[0]), 
						djep.deepCopy(children[1])),
					djep.getNodeFactory().buildFunctionNode("ln",funTab.get("ln"), // ln(z)
						new Node[]{djep.deepCopy(children[0])})),
				dchildren[1]));
				// TODO will NaturalLog always have the name "ln"
   }
 }
 }
