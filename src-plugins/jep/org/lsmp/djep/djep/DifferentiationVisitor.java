/* @author rich
 * Created on 18-Jun-2003
 */
   
package org.lsmp.djep.djep;
import org.lsmp.djep.djep.diffRules.*;
import org.lsmp.djep.xjep.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.PrintStream;

/**
 * A class for performing differentation of an expression.
 * To use do
 * <pre>
 * JEP j = ...; Node in = ...;
 * DifferentiationVisitor dv = new DifferentiationVisitor(jep);
 * dv.addStandardDiffRules();
 * Node out = dv.differentiate(in,"x");
 * </pre>
 * The class follows the visitor pattern described in
 * {@link org.nfunk.jep.ParserVisitor ParserVisitor}.
 * The rules for differentiating specific functions are contained in
 * object which implement
 * {@link DiffRulesI DiffRulesI}
 * A number of inner classes which use this interface are defined for specific
 * function types.
 * In particular
 * {@link MacroDiffRules MacroDiffRules}
 * allow the rule for differentiation to be specified by strings.
 * New rules can be added using
 * {@link DJep#addDiffRule} method.
 * @author R Morris
 * Created on 19-Jun-2003
 */
public class DifferentiationVisitor extends DeepCopyVisitor
{
	private static final boolean DEBUG = false; 
	private DJep localDJep;
	private DJep globalDJep;
	private NodeFactory nf;
	private TreeUtils tu;
//	private OperatorSet opSet;
  /**
   * Construction with a given set of tree utilities 
   */
  public DifferentiationVisitor(DJep jep)
  {
	this.globalDJep = jep;
	

  }
      
  /** The set of all differentation rules indexed by name of function. */ 
  Hashtable diffRules = new Hashtable();
  /** Adds the rules for a given function. */
  void addDiffRule(DiffRulesI rule)
  {
	diffRules.put(rule.getName(),rule);
	if(DEBUG) System.out.println("Adding rule for "+rule.getName());
  }
  /** finds the rule for function with given name. */
  DiffRulesI getDiffRule(String name)
  {
	return (DiffRulesI) diffRules.get(name);
  }
  
  /**
   * Prints all the differentation rules for all functions on System.out.
   */
  public void printDiffRules() { printDiffRules(System.out); }
  
  /**
   * Prints all the differentation rules for all functions on specified stream.
   */
  public void printDiffRules(PrintStream out)
  {
	out.println("Standard Functions and their derivatives");
	for(Enumeration en = globalDJep.getFunctionTable().keys(); en.hasMoreElements();)
	{
		String key = (String) en.nextElement();
		PostfixMathCommandI value = globalDJep.getFunctionTable().get(key);
		DiffRulesI rule = (DiffRulesI) diffRules.get(key);
		if(rule==null)
			out.print(key+" No diff rules specified ("+value.getNumberOfParameters()+" arguments).");
		else
			out.print(rule.toString());
		out.println();
	}
	for(Enumeration en = diffRules.keys(); en.hasMoreElements();)
		{
			String key = (String) en.nextElement();
			DiffRulesI rule = (DiffRulesI) diffRules.get(key);
			if(!globalDJep.getFunctionTable().containsKey(key))
			{
				out.print(rule.toString());
				out.println("\tnot in JEP function list");
			}
		}
	}

	/**
	 * Differentiates an expression tree wrt a variable var.
	 * @param node the top node of the expression tree
	 * @param var the variable to differentiate wrt
	 * @return the top node of the differentiated expression 
	 * @throws ParseException if some error occurred while trying to differentiate, for instance of no rule supplied for given function.
	 * @throws IllegalArgumentException
	 */
	public Node differentiate(Node node,String var,DJep djep) throws ParseException,IllegalArgumentException
	{
	  this.localDJep = djep;
	  this.nf=djep.getNodeFactory();
	  this.tu=djep.getTreeUtils();
	  //this.opSet=djep.getOperatorSet();
	  
	  if (node == null)
		  throw new IllegalArgumentException("node parameter is null");
	  if (var == null)
		  throw new IllegalArgumentException("var parameter is null");

	  Node res = (Node) node.jjtAccept(this,var);
	  return res;
	}

	/********** Now the recursive calls to differentiate the tree ************/

	/**
	 * Applies differentiation to a function.
	 * Used the rules specified by objects of type {@link DiffRulesI}.
	 * @param node The node of the function.
	 * @param data The variable to differentiate wrt.
	 **/

	public Object visit(ASTFunNode node, Object data) throws ParseException
	{
		String name = node.getName();

	   //System.out.println("FUN: "+ node + " nchild "+nchild);
		Node children[] = TreeUtils.getChildrenAsArray(node);
		Node dchildren[] = acceptChildrenAsArray(node,data);

		if(node.getPFMC() instanceof DiffRulesI)
		{
			 return ((DiffRulesI) node.getPFMC()).differentiate(node,(String) data,children,dchildren,localDJep);
		}
		DiffRulesI rules = (DiffRulesI) diffRules.get(name);
		if(rules != null)
		return rules.differentiate(node,(String) data,children,dchildren,localDJep);

		throw new ParseException("Sorry I don't know how to differentiate "+node+"\n");
	}

	public boolean isConstantVar(XVariable var) {
		if(!var.hasEquation()) return true;
		Node eqn = var.getEquation();
		if(eqn instanceof ASTConstant) return true;
		/* So why would we want a=x to be treated as a constant? */
//		if(eqn instanceof ASTVarNode) {
//			return isConstantVar((XVariable)((ASTVarNode) eqn).getVar());
//		}
		return false;
	}
	 /**
	  * Differentiates a variable. 
	  * May want to alter behaviour when using multi equation as diff(f,x)
	  * might not be zero.
	  * @return 1 if the variable has the same name as data, 0 if the variable has a different name.
	  */
	 public Object visit(ASTVarNode node, Object data) throws ParseException {
	   String varName = (String) data;
	   XVariable var = (XVariable) node.getVar();
	   PartialDerivative deriv=null;
	   if(var instanceof DVariable)
	   {
	   		DVariable difvar = (DVariable) var;
	   		if(varName.equals(var.getName()))
	   			return nf.buildConstantNode(tu.getONE());
		
	   		else if(isConstantVar(var))
	   			return nf.buildConstantNode(tu.getZERO());
			
	   		deriv = difvar.findDerivative(varName,localDJep);
	   }
	   else if(var instanceof PartialDerivative)
	   {
   			if(isConstantVar(var))
   				return nf.buildConstantNode(tu.getZERO());
		
			PartialDerivative pvar = (PartialDerivative) var;
			DVariable dvar = pvar.getRoot();
			deriv = dvar.findDerivative(pvar,varName,localDJep);
				
	   }
	   else
		   throw new ParseException("Encountered non differentiable variable");
	   	
	   Node eqn = deriv.getEquation();
	   if(eqn instanceof ASTVarNode)
	   		return nf.buildVariableNode(((ASTVarNode) eqn).getVar());
	   if(eqn instanceof ASTConstant)
			return nf.buildConstantNode(((ASTConstant)eqn).getValue());

	   return nf.buildVariableNode(deriv);
	 }

	 /**
	  * Differentiates a constant.
	  * @return 0 derivatives of constants are always zero.
	  */
	 public Object visit(ASTConstant node, Object data) throws ParseException {
		return nf.buildConstantNode(tu.getZERO());
	 }
}

/*end*/
