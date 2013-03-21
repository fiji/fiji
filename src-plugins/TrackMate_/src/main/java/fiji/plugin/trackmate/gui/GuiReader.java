package fiji.plugin.trackmate.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;

import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v12;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.Version;
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

	/** Version above which (inclusive) we can use the current {@link TmXmlReader}. 
	 * Below this version, we use the {@link TmXmlReader_v12} reader. */ 
	private static final Version DEAL_WITH_VERSION_ABOVE = new Version("2.0.0");
	protected Logger logger = Logger.VOID_LOGGER;
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
		plugin = new TrackMate_();
		plugin.initModules();
		plugin.setLogger(logger);
		
		// Initialize a string holder so that we can cat messages when relaoding log content
		StringBuilder str = new StringBuilder("Loading XML file on:\n" + TMUtils.getCurrentTimeString()+'\n');
		String msg;

		// Open and parse file
		msg = "Opening file "+file.getName()+'\n';
		logger.log(msg);
		str.append(msg);
		TmXmlReader reader = new TmXmlReader(file, plugin);

		if (!reader.checkInput()) {
			logger.error("There was a problem opening the source file:\n" + reader.getErrorMessage() + '\n');
			logger.error("Aborting.\n");
			return;
 		} else {
 			msg = "  Parsing file done.\n";
 			logger.log(msg);
 			str.append(msg);
 		}

		// Check file version & deal with older save format
		String fileVersionStr = reader.getVersion();
		Version fileVersion;
		try {
			fileVersion = new Version(fileVersionStr);
		} catch (IllegalArgumentException iae) {
			logger.error("Cannot deal with file version: " + fileVersionStr + '\n');
			logger.error("Aborting.\n");
			return;
		}
		
		if (fileVersion.compareTo( DEAL_WITH_VERSION_ABOVE ) < 0) {
			logger.log("  Detected an older file format: v"+fileVersionStr);
			logger.log(" Converting on the fly.\n");
			// We substitute an able reader
			reader = new TmXmlReader_v12(file, plugin);
		}

		// Retrieve data and update GUI
		boolean readWasOk = reader.process();
		if (!readWasOk) {
			logger.error("There was some errors when loading the file:\n");
			logger.error(reader.getErrorMessage());
			return;
		}
		
		// Retrieve log text if any
		String log = reader.getLogText();
		if (log != null) {
			wizard.getLogPanel().setTextContent(log + "\n\n" + str.toString());
		}

		if (fileVersion.compareTo(DEAL_WITH_VERSION_ABOVE) < 0) {
			// We need to re-compute track & edge features in this case
			if (plugin.getModel().getTrackModel().getNTracks() > 0) {
				plugin.computeTrackFeatures(true);
				plugin.computeEdgeFeatures(true);
			}
		}

		// Make a new displayer
		displayer = new HyperStackDisplayer(plugin.getModel());
		
		// Send the new instance of plugin to all panel a first
		// time, required by some for proper instantiation.
		passNewPluginToWizard();

		// We now check the content of the retrieved model
		final TrackMateModel model = plugin.getModel();
		Settings settings = model.getSettings();
		ImagePlus imp = settings.imp;

		{
			// Did we get an image?
			if (null == imp) {
				// Provide a dummy empty image if linked image can't be found
				logger.log("Could not find image "+settings.imageFileName+" in "+settings.imageFolder+". Substituting dummy image.\n");
				imp = NewImage.createByteImage("Empty", settings.width, settings.height, settings.nframes * settings.nslices, NewImage.FILL_BLACK);
				imp.setDimensions(1, settings.nslices, settings.nframes);
			} else {
				logger.log("Found a proper image.\n");
			}
			
			settings.imp = imp;
			model.setSettings(settings);
			// We display it only if we have a GUI

			// Update start panel
			StartDialogPanel panel = (StartDialogPanel) wizard.getPanelDescriptorFor(StartDialogPanel.DESCRIPTOR);
			panel.setPlugin(plugin);
			panel.aboutToDisplayPanel();
		}		

		{ // Did we get a detector
			SpotDetectorFactory<?> detectorFactory = settings.detectorFactory;
			if (null == detectorFactory) {
				logger.log("Detector not found in the file.\n");
				// Stop at start panel
				targetDescriptor = StartDialogPanel.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}
			logger.log("Found a detector.\n");

			// Update 2nd panel: detector choice
			DetectorChoiceDescriptor detectorChoiceDescriptor = (DetectorChoiceDescriptor) wizard.getPanelDescriptorFor(DetectorChoiceDescriptor.DESCRIPTOR);
			detectorChoiceDescriptor.setPlugin(plugin);
			detectorChoiceDescriptor.aboutToDisplayPanel();

			// Instantiate descriptor for the detector configuration and update it
			DetectorConfigurationPanelDescriptor detectConfDescriptor = new DetectorConfigurationPanelDescriptor();
			
			detectConfDescriptor.setPlugin(plugin);
			detectConfDescriptor.setWizard(wizard);
			detectConfDescriptor.updateComponent();
			wizard.registerWizardDescriptor(DetectorConfigurationPanelDescriptor.DESCRIPTOR, detectConfDescriptor);
		}


		{ // Did we get spots?
			SpotCollection spots = model.getSpots();
			if (null == spots || spots.isEmpty()) {
				logger.log("Detected spots not found in the file.\n");
				// No spots, so we stop here, and switch to the detector panel
				targetDescriptor = DetectorConfigurationPanelDescriptor.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}
			logger.log("Found the detected spot collection.\n");

			// Next panel that needs to know is the initial filter one
			InitFilterDescriptor panel = (InitFilterDescriptor) wizard.getPanelDescriptorFor(InitFilterDescriptor.DESCRIPTOR);
			panel.aboutToDisplayPanel();
		}


		{ // Did we find a value for the initial spot filter?
			Double initialThreshold = settings.initialSpotFilterValue;
			if (initialThreshold == null) {
				logger.log("Initial filter on spots not set in the file.\n");
				// No initial threshold, so set it
				targetDescriptor = InitFilterDescriptor.DESCRIPTOR;
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}
			logger.log("Found the initial filter on spots.\n");

		}		

		/*
		 * Prepare the next panel, which is the spot filter panel.
		 * Spot filters from the file are always non-null. There are empty at worst, but we cannot say
		 * whether it is because the user did not reach the panel that sets them yet, or because
		 * he chose to have 0 filters. 
		 * It does not matter much anyway.
		 */
		SpotFilterDescriptor spotFilterDescriptor = (SpotFilterDescriptor) wizard.getPanelDescriptorFor(SpotFilterDescriptor.DESCRIPTOR);
		spotFilterDescriptor.setWizard(wizard);

		{ // Did we get filtered spots?
			SpotCollection selectedSpots = model.getFilteredSpots();
			if (null == selectedSpots || selectedSpots.isEmpty()) {
				logger.log("Filtered spots not found in the file.\n");
				// No spot selection, so we display the feature threshold GUI, with the loaded feature threshold
				// already in place.
				targetDescriptor = SpotFilterDescriptor.DESCRIPTOR;
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				spotFilterDescriptor.aboutToDisplayPanel();
				spotFilterDescriptor.displayingPanel();
				return;
			}
			logger.log("Found the filtered spot collection.\n");
			wizard.setDisplayer(displayer);
			spotFilterDescriptor.aboutToDisplayPanel();
			spotFilterDescriptor.displayingPanel();
		}

		{ // Did we get tracker settings
			SpotTracker tracker = settings.tracker;
			if (null == tracker) {
				logger.log("Tracker not found in the file.\n");
				targetDescriptor = SpotFilterDescriptor.DESCRIPTOR;
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				echoLoadingFinished();
				return;
			}
			logger.log("Found a tracker.\n");
		}


		// Update panel: tracker choice
		TrackerChoiceDescriptor trackerChoiceDescriptor = (TrackerChoiceDescriptor) wizard.getPanelDescriptorFor(TrackerChoiceDescriptor.DESCRIPTOR);
		trackerChoiceDescriptor.setCurrentChoiceFromPlugin();

		// Instantiate descriptor for the tracker configuration and update it
		TrackerConfigurationPanelDescriptor trackerDescriptor = new TrackerConfigurationPanelDescriptor();
		trackerDescriptor.setPlugin(plugin);
		trackerDescriptor.updateComponent(); // This will feed the new panel with the current settings content

		wizard.registerWizardDescriptor(TrackerConfigurationPanelDescriptor.DESCRIPTOR, trackerDescriptor);

		{ // Did we get tracks?
			int nTracks = model.getTrackModel().getNTracks();
			if (nTracks < 1) {
				logger.log("No tracks found in the file.\n");
				targetDescriptor = TrackerConfigurationPanelDescriptor.DESCRIPTOR;
				displayer.render();
				wizard.setDisplayer(displayer);
				if (!imp.isVisible())
					imp.show();
				trackerDescriptor.updateComponent();
				echoLoadingFinished();
				return;
			}
			logger.log("Found tracks.\n");
			trackerDescriptor.updateComponent();
		}
		
		/*
		 * At this level, we will find track filters and filtered tracks anyway.
		 * We we can call it a day and exit the loading GUI
		 */
		targetDescriptor = DisplayerPanel.DESCRIPTOR;
		displayer.render();
		wizard.setDisplayer(displayer);
		if (!imp.isVisible())
			imp.show();
		
		// Track filter descriptor
		TrackFilterDescriptor trackFilterDescriptor = (TrackFilterDescriptor) wizard.getPanelDescriptorFor(TrackFilterDescriptor.DESCRIPTOR);
		trackFilterDescriptor.aboutToDisplayPanel();

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
		wizard.getController().setPlugin(plugin);
	}

	private void echoLoadingFinished() {
		logger.log("Loading data finished.\n");
	}

}
