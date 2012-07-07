/* @author rich
 * Created on 16-Nov-2003
 */
package org.lsmp.djep.xjep;

import org.nfunk.jep.*;
import org.nfunk.jep.function.Exp;
import org.nfunk.jep.function.PostfixMathCommandI;
import org.lsmp.djep.xjep.PrintVisitor.PrintRulesI;
import org.lsmp.djep.xjep.function.*;
import java.util.*;
import java.io.PrintStream;
import java.io.Reader;
/**
 * An extended version of JEP adds various routines for working with trees.
 * Has a NodeFactory, and OperatorSet, TreeUtils
 * and Visitors DoNothingVisitor,
 * @author Rich Morris
 * Created on 16-Nov-2003
 */
public class XJep extends JEP {
	/** Creates new nodes */
	protected NodeFactory nf = null;
	/** A few utility functions. */
	protected TreeUtils tu = null;
	protected DeepCopyVisitor copier = null;
	protected SubstitutionVisitor subv = null;
	protected SimplificationVisitor simpv = null;
	protected CommandVisitor commandv = null;
	protected PrintVisitor pv = null;
	private VariableFactory vf = new XVariableFactory();

	/**
	 * Create a new XJep will all the function of JEP plus printing and other features.
	 */
	public XJep()
	{
		this.symTab = new XSymbolTable(vf); 

		/* Creates new nodes */
		nf = new NodeFactory(this);
		/* Collects operators **/
		opSet = new XOperatorSet();
		/* A few utility functions. */
		tu = new TreeUtils();
		
		copier = new DeepCopyVisitor();
		subv = new SubstitutionVisitor();
		ev = new XEvaluatorVisitor();
		simpv = new SimplificationVisitor();
		commandv = new CommandVisitor();
		pv = new PrintVisitor();
		pv.addSpecialRule(opSet.getElement(),new PrintRulesI() {
			public void append(Node node, PrintVisitor pv) throws ParseException {
				node.jjtGetChild(0).jjtAccept(pv, null);
				node.jjtGetChild(1).jjtAccept(pv, null);
			}});
	}

	/** Copy constructions, reuses all the components of argument. */
	protected XJep(XJep j)
	{
		super(j);
		this.commandv=j.commandv;
		this.copier=j.copier;
		this.ev=j.ev;
		this.nf=j.nf;
		this.opSet=j.opSet;
		this.pv=j.pv;
		this.simpv=j.simpv;
		this.subv=j.subv;
		this.tu=j.tu;
	}

	private JEP ingrediant = null;
	/** Conversion constructor.
	 * Turns a JEP object into an XJep object.
	 * @param j 
	 */
	public XJep(JEP j)
	{
		ingrediant=j;
		/* Creates new nodes */
		nf = new NodeFactory(this);
		this.symTab = new XSymbolTable(vf); 
		this.funTab = j.getFunctionTable();
		/* Collects operators **/
		opSet = new XOperatorSet(j.getOperatorSet());
		/* A few utility functions. */
		tu = new TreeUtils();
		copier = new DeepCopyVisitor();
		subv = new SubstitutionVisitor();
		ev = new XEvaluatorVisitor();
		simpv = new SimplificationVisitor();
		commandv = new CommandVisitor();
		pv = new PrintVisitor();
	}
	/**
	 * Creates a new instance of XJep with the same components as this one.
	 * Sub classes should overwrite this method to create objects of the correct type.
	 */
	public XJep newInstance()
	{
		XJep newJep = new XJep(this);
		return newJep;
	}
	/**
	 * Creates a new instance of XJep with the same components as this one and the specified symbol table.
	 * Sub classes should overwrite this method to create objects of the correct type.
	 */
	public XJep newInstance(SymbolTable st)
	{
		XJep newJep = new XJep(this);
		newJep.symTab = st;
		return newJep;
	}

