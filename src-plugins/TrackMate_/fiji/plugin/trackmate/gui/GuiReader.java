package fiji.plugin.trackmate.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;

import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdom.JDOMException;
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
	 * The newly loaded plugin will be accessible through
	 *  Also, calling this method re-initializes all the 
	 *  {@link WizardPanelDescriptor} so that they are updated with the
	 *  plugin new content.
	 * @param file  the file to load
	 */
	public void loadFile(File file) {

		// Init target fields
		TrackMateModel model = new TrackMateModel();
		plugin = new TrackMate_(model);
		plugin.setLogger(logger);

		// Open and parse file
		logger.log("Opening file "+file.getName()+'\n');
		TmXmlReader reader = new TmXmlReader(file, logger);
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


		// Retrieve data and update GUI
		Settings settings = null;
		ImagePlus imp = null;

		// Send the new instance of plugin to all panel a first
		// time, required by some for proper instantiation.
		passNewPluginToWizard();

		{ // Read settings
			settings = reader.getSettings();
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
			model.setSettings(settings);
			logger.log("  Reading image done.\n");
			// We display it only if we have a GUI

			// Update start panel
			WizardPanelDescriptor panel = wizard.getPanelDescriptorFor(StartDialogPanel.DESCRIPTOR);
			panel.aboutToDisplayPanel();
		}


		{ // Try to read segmenter settings
			reader.getSegmenterSettings(settings);
			SegmenterSettings segmenterSettings = settings.segmenterSettings;
			if (null == segmenterSettings) {
				// Stop at start panel
				targetDescriptor = StartDialogPanel.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				logger.log("Loading data finished.\n");
				return;
			}

			logger.log("  Reading segmenter settings done.\n");

			// Update 2nd panel: segmenter choice
			SegmenterChoiceDescriptor segmenterChoiceDescriptor = (SegmenterChoiceDescriptor) wizard.getPanelDescriptorFor(SegmenterChoiceDescriptor.DESCRIPTOR);
			segmenterChoiceDescriptor.aboutToDisplayPanel();

			// Instantiate descriptor for the segmenter configuration and update it
			SegmenterConfigurationPanelDescriptor segmConfDescriptor = new SegmenterConfigurationPanelDescriptor();
			segmConfDescriptor.setPlugin(plugin);
			wizard.registerWizardDescriptor(SegmenterConfigurationPanelDescriptor.DESCRIPTOR, segmConfDescriptor);
			segmConfDescriptor.aboutToDisplayPanel();
		}


		{ // Try to read spots
			SpotCollection spots = reader.getAllSpots();
			if (null == spots) {
				// No spots, so we stop here, and switch to the segmenter panel
				targetDescriptor = SegmenterConfigurationPanelDescriptor.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				logger.log("Loading data finished.\n");
				return;
			}

			// We have a spot field, update the model.
			model.setSpots(spots, false);
			logger.log("  Reading spots done.\n");

			// Next panel that needs to know is the initial filter one
			InitFilterPanel panel = (InitFilterPanel) wizard.getPanelDescriptorFor(InitFilterPanel.DESCRIPTOR);
			panel.aboutToDisplayPanel();
		}


		{ // Try to read the initial threshold
			FeatureFilter initialThreshold = reader.getInitialFilter();
			if (initialThreshold == null) {
				// No initial threshold, so set it
				targetDescriptor = InitFilterPanel.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				logger.log("Loading data finished.\n");
				return;
			}

			// Store it in model
			model.setInitialSpotFilterValue(initialThreshold.value);
			logger.log("  Reading initial spot filter done.\n");

		}		

		// Prepare the next panel, which is the spot filter panel
		SpotFilterDescriptor spotFilterDescriptor = (SpotFilterDescriptor) wizard.getPanelDescriptorFor(SpotFilterDescriptor.DESCRIPTOR);
		spotFilterDescriptor.setPlugin(plugin);

		{ // Try to read feature thresholds
			List<FeatureFilter> featureThresholds = reader.getSpotFeatureFilters();
			if (null == featureThresholds) {
				// No feature thresholds, we assume we have the features calculated, and put ourselves
				// in a state such that the threshold GUI will be displayed.
				spotFilterDescriptor.aboutToDisplayPanel();
				targetDescriptor = SpotFilterDescriptor.DESCRIPTOR;
				TrackMateModelView displayer = new HyperStackDisplayer();
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				logger.log("Loading data finished.\n");
				return;
			}

			// Store thresholds in model
			model.setSpotFilters(featureThresholds);
			spotFilterDescriptor.aboutToDisplayPanel();
			logger.log("  Reading spot filters done.\n");
		}


		{ // Try to read spot selection
			SpotCollection selectedSpots = reader.getFilteredSpots(model.getSpots());
			if (null == selectedSpots) {
				// No spot selection, so we display the feature threshold GUI, with the loaded feature threshold
				// already in place.
				targetDescriptor = SpotFilterDescriptor.DESCRIPTOR;
				TrackMateModelView displayer = new HyperStackDisplayer();
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				logger.log("Loading data finished.\n");
				return;
			}

			model.setFilteredSpots(selectedSpots, false);
			logger.log("  Reading spot selection done.\n");
		}


		{ // Try to read tracker settings
			reader.getTrackerSettings(settings);
			TrackerSettings trackerSettings = settings.trackerSettings;
			if (null == trackerSettings) {
				model.setSettings(settings);
				targetDescriptor = SpotFilterDescriptor.DESCRIPTOR;
				TrackMateModelView displayer = new HyperStackDisplayer();
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				logger.log("Loading data finished.\n");
				return;
			}

			model.setSettings(settings);
			logger.log("  Reading tracker settings done.\n");
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
			SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = reader.readTracks(model.getFilteredSpots());
			if (graph == null) {
				targetDescriptor = TrackerConfigurationPanelDescriptor.DESCRIPTOR;
				TrackMateModelView displayer = new HyperStackDisplayer();
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				trackerDescriptor.aboutToDisplayPanel();
				logger.log("Loading data finished.\n");
				return;
			}
			model.setGraph(graph);
			trackerDescriptor.aboutToDisplayPanel();
			logger.log("  Reading tracks done.\n");
		}

		{ // Try reading track filters
			model.setTrackFilters(reader.getTrackFeatureFilters());
			if (model.getTrackFilters() == null) {
				targetDescriptor = TrackFilterDescriptor.DESCRIPTOR;
				TrackMateModelView displayer = new HyperStackDisplayer();
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				logger.log("Loading data finished.\n");
				return;
			}
			logger.log("  Reading track filters done.\n");
		}


		// Track filter descriptor
		TrackFilterDescriptor trackFilterDescriptor = (TrackFilterDescriptor) wizard.getPanelDescriptorFor(TrackFilterDescriptor.DESCRIPTOR);

		{ // Try reading track selection
			model.setVisibleTrackIndices(reader.getFilteredTracks(), false);
			if (model.getVisibleTrackIndices() == null) {
				targetDescriptor = TrackFilterDescriptor.DESCRIPTOR;
				TrackMateModelView displayer = new HyperStackDisplayer();
				displayer.setModel(model);
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				trackFilterDescriptor.aboutToDisplayPanel();
				logger.log("Loading data finished.\n");
				return;
			}
			trackFilterDescriptor.aboutToDisplayPanel();
			logger.log("  Reading track selection done.\n");
		}

		targetDescriptor = DisplayerPanel.DESCRIPTOR;
		TrackMateModelView displayer = new HyperStackDisplayer();
		displayer.setModel(model);
		displayer.render();
		wizard.setDisplayer(displayer);
		if (!imp.isVisible())
			imp.show();

		// Displayer descriptor
		DisplayerPanel displayerPanel = (DisplayerPanel) wizard.getPanelDescriptorFor(DisplayerPanel.DESCRIPTOR);
		displayerPanel.aboutToDisplayPanel();

		logger.log("Loading data finished.\n");
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
}
