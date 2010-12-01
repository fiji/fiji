import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

public class TJ_Website implements PlugIn {
	
	public void run(String arg) {
		
		try { BrowserLauncher.openURL("http://www.imagescience.org/meijering/software/transformj/"); }
		catch (Throwable e) { TJ.error("Could not open default internet browser"); }
	}
	
}
