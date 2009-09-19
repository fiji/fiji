package Clojure;

import common.RefreshScripts;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import ij.IJ;

public class Refresh_Clojure_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".clj", "Clojure");
		setVerbose(false);
		super.run(arg);
	}

	public void runScript(String path) {
		runScript(path, null);
	}

	public boolean runScript(String path, Map<String,Object> vars) {
		try {
			if (!path.endsWith(".clj") || !new File(path).exists()) {
				IJ.log("Not a clojure script or not found: " + path);
				return false;
			}
			if (IJ.isWindows()) path = path.replace('\\', '/');
			Clojure_Interpreter ci = new Clojure_Interpreter();
			ci.init();
			if (null != vars) {
				ci.pushThreadBindings(vars);
			}
			Object res = ci.evaluate("(load-file \"" + path + "\")");
			if (null != res) {
				String s = res.toString().trim();
				if (s.length() > 0 && !"nil".equals(s)) {
					IJ.log(s);
				}
			}
			ci.destroy();
			return true;
		} catch (Throwable error) {
			printError(error);
			return false;
		}
	}

	/** Will consume and close the stream. */
	public void runScript(final InputStream istream) {
		try {
			Clojure_Interpreter ci = new Clojure_Interpreter();
			ci.init();
			Object res = ci.evaluate(istream);
			if (null != res) {
				String s = res.toString().trim();
				if (s.length() > 0 && !"nil".equals(s)) {
					IJ.log(s);
				}
			}
			ci.destroy();
		} catch (Throwable error) {
			printError(error);
		}
	}
}
