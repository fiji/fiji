/* @author rich
 * Created on 16-Nov-2003
 */
package org.lsmp.djep.djep;
import org.lsmp.djep.djep.diffRules.DivideDiffRule;
import org.lsmp.djep.djep.diffRules.MacroDiffRules;
import org.lsmp.djep.djep.diffRules.MacroFunctionDiffRules;
import org.lsmp.djep.djep.diffRules.MultiplyDiffRule;
import org.lsmp.djep.djep.diffRules.PassThroughDiffRule;
import org.lsmp.djep.djep.diffRules.PowerDiffRule;
import org.lsmp.djep.djep.diffRules.SubtractDiffRule;
import org.lsmp.djep.xjep.*;
import org.nfunk.jep.*;
/**
 * Adds differentation facilities to JEP.
 * For example
 * <pre>
 * DJep j = new DJep();
 * j.addStandardDiffRules();
 * ....
 * Node node = j.parse("x^3");
 * Node diff = j.differentiate(node,"x");
 * Node simp = j.simplify(diff);
 * j.println(simp);
 * Node node2 = j.parse("diff(x^4,x)");
 * Node proc = j.preprocess(node2);
 * Node simp2 = j.simplify(proc);
 * j.println(simp2);
 * </pre>
 * @author Rich Morris
 * Created on 16-Nov-2003
 */
public class DJep extends XJep {
	protected DifferentiationVisitor dv = new DifferentiationVisitor(this);
	/**
	 * Standard constructor.
	 * Use this instead of JEP or XJep if differentation facilities are required.
	 */
	public DJep()
	{
		this.pv = new DPrintVisitor();
//		this.vf = ;
		this.symTab = new DSymbolTable(new DVariableFactory());

		addFunction("diff",new Diff());
		
		//addDiffRule(new AdditionDiffRule("+"));
		addDiffRule(new PassThroughDiffRule("+",this.getOperatorSet().getAdd().getPFMC()));
		addDiffRule(new SubtractDiffRule("-"));
		addDiffRule(new MultiplyDiffRule("*"));
		addDiffRule(new DivideDiffRule("/"));
		addDiffRule(new PowerDiffRule("^"));
		addDiffRule(new PassThroughDiffRule("UMinus",this.getOperatorSet().getUMinus().getPFMC()));

	}
	/**
	 * Differentiate an equation with respect to a variable.
	 * @param node top node of the expression tree to differentiate.
	 * @param name differentiate with respect to this variable.
	 * @return the top node of a new parse tree representing the derivative.
	 * @throws ParseException if for some reason equation cannot be differentiated,
	 * usually if it has not been taught how to differentiate a particular function.
	 */
	public Node differentiate(Node node,String name) throws ParseException
	{
		return dv.differentiate(node,name,this);
	}
	protected DJep(DJep j)
	{
		super(j);
		this.dv=j.dv;
		
	}

	public XJep newInstance()
	{
		DJep newJep = new DJep(this);
		return newJep;
	}
	public XJep newInstance(SymbolTable st)
	{
		DJep newJep = new DJep(this);
		newJep.symTab = st;
		return newJep;
	}

	/** 
	 * Returns the visitor used for differentiation. Allows more advanced functions.
	 */
	public DifferentiationVisitor getDifferentationVisitor() { return dv; }
	
