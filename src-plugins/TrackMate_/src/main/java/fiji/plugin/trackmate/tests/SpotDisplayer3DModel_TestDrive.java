package fiji.plugin.trackmate.tests;

import java.io.File;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import ij.IJ;
import ij3d.Install_J3D;

public class SpotDisplayer3DModel_TestDrive {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(Install_J3D.getJava3DVersion());
		

		File file;
		if (!IJ.isWindows()) {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks2.xml");
		} else {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks2.xml");
		}
		ij.ImageJ.main(args);
		
		TrackMate_ plugin = new TrackMate_();
		plugin.initModules();
		TmXmlReader reader = new TmXmlReader(file, plugin);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		TrackMateModel model = plugin.getModel();
		
		SpotDisplayer3D displayer = new SpotDisplayer3D(model);
		displayer.render();
		
		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL);
		displayer.refresh();
	}

}
