package fiji.plugin.trackmate.tests;

import java.io.File;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;

public class TrackBranchingAnalyzer_Test {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {

		// Load
		File file = new File("/Users/tinevez/Desktop/Data/RECEPTOR.xml");
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();
		TmXmlReader<T> reader = new TmXmlReader<T>(file, plugin);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		System.out.println(plugin.getModel());
		
		// Analyze
		TrackBranchingAnalyzer<T> analyzer = new TrackBranchingAnalyzer<T>(plugin.getModel());
		analyzer.process(plugin.getModel().getTrackIDs());
		System.out.println("Analysis done in " + analyzer.getProcessingTime() + " ms.");
		
	}

}
