package fiji.plugin.trackmate.gui;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.plugin.Duplicator;
import ij.process.ColorProcessor;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import loci.formats.FormatException;
import mpicbg.imglib.type.numeric.RealType;

import org.jdom.DataConversionException;
import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.FeatureThreshold;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.Utils;
import fiji.plugin.trackmate.gui.TrackMateFrame.PanelCard;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer2D;
import fiji.plugin.trackmate.visualization.SpotDisplayer3D;
import fiji.plugin.trackmate.visualization.SpotDisplayer.TrackDisplayMode;

public class TrackMateFrameController implements ActionListener {

	/*
	 * ENUMS
	 */

	private enum GuiState {
		START,
		TUNE_SEGMENTER,
		SEGMENTING,
		INITIAL_THRESHOLDING,
		CALCULATE_FEATURES, 
		THRESHOLD_SPOTS,
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
				return CALCULATE_FEATURES;
			case CALCULATE_FEATURES:
				return THRESHOLD_SPOTS;
			case THRESHOLD_SPOTS:
				return TUNE_TRACKER;
			case TUNE_TRACKER:
				return TRACKING;
			case TRACKING:
				return TUNE_DISPLAY;
			case TUNE_DISPLAY:
			default:
				return TUNE_DISPLAY;
			}
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
			case CALCULATE_FEATURES:
				return INITIAL_THRESHOLDING;
			case THRESHOLD_SPOTS:
				return CALCULATE_FEATURES;
			case TUNE_TRACKER:
				return THRESHOLD_SPOTS;
			case TRACKING:
				return TUNE_TRACKER;
			case TUNE_DISPLAY:
				return TRACKING;
			case START:
			default:
				return START;
			}
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
			case START:
				key = PanelCard.START_DIALOG_KEY;
				break;
			case TUNE_SEGMENTER:
				key = PanelCard.TUNE_SEGMENTER_KEY;
				break;
			case SEGMENTING:
				key = PanelCard.LOG_PANEL_KEY;
				break;
			case INITIAL_THRESHOLDING:
				key = PanelCard.INITIAL_THRESHOLDING_KEY;
				break;
			case THRESHOLD_SPOTS:
				key = PanelCard.THRESHOLD_GUI_KEY;
				break;
			case CALCULATE_FEATURES:
				key = PanelCard.LOG_PANEL_KEY;
				break;
			case TUNE_TRACKER:
				key = PanelCard.TUNE_TRACKER_KEY;
				break;
			case TRACKING:
				key = PanelCard.LOG_PANEL_KEY;
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
			case CALCULATE_FEATURES:
				// Before we switch to the log display when calculating features, we execute the *initial* thresholding step.
				controller.execInitialThresholding();
				// Then we calculate the other features.
				controller.execCalculateFeatures();
				// Then we launch the displayer
				controller.execLaunchdisplayer();
				return;
			case THRESHOLD_SPOTS:
				controller.execLinkDisplayerToThresholdGUI();
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
	
	private static final int DEFAULT_RESAMPLING_FACTOR = 4; // for the 3d viewer
	private static final int DEFAULT_THRESHOLD = 50; // for the 3d viewer
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
	private TrackMateFrame view;
	
	
	/*
	 * CONSTRUCTOR
	 */
	
	public TrackMateFrameController(final TrackMateModelInterface model) {
		this.model = model;
		view = new TrackMateFrame(model);
		logger = view.getLogger();
		model.setLogger(logger);
		if (null != model.getSettings().imp)
			view.setLocationRelativeTo(model.getSettings().imp.getWindow());
		else
			view.setLocationRelativeTo(null);
		view.setVisible(true);
		view.addActionListener(this);
		state = GuiState.START;
		state.updateGUI(view);
		initUpdater();
	}
	
	
	/*
	 * ACTIONLISTENER METHODS
	 */
	

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event == view.NEXT_BUTTON_PRESSED) {
			state = state.nextState();
			state.updateGUI(view);
			state.performTask(this);
		} else if (event == view.PREVIOUS_BUTTON_PRESSED) {
			state = state.previousState();
			state.updateGUI(view);
		} else if (event == view.LOAD_BUTTON_PRESSED) {
			logger.log("Load button pushed.\n");			
		} else if (event == view.SAVE_BUTTON_PRESSED) {
			logger.log("Save button pushed.\n");
		} else {
			logger.error("Unknown event caught: "+event+'\n');
		}
	}
	
	
	/*
	
	private void load() {
		view.jButtonLoad.setEnabled(false);
		if (null == file ) {
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
		
//		cardLayout.show(jPanelMain, LOG_PANEL_KEY);
		logger.log("Opening file "+file.getName()+'\n');
		TmXmlReader reader = new TmXmlReader(file);
		try {
			reader.parse();
		} catch (JDOMException e) {
			logger.error("Problem parsing "+file.getName()+", it is not a valid TrackMate XML file.\nError message is:\n"+e.getLocalizedMessage()+'\n');
		} catch (IOException e) {
			logger.error("Problem reading "+file.getName()+".\nError message is:\n"+e.getLocalizedMessage()+'\n');
		}
		logger.log("  Parsing file done.\n");
		
		// Read settings
		Settings settings = null;
		try {
			settings = reader.getSettings();
		} catch (DataConversionException e1) {
			logger.error("Problem reading the settings field of "+file.getName()+". Error message is:\n"+e1.getLocalizedMessage()+'\n');
		}
		logger.log("  Reading settings done.\n");

		
		// Try to read image
		ImagePlus imp = null;
		try {
			imp = reader.getImage();
		} catch (IOException e) {
			logger.error("Problem loading the image linked by "+file.getName()+". Error message is:\n"+e.getLocalizedMessage()+'\n');
		} catch (FormatException e) {
			logger.error("Problem loading the image linked by "+file.getName()+". Error message is:\n"+e.getLocalizedMessage()+'\n');
		}
		if (null == imp) {
			// Provide a dummy empty image if linked image can't be found
			logger.log("Could not find image "+settings.imageFileName+" in "+settings.imageFolder+". Substituting dummy image.\n");
			imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes * settings.nslices, NewImage.FILL_BLACK);
			imp.setDimensions(1, settings.nslices, settings.nframes);
		}
//		imp.show(); Launching the 2d displayer after that raise a NPE, I don't know why
		settings.imp = imp;
		model.setSettings(settings);
		logger.log("  Reading image done.\n");
		
		// Try to read spots
		TreeMap<Integer, List<Spot>> spots = null;
		try {
			spots = reader.getAllSpots();
		} catch (DataConversionException e) {
			logger.error("Problem reading the spots field of "+file.getName()+". Error message is\n"+e.getLocalizedMessage()+'\n');
		}
		if (null == spots) {
			// No spots, so we stop here, and switch to the start panel
			if (null != startDialogPanel)
				jPanelMain.remove(startDialogPanel);
			startDialogPanel = new StartDialogPanel(trackmate.getSettings());
			jPanelMain.add(startDialogPanel, START_DIALOG_KEY);
			cardLayout.show(jPanelMain, START_DIALOG_KEY);
			state = GuiState.START;
			view.jButtonLoad.setEnabled(true);
			return;
		}
		// We have a spot field, so we can instantiate a new displayer
		model.setSpots(spots);
		logger.log("  Reading spots done - launching displayer.\n");
		displayer = instantiateDisplayer(model);
		// Also update the feature threshold GUI, in case the user move back to it
		thresholdGuiPanel.setSpots(spots.values());
		
		// Try to read spot selection
		TreeMap<Integer, List<Spot>> selectedSpots = null;
		try {
			selectedSpots = reader.getSpotSelection(spots);
		} catch (DataConversionException e) {
			logger.error("Problem reading the spot selection field of "+file.getName()+". Error message is\n"+e.getLocalizedMessage()+'\n');
		}
		if (null == selectedSpots) {
			// No spot selection, so we go to the thresholding panel
			execThresholdingStep();
			state = GuiState.THRESHOLD_BLOBS;
			view.jButtonLoad.setEnabled(true);
			return;
		}
		model.setSpotSelection(selectedSpots);
		logger.log("  Reading spot selection done.\n");
		displayer.setSpotsToShow(selectedSpots);
		
		SimpleGraph<Spot, DefaultEdge> trackGraph = null; 
		try {
			trackGraph = reader.getTracks(selectedSpots);
		} catch (DataConversionException e) {
			logger.error("Problem reading the track field of "+file.getName()+". Error message is\n"+e.getLocalizedMessage()+'\n');
		}
		if (null == trackGraph) {
			// No track so we go to the tracking panel
			execTuneTracker();
			state = GuiState.TUNE_TRACKER;
			view.jButtonLoad.setEnabled(true);
			return;
		}
		logger.log("  Reading tracks done.\n");
		displayer.setTrackGraph(trackGraph);
		model.setTrackGraph(trackGraph);
		state = GuiState.TRACKING;
		view.jButtonLoad.setEnabled(true);
		return;
		
	}
	
	private void save() {
		view.jButtonSave.setEnabled(false);
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
		
		TmXmlWriter writer = new TmXmlWriter(model);
		try {
			writer.writeToFile(file);
			logger.log("Data saved to: "+file.toString()+'\n');
		} catch (FileNotFoundException e) {
			logger.error("File not found:\n"+e.getMessage()+'\n');
		} catch (IOException e) {
			logger.error("Input/Output error:\n"+e.getMessage()+'\n');
		} finally {
			view.jButtonSave.setEnabled(true);
		}
	}

	*/
	
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.jButtonNext.setEnabled(false);
			}
		});
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
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							view.jButtonNext.setEnabled(true);
						}
					});
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
		FeatureThreshold qualityThreshold = view.initThresholdingPanel.getFeatureThreshold();
		String str = "Initial thresholding with a quality threshold ";
		if (qualityThreshold.isAbove)
			str += "above ";
		else
			str += "below ";
		str += "" + String.format("%.1f", qualityThreshold.value) + " ...\n";
		logger.log(str,Logger.BLUE_COLOR);
		ArrayList<FeatureThreshold> featureThresholds = new ArrayList<FeatureThreshold>(1);
		featureThresholds.add(qualityThreshold);
		TreeMap<Integer, List<Spot>> thresholdedSpots = Utils.thresholdSpots(model.getSpots(), featureThresholds);
		int ntotal = 0;
		for (Collection<Spot> spots : model.getSpots().values())
			ntotal += spots.size();
		int nselected = 0;
		for (Collection<Spot> spots : thresholdedSpots.values())
			nselected += spots.size();
		logger.log(String.format("Retained %d spots out of %d.\n", nselected, ntotal));
		model.setSpots(thresholdedSpots);
	}
	
	
	/**
	 * Compute all features on all spots retained after initial thresholding.
	 */
	private void execCalculateFeatures() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.jButtonNext.setEnabled(false);
			}
		});
		logger.log("Calculating features...\n",Logger.BLUE_COLOR);
		// Calculate features
		model.computeFeatures();		
		logger.log("Calculating features done.\n", Logger.BLUE_COLOR);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.jButtonNext.setEnabled(true);
			}
		});
	}
	
	/**
	 * Render spots in another thread, then switch to the thresholding panel. 
	 */
	private void execLaunchdisplayer() {
		// Launch renderer
		logger.log("Rendering results...\n",Logger.BLUE_COLOR);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.jButtonNext.setEnabled(false);
			}
		});
		// Thread for rendering
		new Thread("TrackMate rendering thread") {
			public void run() {
				// Instantiate displayer
				if (null != displayer) {
					displayer.clear();
				}
				displayer = instantiateDisplayer(model);
				displayer.setSpots(model.getSpots());
				// Re-enable the GUI
				logger.log("Rendering done.\n", Logger.BLUE_COLOR);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						view.jButtonNext.setEnabled(true);
					}
				});
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
						model.setFeatureThresholds(view.thresholdGuiPanel.getFeatureThresholds());
						displayer.setSpotsToShow(model.getSelectedSpots());
						updater.doUpdate();
					}
				});
				
				view.thresholdGuiPanel.stateChanged(null); // force redraw
				updater.doUpdate();
			}
		});
	}
	
	/**
	 * Switch to the log panel, and execute the tracking part in another thread.
	 */
	private void execTrackingStep() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.jButtonNext.setEnabled(false);
			}
		});
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
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						view.jButtonNext.setEnabled(true);
					}
				});
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
	
	
	
	/*
	 * STATIC METHODS
	 */
	
	/**
	 * Ensure an 8-bit gray image is sent to the 3D viewer.
	 */
	public static final ImagePlus[] makeImageForViewer(final Settings settings) {
		final ImagePlus origImp = settings.imp;
		origImp.killRoi();
		final ImagePlus imp;
		
		if (origImp.getType() == ImagePlus.GRAY8)
			imp = origImp;
		else {
			imp = new Duplicator().run(origImp);
			new StackConverter(imp).convertToGray8();
		}
		
		int nChannels = imp.getNChannels();
		int nSlices = settings.nslices;
		int nFrames = settings.nframes;
		ImagePlus[] ret = new ImagePlus[nFrames];
		int w = imp.getWidth(), h = imp.getHeight();

		ImageStack oldStack = imp.getStack();
		String oldTitle = imp.getTitle();
		
		for(int i = 0; i < nFrames; i++) {
			
			ImageStack newStack = new ImageStack(w, h);
			for(int j = 0; j < nSlices; j++) {
				int index = imp.getStackIndex(1, j+1, i+settings.tstart+1);
				Object pixels;
				if (nChannels > 1) {
					imp.setPositionWithoutUpdate(1, j+1, i+1);
					pixels = new ColorProcessor(imp.getImage()).getPixels();
				}
				else
					pixels = oldStack.getPixels(index);
				newStack.addSlice(oldStack.getSliceLabel(index), pixels);
			}
			ret[i] = new ImagePlus(oldTitle	+ " (frame " + i + ")", newStack);
			ret[i].setCalibration(imp.getCalibration().copy());
			
		}
		return ret;
	}
	

	/**
	 * Instantiate a suitable {@link SpotDisplayer} for the given model, and render it with
	 * the image content only.
	 */
	private static SpotDisplayer instantiateDisplayer(final TrackMateModelInterface model) {
		SpotDisplayer disp;
		Settings settings = model.getSettings();
		// Render image data
		boolean is3D = settings.imp.getNSlices() > 1;
		if (is3D) { 
			if (!settings.imp.isVisible())
				settings.imp.show();
			final Image3DUniverse universe = new Image3DUniverse();
			universe.show();
			ImagePlus[] images = makeImageForViewer(settings);
			Content imageContent = ContentCreator.createContent(
					settings.imp.getTitle(), 
					images, 
					Content.VOLUME, 
					DEFAULT_RESAMPLING_FACTOR, 
					0,
					null, 
					DEFAULT_THRESHOLD, 
					new boolean[] {true, true, true});
			// Render spots
			disp = new SpotDisplayer3D(universe, settings.segmenterSettings.expectedRadius); 							
			universe.addContentLater(imageContent);

		} else {
			disp = new SpotDisplayer2D(settings);
		}
		disp.render();
		return disp;
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

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		ij.ImageJ.main(args);
		TrackMateModelInterface model = new TrackMate_();
		new TrackMateFrameController(model);
	}

}
