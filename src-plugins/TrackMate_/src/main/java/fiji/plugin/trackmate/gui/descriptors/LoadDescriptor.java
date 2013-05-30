package fiji.plugin.trackmate.gui.descriptors;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class LoadDescriptor extends SomeDialogDescriptor {

	private static final String KEY = "Loading";
	private final TrackMate trackmate;
	private final TrackMateGUIController controller;

	public LoadDescriptor(TrackMateGUIController controller) {
		super(controller.getGUI().getLogPanel());
		this.controller = controller;
		this.trackmate = controller.getPlugin();
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
			logger.error("Aborting.\n"); // If I cannot even open the xml file, it is not work going on.
			return;
		}
		
		// Log
		String logText = reader.getLog() + '\n';
		// Model
		Model model = reader.getModel();
		// Settings -> empty for now.
		Settings settings = new Settings();
		
		// With this we can create a new controller from the provided one:
		TrackMate trackmate = new TrackMate(model, settings);
		TrackMateGUIController newcontroller = controller.createOn(trackmate);
		
		// We feed then the reader with the providers taken from the NEW controller.
		DetectorProvider detectorProvider = newcontroller.getDetectorProvider();
		TrackerProvider trackerProvider = newcontroller.getTrackerProvider();
		SpotAnalyzerProvider spotAnalyzerProvider = newcontroller.getSpotAnalyzerProvider();
		EdgeAnalyzerProvider edgeAnalyzerProvider = newcontroller.getEdgeAnalyzerProvider();
		TrackAnalyzerProvider trackAnalyzerProvider = newcontroller.getTrackAnalyzerProvider();
		reader.readSettings(settings, detectorProvider, trackerProvider, 
				spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider);
		
		// GUI position
		GuiUtils.positionWindow(newcontroller.getGUI(), settings.imp.getWindow());
		
		// GUI state
		String guiState = reader.getGUIState();
		
		// Views
		ViewProvider viewProvider = newcontroller.getViewProvider();
		Collection<TrackMateModelView> views = reader.getViews(viewProvider);
		
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
				newcontroller.getGuimodel().addView(view);
				view.setDisplaySettings(key, displaySettings.get(key));
			}
			view.render();
		}

		// Close the old one
		controller.quit();

		// Text
		newcontroller.getGUI().getLogPanel().setTextContent(logText);
		model.getLogger().log("File loaded on " + TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR);
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
