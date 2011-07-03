package fiji.plugin.trackmate.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.TrackMateFrame.PanelCard;
import fiji.plugin.trackmate.segmentation.SegmenterType;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class TrackMateFrameController implements ActionListener {

	/*
	 * FIELDS
	 */

	private static final boolean DEBUG = false;
	/** This GUI current state. */
	private GuiState state;
	private Logger logger;
	/**
	 * The {@link TrackMateModelView} that renders the segmenting and tracking results on the image data.
	 */
	private TrackMateModelView displayer;
	private File file;

	/** The model describing the data. */
	private TrackMateModel model;
	/** The GUI controlled by this controller.  */
	private TrackMateFrame view;

	/**
	 * Is used to determine how to react to a 'next' button push. If it is set to true, then we are
	 * normally processing through the GUI, and pressing 'next' should update the GUI and process the
	 * data. If is is set to false, then we are currently loading/saving the data, and we should simply
	 * re-generate the data.
	 */
	boolean actionFlag = true;


	/*
	 * CONSTRUCTOR
	 */

	public TrackMateFrameController(final TrackMateModel model) {
		this.model = model;
		this.view = new TrackMateFrame(model);
		this.logger = view.getLogger();

		// Set up GUI and communications
		model.setLogger(logger);
		if (null != model.getSettings().imp)
			view.setLocationRelativeTo(model.getSettings().imp.getWindow());
		else
			view.setLocationRelativeTo(null);
		view.setVisible(true);
		view.addActionListener(this);
		state = GuiState.START;
		updateGUI();
	}

	/*
	 * ACTION LISTENER
	 */

	@Override
	public void actionPerformed(ActionEvent event) {
		if (DEBUG)
			System.out.println("[TrackMateFrameController] Caught event "+event);
		DisplayerPanel displayerPanel = (DisplayerPanel) view.getPanelFor(PanelCard.DISPLAYER_PANEL_KEY);

		if (event == view.NEXT_BUTTON_PRESSED && actionFlag) {

			performPreGUITask();
			state = state.nextState();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() { updateGUI(); }
			});
			performPostGUITask();

		} else if (event == view.PREVIOUS_BUTTON_PRESSED && actionFlag) {

			state = state.previousState();
			updateGUI();

		} else if (event == view.LOAD_BUTTON_PRESSED && actionFlag) {

			load();

		} else if (event == view.SAVE_BUTTON_PRESSED && actionFlag) {

			save();

		} else if ((event == view.NEXT_BUTTON_PRESSED || 
				event == view.PREVIOUS_BUTTON_PRESSED || 
				event == view.LOAD_BUTTON_PRESSED ||
				event == view.SAVE_BUTTON_PRESSED) && !actionFlag ) {

			actionFlag = true;
			updateGUI();

		} else if (event == displayerPanel.TRACK_SCHEME_BUTTON_PRESSED) {

			// Display Track scheme
			final TrackSchemeFrame trackScheme = new TrackSchemeFrame(model);
			trackScheme.setVisible(true);


		}

	}

	/*
	 * GETTERS / SETTERS
	 */

	public void setModel(final TrackMateModel model) {
		this.model = model;
	}

	public TrackMateModel getModel() {
		return model;
	}

	public void setState(final GuiState state) {
		this.state = state;
	}

	public GuiState getState() {
		return state;
	}

	public TrackMateFrame getView() {
		return view;
	}


	/*
	 * GUI STATE PRIVATE METHODS
	 */

	/**
	 * Update the view given in argument in acquaintance with the current state.
	 */
	private void updateGUI() {
		// Display adequate card
		final TrackMateFrame.PanelCard key;
		switch (state) {

		default:
		case SEGMENTING:
		case CALCULATE_FEATURES:
		case THRESHOLD_BLOBS:
		case TRACKING:
			key = PanelCard.LOG_PANEL_KEY;
			break;

		case START:
			key = PanelCard.START_DIALOG_KEY;
			break;

		case CHOOSE_SEGMENTER:
			key = PanelCard.SEGMENTER_CHOICE_KEY;
			break;

		case TUNE_SEGMENTER:
			key = PanelCard.TUNE_SEGMENTER_KEY;
			break;

		case INITIAL_THRESHOLDING:
			key = PanelCard.INITIAL_THRESHOLDING_KEY;
			break;

		case CHOOSE_DISPLAYER:
			key = PanelCard.DISPLAYER_CHOICE_KEY;
			break;

		case TUNE_THRESHOLDS:
			key = PanelCard.SPOT_FILTER_GUI_KEY;
			break;

		case CHOOSE_TRACKER:
			key = PanelCard.TRACKER_CHOICE_KEY;
			break;

		case TUNE_TRACKER:
			key = PanelCard.TUNE_TRACKER_KEY;
			break;

		case TUNE_DISPLAY:
			key = PanelCard.DISPLAYER_PANEL_KEY;
			break;

		case ACTIONS:
			key = PanelCard.ACTION_PANEL_KEY;
		}
		view.displayPanel(key);
		// Update button states
		switch(state) {
		case START:
			view.jButtonPrevious.setEnabled(false);
			view.jButtonNext.setEnabled(true);
			break;
		case ACTIONS:
			view.jButtonPrevious.setEnabled(true);
			view.jButtonNext.setEnabled(false);
			break;
		default:
			view.jButtonPrevious.setEnabled(true);
			view.jButtonNext.setEnabled(true);
		}
		// Extra actions
		switch(state) {

		case TUNE_THRESHOLDS:
			execLinkDisplayerToThresholdGUI();
			break;

		case TUNE_DISPLAY:
			DisplayerPanel displayerPanel = (DisplayerPanel) view.getPanelFor(PanelCard.DISPLAYER_PANEL_KEY);
			displayerPanel.register(displayer);
		}
	}

	private void performPreGUITask() {
		switch(state) {
		case CHOOSE_SEGMENTER:
			execGetSegmenterChoice();
			break;
		case CHOOSE_TRACKER:
			execGetTrackerChoice();
			break;
		}
	}

	/**
	 * Action taken after the GUI has been displayed. 
	 */
	private void performPostGUITask() {
		switch(state) {
		case CHOOSE_SEGMENTER:
			// Get the settings basic fields from the start dialog panel 
			execGetStartSettings();
			return;
		case TUNE_SEGMENTER:
			// If we choose to skip segmentation, initialize the model spot content and skip directly to state where we will be asked for a displayer.
			if (model.getSettings().segmenterType == SegmenterType.MANUAL_SEGMENTER) {
				model.setSpots(new SpotCollection(), false);
				model.setFilteredSpots(new SpotCollection(), false);
				state = GuiState.CHOOSE_DISPLAYER.previousState();
			}
			break;
		case SEGMENTING:
			execSegmentationStep();
			return;
		case CHOOSE_DISPLAYER:
			// Before we switch to the log display when calculating features, we *execute* the initial thresholding step,
			// only if we did not skip segmentation.
			if (model.getSettings().segmenterType != SegmenterType.MANUAL_SEGMENTER)
				execInitialThresholding();
			return;
		case CALCULATE_FEATURES: {
			// Compute the feature first, again, only if we did not skip segmentation.
			if (model.getSettings().segmenterType != SegmenterType.MANUAL_SEGMENTER)
				execCalculateFeatures();
			else {
				// Otherwise we get the manual spot diameter,  and plan to jump to tracking
				model.getSettings().segmenterSettings = view.segmenterSettingsPanel.getSettings();
				state = GuiState.CHOOSE_TRACKER.previousState();
			}
			// Then we launch the displayer
			execLaunchdisplayer();
			return;
		}
		case THRESHOLD_BLOBS:
			execThresholding();
			return;
		case TRACKING:
			execTrackingStep();
			return;
		default:
			return;

		}
	}


	/*
	 * PRIVATE METHODS
	 */


	private void load() {
		try {

			actionFlag = false;
			SwingUtilities.invokeLater(new Runnable() {			
				@Override
				public void run() {
					view.jButtonLoad.setEnabled(false);
					view.jButtonSave.setEnabled(false);
					view.jButtonNext.setEnabled(false);
					view.jButtonPrevious.setEnabled(false);
				}
			});
			view.displayPanel(PanelCard.LOG_PANEL_KEY);

			// New model to feed
			TrackMateModel newModel = new TrackMate_();
			newModel.setLogger(logger);

			if (null == file) {
				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				try {
					file = new File(folder.getPath() + File.separator + model.getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}

			GuiReader reader = new GuiReader(this);
			File tmpFile = reader.askForFile(file);
			if (null == tmpFile) {
				SwingUtilities.invokeLater(new Runnable() {			
					@Override
					public void run() {
						view.jButtonLoad.setEnabled(true);
						view.jButtonSave.setEnabled(true);
						view.jButtonNext.setEnabled(true);
						view.jButtonPrevious.setEnabled(true);
					}
				});
				return;
			}
			file = tmpFile;
			model = reader.loadFile(file);

		} finally {

			SwingUtilities.invokeLater(new Runnable() {			
				@Override
				public void run() {
					view.jButtonLoad.setEnabled(true);
					view.jButtonSave.setEnabled(true);
					view.jButtonNext.setEnabled(true);
					view.jButtonPrevious.setEnabled(true);
				}
			});

		}
	}

	private void save() {
		try {

			SwingUtilities.invokeLater(new Runnable() {			
				@Override
				public void run() {
					view.jButtonLoad.setEnabled(false);
					view.jButtonSave.setEnabled(false);
					view.jButtonNext.setEnabled(false);
					view.jButtonPrevious.setEnabled(false);
				}
			});
			view.displayPanel(PanelCard.LOG_PANEL_KEY);

			logger.log("Saving data...\n", Logger.BLUE_COLOR);
			if (null == file ) {
				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				try {
					file = new File(folder.getPath() + File.separator + model.getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}

			GuiSaver saver = new GuiSaver(this);
			File tmpFile = saver.askForFile(file);
			if (null == tmpFile) {
				actionFlag = false;
				SwingUtilities.invokeLater(new Runnable() {			
					@Override
					public void run() {
						view.jButtonLoad.setEnabled(true);
						view.jButtonSave.setEnabled(true);
						view.jButtonNext.setEnabled(true);
						view.jButtonPrevious.setEnabled(true);
					}
				});
				return;
			}
			file = tmpFile;
			saver.writeFile(file);
		}	finally {
			actionFlag = false;
			SwingUtilities.invokeLater(new Runnable() {			
				@Override
				public void run() {
					view.jButtonLoad.setEnabled(true);
					view.jButtonSave.setEnabled(true);
					view.jButtonNext.setEnabled(true);
					view.jButtonPrevious.setEnabled(true);
				}
			});

		}
	}

	private void execGetStartSettings() {
		model.setSettings(view.startDialogPanel.getSettings());
	}

	private void execGetSegmenterChoice() {
		Settings settings = model.getSettings();
		settings.segmenterType = view.segmenterChoicePanel.getChoice();
		model.setSettings(settings);
	}

	private void execGetTrackerChoice() {
		Settings settings = model.getSettings();
		settings.trackerType = view.trackerChoicePanel.getChoice();
		model.setSettings(settings);
	}

	/**
	 * Switch to the log panel, and execute the segmentation step, which will be delegated to 
	 * the {@link TrackMate_} glue class in a new Thread.
	 */
	private void execSegmentationStep() {
		switchNextButton(false);
		model.getSettings().segmenterSettings = view.segmenterSettingsPanel.getSettings();
		logger.log("Starting segmentation...\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(model.getSettings().toString());
		logger.log(model.getSettings().segmenterSettings.toString());
		new Thread("TrackMate segmentation thread") {					
			public void run() {
				long start = System.currentTimeMillis();
				try {
					model.execSegmentation();
				} catch (Exception e) {
					logger.error("An error occured:\n"+e+'\n');
					e.printStackTrace(logger);
				} finally {
					switchNextButton(true);
					long end = System.currentTimeMillis();
					logger.log(String.format("Segmentation done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				}
			}
		}.start();
	}

	/**
	 * Apply the quality threshold set by the {@link TrackMateFrame#initThresholdingPanel}, and <b>overwrite</b> 
	 * the {@link Spot} collection of the {@link TrackMateModel} with the result.
	 */
	private void execInitialThresholding() {
		FeatureFilter<SpotFeature> initialThreshold = view.initThresholdingPanel.getFeatureThreshold();
		String str = "Initial thresholding with a quality threshold above "+ String.format("%.1f", initialThreshold.value) + " ...\n";
		logger.log(str,Logger.BLUE_COLOR);
		int ntotal = 0;
		for (Collection<Spot> spots : model.getSpots().values())
			ntotal += spots.size();
		model.setInitialSpotFilterValue(initialThreshold.value);
		model.execInitialSpotFiltering();
		int nselected = 0;
		for (Collection<Spot> spots : model.getSpots().values())
			nselected += spots.size();
		logger.log(String.format("Retained %d spots out of %d.\n", nselected, ntotal));
	}


	/**
	 * Compute all features on all spots retained after initial thresholding.
	 */
	private void execCalculateFeatures() {
		switchNextButton(false);
		logger.log("Calculating features...\n",Logger.BLUE_COLOR);
		// Calculate features
		new Thread("TrackMate feature calculation thread") {
			public void run() {
				try {
					model.computeSpotFeatures();		
					logger.log("Calculating features done.\n", Logger.BLUE_COLOR);
				} finally {
					switchNextButton(true);
				}
			}
		}.start();
	}

	/**
	 * Render spots in another thread, then switch to the thresholding panel. 
	 */
	private void execLaunchdisplayer() {
		// Launch renderer
		logger.log("Rendering results...\n",Logger.BLUE_COLOR);
		switchNextButton(false);
		// Thread for rendering
		new Thread("TrackMate rendering thread") {
			public void run() {
				// Instantiate displayer
				try {
					if (null != displayer) {
						displayer.clear();
					}
					displayer = AbstractTrackMateModelView.instantiateView(view.displayerChooserPanel.getChoice(), model);

					// Re-enable the GUI
					logger.log("Rendering done.\n", Logger.BLUE_COLOR);
				} finally {
					switchNextButton(true);
				}
			}
		}.start();
	}

	/**
	 * Link the displayer frame to the threshold gui displayed in the view, so that 
	 * displayed spots are updated live when the user changes something in the view.
	 */
	private void execLinkDisplayerToThresholdGUI() {
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {

				view.spotFilterGuiPanel.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						displayer.setDisplaySettings(TrackMateModelView.KEY_SPOT_COLOR_FEATURE, view.spotFilterGuiPanel.getColorByFeature());
						displayer.refresh();
					}
				});

				view.spotFilterGuiPanel.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent event) {
						// We set the thresholds field of the model but do not touch its selected spot field yet.
						model.setSpotFilters(view.spotFilterGuiPanel.getFeatureThresholds());
						model.execSpotFiltering();
						displayer.refresh();
					}
				});

				view.spotFilterGuiPanel.stateChanged(null); // force redraw
			}
		});
	}

	/**
	 * Retrieve the thresholds list set in the threshold GUI, forward it to the model, and 
	 * perform the threshold in the model.
	 */
	private void execThresholding() {
		logger.log("Performing feature threholding on the following features:\n", Logger.BLUE_COLOR);
		List<FeatureFilter<SpotFeature>> featureThresholds = view.spotFilterGuiPanel.getFeatureThresholds();
		model.setSpotFilters(featureThresholds);
		model.execSpotFiltering();

		int ntotal = 0;
		for(Collection<Spot> spots : model.getSpots().values())
			ntotal += spots.size();
		if (featureThresholds == null || featureThresholds.isEmpty()) {
			logger.log("No feature threshold set, kept the " + ntotal + " spots.\n");
		} else {
			for (FeatureFilter<SpotFeature> ft : featureThresholds) {
				String str = "  - on "+ft.feature.name();
				if (ft.isAbove) 
					str += " above ";
				else
					str += " below ";
				str += String.format("%.1f", ft.value);
				str += '\n';
				logger.log(str);
			}
			int nselected = 0;
			for(Collection<Spot> spots : model.getFilteredSpots().values())
				nselected += spots.size();
			logger.log("Kept "+nselected+" spots out of " + ntotal + ".\n");
		}		
	}

	/**
	 * Switch to the log panel, and execute the tracking part in another thread.
	 */
	private void execTrackingStep() {
		switchNextButton(false);
		model.getSettings().trackerSettings = view.trackerSettingsPanel.getSettings();
		logger.log("Starting tracking...\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(model.getSettings().trackerSettings.toString());
		new Thread("TrackMate tracking thread") {					
			public void run() {
				try {
					long start = System.currentTimeMillis();
					model.execTracking();
					// Re-enable the GUI
					long end = System.currentTimeMillis();
					logger.log(String.format("Tracking done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				} finally {
					switchNextButton(true);
				}
			}
		}.start();
	}

	private void switchNextButton(final boolean state) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.jButtonNext.setEnabled(state);
			}
		});
	}


	/*
	 * ENUMS
	 */


	public void setModelView(TrackMateModelView displayer) {
		this.displayer = displayer;
	}

	public TrackMateModelView getModelView() {
		return displayer;
	}


	/**
	 * A set of constant describing the states of the GUI.
	 */
	public enum GuiState {
		START,
		CHOOSE_SEGMENTER,
		TUNE_SEGMENTER,
		SEGMENTING,
		INITIAL_THRESHOLDING,
		CHOOSE_DISPLAYER,
		CALCULATE_FEATURES,
		TUNE_THRESHOLDS,
		THRESHOLD_BLOBS,
		CHOOSE_TRACKER,
		TUNE_TRACKER,
		TRACKING,
		TUNE_DISPLAY,
		ACTIONS;

		/**
		 * Provide the next state the view should be into when pushing the 'next' button.
		 */
		private GuiState nextState() {
			switch (this) {
			case START:
				return CHOOSE_SEGMENTER;
			case CHOOSE_SEGMENTER:
				return TUNE_SEGMENTER;
			case TUNE_SEGMENTER:
				return SEGMENTING;
			case SEGMENTING:
				return INITIAL_THRESHOLDING;
			case INITIAL_THRESHOLDING:
				return CHOOSE_DISPLAYER;
			case CHOOSE_DISPLAYER:
				return CALCULATE_FEATURES;
			case CALCULATE_FEATURES:
				return TUNE_THRESHOLDS;
			case TUNE_THRESHOLDS:
				return THRESHOLD_BLOBS;
			case THRESHOLD_BLOBS:
				return CHOOSE_TRACKER;
			case CHOOSE_TRACKER:					
				return TUNE_TRACKER;
			case TUNE_TRACKER:
				return TRACKING;
			case TRACKING:
				return TUNE_DISPLAY;
			case TUNE_DISPLAY:
			case ACTIONS:
				return ACTIONS;				
			}
			return null;
		}


		/**
		 * Provide the previous state the view should be into when pushing the 'previous' button.
		 */
		private GuiState previousState() {
			switch (this) {
			case CHOOSE_SEGMENTER:
			case START:
				return START;
			case TUNE_SEGMENTER:
				return CHOOSE_SEGMENTER;
			case SEGMENTING:
				return TUNE_SEGMENTER;
			case INITIAL_THRESHOLDING:
				return SEGMENTING;
			case CHOOSE_DISPLAYER:
				return INITIAL_THRESHOLDING;
			case CALCULATE_FEATURES:
				return CHOOSE_DISPLAYER;
			case TUNE_THRESHOLDS:
				return CALCULATE_FEATURES;
			case THRESHOLD_BLOBS:
				return TUNE_THRESHOLDS;
			case TUNE_TRACKER:
				return CHOOSE_TRACKER;
			case CHOOSE_TRACKER:
				return THRESHOLD_BLOBS;
			case TRACKING:
				return TUNE_TRACKER;
			case TUNE_DISPLAY:
				return TRACKING;
			case ACTIONS:
				return TUNE_DISPLAY;
			}
			return null;
		}
	}



}