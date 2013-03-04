package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.TrackMate_;
import ij.IJ;

import java.io.File;

public class TrackMate_TestDrive {
	
	public static void main(String[] args) {
		
		File file;
		if (IJ.isWindows()) {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.tif");
		} else {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.tif");
		}
		
		ij.ImageJ.main(args);
		ij.ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		imp.show();
		
		final TrackMate_ st = new TrackMate_();
		System.out.println("Running the plugin...");
//		new Thread() {
//			public void run() {
//				try {
//					Thread.sleep(1000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				st.getModel().setLogger(fiji.plugin.trackmate.Logger.DEFAULT_LOGGER);
//			};
//		}.start();
		st.run(null); // launch the GUI;
		
	}
}
