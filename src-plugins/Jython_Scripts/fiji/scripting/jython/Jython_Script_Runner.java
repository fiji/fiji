package fiji.scripting.jython;

import ij.IJ;
import ij.plugin.PlugIn;
import Jython.Refresh_Jython_Scripts;

public class Jython_Script_Runner implements PlugIn {
	public void run(String arg) {
		try {
			new Refresh_Jython_Scripts().runScript(
				getClass().getResource(arg).openStream());
		} catch (Exception e) {
			IJ.handleException(e);
		}
	}
}
