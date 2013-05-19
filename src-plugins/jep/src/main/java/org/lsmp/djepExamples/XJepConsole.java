/* @author rich
 * Created on 21-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;
import org.lsmp.djep.djep.DPrintVisitor;
import org.lsmp.djep.xjep.*;
import org.nfunk.jep.*;
import java.text.NumberFormat;
import java.util.Enumeration;
/**
 * @author Rich Morris
 * Created on 21-Mar-2005
 */
public class XJepConsole extends Console
{
	private static final long serialVersionUID = -3239922790774093668L;
	protected NumberFormat format=null;
	protected boolean verbose = false;
	
	public static void main(String[] args)
	{
		Console c = new XJepConsole();
		c.run(args);
	}
	
	public String getPrompt()
	{
		return "XJep > ";
	}

	public void initialise()
	{
		j = new XJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setAllowAssignment(true);
		j.setImplicitMul(true);
	}

	public void printHelp()
	{
		super.printHelp();
		println("'setMaxLen 80'\tensures all lines and < 80 chars");
		println("'setDP 3'\tonly prints 3 decimal places");
		println("'setFullBrackets true'\tprints equations with full bracketing");
		println("'setComplexI true'\tprint complex numbers in form x+iy");
		println("'invalidate'\tmarks all variables as invalid, forcing reevaluation");
		println("eg 'x=5','y=2*x' gives value 10, 'invalidate', 'x=6', 'y' gives value 12");
	}

	public void printIntroText()
	{
		println("XJep Console");
		super.printStdHelp();
	}

	public void printOps()
	{
		println("Known operators");
		Operator ops[] = j.getOperatorSet().getOperators();
		int maxPrec = -1;
		for(int i=0;i<ops.length;++i)
			if(((XOperator) ops[i]).getPrecedence()>maxPrec) maxPrec=((XOperator) ops[i]).getPrecedence();
		for(int jj=-1;jj<=maxPrec;++jj)
			for(int i=0;i<ops.length;++i)
				if(((XOperator) ops[i]).getPrecedence()==jj)
					println(((XOperator) ops[i]).toFullString());
	}

	public boolean testSpecialCommands(String command)
	{
		if(!super.testSpecialCommands(command)) return false;
		XJep xj = (XJep) this.j;

		if( command.equals("invalidate"))
		{
			resetVars();
			return false;
		}

		if(command.startsWith("setMaxLen"))
		{
			String words[] = split(command);
			int len = Integer.parseInt(words[1]);
			xj.getPrintVisitor().setMaxLen(len);
			return false;
		}
		if(command.startsWith("setDp"))
		{
			String words[] = split(command);
			int dp = Integer.parseInt(words[1]);
			
			format = NumberFormat.getInstance();
			xj.getPrintVisitor().setNumberFormat(format);
			format.setMaximumFractionDigits(dp);
			format.setMinimumFractionDigits(dp);

			return false;
		}
		if(command.startsWith("setFullBrackets"))
		{
			String words[] = split(command);
			if(words.length>1 && words[1].equals("true"))
				xj.getPrintVisitor().setMode(PrintVisitor.FULL_BRACKET,true);
			else
				xj.getPrintVisitor().setMode(PrintVisitor.FULL_BRACKET,true);
			return false;
		}
		if(command.startsWith("setComplexI"))
		{
			String words[] = split(command);
			if(words.length>1 && words[1].equals("true"))
				xj.getPrintVisitor().setMode(PrintVisitor.COMPLEX_I,true);
			else
				xj.getPrintVisitor().setMode(PrintVisitor.COMPLEX_I,true);
			return false;
		}
		if(command.startsWith("verbose"))
		{
			String words[] = split(command);
			if(words.length<2)
				println("verbose should be on or off");
			else if(words[1].equals("on"))
				verbose = true;
			else if(words[1].equals("off"))
				verbose = true;
			else
				println("verbose should be on or off");
			return false;
		}

		return true;
	}

	public void processEquation(Node node) throws ParseException
	{
		XJep xj = (XJep) j;
		if(xj.getPrintVisitor().getMode(PrintVisitor.FULL_BRACKET))	{
			print("Node:\t"); 
			xj.println(node);
		}
		Node processed = xj.preprocess(node);
		if(processed==null) return;
		if(xj.getPrintVisitor().getMode(PrintVisitor.FULL_BRACKET))	{
			print("Processed:\t"); 
			xj.println(processed);
		}
		Node simp = xj.simplify(processed);
		print("Simplified:\t"); 
		println(xj.toString(simp));
		Object val = xj.evaluate(simp);
		String s = xj.getPrintVisitor().formatValue(val);
		println("Value:\t\t"+s);
	}

	public void printVars() {
		PrintVisitor pv = ((XJep) j).getPrintVisitor();
		SymbolTable st = j.getSymbolTable();
		pv.setMode(DPrintVisitor.PRINT_PARTIAL_EQNS,!verbose);

		println("Variables:");
		for(Enumeration  loop = st.keys();loop.hasMoreElements();)
		{
			String s = (String) loop.nextElement();
			XVariable var = (XVariable) st.getVar(s);
			println("\t"+var.toString(pv));
		}
		pv.setMode(DPrintVisitor.PRINT_PARTIAL_EQNS,true);
	}

	public void resetVars()
	{
		this.j.getSymbolTable().clearValues();
	}

}
