package fiji.plugin.trackmate.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;

import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

/**
 * This class is in charge of reading a whole TrackMate file, and return a  
 * {@link TrackMateModel} with its field set. Optionally, 
 * it can also position correctly the state of the GUI.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Apr 28, 2011
 */
public class GuiReader {

	private Logger logger = Logger.VOID_LOGGER;
	private TrackMateWizard wizard;
	private String targetDescriptor;
	private TrackMate_ plugin;
	private TrackMateModelView displayer;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a {@link GuiReader}. The {@link WizardController} will have its state
	 * set according to the data found in the file read.
	 * @param controller
	 */
	public GuiReader(TrackMateWizard wizard) {
		this.wizard = wizard;
		if (null != wizard)
			logger = wizard.getLogger();
	}

	/*
	 * METHODS
	 */

	/**
	 * Return the new {@link TrackMate_} plugin that was instantiated and prepared when calling 
	 * the {@link #loadFile(File)} method.
	 */
	public TrackMate_ getPlugin() {
		return plugin;
	}

	/**
	 * Return the descriptor for the {@link WizardPanelDescriptor} that matches the amount of data 
	 * found in the target file. This identifier can be used to resume the tracking process
	 * to a saved state. 
	 */
	public String getTargetDescriptor() {
		return targetDescriptor;
	}

	/**
	 * Load the file and create a new {@link TrackMate_} plugin
	 * with a new model reflecting the file content.
	 * <p>
	 * Also, calling this method re-initializes all the 
	 * {@link WizardPanelDescriptor} so that they are updated with the
	 * plugin new content.
	 * @param file  the file to load
	 */
	public void loadFile(File file) {
		// Init target fields
		TrackMateModel model = new TrackMateModel();
		plugin = new TrackMate_(model);
		plugin.initModules();
		plugin.setLogger(logger);
		if (displayer ==null ) {
			displayer = new HyperStackDisplayer();
		}

		// Open and parse file
		logger.log("Opening file "+file.getName()+'\n');
		TmXmlReader reader = new TmXmlReader(file, logger);
		reader.parse();
		logger.log("  Parsing file done.\n");

		
		// Retrieve data and update GUI
		Settings settings = null;
		ImagePlus imp = null;

		// Send the new instance of plugin to all panel a first
		// time, required by some for proper instantiation.
		passNewPluginToWizard();

		{ // Read settings
			logger.log("  Loading settings...");
			settings = reader.getSettings();
			echoDone();

			// Try to read image
			logger.log("  Loading image...");
			imp = reader.getImage();		
			if (null == imp) {
				// Provide a dummy empty image if linked image can't be found
				logger.log("\nCould not find image "+settings.imageFileName+" in "+settings.imageFolder+". Substituting dummy image.\n");
				imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes * settings.nslices, NewImage.FILL_BLACK);
				imp.setDimensions(1, settings.nslices, settings.nframes);
			}

			settings.imp = imp;
			model.setSettings(settings);
			echoDone();
			// We display it only if we have a GUI

			// Update start panel
			WizardPanelDescriptor panel = wizard.getPanelDescriptorFor(StartDialogPanel.DESCRIPTOR);
			panel.aboutToDisplayPanel();
		}


		{ // Try to read segmenter settings
			logger.log("  Reading segmenter settings...");
			reader.getSegmenterSettings(settings);
			SegmenterSettings segmenterSettings = settings.segmenterSettings;
			if (null == segmenterSettings) {
				echoNotFound();
				// Stop at start panel
				targetDescriptor = StartDialogPanel.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}
			echoDone();

			// Update 2nd panel: segmenter choice
			SegmenterChoiceDescriptor segmenterChoiceDescriptor = (SegmenterChoiceDescriptor) wizard.getPanelDescriptorFor(SegmenterChoiceDescriptor.DESCRIPTOR);
			segmenterChoiceDescriptor.aboutToDisplayPanel();

			// Instantiate descriptor for the segmenter configuration and update it
			SegmenterConfigurationPanelDescriptor segmConfDescriptor = new SegmenterConfigurationPanelDescriptor();
			segmConfDescriptor.setPlugin(plugin);
			segmConfDescriptor.setWizard(wizard);
			wizard.registerWizardDescriptor(SegmenterConfigurationPanelDescriptor.DESCRIPTOR, segmConfDescriptor);
			segmConfDescriptor.aboutToDisplayPanel();
		}


