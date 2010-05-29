   
package org.lsmp.djep.xjep;
//import org.lsmp.djep.matrixParser.*;
import org.nfunk.jep.*;

/**
 * Simplifies an expression.
 * To use
 * <pre>
 * JEP j = ...; Node in = ...;
 * SimplificationVisitor sv = new SimplificationVisitor(tu);
 * Node out = sv.simplify(in);
 * </pre>
 * 
 * <p>
 * Its intended to completly rewrite this class to that simplification
 * rules can be specified by strings in a way similar to DiffRulesI.
 * It also would be nice to change the rules depending on the type of
 * arguments, for example matrix multiplication is not commutative.
 * But some of the in built rules exploit commutativity.
 * 
 * @author Rich Morris
 * Created on 20-Jun-2003
 * TODO cope with 'a - (-1) * b'
 * TODO cope with '0 - uminus(b)' 
 * TODO cope with simplifying complex numbers
 */

public class SimplificationVisitor extends DoNothingVisitor
{
  private NodeFactory nf;
  private OperatorSet opSet;
  private TreeUtils tu;
  
  public SimplificationVisitor()
  {
  }

  /** must be implemented for subclasses. **/
  public Node simplify(Node node,XJep xjep) throws ParseException,IllegalArgumentException
  {
	nf = xjep.getNodeFactory();
	opSet = xjep.getOperatorSet();
	tu = xjep.getTreeUtils();
	
	if (node == null) return null;
//		throw new IllegalArgumentException(
//			"topNode parameter is null");
	Node res = (Node) node.jjtAccept(this,null);
	return res;
  }
	/** First create a new node and then simplify it. */
	public Node simplifyBuiltOperatorNode(Operator op,Node lhs,Node rhs) throws ParseException
	{
		ASTFunNode res = nf.buildOperatorNode(op,lhs,rhs);
		Node res2 = simplifyOp(res,new Node[]{lhs,rhs});
		return res2;
	}
 	/**
 	 * Simplifies expressions like 2+(3+x) or (2+x)+3
 	 * 
 	 * @param op the root operator
 	 * @param lhs the left hand side node
 	 * @param rhs the right hand side node
 	 * @return null if no rewrite happens or top node or top node of new tree.
 	 * @throws ParseException
 	 */
	public Node simplifyTripple(XOperator op,Node lhs,Node rhs) throws ParseException
	{
		
		XOperator rootOp;
		if(op.isComposite()) rootOp = (XOperator) op.getRootOp();
		else				 rootOp = op;

		if(op.isCommutative() && tu.isConstant(rhs))
		{
			return simplifyBuiltOperatorNode(op,rhs,lhs);
		}			
		if(tu.isConstant(lhs) && tu.isBinaryOperator(rhs))
		{
			Node rhsChild1 = rhs.jjtGetChild(0);
			Node rhsChild2 = rhs.jjtGetChild(1);
			XOperator rhsOp = (XOperator) ((ASTFunNode) rhs).getOperator();
			XOperator rhsRoot;
			if(rhsOp.isComposite())	rhsRoot = (XOperator) rhsOp.getRootOp();
			else					rhsRoot = rhsOp;
	
			if(tu.isConstant(rhsChild1))	
			{
				XOperator op2 = rootOp;
				if(op == rhsOp) op2 = rootOp;
				else			op2 = (XOperator) rootOp.getBinaryInverseOp();

				//	2 + ~( 3 + ~x ) -> (2+~3) + ~~x
				if(rootOp == rhsRoot && rootOp.isAssociative()) 
				{
					Node newnode = simplifyBuiltOperatorNode(op2,
						nf.buildConstantNode(op,lhs,rhsChild1),rhsChild2);
					return newnode;
				}
			
				if(op.isDistributiveOver(rhsRoot))	// 2 * (3 + ~x) -> (2 * 3) + ~(2 @ x)
				{
					Node newnode = simplifyBuiltOperatorNode(rhsOp,
						nf.buildConstantNode(op,lhs,rhsChild1),
						simplifyBuiltOperatorNode(op,lhs,rhsChild2));
					return newnode;
				}
			}


			if(tu.isConstant(rhsChild2))	
			{
				// 2 + ~( x + ~3 ) -> (2 + ~~3) + ~x

				Operator op2 = rootOp;
				if(op == rhsOp) op2 = rootOp;
				else			op2 = rootOp.getBinaryInverseOp();

				if(rootOp == rhsRoot && rootOp.isCommutative() && rootOp.isAssociative())
				{
					Node newnode = simplifyBuiltOperatorNode(op,
						nf.buildConstantNode(op2,lhs,rhsChild2),rhsChild1);
					return newnode;
				}
			
				if(op.isDistributiveOver(rhsRoot))	// 2 * (x + ~3) -> (2 * x) + ~(2 * 3)
				{
					Node newnode = simplifyBuiltOperatorNode(rhsOp,
						simplifyBuiltOperatorNode(op,lhs,rhsChild1),
						nf.buildConstantNode(op,lhs,rhsChild2));
					return newnode;
				}
			}
		}

		if(tu.isBinaryOperator(lhs) && tu.isConstant(rhs))
		{
			Node lhsChild1 = lhs.jjtGetChild(0);
			Node lhsChild2 = lhs.jjtGetChild(1);
			XOperator lhsOp = (XOperator) ((ASTFunNode) lhs).getOperator();
			XOperator lhsRoot;
			if(lhsOp.isComposite())	lhsRoot = (XOperator) lhsOp.getRootOp();
			else					lhsRoot = lhsOp;
	
			if(tu.isConstant(lhsChild1))	
			{
				// (2 + ~x) + ~3    ->   (2 + ~3) + ~x
				if(rootOp == lhsRoot && rootOp.isAssociative() && rootOp.isCommutative())
				{
					Node newnode = simplifyBuiltOperatorNode(lhsOp,
						nf.buildConstantNode(op,lhsChild1,rhs),
						lhsChild2);
					return newnode;
				}
			
				// (2 + ~x) * 3    -->  (2*3) +~ (x*3)
				if(op.isDistributiveOver(lhsRoot)) 
				{
					Node newnode = simplifyBuiltOperatorNode(lhsOp,
						nf.buildConstantNode(op,lhsChild1,rhs),
						simplifyBuiltOperatorNode(op,lhsChild2,rhs));
					return newnode;
				}
			}


			if(tu.isConstant(lhsChild2))	
			{
				// (x + ~2) + !3 -> x + (~2 + !3) -> x + ~(2+~!3)
				// (x*2)*3 -> x*(2*3), (x/2)*3 -> x/(2/3)
				// (x*2)/3 -> x*(2/3), (x/2)/3 -> x/(2*3) 
				if(rootOp == lhsRoot && rootOp.isAssociative())
				{
					Operator op2 = rootOp;
					if(op == lhsOp) op2 = rootOp;
					else			op2 = rootOp.getBinaryInverseOp();
					
					Node newnode = simplifyBuiltOperatorNode(lhsOp,
						lhsChild1,
						nf.buildConstantNode(op2,lhsChild2,rhs));
					return newnode;
				}
			
				// (x + ~2) * 3 -> (x*3) + ~(2*3)
				if(op.isDistributiveOver(lhsRoot))
				{
					Node newnode = simplifyBuiltOperatorNode(lhsOp,
						simplifyBuiltOperatorNode(op,lhsChild1,rhs),
						nf.buildConstantNode(op,lhsChild2,rhs));
					return newnode;
				}
			}
		}
		return null;
	}

