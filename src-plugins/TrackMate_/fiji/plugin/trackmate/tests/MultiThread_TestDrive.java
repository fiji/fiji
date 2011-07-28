package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;

public class MultiThread_TestDrive {

	public static void main(String[] args) throws JDOMException, IOException {
		
		File file = new File("/Users/tinevez/Projects/DMontaras/20052011_8_20.xml");
		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		TrackMateModel model = reader.getModel();
		
		TrackMate_ plugin = new TrackMate_(model);
		
		long start = System.currentTimeMillis();
		plugin.computeSpotFeatures();		
		long end  = System.currentTimeMillis();
		model.getLogger().log(String.format("Calculating features done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
		
		
		
		
	}
	
}
