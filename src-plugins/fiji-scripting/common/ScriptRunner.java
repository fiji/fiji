/** Copyright Albert Cardona 2009
 *  Released under the General Public License, latest version.
 */

package common;

import java.util.Map;
import java.util.Hashtable;
import java.lang.reflect.Method;
import ij.IJ;

import bsh.Interpreter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.python.core.PyDictionary;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/** A convenient script file runner for several scripting languages.
 *  Currently supported: beanshell, javascript and jython. 
 *  */
public class ScriptRunner {

	static public final Hashtable<String,Method> methods = new Hashtable<String,Method>();

	static {
		try {
			methods.put("bsh", ScriptRunner.class.getDeclaredMethod("runBSH", String.class, Map.class));
			methods.put("js", ScriptRunner.class.getDeclaredMethod("runJS", String.class, Map.class));
			methods.put("py", ScriptRunner.class.getDeclaredMethod("runPY", String.class, Map.class));
			// FAILS // methods.put("clj", ScriptRunner.class.getDeclaredMethod("runCLJ", String.class, Map.class));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Run the script at @param path in an environment that has all the @param vars set. */
	static public boolean run(final String path, final Map<String,Object> vars) {
		int i = path.lastIndexOf('.');
		if (-1 == i) {
			System.out.println("ScriptRunner.run: cannot find script extension in path " + path);
			return false;
		}
		String ext = path.substring(i + 1).toLowerCase();
		try {
			return (Boolean) methods.get(ext).invoke(null, path, vars);
		} catch (Throwable e) {
			System.out.println("ScriptRunner.run: cannot execute script with extension " + ext + " at path " + path);
			e.printStackTrace();
			Throwable cause = e.getCause();
			if (null != cause) {
				System.out.println("Caused by:");
				cause.printStackTrace();
			}
			return false;
		}
	}

	/** Run the Beanshell script at @param path in an environment that has all the @param vars set, and no default imports. */
	static public boolean runBSH(final String path, final Map<String,Object> vars) {
		try {
			Interpreter bsh = new Interpreter();
			bsh.setClassLoader(IJ.getClassLoader());
			if (null != vars) {
				for (final Map.Entry<String,Object> e : vars.entrySet()) {
					bsh.set(e.getKey(), e.getValue());
				}
			}
			bsh.source(path);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/** Run the Javascript script at @param path in an environment that has all the @param vars set, and no default imports. */
	static public boolean runJS(final String path, final Map<String,Object> vars) {
		Context cx = Context.enter();
		cx.setApplicationClassLoader(IJ.getClassLoader());
		try {
			Scriptable scope = new ImporterTopLevel(cx);

			if (null != vars) {
				for (final Map.Entry<String,Object> e : vars.entrySet()) {
					ScriptableObject.putProperty(scope, e.getKey(), Context.javaToJS(e.getValue(), scope));
				}
			}

			//if (null == Javascript.Javascript_Interpreter.imports(cx, scope)) IJ.log("Importing ImageJ and java.lang.* classes failed!");

			String script = RefreshScripts.openTextFile(path);
			if (null == script) {
				IJ.log("Could not read javascript file at " + path);
				return false;
			}
			Object result = cx.evaluateString(scope, script, "<cmd>", 1, null);
			// don't print null or undefined results
			if (null != result && !(result instanceof org.mozilla.javascript.Undefined)) {
				IJ.log(cx.toString(result));
			}
		} catch( Throwable t ) {
			t.printStackTrace();
			// should re-throw it
			return false;
		} finally {
			Context.exit();
		}
		return true;
	}

	/** Run the Jython script at @param path in an environment that has all the @param vars set, and no default imports. */
	static public boolean runPY(final String path, final Map<String,Object> vars) {
		try {
			PySystemState pystate = new PySystemState();
			pystate.setClassLoader(IJ.getClassLoader());
			PythonInterpreter pi = new PythonInterpreter(new PyDictionary(), pystate);
			//Jython.Jython_Interpreter.importAll(pi);
			if (null != vars) {
				for (final Map.Entry<String,Object> e : vars.entrySet()) {
					pi.set(e.getKey(), e.getValue());
				}
			}
			pi.execfile(path);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/* // FAILS to find the class ... ?
	static public boolean runCLJ(final String path, final Map<String,Object> vars) {
		try {
			Class c = Class.forName("Clojure.Refresh_Clojure_Scripts");
			return Boolean.TRUE.equals(c.getDeclaredMethod("runScript", new Class[]{String.class, Map.class}).invoke(c.newInstance(), new Object[]{path, vars}));
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}
	*/
}
