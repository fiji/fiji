package fiji.updater;

import ij.IJ;
import ij.plugin.PlugIn;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;
import fiji.updater.logic.XMLFileDownloader;
import fiji.updater.logic.XMLFileReader;

import fiji.updater.ui.SwingTools;
import fiji.updater.ui.UpdaterFrame;
import fiji.updater.ui.ViewOptions;

import fiji.updater.ui.ViewOptions.Option;

import fiji.updater.ui.ij1.GraphicalAuthenticator;
import fiji.updater.ui.ij1.IJ1UserInterface;

import fiji.updater.util.Canceled;
import fiji.updater.util.Progress;
import fiji.updater.util.UserInterface;
import fiji.updater.util.Util;

import ij.Executer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.Authenticator;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import javax.xml.parsers.ParserConfigurationException;

public class Updater implements PlugIn {
	public static String MAIN_URL = "http://fiji.sc/update/";
	public static String UPDATE_DIRECTORY = "/var/www/update/";
	public static String SSH_HOST = "fiji.sc";

	public static final String XML_COMPRESSED = "db.xml.gz";

	// Key names for ij.Prefs for saved values
	// Note: ij.Prefs is only saved during shutdown of Fiji
	public static final String PREFS_USER = "fiji.updater.login";

	public static boolean debug, testRun, hidden;

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

	protected static boolean overwriteWithUpdated(PluginObject plugin) {
		File downloaded = new File(Util.prefix("update/" + plugin.filename));
		if (!downloaded.exists())
			return true; // assume all is well if there is no updated file
		File jar = new File(Util.prefix(plugin.filename));
		if (!jar.delete() && !moveOutOfTheWay(jar))
			return false;
		if (!downloaded.renameTo(jar))
			return false;
		for (;;) {
			downloaded = downloaded.getParentFile();
			if (downloaded == null)
				return true;
			String[] list = downloaded.list();
			if (list != null && list.length > 0)
				return true;
			// dir is empty, remove
			if (!downloaded.delete())
				return false;
		}
	}

	/** This returns true if this seems to be the Debian packaged
	 * version of Fiji, or false otherwise. */

	public static boolean isDebian() {
		String debianProperty = System.getProperty("fiji.debian");
		return debianProperty != null && debianProperty.equals("true");
	}

	/** If this seems to be the Debian packaged version of Fiji,
	 * then produce an error and return true.  Otherwise return false. */

	public static boolean errorIfDebian() {
		// If this is the Debian / Ubuntu packaged version, then
		// insist that the user uses apt-get / synaptic instead:
		if (isDebian()) {
			String message = "You are using the Debian packaged version of Fiji.\n";
			message += "You should update Fiji with your system's usual package manager instead.";
			UserInterface.get().error(message);
			return true;
		} else
			return false;
	}

	protected static boolean moveOutOfTheWay(File file) {
		if (!file.exists())
			return true;
		File backup = new File(file.getParentFile(), file.getName() + ".old");
		if (backup.exists() && !backup.delete()) {
			int i = 2;
			for (;;) {
				backup = new File(file.getParentFile(), file.getName() + ".old" + i);
				if (!backup.exists())
					break;
			}
		}
		return file.renameTo(backup);
	}

	public static void main(String[] args) {
		new Updater().run("");
	}
}