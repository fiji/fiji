package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.InitFilterPanel;

public class InitFilterDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "InitialFiltering";
	private final InitFilterPanel component;
	private final TrackMate trackmate;
	private final TrackMateGUIController controller;


	public InitFilterDescriptor(final TrackMate trackmate, final TrackMateGUIController controller) {
		this.trackmate = trackmate;
		this.controller = controller;
		this.component = new InitFilterPanel();
	}


	@Override
	public InitFilterPanel getComponent() {
		return component;
	}

	@Override
	public void aboutToDisplayPanel() {

		final SpotCollection spots = trackmate.getModel().getSpots();

		final double[] values = new double[spots.getNSpots(false)];
		int index = 0;
		for (final Spot spot : spots.iterable(false)) {
			values[index++] = spot.getFeature(Spot.QUALITY);
		}
		component.setValues(values);

		final Double initialFilterValue = trackmate.getSettings().initialSpotFilterValue;
		component.setInitialFilterValue(initialFilterValue);
	}

	@Override
	public void displayingPanel() {
		controller.getGUI().setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {

		final Model model = trackmate.getModel();
		final Logger logger = model.getLogger();
		final FeatureFilter initialThreshold = component.getFeatureThreshold();
		final String str = "Initial thresholding with a quality threshold above "+ String.format("%.1f", initialThreshold.value) + " ...\n";
		logger.log(str,Logger.BLUE_COLOR);
		final int ntotal = model.getSpots().getNSpots(false);
		trackmate.getSettings().initialSpotFilterValue = initialThreshold.value;
		trackmate.execInitialSpotFiltering();
		final int nselected = model.getSpots().getNSpots(false);
		logger.log(String.format("Retained %d spots out of %d.\n", nselected, ntotal));

		/*
		 * We have some spots so we need to compute spot features will we render them.
		 */
		logger.log("Calculating spot features...\n",Logger.BLUE_COLOR);
		// Calculate features
		final long start = System.currentTimeMillis();
		trackmate.computeSpotFeatures(true);
		final long end  = System.currentTimeMillis();
		logger.log(String.format("Calculating features done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
	}

	@Override
	public String getKey() {
		return KEY;
	}

}

