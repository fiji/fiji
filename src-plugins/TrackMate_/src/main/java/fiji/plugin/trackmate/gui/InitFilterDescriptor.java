package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.Collection;
import java.util.Map;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;

public class InitFilterDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "InitialThresholding";
	private TrackMateWizard wizard;
	private InitFilterPanel component;
	private TrackMate_ plugin;
	private Map<String, double[]> features;
	
	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
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
		TrackMateModel model = plugin.getModel();
		features = model.getFeatureModel().getSpotFeatureValues();
		component = new InitFilterPanel(features);
		Double initialFilterValue = model.getSettings().initialSpotFilterValue;
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
		
		final TrackMateModel model = plugin.getModel();
		FeatureFilter initialThreshold = component.getFeatureThreshold();
		String str = "Initial thresholding with a quality threshold above "+ String.format("%.1f", initialThreshold.value) + " ...\n";
		Logger logger = wizard.getLogger();
		logger.log(str,Logger.BLUE_COLOR);
		int ntotal = 0;
		for (Collection<Spot> spots : model.getSpots().values())
			ntotal += spots.size();
		model.getSettings().initialSpotFilterValue = initialThreshold.value;
		plugin.execInitialSpotFiltering();
		int nselected = 0;
		for (Collection<Spot> spots : model.getSpots().values())
			nselected += spots.size();
		logger.log(String.format("Retained %d spots out of %d.\n", nselected, ntotal));
	}
}
