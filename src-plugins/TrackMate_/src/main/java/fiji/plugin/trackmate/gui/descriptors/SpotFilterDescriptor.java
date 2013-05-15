package fiji.plugin.trackmate.gui.descriptors;

import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.panels.components.FilterGuiPanel;
import fiji.plugin.trackmate.util.TMUtils;

public class SpotFilterDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "SpotFilter";
	private final FilterGuiPanel component;
	private final TrackMate trackmate;
	
	
	public SpotFilterDescriptor(TrackMate trackmate) {
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
		Settings settings = trackmate.getSettings();
		Map<String, double[]> values = TMUtils.getSpotFeatureValues(model.getSpots(), settings.getSpotFeatures(), model.getLogger());
		component.setTarget(settings.getSpotFeatures(), settings.getSpotFilters(),  
				settings.getSpotFeatureNames(), values , "spots");
	}

	@Override
	public void displayingPanel() {
		trackmate.getSettings().setSpotFilters(component.getFeatureFilters());
		trackmate.execSpotFiltering(false);
	}
	
	@Override
	public void aboutToHidePanel() {
		Logger logger = trackmate.getModel().getLogger();
		logger.log("Performing spot filtering on the following features:\n", Logger.BLUE_COLOR);
		final TrackMateModel model = trackmate.getModel();
		List<FeatureFilter> featureFilters = component.getFeatureFilters();
		trackmate.getSettings().setSpotFilters(featureFilters);
		trackmate.execSpotFiltering(false);

		int ntotal = model.getSpots().getNSpots(false);
		if (featureFilters == null || featureFilters.isEmpty()) {
			logger.log("No feature threshold set, kept the " + ntotal + " spots.\n");
		} else {
			for (FeatureFilter ft : featureFilters) {
				String str = "  - on "+trackmate.getSettings().getSpotFeatureNames().get(ft.feature);
				if (ft.isAbove) 
					str += " above ";
				else
					str += " below ";
				str += String.format("%.1f", ft.value);
				str += '\n';
				logger.log(str);
			}
			int nselected = model.getSpots().getNSpots(true);
			logger.log("Kept "+nselected+" spots out of " + ntotal + ".\n");
		}		
	}

	@Override
	public String getKey() {
		return KEY;
	}

}