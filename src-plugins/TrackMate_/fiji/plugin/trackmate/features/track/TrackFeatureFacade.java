package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.TrackMateModel;

public class TrackFeatureFacade {

	private List<TrackFeatureAnalyzer> analyzers;
	private TrackDurationAnalyzer durationAnalyzer;
	private TrackBranchingAnalyzer branchingAnalyzer;
	private TrackSpeedStatisticsAnalyzer speedAnalyzer;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public TrackFeatureFacade() {
		initFeatureAnalyzers();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void processAllFeatures(TrackMateModel model) {
		for (TrackFeatureAnalyzer analyzer : analyzers)
			analyzer.process(model);
	}
	
	/*
	 * PRIVATE METHODS
	 */

	private void initFeatureAnalyzers() {
		this.analyzers = new ArrayList<TrackFeatureAnalyzer>(2);
		this.branchingAnalyzer = new TrackBranchingAnalyzer();
		this.durationAnalyzer = new TrackDurationAnalyzer();
		this.speedAnalyzer = new TrackSpeedStatisticsAnalyzer();
		
		analyzers.add(branchingAnalyzer);
		analyzers.add(durationAnalyzer);
		analyzers.add(speedAnalyzer);
	}


}
