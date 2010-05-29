/* @author rich
 * Created on 18-Jun-2003
 */

package org.lsmp.djep.xjep;
import org.nfunk.jep.*;
import org.nfunk.jep.type.*;
import java.io.PrintStream;
import java.util.Hashtable;
import java.text.NumberFormat;
import java.text.FieldPosition;
/**
 * Prints an expression.
 * Prints the expression with lots of brackets.
 * <tt>((-1.0)/sqrt((1.0-(x^2.0))))</tt>.
 * To use
 * <pre>
 * XJep j = ...; Node in = ...;
 * j.print(in,"x");
 * </pre>
 * @author Rich Morris
 * Created on 20-Jun-2003
 * @since Dec 04 and NumberFormat object can be supplied to modify printing of numbers.
 * @since 21 Dec 04 PrintVisitor can now cope with 3 or more arguments to + and *. 
 * @see XJep#print(Node)
 * @see XJep#print(Node, PrintStream)
 * @see XJep#println(Node)
 * @see XJep#println(Node, PrintStream)
 * @see XJep#toString(Node)
 */
public class PrintVisitor extends ErrorCatchingVisitor
{
  /** All brackets are printed. Removes all ambiguity. */
  public static final int FULL_BRACKET = 1;
  /** Print Complex as 3+2 i */
  public static final int COMPLEX_I = 2;
  private int maxLen = -1;
  protected StringBuffer sb;
  /** The current mode for printing. */
//  protected boolean fullBrackets=false;
  protected int mode=0;
  private Hashtable specialRules = new Hashtable();
  
  /** Creates a visitor to create and print string representations of an expression tree. **/

  public PrintVisitor()
  {
  }

  
  /** Prints the tree descending from node with lots of brackets 
   * or specified stream. 
   * @see XJep#println(Node, PrintStream)
   **/

  public void print(Node node,PrintStream out)
  {
	sb = new StringBuffer();
	acceptCatchingErrors(node,null);
	if(maxLen == -1)
		out.print(sb);
	else
	{
		while(true)	{
			if(sb.length() < maxLen) {
				out.print(sb);
				return;
			}
			int pos = maxLen-2;
			for(int i=maxLen-2;i>=0;--i) {
				char c = sb.charAt(i);
				if(c == '+' || c == '-' || c == '*' || c == '/'){
					pos = i; break;
				}
			}
			//out.println("<"+sb.substring(0,pos+10)+">");
			out.println(sb.substring(0,pos+1));
			sb.delete(0,pos+1);
		}
	}
  }

  /** Prints on System.out. */
  public void print(Node node) { print(node,System.out); }
    
  /** Prints the tree descending from node with a newline at end. **/

  public void println(Node node,PrintStream out)
  {
	print(node,out);
	out.println("");
  }

  /** Prints on System.out. */
  public void println(Node node) { println(node,System.out); }

  /** returns a String representation of the equation. */
  
  public String toString(Node node)
  {
	sb = new StringBuffer();
	acceptCatchingErrors(node,null);
	return sb.toString();
  }
  
	/**
	 * This interface specifies the method needed to implement a special print rule.
	 * A special rule must implement the append method, which should
	 * call pv.append to add data to the output. For example
	 * <pre>
	 * 	pv.addSpecialRule(Operator.OP_LIST,new PrintVisitor.PrintRulesI()
	 *	{
	 *  	public void append(Node node,PrintVisitor pv) throws ParseException
	 *		{
	 *			pv.append("[");
	 *			for(int i=0;i<node.jjtGetNumChildren();++i)
	 *			{
	 *				if(i>0) pv.append(",");
	 *				node.jjtGetChild(i).jjtAccept(pv, null);
	 *			}
	 *			pv.append("]");
	 *		}});
 	 * </pre>
	 * @author Rich Morris
	 * Created on 21-Feb-2004
	 */
  public interface PrintRulesI
  {
  	/** The method called to append data for the rule. **/
  	public void append(Node node,PrintVisitor pv) throws ParseException;
  }

  /** Add a string to buffer. Classes implementing PrintRulesI 
   * should call this add the */
  public void append(String s) { sb.append(s); }

  /** Adds a special print rule to be added for a given operator. 
   * TODO Allow special rules for other functions, i.e. not operators. */
  public void addSpecialRule(Operator op,PrintRulesI rules)
  {
  	specialRules.put(op,rules);
  }

/***************** visitor methods ********************************/

