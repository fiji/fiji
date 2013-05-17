package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import ij3d.Image3DUniverse;
import ij3d.Install_J3D;

import java.io.File;

import org.scijava.util.AppUtils;

public class SpotDisplayer3DModel_TestDrive {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(Install_J3D.getJava3DVersion());
		


		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = reader.getModel();
		
		Image3DUniverse universe = new Image3DUniverse();
		universe.show();

		SpotDisplayer3D displayer = new SpotDisplayer3D(model, new SelectionModel(model), universe);
		displayer.render();
		
		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL);
		displayer.refresh();
		
		System.out.println(universe.getContents());
	}

}
