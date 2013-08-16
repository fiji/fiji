package fiji.plugin.trackmate.gui.descriptors;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v12;
import fiji.plugin.trackmate.io.TmXmlReader_v20;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.Version;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class LoadDescriptor extends SomeDialogDescriptor {

	private static final String KEY = "Loading";
	private final TrackMate trackmate;
	private final TrackMateGUIController controller;

	public LoadDescriptor(final TrackMateGUIController controller) {
		super(controller.getGUI().getLogPanel());
		this.controller = controller;
		this.trackmate = controller.getPlugin();
	}


	@Override
	public void displayingPanel() {

		if (null == file) {
			try {
				final File folder = new File(trackmate.getSettings().imp.getOriginalFileInfo().directory);
				file = new File(folder.getPath() + File.separator + trackmate.getSettings().imp.getShortTitle() +".xml");
			} catch (final NullPointerException npe) {
				final File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
			}
		}

		final Logger logger = logPanel.getLogger();
		final File tmpFile = IOUtils.askForFileForLoading(file, "Load a TrackMate XML file", controller.getGUI(), logger );
		if (null == tmpFile) {
			return;
		}
		file = tmpFile;

		// Read the file content
		TmXmlReader reader = new TmXmlReader(file);
		final Version version = new Version(reader.getVersion());
		if (version.compareTo(new Version("2.0.0")) < 0) {
			logger.log("Detecting a file version " + version +". Using the right reader.\n", Logger.GREEN_COLOR);
			reader = new TmXmlReader_v12(file);
		} else if (version.compareTo(new Version("2.1.0")) < 0) {
			logger.log("Detecting a file version " + version +". Using the right reader.\n", Logger.GREEN_COLOR);
			reader = new TmXmlReader_v20(file);
		}
		if (!reader.isReadingOk()) {
			logger.error(reader.getErrorMessage());
			logger.error("Aborting.\n"); // If I cannot even open the xml file, it is not worth going on.
			return;
		}

		// Log
		final String logText = reader.getLog() + '\n';
		// Model
		final Model model = reader.getModel();
		// Settings -> empty for now.
		final Settings settings = new Settings();

		// With this we can create a new controller from the provided one:
		final TrackMate trackmate = new TrackMate(model, settings);
		final TrackMateGUIController newcontroller = controller.createOn(trackmate);

		// We feed then the reader with the providers taken from the NEW controller.
		final DetectorProvider detectorProvider = newcontroller.getDetectorProvider();
		final TrackerProvider trackerProvider = newcontroller.getTrackerProvider();
		final SpotAnalyzerProvider spotAnalyzerProvider = newcontroller.getSpotAnalyzerProvider();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = newcontroller.getEdgeAnalyzerProvider();
		final TrackAnalyzerProvider trackAnalyzerProvider = newcontroller.getTrackAnalyzerProvider();
		reader.readSettings(settings, detectorProvider, trackerProvider,
				spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider);

		// GUI position
		GuiUtils.positionWindow(newcontroller.getGUI(), settings.imp.getWindow());

		// GUI state
		final String guiState = reader.getGUIState();

		// Views
		final ViewProvider viewProvider = newcontroller.getViewProvider();
		final Collection<TrackMateModelView> views = reader.getViews(viewProvider);
		for (final TrackMateModelView view : views) {
			if (view.getKey().equals(TrackScheme.KEY)) {
				final TrackScheme trackscheme = (TrackScheme) view;
				trackscheme.setSpotImageUpdater(new SpotImageUpdater(settings));
			}
		}

		if (!reader.isReadingOk()) {
			final Logger newlogger = newcontroller.getGUI().getLogger();
			newlogger.error("Some errors occured while reading file:\n");
			newlogger.error(reader.getErrorMessage());
		}
		newcontroller.setGUIStateString(guiState);

		// Setup and render views
		if (views.isEmpty()) { // at least one view.
			views.add(new HyperStackDisplayer(model, newcontroller.getSelectionModel(), settings.imp));
		}
		final Map<String, Object> displaySettings = newcontroller.getGuimodel().getDisplaySettings();
		for (final TrackMateModelView view : views) {
			for (final String key : displaySettings.keySet()) {
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
