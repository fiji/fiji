import ij.IJ;
import ij.plugin.PlugIn;

public class Bare_PlugIn implements PlugIn {
	public void run(String arg) {
		IJ.log("Hello (bare) world!");
	}
}
