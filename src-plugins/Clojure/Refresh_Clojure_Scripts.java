package Clojure;

import common.RefreshScripts;
import java.io.File;
import java.io.InputStream;
import ij.IJ;

public class Refresh_Clojure_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".clj", "Clojure");
		setVerbose(false);
		super.run(arg);
	}

	/** Runs the script at path in the general namespace and interpreter on this JVM, which means the script may change the values of variables and functions in other scripts and in the Clojure interpreter. */
	public void runScript(String path) {
		try {
			if (!path.endsWith(".clj") || !new File(path).exists()) {
				IJ.log("Not a clojure script or not found: " + path);
				return;
			}
			if (IJ.isWindows()) path = path.replace('\\', '/');
			Clojure_Interpreter ci = new Clojure_Interpreter();
			ci.init();
			Object res = ci.evaluate("(load-file \"" + path + "\")");
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
