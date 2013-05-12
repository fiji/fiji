package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;

/**
 * A provider for the track analyzers provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute numerically, 
 * using the method {@link #getFeaturesForKey(String)}. 
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 */
public class TrackAnalyzerProvider {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant track analyzer classes.  */
	protected List<String> names;
	/** The target model to operate on. */
	protected final TrackMateModel model;
	/** The {@link TrackIndexAnalyzer} is the only analyzer we do not re-instantiate 
	 * at every {@link #getTrackFeatureAnalyzer(String)} call, for it has an internal state 
	 * useful for lazy computation of track features. */
	protected final TrackIndexAnalyzer trackIndexAnalyzer;

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model trackFeatureAnalyzers currently available in the 
	 * TrackMate trackmate. Each trackFeatureAnalyzer is identified by a key String, which can be used 
	 * to retrieve new instance of the trackFeatureAnalyzer.
	 * <p>
	 * If you want to add custom trackFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom trackFeatureAnalyzers and provide this 
	 * extended factory to the {@link TrackMate} trackmate.
	 */
	public TrackAnalyzerProvider(TrackMateModel model) {
		this.model = model;
		registerTrackFeatureAnalyzers();
		this.trackIndexAnalyzer = new TrackIndexAnalyzer(model);
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard trackFeatureAnalyzers shipped with TrackMate.
	 */
	protected void registerTrackFeatureAnalyzers() {
		// Names
		names = new ArrayList<String>(4);
		names.add(TrackBranchingAnalyzer.KEY);
		names.add(TrackDurationAnalyzer.KEY);
		names.add(TrackSpeedStatisticsAnalyzer.KEY);
		names.add(TrackLocationAnalyzer.KEY);
		names.add(TrackIndexAnalyzer.KEY);
	}

	/**
	 * @return a new instance of the target trackFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public TrackAnalyzer getTrackFeatureAnalyzer(String key) {
		if (key == TrackDurationAnalyzer.KEY) {
			return new TrackDurationAnalyzer(model);
		} else if (key == TrackBranchingAnalyzer.KEY) {
			return new TrackBranchingAnalyzer(model);
		} else if (key == TrackSpeedStatisticsAnalyzer.KEY) {
			return new TrackSpeedStatisticsAnalyzer(model);
		} else if (key == TrackLocationAnalyzer.KEY) {
			return new TrackLocationAnalyzer(model);
		} else if (key == TrackIndexAnalyzer.KEY) {
			return trackIndexAnalyzer;
		} else {
			return null;
		}
	}

	/**
	 * @return a list of the trackFeatureAnalyzer names available through this provider.
	 */
	public List<String> getAvailableTrackFeatureAnalyzers() {
		return names;
	}

}
