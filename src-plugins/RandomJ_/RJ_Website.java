import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

public class RJ_Website implements PlugIn {

	public void run(String arg) {

		try { BrowserLauncher.openURL("http://www.imagescience.org/meijering/software/randomj/"); }
		catch (Throwable e) { RJ.error("Could not open default internet browser"); }
	}

}