		{ // Try to read spots
			logger.log("  Loading spots...");
			SpotCollection spots = reader.getAllSpots();
			if (null == spots) {
				echoNotFound();
				// No spots, so we stop here, and switch to the segmenter panel
				targetDescriptor = SegmenterConfigurationPanelDescriptor.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}

			// We have a spot field, update the model.
			model.setSpots(spots, false);
			echoDone();

			// Next panel that needs to know is the initial filter one
			InitFilterPanel panel = (InitFilterPanel) wizard.getPanelDescriptorFor(InitFilterPanel.DESCRIPTOR);
			panel.aboutToDisplayPanel();
		}


		{ // Try to read the initial threshold
			logger.log("  Loading initial spot filter...");
			FeatureFilter initialThreshold = reader.getInitialFilter();
			if (initialThreshold == null) {
				echoNotFound();
				// No initial threshold, so set it
				targetDescriptor = InitFilterPanel.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}

			// Store it in model
			model.getSettings().initialSpotFilterValue = initialThreshold.value;
			echoDone();

		}		

		// Prepare the next panel, which is the spot filter panel
		SpotFilterDescriptor spotFilterDescriptor = (SpotFilterDescriptor) wizard.getPanelDescriptorFor(SpotFilterDescriptor.DESCRIPTOR);
		spotFilterDescriptor.setPlugin(plugin);

		{ // Try to read feature thresholds
			logger.log("  Loading spot filters...");
			List<FeatureFilter> featureThresholds = reader.getSpotFeatureFilters();
			if (null == featureThresholds) {
				echoNotFound();
				// No feature thresholds, we assume we have the features calculated, and put ourselves
				// in a state such that the threshold GUI will be displayed.
				spotFilterDescriptor.aboutToDisplayPanel();
				targetDescriptor = SpotFilterDescriptor.DESCRIPTOR;
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}

			// Store thresholds in model
			model.getSettings().setSpotFilters(featureThresholds);
			spotFilterDescriptor.aboutToDisplayPanel();
			echoDone();
		}


		{ // Try to read spot selection
			logger.log("  Loading spot selection...");
			SpotCollection selectedSpots = reader.getFilteredSpots();
			if (null == selectedSpots) {
				echoNotFound();
				// No spot selection, so we display the feature threshold GUI, with the loaded feature threshold
				// already in place.
				targetDescriptor = SpotFilterDescriptor.DESCRIPTOR;
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}

			model.setFilteredSpots(selectedSpots, false);
			echoDone();
		}


		{ // Try to read tracker settings
			logger.log("  Loading tracker settings...");
			reader.getTrackerSettings(settings);
			TrackerSettings trackerSettings = settings.trackerSettings;
			if (null == trackerSettings) {
				echoNotFound();
				model.setSettings(settings);
				targetDescriptor = SpotFilterDescriptor.DESCRIPTOR;
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}

			model.setSettings(settings);
			echoDone();
		}


		// Update panel: tracker choice
		TrackerChoiceDescriptor trackerChoiceDescriptor = (TrackerChoiceDescriptor) wizard.getPanelDescriptorFor(TrackerChoiceDescriptor.DESCRIPTOR);
		trackerChoiceDescriptor.setCurrentChoiceFromPlugin();

		// Instantiate descriptor for the tracker configuration and update it
		TrackerConfigurationPanelDescriptor trackerDescriptor = new TrackerConfigurationPanelDescriptor();
		trackerDescriptor.setPlugin(plugin);
		trackerDescriptor.aboutToDisplayPanel(); // This will feed the new panel with the current settings content

		wizard.registerWizardDescriptor(TrackerConfigurationPanelDescriptor.DESCRIPTOR, trackerDescriptor);


