package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView.ViewType;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class TrackVisualizerTestDrive {

	private static final long serialVersionUID = 1L;
	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
	
	public static void main(String[] args) throws JDOMException, IOException {
	
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		
		// Load objects 
		TrackMateModel model = reader.getModel();
		
		System.out.println("Found "+model.getNTracks()+" tracks.");
		for(int i=0; i<model.getNTracks(); i++) 
			System.out.println(" - "+model.trackToString(i));
		
			
		ImagePlus imp = reader.getImage();
		Settings settings = reader.getSettings();
		settings.segmenterSettings = reader.getSegmenterSettings();
		settings.imp = imp;
		
		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}
		
		// Instantiate displayer
		final TrackMateModelView displayer = AbstractTrackMateModelView.instantiateView(ViewType.HYPERSTACK_DISPLAYER, model);
		displayer.refresh();
		
		// Display Track scheme
		final TrackSchemeFrame frame = new TrackSchemeFrame(model);
		frame.setVisible(true);
		
	}
}
