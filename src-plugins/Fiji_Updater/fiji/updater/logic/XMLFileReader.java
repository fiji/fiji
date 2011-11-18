package fiji.updater.logic;

import fiji.updater.logic.PluginCollection.UpdateSite;

import fiji.updater.logic.PluginObject.Status;

import fiji.updater.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.xml.sax.helpers.DefaultHandler;

/*
 * XML File Reader, as name implies, reads an already downloaded XML file of
 * containing all existing records of Fiji plugins (Upon calling a
 * constructor). It would save the entire information inside pluginRecordsList.
 *
 * Upon specific requests (call methods) for certain information, they will be
 * retrieved from pluginRecordsList, not the XML file itself.
 */
public class XMLFileReader extends DefaultHandler {
	private PluginCollection plugins;

	// this is the name of the update site (null means we read the local db.xml.gz)
	protected String updateSite;

	// every plugin newer than this was not seen by the user yet
	protected long newTimestamp;

	// There might have been warnings
	protected StringBuffer warnings = new StringBuffer();

	// currently parsed
	private PluginObject current;
	private String currentTag, body;

	public XMLFileReader(PluginCollection plugins) {
		this.plugins = plugins;
	}

	public String getWarnings() {
		return warnings.toString();
	}

	public void read(String updateSite) throws ParserConfigurationException, IOException, SAXException {
		UpdateSite site = plugins.getUpdateSite(updateSite);
		if (site == null)
			throw new IOException("Unknown update site: " + site);
		URL url = new URL(site.url + Util.XML_COMPRESSED);
		URLConnection connection = url.openConnection();
		long lastModified = connection.getLastModified();
		read(updateSite, new GZIPInputStream(connection.getInputStream()), site.timestamp);

		// lastModified is a Unix epoch, we need a timestamp
		site.timestamp = Long.parseLong(Util.timestamp(lastModified));
	}

	public void read(File file) throws ParserConfigurationException, IOException, SAXException {
		read(null, new GZIPInputStream(new FileInputStream(file)), 0);
	}

	// timestamp is the timestamp (not the Unix epoch) we last saw updates from this site
	public void read(String updateSite, InputStream in, long timestamp) throws ParserConfigurationException, IOException, SAXException {
		this.updateSite = updateSite;
		newTimestamp = timestamp;

		InputSource inputSource = new InputSource(in);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);

		// commented-out as per Postel's law
		//factory.setValidating(true);

		SAXParser parser = factory.newSAXParser();
		XMLReader xr = parser.getXMLReader();
		xr.setContentHandler(this);
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(inputSource);
	}

	public void startDocument () {
		body = "";
	}

	public void endDocument () { }

	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		if ("".equals (uri))
			currentTag = qName;
		else
			currentTag = name;

		if (currentTag.equals("plugin")) {
			String updateSite = this.updateSite;
			if (updateSite == null) {
				updateSite = atts.getValue("update-site");
				if (updateSite == null)
					updateSite = PluginCollection.DEFAULT_UPDATE_SITE;
			}
			current = new PluginObject(updateSite, atts.getValue("filename"),
				null, 0, Status.NOT_INSTALLED);
			if (this.updateSite != null && !this.updateSite.equals(PluginCollection.DEFAULT_UPDATE_SITE)) {
				PluginObject already = plugins.getPlugin(current.filename);
				if (already != null && !this.updateSite.equals(already.updateSite))
					warnings.append("Warning: '" + current.filename + "' from update site '"
						+ this.updateSite
						+ "' shadows the one from update site '"
						+ already.updateSite
						+ "'\n");
			}
		}
		else if (currentTag.equals("previous-version"))
			current.addPreviousVersion(atts.getValue("checksum"),
				getLong(atts, "timestamp"));
		else if (currentTag.equals("version")) {
			current.setVersion(atts.getValue("checksum"),
					getLong(atts, "timestamp"));
			current.filesize = getLong(atts, "filesize");
		}
		else if (currentTag.equals("dependency")) {
			String timestamp = atts.getValue("timestamp");
			String overrides = atts.getValue("overrides");
			current.addDependency(atts.getValue("filename"),
				getLong(atts, "timestamp"),
				overrides != null && overrides.equals("true"));
		}
		else if (updateSite == null && currentTag.equals("update-site"))
			plugins.addUpdateSite(atts.getValue("name"),
				atts.getValue("url"), atts.getValue("ssh-host"), atts.getValue("upload-directory"),
				Long.parseLong(atts.getValue("timestamp")));
	}

	public void endElement(String uri, String name, String qName) {
		String tagName;
		if ("".equals (uri))
			tagName = qName;
		else
			tagName = name;

		if (tagName.equals("description"))
			current.description = body;
		else if (tagName.equals("author"))
			current.addAuthor(body);
		else if (tagName.equals("platform"))
			current.addPlatform(body);
		else if (tagName.equals("category"))
			current.addCategory(body);
		else if (tagName.equals("link"))
			current.addLink(body);
		else if (tagName.equals("plugin")) {
			if (current.current == null)
				current.setStatus(Status.OBSOLETE_UNINSTALLED);
			else if (current.isNewerThan(newTimestamp)) {
				current.setStatus(Status.NEW);
				current.setAction(plugins, current.isUpdateablePlatform() ?
					PluginObject.Action.INSTALL :
					PluginObject.Action.NEW);
			}
			PluginObject plugin = plugins.getPlugin(current.filename);
			if (updateSite == null && current.updateSite != null &&
					plugins.getUpdateSite(current.updateSite) == null)
				; // ignore plugin with invalid update site
			else if (plugin == null)
				plugins.add(current);
			else {
				plugin.merge(current);
				if (updateSite != null && (plugin.updateSite == null || !plugin.updateSite.equals(current.updateSite)))
					plugin.updateSite = current.updateSite;
			}
			current = null;
		}
		body = "";
	}

	public void characters(char ch[], int start, int length) {
		body += new String(ch, start, length);
	}

	private long getLong(Attributes attributes, String key) {
		String value = attributes.getValue(key);
		return value == null ? 0 : Long.parseLong(value);
	}
}