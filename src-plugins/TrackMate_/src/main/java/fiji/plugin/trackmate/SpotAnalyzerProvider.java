package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.img.ImgPlus;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
public class SpotAnalyzerProvider {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant spot analyzer classes.  */
	protected List<String> analyzerNames;
	protected final TrackMateModel model;
	protected final ImgPlus<?> img;

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
	public SpotAnalyzerProvider(TrackMateModel model, ImgPlus<?> img) {
		this.model = model;
		this.img = img;
		registerSpotFeatureAnalyzers();
	}


	/*
	 * METHODS
	 */

	/**
	 * Registers the standard spotFeatureAnalyzers shipped with TrackMate.
	 */
	protected void registerSpotFeatureAnalyzers() {
		analyzerNames = new ArrayList<String>(3);
		analyzerNames.add(SpotIntensityAnalyzerFactory.KEY);
		analyzerNames.add(SpotContrastAndSNRAnalyzerFactory.KEY); // must be after the statistics one
		analyzerNames.add(SpotRadiusEstimatorFactory.KEY);
	}

	/**
	 * Returns a new instance of the target spotFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this provider, <code>null</code> is returned. 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SpotAnalyzerFactory getSpotFeatureAnalyzer(String key) {
		if (key == SpotIntensityAnalyzerFactory.KEY) {
			return new SpotIntensityAnalyzerFactory(model, img);
		} else if (key == SpotContrastAndSNRAnalyzerFactory.KEY) {
			return new SpotContrastAndSNRAnalyzerFactory(model, img);
		} else if (key == SpotRadiusEstimatorFactory.KEY) {
			return new SpotRadiusEstimatorFactory(model, img);
		} else {
			return null;
		}
	}

	/**
	 * Returns a list of the {@link SpotAnalyzer} names available through this provider.
	 */
	public List<String> getAvailableSpotFeatureAnalyzers() {
		return analyzerNames;
	}

}
