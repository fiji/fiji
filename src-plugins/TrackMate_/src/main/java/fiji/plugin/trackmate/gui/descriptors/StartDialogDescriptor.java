
package fiji.plugin.trackmate.gui.descriptors;

import ij.ImagePlus;
import ij.WindowManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.panels.StartDialogPanel;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

public class StartDialogDescriptor  implements WizardPanelDescriptor {

	private static final String KEY = "Start";
	private final StartDialogPanel panel;
	private final TrackMate trackmate;
	private SpotAnalyzerProvider spotAnalyzerProvider;
	private EdgeAnalyzerProvider edgeAnalyzerProvider;
	private TrackAnalyzerProvider trackAnalyzerProvider;
	private ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener>();
	
	public StartDialogDescriptor(TrackMate trackmate, SpotAnalyzerProvider spotAnalyzerProvider, EdgeAnalyzerProvider edgeAnalyzerProvider, TrackAnalyzerProvider trackAnalyzerProvider) {
		this.trackmate = trackmate;
		this.spotAnalyzerProvider = spotAnalyzerProvider;
		this.edgeAnalyzerProvider = edgeAnalyzerProvider;
		this.trackAnalyzerProvider = trackAnalyzerProvider;
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
	public void aboutToDisplayPanel() {
		ImagePlus imp;
		if (null == trackmate.getSettings().imp) {
			imp = WindowManager.getCurrentImage();
		} else {
			panel.echoSettings(trackmate.getModel(), trackmate.getSettings());
			imp = trackmate.getSettings().imp;
		}
		panel.getFrom(imp);
	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		Settings settings = trackmate.getSettings();
		TrackMateModel model = trackmate.getModel();
		
		/*
		 *  Get settings and pass them to the trackmate managed by the wizard
		 */
		
		panel.updateTo(model, settings);
		trackmate.getModel().getLogger().log(settings.toStringImageInfo());
		
		/*
		 * Configure settings object with spot, edge and track analyzers as specified
		 * in the providers.
		 */
		
		ImgPlus<?> img = ImagePlusAdapter.wrapImgPlus(settings.imp);
		settings.clearSpotAnalyzerFactories();
		List<String> spotAnalyzerKeys = spotAnalyzerProvider.getAvailableSpotFeatureAnalyzers();
		for (String key : spotAnalyzerKeys) {
			SpotAnalyzerFactory<?> spotFeatureAnalyzer = spotAnalyzerProvider.getSpotFeatureAnalyzer(key, img);
			settings.addSpotAnalyzerFactory(spotFeatureAnalyzer);
		}
		
		settings.clearEdgeAnalyzers();
		List<String> edgeAnalyzerKeys = edgeAnalyzerProvider.getAvailableEdgeFeatureAnalyzers();
		for (String key : edgeAnalyzerKeys) {
			EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getEdgeFeatureAnalyzer(key);
			settings.addEdgeAnalyzer(edgeAnalyzer);
		}
		
		settings.clearTrackAnalyzers();
		List<String> trackAnalyzerKeys = trackAnalyzerProvider.getAvailableTrackFeatureAnalyzers();
		for (String key : trackAnalyzerKeys) {
			TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getTrackFeatureAnalyzer(key);
			settings.addTrackAnalyzer(trackAnalyzer);
		}
		
		trackmate.getModel().getLogger().log(settings.toStringFeatureAnalyzersInfo());
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
