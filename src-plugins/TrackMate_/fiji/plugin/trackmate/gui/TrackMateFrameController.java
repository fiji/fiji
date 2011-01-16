package fiji.plugin.trackmate.gui;

import ij.ImagePlus;
import ij.gui.NewImage;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdom.DataConversionException;
import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.FeatureThreshold;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TMUtils;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.TrackMateFrame.PanelCard;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer.DisplayerType;
import fiji.plugin.trackmate.visualization.SpotDisplayer.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.trackscheme.SpotCell;
import fiji.plugin.trackmate.visualization.trackscheme.SpotIconGrabber;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class TrackMateFrameController {

	/*
	 * ENUMS
	 */

	private enum GuiState {
		START,
		TUNE_SEGMENTER,
		SEGMENTING,
		INITIAL_THRESHOLDING,
		CHOOSE_DISPLAYER,
		CALCULATE_FEATURES,
		TUNE_THRESHOLDS,
		THRESHOLD_BLOBS,
		TUNE_TRACKER,
		TRACKING,
		TUNE_DISPLAY;
		
		/**
		 * Provide the next state the view should be into when pushing the 'next' button.
		 */
		public GuiState nextState() {
			switch (this) {
			case START:
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
				return TUNE_TRACKER;
			case TUNE_TRACKER:
				return TRACKING;
			case TRACKING:
				return TUNE_DISPLAY;
			case TUNE_DISPLAY:
				return TUNE_DISPLAY;
			}
			return null;
		}
		

		/**
		 * Provide the previous state the view should be into when pushing the 'previous' button.
		 */
		public GuiState previousState() {
			switch (this) {
			case TUNE_SEGMENTER:
				return START;
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
				return THRESHOLD_BLOBS;
			case TRACKING:
				return TUNE_TRACKER;
			case TUNE_DISPLAY:
				return TRACKING;
			case START:
				return START;
			}
			return null;
		}
		
		/**
		 * Update the view given in argument in adequation with the current state.
		 * @param view
		 */
		public void updateGUI(final TrackMateFrame view) {
			// Display adequate card
			final TrackMateFrame.PanelCard key;
			switch (this) {

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
				key = PanelCard.THRESHOLD_GUI_KEY;
				break;
			
			case TUNE_TRACKER:
				key = PanelCard.TUNE_TRACKER_KEY;
				break;
			
			case TUNE_DISPLAY:
				key = PanelCard.DISPLAYER_PANEL_KEY;
				break;
			}
			final GuiState state = this;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					view.displayPanel(key);
					// Update button states
					switch(state) {
					case START:
						view.jButtonPrevious.setEnabled(false);
						view.jButtonNext.setEnabled(true);
						break;
					case TUNE_DISPLAY:
						view.jButtonPrevious.setEnabled(true);
						view.jButtonNext.setEnabled(false);
						break;
					default:
						view.jButtonPrevious.setEnabled(true);
						view.jButtonNext.setEnabled(true);
					}
					
				}
			});
		}
		
		public void performTask(final TrackMateFrameController controller) {
			switch(this) {
			case TUNE_SEGMENTER:
				// Get the settings field from the 
				controller.execGetStartSettings();
				return;
			case SEGMENTING:
				controller.execSegmentationStep();
				return;
			case CHOOSE_DISPLAYER:
				// Before we switch to the log display when calculating features, we *execute* the initial thresholding step.
				controller.execInitialThresholding();
				return;
			case CALCULATE_FEATURES:
				// Compute the feature first
				controller.execCalculateFeatures();
				// Then we launch the displayer
				controller.execLaunchdisplayer();
				return;
			case TUNE_THRESHOLDS:
				controller.execLinkDisplayerToThresholdGUI();
				return;
			case THRESHOLD_BLOBS:
				controller.execThresholding();
				return;
			case TRACKING:
				controller.execTrackingStep();
				return;
			case TUNE_DISPLAY:
				controller.execLinkDisplayerToTuningGUI();
				return;
				
			default:
				return;
		
			}
		}
	}


	
	/*
	 * CONSTANTS
	 */
	
	private static final String DEFAULT_FILENAME = "TrackMateData.xml";

	/*
	 * FIELDS
	 */
	
	private GuiState state;
	private Logger logger;
	private SpotDisplayer displayer;
	private File file;
	private DisplayUpdater updater;
	
	private TrackMateModelInterface model;
	private final TrackMateFrame view;
	private final TrackMateFrameController controller; 
	
	/**
	 * This action listener is made for normal processing, when the user presses the next/previous
	 * button and expects processing to occur.
	 */
	private final ActionListener inProcessActionListener;	
	/**
	 * Is used to determine how to react to a 'next' button push. If it is set to true, then we are
	 * normally processing through the GUI, and pressing 'next' should update the GUI and process the
	 * data. If is is set to false, then we are currently loading/saving the data, and we should simply
	 * re-generate the data.
	 */
	private boolean actionFlag = true;
	
	
	/*
	 * CONSTRUCTOR
	 */
	
	public TrackMateFrameController(final TrackMateModelInterface model) {
		this.model = model;
		this.view = new TrackMateFrame(model);
		this.controller = this;
		this.logger = view.getLogger();
		
		// Instantiate action listeners
		this.inProcessActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {

				view.jButtonSave.setEnabled(true);
				view.jButtonLoad.setEnabled(true);

				if (actionFlag) {

					if (event == view.NEXT_BUTTON_PRESSED) {					
						state = state.nextState();
						state.updateGUI(view);
						state.performTask(controller);
					} else if (event == view.PREVIOUS_BUTTON_PRESSED) {
						state = state.previousState();
						state.updateGUI(view);
					} else if (event == view.LOAD_BUTTON_PRESSED) {
						load();
					} else if (event == view.SAVE_BUTTON_PRESSED) {
						save();
					} else {
						logger.error("Unknown event caught: "+event+'\n');
					}
					
				} else {
					
					actionFlag = true;
					state.updateGUI(view);
					
				}

			}
		};

		// Set up GUI and communications
		model.setLogger(logger);
		if (null != model.getSettings().imp)
			view.setLocationRelativeTo(model.getSettings().imp.getWindow());
		else
			view.setLocationRelativeTo(null);
		view.setVisible(true);
		view.addActionListener(inProcessActionListener);
		state = GuiState.START;
		state.updateGUI(view);
		initUpdater();
	}
	
	
	private void load() {
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
		@SuppressWarnings("rawtypes")
		TrackMateModelInterface newModel = new TrackMate_();
		newModel.setLogger(logger);
		
		if (null == file) {
			File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
			file = new File(folder.getPath() + File.separator + DEFAULT_FILENAME);
		}
		JFileChooser fileChooser = new JFileChooser(file.getParent());
		fileChooser.setSelectedFile(file);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
		fileChooser.setFileFilter(filter);
		
		int returnVal = fileChooser.showOpenDialog(view);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
		} else {
			logger.log("Load data aborted.\n");
			return;  	    		
		}
		
		logger.log("Opening file "+file.getName()+'\n');
		TmXmlReader reader = new TmXmlReader(file);
		try {
			reader.parse();
		} catch (JDOMException e) {
			logger.error("Problem parsing "+file.getName()+", it is not a valid TrackMate XML file.\nError message is:\n"
					+e.getLocalizedMessage()+'\n');
		} catch (IOException e) {
			logger.error("Problem reading "+file.getName()
					+".\nError message is:\n"+e.getLocalizedMessage()+'\n');
		}
		logger.log("  Parsing file done.\n");
		
		Settings settings = null;
		ImagePlus imp = null;
		
		{ // Read settings
			try {
				settings = reader.getSettings();
			} catch (DataConversionException e) {
				logger.error("Problem reading the settings field of "+file.getName()
						+". Error message is:\n"+e.getLocalizedMessage()+'\n');
				return;
			}
			logger.log("  Reading settings done.\n");

			// Try to read image
			imp = reader.getImage();		
			if (null == imp) {
				// Provide a dummy empty image if linked image can't be found
				logger.log("Could not find image "+settings.imageFileName+" in "+settings.imageFolder+". Substituting dummy image.\n");
				imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes * settings.nslices, NewImage.FILL_BLACK);
				imp.setDimensions(1, settings.nslices, settings.nframes);
			}

			settings.imp = imp;
			newModel.setSettings(settings);
			logger.log("  Reading image done.\n");
		}


		{ // Try to read segmenter settings
			SegmenterSettings segmenterSettings = null;
			try {
				segmenterSettings = reader.getSegmenterSettings();
			} catch (DataConversionException e1) {
				logger.error("Problem reading the segmenter settings field of "+file.getName()
						+". Error message is:\n"+e1.getLocalizedMessage()+'\n');
			}
			if (null == segmenterSettings) {
				// Fill in defaults
				segmenterSettings = new SegmenterSettings();
				settings.segmenterSettings = segmenterSettings;
				settings.segmenterType = segmenterSettings.segmenterType;
				settings.trackerSettings = new TrackerSettings();
				settings.trackerType = settings.trackerSettings.trackerType;
				newModel.setSettings(settings);
				this.model = newModel;
				view.setModel(model);
				// Stop at start panel
				state = GuiState.START;
				logger.log("Loading data finished, press 'next' to resume.\n");
				switchNextButton(true);
				return;
			}

			settings.segmenterSettings = segmenterSettings;
			settings.segmenterType = segmenterSettings.segmenterType;
			settings.trackerSettings = new TrackerSettings(); // put defaults for now
			settings.trackerType = settings.trackerSettings.trackerType;
			newModel.setSettings(settings);
			logger.log("  Reading segmenter settings done.\n");
		}
		
		
		{ // Try to read spots
			TreeMap<Integer, List<Spot>> spots = null;
			try {
				spots = reader.getAllSpots();
			} catch (DataConversionException e) {
				logger.error("Problem reading the spots field of "+file.getName()
						+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}
			if (null == spots) {
				// No spots, so we stop here, and switch to the segmenter panel
				imp.show();
				this.model = newModel;
				view.setModel(model);
				state = GuiState.TUNE_SEGMENTER;
				logger.log("Loading data finished, press 'next' to resume.\n");
				switchNextButton(true);
				return;
			}

			// We have a spot field, update the model.
			newModel.setSpots(spots);
			logger.log("  Reading spots done.\n");
		}
		
		
		{ // Try to read the initial threshold
			FeatureThreshold initialThreshold = null;
			try {
				initialThreshold = reader.getInitialThreshold();
			} catch (DataConversionException e) {
				logger.error("Problem reading the initial threshold field of "+file.getName()
						+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}

			if (initialThreshold == null) {
				// No initial threshold, so set it
				this.model = newModel;
				view.setModel(model);
				state = GuiState.INITIAL_THRESHOLDING;
				logger.log("Loading data finished, press 'next' to resume.\n");
				switchNextButton(true);
				return;
			}

			// Store it in model
			newModel.setInitialThreshold(initialThreshold.value);
			logger.log("  Reading initial threshold done.\n");
		}		
		
		{ // Try to read feature thresholds
			List<FeatureThreshold> featureThresholds = null;
			try {
				featureThresholds = reader.getFeatureThresholds();
			} catch (DataConversionException e) {
				logger.error("Problem reading the feature threholds field of "+file.getName()
						+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}

			if (null == featureThresholds) {
				// No feature thresholds, we assume we have the features calculated, and put ourselves
				// in a state such that the threshold GUI will be displayed.
				this.model = newModel;
				view.setModel(model);
				state = GuiState.CALCULATE_FEATURES;
				actionFlag = true;
				boolean is3D = settings.imp.getNSlices() > 1;
				if (is3D)
					displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.THREEDVIEWER_DISPLAYER, model);
				else 
					displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);					 
				logger.log("Loading data finished, press 'next' to resume.\n");
				switchNextButton(true);
				return;
			}

			// Store thresholds in model
			newModel.setFeatureThresholds(featureThresholds);
			logger.log("  Reading feature thresholds done.\n");
		}


		{ // Try to read spot selection
			TreeMap<Integer, List<Spot>> selectedSpots = null;
			try {
				selectedSpots = reader.getSpotSelection(newModel.getSpots());
			} catch (DataConversionException e) {
				logger.error("Problem reading the spot selection field of "+file.getName()+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}

			// No spot selection, so we display the feature threshold GUI, with the loaded feature threshold
			// already in place.
			if (null == selectedSpots) {
				this.model = newModel;
				view.setModel(model);
				state = GuiState.CALCULATE_FEATURES;
				actionFlag = true;
				boolean is3D = settings.imp.getNSlices() > 1;
				if (is3D)
					displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.THREEDVIEWER_DISPLAYER, model);
				else 
					displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
				displayer.setSpots(model.getSpots());
				logger.log("Loading data finished, press 'next' to resume.\n");
				switchNextButton(true);
				return;
			}

			newModel.setSpotSelection(selectedSpots);
			logger.log("  Reading spot selection done.\n");
		}
		

		{ // Try to read tracker settings
			TrackerSettings trackerSettings = null;
			try {
				trackerSettings = reader.getTrackerSettings();
			} catch (DataConversionException e) {
				logger.error("Problem reading the tracker settings field of "+file.getName()
						+". Error message is:\n"+e.getLocalizedMessage()+'\n');
			}
			if (null == trackerSettings) {
				// Fill in defaults
				trackerSettings = new TrackerSettings();
				settings.trackerSettings = trackerSettings;
				settings.trackerType = trackerSettings.trackerType;
				newModel.setSettings(settings);
				this.model = newModel;
				view.setModel(model);
				// Stop at tune tracker panel
				state = GuiState.TUNE_TRACKER;
				boolean is3D = settings.imp.getNSlices() > 1;
				if (is3D)
					displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.THREEDVIEWER_DISPLAYER, model);
				else 
					displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
				displayer.setSpots(model.getSpots());
				displayer.setSpotsToShow(model.getSelectedSpots());
				logger.log("Loading data finished, press 'next' to resume.\n");
				switchNextButton(true);
				return;
			}

			settings.trackerSettings = trackerSettings;
			settings.trackerType = trackerSettings.trackerType;
			newModel.setSettings(settings);
			logger.log("  Reading tracker settings done.\n");
		}
		

		{ // Try reading the tracks 
			SimpleGraph<Spot, DefaultEdge> trackGraph = null; 
			try {
				trackGraph = reader.getTracks(newModel.getSelectedSpots());
			} catch (DataConversionException e) {
				logger.error("Problem reading the track field of "+file.getName()
						+". Error message is\n"+e.getLocalizedMessage()+'\n');
			}
			if (null == trackGraph) {
				this.model = newModel;
				view.setModel(model);
				// Stop at tune tracker panel
				state = GuiState.TUNE_TRACKER;
				boolean is3D = settings.imp.getNSlices() > 1;
				if (is3D)
					displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.THREEDVIEWER_DISPLAYER, model);
				else 
					displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
				displayer.setSpots(model.getSpots());
				displayer.setSpotsToShow(model.getSelectedSpots());
				logger.log("Loading data finished, press 'next' to resume.\n");
				switchNextButton(true);
				return;
			}
			
			logger.log("  Reading tracks done.\n");
			newModel.setTrackGraph(trackGraph);
		}
		
		this.model = newModel;
		view.setModel(model);
		state = GuiState.TRACKING;
		actionFlag = true; // force redraw and relinking
		boolean is3D = settings.imp.getNSlices() > 1;
		if (is3D)
			displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.THREEDVIEWER_DISPLAYER, model);
		else 
			displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
		displayer.setSpots(model.getSpots());
		displayer.setSpotsToShow(model.getSelectedSpots());
		displayer.setTrackGraph(model.getTrackGraph());
		updater.doUpdate();
		logger.log("Loading data finished, press 'next' to resume.\n");
		switchNextButton(true);
	}
	
	private void save() {
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
			file = new File(folder.getPath() + File.separator + DEFAULT_FILENAME);
		}
		JFileChooser fileChooser = new JFileChooser(file.getParent());
		fileChooser.setSelectedFile(file);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
		fileChooser.setFileFilter(filter);

		int returnVal = fileChooser.showSaveDialog(view);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
		} else {
			logger.log("Save data aborted.\n");
			view.jButtonSave.setEnabled(true);
			return;  	    		
		}
		
		TmXmlWriter writer = new TmXmlWriter(model, logger);
		switch (state) {
		case START:
			model.setSettings(view.startDialogPanel.getSettings());
			writer.appendBasicSettings();
			break;
		case TUNE_SEGMENTER:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			break;
		case SEGMENTING:
		case INITIAL_THRESHOLDING:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendSpots();
			break;		
		case CALCULATE_FEATURES:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendInitialThreshold();
			writer.appendSpots();
			break;
		case TUNE_THRESHOLDS:
		case THRESHOLD_BLOBS:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendInitialThreshold();
			writer.appendFeatureThresholds();
			writer.appendSpots();
			break;
		case TUNE_TRACKER:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendTrackerSettings();
			writer.appendInitialThreshold();
			writer.appendFeatureThresholds();
			writer.appendSpotSelection();
			writer.appendSpots();
			break;
		case TRACKING:
		case TUNE_DISPLAY:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendTrackerSettings();
			writer.appendInitialThreshold();
			writer.appendFeatureThresholds();
			writer.appendSpotSelection();
			writer.appendTracks();
			writer.appendSpots();
			break;
		}
		try {
			writer.writeToFile(file);
			logger.log("Data saved to: "+file.toString()+'\n');
		} catch (FileNotFoundException e) {
			logger.error("File not found:\n"+e.getMessage()+'\n');
		} catch (IOException e) {
			logger.error("Input/Output error:\n"+e.getMessage()+'\n');
		} finally {
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

	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Instantiate the updater and make sure a "quit" hook is registered when
	 * the view is closed.
	 */
	private void initUpdater() {
		updater = new DisplayUpdater();
		view.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				updater.quit();
			}
		});
	}
	
	
	private void execGetStartSettings() {
		model.setSettings(view.startDialogPanel.getSettings());
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
	 * the {@link Spot} collection of the {@link TrackMateModelInterface} with the result.
	 */
	private void execInitialThresholding() {
		FeatureThreshold initialThreshold = view.initThresholdingPanel.getFeatureThreshold();
		String str = "Initial thresholding with a quality threshold above "+ String.format("%.1f", initialThreshold.value) + " ...\n";
		logger.log(str,Logger.BLUE_COLOR);
		int ntotal = 0;
		for (Collection<Spot> spots : model.getSpots().values())
			ntotal += spots.size();
		model.setInitialThreshold(initialThreshold.value);
		model.execInitialThresholding();
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
		model.computeFeatures();		
		logger.log("Calculating features done.\n", Logger.BLUE_COLOR);
		switchNextButton(true);
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
				if (null != displayer) {
					displayer.clear();
				}
				displayer = SpotDisplayer.instantiateDisplayer(view.displayerChooserPanel.getDisplayerType(), model);
				displayer.setSpots(model.getSpots());
				// Re-enable the GUI
				logger.log("Rendering done.\n", Logger.BLUE_COLOR);
				switchNextButton(true);
				updater.doUpdate();
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

				view.thresholdGuiPanel.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						displayer.setColorByFeature(view.thresholdGuiPanel.getColorByFeature());
						updater.doUpdate();
					}
				});
				
				view.thresholdGuiPanel.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent event) {
						// We set the thresholds field of the model but do not touch its selected spot field yet.
						model.setFeatureThresholds(view.thresholdGuiPanel.getFeatureThresholds());
						displayer.setSpotsToShow(TMUtils.thresholdSpots(model.getSpots(), model.getFeatureThresholds()));
						updater.doUpdate();
					}
				});
				
				view.thresholdGuiPanel.stateChanged(null); // force redraw
				updater.doUpdate();
			}
		});
	}
	
	/**
	 * Retrieve the thresholds list set in the threshold GUI, forward it to the model, and 
	 * perform the threshold in the model.
	 */
	private void execThresholding() {
		logger.log("Performing feature threholding on the following features:\n", Logger.BLUE_COLOR);
		List<FeatureThreshold> featureThresholds = view.thresholdGuiPanel.getFeatureThresholds();
		model.setFeatureThresholds(featureThresholds);
		model.execThresholding();
		displayer.setSpotsToShow(model.getSelectedSpots());
		
		int ntotal = 0;
		for(Collection<Spot> spots : model.getSpots().values())
			ntotal += spots.size();
		if (featureThresholds == null || featureThresholds.isEmpty()) {
			logger.log("No feature threshold set, kept the " + ntotal + " spots.\n");
		} else {
			for (FeatureThreshold ft : featureThresholds) {
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
			for(Collection<Spot> spots : model.getSelectedSpots().values())
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
				long start = System.currentTimeMillis();
				model.execTracking();
				displayer.setTrackGraph(model.getTrackGraph());
				displayer.setDisplayTrackMode(TrackDisplayMode.ALL_WHOLE_TRACKS, 20);
				updater.doUpdate();
				// Re-enable the GUI
				switchNextButton(true);
				long end = System.currentTimeMillis();
				logger.log(String.format("Tracking done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
			}
		}.start();
	}
	
	/**
	 * Link the displayer to the tuning display panel in the view.
	 */
	private void execLinkDisplayerToTuningGUI() {
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				
				view.displayerPanel.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						if (event == view.displayerPanel.SPOT_COLOR_MODE_CHANGED) {
							displayer.setColorByFeature(view.displayerPanel.getColorSpotByFeature());
						} else if (event == view.displayerPanel.SPOT_VISIBILITY_CHANGED) {
							displayer.setSpotVisible(view.displayerPanel.isDisplaySpotSelected());
						} else if (event == view.displayerPanel.TRACK_DISPLAY_MODE_CHANGED) {
							displayer.setDisplayTrackMode(view.displayerPanel.getTrackDisplayMode(), view.displayerPanel.getTrackDisplayDepth());
						} else if (event == view.displayerPanel.TRACK_VISIBILITY_CHANGED) {
							displayer.setTrackVisible(view.displayerPanel.isDisplayTrackSelected());
						} else if (event == view.displayerPanel.TRACK_SCHEME_BUTTON_PRESSED) {
							launchTrackScheme();
						} else {
							logger.error("Unknown event caught: "+event+'\n');
						}
						updater.doUpdate();
					}
				});
				
				updater.doUpdate();
				
				
			}
		});
	}
	
	private void launchTrackScheme() {
		// Update icons
		if (null != model.getSettings().imp) {
			SpotIconGrabber grabber = new SpotIconGrabber(model.getSettings());
			grabber.updateIcon(model.getSpots());
		}
		
		// Display Track scheme
		final TrackSchemeFrame frame = new TrackSchemeFrame(model.getTrackGraph(), model.getSettings());
		frame.setVisible(true);

		frame.jgraph.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					// Get Cell under Mousepointer
					int x = e.getX(), y = e.getY();
					Object obj = frame.jgraph.getFirstCellForLocation(x, y);
					
					if (obj instanceof SpotCell) {
						SpotCell sc = (SpotCell) obj;
						Spot spot = sc.getSpot();
						displayer.highlight(spot);
					} else {
						System.out.println("Double-slicked on a "+obj.getClass().getCanonicalName());// DEBUG
					}
				}
			}
		});
	
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
	 * INNER CLASSES
	 */
	
	
	/**
	 * This is a helper class modified after a class by Albert Cardona. Here, it is in
	 * charge of refreshing the displayer view of the spot and tracks.
	 */
	private class DisplayUpdater extends Thread {
		long request = 0;

		// Constructor autostarts thread
		DisplayUpdater() {
			super("TrackMate displayer thread");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call displayer update from this thread
					if (r > 0)
						displayer.refresh(); // Is likely to generate NPE
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		ij.ImageJ.main(args);
		@SuppressWarnings("rawtypes")
		TrackMateModelInterface model = new TrackMate_();
		new TrackMateFrameController(model);
	}

}
