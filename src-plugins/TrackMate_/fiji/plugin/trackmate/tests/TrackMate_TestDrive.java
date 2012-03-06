package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.TrackMate_;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;

public class TrackMate_TestDrive {
	
	public static void main(String[] args) {

//		System.out.println("Java3D version: "+Install_J3D.getJava3DVersion());
		
		File file;
		if (IJ.isWindows()) {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.tif");
		} else {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.tif");
		}
		
		ij.ImageJ.main(args);
		ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		imp.show();
		
		TrackMate_ st = new TrackMate_();
		System.out.println("Running the plugin...");
		st.run(null); // launch the GUI;
	}
}
