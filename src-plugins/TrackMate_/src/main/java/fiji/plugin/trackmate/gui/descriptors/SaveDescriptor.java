package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class SaveDescriptor extends SomeDialogDescriptor {

	private final TrackMate trackmate;
	private final Frame frame;
	private final DetectorProvider detectorProvider;
	private final TrackerProvider trackerProvider;

	public SaveDescriptor(LogPanel logPanel, TrackMate trackmate, DetectorProvider detectorProvider, TrackerProvider trackerProvider, Frame frame) {
		super(logPanel);
		this.trackmate = trackmate;
		this.detectorProvider = detectorProvider;
		this.trackerProvider = trackerProvider;
		this.frame = frame;
	}


	@Override
	public void displayingPanel() {

		Logger logger = logPanel.getLogger();
		logger.log("Saving data...\n", Logger.BLUE_COLOR);
		if (null == file ) {
			//	File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
			File folder = new File(trackmate.getSettings().imp.getOriginalFileInfo().directory);
			try {
				file = new File(folder.getPath() + File.separator + trackmate.getSettings().imp.getShortTitle() +".xml");
			} catch (NullPointerException npe) {
				file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
			}
		}

		// If we are to save tracks, we better ensures that track and edge features are there, even if we have to enforce it
		if (trackmate.getModel().getTrackModel().getNTracks() > 0) {
			trackmate.computeEdgeFeatures(true);
			trackmate.computeTrackFeatures(true);
		}

		File tmpFile = IOUtils.askForFile(file, frame, logger);
		if (null == tmpFile) {
			return;
		}
		file = tmpFile;

		TmXmlWriter writer = new TmXmlWriter(file);

		writer.appendLog(logPanel.getTextContent());
		writer.appendModel(trackmate.getModel());
		writer.appendSettings(trackmate.getSettings(), detectorProvider, trackerProvider);

		try {
			writer.writeToFile();
			logger.log("Data saved to: "+file.toString()+'\n');
		} catch (FileNotFoundException e) {
			logger.error("File not found:\n"+e.getMessage()+'\n');
		} catch (IOException e) {
			logger.error("Input/Output error:\n"+e.getMessage()+'\n');
		} 

	}

}
