package fiji.updater;

import fiji.updater.logic.PluginListBuilder;
import fiji.updater.logic.UpdateFiji;
import fiji.updater.logic.XMLFileDownloader;
import fiji.updater.logic.XMLFileReader;

import fiji.updater.ui.UpdaterFrame;

import fiji.updater.util.Canceled;
import fiji.updater.util.Progress;
import fiji.updater.util.Util;

import ij.IJ;

import ij.plugin.PlugIn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import javax.xml.parsers.ParserConfigurationException;

public class Updater implements PlugIn {
	//public static final String MAIN_URL = "http://pacific.mpi-cbg.de/update/"; //TODO
	public static String MAIN_URL = "http://pacific.mpi-cbg.de/uploads/incoming/plugins/";
	//public static final String UPDATE_DIRECTORY = "/update/";
	public static final String UPDATE_DIRECTORY = "/incoming/plugins/";

	public static final String TXT_FILENAME = "current.txt";
	public static final String XML_LOCK = "db.xml.gz.lock";
	public static final String XML_COMPRESSED = "db.xml.gz";
	public static final String XML_FILENAME = "db.xml";
	public static final String XML_BACKUP = "db.bak";

	// Key names for ij.Prefs for saved values ("cookies")
	// Note: ij.Prefs is only saved during shutdown of Fiji
	public static final String PREFS_XMLDATE = "fiji.updater.xmlDate";
	public static final String PREFS_USER = "fiji.updater.login";

	// TODO: move more functionality into this class; the ui should be the ui only!!!
	public void run(String arg) {
		final UpdaterFrame main = new UpdaterFrame();

		// TODO: use ProgressPane in main window
		Progress progress = main.getProgress("Starting up...");

		XMLFileDownloader downloader = new XMLFileDownloader();
		downloader.addProgress(progress);
		try {
			downloader.start();
			// TODO: it is a parser, not a reader.  And it should
			// be a static method.
			new XMLFileReader(downloader.getInputStream());
		} catch (Canceled e) {
			main.dispose();
			IJ.error("Canceled");
			return;
		} catch (Exception e) {
			e.printStackTrace();
			new File(Util.prefix(XML_COMPRESSED))
					.deleteOnExit();
			IJ.error("Download/checksum failed: " + e);
			fallBackToOldUpdater();
			return;
		}

		progress = main.getProgress("Matching with local files...");
		PluginListBuilder pluginListBuilder =
			new PluginListBuilder(progress);
		try {
			pluginListBuilder.updateFromLocal();
		} catch (Canceled e) {
			main.dispose();
			IJ.error("Canceled");
			return;
		}

		main.setLastModified(downloader.getXMLLastModified());
		main.updatePluginsTable();
	}

	protected void fallBackToOldUpdater() {
		try {
			// TODO: replace by special mode of the Plugin Manager
			UpdateFiji updateFiji = new UpdateFiji();
			updateFiji.hasGUI = true;
			updateFiji.exec(UpdateFiji.defaultURL);
		} catch (SecurityException se) {
			IJ.error("Security exception: " + se);
		}
	}
}
