/** Albert Cardona 2008. Released under General Public License. */
package Javascript;

import common.AbstractInterpreter;

import ij.IJ;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;

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
			scope = getScopeAndImportAll(cx);
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

	protected static Scriptable getScopeAndImportAll(Context cx) {
		try {
			cx.setApplicationClassLoader(IJ.getClassLoader());
			Scriptable scope = new ImporterTopLevel(cx);
			new Javascript_Interpreter(cx, scope).importAll();
			return scope;
		} catch (Throwable t) { }

		// work around obsolete Rhino, e.g. in Matlab
		final ClassLoader loader = IJ.getClassLoader();
		final Map<String, String> map = new HashMap<String, String>();
		Map<String, List<String>> defaultImports = getDefaultImports();
		for (String packageName : defaultImports.keySet())
			for (String className : defaultImports.get(packageName))
				map.put(className, packageName);

		return new ImporterTopLevel(cx) {
			// reverse map class -> package
			@Override
			public Object get(String name, Scriptable start) {
				Object result = super.get(name, start);
				if (result != UniqueTag.NOT_FOUND)
					return result;
				String packageName = map.get(name);
				if (packageName == null)
					return result;
				try {
					Class<?> clazz = loader.loadClass(packageName + "." + name);
					return new NativeJavaClass(this, clazz);
				} catch (ClassNotFoundException e) {
					return result;
				}
			};
		};
	}

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
