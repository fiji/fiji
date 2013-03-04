package fiji.plugin.trackmate.tests;

import fiji.SampleImageLoader;
import fiji.plugin.trackmate.TrackMate_;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.scijava.util.AppUtils;

public class TrackMate_TestDrive {
	
	public static void main(String[] args) {
		
		final File file = new File(AppUtils.getBaseDirectory(TrackMate_.class), "samples/FakeTracks.tif");
		
		new ImageJ();
		if (!file.exists()) try {
			final File parent = file.getParentFile();
			if (!parent.isDirectory()) parent.mkdirs();
			SampleImageLoader.download(new URL("http://fiji.sc/samples/FakeTracks.tif").openConnection(), file, 0, 1, true);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		ImagePlus imp = IJ.openImage(file.getAbsolutePath());
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