  /**
   * Simplifies an addition. Performs the following rules
   * <pre>
   * 0+x -> x
   * x+0 -> x
   * m+n -> (m+n) where m,n are numbers
   * x - (-2) -> x + 2 for any negative number -2
   * x + (-2) -> x - 2 for any negative number -2
   * 2 +/- ( 3 +/- x ) ->  (2 +/- 3 ) +/- x and similar
   * </pre>
   */
  
  public Node simplifyAdd(Node lhs,Node rhs) throws ParseException
  {
	if(tu.isInfinity(lhs))
	{	// Inf + Inf -> NaN TODO not correct for signed infinity 
		if(tu.isInfinity(rhs))
			return nf.buildConstantNode(tu.getNAN());
		// Inf + x -> Inf
		return nf.buildConstantNode(tu.getPositiveInfinity());
	}
	if(tu.isInfinity(rhs)) // x + Inf -> Inf
		return nf.buildConstantNode(tu.getPositiveInfinity());
	  
	if(tu.isZero(lhs))	// 0+x -> x
		return rhs;
	if(tu.isZero(rhs))	// x + 0 -> x
		return lhs;

	if(tu.isNegative(lhs)) // -3 + x -> x - 3
	{
		Node newnode = nf.buildOperatorNode(opSet.getSubtract(),
			rhs,
			nf.buildConstantNode(opSet.getUMinus(),lhs));
		return newnode;
	}
	if(tu.isNegative(rhs)) // x + -3 -> x - 3
	{
		Node newnode = nf.buildOperatorNode(opSet.getSubtract(),
			lhs,
			nf.buildConstantNode(opSet.getUMinus(),rhs));
		return newnode;
	}
	return null;
//	return nf.buildOperatorNode(node.getOperator(),lhs,dimKids[1]);
//	return opSet.buildAddNode(lhs,dimKids[1]);
  }

