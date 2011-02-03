/** Albert Cardona 2008. Released under General Public License. */
package Javascript;

import common.AbstractInterpreter;

import ij.IJ;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

public class Javascript_Interpreter extends AbstractInterpreter {

	private Scriptable scope = null;
	private Context cx = null;

	public Javascript_Interpreter() { }

	public Javascript_Interpreter(Context cx, Scriptable scope) {
		this.cx = cx;
		this.scope = scope;
	}

	protected void threadStarting() {
		try {
			// Initialize inside the executer thread in parent class
			println("Starting Javascript ...");
			cx = Context.enter();
			cx.setApplicationClassLoader(IJ.getClassLoader());
			//scope = cx.initStandardObjects(null); // reuse
			// the above, with a null arg, enables reuse of the scope in subsequent calls.
			// But this one is better: includes importClass and importPackage js functions
			scope = new ImporterTopLevel(cx);
			importAll();
			println("done.");
			createBuiltInFunctions();
		} catch (Throwable t) {
			t.printStackTrace(print_out);
		}
	}

	protected void createBuiltInFunctions() {
		StringBuffer fns = new StringBuffer();
		// 1 - "print" as a shortcut for IJ.log:
		try {
			eval("function print(x) { IJ.log(null == x ? null : x.toString()); return x; }");
			fns.append("print,");
		} catch (Throwable e) {}
		// -- other functions here
		// ...

		if (fns.length() > 0) {
			fns.setLength(fns.length()-1); // remove last comma
			println("Created built-in functions: "+ fns);
		}
	}

	protected Object eval(String text) throws Throwable {
		if (null == scope) return null;
		Object result = cx.evaluateString(scope, text, "<cmd>", 1, null);
		return cx.toString(result);
	}

	public void run(String ignored) {
		setTitle("Javascript Interpreter");
		super.run(ignored);
	}

	protected void windowClosing() {}

	protected void threadQuitting() {
		Context.exit();
	}

	protected final static Set excludeFromImport =
		new HashSet(Arrays.<String>asList("InternalError", "Math",
			"Number", "Boolean", "Error", "String", "Object", "Array"));

	/** Import all ImageJ and java.lang classes. */
	protected String getImportStatement(String packageName, Iterable<String> classNames) {
		StringBuffer sb = new StringBuffer();
		if (!"".equals(packageName))
			packageName += ".";
		for (String className : classNames)
			if (!excludeFromImport.contains(className))
				sb.append("importClass(Packages.").append(packageName)
					.append(className).append(");");
		return sb.toString();
	}

	protected String getLineCommentMark() {
		return "//";
	}
}
