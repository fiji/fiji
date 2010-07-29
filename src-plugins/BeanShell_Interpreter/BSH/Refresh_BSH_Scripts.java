package BSH;

import bsh.EvalError;

import common.RefreshScripts;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.PrintStream;
import bsh.Interpreter;
import ij.IJ;


public class Refresh_BSH_Scripts extends RefreshScripts {

	public void run(String arg) {
		setLanguageProperties(".bsh","BeanShell");
		setVerbose(false);
		super.run(arg);
	}

	/** Runs the script at path */
	public void runScript(String path) {
		try {
			if (!path.endsWith(".bsh") || !new File(path).exists()) {
				IJ.log("Not a BSH script or not found: " + path);
				return;
			}
			// The stream will be closed by runScript(InputStream)
			runScript(new BufferedInputStream(new FileInputStream(new File(path))), path);
		} catch (Throwable error) {
			printError(error);
		}
	}

	/** Will consume and close the stream. */
	public void runScript(InputStream istream) {
		runScript(istream, null);
	}

	/** Will consume and close the stream. */
	public void runScript(InputStream istream, String sourceFileName) {
		try {
			Interpreter interpreter = new Interpreter();
			interpreter.setOut(new PrintStream(out));
			interpreter.setErr(new PrintStream(err));
			interpreter.eval(new InputStreamReader(istream),
				interpreter.getNameSpace(), sourceFileName);
		} catch (Throwable error) {
			if (error instanceof EvalError)
				((EvalError)error).setMessage(error.toString());
			printError(error);
		}
	}
}
