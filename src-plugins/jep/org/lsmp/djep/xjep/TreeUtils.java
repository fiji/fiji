/*
 * Created on 16-Jun-2003 by Rich webmaster@pfaf.org
 * www.singsurf.org
 */

package org.lsmp.djep.xjep;

import org.nfunk.jep.*;
import org.nfunk.jep.type.*;

/**
 * A set of Utility functions for working with JEP expression trees.
 * Main methods are
 * <ul>
 * <li> {@link #isConstant isConstant} test if its a constant. Many other is... methods.
 * <li> {@link #getValue getValue} extracts the value from a node without needing to cast and check types.
 * </ul>
 * @author rich
 */
public class TreeUtils {
	/** Real zero. Note that this is a Double, if a different number
	 * format is needed then this class should be sub-classed.
	 */
	protected static Double ZERO = new Double(0.0);
	/** Real One */
	protected static Double ONE = new Double(1.0);
	/** Real Minus One */
	protected static Double MINUSONE = new Double(-1.0);
	/** Complex Zero **/
	protected static Complex CZERO = new Complex(0.0,0.0);
	/** Complex One **/
	protected static Complex CONE = new Complex(1.0,0.0);
	/** Complex i **/
	protected static Complex CI = new Complex(0.0,1.0);
	/** Complex Minus One **/
	protected static Complex CMINUSONE = new Complex(-1.0,0.0);
	/** Complex Minus i **/
	protected static Complex CMINUSI = new Complex(0.0,-1.0);
	/** Real NaN */
	protected static Double NAN = new Double(Double.NaN);
	/** Real positive infinity */
	protected static Double PosInf = new Double(Double.POSITIVE_INFINITY);
	/** Real NaN */
	protected static Double NegInf = new Double(Double.NEGATIVE_INFINITY);
	
	/**
	 * Default constructor.
	 * TODO Should use the NumberFactory to create numbers!
	 */
	public TreeUtils() {}
	//public static TreeUtils getInstance() { return INSTANCE; }
	
	/**
	 * Returns the value represented by node
	 * @throws IllegalArgumentException if given something which is not an ASTConstant
	 */
	public String getName(Node node) throws IllegalArgumentException
	{
		if(isVariable(node))
			return ((ASTVarNode) node).getName();
		if(isFunction(node))
			return ((ASTFunNode) node).getName();
		
		throw new IllegalArgumentException("Tried to find the name of constant node");
	}

	/** gets the PostfixMathCommand with a given name. */
	/*
	public PostfixMathCommandI getPfmc(String name)
	{
		return (PostfixMathCommandI) myFunTab.get(name);
	}
	*/
	
	/**
	 * Returns the value represented by node
	 * @throws IllegalArgumentException if given something which is not an ASTConstant
	 */
	public Object getValue(Node node) throws IllegalArgumentException
	{
		if(!isConstant(node)) throw new IllegalArgumentException("Tried to find the value of a non constant node");
		return ((ASTConstant) node).getValue();
	}
	
	/**
	 * Returns the double value represented by node
	 * @throws IllegalArgumentException if given something which is not an ASTConstant with a Double value
	 */
	public double doubleValue(Node node) throws IllegalArgumentException
	{
		return ((Double) getValue(node)).doubleValue();
	}
	/**
	 * Returns the long value represented by node
	 * @throws IllegalArgumentException if given something which is not an ASTConstant with a Double value
	 */
	public long longValue(Node node) throws IllegalArgumentException
	{
		return ((Number) getValue(node)).longValue();
	}
	/**
	 * Returns the int value represented by node
	 * @throws IllegalArgumentException if given something which is not an ASTConstant with a Double value
	 */
	public int intValue(Node node) throws IllegalArgumentException
	{
		return ((Number) getValue(node)).intValue();
	}

	/**
	 * Returns the Complex value represented by node
	 * @throws IllegalArgumentException if given something which is not an ASTConstant with a Complex value
	 */
	public Complex complexValue(Node node) throws IllegalArgumentException
	{
		return ((Complex) getValue(node));
	}

	/**
	 * returns true if node is a ASTConstant 
	 */
	public boolean isConstant(Node node)
	{
		return (node instanceof ASTConstant);
	}
	 
	/**
	 * returns true if node is a ASTConstant with Double value 
	 */
	public boolean isReal(Node node)
	{
			return (node instanceof ASTConstant)
			 && ( ((ASTConstant) node).getValue() instanceof Double );
	}
	
	/**
	 * returns true if node is a ASTConstant with Double value representing an integer.
	 */
	public boolean isInteger(Node node)
	{
			if(isReal(node))
			{
				Number val = (Number) ((ASTConstant) node).getValue();
				double x = val.doubleValue();
				double xInt = Math.rint(x);
				return x == xInt;
			}
			return false;
	}

	/**
	 * returns true if node is a ASTConstant with value Double(0) or Complex(0,0) 
	 */
	public boolean isZero(Node node)
	{
		   return ( isReal(node)
					&& ( ((ASTConstant) node).getValue().equals(ZERO)) )
				||( isComplex(node)
					&& ( ((Complex) ((ASTConstant) node).getValue()).equals(CZERO,0.0) ) );
	}

	/**
	 * returns true if node is a ASTConstant with value Double(0) or Complex(0,0)
	 * @param tol	tolerance for testing for zero
	 */
	
