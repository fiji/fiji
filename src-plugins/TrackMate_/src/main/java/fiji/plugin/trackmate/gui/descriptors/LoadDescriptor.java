package fiji.plugin.trackmate.gui.descriptors;

import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class LoadDescriptor extends SomeDialogDescriptor {

	private static final String KEY = "Loading";
	private final TrackMate trackmate;
	private final DetectorProvider detectorProvider;
	private final TrackerProvider trackerProvider;
	private final SpotAnalyzerProvider spotAnalyzerProvider;
	private final EdgeAnalyzerProvider edgeAnalyzerProvider;
	private final TrackAnalyzerProvider trackAnalyzerProvider;
	private final TrackMateGUIController controller;

	public LoadDescriptor(TrackMateGUIController controller, DetectorProvider detectorProvider, TrackerProvider trackerProvider, 
			SpotAnalyzerProvider spotAnalyzerProvider, EdgeAnalyzerProvider edgeAnalyzerProvider, TrackAnalyzerProvider trackAnalyzerProvider) {
		super(controller.getGUI().getLogPanel());
		this.controller = controller;
		this.trackmate = controller.getPlugin();
		this.detectorProvider = detectorProvider;
		this.trackerProvider = trackerProvider;
		this.spotAnalyzerProvider = spotAnalyzerProvider;
		this.edgeAnalyzerProvider = edgeAnalyzerProvider;
		this.trackAnalyzerProvider = trackAnalyzerProvider;
	}


	@Override
	public void displayingPanel() {


		if (null == file) {
			try {
				File folder = new File(trackmate.getSettings().imp.getOriginalFileInfo().directory);
				file = new File(folder.getPath() + File.separator + trackmate.getSettings().imp.getShortTitle() +".xml");
			} catch (NullPointerException npe) {
				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
			}
		}

		Logger logger = logPanel.getLogger();
		File tmpFile = IOUtils.askForFileForLoading(file, controller.getGUI(), logger );
		if (null == tmpFile) {
			return;
		}
		file = tmpFile;

		// Read the file content
		TmXmlReader reader = new TmXmlReader(file);
		if (!reader.isReadingOk()) {
			logger.error(reader.getErrorMessage());
			logger.error("Aborting.\n"); // If I cannot even open the xml file, it is not worh going on.
			return;
		}
		
		String logText = reader.getLog();
		TrackMateModel model = reader.getModel();
		Settings settings = reader.getSettings(detectorProvider, trackerProvider, 
				spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider);
		String guiState = reader.getGUIState();

		if (!reader.isReadingOk()) {
			logger.error("Some errors occured while reading file:\n");
			logger.error(reader.getErrorMessage());
		}
		
		/*
		 *  Reinstantiate a GUI and close the old one
		 */
		
		TrackMate newtrackmate = new TrackMate(model, settings);
		TrackMateGUIController newcontroller = new TrackMateGUIController(newtrackmate, settings.imp);
		newcontroller.getGUI().getLogPanel().setTextContent(logText);
		if (!reader.isReadingOk()) {
			Logger newlogger = newcontroller.getGUI().getLogger();
			newlogger.error("Some errors occured while reading file:\n");
			newlogger.error(reader.getErrorMessage());
		}
		newcontroller.setGUIStateString(guiState);

		// Clsoe the old one
		controller.quit();
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
