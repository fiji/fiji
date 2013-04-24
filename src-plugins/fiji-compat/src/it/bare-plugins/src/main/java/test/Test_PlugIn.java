package test;

import ij.IJ;
import ij.plugin.PlugIn;

public class Test_PlugIn implements PlugIn {
	public void run(String arg) {
		IJ.log("Hello (test) world!");
	}
}
