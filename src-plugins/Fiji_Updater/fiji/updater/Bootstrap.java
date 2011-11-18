package fiji.updater;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginCollection.Filter;
import fiji.updater.logic.PluginObject;

import fiji.updater.logic.PluginObject.Status;

import fiji.updater.logic.XMLFileDownloader;

import fiji.updater.util.Downloader;
import fiji.updater.util.Progress;
import fiji.updater.util.StderrProgress;
import fiji.updater.util.Util;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * This class is the command-line interface into Fiji's Updater.
 */
public class Bootstrap {
	public static void main(String[] args) throws Exception {
		new Bootstrap().run(args);
	}

	protected PluginCollection plugins;
	protected Progress progress;

	public Bootstrap() throws IOException, ParserConfigurationException, SAXException {
		ensureFijiDirIsSet();
		plugins = new PluginCollection();
		try {
			plugins.read();
		} catch (FileNotFoundException e) { /* ignore */ }
		progress = new StderrProgress(80);
		XMLFileDownloader downloader = new XMLFileDownloader(plugins);
		downloader.addProgress(progress);
		downloader.start();
	}

	protected void ensureFijiDirIsSet() {
		if (System.getProperty("fiji.dir") == null) {
			String fijiDir = getClass().getResource("/fiji/updater/Bootstrap.class").getPath();
			if (fijiDir.startsWith("file:"))
				fijiDir = fijiDir.substring(5);
			int bang = fijiDir.indexOf("!/");
			if (bang > 0)
				fijiDir = fijiDir.substring(0, bang);
			for (String suffix : new String[] { "/Fiji_Updater.jar", "/plugins" })
				if (fijiDir.endsWith(suffix))
					fijiDir = fijiDir.substring(0, fijiDir.length() - suffix.length());
			System.setProperty("fiji.dir", fijiDir);
		}
	}

	public void run(String[] args) {
		final List<String> files = args.length == 0 ? null : Arrays.asList(args);

		checksum(files);
		updateOrDelete(files);
	}

	protected class FileFilter implements Filter {
		protected Set<String> fileNames;

		public FileFilter(List<String> files) {
			if (files != null && files.size() > 0) {
				fileNames = new HashSet<String>();
				for (String file : files)
					fileNames.add(Util.stripPrefix(file, ""));
			}
		}

		public boolean matches(PluginObject plugin) {
			if (!plugin.isUpdateablePlatform())
				return false;
			if (fileNames != null &&
					!fileNames.contains(plugin.filename))
				return false;
			return plugin.getStatus() !=
				Status.OBSOLETE_UNINSTALLED;
		}
	}

	public void checksum(List<String> files) {
		Checksummer checksummer = new Checksummer(plugins, progress);
		if (files != null && files.size() > 0)
			checksummer.updateFromLocal(files);
		else
			checksummer.updateFromLocal();
	}

	protected void updateOrDelete(List<String> files) {
		for (PluginObject plugin : plugins.filter(new FileFilter(files)))
			switch (plugin.getStatus()) {
			case MODIFIED:
			case UPDATEABLE:
			case NEW:
			case NOT_INSTALLED:
				download(plugin);
				break;
			case NOT_FIJI:
			case OBSOLETE_MODIFIED:
			case OBSOLETE:
				delete(plugin);
				break;
			default:
				if (files != null && files.size() > 0)
					System.err.println("Not updating " + plugin.filename + " (" + plugin.getStatus() + ")");
			}
		try {
			plugins.write();
		} catch (Exception e) {
			System.err.println("Could not write db.xml.gz:");
			e.printStackTrace();
		}
	}

	class OnePlugin implements Downloader.FileDownload {
		PluginObject plugin;

		OnePlugin(PluginObject plugin) {
			this.plugin = plugin;
		}

		public String getDestination() {
			return Util.prefix(plugin.filename);
		}

		public String getURL() {
			return plugins.getURL(plugin);
		}

		public long getFilesize() {
			return plugin.filesize;
		}

		public String toString() {
			return plugin.filename;
		}
	}

	public void download(PluginObject plugin) {
		try {
			new Downloader(progress).start(new OnePlugin(plugin));
			if (Util.isLauncher(plugin.filename) && !Util.platform.startsWith("win")) try {
				Runtime.getRuntime().exec(new String[] { "chmod", "0755", Util.prefix(plugin.filename) });
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Could not mark " + plugin.filename + " as executable");
			}
			System.err.println("Installed " + plugin.filename);
		} catch (IOException e) {
			System.err.println("IO error downloading "
				+ plugin.filename + ": " + e.getMessage());
		}
	}

	public void delete(PluginObject plugin) {
		if (new File(plugin.filename).delete())
			System.err.println("Deleted " + plugin.filename);
		else
			System.err.println("Failed to delete "
					+ plugin.filename);
	}

	protected static class ProxyAuthenticator extends Authenticator {
		protected Console console = System.console();

		protected PasswordAuthentication getPasswordAuthentication() {
			String user = console.readLine("                                  \rProxy User: ");
			char[] password = console.readPassword("Proxy Password: ");
			return new PasswordAuthentication(user, password);
		}
	}
}