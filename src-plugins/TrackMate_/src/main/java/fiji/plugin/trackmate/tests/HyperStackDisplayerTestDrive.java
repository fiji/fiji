package fiji.plugin.trackmate.tests;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import org.jdom2.JDOMException;
import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;


public class HyperStackDisplayerTestDrive {

	public static void main(String[] args) throws JDOMException, IOException {

		ij.ImageJ.main(args);

		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		TmXmlReader reader = new TmXmlReader(file);

		Model model = reader.getModel();
		Settings settings = new Settings();
		reader.readSettings(settings, null, null, null, null, null);
		ImagePlus imp = settings.imp;
		
		SelectionModel selectionModel = new SelectionModel(model);
		HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp);
		displayer.render();
		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD);
		displayer.setDisplaySettings(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES, true);
		displayer.setDisplaySettings(TrackMateModelView.KEY_SPOTS_VISIBLE, true);
		
//		TrackScheme trackScheme = new TrackScheme(model, selectionModel);
//		trackScheme.render();
		
	}
	
}
