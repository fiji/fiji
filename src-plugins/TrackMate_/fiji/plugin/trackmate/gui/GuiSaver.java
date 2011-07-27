package fiji.plugin.trackmate.gui;

import ij.IJ;

import java.awt.FileDialog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.TrackMateFrame.PanelCard;
import fiji.plugin.trackmate.gui.TrackMateFrameController.GuiState;
import fiji.plugin.trackmate.io.TmXmlWriter;

/**
 * This class is in charge of writing a {@link TrackMateModel} to a file, from
 * the current state and content of the controller {@link TrackMateFrameController}.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Apr 28, 2011
 */
public class GuiSaver {

	private TrackMateFrameController controller;
	private Logger logger = Logger.VOID_LOGGER;
	
	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Construct a {@link GuiReader}. The {@link TrackMateFrameController} will have its state
	 * set according to the data found in the file read.
	 * @param controller
	 */
	public GuiSaver(TrackMateFrameController controller) {
		this.controller = controller;
		logger = controller.getView().getLogger();
	}
	
	
	/*
	 * METHODS
	 */
	
	
	public void writeFile(File file) {
		
		TrackMateModel model = controller.getPlugin().getModel();
		GuiState state = controller.getState();
		
		TmXmlWriter writer = new TmXmlWriter(model, logger);
		switch (state) {
		case START:
			model.setSettings(((StartDialogPanel) controller.getView().getPanelFor(PanelCard.START_DIALOG_KEY)).getSettings());
			writer.appendBasicSettings();
			break;
		case TUNE_SEGMENTER:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			break;
		case SEGMENTING:
		case INITIAL_THRESHOLDING:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendSpots();
			break;		
		case CALCULATE_FEATURES:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpots();
			break;
		case TUNE_SPOT_FILTERS:
		case FILTER_SPOTS:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendSpots();
			break;
		case TUNE_TRACKER:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendTrackerSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendFilteredSpots();
			writer.appendSpots();
			break;
		case TRACKING:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendTrackerSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendFilteredSpots();
			writer.appendTracks();
			writer.appendSpots();
			break;
		case TUNE_TRACK_FILTERS:
		case FILTER_TRACKS:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendTrackerSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendFilteredSpots();
			writer.appendTracks();
			writer.appendTrackFilters();
			writer.appendSpots();
			break;
		case TUNE_DISPLAY:
			writer.appendBasicSettings();
			writer.appendSegmenterSettings();
			writer.appendTrackerSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendFilteredSpots();
			writer.appendTracks();
			writer.appendTrackFilters();
			writer.appendFilteredTracks();
			writer.appendSpots();
			break;
		}
		try {
			writer.writeToFile(file);
			logger.log("Data saved to: "+file.toString()+'\n');
		} catch (FileNotFoundException e) {
			logger.error("File not found:\n"+e.getMessage()+'\n');
		} catch (IOException e) {
			logger.error("Input/Output error:\n"+e.getMessage()+'\n');
		} 
	}
	
	
	public File askForFile(File file) {
		JFrame parent;
		if (null == controller) 
			parent = null;
		else
			parent = controller.getView();
		
		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			FileDialog dialog =	new FileDialog(parent, "Save to a TrackMate file", FileDialog.SAVE);
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
				logger.log("Save data aborted.\n");
				return null;
			}
			if (!selectedFile.endsWith(".xml"))
				selectedFile += ".xml";
			file = new File(dialog.getDirectory(), selectedFile);
		} else {
			JFileChooser fileChooser = new JFileChooser(file.getParent());
			fileChooser.setSelectedFile(file);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
			fileChooser.setFileFilter(filter);

			int returnVal = fileChooser.showSaveDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Save data aborted.\n");
				return null;  	    		
			}
		}
		return file;
	}
}
