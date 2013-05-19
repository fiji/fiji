package fiji.plugin.trackmate.tests;

import java.io.File;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v12;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TmXmlReaderV12TestDrive {

//	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks_v12.xml");
	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks_v1.2.xml");

	public static void main(String args[]) {

		//		ij.ImageJ.main(args);

		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader reader = new TmXmlReader_v12(file);
		TrackMateModel model = reader.getModel();
		Settings settings = reader.getSettings(new DetectorProvider(model), new TrackerProvider(model), 
				new SpotAnalyzerProvider(model), new EdgeAnalyzerProvider(model), new TrackAnalyzerProvider(model));

		System.out.println(model);
		System.out.println(settings);
		
	}


}