  /**
   * Simplifies a subtraction. Performs the following rules
   * <pre>
   * 0-x -> 0-x
   * x-0 -> x
   * m-n -> (m-n) where m,n are numbers
   * x - (-2) -> x + 2 for any negative number -2
   * x + (-2) -> x - 2 for any negative number -2
   * 2 +/- ( 3 +/- x ) ->  (2 +/- 3 ) +/- x and similar
   * </pre>
   * @param lhs the left hand side
   * @param rhs the right hand side
   */
  
  public Node simplifySubtract(Node lhs,Node rhs) throws ParseException
  {
	if(tu.isInfinity(lhs))
	{	// Inf + Inf -> NaN TODO not correct for signed infinity 
		if(tu.isInfinity(rhs))
			return nf.buildConstantNode(tu.getNAN());
		// Inf + x -> Inf
		return nf.buildConstantNode(tu.getPositiveInfinity());
	}
	if(tu.isInfinity(rhs)) // x + Inf -> Inf
		return nf.buildConstantNode(tu.getPositiveInfinity());

	if(tu.isZero(rhs))	// x - 0 -> x
		return lhs;
	// TODO implement 0 - x -> -(x)
	
	if(tu.isNegative(rhs)) // x - (-2) -> x + 2
	{
		Node newnode = simplifyBuiltOperatorNode(opSet.getAdd(),
			lhs,
			nf.buildConstantNode(opSet.getUMinus(),rhs));
		return newnode;
	}
	
	if(tu.getOperator(rhs)==opSet.getUMinus())
	{
		Node newnode = simplifyBuiltOperatorNode(opSet.getAdd(),
			lhs,
			rhs.jjtGetChild(0));
		return newnode;
	}
/*	if(tu.getOperator(rhs)==opSet.getMultiply())
	{
		if(tu.isNegative(rhs.jjtGetChild(0))) // a - (-2) * b -> a + 2 * b
		{
			Node newnode = simplifyBuiltOperatorNode(
				opSet.getAdd(),
				lhs,
				nf.buildOperatorNode(
					opSet.getMultiply(),
					nf.buildConstantNode(
						opSet.getUMinus(),rhs.jjtGetChild(0)),
					rhs.jjtGetChild(1)));
			return newnode;
		}
	}
*/
	return null;
//	return nf.buildOperatorNode(((ASTOpNode) node).getOperator(),lhs,rhs);
//	return tu.buildSubtract(lhs,rhs);
  }
  
  /**
   * Simplifies a multiplication.
   * <pre>
   * 0 * Inf -> NaN
   * 0 * x -> 0
   * x * 0 -> 0
   * 1 * x -> x
   * x * 1 -> x
   * Inf * x -> Inf
   * x * Inf -> Inf
   * 2 * ( 3 * x) -> (2*3) * x
   * and similar.
   * </pre>
   */
  
