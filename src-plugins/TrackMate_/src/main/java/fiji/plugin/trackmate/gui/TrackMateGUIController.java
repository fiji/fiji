package fiji.plugin.trackmate.gui;

import ij.IJ;
import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.descriptors.ActionChooserDescriptor;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectorChoiceDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectorConfigurationDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectorDescriptor;
import fiji.plugin.trackmate.gui.descriptors.GrapherDescriptor;
import fiji.plugin.trackmate.gui.descriptors.TrackerConfigurationDescriptor;
import fiji.plugin.trackmate.gui.descriptors.ViewChoiceDescriptor;
import fiji.plugin.trackmate.gui.descriptors.InitFilterDescriptor;
import fiji.plugin.trackmate.gui.descriptors.LoadDescriptor;
import fiji.plugin.trackmate.gui.descriptors.LogPanelDescriptor;
import fiji.plugin.trackmate.gui.descriptors.SaveDescriptor;
import fiji.plugin.trackmate.gui.descriptors.SpotFilterDescriptor;
import fiji.plugin.trackmate.gui.descriptors.StartDialogDescriptor;
import fiji.plugin.trackmate.gui.descriptors.TrackFilterDescriptor;
import fiji.plugin.trackmate.gui.descriptors.TrackerChoiceDescriptor;
import fiji.plugin.trackmate.gui.descriptors.TrackingDescriptor;
import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;
import fiji.plugin.trackmate.gui.panels.StartDialogPanel;
import fiji.plugin.trackmate.providers.ActionProvider;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class TrackMateGUIController implements ActionListener {
	
	/*
	 * FIELDS
	 */

	private static final boolean DEBUG = false;
	private final Logger logger;
	/** The trackmate piloted here. */
	private final TrackMate trackmate;
	/** The GUI controlled by this controller.  */
	private final TrackMateWizard gui;
	private final TrackMateGUIModel guimodel;
	
	protected DetectorProvider detectorProvider;
	protected ViewProvider viewProvider;
	protected TrackerProvider trackerProvider;
	protected ActionProvider actionProvider;

	protected DetectorConfigurationDescriptor detectorConfigurationDescriptor;
	protected DetectorChoiceDescriptor detectorChoiceDescriptor;
	protected StartDialogDescriptor startDialoDescriptor;
	protected DetectorDescriptor detectorDescriptor;
	protected InitFilterDescriptor initFilterDescriptor;
	protected ViewChoiceDescriptor viewChoiceDescriptor;
	protected SpotFilterDescriptor spotFilterDescriptor;
	protected TrackerChoiceDescriptor trackerChoiceDescriptor;
	protected TrackerConfigurationDescriptor trackerConfigurationDescriptor;
	protected TrackingDescriptor trackingDescriptor;
	protected GrapherDescriptor grapherDescriptor;
	protected TrackFilterDescriptor trackFilterDescriptor;
	protected ConfigureViewsDescriptor configureViewsDescriptor;
	protected ActionChooserDescriptor actionChooserDescriptor;
	protected LogPanelDescriptor logPanelDescriptor;
	


	/*
	 * CONSTRUCTOR
	 */

	public TrackMateGUIController(final TrackMate trackmate, ImagePlus imp) {

		// I can't stand the metal look. If this is a problem, contact me (jeanyves.tinevez@gmail.com)
		if (IJ.isMacOSX() || IJ.isWindows()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		}
		
		
		this.gui = new TrackMateWizard(this);
		this.logger = gui.getLogger();
		this.trackmate = trackmate;
		
		createProviders();
		createDescriptors();

		this.guimodel = new TrackMateGUIModel();
		guimodel.currentDescriptor = startDialoDescriptor;

		trackmate.getModel().setLogger(logger);
		gui.setVisible(true);
		GuiUtils.positionWindow(gui, imp.getWindow());
		gui.addActionListener(this);

		init();
	}

	/*
	 * PUBLIC METHODS 
	 */

	
	/**
	 * Expose the {@link TrackMateWizard} instance controlled here.
	 */
	public TrackMateWizard getWizard() {
		return gui;
	}

	/**
	 * Expose the {@link TrackMate} trackmate piloted by the wizard.
	 */
	public TrackMate getPlugin() {
		return trackmate;
	}



	/*
	 * PROTECTED METHODS
	 */
	
	protected void createProviders() {
		detectorProvider 	= new DetectorProvider(); 
		viewProvider 		= new ViewProvider(trackmate.getModel(), trackmate.getSettings());
		trackerProvider 	= new TrackerProvider(trackmate.getModel(), trackmate.getSettings());
		actionProvider 		= new ActionProvider();
	}

	/**
	 * Creates the map of next descriptor for each descriptor.
	 */
	protected void createDescriptors() {
		
		/*
		 * Logging panel: receive message, share with the TrackMateModel
		 */
		LogPanel logPanel = gui.getLogPanel();
		logPanelDescriptor = new LogPanelDescriptor(logPanel); 
		
		/*
		 * Start panel
		 */
		startDialoDescriptor 		= new StartDialogDescriptor(trackmate);
		
		/*
		 * Choose detector
		 */
		detectorChoiceDescriptor 	= new DetectorChoiceDescriptor(detectorProvider, trackmate);
		
		/*
		 * Configure chosen detector
		 */
		detectorConfigurationDescriptor = new DetectorConfigurationDescriptor(detectorProvider, trackmate);
		
		/*
		 * Execute and report detection progress
		 */
		detectorDescriptor 			= new DetectorDescriptor(logPanel, trackmate);
		
		/*
		 * Initial spot filter: discard obvious spurious spot based on quality.
		 */
		initFilterDescriptor		= new InitFilterDescriptor(trackmate);
		
		/*
		 * Select and render a view
		 */
		viewChoiceDescriptor		= new ViewChoiceDescriptor(viewProvider);
		
		/*
		 * Spot filtering
		 */
		spotFilterDescriptor 		= new SpotFilterDescriptor(trackmate);
		spotFilterDescriptor.getComponent().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				displaySettings.put(TrackMateModelView.KEY_SPOT_COLOR_FEATURE, spotFilterDescriptor.getComponent().getColorByFeature());
			}
		});
		spotFilterDescriptor.getComponent().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if (DEBUG) {
					System.out.println("[TrackMateGUIControllaer] stateChanged caught in SpotFilterDescriptor.");
				}
				// We set the thresholds field of the model but do not touch its selected spot field yet.
				trackmate.getSettings().setSpotFilters(spotFilterDescriptor.getComponent().getFeatureFilters());
				trackmate.execSpotFiltering(false);
			}
		});
		
		/*
		 * Choose a tracker
		 */
		trackerChoiceDescriptor		= new TrackerChoiceDescriptor(trackerProvider, trackmate);
		
		/*
		 * Configure chosen tracker
		 */
		trackerConfigurationDescriptor = new TrackerConfigurationDescriptor(trackerProvider, trackmate);
		
		/*
		 * Execute tracking
		 */
		trackingDescriptor			= new TrackingDescriptor(logPanel, trackerProvider, trackmate);
		
		/*
		 * Track filtering 
		 */
		
		trackFilterDescriptor		= new TrackFilterDescriptor(trackmate);
		trackFilterDescriptor.getComponent().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				PerTrackFeatureColorGenerator generator = new PerTrackFeatureColorGenerator(trackmate.getModel(), TrackIndexAnalyzer.TRACK_INDEX);
				generator.setFeature(trackFilterDescriptor.getComponent().getColorByFeature());
				displaySettings.put(TrackMateModelView.KEY_TRACK_COLORING, generator);
			}
		});
		trackFilterDescriptor.getComponent().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				// We set the thresholds field of the model but do not touch its selected spot field yet.
				trackmate.getSettings().setTrackFilters(trackFilterDescriptor.getComponent().getFeatureFilters());
				trackmate.execTrackFiltering(false);
			}
		});
		
		/*
		 * Finished, let's change the display settings.
		 */
		configureViewsDescriptor	= new ConfigureViewsDescriptor(trackmate);
		
		/*
		 * Export and graph features.
		 */
		grapherDescriptor			= new GrapherDescriptor(trackmate);
		
		/*
		 * Offer to take some actions on the data.
		 */
		actionChooserDescriptor		= new ActionChooserDescriptor(actionProvider, trackmate);
	}
	
	protected WizardPanelDescriptor nextDescriptor(WizardPanelDescriptor currentDescriptor) {

		if (currentDescriptor == startDialoDescriptor) {
			return  detectorChoiceDescriptor;
			
		} else if (currentDescriptor == detectorChoiceDescriptor) {
			return detectorConfigurationDescriptor;
		} else if (currentDescriptor == detectorConfigurationDescriptor) {
			
			if (trackmate.getSettings().detectorFactory.getKey().equals(ManualDetectorFactory.DETECTOR_KEY)) {
				return viewChoiceDescriptor;
			} else {
				return detectorDescriptor;
			}

		} else if (currentDescriptor == detectorDescriptor) {
			return initFilterDescriptor;
			
		} else if (currentDescriptor == initFilterDescriptor) {
			return viewChoiceDescriptor;
			
		} else if (currentDescriptor == viewChoiceDescriptor) {
			return spotFilterDescriptor;
			
		} else if (currentDescriptor == spotFilterDescriptor) {
			return trackerChoiceDescriptor;
			
		} else if (currentDescriptor == trackerChoiceDescriptor) {
			return trackerConfigurationDescriptor;
			
		} else if (currentDescriptor == trackerConfigurationDescriptor) {
			return trackingDescriptor;
			
		} else if (currentDescriptor == trackingDescriptor) {
			return trackFilterDescriptor;
			
		} else if (currentDescriptor == trackFilterDescriptor) {
			return configureViewsDescriptor;
			
		} else if (currentDescriptor == configureViewsDescriptor) {
			return grapherDescriptor;
			
		} else if (currentDescriptor == grapherDescriptor) {
			return actionChooserDescriptor;
			
		} else if (currentDescriptor == actionChooserDescriptor) {
			return null;
			
		} else {
			throw new IllegalArgumentException("Next descriptor for " + currentDescriptor + " is unknown.");
		}
	}
	
	protected  WizardPanelDescriptor previousDescriptor(WizardPanelDescriptor currentDescriptor) {
		
		if (currentDescriptor == startDialoDescriptor) {
			return null;
			
		} else if (currentDescriptor == detectorChoiceDescriptor) {
			return startDialoDescriptor;
			
		} else if (currentDescriptor == detectorConfigurationDescriptor) {
			return detectorChoiceDescriptor;
			
		} else if (currentDescriptor == detectorDescriptor) {
			return detectorConfigurationDescriptor;
			
		} else if (currentDescriptor == initFilterDescriptor) {
			return detectorDescriptor;
			
		} else if (currentDescriptor == viewChoiceDescriptor) {
			return detectorConfigurationDescriptor;
			
		} else if (currentDescriptor == spotFilterDescriptor) {
			return viewChoiceDescriptor;
			
		} else if (currentDescriptor == trackerChoiceDescriptor) {
			return spotFilterDescriptor;
			
		} else if (currentDescriptor == trackerConfigurationDescriptor) {
			return trackerChoiceDescriptor;
			
		} else if (currentDescriptor == trackingDescriptor) {
			return trackerConfigurationDescriptor;
			
		} else if (currentDescriptor == trackFilterDescriptor) {
			return trackerConfigurationDescriptor;
			
		} else if (currentDescriptor == configureViewsDescriptor) {
			return trackFilterDescriptor;
			
		} else if (currentDescriptor == grapherDescriptor) {
			return configureViewsDescriptor;
			
		} else if (currentDescriptor == actionChooserDescriptor) {
			return grapherDescriptor;
			
		} else {
			throw new IllegalArgumentException("Previous descriptor for " + currentDescriptor + " is unknown.");
		}
	}

	
	
	/**
	 * Display the first panel
	 */
	protected void init() {
		// We need to listen to events happening on the View configuration
		configureViewsDescriptor.getComponent().addActionListener(this);

		// Get start panel id
		gui.setPreviousButtonEnabled(false);
		String id = StartDialogPanel.DESCRIPTOR;
		WizardPanelDescriptor panelDescriptor  = gui.getPanelDescriptorFor(id);
		
		String welcomeMessage = TrackMate.PLUGIN_NAME_STR + "v" + TrackMate.PLUGIN_NAME_VERSION + " started on:\n" + 
				TMUtils.getCurrentTimeString() + '\n';
		// Log GUI processing start
		gui.getLogger().log(welcomeMessage, Logger.BLUE_COLOR);

		// Execute about to be displayed action of the new one
		panelDescriptor.aboutToDisplayPanel();

		// Display matching panel
		gui.showDescriptorPanelFor(id);

		//  Show the panel in the dialog, and execute action after display
		panelDescriptor.displayingPanel();  
	}

	/*
	 * ACTION LISTENER
	 */

	@Override
	public void actionPerformed(ActionEvent event) {
		if (DEBUG)
			System.out.println("[TrackMateGUIController] Caught event "+event);

		if (event == gui.NEXT_BUTTON_PRESSED && guimodel.actionFlag) {

			new Thread("TrackMate moving to next step thread.") {
				public void run() {
					next();
				};
			}.start();


		} else if (event == gui.PREVIOUS_BUTTON_PRESSED && guimodel.actionFlag) {

			new Thread("TrackMate moving to previous step thread.") {
				public void run() {
					previous();
				};
			}.start();

		} else if (event == gui.LOAD_BUTTON_PRESSED && guimodel.actionFlag) {

			guimodel.actionFlag = false;
			gui.jButtonNext.setText("Resume");
			disableButtonsAndStoreState();

			new Thread("TrackMate moving to load state thread.") {
				public void run() {
					load();
				};
			}.start();

		} else if (event == gui.SAVE_BUTTON_PRESSED && guimodel.actionFlag) {

			guimodel.actionFlag = false;
			gui.jButtonNext.setText("Resume");
			disableButtonsAndStoreState();

			new Thread("TrackMate moving to save state thread.") {
				public void run() {
					save();
				};
			}.start();

		} else if ((event == gui.NEXT_BUTTON_PRESSED || 
				event == gui.PREVIOUS_BUTTON_PRESSED || 
				event == gui.LOAD_BUTTON_PRESSED ||
				event == gui.SAVE_BUTTON_PRESSED) && !guimodel.actionFlag ) {

			// Display old panel, but do not execute its actions
			guimodel.actionFlag = true;
			String id = gui.getCurrentPanelDescriptor().getNextDescriptorID();
			gui.showDescriptorPanelFor(id);

			// Put back buttons
			gui.jButtonNext.setText("Next");
			restoreButtonsState();

		} else if (event == gui.LOG_BUTTON_PRESSED) {
			
			if (guimodel.logButtonState) {
				
				restoreButtonsState();
				gui.showDescriptorPanelFor(guimodel.previousPanelID);
				
			} else {
				
				disableButtonsAndStoreState();
				guimodel.previousPanelID = gui.getCurrentPanelDescriptor().getComponentID();
				gui.showDescriptorPanelFor(LogPanelDescriptor.DESCRIPTOR);
				
			}
			guimodel.logButtonState = !guimodel.logButtonState;
			
		}
	}


	private void next() {

		gui.setNextButtonEnabled(false);
		try {

			// Execute leave action of the old panel
			WizardPanelDescriptor oldDescriptor = guimodel.currentDescriptor;
			if (oldDescriptor != null) {
				oldDescriptor.aboutToHidePanel();
			}

			// Find a store new one to display
			WizardPanelDescriptor panelDescriptor  = nextDescriptor(oldDescriptor);
			guimodel.currentDescriptor = panelDescriptor;

			// Check if the new panel has a next panel. If not, disable the next button
			if (null == nextDescriptor(panelDescriptor)) {
				gui.setNextButtonEnabled(false);
			}

			// Re-enable the previous button, in case it was disabled
			gui.setPreviousButtonEnabled(true);

			// Execute about to be displayed action of the new one
			panelDescriptor.aboutToDisplayPanel();

			// Display matching panel
			gui.show(panelDescriptor);

			//  Show the panel in the dialog, and execute action after display
			panelDescriptor.displayingPanel();

		} finally {

			gui.setNextButtonEnabled(true);
		}

	}

	private void previous() {
		// Move to previous panel, but do not execute its actions
		WizardPanelDescriptor olDescriptor = guimodel.currentDescriptor;
		WizardPanelDescriptor panelDescriptor = previousDescriptor(olDescriptor);
		gui.show(panelDescriptor);
		guimodel.currentDescriptor = panelDescriptor;
		
		// Check if the new panel has a next panel. If not, disable the next button
		if (null == previousDescriptor(panelDescriptor)) {
			gui.setPreviousButtonEnabled(false);
		}

		// Re-enable the previous button, in case it was disabled
		gui.setNextButtonEnabled(true);
	}

	private void load() {
		// Store current state
		WizardPanelDescriptor oldDescriptor = gui.getCurrentPanelDescriptor();

		// Move to load state and show log panel
		final LoadDescriptor loadDescriptor = (LoadDescriptor) gui.getPanelDescriptorFor(LoadDescriptor.DESCRIPTOR);
		loadDescriptor.setTargetNextID(oldDescriptor.getDescriptorID());
		loadDescriptor.aboutToDisplayPanel();
		gui.showDescriptorPanelFor(LoadDescriptor.DESCRIPTOR);

		// Instantiate GuiReader, ask for file, and load it in memory
		loadDescriptor.displayingPanel();

		// Enable or disable next and previous button depending on the target descriptor.
		WizardPanelDescriptor nextPanel = gui.getPanelDescriptorFor(loadDescriptor.getNextDescriptorID());
		if (nextPanel.getNextDescriptorID() == null) {
			guimodel.nextButtonState = false;
		} else {
			guimodel.nextButtonState = true;
		}
		if (nextPanel.getPreviousDescriptorID() == null) {
			guimodel.previousButtonState = false;
		} else {
			guimodel.previousButtonState = true;
		}

	}

	private void save() {
		// Store current state
		WizardPanelDescriptor oldDescriptor = gui.getCurrentPanelDescriptor();

		// Move to save state and execute
		SaveDescriptor saveDescriptor = (SaveDescriptor) gui.getPanelDescriptorFor(SaveDescriptor.DESCRIPTOR);
		saveDescriptor.setTargetNextID(oldDescriptor.getDescriptorID());
		saveDescriptor.aboutToDisplayPanel();
		gui.showDescriptorPanelFor(SaveDescriptor.DESCRIPTOR);
		gui.getLogger().log(TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR);
		saveDescriptor.displayingPanel();
	}
	

	/**
	 * Disable the 4 bottom buttons and memorize their state to that they
	 * can be restored when calling {@link #restoreButtonsState()}. 
	 */
	private void disableButtonsAndStoreState() {
		guimodel.loadButtonState 		= gui.jButtonLoad.isEnabled();
		guimodel.saveButtonState 		= gui.jButtonSave.isEnabled();
		guimodel.previousButtonState 	= gui.jButtonPrevious.isEnabled();
		guimodel.nextButtonState 		= gui.jButtonNext.isEnabled();
		gui.setLoadButtonEnabled(false);
		gui.setSaveButtonEnabled(false);
		gui.setPreviousButtonEnabled(false);
		gui.setNextButtonEnabled(false);
	}

	/**
	 * Restore the button state saved when calling {@link #disableButtonsAndStoreState()}.
	 * Do nothing if {@link #disableButtonsAndStoreState()} was not called before. 
	 */
	private void restoreButtonsState() {
		gui.setLoadButtonEnabled(guimodel.loadButtonState);
		gui.setSaveButtonEnabled(guimodel.saveButtonState);
		gui.setPreviousButtonEnabled(guimodel.previousButtonState);
		gui.setNextButtonEnabled(guimodel.nextButtonState);
	}

}