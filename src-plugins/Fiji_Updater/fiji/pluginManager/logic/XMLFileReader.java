package fiji.pluginManager.logic;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * XML File Reader, as name implies, reads an already downloaded XML file of containing all
 * existing records of Fiji plugins (Upon calling a constructor). It would save the entire
 * information inside pluginRecordsList.
 *
 * Upon specific requests (call methods) for certain information, they will be retrieved from
 * pluginRecordsList, not the XML file itself.
 *
 */
public class XMLFileReader extends DefaultHandler {
	private Properties properties; //properties of a _single_ version of a single plugin
	private String currentFilename;
	private List<String> links;
	private List<String> authors;
	private List<Dependency> dependencyList;
	private String currentTag;

	//plugin names mapped to list of their respective versions
	private Map<String, PluginCollection> pluginRecords;

	public XMLFileReader(String fileLocation) throws ParserConfigurationException,
	IOException, SAXException {
		initialize(new InputSource(fileLocation));
	}

	public XMLFileReader(InputStream in) throws ParserConfigurationException,
	IOException, SAXException {
		initialize(new InputSource(in));
	}

	private void initialize(InputSource inputSource) throws ParserConfigurationException,
	SAXException, IOException {
		properties = new Properties();
		pluginRecords = new TreeMap<String, PluginCollection>();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		//factory.setValidating(true); //commented out per postel's law
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();

		XMLReader xr = parser.getXMLReader();
		xr.setContentHandler(this);
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(inputSource);
	}

	public void getLatestDigestsAndDates(Map<String, String> latestDigests, Map<String, String> latestDates) {
		for (String pluginName : pluginRecords.keySet()) {
			PluginObject plugin = pluginRecords.get(pluginName).getLatestPlugin();
			latestDigests.put(plugin.getFilename(), plugin.getChecksum());
			latestDates.put(plugin.getFilename(), plugin.getTimestamp());
		}
	}

	public Map<String, PluginCollection> getAllPluginRecords() {
		return pluginRecords;
	}

	//Get the latest version, which _will_ have all the important details/information
	private PluginObject getPluginMatching(String filename) {
		PluginCollection versions = pluginRecords.get(filename);
		if (versions != null) {
			return versions.getLatestPlugin();
		}
		throw new Error("Plugin " + filename + " does not exist.");
	}

	//Get filesize associated with latest version, assumed filename is correct
	public long getFilesizeFrom(String filename) {
		return getPluginMatching(filename).getFilesize(); //only useful for latest
	}

	//Get description associated with latest version, assumed filename is correct
	public PluginDetails getPluginDetailsFrom(String filename) {
		return getPluginMatching(filename).getPluginDetails();
	}

	//Get dependencies associated with latest version, assumed filename is correct
	public List<Dependency> getDependenciesFrom(String filename) {
		return getPluginMatching(filename).getDependencies(); //only useful for latest
	}

	//Get timestamp associated with specified version, assumed filename & digest are correct
	public String getTimestampFromRecords(String filename, String digest) {
		PluginCollection versions = pluginRecords.get(filename);
		String timestamp = null;
		if (versions != null) {
			PluginObject match = versions.getPluginFromDigest(filename, digest);
			if (match != null)
				timestamp = match.getTimestamp();
		}
		return timestamp;
	}

	public void startDocument () { }

	public void endDocument () {
		//no longer needed after parsing, set back to default values
		properties = null;
		currentFilename = null;
		links = null;
		authors = null;
		dependencyList = null;
		currentTag = null;
	}

	public void startElement (String uri, String name, String qName, Attributes atts) {
		if ("".equals (uri))
			currentTag = qName;
		else
			currentTag = name;

		properties.setProperty(currentTag, ""); //cannot put null value
		if (currentTag.equals("plugin")) {
			resetPluginValues();
			currentFilename = atts.getValue("filename");
		} else if (currentTag.equals("version") || currentTag.equals("previous-version")) {
			resetPluginValues();
			properties.setProperty("timestamp", atts.getValue("timestamp"));
			properties.setProperty("checksum", atts.getValue("checksum"));
			String filesize = atts.getValue("filesize");
			properties.setProperty("filesize", (filesize == null ? "0" : filesize));
		}
	}

	private void resetPluginValues() {
		properties = new Properties();
		dependencyList = new ArrayList<Dependency>();
		links = new ArrayList<String>();
		authors = new ArrayList<String>();
	}

	public void endElement (String uri, String name, String qName) {
		String tagName;
		if ("".equals (uri))
			tagName = qName;
		else
			tagName = name;

		if (tagName.equals("version") || tagName.equals("previous-version")) {
			PluginObject plugin = new PluginObject(currentFilename,
					properties.getProperty("checksum"),
					properties.getProperty("timestamp"),
					PluginObject.Status.NOT_INSTALLED, true, true);
			if (tagName.equals("version"))
				plugin.setPluginDetails(new PluginDetails(
						properties.getProperty("description"), links, authors));
			plugin.setFilesize(Integer.parseInt(properties.getProperty("filesize")));
			if (dependencyList.size() > 0)
				plugin.setDependency(dependencyList);

			PluginCollection versions;
			if (pluginRecords.containsKey(plugin.getFilename())) {
				versions = pluginRecords.get(plugin.getFilename());
			} else {
				versions = new PluginCollection();
				pluginRecords.put(plugin.getFilename(), versions);
			}
			versions.add(plugin);

		} else if (tagName.equals("dependency")) {
			Dependency dependency = new Dependency(
					properties.getProperty("filename"),
					properties.getProperty("date"),
					properties.getProperty("relation"));
			dependencyList.add(dependency);

		} else {
			String value = properties.getProperty(tagName);
			if (value != null) {
				if (tagName.equals("link"))
					links.add(value);
				else if (tagName.equals("author"))
					authors.add(value);
			}

		}
	}

	public void characters(char ch[], int start, int length) {
		String value = new String(ch, start, length);
		properties.setProperty(currentTag, properties.getProperty(currentTag, "") + value);
	}

}
