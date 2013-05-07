package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackerProvider;

public class TrackerChoiceDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "TrackerChoice";
	private ListChooserPanel component;
	private TrackMate trackmate;
	private TrackMateWizard wizard;
	
	/*
	 * METHODS
	 */
	
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
		return TrackerConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return SpotFilterDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void displayingPanel() { 
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		
		// Configure the detector provider with choice made in panel
		TrackerProvider provider = trackmate.getTrackerProvider();
		int index = component.jComboBoxChoice.getSelectedIndex();
		String key = provider.getKeys().get(index);
		
		/* The next line will setup the TrackerProvider to return everything
		 * linked to the targeted tracker when required. We do not instantiate
		 * the tracker yet, because it would not receive the right settings, that
		 * will be generated in the next GUI step.  
		 * We nonetheless do a basic checking to ensure we received a known tracker. */
		boolean ok = provider.select(key);
		
		// Check
		if (!ok) {
			Logger logger = wizard.getLogger();
			logger.error("Choice panel returned a tracker unkown to this trackmate:.\n" +
					provider.getErrorMessage()+
					"Using default "+provider.getCurrentKey());
		}
		
		// Instantiate next descriptor for the wizard
		TrackerConfigurationPanelDescriptor descriptor = new TrackerConfigurationPanelDescriptor();
		descriptor.setPlugin(trackmate);
		descriptor.setWizard(wizard);
		wizard.registerWizardDescriptor(TrackerConfigurationPanelDescriptor.DESCRIPTOR, descriptor);
	}

	@Override
	public void setPlugin(TrackMate trackmate) {
		this.trackmate = trackmate;
		TrackerProvider provider = trackmate.getTrackerProvider();
		List<String> trackerNames = provider.getNames();
		List<String> infoTexts = provider.getInfoTexts();
		this.component = new ListChooserPanel(trackerNames, infoTexts, "tracker");
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void setWizard(TrackMateWizard wizard) {	
		this.wizard = wizard;
	}

	void setCurrentChoiceFromPlugin() {
		
		String key;
		if (null != trackmate.getSettings().tracker) {
			key = trackmate.getSettings().tracker.getKey();
		} else {
			key = trackmate.getTrackerProvider().getCurrentKey(); // back to default 
		}
		int index = trackmate.getTrackerProvider().getKeys().indexOf(key);
		
		if (index < 0) {
			wizard.getLogger().error("[TrackerChoiceDescriptor] Cannot find tracker named "+key+" in current trackmate.");
			return;
		}
		component.jComboBoxChoice.setSelectedIndex(index);
	}
	
}
