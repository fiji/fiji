package emblcmci.scalainterp;
 
import java.io.PrintWriter;

import ij.IJ;
import common.AbstractInterpreter;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;
import scala.tools.nsc.interpreter.IMain.ReadEvalPrint;
//import scala.collection.convert.DecorateAsScala;

public class Scala_Interpreter extends AbstractInterpreter{

	IMain imain = null;
	ReadEvalPrint rep = null;
	public void run(String args){
		Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
		super.run(args);
		setTitle("Scala Interpreter");
		println("Starting Scala...");
		prompt.setEnabled(false);
		Settings settings = new Settings();
//		the line below is currently set as VM argument
//		settings.usejavacp(). = true;
		PrintWriter stream = new PrintWriter(this.out);
		imain = new IMain(settings, stream);
		rep = imain.new ReadEvalPrint();
		//import all ImageJ classes, modify this later. 
		//using IMain.quiteImport() should be faster
		importAll();
		prompt.setEnabled(true);
	}
	//not done. output should appear in the 
	// interpreter.
	String varname, varval;
	@Override
	protected Object eval(String arg0) throws Throwable {
		imain.interpret(arg0);
		varname = imain.mostRecentVar();
		varval = imain.valueOfTerm(varname).toString();
		return varname + ": " + varval;
	}

	@Override
	//modified jython interpreter
	// 
	protected String getImportStatement(String packageName, Iterable<String> classNames) {
		// TODO Auto-generated method stub
		StringBuffer buffer = new StringBuffer();
		buffer.append("{");
		for (String className : classNames) {
			if (buffer.length() > 2)
				buffer.append(", ");
			buffer.append(className);
		}
		buffer.append("}");
		return "".equals(packageName) ?
			"import " + buffer + "\n":
			"import " + packageName + "." + buffer + "\n";
	}

	@Override
	protected String getLineCommentMark() {
		return "//";
	}
	static public void main(String[] args){
		Scala_Interpreter si = new Scala_Interpreter();
		si.run(null);
	}

}
