package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;

import org.jdom2.JDOMException;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;


public class HyperStackDisplayerTestDrive {

//	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");

	public static void main(String[] args) throws JDOMException, IOException {
		
		TrackMate_ plugin = new TrackMate_();
		TmXmlReader reader = new TmXmlReader(file, plugin);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		TrackMateModel model = plugin.getModel();
		
		ij.ImageJ.main(args);
		
		if (null != plugin.getSettings().imp) {
			plugin.computeSpotFeatures(false);
		}
				
		final TrackMateModelView displayer = new HyperStackDisplayer(model, plugin.getSettings());
		displayer.render();
//		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD);
		displayer.setDisplaySettings(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES, true);
		
		final TrackScheme trackScheme = new TrackScheme(model, plugin.getSettings());
		trackScheme.render();
		
	}
	
}
