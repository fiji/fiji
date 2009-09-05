package fiji.updater.logic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

// TODO: rename into XMLFileWriter

public class XMLFileHandler {
	private TransformerHandler handler;
	private final String XALAN_INDENT_AMOUNT =
		"{http://xml.apache.org/xslt}" + "indent-amount";
	private final static String dtd =
		"<!DOCTYPE pluginRecords [\n"
		+ "<!ELEMENT pluginRecords (plugin*)>\n"
		+ "<!ELEMENT plugin (version, previous-version*)>\n"
		+ "<!ELEMENT version (description?, dependency*, link*, author*)>\n"
		+ "<!ELEMENT previous-version EMPTY>\n"
		+ "<!ELEMENT description (#PCDATA)>\n"
		+ "<!ELEMENT dependency EMPTY>\n"
		+ "<!ELEMENT link (#PCDATA)>\n"
		+ "<!ELEMENT author (#PCDATA)>\n"
		+ "<!ATTLIST plugin filename CDATA #REQUIRED>\n"
		+ "<!ATTLIST dependency filename CDATA #REQUIRED>\n"
		+ "<!ATTLIST dependency timestamp CDATA #IMPLIED>\n"
		+ "<!ATTLIST dependency relation CDATA #IMPLIED>\n"
		+ "<!ATTLIST version timestamp CDATA #REQUIRED>\n"
		+ "<!ATTLIST version checksum CDATA #REQUIRED>\n"
		+ "<!ATTLIST version filesize CDATA #REQUIRED>\n"
		+ "<!ATTLIST previous-version timestamp CDATA #REQUIRED>\n"
		+ "<!ATTLIST previous-version checksum CDATA #REQUIRED>]>\n";

	public XMLFileHandler(OutputStream outputStream)
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

	public TransformerHandler getXMLHandler() {
		return handler;
	}

	//throws an error if validation fails
	public void validateXMLContents(InputStream inputStream)
	throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		factory.setNamespaceAware(true);
		SAXParser parser = factory.newSAXParser();

		XMLReader xr = parser.getXMLReader();
		xr.setErrorHandler(new XMLFileErrorHandler());
		xr.parse(new InputSource(inputStream));
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
