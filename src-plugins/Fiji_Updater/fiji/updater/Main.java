package fiji.updater;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;
import fiji.updater.logic.PluginObject.Status;
import fiji.updater.logic.XMLFileReader;
import fiji.updater.logic.XMLFileWriter;

import fiji.updater.util.Downloader;
import fiji.updater.util.StderrProgress;
import fiji.updater.util.UpdateJava;
import fiji.updater.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * This class is the command-line interface into Fiji's Updater.
 */
public class Main {
	protected PluginCollection plugins;
	protected StderrProgress progress;

	public Main() throws IOException, MalformedURLException,
			ParserConfigurationException, SAXException {
		this(new URL(Updater.MAIN_URL + Updater.XML_COMPRESSED));
	}

	public Main(URL url) throws IOException,
			ParserConfigurationException, SAXException {
		this(url.openStream());
	}

	public Main(File file) throws FileNotFoundException,IOException,
			ParserConfigurationException, SAXException {
		this(new FileInputStream(file));
	}

	public Main(InputStream in) throws IOException,
			ParserConfigurationException, SAXException {
		plugins = PluginCollection.getInstance();
		progress = new StderrProgress();
		new XMLFileReader(new GZIPInputStream(in), 0);
	}

	public void checksum() {
		checksum(null);
	}

	public void checksum(List<String> files) {
		Checksummer checksummer = new Checksummer(progress);
		if (files != null && files.size() > 0)
			checksummer.updateFromLocal(files);
		else
			checksummer.updateFromLocal();
	}

	class Filter implements PluginCollection.Filter {
		Set<String> fileNames;

		Filter(List<String> files) {
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

	public void list(List<String> files) {
		checksum(files);
		for (PluginObject plugin : plugins.filter(new Filter(files)))
			System.out.println(plugin.filename + "\t("
				+ plugin.getStatus() + ")\t"
				+ plugin.getTimestamp());
	}

	public void listCurrent(List<String> files) {
		for (PluginObject plugin : plugins.filter(new Filter(files)))
			System.out.println(plugin.filename + "-"
				+ plugin.getTimestamp());
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
			return Updater.MAIN_URL + plugin.filename + "-"
				+ plugin.getTimestamp();
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
		for (PluginObject plugin : plugins.filter(new Filter(files)))
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

	protected static Main instance;

	public static Main getInstance() {
		if (instance == null) try {
			instance = new Main();
		} catch (Exception e) {
			System.err.println("Could not parse db.xml.gz: "
				+ e.getMessage());
			throw new RuntimeException(e);
		}
		return instance;
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
			+ "\t--list [<files>]\n"
			+ "\t--list-current [<files>]\n"
			+ "\t--update [<files>]\n"
			+ "\t--update-java");
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			System.exit(0);
		}
		String command = args[0];
		if (command.equals("--list"))
			getInstance().list(makeList(args, 1));
		else if (command.equals("--list-current"))
			getInstance().listCurrent(makeList(args, 1));
		else if (command.equals("--update"))
			getInstance().update(makeList(args, 1));
		else if (command.equals("--update-java"))
			new UpdateJava().run(null);
		else
			usage();
	}
}
