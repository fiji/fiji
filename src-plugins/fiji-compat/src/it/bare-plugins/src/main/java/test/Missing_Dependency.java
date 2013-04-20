package test;

import ij.IJ;
import ij.plugin.PlugIn;

public class Missing_Dependency implements PlugIn {
	public void run(String arg) {
		IJ.log("Dependency says: " + new Dependency());
	}
}
