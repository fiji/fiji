import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

public class FJ_Website implements PlugIn {

	public void run(String arg) {

		try { BrowserLauncher.openURL("http://www.imagescience.org/meijering/software/featurej/"); }
		catch (Throwable e) { FJ.error("Could not open default internet browser"); }
	}

}
