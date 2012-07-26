package fiji.updater;

import ij.plugin.PlugIn;

public class UptodateCheck implements PlugIn {
	public void run(String arg) {
		if ("quick".equals(arg))
			new Updater().run("check");
	}
}
