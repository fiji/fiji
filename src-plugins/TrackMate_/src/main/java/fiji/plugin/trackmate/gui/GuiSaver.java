package fiji.plugin.trackmate.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlWriter;

/**
 * This class is in charge of writing a {@link TrackMateModel} to a file, from
 * the current state and content of the wizard {@link TrackMateWizard}.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>  2011 - 2012
 */
public class GuiSaver <T extends RealType<T> & NativeType<T>> {

	private TrackMateWizard<T> wizard;
	private Logger logger = Logger.VOID_LOGGER;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a {@link GuiReader}. The {@link WizardController} will have its state
	 * set according to the data found in the file read.
	 * @param wizard
	 */
	public GuiSaver(TrackMateWizard<T> wizard) {
		this.wizard = wizard;
		logger = wizard.getLogger();
	}

	/*
	 * METHODS
	 */


	public void writeFile(final File file, final TrackMateModel<T> model, final String targetID) {

		TmXmlWriter<T> writer = new TmXmlWriter<T>(wizard.getController().getPlugin());

		if (targetID.equals(StartDialogPanel.DESCRIPTOR) || targetID.equals(DetectorChoiceDescriptor.DESCRIPTOR) ) {

			model.setSettings( ((StartDialogPanel<T>) wizard.getPanelDescriptorFor(StartDialogPanel.DESCRIPTOR)).getSettings());
			writer.appendBasicSettings(); 
			
		} else if ( targetID.equals(DetectorConfigurationPanelDescriptor.DESCRIPTOR) ) {

				writer.appendBasicSettings();
				writer.appendDetectorSettings();

		} else if (targetID.equals(DetectorDescriptor.DESCRIPTOR) || targetID.equals(InitFilterDescriptor.DESCRIPTOR) ) {

			writer.appendBasicSettings();
			writer.appendDetectorSettings();
			writer.appendSpots();

		} else if  (targetID.equals(LaunchDisplayerDescriptor.DESCRIPTOR) || targetID.equals(DisplayerChoiceDescriptor.DESCRIPTOR) ) {

			writer.appendBasicSettings();
			writer.appendDetectorSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpots();
			
		} else if  (targetID.equals(SpotFilterDescriptor.DESCRIPTOR) || targetID.equals(TrackerChoiceDescriptor.DESCRIPTOR) ) {
			
			writer.appendBasicSettings();
			writer.appendDetectorSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendSpots();
			
		} else if  (targetID.equals(TrackerConfigurationPanelDescriptor.DESCRIPTOR) ) {

			writer.appendBasicSettings();
			writer.appendDetectorSettings();
			writer.appendTrackerSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendFilteredSpots();
			writer.appendSpots();

		} else if  (targetID.equals(TrackingDescriptor.DESCRIPTOR)) {

			writer.appendBasicSettings();
			writer.appendDetectorSettings();
			writer.appendTrackerSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendFilteredSpots();
			writer.appendTracks();
			writer.appendSpots();
			
		} else if  (targetID.equals(TrackFilterDescriptor.DESCRIPTOR) ) {

			writer.appendBasicSettings();
			writer.appendDetectorSettings();
			writer.appendTrackerSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendFilteredSpots();
			writer.appendTracks();
			writer.appendTrackFilters();
			writer.appendSpots();

		} else {
			
			writer.appendBasicSettings();
			writer.appendDetectorSettings();
			writer.appendTrackerSettings();
			writer.appendInitialSpotFilter();
			writer.appendSpotFilters();
			writer.appendFilteredSpots();
			writer.appendTracks();
			writer.appendTrackFilters();
			writer.appendFilteredTracks();
			writer.appendSpots();

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


	
}
