package fiji.plugin.trackmate.tests;

import fiji.SampleImageLoader;
import fiji.plugin.trackmate.ManualTrackingPlugIn_;
import fiji.plugin.trackmate.TrackMate;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.scijava.util.AppUtils;

public class ManualTrackingPlugIn_TestDrive {

	public static void main(final String[] args) {
		ImageJ.main(args);

		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.tif");

		if (!file.exists()) try {
			final File parent = file.getParentFile();
			if (!parent.isDirectory()) parent.mkdirs();
			SampleImageLoader.download(new URL("http://fiji.sc/samples/FakeTracks.tif").openConnection(), file, 0, 1, true);
		} catch (final IOException e) {
			e.printStackTrace();
			return;
		}
		final ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		imp.show();

		final ManualTrackingPlugIn_ plugin = new ManualTrackingPlugIn_();
		plugin.run(null); // launch the GUI;
	}

}
