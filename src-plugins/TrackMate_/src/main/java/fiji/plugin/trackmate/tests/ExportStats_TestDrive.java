package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.TrackMate;
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
//			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
			file = new File("/Users/tinevez/Desktop/Data/RECEPTOR.xml");
		} else {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
		}
		ij.ImageJ.main(args);
		
		TrackMate trackmate = new TrackMate();
		TmXmlReader reader = new TmXmlReader(file, trackmate);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		System.out.println(trackmate.getModel());
		
		// Export
		ExportStatsToIJAction exporter = new ExportStatsToIJAction();
		exporter.execute(trackmate);
		
	}

}
