/* @author rich
 * Created on 14-Dec-2004
 */
package org.lsmp.djep.sjep;

import org.lsmp.djep.xjep.*;
import org.nfunk.jep.*;
import org.nfunk.jep.type.*;
import org.nfunk.jep.function.*;
/**
 * Main entry point for simplification routines.
 *
 *<p>
Uses a complete reworking of the ways equations are represented.
A tree structure is built from Polynomials, Monomials, PVariable etc.
An equation like 
<pre>1+2 x^2+3 x y+4 x sin(y)</pre>
is represented as
<pre>
Polynomial([
  Monomial(2.0,[PVariable(x)],[2])]),
  Monomial(3.0,[x,y],[1,1]),
  Monomial(4.0,[x,Function(sin,arg)],[1,1])
])
</pre>
</p>

<p>
A total ordering of all expressions is used. 
As the representation is constructed the total ordering of terms is maintained. 
This helps ensure that polynomials are always in their simplest form
and also allows comparison of equations.
</p>
<p>   
The following sequence illustrates current ordering. 
This ordering may change without warning.
<ul>
<li>-1 numbers sorted by values
<li>0
<li>1 numbers before monomials
<li>a^-2 powers in increasing order
<li>a^-1
<li>a
<li>a^2
<li>a^3
<li>a^x numeric powers before symbolic powers
<li>a b single variable monomials before multiple variables
<li>a^2 b
<li>b variables sorted alphabetically
<li>cos(a) monomials before functions
<li>sin(a) function names sorted alphabetically
<li>a+b functions before polynomials
<li>a+b+c
<li>
</ul>
 * @author Rich Morris
 * Created on 14-Dec-2004
 */
public class PolynomialCreator extends DoNothingVisitor {
	private XJep jep;
	Object zero,one,minusOne,infinity,nan,two;
	PConstant zeroConstant,oneConstant,minusOneConstant,infConstant,nanConstant,twoConstant;
	Monomial zeroMonomial,unitMonomial,infMonomial,nanMonomial;
	Polynomial zeroPolynomial,unitPolynomial,infPolynomial,nanPolynomial;
	NumberFactory numf;
	OperatorSet os;
	NodeFactory nf;
	//boolean expand=false;
	private PolynomialCreator() {}
	public PolynomialCreator(XJep j)
	{
		jep = j;
		numf = j.getNumberFactory();
		os = j.getOperatorSet();
		nf = j.getNodeFactory();
		
		zero = j.getNumberFactory().getZero();
		one = j.getNumberFactory().getOne();
		minusOne = j.getNumberFactory().getMinusOne();
		two = j.getNumberFactory().getTwo();
		try {
			infinity = div(one,zero);
			nan = div(zero,zero);
		} catch(ParseException e) {
			infinity = new Double(Double.POSITIVE_INFINITY);
			nan = new Double(Double.NaN);
		}
			
		zeroConstant = new PConstant(this,zero);
		oneConstant = new PConstant(this,one);
		twoConstant = new PConstant(this,two);
		minusOneConstant = new PConstant(this,minusOne);
		infConstant = new PConstant(this,infinity);
		nanConstant = new PConstant(this,nan);
	}

	/**
	 * Converts an expression into the polynomial representation. 
	 * @param node top node of expression
	 * @return top node of polynomial form of expression
	 * @throws ParseException if expression cannot be converted.
	 */
	public PNodeI createPoly(Node node) throws ParseException
	{
		return (PNodeI) node.jjtAccept(this,null);
	}

	/**
	 * Simplifies an expression.
	 * 
	 * @param node top node to expression to be simplified.
	 * @return a simplified expression
	 * @throws ParseException
	 */
	public Node simplify(Node node) throws ParseException
	{
		PNodeI poly = createPoly(node);
		return poly.toNode();
	}
	/**
	 * Expands an expression.
	 * Will always expand brackets for multiplication and simple powers.  
	 * For instance
	 * <code>(1+x)^3 -> 1+3x+3x^2+x^3</code>
	 * 
	 * @param node top node to expression to be simplified.
	 * @return a simplified expression
	 * @throws ParseException
	 */

	public Node expand(Node node) throws ParseException
	{
		PNodeI poly = createPoly(node);
		PNodeI expand = poly.expand();
		return expand.toNode();
	}

	/**
	 * Compares two nodes.
	 * Uses a total ordering of expressions.
	 * Expands equations before comparison.
	 * 
	 * @param node1
	 * @param node2
	 * @return -1 if node1<node2, 0 if node1==node2, +1 if node1>node2
	 * @throws ParseException
	 */
	public int compare(Node node1,Node node2) throws ParseException
	{
		PNodeI poly1 = createPoly(node1);
		PNodeI exp1 = poly1.expand();
		PNodeI poly2 = createPoly(node2);
		PNodeI exp2 = poly2.expand();
		return exp1.compareTo(exp2);
	}

	/**
	 * Compares two nodes.
	 * Uses a total ordering of expressions.
	 * May give some false negatives is simplification cannot reduce
	 * two equal expressions to the same canonical form.
	 * Expands equations before comparison.
	 * 
	 * @param node1
	 * @param node2
	 * @return true if two nodes represents same expression.
	 * @throws ParseException
	 */
	public boolean equals(Node node1,Node node2) throws ParseException
	{
		PNodeI poly1 = createPoly(node1);
		PNodeI exp1 = poly1.expand();
		PNodeI poly2 = createPoly(node2);
		PNodeI exp2 = poly2.expand();
		return exp1.equals(exp2);
	}

