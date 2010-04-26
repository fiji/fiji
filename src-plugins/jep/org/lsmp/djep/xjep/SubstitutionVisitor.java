/* @author rich
 * Created on 16-Nov-2003
 */
package org.lsmp.djep.xjep;
import org.nfunk.jep.*;
/**
 * Allows substitution of a given variable with an expression tree.
 * Substitution is best done using the 
 * {@link XJep#substitute(Node,String,Node) XJep.substitute} method. 
 * For example
 * <pre>
 * XJepI xjep = ...;
 * Node node = xjep.parse("x^2+x");
 * Node sub = xjep.parse("sin(y)");
 * Node res = xjep.substitute(node,"x",sub,xjep);
 * </pre>
 * Will give the expression "(sin(y))^2+sin(y)".
 * 
 * @author Rich Morris
 * Created on 16-Nov-2003
 */
public class SubstitutionVisitor extends DoNothingVisitor {

	private String names[];
	private Node replacements[];
	private XJep xjep;
	public SubstitutionVisitor() {}

	/**
	 * Substitutes all occurrences of variable var with replacement.
	 * Does not do a DeepCopy.
	 * @param orig	the expression we wish to perform the substitution on
	 * @param name	the name of the variable
	 * @param replacement	the expression var is substituted for
	 * @return the tree with variable replace (does not do a DeepCopy)
	 * @throws ParseException
	 */
	public Node substitute(Node orig,String name,Node replacement,XJep xj) throws ParseException
	{
		this.names = new String[]{name};
		this.replacements = new Node[]{replacement};
		this.xjep=xj;
		Node res = (Node) orig.jjtAccept(this,null);
		return res;
	}

	/**
	 * Substitutes all occurrences of a set of variable var with a set of replacements.
	 * Does not do a DeepCopy.
	 * @param orig	the expression we wish to perform the substitution on
	 * @param names	the names of the variable
	 * @param replacements	the expression var is substituted for
	 * @return the tree with variable replace (does not do a DeepCopy)
	 * @throws ParseException
	 */
	public Node substitute(Node orig,String names[],Node replacements[],XJep xj) throws ParseException
	{
		this.names = names;
		this.replacements = replacements;
		this.xjep=xj;
		Node res = (Node) orig.jjtAccept(this,null);
		return res;
	}

	public Object visit(ASTVarNode node, Object data) throws ParseException
	{
		for(int i=0;i<names.length;++i)
		{
			if(names[i].equals(node.getName()))
				return xjep.deepCopy(replacements[i]);
		}
		if(node.getVar().isConstant())
			return xjep.getNodeFactory().buildVariableNode(xjep.getSymbolTable().getVar(node.getName()));
			
		throw new ParseException("No substitution specified for variable "+node.getName());
	}
}
