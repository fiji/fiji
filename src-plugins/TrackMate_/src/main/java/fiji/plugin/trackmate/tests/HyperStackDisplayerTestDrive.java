package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.action.GrabSpotImageAction;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;


public class HyperStackDisplayerTestDrive {

//	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");

	public static <T extends RealType<T> & NativeType<T>>void main(String[] args) throws JDOMException, IOException {
		
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();
		TmXmlReader<T> reader = new TmXmlReader<T>(file, plugin , Logger.DEFAULT_LOGGER);
		reader.parse();
		
		ij.ImageJ.main(args);
		
		final TrackMateModel<T> model = reader.getModel();
		GrabSpotImageAction<T> action = new GrabSpotImageAction<T>();
		action.execute(new TrackMate_<T>(model));

		// Grab spot icons
		if (null != model.getSettings().imp)
			model.getFeatureModel().computeSpotFeatures(model.getSpots());
				
		final TrackMateModelView<T> displayer = new HyperStackDisplayer<T>();
		displayer.setModel(model);
		displayer.render();
//		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD);
		displayer.setDisplaySettings(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES, true);
		
		final TrackScheme<T> trackScheme = new TrackScheme<T>(model);
		trackScheme.render();
		
	}
	
}
