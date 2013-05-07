package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.Map;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.util.TMUtils;

public class InitFilterDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "InitialThresholding";
	private TrackMateWizard wizard;
	private InitFilterPanel component;
	private TrackMate trackmate;
	private Map<String, double[]> features;
	
	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate trackmate) {
		this.trackmate = trackmate;
	}

	@Override
	public Component getComponent() {
		return component;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}
	
	@Override
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return DisplayerChoiceDescriptor.DESCRIPTOR;
	}


	@Override
	public String getPreviousDescriptorID() {
		return DetectorDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		TrackMateModel model = trackmate.getModel();
		Settings settings = trackmate.getSettings();
		features = TMUtils.getSpotFeatureValues(model.getSpots(), settings.getSpotFeatures(), model.getLogger());
		component = new InitFilterPanel(features);
		Double initialFilterValue = trackmate.getSettings().initialSpotFilterValue;
		component.setInitialFilterValue(initialFilterValue);
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void displayingPanel() {
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		
		component.updater.quit();
		
		final TrackMateModel model = trackmate.getModel();
		FeatureFilter initialThreshold = component.getFeatureThreshold();
		String str = "Initial thresholding with a quality threshold above "+ String.format("%.1f", initialThreshold.value) + " ...\n";
		Logger logger = wizard.getLogger();
		logger.log(str,Logger.BLUE_COLOR);
		int ntotal = model.getSpots().getNSpots(false);
		trackmate.getSettings().initialSpotFilterValue = initialThreshold.value;
		trackmate.execInitialSpotFiltering();
		int nselected = model.getSpots().getNSpots(false);
		logger.log(String.format("Retained %d spots out of %d.\n", nselected, ntotal));
	}
}