  public Node simplifyMultiply(Node child1,Node child2) throws ParseException
  {
	if(tu.isZero(child1))
	{	// 0*Inf -> NaN 
		if(tu.isInfinity(child2))
			return nf.buildConstantNode(tu.getNAN());
		// 0*x -> 0
		return nf.buildConstantNode(tu.getZERO());
	}
	if(tu.isZero(child2))
	{ // Inf*0 -> NaN
		if(tu.isInfinity(child1))
			return nf.buildConstantNode(tu.getNAN());
		// 0 * x -> 0
		return nf.buildConstantNode(tu.getZERO());
	}
	if(tu.isInfinity(child1)) // Inf * x -> Inf
			return nf.buildConstantNode(tu.getPositiveInfinity());
	if(tu.isInfinity(child2)) // x * Inf -> Inf
			return nf.buildConstantNode(tu.getPositiveInfinity());
	  			  
	if(tu.isOne(child1))	// 1*x -> x
			  return child2;
	if(tu.isOne(child2))	// x*1 -> x
			  return child1;
	
	if(tu.isMinusOne(child1))	// -1*x -> -x
	{
		Node newnode = nf.buildOperatorNode(opSet.getUMinus(),child2);
		return newnode;
	}

	if(tu.isMinusOne(child2))	// x*-1 -> -x
	{
		Node newnode = nf.buildOperatorNode(opSet.getUMinus(),child1);
		return newnode;
	}
	return null;
//	return nf.buildOperatorNode(((ASTOpNode) node).getOperator(),child1,child2);
//  return tu.buildMultiply(child1,child2);
	}
	/**
	 * Simplifies a division.
	 * <pre>
	 * 0/0 -> NaN
	 * 0/Inf -> Inf
	 * 0/x -> Inf
	 * x/0 -> Inf
	 * x/1 -> x
	 * Inf / x -> Inf
	 * x / Inf -> 0
	 * 2 / ( 3 * x) -> (2/3) / x
	 * 2 / ( x * 3) -> (2/3) / x
	 * 2 / ( 3 / x) -> (2/3) * x
	 * 2 / ( x / 3) -> (2*3) / x
	 * (2 * x) / 3 -> (2/3) * x
	 * (x * 2) / 3 -> x * (2/3)
	 * (2 / x) / 3 -> (2/3) / x
	 * (x / 2) / 3 -> x / (2*3)
	 * </pre>
	 */
	public Node simplifyDivide(Node child1,Node child2) throws ParseException
	{
	  if(tu.isZero(child2))
	  {
		if(tu.isZero(child1))	// 0/0 -> NaN
			return nf.buildConstantNode(tu.getNAN());
		// x/0 -> Inf
		return nf.buildConstantNode(tu.getPositiveInfinity());
	  }
		  
	  if(tu.isZero(child1))
	  {		// 0/x -> 0
		return child1;
	  }
	  //if(tu.isOne(child1))	// 1/x -> 1/x
	  //		  return child2;
	  if(tu.isOne(child2))	// x/1 -> x
			  return child1;
			
	  if(tu.isInfinity(child1)) // Inf / x -> Inf
			  return nf.buildConstantNode(tu.getPositiveInfinity());
	  if(tu.isInfinity(child2)) // x / Inf -> 0
			  return nf.buildConstantNode(tu.getZERO());
  	  return null;
//	  return nf.buildOperatorNode(((ASTOpNode) node).getOperator(),child1,child2);
//	  return opSet.buildDivideNode(child1,child2);
	}

	/** Simplify a power.
	 * <pre>
	 * x^0 -> 1
	 * x^1 -> x
	 * 0^0 -> NaN
	 * 0^x -> 0
	 * 1^x -> 1
	 * </pre>
	 */
	public Node simplifyPower(Node child1,Node child2) throws ParseException
	{
		if(tu.isZero(child1))
		{
			if(tu.isZero(child2))	// 0^0 -> NaN
				return nf.buildConstantNode(tu.getNAN());
			// 0^x -> 0
			return nf.buildConstantNode(tu.getZERO());
		}
		if(tu.isZero(child2))	// x^0 -> 1
			return nf.buildConstantNode(tu.getONE());
		if(tu.isOne(child1))	// 1^x -> 1
			return nf.buildConstantNode(tu.getONE());
		if(tu.isOne(child2))	// x^1 -> x
			return child1;
			
		if(tu.isConstant(child2) && tu.getOperator(child1) == opSet.getPower())
		{
			if(tu.isConstant(child1.jjtGetChild(1)))
			{
				/* (x^3)^4 -> x^(3*4) */
				return nf.buildOperatorNode(
					opSet.getPower(),
					child1.jjtGetChild(0),
					nf.buildConstantNode(
						opSet.getMultiply(),
						child1.jjtGetChild(1),
						child2));
			}
		}
		return null;	
//		return nf.buildOperatorNode(((ASTOpNode) node).getOperator(),child1,child2);
//		return tu.buildPower(child1,child2);
	}

