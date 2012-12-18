package fiji.plugin.trackmate.tests;

import java.io.File;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v12;

public class TmXmlReaderV12TestDrive {

	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks_v12.xml");

	public static <T extends RealType<T> & NativeType<T>> void main(String args[]) {

		//		ij.ImageJ.main(args);

		System.out.println("Opening file: "+file.getAbsolutePath());		
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();
		TmXmlReader<T> reader = new TmXmlReader_v12<T>(file, plugin);
		if (!reader.checkInput() && !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		TrackMateModel<T> model = plugin.getModel();

		System.out.println(model);

	}


}
