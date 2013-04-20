import ij.IJ;
import ij.plugin.PlugIn;

public class Bare_PlugIn implements PlugIn {
	@Override
	public void run(String arg) {
		IJ.log("Hello (bare) world!");
	}
}