	public Object visit(ASTConstant node, Object data) throws ParseException {

		return new PConstant(this,node.getValue());
	}

	public Object visit(ASTVarNode node, Object data) throws ParseException {
		return new PVariable(this,(XVariable) node.getVar());
	}

	public Object visit(ASTFunNode node, Object data) throws ParseException {
		int nChild = node.jjtGetNumChildren();
		PNodeI args[] = new PNodeI[nChild];
		for(int i=0;i<nChild;++i) {
			args[i] = (PNodeI) node.jjtGetChild(i).jjtAccept(this,data);
		}

/*		jep.println(node);
		for(int i=0;i<nChild;++i)
			System.out.println("\t"+args[i].toString());
*/		
		XOperator op = (XOperator) node.getOperator();
		if(op == os.getAdd())
		{
			PNodeI res = args[0];
			for(int i=1;i<nChild;++i)
				res = res.add(args[i]);
			return res;
		}
		else if(op == os.getSubtract())
		{
			if(args.length!=2) throw new ParseException("Subtract must have two args it has "+args.length);
			return args[0].sub(args[1]);
		}
		else if(op == os.getUMinus())
		{
			PNodeI res = args[0];
			return res.negate();
		}
		else if(op == os.getMultiply())
		{
			PNodeI res = args[0];
			for(int i=1;i<nChild;++i)
				res = res.mul(args[i]);
			return res;
		}
		else if(op == os.getDivide())
		{
			if(args.length!=2) throw new ParseException("Divide must have two args it has "+args.length);
			return args[0].div(args[1]);
		}
		else if(op == os.getPower())
		{
			if(args.length!=2) throw new ParseException("Power must have two args it has "+args.length);
			return args[0].pow(args[1]);
		}
		
		boolean allConst = true;
		for(int i=0;i<args.length;++i)
			if(!(args[i] instanceof PConstant)) { allConst = false; break; }

		if(allConst)
		{
			Node newNodes[] = new Node[args.length];
			for(int i=0;i<args.length;++i)
				newNodes[i] = args[i].toNode();
			Node topNode;
			if(op != null)
				topNode = nf.buildOperatorNode(op,newNodes);
			else
				topNode = nf.buildFunctionNode(node.getName(),node.getPFMC(),newNodes);
			
			Object val;
			try	{
				val = jep.evaluate(topNode);
			} catch(Exception e) {
				throw new ParseException(e.getMessage());
			}
			return new PConstant(this,val);
		}
		
		if(op != null)
			return new POperator(this,op,args);
		return new PFunction(this,node.getName(),node.getPFMC(),args);
		
		//throw new ParseException("Polynomial: Sorry don't know how to convert "+node.getName());
	}

	Object add(Object a,Object b) throws ParseException {
		return ((Add) os.getAdd().getPFMC()).add(a,b);
	}
	
	Object sub(Object a,Object b) throws ParseException {
		return ((Subtract) os.getSubtract().getPFMC()).sub(a,b);
	}


	Object mul(Object a,Object b) throws ParseException {
		return ((Multiply) os.getMultiply().getPFMC()).mul(a,b);
	}
	
	Object div(Object a,Object b) throws ParseException {
		return ((Divide) os.getDivide().getPFMC()).div(a,b);
	}

	Object intToValue(int i)
	{
		return new Double(i);	
	}
	Object raise(Object a,Object b) throws ParseException 
	{
		return 
		((Power) os.getPower().getPFMC()).power(a,b);
	}

	Object neg(Object val) throws ParseException {	
		return ((UMinus) os.getUMinus().getPFMC()).umin(val);
	}

	int cmp(Object a,Object b) throws ParseException {
		if(a.equals(b)) return 0;
		
		if(a instanceof Complex)
		{
			Complex ca = (Complex) a;
			double ax = ca.re(), ay = ca.im();
			if(b instanceof Complex)
			{
				Complex cb = (Complex) b;
				double bx = cb.re(), by = cb.im();
				if(ax == bx)
				{
					if(ay==by) return 0;
					else if(ay<by) return -1;
					else return 0;
				}
				else if(ax < bx) return -1;
				else return 1;
			}
			else if(b instanceof Number)
			{
				double bx = ((Number) b).doubleValue(), by = 0.0;
				if(ax == bx)
				{
					if(ay==by) return 0;
					else if(ay<by) return -1;
					else return 0;
				}
				else if(ax < bx) return -1;
				else return 1;
			}
			throw new ParseException("Don't know how to compare a Complex with "+b+" ("+b.getClass().getName()+")");
		}
		else if(a instanceof Number)
		{
			if(b instanceof Complex)
			{
				double ax = ((Number) a).doubleValue(), ay = 0.0;
				Complex cb = (Complex) b;
				double bx = cb.re(), by = cb.im();
				if(ax == bx)
				{
					if(ay==by) return 0;
					else if(ay<by) return -1;
					else return 0;
				}
				else if(ax < bx) return -1;
				else return 1;
			}
		}
		if(a instanceof Comparable && a.getClass().equals(b.getClass()))
			return ((Comparable) a).compareTo(b);
			
		if((a instanceof Number) && (b instanceof Number))
		{
			double ax = ((Number) a).doubleValue();
			double bx = ((Number) b).doubleValue();

			if(ax == bx) return 0;
			else if(ax < bx) return -1;
			else return 1;
		}
		
		if(a instanceof Comparable)
			return ((Comparable) a).compareTo(b);
		
		throw new IllegalArgumentException("Sorry don't know how to compare "+a+" ("+a.getClass().getName()+") and "+b+" ("+b.getClass().getName()+")");	
	}
}
