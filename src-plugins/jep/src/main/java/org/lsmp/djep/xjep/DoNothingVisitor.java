/* @author rich
 * Created on 16-Nov-2003
 */
package org.lsmp.djep.xjep;

import org.nfunk.jep.*;

/**
 * A Visitor which visits each node of a expression tree.
 * It returns the top node.
 * This visitor should be extended by Visitors which modify trees in place.
 * 
 * @author Rich Morris
 * Created on 16-Nov-2003
 */
public abstract class DoNothingVisitor implements ParserVisitor {

	/*
	 * The following methods was used to facilitate 
	 * using visitors which implemented a interface
	 * which subclassed ParserVisitor.
	 *  
	 * If subclassed to extend to implement a different visitor
	 * this method should be overwritten to ensure the correct 
	 * accept method is called.
	 * This method simply calls the jjtAccept(ParserVisitor this,Object data) of node.
	 *
	 * We no longer need this as we use ParseVisitor everywhere,
	 * but kept for future reference.
	 * 
	private Object nodeAccept(Node node, Object data) throws ParseException
	{
		return node.jjtAccept(this,data);
	}
	*/
	
	/**
	 * Gets the result of visiting children of a array of nodes.
	 */
	
	protected Node[] acceptChildrenAsArray(Node node,Object data)  throws ParseException
	{
		int n = node.jjtGetNumChildren();
		Node children[] = new Node[n];
		for(int i=0;i<n;++i)
			children[i]= (Node) node.jjtGetChild(i).jjtAccept(this,data);
		return children;
	}
	


	public Object visit(SimpleNode node, Object data) throws ParseException
	{
		throw new ParseException(this.toString()+": encountered a simple node, problem with visitor.");
	}

	public Object visit(ASTStart node, Object data) throws ParseException
	{
		throw new ParseException(this.toString()+": encountered a start node, problem with visitor.");
	}


	public Object visit(ASTConstant node, Object data)  throws ParseException
	{
		return node;
	}

	public Object visit(ASTVarNode node, Object data)  throws ParseException
	{
		return node;
	}

	public Object visit(ASTFunNode node, Object data)  throws ParseException
	{
		Node children[] = acceptChildrenAsArray(node,data);
		TreeUtils.copyChildrenIfNeeded(node,children);
		return node;
	}
}
