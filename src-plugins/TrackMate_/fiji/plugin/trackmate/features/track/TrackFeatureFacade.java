package fiji.plugin.trackmate.features.track;

import java.util.List;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackCollection;

public class TrackFeatureFacade {

	private Settings settings;
	private List<TrackFeatureAnalyzer> analyzers;
	private TrackDurationAnalyzer durationAnalyzer;
	private TrackBranchingAnalyzer branchingAnalyzer;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public TrackFeatureFacade(final Settings settings) {
		this.settings = settings;
		initFeatureAnalyzers();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void processAllFeatures(TrackCollection tracks) {
		for (TrackFeatureAnalyzer analyzer : analyzers)
			analyzer.process(tracks);
	}
	
	/*
	 * PRIVATE METHODS
	 */

	private void initFeatureAnalyzers() {
		this.branchingAnalyzer = new TrackBranchingAnalyzer(settings.dt);
		this.durationAnalyzer = new TrackDurationAnalyzer();
		
		analyzers.add(branchingAnalyzer);
		analyzers.add(durationAnalyzer);
	}


}
