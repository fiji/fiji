package fiji.updater.logic;

import java.io.IOException;
import java.io.InputStream;

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

	// currently parsed
	private PluginObject current;
	private String currentTag, body;

	public XMLFileReader(String fileLocation)
			throws ParserConfigurationException, IOException,
			       SAXException {
		initialize(new InputSource(fileLocation));
	}

	public XMLFileReader(InputStream in)
			throws ParserConfigurationException, IOException,
			       SAXException {
		initialize(new InputSource(in));
	}

	private void initialize(InputSource inputSource)
			throws ParserConfigurationException, SAXException,
			       IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);

		//commented out per postel's law
		//factory.setValidating(true);

		SAXParser parser = factory.newSAXParser();
		XMLReader xr = parser.getXMLReader();
		xr.setContentHandler(this);
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(inputSource);
	}

	public void startDocument () {
		plugins = PluginCollection.getInstance();
		body = "";
	}

	public void endDocument () { }

	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		if ("".equals (uri))
			currentTag = qName;
		else
			currentTag = name;

		if (currentTag.equals("plugin"))
			current = new PluginObject(atts.getValue("filename"),
				"", 0, PluginObject.Status.NOT_INSTALLED);
		else if (currentTag.equals("previous-version"))
			current.addPreviousVersion(atts.getValue("checksum"),
				getLong(atts, "timestamp"));
		else if (currentTag.equals("version")) {
			current.current.checksum = atts.getValue("checksum");
			current.current.timestamp = getLong(atts, "timestamp");
			current.filesize = getLong(atts, "filesize");
		}
		else if (currentTag.equals("dependency")) {
			String timestamp = atts.getValue("timestamp");
			String relation = atts.getValue("relation");
			current.addDependency(atts.getValue("filename"),
				getLong(atts, "timestamp"),
				relation == null ? "at-least" : relation);
		}
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
		else if (tagName.equals("link"))
			current.addLink(body);
		else if (tagName.equals("plugin")) {
			plugins.add(current);
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
