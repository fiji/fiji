/* @author rich
 * Created on 16-Nov-2003
 */
package org.lsmp.djep.matrixJep;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.function.*;
import org.lsmp.djep.xjep.*;

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
public class MatrixNodeFactory extends NodeFactory {

	public MatrixNodeFactory(XJep xj)
	{
	    super(xj);
	}
	
	/** Creates an ASTConstant node with specified value. **/
	public ASTConstant buildConstantNode(Object value) throws ParseException
	{
		ASTMConstant node  = new ASTMConstant(ParserTreeConstants.JJTCONSTANT);
		node.setValue(value);
		return node;
	}

	/** Creates a ASTVariable node with specified value. **/
	public ASTVarNode buildVariableNode(Variable var) throws ParseException
	{
		ASTMVarNode node  = new ASTMVarNode(ParserTreeConstants.JJTVARNODE);
		node.setVar(var);
		return node;
	}
	

	/**
	 * Builds a function with n arguments
	 * @param name of function.
	 * @param pfmc PostfixMathCommand for function.
	 * @param arguments the arguments to the function.
	 * @return top Node of expression 
	 */

	public ASTFunNode buildFunctionNode(String name,PostfixMathCommandI pfmc,Node[] arguments) throws ParseException
	{
		ASTMFunNode res = new ASTMFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setFunction(name,pfmc);
		copyChildren(res,arguments);
		res.setDim(calcDim(name,pfmc,arguments));
		return res;		
	}

	/** Calculates the dimension of node using the dimensions
	 * of the children. Does not recurse down the tree.
	 */
	public Dimensions calcDim(String name,PostfixMathCommandI pfmc,Node arguments[])
		throws ParseException
	{
		MatrixNodeI children[] = new MatrixNodeI[arguments.length];
		for(int i=0;i<arguments.length;++i)
			children[i] = (MatrixNodeI) arguments[i];

		if(pfmc instanceof BinaryOperatorI)
		{
			if(children.length!=2) throw new ParseException("Operator "+name+" must have two elements, it has "+children.length);
			BinaryOperatorI bin = (BinaryOperatorI) pfmc;
			Dimensions dim = bin.calcDim(children[0].getDim(),children[1].getDim());
			return dim;
		}
		else if(pfmc instanceof UnaryOperatorI)
		{
			if(children.length!=1) throw new ParseException("Operator "+name+" must have one elements, it has "+children.length);
			UnaryOperatorI uni = (UnaryOperatorI) pfmc;
			Dimensions dim = uni.calcDim(children[0].getDim());
			return dim;
		}
		else if(pfmc instanceof NaryOperatorI)
		{
			Dimensions dims[] = new Dimensions[children.length];
			for(int i=0;i<children.length;++i)
				dims[i]=children[i].getDim();
			//if(arguments.length!=1) throw new ParseException("Operator "+op.getName()+" must have one elements, it has "+arguments.length);
			NaryOperatorI uni = (NaryOperatorI) pfmc;
			Dimensions dim = uni.calcDim(dims);
			return dim;
		}
		else
		{
			return Dimensions.ONE;
	//		System.out.println("Warning: assuming 1 for dimensions of "+node.getName());
		}
	}

	/** Calculates the dimension of node using the dimensions
	 * of the children.
	 */
	public Dimensions calcDim(Operator op,Node arguments[])	throws ParseException
	{
		return calcDim(op.getName(),op.getPFMC(),arguments);
	}
	
	/**
	 * Builds a function with n arguments
	 * @param node the properties (name and pfmc) of this node will be copied.
	 * @param children the arguments to the function.
	 * @return top Node of expression 
	 * 
	 * @since 2.3.3 if possible use dimension of existing node. (Needed when deep copying MList functions)
	 */
	public ASTFunNode buildFunctionNode(ASTFunNode node,Node[] children) throws ParseException
	{
		if(node instanceof ASTMFunNode)
		{
			if(node.isOperator())
				return buildOperatorNode(node.getOperator(),children,
				((ASTMFunNode) node).getDim()); 
			ASTMFunNode res = new ASTMFunNode(ParserTreeConstants.JJTFUNNODE);
			res.setFunction(node.getName(),node.getPFMC());
			copyChildren(res,children);
			res.setDim(((ASTMFunNode) node).getDim()); 
			return res;
		}
		//MatrixNodeI children[] = (MatrixNodeI []) arguments;
		if(node.isOperator())
			return buildOperatorNode(node.getOperator(),children); 
		ASTMFunNode res = new ASTMFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setFunction(node.getName(),node.getPFMC());
		copyChildren(res,children);
		res.setDim(calcDim(node.getName(),node.getPFMC(),children));
		return res;		
	}
	
	/** create a function node with a known dimension */
	public ASTFunNode buildFunctionNode(ASTFunNode node,Node[] arguments,Dimensions dim)
	{
		ASTMFunNode res = new ASTMFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setFunction(node.getName(),node.getPFMC());
		res.setDim(dim);
		copyChildren(res,arguments);
		return res;		
	}

	
	/**
	 * Builds a operator node with n arguments
	 * @param op the operator for this node
	 * @param arguments the arguments to the function.
	 * @return top Node of expression 
	 */

	public ASTFunNode buildOperatorNode(Operator op,Node[] arguments) throws ParseException
	{
			
		ASTMFunNode res = new ASTMFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setOperator(op);
		copyChildren(res,arguments);
		res.setDim(calcDim(op,arguments));
		return res;
	}

	/** create a function node with a known dimension */
	public ASTFunNode buildOperatorNode(Operator op,Node[] arguments,Dimensions dim)
	{
		ASTMFunNode res = new ASTMFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setOperator(op);
		res.setDim(dim);
		copyChildren(res,arguments);
		return res;		
	}

	/**
	 * Creates an operator node, but don't fill in the children or calculate
	 * its dimension.
	 */
	public ASTFunNode buildUnfinishedOperatorNode(Operator op)
	{
		ASTMFunNode res = new ASTMFunNode(ParserTreeConstants.JJTFUNNODE);
		res.setOperator(op);
		return res;		
	}
}
