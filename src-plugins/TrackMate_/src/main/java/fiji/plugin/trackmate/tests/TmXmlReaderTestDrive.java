package fiji.plugin.trackmate.tests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TmXmlReaderTestDrive {

	public static void main(String args[]) {

		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader reader = new TmXmlReader(file);
		Model model = reader.getModel();
		
		Settings settings = new Settings();
		reader.readSettings(settings, new DetectorProvider(model), new TrackerProvider(model), 
				new SpotAnalyzerProvider(model), new EdgeAnalyzerProvider(model), new TrackAnalyzerProvider(model));
		
		System.out.println(settings); 
		System.out.println(model);
		System.out.println(model.getFeatureModel().echo()); 
		
	}


}
