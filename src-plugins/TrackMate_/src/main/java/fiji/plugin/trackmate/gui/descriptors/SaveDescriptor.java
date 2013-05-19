package fiji.plugin.trackmate.gui.descriptors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class SaveDescriptor extends SomeDialogDescriptor {

	private static final String KEY = "Saving";
	private final TrackMate trackmate;
	private final DetectorProvider detectorProvider;
	private final TrackerProvider trackerProvider;
	private final TrackMateGUIController controller;

	public SaveDescriptor(TrackMateGUIController controller, DetectorProvider detectorProvider, TrackerProvider trackerProvider) {
		super(controller.getGUI().getLogPanel());
		this.trackmate = controller.getPlugin();
		this.controller = controller;
		this.detectorProvider = detectorProvider;
		this.trackerProvider = trackerProvider;
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

		File tmpFile = IOUtils.askForFileForSaving(file, controller.getGUI(), logger);
		if (null == tmpFile) {
			return;
		}
		file = tmpFile;
		
		/*
		 * Write model, settings and GUI state
		 */

		TmXmlWriter writer = new TmXmlWriter(file);

		writer.appendLog(logPanel.getTextContent());
		writer.appendModel(trackmate.getModel());
		writer.appendSettings(trackmate.getSettings(), detectorProvider, trackerProvider);
		writer.appendGUIState(controller.getGuimodel());

		try {
			writer.writeToFile();
			logger.log("Data saved to: "+file.toString()+'\n');
		} catch (FileNotFoundException e) {
			logger.error("File not found:\n"+e.getMessage()+'\n');
			return;
		} catch (IOException e) {
			logger.error("Input/Output error:\n"+e.getMessage()+'\n');
			return;
		}
				
	}
	
	@Override
	public String getKey() {
		return KEY;
	}

}
