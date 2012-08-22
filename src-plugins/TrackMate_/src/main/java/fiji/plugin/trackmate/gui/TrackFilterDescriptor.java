package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class TrackFilterDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {
	
	public static final String DESCRIPTOR = "TrackFilter";
	private TrackMateWizard<T> wizard;
	private FilterGuiPanel component = new FilterGuiPanel();
	private TrackMate_<T> plugin;

	@Override
	public void setWizard(TrackMateWizard<T> wizard) {
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
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
		TrackMateModel<T> model = plugin.getModel();
		component.setTarget(model.getFeatureModel().getTrackFeatures(), model.getSettings().getTrackFilters(),  
				model.getFeatureModel().getTrackFeatureNames(), model.getFeatureModel().getTrackFeatureValues(), "tracks");
		linkGuiToView();
	}

	@Override
	public void displayingPanel() {}
	
	public void linkGuiToView() {

		// Link displayer and component
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {

				component.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						wizard.getDisplayer().setDisplaySettings(TrackMateModelView.KEY_TRACK_COLOR_FEATURE, component.getColorByFeature());
						wizard.getDisplayer().refresh();
					}
				});

				component.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent event) {
						// We set the thresholds field of the model but do not touch its selected spot field yet.
						plugin.getModel().getSettings().setTrackFilters(component.getFeatureFilters());
						plugin.execTrackFiltering();
					}
				});
				
				wizard.setNextButtonEnabled(true);
			}
		});
	}

	@Override
	public void aboutToHidePanel() {
		final Logger logger = wizard.getLogger();
		logger.log("Performing track filtering on the following features:\n", Logger.BLUE_COLOR);
		List<FeatureFilter> featureFilters = component.getFeatureFilters();
		final TrackMateModel<T> model = plugin.getModel();
		model.getSettings().setTrackFilters(featureFilters);
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