	public void addStandardFunctions()
	{
		if(ingrediant!=null)
		{
			ingrediant.addStandardFunctions();
		} 
		else super.addStandardFunctions();
		addFunction("eval",new Eval());
		addFunction("Sum",new Sum(this));
		addFunction("Product",new Product());
		addFunction("Min",new Min());
		addFunction("Max",new Max());
		addFunction("MinArg",new MinArg());
		addFunction("MaxArg",new MaxArg());
		addFunction("Simpson",new Simpson());
		addFunction("Trapezium",new Trapezium());
		addFunction("toBase",new ToBase());
		addFunction("toHex",new ToBase(16,"0x"));
		addFunction("fromBase",new FromBase());
		addFunction("fromHex",new FromBase(16,"0x"));
		
		addFunction("exp",new Exp());
		addFunction("Define",new Define(this));
		try {
		MacroFunction sec = new MacroFunction("sec",1,"1/cos(x)",this);
		addFunction("sec",sec);
		MacroFunction cosec = new MacroFunction("cosec",1,"1/sin(x)",this);
		addFunction("cosec",cosec);
		MacroFunction cot = new MacroFunction("cot",1,"1/tan(x)",this);
		addFunction("cot",cot);
		} catch (ParseException e) {System.err.println(e.getMessage());}
	}

	public void addStandardConstants()
	{
		if(ingrediant!=null)
		{
			ingrediant.addStandardConstants();
			for(Enumeration en=ingrediant.getSymbolTable().elements();en.hasMoreElements();)
			{
				Variable var = (Variable) en.nextElement();
				if(var.isConstant())
					this.symTab.addConstant(var.getName(),var.getValue());
				//else
				//	this.symTab.addVariable(var.getName(),var.getValue());
			}
		} 
		else super.addStandardConstants();
	}

	public void addComplex()
	{
		if(ingrediant!=null)
		{
			ingrediant.addComplex();
		} 
		else super.addComplex();
		try {
		MacroFunction complex = new MacroFunction("macrocomplex",2,"x+i*y",this);
		addFunction("macrocomplex",complex);
		} catch (ParseException e) {System.err.println(e.getMessage());}
	}
		

	/** Returns a deep copy of an expression tree. */
	public Node deepCopy(Node node) throws ParseException
	{
		return copier.deepCopy(node,this);
	}
	/** Returns a simplification of an expression tree. */
	public Node simplify(Node node) throws ParseException
	{
		return simpv.simplify(node,this);
	}
	/** Pre-processes an equation to allow the diff and eval operators to be used. */
	public Node preprocess(Node node) throws ParseException
	{
		return commandv.process(node,this);
	}
	/** Substitute all occurrences of a named variable with an expression tree. */ 
	public Node substitute(Node orig,String name,Node replacement) throws ParseException
	{
		return subv.substitute(orig,name,replacement,this);
	}
	/** Substitute all occurrences of a set of named variable with a set of expression tree. */ 
	public Node substitute(Node orig,String names[],Node replacements[]) throws ParseException
	{
		return subv.substitute(orig,names,replacements,this);
	}
	/** Prints the expression tree on standard output. */
	public void print(Node node) { pv.print(node); }
	/** Prints the expression tree on given stream. */
	public void print(Node node,PrintStream out) { pv.print(node,out); }
	/** Prints the expression tree on standard output with newline at end. */
	public void println(Node node) { pv.println(node); }
	/** Prints the expression tree on given stream with newline at end. */
	public void println(Node node,PrintStream out) { pv.println(node,out); }
	/** Returns a string representation of a expression tree. */
	public String toString(Node node) { return pv.toString(node); }
	/** Returns the node factory, used for constructing trees of Nodes. */ 
	public NodeFactory getNodeFactory() {return nf;}
	/** Returns the TreeUtilitities, used for examining properties of nodes. */ 
	public TreeUtils getTreeUtils() { return tu; }
//	public SimplificationVisitor getSimpV() { return simpv; }
	/** Returns the PrintVisitor, used for printing equations. */ 
	public PrintVisitor getPrintVisitor() {	return pv;	}