	public boolean isZero(Node node,double tol)
	{
		   return ( isReal(node)
					&&
					(  (((ASTConstant) node).getValue().equals(ZERO)) )
					 || Math.abs(doubleValue(node)) < tol )
				||( isComplex(node)
					&& ( ((Complex) ((ASTConstant) node).getValue()).equals(CZERO,tol) ) );
	}

	/**
	 * returns true if node is a ASTConstant with value Double(1) or Complex(1,0) 
	 */
	public boolean isOne(Node node)
	{
		return ( isReal(node)
				 && ( ((ASTConstant) node).getValue().equals(ONE)) )
			 ||( isComplex(node)
				 && ( ((Complex) ((ASTConstant) node).getValue()).equals(CONE,0.0) ) );
	}

	/**
	 * returns true if node is a ASTConstant with value Double(-1) or Complex(-1,0) 
	 */
	public boolean isMinusOne(Node node)
	{
		return ( isReal(node)
				 && ( ((ASTConstant) node).getValue().equals(MINUSONE)) )
			 ||( isComplex(node)
				 && ( ((Complex) ((ASTConstant) node).getValue()).equals(CMINUSONE,0.0) ) );
	}
	/** 
	 * returns true if node is a ASTConstant with a Infinite component
	 * TODO do proper treatment of signed infinity 
	 */

	public boolean isInfinity(Node node)
	{
		if(isReal(node))
		{
			Double dub = (Double) ((ASTConstant) node).getValue();
			return dub.isInfinite();
		}
		if(isComplex(node))
		{
			Complex z = (Complex) ((ASTConstant) node).getValue();
			return Double.isInfinite(z.re()) 
				|| Double.isInfinite(z.im());
		}
		return false;
	}

	/**
	 * returns true if node is a ASTConstant with a NaN component 
	 */
	public boolean isNaN(Node node)
	{
		if(isReal(node))
		{
			Double dub = (Double) ((ASTConstant) node).getValue();
			return dub.isNaN();
		}
		if(isComplex(node))
		{
			Complex z = (Complex) ((ASTConstant) node).getValue();
			return Double.isNaN(z.re()) 
				|| Double.isNaN(z.im());
		}
		return false;
	}

	/**
	 * returns true if node is an ASTConstant with a negative Double value 
	 */
	public boolean isNegative(Node node)
	{
			return isReal(node)
					 && ( ((Double) ((ASTConstant) node).getValue()).doubleValue() < 0.0 );
	}

	/**
	 * returns true if node is an ASTConstant with a positive Double value 
	 */
	public boolean isPositive(Node node)
	{
			return isReal(node)
					 && ( ((Double) ((ASTConstant) node).getValue()).doubleValue() > 0.0 );
	}

	/**
	 * returns true if node is an ASTConstant of type Complex
	 */
	 public boolean isComplex(Node node)
	 {
			return isConstant(node)
				 && ( ((ASTConstant) node).getValue() instanceof Complex );
	 }

	/**
	 * returns true if node is an ASTVarNode
	 */
	public boolean isVariable(Node node)
	{
	   return (node instanceof ASTVarNode);
	}
	
	/**
	 * returns true if node is an ASTOpNode
	 */
	public boolean isOperator(Node node)
	{
	   return (node instanceof ASTFunNode) && ((ASTFunNode) node).isOperator();
	}

	public boolean isBinaryOperator(Node node)
	{
	   if(isOperator(node))
	   {
	   		return ((XOperator) ((ASTFunNode) node).getOperator()).isBinary();
	   }
	   return false;
	}

	public boolean isUnaryOperator(Node node)
	{
	   if(isOperator(node))
	   {
			return ((XOperator) ((ASTFunNode) node).getOperator()).isUnary();
	   }
	   return false;
	}

	/**
	 * returns the operator for a node or null if it is not an operator node.
	 */
	public Operator getOperator(Node node)
	{
	   if(isOperator(node))
	   	return ((ASTFunNode) node).getOperator();
	   return null;
	}

	/**
	 * returns true if node is an ASTFunNode
	 */
	public boolean isFunction(Node node)
	{
	   return (node instanceof ASTFunNode);
	}
	
	/**
	 * Sets the children of a node if they have changed for it current children.
	 */
	public static Node copyChildrenIfNeeded(Node node,Node children[]) throws ParseException
	{
		int n=node.jjtGetNumChildren();
		if(n!=children.length)
			throw new ParseException("copyChildrenIfNeeded: umber of children of node not the same as supplied children");
		for(int i=0;i<n;++i)
			if(node.jjtGetChild(i) != children[i])
			{
				node.jjtAddChild(children[i],i);
				children[i].jjtSetParent(node);
			}
		return node;
	}

	/** returns the children of a node as an array of nodes. */
	static public Node[] getChildrenAsArray(Node node)
	{
		int n = node.jjtGetNumChildren();
		Node[] children = new Node[n];
		for(int i=0;i<n;++i)
			children[i]=node.jjtGetChild(i);
		return children;
	}

	public Object getCI() {	return CI;	}
	public Object getCMINUSI() {return CMINUSI;	}
	public Object getCMINUSONE() {return CMINUSONE;	}
	public Object getCONE() {return CONE;	}
	public Object getCZERO() {return CZERO;	}
	public Object getMINUSONE() {return MINUSONE;	}
	public Object getONE() {return ONE;	}
	public Object getZERO() {return ZERO;	}
	public Object getNAN() { return NAN; }
	public Object getPositiveInfinity() { return PosInf; }
	public Object getNegativeInfinity() { return NegInf; }
	public Object getNumber(double val) { return new Double(val); }
}
