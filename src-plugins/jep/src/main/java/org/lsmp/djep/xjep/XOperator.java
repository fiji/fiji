/* @author rich
 * Created on 03-Aug-2003
 */
package org.lsmp.djep.xjep;

import org.nfunk.jep.function.PostfixMathCommandI;
import org.nfunk.jep.*;

/**
 * An Operator with additional information about its commutativity etc.
 * <p>
 * Operators have a number of properties:
 * <ul>
 * <li>A symbol or name of the operator "+".
 * <li>The number of arguments NO_ARGS 0, UNARY 1 (eg UMINUS -x), 
 * BINARY 2 (eq x+y), and NARY either 3 ( a>b ? a : b) or
 * unspecified like a list [x,y,z,w].
 * <li>The binging of the operator, LEFT 1+2+3 -> (1+2)+3 or RIGHT 1=2=3 -> 1=(2=3).
 * <li>Whether the operator is ASSOCIATIVE or COMMUTATIVE.
 * <li>The precedence of the operators + has a higher precedence than *.
 * <li>For unary opperators they can either be PREFIX like -x or SUFIX like x%.
 * <li>Comparative operators can be REFLEXIVE, SYMMETRIC, TRANSITIVE or EQUIVILENCE which has all three properties.
 * <li>A reference to a PostfixtMathCommandI object which is used to evaluate an equation containing the operator.
 * </ul>
 * various is... and get... methods are provided to query the properties of the opperator.
 * 
 * @author Rich Morris
 * Created on 19-Oct-2003
 */
public class XOperator extends Operator {
	/** No arguments to operator */
	public static final int NO_ARGS=0;
	/** Unary operators, such as -x !x ~x */
	public static final int UNARY=1;
	/** Binary operators, such as x+y, x>y */
	public static final int BINARY=2;
	/** Trinary ops such as ?: and or higher like [x,y,z,w] */
	public static final int NARY=3;
	/** Left binding like +: 1+2+3 -> (1+2)+3 */
	public static final int LEFT=4;
	/** Right binding like =: 1=2=3 -> 1=(2=3) */
	public static final int RIGHT=8;
	/** Associative operators x*(y*z) == (x*y)*z . */
	public static final int ASSOCIATIVE=16;
	/** Commutative operators x*y = y*x. */
	public static final int COMMUTATIVE=32;
	/** Reflecive relations x=x for all x. */
	public static final int REFLEXIVE=64;
	/** Symmetric relation x=y implies y=x. */
	public static final int SYMMETRIC=128;
	/** Transative relations x=y and y=z implies x=z */
	public static final int TRANSITIVE=256;
	/** Equivilence relations = reflexive, transative and symetric. */
	public static final int EQUIVILENCE=TRANSITIVE+REFLEXIVE+SYMMETRIC;
	/** prefix operators -x **/
	public static final int PREFIX=512;
	/** postfix operators  x%, if neiter prefix and postif then infix, if both trifix like x?y:z **/
	public static final int SUFIX=1024;
	/** self inverse operators like -(-x) !(!x) **/
	public static final int SELF_INVERSE=2048;
	/** composite operators, like a-b which is a+(-b) **/
	public static final int COMPOSITE=4096;
	/** For non commutative operators printing can be determined by the left or right binding. 
	 *  For example (a-b)-c is printed as a-b-c. 
	 *  But a/b/c could be ambiguous so (a/b)/c is printed with brackets.
	 */	
	public static final int USE_BINDING_FOR_PRINT=8192;
	/** flags for type of operator */
	private int flags;

	/** construct a new operator.
	 * 
	 * @param name	printable name of operator
	 * @param pfmc  postfix math command for opperator
	 * @param flags set of flags defining the porperties of the operator.
	 */
	public XOperator(String name,PostfixMathCommandI pfmc,int flags)
	{
		super(name,pfmc);
		this.flags = flags; 
	}
	/**
	 * Allows a given precedent to be set.
	 * @param name
	 * @param pfmc
	 * @param flags
	 * @param precedence
	 */
	public XOperator(String name,PostfixMathCommandI pfmc,int flags,int precedence)
	{
		this(name,pfmc,flags);
		this.precedence=precedence;
	}
	/** construct a new operator, with a different name and symbol
	 * 
	 * @param name	name of operator, must be unique, used when describing operator
	 * @param symbol printable name of operator, used for printing equations
	 * @param pfmc  postfix math command for opperator
	 * @param flags set of flags defining the porperties of the operator.
	 */
	public XOperator(String name,String symbol,PostfixMathCommandI pfmc,int flags)
	{
		super(name,symbol,pfmc);
		this.flags = flags; 
	}
	/**
	 * Allows a given precedent to be set.
	 * @param name
	 * @param pfmc
	 * @param flags
	 * @param precedence
	 */
	public XOperator(String name,String symbol,PostfixMathCommandI pfmc,int flags,int precedence)
	{
		super(name,symbol,pfmc);
		this.precedence=precedence;
		this.flags=flags;
	}

