package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.IJ;
import ij.ImageJ;

import java.io.File;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ExportStats_TestDrive {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {
		
		ImageJ.main(args);
		
		File file;
		if (!IJ.isWindows()) {
//			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
			file = new File("/Users/tinevez/Desktop/Data/RECEPTOR.xml");
		} else {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
		}
		ij.ImageJ.main(args);
		
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();
		TmXmlReader<T> reader = new TmXmlReader<T>(file, plugin);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		System.out.println(plugin.getModel());
		
		// Export
		ExportStatsToIJAction<T> exporter = new ExportStatsToIJAction<T>();
		exporter.execute(plugin);
		
	}

}
