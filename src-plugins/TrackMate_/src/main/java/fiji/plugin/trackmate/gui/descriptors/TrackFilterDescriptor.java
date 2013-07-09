package fiji.plugin.trackmate.gui.descriptors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel.Category;
import fiji.plugin.trackmate.gui.panels.components.FilterGuiPanel;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;

public class TrackFilterDescriptor implements WizardPanelDescriptor {

	private final ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
	private final ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener>();
	private static final String KEY = "FilterTracks";
	private FilterGuiPanel component;
	private final TrackMate trackmate;
	private final PerTrackFeatureColorGenerator trackColorGenerator;
	private final TrackMateGUIController controller;

	public TrackFilterDescriptor(final TrackMate trackmate, final PerTrackFeatureColorGenerator trackColorGenerator, final TrackMateGUIController controller) {
		this.trackmate = trackmate;
		this.trackColorGenerator = trackColorGenerator;
		this.controller = controller;
	}

	@Override
	public FilterGuiPanel getComponent() {
		return component;
	}

	@Override
	public void aboutToDisplayPanel() {
		component = new FilterGuiPanel(trackmate.getModel(),
				Arrays.asList(new Category[] {Category.TRACKS, Category.DEFAULT }));
		component.setFilters(trackmate.getSettings().getTrackFilters());
		component.setColorFeature(TrackIndexAnalyzer.TRACK_INDEX);
		component.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				fireAction(event);
			}
		});
		component.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent event) {
				fireThresholdChanged(event);
			}
		});
		component.setColorFeature(trackColorGenerator.getFeature());
	}

	@Override
	public void displayingPanel() {
		if (null == component) {
			// Happens when loading at this stage.
			aboutToDisplayPanel();
		} else {
			component.setColorFeature(trackColorGenerator.getFeature());
		}
		controller.getGUI().setNextButtonEnabled(true);
	}


	@Override
	public void aboutToHidePanel() {
		final Logger logger = trackmate.getModel().getLogger();
		logger.log("Performing track filtering on the following features:\n", Logger.BLUE_COLOR);
		final List<FeatureFilter> featureFilters = component.getFeatureFilters();
		final Model model = trackmate.getModel();
		trackmate.getSettings().setTrackFilters(featureFilters);
		trackmate.execTrackFiltering(true);

		if (featureFilters == null || featureFilters.isEmpty()) {
			logger.log("No feature threshold set, kept the " + model.getTrackModel().nTracks(false) + " tracks.\n");
		} else {
			for (final FeatureFilter ft : featureFilters) {
				String str = "  - on "+model.getFeatureModel().getTrackFeatureNames().get(ft.feature);
				if (ft.isAbove)
					str += " above ";
				else
					str += " below ";
				str += String.format("%.1f", ft.value);
				str += '\n';
				logger.log(str);
			}
			logger.log("Kept "+model.getTrackModel().nTracks(true)+" tracks out of "+model.getTrackModel().nTracks(false)+".\n");
		}
		trackmate.computeEdgeFeatures(true);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	/**
	 * Adds an {@link ActionListener} to this panel. These listeners will be notified when
	 * a button is pushed or when the feature to color is changed.
	 */
	public void addActionListener(final ActionListener listener) {
		actionListeners.add(listener);
	}

	/**
	 * Removes an ActionListener from this panel.
	 * @return true if the listener was in the ActionListener collection of this instance.
	 */
	public boolean removeActionListener(final ActionListener listener) {
		return actionListeners.remove(listener);
	}

	public Collection<ActionListener> getActionListeners() {
		return actionListeners;
	}


	/**
	 * Forwards the given {@link ActionEvent} to all the {@link ActionListener} of this panel.
	 */
	private void fireAction(final ActionEvent e) {
		for (final ActionListener l : actionListeners)
			l.actionPerformed(e);
	}

	/**
	 * Add an {@link ChangeListener} to this panel. The {@link ChangeListener} will
	 * be notified when a change happens to the thresholds displayed by this panel, whether
	 * due to the slider being move, the auto-threshold button being pressed, or
	 * the combo-box selection being changed.
	 */
	public void addChangeListener(final ChangeListener listener) {
		changeListeners.add(listener);
	}

	/**
	 * Remove a ChangeListener from this panel.
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeChangeListener(final ChangeListener listener) {
		return changeListeners.remove(listener);
	}

	public Collection<ChangeListener> getChangeListeners() {
		return changeListeners;
	}

	private void fireThresholdChanged(final ChangeEvent e) {
		for (final ChangeListener cl : changeListeners)  {
			cl.stateChanged(e);
		}
	}
}
