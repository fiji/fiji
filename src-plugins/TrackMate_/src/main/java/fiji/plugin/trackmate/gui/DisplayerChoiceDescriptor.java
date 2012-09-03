package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class DisplayerChoiceDescriptor<T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "DisplayerChoice";
	private TrackMate_<T> plugin;
	private ListChooserPanel component;
	private TrackMateWizard<T> wizard;
	
	/*
	 * METHODS
	 */
	
	@Override
	public void setWizard(TrackMateWizard<T> wizard) {
		this.wizard = wizard;
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
		return LaunchDisplayerDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		if (plugin.getModel().getSettings().detectorFactory.equals(ManualDetectorFactory.NAME)) {
			return DetectorConfigurationPanelDescriptor.DESCRIPTOR;
		} else {
			return InitFilterDescriptor.DESCRIPTOR;
		}
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() {
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		String displayerName = component.getChoice();
		TrackMateModelView<T> displayer = plugin.getViewFactory().getView(displayerName);
		wizard.setDisplayer(displayer);
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
		this.plugin = plugin;
		List<String> viewerNames = plugin.getViewFactory().getAvailableViews();
		List<String> infoTexts = new ArrayList<String>(viewerNames.size());
		for(String key : viewerNames) {
			infoTexts.add(plugin.getViewFactory().getInfoText(key));
		}
		this.component = new ListChooserPanel(viewerNames, infoTexts, "view");
	}


}
