package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Frame;
import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.GuiReader;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class LoadDescriptor extends SomeDialogDescriptor {

	private final TrackMate trackmate;
	private final Frame frame;
	private final DetectorProvider detectorProvider;
	private final TrackerProvider trackerProvider;
	private final SpotAnalyzerProvider spotAnalyzerProvider;
	private final EdgeAnalyzerProvider edgeAnalyzerProvider;
	private final TrackAnalyzerProvider trackAnalyzerProvider;

	public LoadDescriptor(LogPanel logPanel, TrackMate trackmate, DetectorProvider detectorProvider, TrackerProvider trackerProvider, 
			SpotAnalyzerProvider spotAnalyzerProvider, EdgeAnalyzerProvider edgeAnalyzerProvider, TrackAnalyzerProvider trackAnalyzerProvider, 
			Frame frame) {
		super(logPanel);
		this.trackmate = trackmate;
		this.detectorProvider = detectorProvider;
		this.trackerProvider = trackerProvider;
		this.spotAnalyzerProvider = spotAnalyzerProvider;
		this.edgeAnalyzerProvider = edgeAnalyzerProvider;
		this.trackAnalyzerProvider = trackAnalyzerProvider;
		this.frame = frame;
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
		File tmpFile = IOUtils.askForFile(file, frame, logger );
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

		if (!reader.isReadingOk()) {
			logger.error(reader.getErrorMessage());
		}
		
		// Determine most likely GUI state.
		
		// TODO
	}

}
