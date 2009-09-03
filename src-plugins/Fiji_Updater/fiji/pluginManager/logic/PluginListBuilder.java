package fiji.pluginManager.logic;

import fiji.pluginManager.logic.PluginObject.Status;

import fiji.pluginManager.util.PluginData;

import ij.IJ;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/*
 * PluginListBuilder's overall role is to be in charge of building of a plugin list
 * for interface usage.
 *
 * 1st step: Get information of local plugins (checksums and version)
 * 2nd step: Given XML file, get information of latest Fiji plugins (checksums and version)
 * 3rd step: Build up list of "PluginObject" using both local and updates
 *
 * digests and dates hold checksums and versions of local plugins respectively
 * latestDigests and latestDates hold checksums and versions of latest Fiji plugins
 */
public class PluginListBuilder extends PluginDataObservable {
	private final String[] pluginDirectories = {"plugins", "jars", "retro", "misc"};
	public PluginCollection pluginCollection; //info available after list is built
	private Map<String, String> digests;
	private Map<String, String> dates;
	private Map<String, String> latestDates;
	private Map<String, String> latestDigests;
	private XMLFileReader xmlFileReader;
	private PluginData util;

	public PluginListBuilder(XMLFileReader xmlFileReader, PluginData util) {
		this.util = util;
		if (xmlFileReader == null) throw new Error("XMLFileReader object is null");
		this.xmlFileReader = xmlFileReader;

		//initialize storage
		dates = new TreeMap<String, String>();
		digests = new TreeMap<String, String>();
		latestDates = new TreeMap<String, String>();
		latestDigests = new TreeMap<String, String>();
		pluginCollection = new PluginCollection();
	}

	public void buildFullPluginList() throws ParserConfigurationException, IOException, SAXException {
		//Generates information of plugins on local side (digests, dates)
		buildLocalPluginData();
		//Generates information of latest plugins on remote side
		xmlFileReader.getLatestDigestsAndDates(latestDigests, latestDates);
		//Builds up a list of PluginObjects, of both local and remote
		generatePluginList();
	}

	//To get data of plugins on the local side
	private void buildLocalPluginData() throws ParserConfigurationException, SAXException, IOException {
		List<String> queue = generatePluginNamelist();

		//To calculate the checksums on the local side
		Iterator<String> iter = queue.iterator();
		currentlyLoaded = 0;
		totalToLoad = queue.size();
		while (iter.hasNext()) {
			String outputFilename = util.initializeFilename(iter.next());
			String outputDigest = util.getDigestFromFile(outputFilename);
			digests.put(outputFilename, outputDigest);

			//null indicate XML records does not have such plugin filename and checksums
			String outputDate = xmlFileReader.getTimestampFromRecords(outputFilename,
					outputDigest);
			dates.put(outputFilename, outputDate);

			changeStatus(outputFilename, ++currentlyLoaded, totalToLoad);
		}
	}

	private List<String> generatePluginNamelist() {
		List<String> queue = new ArrayList<String>();

		//Add Fiji launchers if they exist
		if (util.isDeveloper()) {
			for (String launcher : util.getLaunchers()) //From precompiled
				addFileIfExists(launcher, queue);
		} else //Only relevant launcher(s) added
			for (String launcher : util.getRelevantLaunchers())
				addFileIfExists((util.getUseMacPrefix() ? util.getMacPrefix() : "") + launcher, queue);

		//Gather filenames of all local plugins
		addFileIfExists("ij.jar", queue);
		for (String directory : pluginDirectories) {
			File dir = new File(util.prefix(directory));
			if (!dir.isDirectory())
				throw new Error("Plugin Directory " + directory + " does not exist!");
			else
				queueDirectory(queue, directory);
		}

		return queue;
	}

	private void addFileIfExists(String filename, List<String> queue) {
		if (util.fileExists(filename))
			queue.add(filename);
	}