	/**
	 * Calculates the value for the variables equation and returns that value.  
	 * If the variable does not have an equation just return its value.
	 */
	public Object calcVarValue(String name) throws Exception
	{
		XVariable xvar = (XVariable) getVar(name);
		return xvar.calcValue(this);
	}

	/**
	 * Continue parsing without re-initilising the stream.
	 * Allows re-entrance of parser so that strings like
	 * "x=1; y=2; z=3;" can be parsed.
	 * When a semi colon is encountered parsing finishes leaving the rest of the string unparsed.
	 * Parsing can be resumed from the current position by using this method.
	 * For example
	 * <pre>
	 * XJep j = new XJep();
	 * j.restartParser("x=1;y=2; z=3;");
	 * Node node;
	 * try {
	 * while((node = j.continueParsing())!=null) {
	 *    j.println(node);
	 * } }catch(ParseException e) {}
	 * </pre>
	 * @return top node of equation parsed to date or null if empty equation
	 * @throws ParseException
	 * @see #restartParser(String)
	 */
	public Node continueParsing() throws ParseException {
		return parser.continueParse();
	}

	/**
	 * Restarts the parser with the given string.
	 * @param str String containing a sequence of equations separated by semi-colons.
	 * @see #continueParsing
	 */
	public void restartParser(String str) {
		parser.restart(new java.io.StringReader(str), this);
	}

	/**
	 * Restarts the parser with the given Reader.
	 * @param reader Reader from which equations separated by semi-colons will be read.
	 * @see #continueParsing
	 */
	public void restartParser(Reader reader) {
		parser.restart(reader, this);
	}

	/**
	 * Finds all the variables in an equation.
	 * 
	 * @param n the top node of the expression
	 * @param v a vector to store the list of variables (new variables will be added on end) 
	 * @return v
	 */
	public Vector getVarsInEquation(Node n,Vector v) {
		if(n instanceof ASTVarNode) {
			Variable var = ((ASTVarNode) n).getVar();
			if(!v.contains(var)) v.add(var);
		}
		else if(n instanceof ASTFunNode) {
			for(int i=0;i<n.jjtGetNumChildren();++i)
				getVarsInEquation(n.jjtGetChild(i),v);
		}
		return v;
	}
	
	/**
	 * Finds all the variables in an equation and if any of those 
	 * variables are defined by equations find the variables 
	 * in those equations as well.
	 * The result is an ordered sequence, evaluating each
	 * variable in turn will correctly allow the final equation to be evaluated.
	 * <p>
	 * For example if the equation is <code>a+b</code> and 
	 * <code>a=c+d</code>, <code>b=c+e</code> then the
	 * result will be the sequence <code>(c,d,a,e,b)</code>
	 * 
	 * @param n top node
	 * @param v vector for storing results, new variables will be added on the end.
	 * @return v, the ordered sequence of variables
	 * @throws ParseException if equation is recursive i.e. <code>x=y; y=x+1;</code>
	 */
	public Vector recursiveGetVarsInEquation(Node n,Vector v) throws ParseException {
		if(n instanceof ASTVarNode) {
			XVariable var = (XVariable) (((ASTVarNode) n).getVar());
			if(!v.contains(var))
			{
				if(var.hasEquation())
					recursiveGetVarsInEquation(var.getEquation(),v);
				if(v.contains(var))
					throw new ParseException("Recursive definition for "+var.getName());
				v.add(var);
			}
		}
		else if(n instanceof ASTFunNode) {
			for(int i=0;i<n.jjtGetNumChildren();++i)
				recursiveGetVarsInEquation(n.jjtGetChild(i),v);
		}
		return v;
	}
	
    /**
     *
     */

    public Object evaluate(PostfixMathCommandI pfmc,Node node) throws ParseException {
        // TODO Auto-generated method stub
        return super.evaluate(node);
    }
}
