package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class SpotFilterDescriptor implements WizardPanelDescriptor {

	private static final boolean DEBUG = false;
	public static final String DESCRIPTOR = "SpotFilter";
	private TrackMateWizard wizard;
	private FilterGuiPanel component = new FilterGuiPanel();
	private TrackMate_ plugin;
	
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
		return TrackerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return DisplayerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		TrackMateModel model = plugin.getModel();
		component.setTarget(model.getFeatureModel().getSpotFeatures(), model.getSettings().getSpotFilters(),  
				model.getFeatureModel().getSpotFeatureNames(), model.getFeatureModel().getSpotFeatureValues(), "spots");
		linkGuiToView();
	}

	@Override
	public void displayingPanel() {
		plugin.getModel().getSettings().setSpotFilters(component.getFeatureFilters());
		plugin.execSpotFiltering(false);
		wizard.getDisplayer().refresh();
	}
	
	public void linkGuiToView() {
		
		if (DEBUG) {
			System.out.println("[SpotFilterDescriptor] calling #linkGuiToView().");
		}
		
		 // Link displayer and component
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {

				component.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						
						wizard.getDisplayer().setDisplaySettings(TrackMateModelView.KEY_SPOT_COLOR_FEATURE, component.getColorByFeature());
						wizard.getDisplayer().refresh();
					}
				});

				component.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent event) {
						if (DEBUG) {
							System.out.println("[SpotFilterDescriptor] stateChanged caught.");
						}
						// We set the thresholds field of the model but do not touch its selected spot field yet.
						plugin.getModel().getSettings().setSpotFilters(component.getFeatureFilters());
						plugin.execSpotFiltering(false);
						wizard.getDisplayer().refresh();
					}
				});

				wizard.setNextButtonEnabled(true);
			}
		});
	}

	@Override
	public void aboutToHidePanel() {
		Logger logger = wizard.getLogger();
		logger.log("Performing spot filtering on the following features:\n", Logger.BLUE_COLOR);
		final TrackMateModel model = plugin.getModel();
		List<FeatureFilter> featureFilters = component.getFeatureFilters();
		model.getSettings().setSpotFilters(featureFilters);
		plugin.execSpotFiltering(false);

		int ntotal = model.getSpots().getNSpots();
		if (featureFilters == null || featureFilters.isEmpty()) {
			logger.log("No feature threshold set, kept the " + ntotal + " spots.\n");
		} else {
			for (FeatureFilter ft : featureFilters) {
				String str = "  - on "+model.getFeatureModel().getSpotFeatureNames().get(ft.feature);
				if (ft.isAbove) 
					str += " above ";
				else
					str += " below ";
				str += String.format("%.1f", ft.value);
				str += '\n';
				logger.log(str);
			}
			int nselected = model.getFilteredSpots().getNSpots();
			logger.log("Kept "+nselected+" spots out of " + ntotal + ".\n");
		}		
	}
}
