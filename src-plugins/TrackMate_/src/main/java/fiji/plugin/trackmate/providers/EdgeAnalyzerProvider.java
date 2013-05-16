package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;

/**
 * A provider for the edge analyzers provided in the GUI.
 */
public class EdgeAnalyzerProvider {


	protected final TrackMateModel model;
	protected List<String> names;
	
	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model spotFeatureAnalyzers currently available in the 
	 * TrackMate trackmate. Each spotFeatureAnalyzer is identified by a key String, which can be used 
	 * to retrieve new instance of the spotFeatureAnalyzer.
	 * <p>
	 * If you want to add custom spotFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom spotFeatureAnalyzers and provide this 
	 * extended factory to the {@link TrackMate} trackmate.
	 */
	public EdgeAnalyzerProvider(TrackMateModel model) {
		this.model = model;
		registerEdgeFeatureAnalyzers();
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard spotFeatureAnalyzers shipped with TrackMate.
	 */
	protected void registerEdgeFeatureAnalyzers() {
		// Names
		names = new ArrayList<String>(3);
		names.add(EdgeVelocityAnalyzer.KEY);
		names.add(EdgeTimeLocationAnalyzer.KEY);
		names.add(EdgeTargetAnalyzer.KEY);
	}

	/**
	 * Returns a new instance of the target edgeFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public EdgeAnalyzer getEdgeFeatureAnalyzer(String key) {
		
		if (key.equals(EdgeTargetAnalyzer.KEY)) {
			return new EdgeTargetAnalyzer(model);
			
		} else if (key.equals(EdgeVelocityAnalyzer.KEY)) {
			return new EdgeVelocityAnalyzer(model);
			
		} else if (key.equals(EdgeTimeLocationAnalyzer.KEY)) {
			return new EdgeTimeLocationAnalyzer(model);
			
		} else {
			return null;
			
		}
	}

	/**
	 * Returns a list of the EdgeFeatureAnalyzer names available through this provider.
	 */
	public List<String> getAvailableEdgeFeatureAnalyzers() {
		return names;
	}

}
