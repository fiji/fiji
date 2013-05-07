package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class SpotFilterDescriptor implements WizardPanelDescriptor {

	private static final boolean DEBUG = false;
	public static final String DESCRIPTOR = "SpotFilter";
	private TrackMateWizard wizard;
	private FilterGuiPanel component = new FilterGuiPanel();
	private TrackMate trackmate;
	
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
		return TrackerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return DisplayerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		TrackMateModel model = trackmate.getModel();
		Settings settings = trackmate.getSettings();
		Map<String, double[]> values = TMUtils.getSpotFeatureValues(model.getSpots(), settings.getSpotFeatures(), model.getLogger());
		component.setTarget(settings.getSpotFeatures(), settings.getSpotFilters(),  
				settings.getSpotFeatureNames(), values , "spots");
		linkGuiToView();
	}

	@Override
	public void displayingPanel() {
		trackmate.getSettings().setSpotFilters(component.getFeatureFilters());
		trackmate.execSpotFiltering(false);
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
						trackmate.getSettings().setSpotFilters(component.getFeatureFilters());
						trackmate.execSpotFiltering(false);
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
		final TrackMateModel model = trackmate.getModel();
		List<FeatureFilter> featureFilters = component.getFeatureFilters();
		trackmate.getSettings().setSpotFilters(featureFilters);
		trackmate.execSpotFiltering(false);

		int ntotal = model.getSpots().getNSpots(false);
		if (featureFilters == null || featureFilters.isEmpty()) {
			logger.log("No feature threshold set, kept the " + ntotal + " spots.\n");
		} else {
			for (FeatureFilter ft : featureFilters) {
				String str = "  - on "+trackmate.getSettings().getSpotFeatureNames().get(ft.feature);
				if (ft.isAbove) 
					str += " above ";
				else
					str += " below ";
				str += String.format("%.1f", ft.value);
				str += '\n';
				logger.log(str);
			}
			int nselected = model.getSpots().getNSpots(true);
			logger.log("Kept "+nselected+" spots out of " + ntotal + ".\n");
		}		
	}
}
