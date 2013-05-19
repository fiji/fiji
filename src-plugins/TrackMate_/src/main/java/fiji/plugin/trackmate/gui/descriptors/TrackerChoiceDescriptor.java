package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TrackerChoiceDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ChooseTracker";
	private final ListChooserPanel component;
	private final TrackMate trackmate;
	private final TrackerProvider trackerProvider;
	
	public TrackerChoiceDescriptor(TrackerProvider trackerProvider, TrackMate trackmate) {
		this.trackmate = trackmate;
		this.trackerProvider = trackerProvider;
		List<String> trackerNames = trackerProvider.getNames();
		List<String> infoTexts = trackerProvider.getInfoTexts();
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
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		
		// Configure the detector provider with choice made in panel
		int index = component.getChoice();
		String key = trackerProvider.getKeys().get(index);
		boolean ok = trackerProvider.select(key);
		
		// Check
		if (!ok) {
			Logger logger = trackmate.getModel().getLogger();
			logger.error("Choice panel returned a tracker unkown to this trackmate:.\n" +
					trackerProvider.getErrorMessage()+
					"Using default "+trackerProvider.getCurrentKey());
		}
		
		trackmate.getSettings().tracker = trackerProvider.getTracker();
	}

	private void setCurrentChoiceFromPlugin() {
		
		String key;
		if (null != trackmate.getSettings().tracker) {
			key = trackmate.getSettings().tracker.getKey();
		} else {
			key = trackerProvider.getCurrentKey(); // back to default 
		}
		int index = trackerProvider.getKeys().indexOf(key);
		
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
