package fiji.plugin.trackmate.tests;

import fiji.SampleImageLoader;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMatePlugIn_;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.scijava.util.AppUtils;

public class TrackMate_TestDrive {
	
	public static void main(String[] args) {
		
		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.tif");
		
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
//		IJ.run(imp, "Properties...", "channels=1 slices=1 frames=200 unit=um pixel_width=1.0000 pixel_height=1.0000 voxel_depth=1.0000 frame=[0 sec] origin=0,0");
		imp.show();
//		IJ.runMacro("makeRectangle(632, 323, 124, 98);");
//		IJ.run(imp, "Crop", "");
//		IJ.run(imp, "Subtract Background...", "rolling=50 stack");
		
		final TrackMatePlugIn_ plugin = new TrackMatePlugIn_();
		System.out.println("Running the trackmate...");
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
		plugin.run(null); // launch the GUI;
		
	}
}
