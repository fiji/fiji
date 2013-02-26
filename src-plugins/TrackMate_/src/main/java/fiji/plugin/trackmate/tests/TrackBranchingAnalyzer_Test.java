package fiji.plugin.trackmate.tests;

import java.io.File;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;

public class TrackBranchingAnalyzer_Test {

	public static void main(String[] args) {

		// Load
		File file = new File("/Users/tinevez/Desktop/Data/RECEPTOR.xml");
		TrackMate_ plugin = new TrackMate_();
		plugin.initModules();
		TmXmlReader reader = new TmXmlReader(file, plugin);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		System.out.println(plugin.getModel());
		
		// Analyze
		TrackBranchingAnalyzer analyzer = new TrackBranchingAnalyzer(plugin.getModel());
		analyzer.process(plugin.getModel().getTrackModel().getTrackIDs());
		System.out.println("Analysis done in " + analyzer.getProcessingTime() + " ms.");
		
	}

}