	/** simplifies operators, does not descend into children */

	public Node simplifyOp(ASTFunNode node,Node children[]) throws ParseException
	{
		boolean allConst=true;
		XOperator op= (XOperator) node.getOperator();
		// TODO a bit of a hack to prevent lists of constants being converted
		// what happens is that for [[1,2],[3,4]] the dimension is not passed
		// into buildConstantNode so list is treated as [1,2,3,4]
		// Ideally there would be a special simplification rule for List 
		if(op.getPFMC() instanceof org.nfunk.jep.function.List) return node;
		int nchild=children.length;
		for(int i=0;i<nchild;++i)
		{
			if(!tu.isConstant(children[i]))
				allConst=false;
			if(tu.isNaN(children[i]))
				return nf.buildConstantNode(tu.getNAN());
		}	
		if(allConst)
			return nf.buildConstantNode(op,children);
		
		if(nchild==1)
		{
			if(tu.isUnaryOperator(children[0]) && op == tu.getOperator(children[0]))
			{
				if(op.isSelfInverse()) return children[0].jjtGetChild(0);
			}
		}
		if(nchild==2)
		{
			Node res=null;
			if(opSet.getAdd() == op) res = simplifyAdd(children[0],children[1]);
			if(opSet.getSubtract() == op) res = simplifySubtract(children[0],children[1]);
			if(opSet.getMultiply() == op) res = simplifyMultiply(children[0],children[1]);
			if(opSet.getDivide() == op) res = simplifyDivide(children[0],children[1]);
			if(opSet.getPower() == op) res = simplifyPower(children[0],children[1]);
			if(res!=null)
			{
				if(tu.isConstant(res)) return res;
				if(tu.isOperator(res))
				{
					Node res2 = simplifyOp((ASTFunNode) res,TreeUtils.getChildrenAsArray(res));
					return res2;
				} 
				return res;
			}
			res = this.simplifyTripple(op,children[0],children[1]);
			if(res!=null)
			{
				if(tu.isConstant(res)) return res;
				if(tu.isOperator(res))
				{
					Node res2 = simplifyOp((ASTFunNode) res,TreeUtils.getChildrenAsArray(res));
					return res2;
				} 
				return res;
			}
		}
		return node;
	}
	
	public Object visit(ASTFunNode node, Object data) throws ParseException
	{
		int nchild = node.jjtGetNumChildren();

		if(node.isOperator())
		{
			XOperator op= (XOperator) node.getOperator();
			if( (op.isBinary() && nchild !=2)
			 || (op.isUnary() && nchild !=1))
			 throw new ParseException("Wrong number of children for "+nchild+" for operator "+op.getName());
	
			Node children[] = acceptChildrenAsArray(node,data);
			TreeUtils.copyChildrenIfNeeded(node,children);
	
			Node res = simplifyOp(node,children);
			if(res == null)
				throw new ParseException("null res from simp op");
			return res;
		}		
		
		Node children[] = acceptChildrenAsArray(node,data);

		boolean allConst=true;
		for(int i=0;i<nchild;++i)
		{
			if(!tu.isConstant(children[i]))
				allConst=false;
			if(tu.isNaN(children[i]))
				return nf.buildConstantNode(tu.getNAN());
		}	
		if(allConst)
			return nf.buildConstantNode(node.getPFMC(),children);
	
		return TreeUtils.copyChildrenIfNeeded(node,children);
		
	}
}
