package fiji.plugin.trackmate;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.descriptors.SomeDialogDescriptor;
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
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class LoadTrackMatePlugIn_ extends SomeDialogDescriptor implements PlugIn {

	private JFrame frame;
	private static final String KEY = "LoadPlugin";


	public LoadTrackMatePlugIn_() {
		super(new LogPanel());
	}




	@Override
	public void run(String arg0) {
		displayingPanel();
		
		if (null == file) {
			File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
			file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
		}

		Logger logger = logPanel.getLogger();
		File tmpFile = IOUtils.askForFileForLoading(file, "Load a TrackMate XML file", frame, logger );
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
		TrackMateGUIController controller = new TrackMateGUIController(trackmate);
		
		// We feed then the reader with the providers taken from the NEW controller.
		DetectorProvider detectorProvider = controller.getDetectorProvider();
		TrackerProvider trackerProvider = controller.getTrackerProvider();
		SpotAnalyzerProvider spotAnalyzerProvider = controller.getSpotAnalyzerProvider();
		EdgeAnalyzerProvider edgeAnalyzerProvider = controller.getEdgeAnalyzerProvider();
		TrackAnalyzerProvider trackAnalyzerProvider = controller.getTrackAnalyzerProvider();
		reader.readSettings(settings, detectorProvider, trackerProvider, 
				spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider);
		
		// GUI position
		GuiUtils.positionWindow(controller.getGUI(), settings.imp.getWindow());
		
		// GUI state
		String guiState = reader.getGUIState();
		
		// Views
		ViewProvider viewProvider = controller.getViewProvider();
		Collection<TrackMateModelView> views = reader.getViews(viewProvider);
		for (TrackMateModelView view : views) {
			if (view.getKey().equals(TrackScheme.KEY)) {
				TrackScheme trackscheme = (TrackScheme) view;
				trackscheme.setSpotImageUpdater(new SpotImageUpdater(settings));
			}
		}
		
		if (!reader.isReadingOk()) {
			Logger newlogger = controller.getGUI().getLogger();
			newlogger.error("Some errors occured while reading file:\n");
			newlogger.error(reader.getErrorMessage());
		}
		controller.setGUIStateString(guiState);

		// Setup and render views
		if (views.isEmpty()) { // at least one view.
			views.add(new HyperStackDisplayer(model, controller.getSelectionModel(), settings.imp));
		}
		Map<String, Object> displaySettings = controller.getGuimodel().getDisplaySettings();
		for (TrackMateModelView view : views) {
			for (String key : displaySettings.keySet()) {
				controller.getGuimodel().addView(view);
				view.setDisplaySettings(key, displaySettings.get(key));
			}
			view.render();
		}
		
		// Text
		controller.getGUI().getLogPanel().setTextContent(logText);
		model.getLogger().log("File loaded on " + TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR);
		
		// Close log
		frame.dispose();
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
	public static void main(String[] args) {
		ImageJ.main(args);
		SomeDialogDescriptor.file =  new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		LoadTrackMatePlugIn_ plugIn = new LoadTrackMatePlugIn_();
		plugIn.run(null);
	}


}
