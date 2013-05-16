package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.IJ;
import ij.ImageJ;

import java.io.File;

public class ExportStats_TestDrive {

	public static void main(String[] args) {
		
		ImageJ.main(args);
		
		File file;
		if (!IJ.isWindows()) {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
//			file = new File("/Users/tinevez/Desktop/Data/RECEPTOR.xml");
		} else {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
		}
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = reader.getModel();
		System.out.println(model);
		
		TrackMate trackmate = new TrackMate(model, new Settings());
		
		// Export
		ExportStatsToIJAction exporter = new ExportStatsToIJAction(trackmate, null);
		exporter.execute();
		
	}

}
