package fiji.plugin.trackmate.gui.descriptors;

import java.io.File;
import java.util.Collection;
import java.util.Map;

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
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class LoadDescriptor extends SomeDialogDescriptor {

	private static final String KEY = "Loading";
	private final TrackMate trackmate;
	private final DetectorProvider detectorProvider;
	private final TrackerProvider trackerProvider;
	private final SpotAnalyzerProvider spotAnalyzerProvider;
	private final EdgeAnalyzerProvider edgeAnalyzerProvider;
	private final TrackAnalyzerProvider trackAnalyzerProvider;
	private final TrackMateGUIController controller;
	private final ViewProvider viewProvider;

	public LoadDescriptor(TrackMateGUIController controller, DetectorProvider detectorProvider, TrackerProvider trackerProvider, 
			SpotAnalyzerProvider spotAnalyzerProvider, EdgeAnalyzerProvider edgeAnalyzerProvider, TrackAnalyzerProvider trackAnalyzerProvider,
			ViewProvider viewProvider) {
		super(controller.getGUI().getLogPanel());
		this.controller = controller;
		this.viewProvider = viewProvider;
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
		
		// Log
		String logText = reader.getLog();
		// Model
		TrackMateModel model = reader.getModel();
		// Settings
		Settings settings = reader.getSettings(detectorProvider, trackerProvider, 
				spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider);
		// GUI state
		String guiState = reader.getGUIState();
		
		/*
		 *  Re-instantiate a GUI and close the old one
		 */
		
		TrackMate newtrackmate = new TrackMate(model, settings);
		TrackMateGUIController newcontroller = new TrackMateGUIController(newtrackmate, settings.imp);
		newcontroller.getGUI().getLogPanel().setTextContent(logText);
		
		// Views
		// We need a new view provider, with the new model and settings etc...
		ViewProvider newViewProvider = viewProvider.copyOn(model, settings, newcontroller.getSelectionModel());
		Collection<TrackMateModelView> views = reader.getViews(newViewProvider);
		
		if (!reader.isReadingOk()) {
			Logger newlogger = newcontroller.getGUI().getLogger();
			newlogger.error("Some errors occured while reading file:\n");
			newlogger.error(reader.getErrorMessage());
		}
		newcontroller.setGUIStateString(guiState);

		// Setup and render views
		if (views.isEmpty()) { // at least one view.
			views.add(new HyperStackDisplayer(model, newcontroller.getSelectionModel(), settings.imp));
		}
		Map<String, Object> displaySettings = newcontroller.getGuimodel().getDisplaySettings();
		for (TrackMateModelView view : views) {
			for (String key : displaySettings.keySet()) {
				view.setDisplaySettings(key, displaySettings.get(key));
			}
			view.render();
		}

		// Close the old one
		controller.quit();
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
