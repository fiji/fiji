package fiji.updater;

import fiji.updater.logic.Checksummer;
import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;
import fiji.updater.logic.PluginObject.Status;
import fiji.updater.logic.XMLFileReader;
import fiji.updater.logic.XMLFileWriter;

import fiji.updater.util.StderrProgress;
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
		new XMLFileReader(new GZIPInputStream(in), 0);
	}

	public void checksum() {
		checksum(null);
	}

	public void checksum(List<String> files) {
		StderrProgress progress = new StderrProgress();
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

	public static void usage() {
		System.err.println("Usage: fiji.update.Main <command>\n"
			+ "\n"
			+ "Commands:\n"
			+ "\t--list [<files>]");
	}

	public static List<String> makeList(String[] list, int start) {
		List<String> result = new ArrayList<String>();
		while (start < list.length)
			result.add(list[start++]);
		return result;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			System.exit(0);
		}
		String command = args[0];
		if (command.equals("--list"))
			getInstance().list(makeList(args, 1));
		else
			usage();
	}
}
