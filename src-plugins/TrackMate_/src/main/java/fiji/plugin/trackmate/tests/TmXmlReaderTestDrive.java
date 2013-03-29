package fiji.plugin.trackmate.tests;

import java.io.File;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;

public class TmXmlReaderTestDrive {

	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
//	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
	//	private static final File file = new File("/Users/tinevez/Projects/ELaplantine/2011-06-29/Dish4_avg-cell1.xml");
	//	private static final File file = new File("/Users/tinevez/Projects/DMontaras/Mutant/20052011_16_20.xml");

	public static void main(String args[]) {

		//		ij.ImageJ.main(args);

		System.out.println("Opening file: "+file.getAbsolutePath());		
		TrackMate_ plugin = new TrackMate_();
		plugin.initModules();
		TmXmlReader reader = new TmXmlReader(file, plugin);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
			return;
		}
		TrackMateModel model = plugin.getModel();

		System.out.println(model);
		
//		 Instantiate displayer
		fiji.plugin.trackmate.visualization.AbstractTrackMateModelView displayer 
			= new fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer(model);
		displayer.render();
		displayer.refresh();

	}


}
