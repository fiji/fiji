package fiji.plugin.trackmate.tests;

import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.jdom.Element;
import org.jdom.input.SAXHandler;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.xml.sax.SAXException;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.segmentation.BasicSegmenterSettings;
import fiji.plugin.trackmate.segmentation.LogSegmenterSettings;
import fiji.plugin.trackmate.tracking.TrackerSettings;

public class Marshalling_TestDrive {

	public static void main(String[] args) throws JAXBException, SAXException, IOException {
		
		XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
		SAXHandler handler = new SAXHandler();
		
		// Marshall mother class
		BasicSegmenterSettings ss = new BasicSegmenterSettings();
		JAXBContext context = JAXBContext.newInstance(ss.getClass());
		Marshaller marshaller = context.createMarshaller();
		marshaller.marshal(ss, handler);
		out.output(handler.getCurrentElement(), System.out);
		
		// Marshall derived class
		System.out.println();
		LogSegmenterSettings ls = new LogSegmenterSettings();
		context = JAXBContext.newInstance(ls.getClass());
		marshaller = context.createMarshaller();
		marshaller.marshal(ls, handler);
		Element el = handler.getCurrentElement();
		out.output(el, System.out);
		
		// Modify marshalled element
		System.out.println();
		el.getAttribute("threshold").setValue("3.14156");
		out.output(el, System.out);
		
		// Un-marshall modified element
		System.out.println();
		Unmarshaller unmarshaller = context.createUnmarshaller();
		LogSegmenterSettings ls2  = (LogSegmenterSettings) unmarshaller.unmarshal(new JDOMSource(el));
		System.out.println(ls2.toString());
		
		// TrackerSettings
		TrackerSettings ts = new TrackerSettings();
		ts.linkingFeaturePenalties.put(Spot.QUALITY, 3.5);
		
		context = JAXBContext.newInstance(ts.getClass());
		marshaller = context.createMarshaller();
		marshaller.marshal(ts, handler);
		el = handler.getCurrentElement();
		out.output(el, System.out);
		
		
	}
	
}
