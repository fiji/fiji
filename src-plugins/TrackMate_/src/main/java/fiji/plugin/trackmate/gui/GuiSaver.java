package fiji.plugin.trackmate.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlWriter;

/**
 * This class is in charge of writing a {@link TrackMateModel} to a file, from
 * the current state and content of the wizard {@link TrackMateWizard}.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>  2011 - 2012
 */
public class GuiSaver {

	private TrackMateWizard wizard;
	private Logger logger = Logger.VOID_LOGGER;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a {@link GuiReader}. The {@link WizardController} will have its state
	 * set according to the data found in the file read.
	 * @param wizard
	 */
	public GuiSaver(TrackMateWizard wizard) {
		this.wizard = wizard;
		logger = wizard.getLogger();
	}

	/*
	 * METHODS
	 */


	/**
	 * Write the model in the plugin managed by this GUI in the file specified.
	 * @param file  the file to write in.
	 */
	public void writeFile(final File file) {

		String log = wizard.getLogPanel().getTextContent();

		TmXmlWriter writer = new TmXmlWriter(wizard.getController().getPlugin(), log);
		
		if (!writer.checkInput()) {
			logger.error("There was some errors preparing to write:\n" + writer.getErrorMessage());
			logger.error("Aborting.\n");
			return;
		}

		if (!writer.process()) {
			logger.error("There was some errors when preparing the file:\n" + writer.getErrorMessage());
			logger.error("Aborting.\n");
			return;
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
