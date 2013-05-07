package fiji.plugin.trackmate.tests;

import java.io.File;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v12;

public class TmXmlReaderV12TestDrive {

	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks_v12.xml");
//	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks_v1.2.xml");

	public static void main(String args[]) {

		//		ij.ImageJ.main(args);

		System.out.println("Opening file: "+file.getAbsolutePath());		
		TrackMate trackmate = new TrackMate();
		TmXmlReader reader = new TmXmlReader_v12(file, trackmate);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
			return;
		}
		TrackMateModel model = trackmate.getModel();

		System.out.println(model);

	}


}
