package fiji.plugin.trackmate.gui;

import java.util.List;

import net.imglib2.img.ImgPlus;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;
import fiji.plugin.trackmate.util.TMUtils;

public class ManualTrackingGUIController extends TrackMateGUIController {

	public ManualTrackingGUIController(final TrackMate trackmate) {
		super(trackmate);
	}

	@Override
	protected WizardPanelDescriptor getFirstDescriptor() {
		return configureViewsDescriptor;
	}


	@Override
	protected WizardPanelDescriptor previousDescriptor(final WizardPanelDescriptor currentDescriptor) {
		if (currentDescriptor == configureViewsDescriptor) {
			return null;
		} else {
			return super.previousDescriptor(currentDescriptor);
		}
	}

	@Override
	protected void createProviders() {
		super.createProviders();

		trackmate.getModel().setLogger(logger);

		/*
		 * Immediately declare feature analyzers to settings object
		 */

		final Settings settings = trackmate.getSettings();

		final ImgPlus<?> img = TMUtils.rawWraps(settings.imp);
		settings.clearSpotAnalyzerFactories();
		final List<String> spotAnalyzerKeys = spotAnalyzerProvider.getAvailableSpotFeatureAnalyzers();
		for (final String key : spotAnalyzerKeys) {
			final SpotAnalyzerFactory<?> spotFeatureAnalyzer = spotAnalyzerProvider.getSpotFeatureAnalyzer(key, img);
			settings.addSpotAnalyzerFactory(spotFeatureAnalyzer);
		}

		settings.clearEdgeAnalyzers();
		final List<String> edgeAnalyzerKeys = edgeAnalyzerProvider.getAvailableEdgeFeatureAnalyzers();
		for (final String key : edgeAnalyzerKeys) {
			final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getEdgeFeatureAnalyzer(key);
			settings.addEdgeAnalyzer(edgeAnalyzer);
		}

		settings.clearTrackAnalyzers();
		final List<String> trackAnalyzerKeys = trackAnalyzerProvider.getAvailableTrackFeatureAnalyzers();
		for (final String key : trackAnalyzerKeys) {
			final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getTrackFeatureAnalyzer(key);
			settings.addTrackAnalyzer(trackAnalyzer);
		}

		trackmate.getModel().getLogger().log(settings.toStringFeatureAnalyzersInfo());

		/*
		 * Immediately declare features to model.
		 */

		trackmate.computeSpotFeatures(false);
		trackmate.computeEdgeFeatures(false);
		trackmate.computeTrackFeatures(false);
	}

}
