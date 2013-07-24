import ij.IJ;
import ij.plugin.PlugIn;

public class Another_Bare_PlugIn implements PlugIn {
	public void run(String arg) {
		IJ.log("Hello (another bare) world!");
	}
}

