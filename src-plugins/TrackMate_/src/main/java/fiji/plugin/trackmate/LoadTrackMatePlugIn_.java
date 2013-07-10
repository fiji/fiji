package fiji.plugin.trackmate;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.descriptors.SomeDialogDescriptor;
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

public class LoadTrackMatePlugIn_ extends SomeDialogDescriptor implements PlugIn {

	private JFrame frame;
	private static final String KEY = "LoadPlugin";


	public LoadTrackMatePlugIn_() {
		super(new LogPanel());
	}




	@Override
	public void run(final String arg0) {

		// I can't stand the metal look. If this is a problem, contact me (jeanyves.tinevez@gmail.com)
		if (IJ.isMacOSX() || IJ.isWindows()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
			} catch (final InstantiationException e) {
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
				e.printStackTrace();
			} catch (final UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		}

		if (null == file) {
			final File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
			file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
		}

		final Logger logger = Logger.IJ_LOGGER; // logPanel.getLogger();
		final File tmpFile = IOUtils.askForFileForLoading(file, "Load a TrackMate XML file", frame, logger );
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

		// Tune model and settings to be usable in the GUI even with old versions
		 if (version.compareTo(new Version("2.0.0")) < 0) {
			 settings.addEdgeAnalyzer(new EdgeTargetAnalyzer(model));
			 settings.addEdgeAnalyzer(new EdgeVelocityAnalyzer(model));
			 trackmate.computeEdgeFeatures(true);
			 model.setLogger(Logger.IJ_LOGGER);
			 trackmate.computeEdgeFeatures(true);
		 } else if (version.compareTo(new Version("2.1.0")) < 0) {
			 model.setLogger(Logger.IJ_LOGGER);
//			 trackmate.computeTrackFeatures(true);
		 }

		final TrackMateGUIController controller = new TrackMateGUIController(trackmate);

		// We feed then the reader with the providers taken from the NEW controller.
		final DetectorProvider detectorProvider = controller.getDetectorProvider();
		final TrackerProvider trackerProvider = controller.getTrackerProvider();
		final SpotAnalyzerProvider spotAnalyzerProvider = controller.getSpotAnalyzerProvider();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = controller.getEdgeAnalyzerProvider();
		final TrackAnalyzerProvider trackAnalyzerProvider = controller.getTrackAnalyzerProvider();
		reader.readSettings(settings, detectorProvider, trackerProvider,
				spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider);

		// GUI position
		GuiUtils.positionWindow(controller.getGUI(), settings.imp.getWindow());

		// GUI state
		final String guiState = reader.getGUIState();

		// Views
		final ViewProvider viewProvider = controller.getViewProvider();
		final Collection<TrackMateModelView> views = reader.getViews(viewProvider);
		for (final TrackMateModelView view : views) {
			if (view.getKey().equals(TrackScheme.KEY)) {
				final TrackScheme trackscheme = (TrackScheme) view;
				trackscheme.setSpotImageUpdater(new SpotImageUpdater(settings));
			}
		}

		if (!reader.isReadingOk()) {
			final Logger newlogger = controller.getGUI().getLogger();
			newlogger.error("Some errors occured while reading file:\n");
			newlogger.error(reader.getErrorMessage());
		}
		controller.setGUIStateString(guiState);

		// Setup and render views
		if (views.isEmpty()) { // at least one view.
			views.add(new HyperStackDisplayer(model, controller.getSelectionModel(), settings.imp));
		}
		final Map<String, Object> displaySettings = controller.getGuimodel().getDisplaySettings();
		for (final TrackMateModelView view : views) {
			for (final String key : displaySettings.keySet()) {
				controller.getGuimodel().addView(view);
				view.setDisplaySettings(key, displaySettings.get(key));
			}
			view.render();
		}

		// Text
		controller.getGUI().getLogPanel().setTextContent(logText);
		model.getLogger().log("File loaded on " + TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR);
	}


	@Override
	public void displayingPanel() {
		frame = new JFrame();
		frame.getContentPane().add(logPanel);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	/*
	 * MAIN METHOD
	 */

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		ImageJ.main(args);
		SomeDialogDescriptor.file =  new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		final LoadTrackMatePlugIn_ plugIn = new LoadTrackMatePlugIn_();
		plugIn.run(null);
	}


}
