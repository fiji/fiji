package fiji.plugin.trackmate.tests;

import java.io.File;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v20;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TmXmlReaderV20_TestDrive {

	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks_v20.xml");

	public static void main(final String args[]) {

		//		ij.ImageJ.main(args);

		System.out.println("Opening file: "+file.getAbsolutePath());
		final TmXmlReader reader = new TmXmlReader_v20(file);
		final Model model = reader.getModel();
		final Settings settings = new Settings();
		reader.readSettings(settings, new DetectorProvider(model), new TrackerProvider(model),
				new SpotAnalyzerProvider(model), new EdgeAnalyzerProvider(model), new TrackAnalyzerProvider(model));

		System.out.println(model);
		System.out.println(settings);

	}
}

