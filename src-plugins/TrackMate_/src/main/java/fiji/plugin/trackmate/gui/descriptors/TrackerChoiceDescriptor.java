package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.ManualTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;

public class TrackerChoiceDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ChooseTracker";
	private final ListChooserPanel component;
	private final TrackMate trackmate;
	private final TrackerProvider trackerProvider;
	private final TrackMateGUIController controller;

	public TrackerChoiceDescriptor(final TrackerProvider trackerProvider, final TrackMate trackmate, final TrackMateGUIController controller) {
		this.trackmate = trackmate;
		this.trackerProvider = trackerProvider;
		this.controller = controller;
		final List<String> trackerNames = trackerProvider.getNames();
		final List<String> infoTexts = trackerProvider.getInfoTexts();
		this.component = new ListChooserPanel(trackerNames, infoTexts, "tracker");
		setCurrentChoiceFromPlugin();
	}

	/*
	 * METHODS
	 */

	@Override
	public Component getComponent() {
		return component;
	}

	@Override
	public void aboutToDisplayPanel() {
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void displayingPanel() {
		controller.getGUI().setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {

		// Configure the detector provider with choice made in panel
		final int index = component.getChoice();
		final String key = trackerProvider.getKeys().get(index);
		final boolean ok = trackerProvider.select(key);

		// Check
		if (!ok) {
			final Logger logger = trackmate.getModel().getLogger();
			logger.error("Choice panel returned a tracker unkown to this trackmate:.\n" +
					trackerProvider.getErrorMessage()+
					"Using default "+trackerProvider.getCurrentKey());
		}

		final SpotTracker tracker = trackerProvider.getTracker();
		trackmate.getSettings().tracker = tracker;

		if (tracker.getKey().equals(ManualTracker.TRACKER_KEY)) {
			/*
			 * Compute track and edge features now to ensure they will be available
			 * in the next descriptor.
			 */
			final Thread trackFeatureCalculationThread = new Thread("TrackMate track feature calculation thread") {
				@Override
				public void run() {
					trackmate.computeTrackFeatures(true);
					trackmate.computeEdgeFeatures(true);
				}
			};
			trackFeatureCalculationThread.start();
			try {
				trackFeatureCalculationThread.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void setCurrentChoiceFromPlugin() {

		String key;
		if (null != trackmate.getSettings().tracker) {
			key = trackmate.getSettings().tracker.getKey();
		} else {
			key = trackerProvider.getCurrentKey(); // back to default
		}
		final int index = trackerProvider.getKeys().indexOf(key);

		if (index < 0) {
			trackmate.getModel().getLogger().error("[TrackerChoiceDescriptor] Cannot find tracker named "+key+" in current trackmate.");
			return;
		}
		component.setChoice(index);
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
