/** Scala_Interpreter.java
 * 
 * ImageJ/Fiji plugin for scala REPL,
 * extending AbstractInterpreter abstract class.
 *  
 * Kota Miura (miura@embl)
 * http://cmci.embl.de
 * Nov 12, 2012
 */

package emblcmci.scalainterp;
 
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;
import common.AbstractInterpreter;
import scala.Option;
import scala.collection.immutable.List;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;
import scala.collection.JavaConversions;

public class Scala_Interpreter extends AbstractInterpreter{

	IMain imain = null;
	String varname, vartype, varval, aline;
	Option<Object> varobj;
	final ArrayList<String> preimport_list =  
			new ArrayList<String>(Arrays.asList
					("ij._", "java.lang.String"));
	
	public void run(String args){
		Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
		super.run(args);
		super.setTitle("Scala Interpreter");
		println("Starting Scala...");
		prompt.setEnabled(false);
		PrintStream out = new PrintStream(this.out);
		System.setOut(out);
		System.setErr(out);
		Settings settings = new Settings();
		//val settings = new Settings; settings.usejavacp.value = true
		List<String> param = List.make(1, "true");
		settings.usejavacp().tryToSet(param);
		imain = new IMain(settings, print_out);
		//instead of importAll();
		preimport();
		prompt.setEnabled(true);
		println("ij package is imported. Ready.");
	}
	/**
	 * evaluates Scala commands. 
	 * value to be returned could probably be be simpler. 
	 */
	@Override
	protected Object eval(String arg0) throws Throwable {
		imain.interpret(arg0);
		//List<Name> lines = imain.visibleTermNames();
		//varname = imain.mostRecentVar();
		//varobj = imain.valueOfTerm(varname);
		return null;
	}

	/**
	 * Overriding super abstract method.
	 * Implemented, but not used.
	 */
	@Override
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


    /** 
     * Work around of AbstractInterpreter.importAll()
     */
	public void preimport() {
		imain.quietImport(JavaConversions.asScalaBuffer(preimport_list).toList());
//		final String[] importstatements = {
//	            "ij._", "java.lang.String", "script.imglib.math.Compute"
//	        };
//		for (String statement : importstatements){
//			try {
//				eval("import " + statement);
//			} catch (Throwable e) {
//				IJ.log("Failed importing " + statement);
//				e.printStackTrace();
//			}
//		}
	}

	@Override
	protected String getLineCommentMark() {
		return "//";
	}
	/**
	 * For debugging. 
	 * @param args
	 */
	static public void main(String[] args){
		Scala_Interpreter si = new Scala_Interpreter();
		si.run(null);
	}
}
