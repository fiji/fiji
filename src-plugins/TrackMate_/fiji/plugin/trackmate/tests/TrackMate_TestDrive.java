package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.TrackMate_;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;

public class TrackMate_TestDrive {
	
	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.tif");
//	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.tif");

	public static void main(String[] args) {

//		System.out.println("Java3D version: "+Install_J3D.getJava3DVersion());
		
		ij.ImageJ.main(args);
		ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		imp.show();
		
		TrackMate_ st = new TrackMate_();
		System.out.println("Running the plugin...");
		st.run(null); // launch the GUI;
	}
}
