package BSH;

import common.RefreshScripts;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
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
			new Interpreter().eval(new BufferedReader(new FileReader(path)));
		} catch (Throwable error) {
			printError(error);
		}
	}
}
