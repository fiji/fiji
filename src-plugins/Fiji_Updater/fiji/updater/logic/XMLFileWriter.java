package fiji.updater.logic;

import fiji.updater.logic.PluginCollection.UpdateSite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.xml.sax.helpers.AttributesImpl;

public class XMLFileWriter {
	protected PluginCollection plugins;
	protected TransformerHandler handler;
	protected final String XALAN_INDENT_AMOUNT =
		"{http://xml.apache.org/xslt}" + "indent-amount";
	protected final static String dtd =
		"<!DOCTYPE pluginRecords [\n"
		+ "<!ELEMENT pluginRecords (update-site*, plugin*)>\n"
		+ "<!ELEMENT update-site EMPTY>\n"
		+ "<!ELEMENT plugin (platform*, category*, version?, previous-version*)>\n"
		+ "<!ELEMENT version (description?, dependency*, link*, author*)>\n"
		+ "<!ELEMENT previous-version EMPTY>\n"
		+ "<!ELEMENT description (#PCDATA)>\n"
		+ "<!ELEMENT dependency EMPTY>\n"
		+ "<!ELEMENT link (#PCDATA)>\n"
		+ "<!ELEMENT author (#PCDATA)>\n"
		+ "<!ELEMENT platform (#PCDATA)>\n"
		+ "<!ELEMENT category (#PCDATA)>\n"
		+ "<!ATTLIST update-site name CDATA #REQUIRED>\n"
		+ "<!ATTLIST update-site url CDATA #REQUIRED>\n"
		+ "<!ATTLIST update-site ssh-host CDATA #IMPLIED>\n"
		+ "<!ATTLIST update-site upload-directory CDATA #IMPLIED>\n"
		+ "<!ATTLIST update-site timestamp CDATA #REQUIRED>\n"
		+ "<!ATTLIST plugin update-site CDATA #IMPLIED>\n"
		+ "<!ATTLIST plugin filename CDATA #REQUIRED>\n"
		+ "<!ATTLIST dependency filename CDATA #REQUIRED>\n"
		+ "<!ATTLIST dependency timestamp CDATA #IMPLIED>\n"
		+ "<!ATTLIST dependency overrides CDATA #IMPLIED>\n"
		+ "<!ATTLIST version timestamp CDATA #REQUIRED>\n"
		+ "<!ATTLIST version checksum CDATA #REQUIRED>\n"
		+ "<!ATTLIST version filesize CDATA #REQUIRED>\n"
		+ "<!ATTLIST previous-version timestamp CDATA #REQUIRED>\n"
		+ "<!ATTLIST previous-version checksum CDATA #REQUIRED>]>\n";

	public XMLFileWriter(PluginCollection plugins) {
		this.plugins = plugins;
	}

