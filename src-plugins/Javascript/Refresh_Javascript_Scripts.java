/** Albert Cardona 2008. Released under General Public License. */
package Javascript;

import ij.IJ;

import common.RefreshScripts;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ImporterTopLevel;

public class Refresh_Javascript_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".js","Javascript");
		setVerbose(false);
		super.run(arg);
	}

	public void runScript(String filename) {
		Context cx = Context.enter();
		try {
			Scriptable scope = new ImporterTopLevel(cx);
			if (null == Javascript_Interpreter.imports(cx, scope)) IJ.log("Importing ImageJ and java.lang.* classes failed!");
			String script = openTextFile(filename);
			if (null == script) IJ.log("Could not read javascript file at " + filename);
			Object result = cx.evaluateString(scope, script, "<cmd>", 1, null);
			// don't print null or undefined results
			if (null != result && !(result instanceof org.mozilla.javascript.Undefined)) {
				IJ.log(cx.toString(result));
			}
		} catch( Throwable t ) {
			printError(t);
		} finally {
			Context.exit();
		}
	}
}
