package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.features.edges.EdgeFeatureAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;

/**
 * A provider for the edge analyzers provided in the GUI.
 */
public class EdgeFeatureAnalyzerProvider <T extends RealType<T> & NativeType<T>> {


	protected final TrackMateModel<T> model;
	private ArrayList<String> names;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model spotFeatureAnalyzers currently available in the 
	 * TrackMate plugin. Each spotFeatureAnalyzer is identified by a key String, which can be used 
	 * to retrieve new instance of the spotFeatureAnalyzer.
	 * <p>
	 * If you want to add custom spotFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom spotFeatureAnalyzers and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public EdgeFeatureAnalyzerProvider(TrackMateModel<T> model) {
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
		names = new ArrayList<String>(1);
		names.add(EdgeTargetAnalyzer.KEY);
	}

	/**
	 * @return a new instance of the target edgeFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public EdgeFeatureAnalyzer getEdgeFeatureAnalyzer(String key) {
		if (key == EdgeTargetAnalyzer.KEY) {
			return new EdgeTargetAnalyzer<T>(model);
		} else {
			return null;
		}
	}

	/**
	 * @return a list of the edgeFeatureAnalyzer names available through this factory.
	 */
	public List<String> getAvailableEdgeFeatureAnalyzers() {
		return names;
	}


}
