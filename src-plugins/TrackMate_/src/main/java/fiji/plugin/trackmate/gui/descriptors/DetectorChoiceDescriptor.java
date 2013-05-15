package fiji.plugin.trackmate.gui.descriptors;

import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.DetectorProvider;

public class DetectorChoiceDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ChooseDetector";
	private ListChooserPanel component;
	private final TrackMate trackmate;
	private final DetectorProvider detectorProvider;

	
	public DetectorChoiceDescriptor(DetectorProvider detectorProvider, TrackMate trackmate) {
		this.trackmate = trackmate;
		this.detectorProvider = detectorProvider;
		List<String> detectorNames =  detectorProvider.getNames();
		List<String> infoTexts = detectorProvider.getInfoTexts();
		this.component = new ListChooserPanel(detectorNames, infoTexts, "detector");
		setCurrentChoiceFromPlugin();
	}
	
	/*
	 * METHODS
	 */

	@Override
	public ListChooserPanel getComponent() {
		return component;
	}


	@Override
	public void aboutToDisplayPanel() {
		setCurrentChoiceFromPlugin();
	}
	
	private void setCurrentChoiceFromPlugin() {
		String key;
		if (null != trackmate.getSettings().detectorFactory) {
			key = trackmate.getSettings().detectorFactory.getKey();
		} else {
			key = detectorProvider.getCurrentKey(); // back to default 
		}
		int index = detectorProvider.getKeys().indexOf(key);
		if (index < 0) {
			trackmate.getModel().getLogger().error("[DetectorChoiceDescriptor] Cannot find detector named "+key+" in current trackmate.");
			return;
		}
		component.setChoice(index);
	}

	@Override
	public void displayingPanel() { 
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void aboutToHidePanel() {
		
		// Configure the detector provider with choice made in panel
		int index = component.getChoice();
		String key = detectorProvider.getKeys().get(index);
		detectorProvider.select(key);
		
		// Configure trackmate settings with selected detector
		trackmate.getSettings().detectorFactory = detectorProvider.getDetectorFactory();
		
		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		Map<String, Object> currentSettings = trackmate.getSettings().detectorSettings;
		if (!detectorProvider.checkSettingsValidity(currentSettings)) {
			Map<String, Object> defaultSettings = detectorProvider.getDefaultSettings();
			trackmate.getSettings().detectorSettings = defaultSettings;
		}
	}
	
	@Override
	public String getKey() {
		return KEY;
	}


}
