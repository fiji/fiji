package emblcmci.scalainterp;
 
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ij.IJ;
import common.AbstractInterpreter;
import common.RefreshScripts;
import fiji.InspectJar;
import scala.Option;
//import scala.collection.Map;
import scala.collection.immutable.List;
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
		List<String> param = List.make(1, "true");
		settings.usejavacp().tryToSet(param);
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
	String varname, vartype, varval, aline;
	Option<Object> varobj;
	@Override
	protected Object eval(String arg0) throws Throwable {
		imain.interpret(arg0);
		varname = imain.mostRecentVar();
		varobj = imain.valueOfTerm(varname);
		if (varobj.toList().size()>0){
			varval = imain.valueOfTerm(varname).productElement(0).toString();
			vartype = imain.valueOfTerm(varname).productElement(0).getClass().getSimpleName();
			aline = varname + ": " + vartype + " = " + varval;
		} else{
			aline = varname + ": None";
		}
		return aline;

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

    /** pre-import all ImageJ java classes and TrakEM2 java classes */
	/*
	public void importAll(IMain imain) {
        if (System.getProperty("jnlp") != null) {
            println("Because Fiji was started via WebStart, no packages were imported implicitly");
            return;
        }
 
        Map<String, ArrayList<String>> classNames = getDefaultImports2();
        try {        
        	for (String packageName: classNames.keySet())      	
        		imain.quietImport(packageName+"."+ classNames.get(packageName));
//            eval(statement);
        } catch (Throwable e) {
            RefreshScripts.printError(e);
            return;
        }
        println("All ImageJ and java.lang classes imported.");
    }
    protected static Map<String, ArrayList<String>> defaultImports;
    
    public static Map<String, ArrayList<String>> getDefaultImports2() {
        if (defaultImports != null)
            return defaultImports;
 
        final String[] classNames = {
            "ij.IJ", "java.lang.String", "script.imglib.math.Compute"
        };
        InspectJar inspector = new InspectJar();
        for (String className : classNames) try {
            String baseName = className.substring(className.lastIndexOf('.') + 1);
            URL url = Class.forName(className).getResource(baseName + ".class");
            inspector.addJar(url);
        } catch (Exception e) {
            if (IJ.debugMode)
                IJ.log("Warning: class " + className
                        + " was not found!");
        }
        defaultImports = new HashMap<String, ArrayList<String>>();
        Set<String> prefixes = new HashSet<String>();
        prefixes.add("script.");
        for (String className : classNames)
            prefixes.add(className.substring(0, className.lastIndexOf('.')));
        for (String className : inspector.classNames(true)) {
            if (!hasPrefix(className, prefixes))
                continue;
            int dot = className.lastIndexOf('.');
            String packageName = dot < 0 ? "" : className.substring(0, dot);
            String baseName = className.substring(dot + 1);
            ArrayList<String> list = defaultImports.get(packageName);
            if (list == null) {
                list = new ArrayList<String>();
                defaultImports.put(packageName, list);
            }
            list.add(baseName);
        }
        // remove non-unique class names
        Map<String, String> reverse = new HashMap<String, String>();
        for (String packageName : defaultImports.keySet()) {
            Iterator<String> iter = defaultImports.get(packageName).iterator();
            while (iter.hasNext()) {
                String className = iter.next();
                if (reverse.containsKey(className)) {
                    if (IJ.debugMode)
                        IJ.log("Not auto-importing " + className + " (is in both " + packageName + " and " + reverse.get(className) + ")");
                    iter.remove();
                    defaultImports.get(reverse.get(className)).remove(className);
                }
                else
                    reverse.put(className, packageName);
            }
        }
        return defaultImports;
    }    
*/
	
	@Override
	protected String getLineCommentMark() {
		return "//";
	}
	static public void main(String[] args){
		Scala_Interpreter si = new Scala_Interpreter();
		si.run(null);
	}

}
