package Clojure;

import common.RefreshScripts;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
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
			if (null == vars) {
				vars = new HashMap<String,Object>();
			}
			// Redirect output to whatever it was set to with super.setOutputStreams
			PrintWriter pout = new PrintWriter(super.out);
			vars.put("*out*", pout);
			ci.pushThreadBindings(vars); // into clojure.core namespace
			Object res = ci.evaluate(new StringBuilder("(load-file \"").append(path).append("\")").toString());
			if (null != res) {
				String s = res.toString().trim();
				if (s.length() > 0 && !"nil".equals(s)) {
					IJ.log(s); // Not using pout.print(s); pout.flush(); because it will print all the declarations, and it's annoying.
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

			// Redirect output to whatever it was set to with super.setOutputStreams
			HashMap<String,Object> vars = new HashMap<String,Object>();
			PrintWriter pout = new PrintWriter(super.out);
			vars.put("*out*", pout);
			ci.pushThreadBindings(vars); // into clojure.core namespace

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
