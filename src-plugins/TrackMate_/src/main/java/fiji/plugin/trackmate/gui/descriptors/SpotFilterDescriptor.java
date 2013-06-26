package fiji.plugin.trackmate.gui.descriptors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel.Category;
import fiji.plugin.trackmate.gui.panels.components.FilterGuiPanel;

public class SpotFilterDescriptor implements WizardPanelDescriptor {

	private ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener>();
	private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
	private static final String KEY = "SpotFilter";
	private FilterGuiPanel component;
	private final TrackMate trackmate;
	
	
	public SpotFilterDescriptor(TrackMate trackmate) {
		this.trackmate = trackmate;
	}
	
	@Override
	public FilterGuiPanel getComponent() {
		return component;
	}

	@Override
	public void aboutToDisplayPanel() {
		component = new FilterGuiPanel(trackmate.getModel(), Category.SPOTS);
		component.refreshDisplayedFeatureValues();
		Settings settings = trackmate.getSettings();
		component.setFilters(settings.getSpotFilters());
		component.setColorFeature(ColorByFeatureGUIPanel.UNIFORM_KEY);
		component.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				fireAction(event);
			}
		});
		component.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				fireThresholdChanged(event);
			}
		});
	}

	@Override
	public void displayingPanel() {
		if (null == component) {
			// This happens when we load data: the component gets initialized only in another method
			aboutToDisplayPanel();
		}
		trackmate.getSettings().setSpotFilters(component.getFeatureFilters());
		trackmate.execSpotFiltering(false);
	}
	
	@Override
	public void aboutToHidePanel() {
		Logger logger = trackmate.getModel().getLogger();
		logger.log("Performing spot filtering on the following features:\n", Logger.BLUE_COLOR);
		final Model model = trackmate.getModel();
		List<FeatureFilter> featureFilters = component.getFeatureFilters();
		trackmate.getSettings().setSpotFilters(featureFilters);
		trackmate.execSpotFiltering(false);

		int ntotal = model.getSpots().getNSpots(false);
		if (featureFilters == null || featureFilters.isEmpty()) {
			logger.log("No feature threshold set, kept the " + ntotal + " spots.\n");
		} else {
			for (FeatureFilter ft : featureFilters) {
				String str = "  - on "+trackmate.getModel().getFeatureModel().getSpotFeatureNames().get(ft.feature);
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

	@Override
	public String getKey() {
		return KEY;
	}

	
	/**
	 * Adds an {@link ActionListener} to this panel. These listeners will be notified when
	 * a button is pushed or when the feature to color is changed.
	 */
	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
	}
	
	/**
	 * Removes an ActionListener from this panel. 
	 * @return true if the listener was in the ActionListener collection of this instance.
	 */
	public boolean removeActionListener(ActionListener listener) {
		return actionListeners.remove(listener);
	}
	
	public Collection<ActionListener> getActionListeners() {
		return actionListeners;
	}
	

	/** 
	 * Forwards the given {@link ActionEvent} to all the {@link ActionListener} of this panel.
	 */
	private void fireAction(ActionEvent e) {
		for (ActionListener l : actionListeners)
			l.actionPerformed(e);
	}
	
	/**
	 * Add an {@link ChangeListener} to this panel. The {@link ChangeListener} will
	 * be notified when a change happens to the thresholds displayed by this panel, whether
	 * due to the slider being move, the auto-threshold button being pressed, or
	 * the combo-box selection being changed.
	 */
	public void addChangeListener(ChangeListener listener) {
		changeListeners.add(listener);
	}

	/**
	 * Remove a ChangeListener from this panel. 
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeChangeListener(ChangeListener listener) {
		return changeListeners.remove(listener);
	}

	public Collection<ChangeListener> getChangeListeners() {
		return changeListeners;
	}
	
	private void fireThresholdChanged(ChangeEvent e) {
		for (ChangeListener cl : changeListeners)  {
			cl.stateChanged(e);
		}
	}
}