package fiji.help;

import fiji.util.MenuItemDiverter;

import ij.IJ;

import ij.plugin.BrowserLauncher;

public class Context_Help extends MenuItemDiverter {
        public final static String url =
                "http://pacific.mpi-cbg.de/wiki/index.php/";

	protected String getTitle() {
		return "Context Help";
	}

	protected void action(String arg) {
		IJ.showStatus("Opening help for " + arg + "...");
		new BrowserLauncher().run(url + arg.replace(' ', '_')
			+ "?menuentry=yes");
	}
}
