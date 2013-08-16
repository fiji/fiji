/* @author rich
 * Created on 01-Apr-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import java.util.Enumeration;
import java.util.Vector;

import org.lsmp.djep.djep.DPrintVisitor;
import org.lsmp.djep.matrixJep.MatrixJep;
import org.lsmp.djep.matrixJep.MatrixVariableI;
import org.lsmp.djep.mrpe.MRpCommandList;
import org.lsmp.djep.mrpe.MRpEval;
import org.lsmp.djep.mrpe.MRpRes;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

/**
 * An example of using MRpEval with differentation.
 * 
 * @author Rich Morris
 * Created on 01-Apr-2005
 */
public class MRpDiffExample {
	MatrixJep mj;
	MRpEval mrpe=null;
	MRpCommandList allCommands[];
	int xref,yref;
	double xmin = -1.0,xmax = 1.0,ymin=1.0,ymax=1.0;
	int xsteps = 100,ysteps = 100;

	public static void main(String args[]) {
		timePrint("\tStart");
		MRpDiffExample surf = new MRpDiffExample();
		timePrint("\tDone init");
		try {
		surf.compile("th=pi*x;phi=pi*y;f=[cos(th) cos(phi),sin(th) cos(phi),sin(phi)];dx=diff(f,x);dy=diff(f,y);dx^dy;");
		timePrint("\tDone parse");

		surf.calcMRPE();
		surf.mrpe.cleanUp();
		timePrint("\tDone mrpe");
		
		} catch(Exception e) { System.out.println(e.getClass().getName()+": "+e.getMessage()); }
	}
	static long oldTime = 0;
	public static void timePrint(String msg) {
		long time = System.currentTimeMillis();
		long timediff = time-oldTime;
		oldTime = time;
		System.out.println(""+timediff+"\t"+msg);
	}
	public MRpDiffExample() {

		mj = new MatrixJep();
		mj.setAllowAssignment(true);
		mj.setAllowUndeclared(true);
		mj.setImplicitMul(true);
		mj.addComplex();
		mj.addStandardConstants();
		mj.addStandardFunctions();
		mj.addStandardDiffRules();
		mrpe = new MRpEval(mj);
		//mj.getPrintVisitor().setMode(PrintVisitor.FULL_BRACKET,true);
		mj.getPrintVisitor().setMode(DPrintVisitor.PRINT_PARTIAL_EQNS,false);
	}

	/**
	 * Compile a sequence of equations.
	 * 
	 * @param text
	 */
	public void compile(String text)
	{
		mj.restartParser(text);
		try
		{
			Vector eqns = new Vector();
			Node n;
			while((n = mj.continueParsing())!=null) {
				Node n2 = mj.preprocess(n);
				Node n3 = mj.simplify(n2);
				eqns.add(n3);
			}
			// gets the top equation
			Node topEqn = (Node) eqns.get(eqns.size()-1);
			// differentiate it
			Node dx = mj.differentiate(topEqn,"x");
			Node dy = mj.differentiate(topEqn,"y");
			
			// create a list of all variables needed to 
			// successfully evaluate topEqn, dx, and dy
			Vector deps = mj.recursiveGetVarsInEquation(topEqn,new Vector());
			deps = mj.recursiveGetVarsInEquation(dx,deps);
			deps = mj.recursiveGetVarsInEquation(dy,deps);
			
			// Compile all equations needed for successful evaluation
			// of top, dx, dy
			Vector coms = new Vector();
			for(Enumeration en=deps.elements();en.hasMoreElements();) {
				MatrixVariableI var = (MatrixVariableI) en.nextElement();
				if(var.hasEquation()) {
					Node eqn = var.getEquation();
					System.out.print("Compiling "+var.getName()+"=");
					mj.println(eqn);
					coms.add(mrpe.compile(var,eqn));
				}
				else {
					// The variable has no equation
					System.out.println("Ignoring "+var.getName());
				}
			}
			// compile the top equation and derivatives
			System.out.print("Compiling ");	mj.println(topEqn);
			coms.add(mrpe.compile(topEqn));
			System.out.print("Compiling ");	mj.println(dx);
			coms.add(mrpe.compile(dx));
			System.out.print("Compiling ");	mj.println(dy);
			coms.add(mrpe.compile(dy));

			// put into any array
			int i=0;
			allCommands = new MRpCommandList[coms.size()];
			for(Enumeration en=coms.elements();en.hasMoreElements();++i)
				allCommands[i] = (MRpCommandList) en.nextElement();

			// finds the references for two variables
			xref = mrpe.getVarRef(mj.getVar("x"));
			yref = mrpe.getVarRef(mj.getVar("y"));
		}
		catch(ParseException e) {e.getMessage();}
	}

	public void calcMRPE() {
		double topRes[]=null,dxRes[]=null,dyRes[]=null;
		for(int i=0;i<=xsteps;++i) {
			double x = xmin + ((xmax - xmin)*i)/xsteps;
			mrpe.setVarValue(xref,x);
			for(int j=0;j<=ysteps;++j) {
				double y = ymin + ((ymax - ymin)*j)/ysteps;
				mrpe.setVarValue(yref,y);

				MRpRes res=null;
				for(int k=0;k<allCommands.length-3;++k)
					res = mrpe.evaluate(allCommands[k]);
				res = mrpe.evaluate(allCommands[allCommands.length-3]);
				topRes = (double []) res.toArray();
				res = mrpe.evaluate(allCommands[allCommands.length-2]);
				dxRes = (double []) res.toArray();
				res = mrpe.evaluate(allCommands[allCommands.length-1]);
				dyRes = (double []) res.toArray();
				
			}
		}
		System.out.println("top ["+topRes[0]+","+topRes[1]+","+topRes[2]+"]");
		System.out.println("dx ["+dxRes[0]+","+dxRes[1]+","+dxRes[2]+"]");
		System.out.println("dy ["+dyRes[0]+","+dyRes[1]+","+dyRes[2]+"]");
	}
}
