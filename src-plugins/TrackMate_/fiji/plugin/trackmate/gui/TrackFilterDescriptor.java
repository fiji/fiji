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

public class TrackFilterDescriptor implements WizardPanelDescriptor {
	public static final String DESCRIPTOR = "TrackFilter";
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
	public String getNextDescriptorID() {
		return DisplayerPanel.DESCRIPTOR;
	}
	
	@Override
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return TrackerConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		TrackMateModel model = plugin.getModel();
		component.setTarget(model.getFeatureModel().getTrackFeatures(), model.getTrackFilters(),  
				model.getFeatureModel().getTrackFeatureNames(), model.getFeatureModel().getTrackFeatureValues(), "tracks"); 
	}

	@Override
	public void displayingPanel() {

		// Link displayer and component
		final TrackMateModelView displayer = wizard.getDisplayer();
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {

				component.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_COLOR_FEATURE, component.getColorByFeature());
						displayer.refresh();
					}
				});

				component.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent event) {
						// We set the thresholds field of the model but do not touch its selected spot field yet.
						plugin.getModel().setTrackFilters(component.getFeatureFilters());
						plugin.execTrackFiltering();
					}
				});
			}
		});
	}

	@Override
	public void aboutToHidePanel() {
		final Logger logger = wizard.getLogger();
		logger.log("Performing track filtering on the following features:\n", Logger.BLUE_COLOR);
		List<FeatureFilter> featureFilters = component.getFeatureFilters();
		final TrackMateModel model = plugin.getModel();
		model.setTrackFilters(featureFilters);
		plugin.execTrackFiltering();

		if (featureFilters == null || featureFilters.isEmpty()) {
			logger.log("No feature threshold set, kept the " + model.getNTracks() + " tracks.\n");
		} else {
			for (FeatureFilter ft : featureFilters) {
				String str = "  - on "+model.getFeatureModel().getTrackFeatureNames().get(ft.feature);
				if (ft.isAbove) 
					str += " above ";
				else
					str += " below ";
				str += String.format("%.1f", ft.value);
				str += '\n';
				logger.log(str);
			}
			logger.log("Kept "+model.getNFilteredTracks()+" tracks out of "+model.getNTracks()+".\n");
		}		
	}
}
