package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;

import org.jdom2.JDOMException;
import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class TrackSchemeTestDrive {
	
	public static void main(String[] args) throws JDOMException, IOException {
	
		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/RECEPTOR.xml");
		
		TmXmlReader reader = new TmXmlReader(file);
		Model model = reader.getModel();
		
		System.out.println("From the XML file:");
		System.out.println("Found "+model.getTrackModel().nTracks(false)+" tracks in total.");
		System.out.println();
		
		// Instantiate displayer
		SelectionModel sm = new SelectionModel(model);
		TrackScheme trackscheme = new TrackScheme(model, sm);
		trackscheme.render();
		trackscheme.refresh();
	}
}
