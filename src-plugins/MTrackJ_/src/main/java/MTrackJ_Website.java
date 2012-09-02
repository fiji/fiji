import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

public class MTrackJ_Website implements PlugIn {
	
	public void run(String arg) {
		
		try { BrowserLauncher.openURL("http://www.imagescience.org/meijering/software/mtrackj/"); }
		catch (Throwable e) { MTrackJ_.error("Could not open default internet browser"); }
	}
	
}