	//recursively looks into a directory and adds the relevant file
	private void queueDirectory(List<String> queue, String path) {
		File dir = new File(util.prefix(path));
		if (!dir.isDirectory())
			return;
		String[] list = dir.list();
		for (int i = 0; i < list.length; i++)
			if (list[i].equals(".") || list[i].equals(".."))
				continue;
			else if (list[i].endsWith(".jar")) {
				//Ignore any empty files (Indicates the plugin is uninstalled)
				if (new File(util.prefix(path + File.separator + list[i])).length() > 0)
					queue.add(path + File.separator + list[i]);
			} else
				queueDirectory(queue,
					path + File.separator + list[i]);
	}

	private void generatePluginList() {
		//Converts data gathered into lists of PluginObject, ready for UI classes usage
		Iterator<String> iterLatest = latestDigests.keySet().iterator();
		while (iterLatest.hasNext()) {
			String pluginName = iterLatest.next();
			if (!util.isDeveloper() && util.isFijiLauncher(pluginName)) {
				if (Arrays.binarySearch(util.getRelevantLaunchers(), pluginName) < 0)
					continue; //don't list if not relevant (platform-specific)
			}
			String digest = digests.get(pluginName);
			String remoteDigest = latestDigests.get(pluginName);
			String date = dates.get(pluginName);
			String remoteDate = latestDates.get(pluginName);
			PluginObject myPlugin = null;

			//null implies that although Fiji plugin, version indicates it does not exist in records
			boolean isRecorded = true;
			if (date == null) {
				//use local plugin's last modified timestamp instead
				date = util.getTimestampFromFile(pluginName);
				isRecorded = false;
			}

			if (digest != null && remoteDigest.equals(digest)) { //if latest version installed
				myPlugin = new PluginObject(pluginName, digest, date,
						Status.INSTALLED, true, true);
			} else if (digest == null) { //if new file (Not installed yet)
				myPlugin = new PluginObject(pluginName, remoteDigest, remoteDate,
						Status.NOT_INSTALLED, true, true);
			} else { //if its installed but can be updated
				myPlugin = new PluginObject(pluginName, digest, date,
						Status.UPDATEABLE, true, isRecorded);
				//set latest update details
				myPlugin.setUpdateDetails(remoteDigest, remoteDate);
			}
			//Plugin shall only contains the latest version's details
			PluginDetails details = xmlFileReader.getPluginDetailsFrom(pluginName);
			myPlugin.setPluginDetails(new PluginDetails(details.getDescription(), details.getLinks(),
					details.getAuthors()));
			myPlugin.setDependency(xmlFileReader.getDependenciesFrom(pluginName));
			myPlugin.setFilesize(xmlFileReader.getFilesizeFrom(pluginName));

			pluginCollection.add(myPlugin);
		}

		//To capture non-Fiji plugins
		Iterator<String> iterCurrent = digests.keySet().iterator();
		while (iterCurrent.hasNext()) {
			String name = iterCurrent.next();
			//If it is not a Fiji plugin (Not found in list of up-to-date versions)
			if (!latestDigests.containsKey(name)) {
				String digest = digests.get(name);
				String date = util.getTimestampFromFile(name);
				//implies third-party plugin, no description nor dependency information available
				PluginObject myPlugin = new PluginObject(name, digest, date,
						Status.INSTALLED, false, false);
				myPlugin.setFilesize(util.getFilesizeFromFile(util.prefix(myPlugin.getFilename())));
				pluginCollection.add(myPlugin);
			}
		}

		for (PluginObject plugin : pluginCollection) {
			File file = new File(util.prefix(plugin.getFilename()));
			if (!file.exists() || file.canWrite())
				continue;
			plugin.setIsReadOnly(true);
			IJ.log(plugin.getFilename() + " is read-only file.");
		} //Still remains in pluginCollection for dependency reference purposes

		setStatusComplete(); //indicate to observer there's no more tasks
	}
}
