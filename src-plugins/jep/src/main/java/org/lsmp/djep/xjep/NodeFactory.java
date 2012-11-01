/* @author rich
 * Created on 16-Nov-2003
 */
package org.lsmp.djep.xjep;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
/**
 * This class is used to create nodes of specified types.
 * It can be sub-classed to change the nature of how nodes
 * are constructed. Generally there are two methods for creating
 * nodes, methods which take an existing node and methods which
 * take the components.
 * 
 * @author Rich Morris
 * Created on 16-Nov-2003
 */
public class NodeFactory {
    private XJep xj;
	public NodeFactory(XJep xj) {this.xj=xj;}
	private NodeFactory() {}
	/**
	 * Sets the children of node to be those specified in array.
	 * @param node the node whose children will be set.
	 * @param children an array of nodes which will be the children of the node.
	 */
	public void copyChildren(Node node,Node children[])
	{	
		int nchild = children.length; 
		node.jjtOpen();
		for(int i=0;i<nchild;++i)
		{
			children[i].jjtSetParent(node);
			node.jjtAddChild(children[i],i);
		}
		node.jjtClose();
	}
	
	/** Creates an ASTConstant node with specified value. 
	 * This method should be overwritten by subclasses.
	 * @throws ParseException
	 **/
	public ASTConstant buildConstantNode(Object value) throws ParseException
	{
		ASTConstant node  = new ASTConstant(ParserTreeConstants.JJTCONSTANT);
		node.setValue(value);
		return node;
	}

	/** Create an ASTConstant with same value as argument. *
	 * @throws ParseException*/
	public ASTConstant buildConstantNode(ASTConstant node) throws ParseException
	{
		return buildConstantNode(node.getValue());
	}	


	/** Creates a ASTConstant whose value of applying the operator to its arguments. */
	public ASTConstant buildConstantNode(PostfixMathCommandI pfmc,Node children[]) throws IllegalArgumentException,ParseException
	{
/*		Stack stack = new Stack();
		for(int i=0;i<children.length;++i)
		{
			if(!(children[i] instanceof ASTConstant))
				throw new ParseException("buildConstantNode: arguments must all be constant nodes");
			stack.push(((ASTConstant) children[i]).getValue());
		}
		pfmc.setCurNumberOfParameters(children.length);
		pfmc.run(stack);
		return buildConstantNode(stack.pop());
*/
	    Object val = xj.getEvaluatorVisitor().eval(pfmc,children);
		return buildConstantNode(val);
	}

	/** Creates a ASTConstant whose value of applying the operator to its arguments. */
	public ASTConstant buildConstantNode(Operator op,Node children[]) throws IllegalArgumentException,ParseException
	{
		return buildConstantNode(op.getPFMC(),children);
	}

	/** Creates a ASTConstant whose value of applying binary operator to its arguments. */
	public ASTConstant buildConstantNode(Operator op,Node child1,Node child2) throws IllegalArgumentException,ParseException
	{
		return buildConstantNode(op.getPFMC(),new Node[]{child1,child2});
	}

	/** Creates a ASTConstant whose value of applying a unary operator to its arguments. */
	public ASTConstant buildConstantNode(Operator op,Node child1) throws IllegalArgumentException,ParseException
	{
		return buildConstantNode(op.getPFMC(),new Node[]{child1});
	}

	/** creates a new ASTVarNode with the same name as argument. 
	 * @throws ParseException*/ 
	public ASTVarNode buildVariableNode(ASTVarNode node) throws ParseException
	{
		   return buildVariableNode(node.getVar());
	}
	
	/** creates a new ASTVarNode with a given variable. 
	 * This method should be sub-classed
	 * @throws ParseException
	 */ 
	public ASTVarNode buildVariableNode(Variable var) throws ParseException
	{
		ASTVarNode node  = new ASTVarNode(ParserTreeConstants.JJTVARNODE);
		node.setVar(var);
		return node;
	}

	public ASTVarNode buildVariableNode(String name,Object value) throws ParseException
	{
	    return buildVariableNode(xj.getSymbolTable().addVariable(name,value));
	}
	/**
	 * Builds a operator node with n arguments
	 * This method should be sub-classed
	 * @param op the operator to use
	 * @param arguments the arguments to the function.
	 * @return top Node of expression 
	 * @throws ParseException
	 */
	
	public ASTFunNode buildOperatorNode(Operator op,Node[] arguments) throws ParseException
	{
		ASTFunNode res = new ASTFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setOperator(op);
		copyChildren(res,arguments);
		return res;		
	}
	
	/**
	 * Builds a function with n arguments
	 * This method should be sub-classed
	 * @param name of function.
	 * @param pfmc PostfixMathCommand for function.
	 * @param arguments the arguments to the function.
	 * @return top Node of expression 
	 * @throws ParseException
	 */

	public ASTFunNode buildFunctionNode(String name,PostfixMathCommandI pfmc,Node[] arguments) throws ParseException
	{
		ASTFunNode res = new ASTFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setFunction(name,pfmc);
		copyChildren(res,arguments);
		return res;		
	}


	/** An unfinished node. Caller has responsibility for
	 * filling in the children. */
	public ASTFunNode buildUnfinishedOperatorNode(Operator op)
	{
		ASTFunNode res = new ASTFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setOperator(op);
		return res;		
	}
	
	/** creates a unary function. 
	 * @throws ParseException*/
	
	public ASTFunNode buildOperatorNode(Operator op,Node child) throws ParseException
	{
		return buildOperatorNode(op,new Node[]{child});		
	}
	
	/** creates a binary function. 
	 * @throws ParseException*/
	
	public ASTFunNode buildOperatorNode(Operator op,Node lhs,Node rhs) throws ParseException
	{
		return buildOperatorNode(op,new Node[]{lhs,rhs});		
	}
	
	/**
	 * Builds a function with n arguments and same fun as specified in arguments.
	 * @param node the properties (name and pfmc) of this node will be copied.
	 * @param arguments the arguments to the function.
	 * @return top Node of expression 
	 * @throws ParseException
	 */
	public ASTFunNode buildFunctionNode(ASTFunNode node,Node[] arguments) throws ParseException
	{
		if(node.getOperator()!=null)
			return buildOperatorNode(node.getOperator(),arguments); 
		return buildFunctionNode(node.getName(),node.getPFMC(),arguments);
	}

}