		{ // Try reading the tracks
			logger.log("  Loading tracks...");
			SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = reader.readTrackGraph();
			if (graph == null) {
				echoNotFound();
				targetDescriptor = TrackerConfigurationPanelDescriptor.DESCRIPTOR;
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				trackerDescriptor.aboutToDisplayPanel();
				echoLoadingFinished();
				return;
			}
			model.setGraph(graph);
			model.setTrackSpots(reader.readTrackSpots(graph));
			model.setTrackEdges(reader.readTrackEdges(graph));
			reader.readTrackFeatures(model.getFeatureModel());
			trackerDescriptor.aboutToDisplayPanel();
			echoDone();
		}

		{ // Try reading track filters
			logger.log("  Loading track filters...");
			model.getSettings().setTrackFilters(reader.getTrackFeatureFilters());
			if (model.getSettings().getTrackFilters() == null) {
				echoNotFound();
				targetDescriptor = TrackFilterDescriptor.DESCRIPTOR;
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}
			echoDone();
		}


		// Track filter descriptor
		TrackFilterDescriptor trackFilterDescriptor = (TrackFilterDescriptor) wizard.getPanelDescriptorFor(TrackFilterDescriptor.DESCRIPTOR);

		{ // Try reading track selection
			logger.log("  Loading track selection...");
			model.setVisibleTrackIndices(reader.getFilteredTracks(), false);
			if (model.getVisibleTrackIndices() == null) {
				echoNotFound();
				targetDescriptor = TrackFilterDescriptor.DESCRIPTOR;
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				trackFilterDescriptor.aboutToDisplayPanel();
				echoLoadingFinished();
				return;
			}
			trackFilterDescriptor.aboutToDisplayPanel();
			echoDone();
		}

		targetDescriptor = DisplayerPanel.DESCRIPTOR;
		displayer.setModel(model);
		displayer.render();
		wizard.setDisplayer(displayer);
		if (!imp.isVisible())
			imp.show();

		// Displayer descriptor
		DisplayerPanel displayerPanel = (DisplayerPanel) wizard.getPanelDescriptorFor(DisplayerPanel.DESCRIPTOR);
		displayerPanel.aboutToDisplayPanel();

		echoLoadingFinished();
	}

	/**
	 * Set the instance of {@link TrackMateModelView} that will be used to display the loaded model.
	 * If <code>null</code> or not set, a plain {@link HyperStackDisplayer} will be used.
	 * @param displayer  the target blank (yet) displayer
	 */
	public void setDisplayer(TrackMateModelView displayer) {
		this.displayer = displayer;
	}
	

	public File askForFile(File file) {
		JFrame parent;
		if (null == wizard) 
			parent = null;
		else
			parent = wizard;

		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			FileDialog dialog =	new FileDialog(parent, "Select a TrackMate file", FileDialog.LOAD);
			dialog.setDirectory(file.getParent());
			dialog.setFile(file.getName());
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			};
			dialog.setFilenameFilter(filter);
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			if (null == selectedFile) {
				logger.log("Load data aborted.\n");
				return null;
			}
			file = new File(dialog.getDirectory(), selectedFile);

		} else {
			// use a swing file dialog on the other platforms
			JFileChooser fileChooser = new JFileChooser(file.getParent());
			fileChooser.setSelectedFile(file);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
			fileChooser.setFileFilter(filter);
			int returnVal = fileChooser.showOpenDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Load data aborted.\n");
				return null;  	    		
			}
		}
		return file;
	}


	/**
	 *  Pass new plugin instance to all panels.
	 */
	private void passNewPluginToWizard() {
		for (WizardPanelDescriptor descriptor : wizard.getWizardPanelDescriptors()) {
			descriptor.setPlugin(plugin);
		}
	}
	
	private void echoDone() {
		logger.log("\b\b\b done.\n");
	}
	
	private void echoNotFound() {
		logger.log(" Not found.\n");
	}
	
	private void echoLoadingFinished() {
		logger.log("Loading data finished.\n");
	}
	
}
