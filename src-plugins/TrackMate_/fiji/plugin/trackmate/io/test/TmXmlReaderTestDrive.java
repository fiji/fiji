package fiji.plugin.trackmate.io.test;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class TmXmlReaderTestDrive {

	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
	
	public static void main(String args[]) {
		
		ij.ImageJ.main(args);
		
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = null;
		// Parse
		try {
			reader.parse();
			model = reader.getModel();
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(model.getSettings());
		System.out.println(model.getSettings().segmenterSettings);
		System.out.println(model.getSettings().trackerSettings);
		
		// Instantiate displayer
		TrackMateModelView displayer = new HyperStackDisplayer(model);
		displayer.render();
		displayer.setSpots(model.getSpots());
		displayer.setSpotsToShow(model.getFilteredSpots());
		displayer.refresh();
		displayer.setTrackGraph(model.getTrackGraph());
		
		
	}
	
	
}
