
package fiji.plugin.trackmate.gui.descriptors;

import ij.ImagePlus;
import ij.WindowManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.imglib2.meta.ImgPlus;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.StartDialogPanel;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class StartDialogDescriptor  implements WizardPanelDescriptor {

	private static final String KEY = "Start";
	private final StartDialogPanel panel;
	private ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener>();
	private final TrackMateGUIController controller;
	/** The view that is launched immediately when this descriptor leaves. It will be used
	 * as a central view.*/
	private HyperStackDisplayer mainView;
	
	public StartDialogDescriptor(TrackMateGUIController controller) {
		this.controller = controller;
		this.panel = new StartDialogPanel();
		panel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				fireAction(event);
			}
		});
	}
	
	/*
	 * METHODS
	 */
	
	/**
	 * Returns <code>true</code> if the {@link ImagePlus} selected is valid and can
	 * be processed.
	 * @return  a boolean flag.
	 */
	public boolean isImpValid() {
		return panel.isImpValid();
	}
	
	/*
	 * WIZARDPANELDESCRIPTOR METHODS
	 */


	@Override
	public StartDialogPanel getComponent() {
		return panel;
	}


	@Override
	public void aboutToDisplayPanel() { }

	@Override
	public void displayingPanel() { 
		ImagePlus imp;
		TrackMate trackmate = controller.getPlugin();
		if (null == trackmate.getSettings().imp) {
			imp = WindowManager.getCurrentImage();
		} else {
			panel.echoSettings(trackmate.getModel(), trackmate.getSettings());
			imp = trackmate.getSettings().imp;
		}
		panel.getFrom(imp);
	}

	@Override
	public void aboutToHidePanel() {
		TrackMate trackmate = controller.getPlugin();
		Settings settings = trackmate.getSettings();
		Model model = trackmate.getModel();
		
		/*
		 *  Get settings and pass them to the trackmate managed by the wizard
		 */
		
		panel.updateTo(model, settings);
		trackmate.getModel().getLogger().log(settings.toStringImageInfo());
		
		/*
		 * Configure settings object with spot, edge and track analyzers as specified
		 * in the providers.
		 */
		
		ImgPlus<?> img = TMUtils.rawWraps(settings.imp);
		settings.clearSpotAnalyzerFactories();
		SpotAnalyzerProvider spotAnalyzerProvider = controller.getSpotAnalyzerProvider();
		List<String> spotAnalyzerKeys = spotAnalyzerProvider.getAvailableSpotFeatureAnalyzers();
		for (String key : spotAnalyzerKeys) {
			SpotAnalyzerFactory<?> spotFeatureAnalyzer = spotAnalyzerProvider.getSpotFeatureAnalyzer(key, img);
			settings.addSpotAnalyzerFactory(spotFeatureAnalyzer);
		}
		
		settings.clearEdgeAnalyzers();
		EdgeAnalyzerProvider edgeAnalyzerProvider = controller.getEdgeAnalyzerProvider();
		List<String> edgeAnalyzerKeys = edgeAnalyzerProvider.getAvailableEdgeFeatureAnalyzers();
		for (String key : edgeAnalyzerKeys) {
			EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getEdgeFeatureAnalyzer(key);
			settings.addEdgeAnalyzer(edgeAnalyzer);
		}
		
		settings.clearTrackAnalyzers();
		TrackAnalyzerProvider trackAnalyzerProvider = controller.getTrackAnalyzerProvider();
		List<String> trackAnalyzerKeys = trackAnalyzerProvider.getAvailableTrackFeatureAnalyzers();
		for (String key : trackAnalyzerKeys) {
			TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getTrackFeatureAnalyzer(key);
			settings.addTrackAnalyzer(trackAnalyzer);
		}
		
		trackmate.getModel().getLogger().log(settings.toStringFeatureAnalyzersInfo());
		
		/*
		 * Launch the ImagePlus view now.
		 */
		
		// De-register old one, if any.
		if (mainView != null) {
			mainView.clear();
			model.removeModelChangeListener(mainView);
		}
		
		SelectionModel selectionModel = controller.getSelectionModel();
		mainView = new HyperStackDisplayer(model, selectionModel, settings.imp);
		controller.getGuimodel().addView(mainView);
		Map<String, Object> displaySettings = controller.getGuimodel().getDisplaySettings();
		for (String key : displaySettings.keySet()) {
			mainView.setDisplaySettings(key, displaySettings.get(key));
		}
		mainView.render();
	}

	@Override
	public String getKey() {
		return KEY;
	}
	
	/*
	 * LISTERNER METHODS
	 */
	
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
	 * Forward the given {@link ActionEvent} to all the {@link ActionListener} of this panel.
	 */
	private void fireAction(ActionEvent e) {
		for (ActionListener l : actionListeners)
			l.actionPerformed(e);
	}

	
}
