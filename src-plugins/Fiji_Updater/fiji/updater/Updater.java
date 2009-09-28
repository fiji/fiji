package fiji.updater;

import ij.IJ;
import ij.WindowManager;

import ij.plugin.PlugIn;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.XMLFileDownloader;
import fiji.updater.logic.XMLFileReader;

import fiji.updater.ui.UpdaterFrame;
import fiji.updater.ui.ViewOptions;

import fiji.updater.ui.ViewOptions.Option;

import fiji.updater.util.Canceled;
import fiji.updater.util.Progress;
import fiji.updater.util.Util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import java.net.UnknownHostException;

import javax.swing.JOptionPane;

import javax.xml.parsers.ParserConfigurationException;

public class Updater implements PlugIn {
	public static String MAIN_URL = "http://pacific.mpi-cbg.de/update/";
	public static String UPDATE_DIRECTORY = "/var/www/update/";

	public static final String TXT_FILENAME = "current.txt";
	public static final String XML_LOCK = "db.xml.gz.lock";
	public static final String XML_COMPRESSED = "db.xml.gz";
	public static final String XML_FILENAME = "db.xml";
	public static final String XML_BACKUP = "db.bak";

	// Key names for ij.Prefs for saved values
	// Note: ij.Prefs is only saved during shutdown of Fiji
	public static final String PREFS_USER = "fiji.updater.login";

	public static boolean debug, testRun;

	public void run(String arg) {
		final UpdaterFrame main = new UpdaterFrame();
		main.setLocationRelativeTo(IJ.getInstance());
		main.setEasyMode(true);
		main.setVisible(true);
		WindowManager.addWindow(main);

		PluginCollection plugins = PluginCollection.getInstance();
		plugins.removeAll(plugins);
		Progress progress = main.getProgress("Starting up...");
		XMLFileDownloader downloader = new XMLFileDownloader();
		downloader.addProgress(progress);
		try {
			downloader.start();
			// TODO: it is a parser, not a reader.  And it should
			// be a static method.
			new XMLFileReader(downloader.getInputStream());
		} catch (Canceled e) {
			downloader.done();
			main.dispose();
			IJ.error("Canceled");
			return;
		} catch (Exception e) {
			e.printStackTrace();
			new File(Util.prefix(XML_COMPRESSED))
					.deleteOnExit();
			downloader.done();
			main.dispose();
			String message;
			if (e instanceof UnknownHostException)
				message = "Failed to lookup host "
					+ e.getMessage();
			else
				message = "Download/checksum failed: " + e;
			IJ.error(message);
			return;
		}

		progress = main.getProgress("Matching with local files...");
		Checksummer checksummer = new Checksummer(progress);
		try {
			if (debug)
				checksummer.done();
			else
				checksummer.updateFromLocal();
		} catch (Canceled e) {
			checksummer.done();
			main.dispose();
			IJ.error("Canceled");
			return;
		}

		if ("update".equals(arg)) {
			plugins.markForUpdate(false);
			main.setViewOption(Option.UPDATEABLE);
			if (testRun)
				System.err.println("forceable updates: "
					+ Util.join(", ",
						plugins.updateable(true))
					+ ", changes: "
					+ Util.join(", ", plugins.changes()));
			else if (plugins.hasForcableUpdates())
				main.warn("There are locally modified files!");
			else if (!plugins.hasChanges())
				main.info("Your Fiji is up to date!");
		}

		main.setLastModified(downloader.getXMLLastModified());
		main.updatePluginsTable();
	}
}
