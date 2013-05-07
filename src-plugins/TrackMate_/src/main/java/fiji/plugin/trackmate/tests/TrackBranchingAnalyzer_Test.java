package fiji.plugin.trackmate.tests;

import java.io.File;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;

public class TrackBranchingAnalyzer_Test {

	public static void main(String[] args) {

		// Load
		File file = new File("/Users/tinevez/Desktop/Data/RECEPTOR.xml");
		TrackMate trackmate = new TrackMate();
		TmXmlReader reader = new TmXmlReader(file, trackmate);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		System.out.println(trackmate.getModel());
		
		// Analyze
		TrackBranchingAnalyzer analyzer = new TrackBranchingAnalyzer(trackmate.getModel());
		analyzer.process(trackmate.getModel().getTrackModel().getTrackIDs());
		System.out.println("Analysis done in " + analyzer.getProcessingTime() + " ms.");
		
	}

}