	public byte[] toByteArray(boolean local) throws SAXException,
			TransformerConfigurationException, IOException,
			ParserConfigurationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		write(out, local);
		return out.toByteArray();
	}

	public byte[] toCompressedByteArray(boolean local) throws SAXException,
			TransformerConfigurationException, IOException,
			ParserConfigurationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		write(new GZIPOutputStream(out), local);
		return out.toByteArray();
	}

	public void validate(boolean local) throws SAXException,
			TransformerConfigurationException, IOException,
			ParserConfigurationException {
		ByteArrayInputStream in = new ByteArrayInputStream(toByteArray(local));
		validate(in);
	}

	public void write(OutputStream out, boolean local) throws SAXException,
			TransformerConfigurationException, IOException,
			ParserConfigurationException {
		createHandler(out);

		handler.startDocument();
		AttributesImpl attr = new AttributesImpl();

		handler.startElement("", "", "pluginRecords", attr);
		if (local) {
			for (String name : plugins.getUpdateSiteNames()) {
				attr.clear();
				UpdateSite site = plugins.getUpdateSite(name);
				setAttribute(attr, "name", name);
				setAttribute(attr, "url", site.url);
				if (site.sshHost != null)
					setAttribute(attr, "ssh-host", site.sshHost);
				if (site.uploadDirectory != null)
					setAttribute(attr, "upload-directory", site.uploadDirectory);
				setAttribute(attr, "timestamp", "" + site.timestamp);
				writeSimpleTag("update-site", null, attr);
			}
		}

		for (PluginObject plugin : plugins.fijiPlugins()) {
			attr.clear();
			assert(plugin.updateSite != null && !plugin.updateSite.equals(""));
			if (local && !plugin.updateSite.equals(PluginCollection.DEFAULT_UPDATE_SITE))
				setAttribute(attr, "update-site", plugin.updateSite);
			setAttribute(attr, "filename", plugin.filename);
			handler.startElement("", "", "plugin", attr);
			writeSimpleTags("platform", plugin.getPlatforms());
			writeSimpleTags("category", plugin.getCategories());

			PluginObject.Version current = plugin.current;
			if (plugin.getChecksum() != null) {
				attr.clear();
				setAttribute(attr, "checksum",
						plugin.getChecksum());
				setAttribute(attr, "timestamp",
						plugin.getTimestamp());
				setAttribute(attr, "filesize", plugin.filesize);
				handler.startElement("", "", "version", attr);
				if (plugin.description != null)
					writeSimpleTag("description",
							plugin.description);

				for (Dependency dependency :
						plugin.getDependencies()) {
					attr.clear();
					setAttribute(attr, "filename",
							dependency.filename);
					setAttribute(attr, "timestamp",
							dependency.timestamp);
					if (dependency.overrides)
						setAttribute(attr,
							"overrides", "true");
					writeSimpleTag("dependency", null, attr);
				}

				writeSimpleTags("link", plugin.getLinks());
				writeSimpleTags("author", plugin.getAuthors());
				handler.endElement("", "", "version");
			}
			if (current != null && !current.checksum
					.equals(plugin.getChecksum()))
				plugin.addPreviousVersion(current.checksum,
						current.timestamp);
			for (PluginObject.Version version :
                                        plugin.getPrevious()) {
                                attr.clear();
                                setAttribute(attr, "timestamp",
                                                version.timestamp);
                                setAttribute(attr, "checksum",
                                                version.checksum);
                                writeSimpleTag("previous-version", null, attr);
                        }
                        handler.endElement("", "", "plugin");
                }
                handler.endElement("", "", "pluginRecords");
                handler.endDocument();
                out.flush();
                out.close();
        }

        protected void setAttribute(AttributesImpl attributes,
                        String key, long value) {
                setAttribute(attributes, key, "" + value);
        }

        protected void setAttribute(AttributesImpl attributes,
                        String key, String value) {
                attributes.addAttribute("", "", key, "CDATA", value);
        }

        protected void writeSimpleTags(String tagName, Iterable<String> values)
                        throws SAXException {
                for (String value : values)
                        writeSimpleTag(tagName, value);
        }

        protected void writeSimpleTag(String tagName, String value)
                        throws SAXException {
                writeSimpleTag(tagName, value, new AttributesImpl());
        }

        protected void writeSimpleTag(String tagName, String value,
                                AttributesImpl attributes)
                        throws SAXException {
                handler.startElement("", "", tagName, attributes);
                if (value != null)
                        handler.characters(value.toCharArray(),
                                        0, value.length());
                handler.endElement("", "", tagName);
        }

	protected void createHandler(OutputStream outputStream)
			throws TransformerConfigurationException {
		StreamResult streamResult =
			new StreamResult(new DTDInserter(outputStream));
		SAXTransformerFactory tf = (SAXTransformerFactory)
			SAXTransformerFactory.newInstance();

		handler = tf.newTransformerHandler();
		Transformer serializer = handler.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.setOutputProperty(XALAN_INDENT_AMOUNT, "4");
		handler.setResult(streamResult);
	}

	protected void validate(InputStream inputStream)
			throws ParserConfigurationException, SAXException,
			       IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();

		XMLReader xr = parser.getXMLReader();
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(new InputSource(inputStream));
		inputStream.close();
	}

	/*
	 * This is an ugly hack; SAX does not let us embed the DTD in a
	 * transformer, but we really want to.
	 *
	 * So this class wraps an output stream, and inserts the DTD when
	 * it sees the string "<pluginRecords>".
	 *
	 * It fails if that string is not written in one go.
	 */
	static class DTDInserter extends OutputStream {
		private OutputStream out;
		private boolean dtdInserted;

		DTDInserter(OutputStream out) {
			this.out = out;
		}

		public void close() throws IOException {
			if (!dtdInserted)
				throw new IOException("DTD not inserted!");
			out.close();
		}

		public void flush() throws IOException {
			out.flush();
		}

		public void write(byte[] b, int off, int len)
				throws IOException {
			if (!insertDTDIfNecessary(b, off, len))
				out.write(b, off, len);
		}

		public void write(byte[] b) throws IOException {
			if (!insertDTDIfNecessary(b, 0, b.length))
				out.write(b);
		}

		public void write(int b) throws IOException {
			out.write(b);
		}

		private boolean insertDTDIfNecessary(byte[] b, int off, int len)
				throws IOException {
			if (dtdInserted)
				return false;
			int found = off + new String(b, off, len)
				.indexOf("<pluginRecords>");
			if (found < 0)
				return false;

			if (found > off)
				out.write(b, off, found - off);
			out.write(dtd.getBytes());
			out.write(b, found, len - found);
			dtdInserted = true;
			return true;
		}
	}
}
