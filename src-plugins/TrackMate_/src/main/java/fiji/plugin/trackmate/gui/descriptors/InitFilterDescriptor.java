package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.panels.InitFilterPanel;

public class InitFilterDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "InitialFiltering";
	private final InitFilterPanel component;
	private final TrackMate trackmate;
	
	
	public InitFilterDescriptor(TrackMate trackmate) {
		this.trackmate = trackmate;
		this.component = new InitFilterPanel();
	}
	
	
	@Override
	public InitFilterPanel getComponent() {
		return component;
	}

	@Override
	public void aboutToDisplayPanel() {
		
		SpotCollection spots = trackmate.getModel().getSpots();
		
		double[] values = new double[spots.getNSpots(false)];
		int index = 0;
		for (Spot spot : spots.iterable(false)) {
			values[index++] = spot.getFeature(Spot.QUALITY);
		}
		component.setValues(values);
		
		Double initialFilterValue = trackmate.getSettings().initialSpotFilterValue;
		component.setInitialFilterValue(initialFilterValue);
	}

	@Override
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		
		final TrackMateModel model = trackmate.getModel();
		Logger logger = model.getLogger();
		FeatureFilter initialThreshold = component.getFeatureThreshold();
		String str = "Initial thresholding with a quality threshold above "+ String.format("%.1f", initialThreshold.value) + " ...\n";
		logger.log(str,Logger.BLUE_COLOR);
		int ntotal = model.getSpots().getNSpots(false);
		trackmate.getSettings().initialSpotFilterValue = initialThreshold.value;
		trackmate.execInitialSpotFiltering();
		int nselected = model.getSpots().getNSpots(false);
		logger.log(String.format("Retained %d spots out of %d.\n", nselected, ntotal));
		
		/*
		 * We have some spots so we need to compute spot features will we render them.
		 */
		logger.log("Calculating spot features...\n",Logger.BLUE_COLOR);
		// Calculate features
		long start = System.currentTimeMillis();
		trackmate.computeSpotFeatures(true);		
		long end  = System.currentTimeMillis();
		logger.log(String.format("Calculating features done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
	}
	
	@Override
	public String getKey() {
		return KEY;
	}

}

