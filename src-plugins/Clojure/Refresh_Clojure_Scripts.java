package Clojure;

import common.RefreshScripts;
import java.io.File;
import ij.IJ;

public class Refresh_Clojure_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".clj","Clojure");
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
			Object res = Clojure_Interpreter.evaluate("(load-file \"" + path + "\")");
			if (null != res) {
				String s = res.toString();
				if (s.length() > 0) IJ.log(res.toString());
			}
			Clojure_Interpreter.destroy();
		} catch (Throwable error) {
			printError(error);
		}
	}
}