	 /** 
	   * Adds the standard set of differentation rules. 
	   * Corresponds to all standard functions in the JEP plus a few more.
	   * <pre>
	   * sin,cos,tan,asin,acos,atan,sinh,cosh,tanh,asinh,acosh,atanh
	   * sqrt,log,ln,abs,angle
	   * sum,im,re are handled separately.
	   * rand and mod currently un-handled
	   * 
	   * Also adds rules for functions not in JEP function list:
	   * 	sec,cosec,cot,exp,pow,sgn 
	   * 
	   * TODO include if, min, max, sgn
	   * </pre>
	   * @return false on error
	   */
	  public boolean addStandardDiffRules()
	  {
	  	try
	  	{
	  		addDiffRule(new MacroDiffRules(this,"sin","cos(x)"));
			addDiffRule(new MacroDiffRules(this,"cos","-sin(x)")); 	
			addDiffRule(new MacroDiffRules(this,"tan","1/((cos(x))^2)"));

			
			addDiffRule(new MacroDiffRules(this,"sec","sec(x) * tan(x)"));
			addDiffRule(new MacroDiffRules(this,"cosec","-cosec(x) * cot(x)"));
			addDiffRule(new MacroDiffRules(this,"cot","-(cosec(x))^2"));
				
			addDiffRule(new MacroDiffRules(this,"asin","1/(sqrt(1-x^2))"));
			addDiffRule(new MacroDiffRules(this,"acos","-1/(sqrt(1-x^2))"));
			addDiffRule(new MacroDiffRules(this,"atan","1/(1+x^2)"));

			addDiffRule(new MacroDiffRules(this,"sinh","cosh(x)"));
			addDiffRule(new MacroDiffRules(this,"cosh","sinh(x)"));
			addDiffRule(new MacroDiffRules(this,"tanh","1-(tanh(x))^2"));

			addDiffRule(new MacroDiffRules(this,"asinh","1/(sqrt(1+x^2))"));
			addDiffRule(new MacroDiffRules(this,"acosh","1/(sqrt(x^2-1))"));
			addDiffRule(new MacroDiffRules(this,"atanh","1/(1-x^2)"));

			addDiffRule(new MacroDiffRules(this,"sqrt","1/(2 (sqrt(x)))"));
			
			addDiffRule(new MacroDiffRules(this,"exp","exp(x)"));
//			this.addFunction("pow",new Pow());
//			addDiffRule(new MacroDiffRules(this,"pow","y*(pow(x,y-1))","(ln(x)) (pow(x,y))"));
			addDiffRule(new MacroDiffRules(this,"ln","1/x"));
			addDiffRule(new MacroDiffRules(this,"log",	// -> (1/ln(10)) /x = log(e) / x but don't know if e exists
				this.getNodeFactory().buildOperatorNode(this.getOperatorSet().getDivide(),
					this.getNodeFactory().buildConstantNode(
						this.getTreeUtils().getNumber(1/Math.log(10.0))),
					this.getNodeFactory().buildVariableNode(this.getSymbolTable().makeVarIfNeeded("x")))));
			// TODO problems here with using a global variable (x) in an essentially local context
			addDiffRule(new MacroDiffRules(this,"abs","abs(x)/x"));
			addDiffRule(new MacroDiffRules(this,"atan2","-y/(x^2+y^2)","x/(x^2+y^2)"));
			addDiffRule(new MacroDiffRules(this,"mod","1","0"));
			addDiffRule(new PassThroughDiffRule(this,"sum"));
			addDiffRule(new PassThroughDiffRule(this,"re"));
			addDiffRule(new PassThroughDiffRule(this,"im"));
			addDiffRule(new PassThroughDiffRule(this,"rand"));
			
			MacroFunction cmplx = (MacroFunction) this.getFunctionTable().get("macrocomplex");
			if(cmplx!=null)
			    addDiffRule(new MacroFunctionDiffRules(this,cmplx));
			
	/*		addDiffRule(new PassThroughDiffRule("\"<\"",this.getOperatorSet().getLT().getPFMC()));
			addDiffRule(new PassThroughDiffRule("\">\"",new Comparative(1)));
			addDiffRule(new PassThroughDiffRule("\"<=\"",new Comparative(2)));
			addDiffRule(new PassThroughDiffRule("\">=\"",new Comparative(3)));
			addDiffRule(new PassThroughDiffRule("\"!=\"",new Comparative(4)));
			addDiffRule(new PassThroughDiffRule("\"==\"",new Comparative(5)));
	*/		
//			addDiffRule(new DiffDiffRule(this,"diff"));
			// TODO do we want to add eval here?
//			addDiffRule(new EvalDiffRule(this,"eval",eval));
			
			//addDiffRule(new PassThroughDiffRule("\"&&\""));
			//addDiffRule(new PassThroughDiffRule("\"||\""));
			//addDiffRule(new PassThroughDiffRule("\"!\""));
			
			// also consider if, min, max, sgn, dot, cross, 
			//addDiffRule(new MacroDiffRules(this,"sgn","0"));
			return true;
	  	}
	  	catch(ParseException e)
	  	{
	  		System.err.println(e.getMessage());
	  		return false;
	  	}
	  }

	/**
	 * Adds a rule with instruction on how to differentiate a function.
	 * @param rule
	 */
	public void addDiffRule(DiffRulesI rule) {
		dv.addDiffRule(rule);
	}

}
