/** Albert Cardona 2008. Released under General Public License. */
package Javascript;

import ij.IJ;

import common.RefreshScripts;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ImporterTopLevel;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;

public class Refresh_Javascript_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".js","Javascript");
		setVerbose(false);
		super.run(arg);
	}

	public void runScript(String filename) {
		try {
			if (! new File(filename).exists()) {
				IJ.log("Could not read javascript file at " + filename);
				return;
			}
			// The stream will be closed by runScript(InputStream)
			runScript(new FileInputStream(filename));
		} catch (Throwable t) {
			printError(t);
		}
	}

	/** Will consume and close the stream. */
	public void runScript(InputStream istream) {
		try {
			Context cx = Context.enter();
			cx.setApplicationClassLoader(IJ.getClassLoader());
			Scriptable scope = new ImporterTopLevel(cx);
			if (null == Javascript_Interpreter.imports(cx, scope)) IJ.log("Importing ImageJ and java.lang.* classes failed!");
			Reader reader = null;
			Object result = null;
			try {
				reader = new BufferedReader(new InputStreamReader(istream));
				result = cx.evaluateReader(scope, reader, "<cmd>", 1, null);
			} catch (Exception e) {
				printError(e);
				return;
			} finally {
				if (null != reader) {
					try {
						reader.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
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
