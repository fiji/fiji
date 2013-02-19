package fiji.updater;

import ij.IJ;
import ij.plugin.PlugIn;

public class Updater implements PlugIn {
	public void run(String arg) {
		try {
			final Adapter adapter = new Adapter(true);
			if ("check".equals(arg)) {
				// "quick" is used on startup; don't produce an error in the Debian packaged version
				if (Adapter.isDebian())
					return;
				adapter.checkOrShowDialog();
				return;
			}
			adapter.runUpdater();
		} catch (Throwable t) {
			IJ.handleException(t);
		}
	}

	public static void main(String[] args) {
		new Updater().run("");
	}
}