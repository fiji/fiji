package fiji.plugin.trackmate.gui.descriptors;

import java.awt.event.ActionListener;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.panels.components.FilterGuiPanel;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;

public class TrackFilterDescriptor implements WizardPanelDescriptor {
	
	private static final String KEY = "FilterTracks";
	private final FilterGuiPanel component;
	private final TrackMate trackmate;
	
	public TrackFilterDescriptor(TrackMate trackmate) {
		this.trackmate = trackmate;
		this.component = new FilterGuiPanel();
	}

	@Override
	public FilterGuiPanel getComponent() {
		return component;
	}

	@Override
	public void aboutToDisplayPanel() {
		TrackMateModel model = trackmate.getModel();
		component.setTarget(model.getFeatureModel().getTrackFeatures(), trackmate.getSettings().getTrackFilters(),  
				model.getFeatureModel().getTrackFeatureNames(), model.getFeatureModel().getTrackFeatureValues(), "tracks");
		component.setColorByFeature(TrackIndexAnalyzer.TRACK_INDEX);
		
		PerTrackFeatureColorGenerator generator = new PerTrackFeatureColorGenerator(model, TrackIndexAnalyzer.TRACK_INDEX);
		generator.setFeature(component.getColorByFeature());
		
		for (ActionListener listener : component.getActionListeners()) {
			listener.actionPerformed(component.COLOR_FEATURE_CHANGED);
		}
	}

	@Override
	public void displayingPanel() {}
	

	@Override
	public void aboutToHidePanel() {
		final Logger logger = trackmate.getModel().getLogger();
		logger.log("Performing track filtering on the following features:\n", Logger.BLUE_COLOR);
		List<FeatureFilter> featureFilters = component.getFeatureFilters();
		final TrackMateModel model = trackmate.getModel();
		trackmate.getSettings().setTrackFilters(featureFilters);
		trackmate.execTrackFiltering(true);

		if (featureFilters == null || featureFilters.isEmpty()) {
			logger.log("No feature threshold set, kept the " + model.getTrackModel().getNTracks() + " tracks.\n");
		} else {
			for (FeatureFilter ft : featureFilters) {
				String str = "  - on "+model.getFeatureModel().getTrackFeatureNames().get(ft.feature);
				if (ft.isAbove) 
					str += " above ";
				else
					str += " below ";
				str += String.format("%.1f", ft.value);
				str += '\n';
				logger.log(str);
			}
			logger.log("Kept "+model.getTrackModel().getNFilteredTracks()+" tracks out of "+model.getTrackModel().getNTracks()+".\n");
		}

		trackmate.computeEdgeFeatures(true);
	}
	
	@Override
	public String getKey() {
		return KEY;
	}
}
