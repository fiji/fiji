/* @author rich
 * Created on 01-Apr-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import java.util.Enumeration;
import java.util.Vector;

import org.lsmp.djep.matrixJep.MatrixJep;
import org.lsmp.djep.matrixJep.MatrixVariableI;
import org.lsmp.djep.mrpe.MRpCommandList;
import org.lsmp.djep.mrpe.MRpEval;
import org.lsmp.djep.mrpe.MRpRes;
import org.lsmp.djep.vectorJep.VectorJep;
import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.Scaler;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.Variable;

/**
 * @author Rich Morris
 * Created on 01-Apr-2005
 */
public class MRpSurfExample {
	MatrixJep mj;
	VectorJep vj;
	MRpEval mrpe=null;
	MRpCommandList allCommands[];
	Node allEqns[];
	Node vecEqns[];
//	LVars psVars[];
	MatrixVariableI xVar,yVar;
	Variable xVVar,yVVar;
	int xref,yref;

	double xmin = -1.0,xmax = 1.0,ymin=1.0,ymax=1.0;
	int xsteps = 1000,ysteps = 1000;

	public static void main(String args[]) {
		timePrint("\tStart");
		MRpSurfExample surf = new MRpSurfExample();
		timePrint("\tDone init");
		try {
		surf.equationChanged("th=pi*x;phi=pi*y;f=[cos(th) cos(phi),sin(th) cos(phi),sin(phi)];");
		surf.vecEquationChanged(new String[]{"th=pi*x;","phi=pi*y;","[cos(th) cos(phi),sin(th) cos(phi),sin(phi)];"});
		timePrint("\tDone parse");

		surf.calcMRPE();
		surf.mrpe.cleanUp();
		timePrint("\tDone mrpe");
		
		surf.calcMJ();
		timePrint("\tDone MJ");
		
		surf.calcVJ();
		timePrint("\tDone VJ");
		} catch(Exception e) { System.out.println(e.getClass().getName()+": "+e.getMessage()); }
	}
	static long oldTime = 0;
	public static void timePrint(String msg) {
		long time = System.currentTimeMillis();
		long timediff = time-oldTime;
		oldTime = time;
		System.out.println(""+timediff+"\t"+msg);
	}
	public MRpSurfExample() {
		vj = new VectorJep();
		vj.setAllowAssignment(true);
		vj.setAllowUndeclared(true);
		vj.setImplicitMul(true);
		vj.addComplex();
		vj.addStandardConstants();
		vj.addStandardFunctions();

		mj = new MatrixJep();
		mj.setAllowAssignment(true);
		mj.setAllowUndeclared(true);
		mj.setImplicitMul(true);
		mj.addComplex();
		mj.addStandardConstants();
		mj.addStandardFunctions();
		mj.addStandardDiffRules();
		mrpe = new MRpEval(mj);
	}

	public void equationChanged(String text)
	{
		mj.restartParser(text);
		try
		{
			Vector coms = new Vector();
			Vector eqns = new Vector();
			Node n;
			while((n = mj.continueParsing())!=null) {
				Node n2 = mj.preprocess(n);
				MRpCommandList com = mrpe.compile(n2);
				coms.add(com);
				eqns.add(n2);
			}
			int i=0;
			allCommands = new MRpCommandList[coms.size()];
			for(Enumeration en=coms.elements();en.hasMoreElements();++i)
				allCommands[i] = (MRpCommandList) en.nextElement();
			i=0;
			allEqns = new Node[eqns.size()];
			for(Enumeration en=eqns.elements();en.hasMoreElements();++i)
				allEqns[i] = (Node) en.nextElement();
			xVar = (MatrixVariableI) mj.getVar("x");
			yVar = (MatrixVariableI) mj.getVar("y");
			xref = mrpe.getVarRef(xVar);
			yref = mrpe.getVarRef(yVar);

		}
		catch(ParseException e) {System.out.println(e.getMessage());}
	}

	public void vecEquationChanged(String lines[])
	{
		try
		{
			vecEqns = new Node[lines.length];
			for(int i=0;i<lines.length;++i) {
				Node n2 = vj.parse(lines[i]);
				vecEqns[i]=n2;
			}
			xVVar = vj.getVar("x");
			yVVar = vj.getVar("y");
		}
		catch(ParseException e) {e.getMessage();}
	}


	public void calcMRPE() {
		double topRes[]=null;

		for(int i=0;i<=xsteps;++i) {
			double x = xmin + ((xmax - xmin)*i)/xsteps;
			mrpe.setVarValue(xref,x);
			for(int j=0;j<=ysteps;++j) {
				double y = ymin + ((ymax - ymin)*j)/ysteps;
				mrpe.setVarValue(yref,y);

				MRpRes res=null;
				for(int k=0;k<allCommands.length;++k)
					res = mrpe.evaluate(allCommands[k]);
				topRes = (double []) res.toArray();
				
				//System.out.println("["+x+","+y+"]->["+topRes[0]+","+topRes[1]+","+topRes[2]+"]");
			}
		}
		System.out.println("res "+topRes[0]+","+topRes[1]+","+topRes[2]);
	}

	public void calcMJ() throws ParseException {
		Object topRes[]=null;

		Scaler xVal = (Scaler) xVar.getMValue();
		Scaler yVal = (Scaler) yVar.getMValue();
		xVar.setValidValue(true);
		yVar.setValidValue(true);
		for(int i=0;i<=xsteps;++i) {
			double x = xmin + ((xmax - xmin)*i)/xsteps;
			xVal.setEle(0,new Double(x));
			for(int j=0;j<=ysteps;++j) {
				double y = ymin + ((ymax - ymin)*j)/ysteps;
				yVal.setEle(0,new Double(y));

				Object res=null;
				for(int k=0;k<allEqns.length;++k)
					res = mj.evaluate(allEqns[k]);
				topRes = ((MVector) res).getEles();
				
				//System.out.println("["+x+","+y+"]->["+topRes[0]+","+topRes[1]+","+topRes[2]+"]");
			}
		}
		System.out.println("res "+topRes[0]+","+topRes[1]+","+topRes[2]);
	}

	public void calcVJ() throws ParseException,Exception {
		Object topRes[]=null;
		for(int i=0;i<=xsteps;++i) {
			double x = xmin + ((xmax - xmin)*i)/xsteps;
			xVVar.setValue(new Double(x));
			for(int j=0;j<=ysteps;++j) {
				double y = ymin + ((ymax - ymin)*j)/ysteps;
				yVVar.setValue(new Double(y));

				Object res=null;
				for(int k=0;k<vecEqns.length;++k)
					res = vj.evaluate(vecEqns[k]);
				topRes = ((MVector) res).getEles();
				
				//System.out.println("["+x+","+y+"]->["+topRes[0]+","+topRes[1]+","+topRes[2]+"]");
			}
		}
		System.out.println("res "+topRes[0]+","+topRes[1]+","+topRes[2]);
	}
}
