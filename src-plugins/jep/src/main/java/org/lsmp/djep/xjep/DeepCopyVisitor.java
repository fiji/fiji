/* @author rich
 * Created on 16-Nov-2003
 */
package org.lsmp.djep.xjep;

import org.nfunk.jep.*;

/**
 * A Visitor which returns an exact copy of the tree.
 * This class should be extended by visitors which
 * modify trees and creates a new tree.
 * 
 * @author Rich Morris
 * Created on 16-Nov-2003
 */
public class DeepCopyVisitor extends DoNothingVisitor implements ParserVisitor {

	private XJep xjep;
	/** Creates a deepCopy of a Node **/	
	public Node deepCopy(Node node,XJep xj) throws ParseException
	{
		this.xjep = xj;
		Node res = (Node) node.jjtAccept(this,null);
		return res;
	}

	public Object visit(ASTConstant node, Object data)  throws ParseException
	{
		return xjep.getNodeFactory().buildConstantNode(node);
	}

	public Object visit(ASTVarNode node, Object data)  throws ParseException
	{
		return xjep.getNodeFactory().buildVariableNode(node);
	}

	public Object visit(ASTFunNode node, Object data)  throws ParseException
	{
		Node children[]=acceptChildrenAsArray(node,data);
		return xjep.getNodeFactory().buildFunctionNode(node,children);
	}
}