	/** print the node with no brackets. */
	private void printNoBrackets(Node node) throws ParseException
	{
		node.jjtAccept(this,null);
	}
	
	/** print a node surrounded by brackets. */
	private void printBrackets(Node node) throws ParseException
	{
		sb.append("(");
		printNoBrackets(node);
		sb.append(")");
	}
	
	/** print a unary operator. */
	private Object visitUnary(ASTFunNode node, Object data) throws ParseException
	{
		Node rhs = node.jjtGetChild(0);
	
		// now print the node
		sb.append(node.getOperator().getSymbol());
		// now the rhs
		if(rhs instanceof ASTFunNode && ((ASTFunNode) rhs).isOperator())
			printBrackets(rhs);	// -(-3) -(1+2) or !(-3)
		else
			printNoBrackets(rhs);
		
		return data;
	}
	
	private boolean testLeft(XOperator top,Node lhs)
	{
		if((mode & FULL_BRACKET)!= 0)
		{
			return true;
		}
		else if(lhs instanceof ASTFunNode && ((ASTFunNode) lhs).isOperator())
		{
			XOperator lhsop = (XOperator) ((ASTFunNode) lhs).getOperator();
			if(top == lhsop)
			{
				if(top.getBinding() == XOperator.LEFT	// (1-2)-3 -> 1-2-3
					&& top.isAssociative() )
						return false;
				else if(top.useBindingForPrint())
						return false;
				else
						return true;				// (1=2)=3 -> (1=2)=3
			}
			else if(top.getPrecedence() == lhsop.getPrecedence())
			{
				if(lhsop.getBinding() == XOperator.LEFT && lhsop.isAssociative())
						return false;
				else if(lhsop.useBindingForPrint())
						return false;
				else	return true;
			} 				// (1=2)=3 -> (1=2)=3
				
			else if(top.getPrecedence() > lhsop.getPrecedence()) // (1*2)+3
						return false;
			else
						return true;
		}
		else
			return false;
	
	}
	
	private boolean testMid(XOperator top,Node rhs)
	{
		if((mode & FULL_BRACKET)!= 0)
		{
			return true;
		}
		else if(rhs instanceof ASTFunNode && ((ASTFunNode) rhs).isOperator())
		{
			XOperator rhsop = (XOperator) ((ASTFunNode) rhs).getOperator();
			if(top == rhsop)
			{
				return false;
			}
			else if(top.getPrecedence() == rhsop.getPrecedence())
			{
				return false;	// a+(b-c) -> a+b-c
			}
			else if(top.getPrecedence() > rhsop.getPrecedence()) // 1+(2*3) -> 1+2*3
						return false;
			else
						return true;
		}
		else
			return false;
	}
	
	private boolean testRight(XOperator top,Node rhs)
	{
		if((mode & FULL_BRACKET)!= 0)
		{
			return true;
		}
		else if(rhs instanceof ASTFunNode && ((ASTFunNode) rhs).isOperator())
		{
			XOperator rhsop = (XOperator) ((ASTFunNode) rhs).getOperator();
			if(top == rhsop)
			{
				if(top.getBinding() == XOperator.RIGHT	// 1=(2=3) -> 1=2=3
					|| top.isAssociative() )			// 1+(2-3) -> 1+2-3
						return false;
				return true;				// 1-(2+3) -> 1-(2-3)
			}
			else if(top.getPrecedence() == rhsop.getPrecedence())
			{
				if(top.getBinding() == XOperator.LEFT && top.isAssociative() )			// 1+(2-3) -> 1+2-3)
					return false;	// a+(b-c) -> a+b-c
				return true;		// a-(b+c) -> a-(b+c)
			}
			else if(top.getPrecedence() > rhsop.getPrecedence()) // 1+(2*3) -> 1+2*3
						return false;
			else
						return true;
		}
		else
			return false;
	}
	
