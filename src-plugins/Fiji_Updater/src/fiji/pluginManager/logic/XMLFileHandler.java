package fiji.pluginManager.logic;

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

public class XMLFileHandler {
	private StreamResult streamResult;
	private SAXTransformerFactory tf;
	private TransformerHandler handler;
	private final String XALAN_INDENT_AMOUNT =
		"{http://xml.apache.org/xslt}" + "indent-amount";

	public XMLFileHandler(OutputStream outputStream) throws TransformerConfigurationException {
		streamResult = new StreamResult(outputStream);
		tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

		handler = tf.newTransformerHandler();
		Transformer serializer = handler.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, PluginManager.DTD_FILENAME);
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");
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
}
