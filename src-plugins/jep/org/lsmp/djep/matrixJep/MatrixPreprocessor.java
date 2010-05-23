/* @author rich
 * Created on 30-Oct-2003
 */
package org.lsmp.djep.matrixJep;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import org.lsmp.djep.djep.*;
import org.lsmp.djep.xjep.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.function.*;
/**
 * This visitor does the majority of preprocessing work.
 * Specifically it
 * <ul>
 * <li>Sets the dimension of each node.
 * <li>For assignment equations it adds an entry in the VariableTable
 * <li>For diff opperator it calculates the derivative.
 * <li>For the List opperator it finds the dimensions and
 * returns a ASTTensor.
 * <li>For the Hat opperator it finds the dimension and returns
 * a Power or Wedge opperator.
 * </ul>
 * The visitor will return a new Tree.
 * 
 * @author Rich Morris
 * Created on 30-Oct-2003
 */
public class MatrixPreprocessor implements ParserVisitor
{
	private MatrixJep mjep;
	private MatrixNodeFactory nf;
	private DSymbolTable vt;

	public MatrixPreprocessor() {}

	/**
	 * Main entry point: pre-process a node. 
	 * @param node	Top node of tree. 
	 * @param mdjep	Reference to MatrixJep instance
	 * @return	A new tree with all preprocessing carried out.
	 * @throws ParseException
	 */	
	public MatrixNodeI preprocess(Node node,MatrixJep mdjep) throws ParseException
	{
		this.mjep=mdjep;
		this.nf=(MatrixNodeFactory) mdjep.getNodeFactory();
		this.vt=(DSymbolTable) mdjep.getSymbolTable();
		return (MatrixNodeI) node.jjtAccept(this,null);
	}
	
	/**
	 * Returns an array of matrix nodes which are the results of visiting each child.
	 */
	public MatrixNodeI[] visitChildrenAsArray(Node node,Object data) throws ParseException 
	{
		int nchild = node.jjtGetNumChildren();
		MatrixNodeI children[] = new MatrixNodeI[nchild];
		for(int i=0;i<nchild;++i)
		{
		  MatrixNodeI no = (MatrixNodeI) node.jjtGetChild(i).jjtAccept(this,data);
		  children[i] = no;
		}
		return children;
	}
	
	////////////////////////////////////////////////////////////////////
	
	public Object visit(SimpleNode node, Object data)	{ return null;	}
	public Object visit(ASTStart node, Object data)	{ return null;	}

	/** constants **/
	public Object visit(ASTConstant node, Object data) throws ParseException
	{
		return nf.buildConstantNode(node.getValue());
	}
	/** multi-dimensional differentiable variables */
	public Object visit(ASTVarNode node, Object data) throws ParseException
	{
		return nf.buildVariableNode(vt.getVar(node.getName()));
	}

	/** visit functions and operators **/
	public Object visit(ASTFunNode node, Object data) throws ParseException
	{
		PostfixMathCommandI pfmc=node.getPFMC();
		if(pfmc instanceof SpecialPreProcessorI)
		{
			SpecialPreProcessorI spp = (SpecialPreProcessorI) node.getPFMC();
			return spp.preprocess(node,this,mjep,nf);
		}
		if(node.isOperator()) return visitOp(node,data);
		if(node.getPFMC() instanceof CommandVisitorI)
				throw new IllegalArgumentException("MatrixPreprocessor: encountered and instance of CommandVisitorI  for function "+node.getName());

		MatrixNodeI children[] = visitChildrenAsArray(node,data);
		ASTMFunNode res = (ASTMFunNode) nf.buildFunctionNode(node,children);
		return res;
	}

	/** operators +,-,*,/ **/
	public Object visitOp(ASTFunNode node, Object data) throws ParseException
	{
		PostfixMathCommandI pfmc=node.getPFMC();
		MatrixNodeI children[] = visitChildrenAsArray(node,data);

		if(pfmc instanceof BinaryOperatorI)
		{
			if(node.jjtGetNumChildren()!=2) throw new ParseException("Operator "+node.getOperator().getName()+" must have two elements, it has "+children.length);
			BinaryOperatorI bin = (BinaryOperatorI) pfmc;
			Dimensions dim = bin.calcDim(children[0].getDim(),children[1].getDim());
			return (ASTMFunNode) nf.buildOperatorNode(node.getOperator(),children,dim);
		}
		else if(pfmc instanceof UnaryOperatorI)
		{
			if(children.length!=1) throw new ParseException("Operator "+node.getOperator().getName()+" must have one elements, it has "+children.length);
			UnaryOperatorI uni = (UnaryOperatorI) pfmc;
			Dimensions dim = uni.calcDim(children[0].getDim());
			return (ASTMFunNode) nf.buildOperatorNode(node.getOperator(),children,dim);
		}
		else if(pfmc instanceof NaryOperatorI)
		{
			Dimensions dims[] = new Dimensions[children.length];
			for(int i=0;i<children.length;++i)
				dims[i]=children[i].getDim();
			NaryOperatorI uni = (NaryOperatorI) pfmc;
			Dimensions dim = uni.calcDim(dims);
			return (ASTMFunNode) nf.buildOperatorNode(node.getOperator(),children,dim);
		}
		else
		{
			//throw new ParseException("Operator must be unary or binary. It is "+op);
			Dimensions dim = Dimensions.ONE;
			return (ASTMFunNode) nf.buildOperatorNode(node.getOperator(),children,dim);
		}
	}
}