	public XOperator(Operator op,int flags,int precedence)
	{
		this(op.getName(),op.getSymbol(),op.getPFMC(),flags,precedence);
	}

	public XOperator(Operator op,int flags)
	{
		this(op.getName(),op.getSymbol(),op.getPFMC(),flags);
	}
	/** Creates a new XOperators with same flags and precedance as argument. */
//	public XOperator(XOperator op,PostfixMathCommandI pfmc)
//	{
//		this(op.getName(),op.getSymbol(),op.getPFMC(),op.flags,op.precedence);
//	}

	/** precedence of operator, 0 is most tightly bound, so prec("*") < prec("+"). */
	private int precedence = -1;
	public final int getPrecedence() {return precedence;}
	protected final void setPrecedence(int i) {precedence = i;}

	/** Operators this is distributative over **/
	private Operator distribOver[] = new Operator[0];

	protected final void setDistributiveOver(Operator op)
	{
		int len = distribOver.length;
		Operator temp[] = new Operator[len+1];
		for(int i=0;i<len;++i)	temp[i] = distribOver[i];
		temp[len]=op;
		distribOver=temp; 
	}
	public boolean isDistributiveOver(Operator op)
	{
		for(int i=0;i<distribOver.length;++i)
			if(op == distribOver[i])
				return true;
		return false;	
	}
	
	/** For composite operators like a-b which is really a+(-b) there is a root operator and an inverse operator **/
	private Operator rootOperator=null;
	private Operator inverseOperator=null;
	private Operator binaryInverseOperator=null;
	protected void setRootOp(Operator root)	{rootOperator=root;}
	protected void setInverseOp(Operator inv){inverseOperator = inv;}
	protected void setBinaryInverseOp(Operator inv){binaryInverseOperator = inv;}
	public Operator getRootOp() { return rootOperator; }
	public Operator getInverseOp() { return inverseOperator; }
	public Operator getBinaryInverseOp() { return binaryInverseOperator; }

	/** 
	 * When parsing how is x+y+z interpreted.
	 * Can be Operator.LEFT x+y+z -> (x+y)+z or
	 * Operator.RIGHT x=y=z -> x=(y=z). 
	 */  
	public final int getBinding() { return (flags & (LEFT | RIGHT)); }
	public final boolean isAssociative() {return ((flags & ASSOCIATIVE) == ASSOCIATIVE);}
	public final boolean isCommutative() { return ((flags & COMMUTATIVE) == COMMUTATIVE);}
	public final boolean isBinary() {	return ((flags & 3) == BINARY);	}
	public final boolean isUnary() {	return ((flags & 3) == UNARY);	}
	public final boolean isNary() {	return ((flags & 3) == NARY);	}
	public final int numArgs() { return (flags & 3);	}
	public final boolean isTransitive() {	return ((flags & TRANSITIVE) == TRANSITIVE);	}
	public final boolean isSymmetric() {	return ((flags & SYMMETRIC) == SYMMETRIC);	}
	public final boolean isReflexive() {	return ((flags & REFLEXIVE) == REFLEXIVE);	}
	public final boolean isEquivilence() {return ((flags & EQUIVILENCE) == EQUIVILENCE);	}
	public final boolean isPrefix() {return ((flags & PREFIX) == PREFIX);	}
	public final boolean isSufix() {return ((flags & SUFIX) == SUFIX);	}
	public final boolean isComposite() {return ((flags & COMPOSITE) == COMPOSITE);	}
	public final boolean isSelfInverse() {return ((flags & SELF_INVERSE) == SELF_INVERSE);	}
	public final boolean useBindingForPrint() {return ((flags & USE_BINDING_FOR_PRINT) == USE_BINDING_FOR_PRINT);	}

	/** returns a verbose representation of the operator and all its properties. **/
	
	public String toFullString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Operator: \""+getSymbol()+"\"");
		if(!getName().equals(getSymbol())) sb.append(" "+getName());
		switch(numArgs()){
		case 0: sb.append(" no arguments,"); break;
		case 1: sb.append(" unary,"); break;
		case 2: sb.append(" binary,"); break;
		case 3: sb.append(" variable number of arguments,"); break;
		}
		if(isPrefix() && isSufix()) sb.append(" trifix,");
		else if(isPrefix()) sb.append(" prefix,");
		else if(isSufix()) sb.append(" sufix,");
		else sb.append(" infix,");
		if(getBinding()==LEFT) sb.append(" left binding,");
		else if(getBinding()==RIGHT) sb.append(" right binding,");
		if(isAssociative()) sb.append(" associative,");
		if(isCommutative()) sb.append(" commutative,");
		sb.append(" precedence "+getPrecedence()+",");
		if(isEquivilence())
			sb.append(" equivilence relation,");
		else
		{
			if(isReflexive()) sb.append(" reflexive,");
			if(isSymmetric()) sb.append(" symmetric,");
			if(isTransitive()) sb.append(" transitive,");
		}
		sb.setCharAt(sb.length()-1,'.');
		return sb.toString();
	}
}
