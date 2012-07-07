package fiji.updater.logic;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLFileErrorHandler implements ErrorHandler {
	public void error(SAXParseException e) throws SAXException {
		throwError(e);
	}

	public void fatalError(SAXParseException e) throws SAXException {
		throwError(e);
	}

	public void warning(SAXParseException e) throws SAXException {
		System.out.println("XML File Warning: " + e.getLocalizedMessage());
	}

	private void throwError(SAXParseException e) {
		throw new Error(e.getLocalizedMessage() +
				"\n\nPublic ID: " + (e.getPublicId() == null ? "None" : e.getPublicId()) +
				", System ID: " + (e.getPublicId() == null ? "None" : e.getPublicId()) +
				",\nLine number: " + e.getLineNumber() + ", Column number: " + e.getColumnNumber());
	}
}
