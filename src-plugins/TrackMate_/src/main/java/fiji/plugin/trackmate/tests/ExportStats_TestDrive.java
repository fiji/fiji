package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;

import java.io.File;

import org.scijava.util.AppUtils;

public class ExportStats_TestDrive {

	public static void main(String[] args) {
		
		ImageJ.main(args);
		
		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = reader.getModel();
		
		model.setLogger(Logger.DEFAULT_LOGGER);
//		System.out.println(model);
//		System.out.println(model.getFeatureModel());
		
		TrackMate trackmate = new TrackMate(model, null);
		
		// Export
		ExportStatsToIJAction exporter = new ExportStatsToIJAction(trackmate, null);
		exporter.execute();
		
	}

}