	private Object visitNaryBinary(ASTFunNode node,XOperator op) throws ParseException
	{
		int n = node.jjtGetNumChildren();
		for(int i=0;i<n;++i)
		{
			if(i>0) sb.append(op.getSymbol());
			
			Node arg = node.jjtGetChild(i);
			if(testMid(op,arg))
				printBrackets(arg);
			else
				printNoBrackets(arg);
		}
		return null;
	}
	public Object visit(ASTFunNode node, Object data) throws ParseException
	{
		if(!node.isOperator()) return visitFun(node);
		if(node instanceof PrintRulesI)
		{
			((PrintRulesI) node).append(node,this);
			return null;
		}
		if(node.getOperator()==null)
		{
			throw new ParseException("Null operator in print for "+node);
		}
		if(specialRules.containsKey(node.getOperator()))
		{
			((PrintRulesI) specialRules.get(node.getOperator())).append(node,this);
			return null;
		}
		if(node.getPFMC() instanceof org.nfunk.jep.function.List)
		{	
			append("[");
				for(int i=0;i<node.jjtGetNumChildren();++i)
				{
					if(i>0) append(",");
					node.jjtGetChild(i).jjtAccept(this, null);
				}
				append("]");
			return null;
		}
			
		if(((XOperator) node.getOperator()).isUnary())
			return visitUnary(node,data);
	
		if(((XOperator) node.getOperator()).isBinary())
		{
			XOperator top = (XOperator) node.getOperator();
			if(node.jjtGetNumChildren()!=2)
				return visitNaryBinary(node,top);
			Node lhs = node.jjtGetChild(0);
			Node rhs = node.jjtGetChild(1);
		
			if(testLeft(top,lhs))
				printBrackets(lhs);
			else
				printNoBrackets(lhs);
			
			// now print the node
			sb.append(node.getOperator().getSymbol());
			// now the rhs
	
			if(testRight(top,rhs))
				printBrackets(rhs);
			else
				printNoBrackets(rhs);
	
		}
		return null;
	}

	/** prints a standard function: fun(arg,arg) */
	private Object visitFun(ASTFunNode node) throws ParseException
	{
		sb.append(node.getName()+"(");
		for(int i=0;i<node.jjtGetNumChildren();++i)
		{
			if(i>0) sb.append(",");
			node.jjtGetChild(i).jjtAccept(this, null);
		}
		sb.append(")");
	
		return null;
	}

	public Object visit(ASTVarNode node, Object data) throws ParseException  {
		sb.append(node.getName());
		return data;
	}

	public Object visit(ASTConstant node, Object data) {
		Object val = node.getValue();
		formatValue(val,sb);
		return data;
	}

	private FieldPosition fp = new FieldPosition(NumberFormat.FRACTION_FIELD);

	/** Appends a formatted versions of val to the string buffer.
	 * 
	 * @param val The value to format
	 * @param sb1  The StingBuffer to append to
	 */
	public void formatValue(Object val,StringBuffer sb1)
	{
		if(format != null)
		{
			if(val instanceof Number)
				format.format(val,sb1,fp);
			else if(val instanceof Complex)
			{
				if((mode | COMPLEX_I) == COMPLEX_I)
					sb1.append(((Complex) val).toString(format,true));
				else
					sb1.append(((Complex) val).toString(format));
			}
			else
				sb1.append(val);
		}
		else
			sb1.append(val);
	}
	
	/** Returns a formated version of the value. */
	public String formatValue(Object val)
	{
	  	StringBuffer sb2 = new StringBuffer();
	  	formatValue(val,sb2);
	  	return sb2.toString();
	}
	/**
	 * Return the current print mode.
	 */
	public int getMode() {
		return mode;
	}
	public boolean getMode(int testmode) {
		return( (this.mode | testmode ) == testmode); 
	}
	/**
	 * Set printing mode.
	 * In full bracket mode the brackets each element in the tree will be surrounded
	 * by brackets to indicate the tree structure. 
	 * In the default mode, (full bracket off) the number of brackets is
	 * minimised so (x+y)+z will be printed as x+y+z.
	 * @param mode which flags to change, typically FULL_BRACKET
	 * @param flag whether to switch this mode on or off
	 */
	public void setMode(int mode,boolean flag) {
		if(flag)
			this.mode |= mode;
		else
			this.mode ^= mode;
	}
	/** The NumberFormat object used to print numbers. */
	protected NumberFormat format;
	public void setNumberFormat(NumberFormat format)
	{
		this.format = format;
	}
	
	/**
	 * Sets the maximum length printed per line.
	 * If the value is not -1 then the string will be broken into chunks
	 * each of which is less than the max length.
	 * @param i the maximum length
	 */
	public void setMaxLen(int i) {
		maxLen = i;
	}
	/**
	 * @return the maximum length printed per line
	 */
	public int getMaxLen() {
		return maxLen;
	}

}

/*end*/
