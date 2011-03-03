package fiji.updater;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginCollection.Filter;
import fiji.updater.logic.PluginCollection.UpdateSite;
import fiji.updater.logic.PluginObject;

import fiji.updater.logic.PluginObject.Status;

import fiji.updater.logic.XMLFileDownloader;

import fiji.updater.util.Downloader;
import fiji.updater.util.Progress;
import fiji.updater.util.StderrProgress;
import fiji.updater.util.UpdateJava;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * This class is the command-line interface into Fiji's Updater.
 */
public class Main {
	protected PluginCollection plugins;
	protected Progress progress;

	public Main() throws IOException, ParserConfigurationException, SAXException {
		plugins = new PluginCollection();
		try {
			plugins.read();
		} catch (FileNotFoundException e) { /* ignore */ }
		progress = new StderrProgress();
		XMLFileDownloader downloader = new XMLFileDownloader(plugins);
		downloader.addProgress(progress);
		downloader.start();
	}

	public void checksum() {
		checksum(null);
	}

	public void checksum(List<String> files) {
		Checksummer checksummer = new Checksummer(plugins, progress);
		if (files != null && files.size() > 0)
			checksummer.updateFromLocal(files);
		else
			checksummer.updateFromLocal();
	}

	protected class FileFilter implements Filter {
		protected Set<String> fileNames;

		public FileFilter(List<String> files) {
			if (files != null && files.size() > 0)
				fileNames = new HashSet<String>(files);
		}

		public boolean matches(PluginObject plugin) {
			if (fileNames != null &&
					!fileNames.contains(plugin.filename))
				return false;
			return plugin.getStatus() !=
				Status.OBSOLETE_UNINSTALLED;
		}
	}

	public void listCurrent(List<String> files) {
		for (PluginObject plugin : plugins.filter(new FileFilter(files)))
			System.out.println(plugin.filename + "-"
				+ plugin.getTimestamp());
	}

	public void list(List<String> files, Filter filter) {
		checksum(files);
		if (filter == null)
			filter = new FileFilter(files);
		else
			filter = plugins.and(new FileFilter(files), filter);
		for (PluginObject plugin : plugins.filter(filter))
			System.out.println(plugin.filename + "\t("
				+ plugin.getStatus() + ")\t"
				+ plugin.getTimestamp());
	}

	public void list(List<String> files) {
		list(files, null);
	}

	public void listUptodate(List<String> files) {
		list(files, plugins.is(Status.INSTALLED));
	}

	public void listNotUptodate(List<String> files) {
		list(files, plugins.not(plugins.is(Status.INSTALLED)));
	}

	class OnePlugin implements Downloader.FileDownload {
		PluginObject plugin;

		OnePlugin(PluginObject plugin) {
			this.plugin = plugin;
		}

		public String getDestination() {
			return plugin.filename;
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

	public void update(List<String> files) {
		checksum(files);
		for (PluginObject plugin : plugins.filter(new FileFilter(files)))
			switch (plugin.getStatus()) {
			case UPDATEABLE:
			case MODIFIED:
			case NEW:
				download(plugin);
				break;
			case NOT_FIJI:
			case OBSOLETE:
			case OBSOLETE_MODIFIED:
				delete(plugin);
				break;
			}
	}

	public static Main getInstance() {
		try {
			return new Main();
		} catch (Exception e) {
			System.err.println("Could not parse db.xml.gz: "
				+ e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public static List<String> makeList(String[] list, int start) {
		List<String> result = new ArrayList<String>();
		while (start < list.length)
			result.add(list[start++]);
		return result;
	}

	public static void usage() {
		System.err.println("Usage: fiji.update.Main <command>\n"
			+ "\n"
			+ "Commands:\n"
			+ "\tlist [<files>]\n"
			+ "\tlist-current [<files>]\n"
			+ "\tlist-uptodate [<files>]\n"
			+ "\tlist-not-uptodate [<files>]\n"
			+ "\tupdate [<files>]\n"
			+ "\tupdate-java");
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			System.exit(0);
		}
		String command = args[0];
		if (command.equals("list"))
			getInstance().list(makeList(args, 1));
		else if (command.equals("list-current"))
			getInstance().listCurrent(makeList(args, 1));
		else if (command.equals("list-uptodate"))
			getInstance().listUptodate(makeList(args, 1));
		else if (command.equals("list-not-uptodate"))
			getInstance().listNotUptodate(makeList(args, 1));
		else if (command.equals("update"))
			getInstance().update(makeList(args, 1));
		else if (command.equals("update-java"))
			new UpdateJava().run(null);
		else
			usage();
	}
}
