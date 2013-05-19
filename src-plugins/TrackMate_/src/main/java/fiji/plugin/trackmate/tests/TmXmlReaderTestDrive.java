package fiji.plugin.trackmate.tests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;

public class TmXmlReaderTestDrive {

	public static void main(String args[]) {

		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = reader.getModel();
		System.out.println(model);
		
	}


}
