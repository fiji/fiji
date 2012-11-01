package fiji.plugin.trackmate.tests;

import ij.IJ;

import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.gui.GuiReader;
import fiji.plugin.trackmate.gui.LoadDescriptor;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.WizardController;

public class GUIReader_TestDrive {
	
	
	public static void main(String args[]) {

		File file;
		if (IJ.isWindows()) {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
		} else {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
		}
		
		ij.ImageJ.main(args);
		
		TrackMate_ plugin = new TrackMate_();
		plugin.setLogger(Logger.DEFAULT_LOGGER);

		plugin.initModules();
		
		WizardController controller = new WizardController(plugin);
		TrackMateWizard wizard = controller.getWizard();
		
		wizard.showDescriptorPanelFor(LoadDescriptor.DESCRIPTOR);
		
		
		GuiReader greader = new GuiReader(wizard);
		file = greader.askForFile(file);

		System.out.println("Opening file: "+file.getAbsolutePath());		
		
		plugin = greader.getPlugin();
		greader.loadFile(file);
		
		wizard.showDescriptorPanelFor(DisplayerPanel.DESCRIPTOR);
		
		
		
		
	}

}
