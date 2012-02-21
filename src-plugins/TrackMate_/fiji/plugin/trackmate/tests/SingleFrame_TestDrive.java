package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.LogSegmenter;
import fiji.plugin.trackmate.segmentation.LogSegmenterSettings;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.List;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

public class SingleFrame_TestDrive {
	
	public static void main(String[] args) {
		
		// Open image
		File file = new File("/Users/tinevez/Desktop/VIRUS/VIRUS snr 7 density mid-crop.tif");
		ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		
		// Set segmentation target
		int segmentationChannel = 1;
		int frame = 0; // 0-based

		// Prepare segmenter instance
		LogSegmenter<UnsignedByteType> segmenter = new LogSegmenter<UnsignedByteType>();
		LogSegmenterSettings ss = new LogSegmenterSettings();
		ss.doSubPixelLocalization = false;
		ss.expectedRadius = 2.5f;
		
		// Build settings object
		Settings settings = new Settings(imp);
		settings.segmentationChannel = segmentationChannel;
		settings.tstart = frame;
		settings.tend = frame;
		settings.segmenter = segmenter;
		settings.segmenterSettings = ss;
		
		// Feed this to the model & plugin
		TrackMateModel model = new TrackMateModel();
		model.setSettings(settings);
		TrackMate_ plugin = new TrackMate_(model);
		
		// Segment using the plugin
		plugin.execSegmentation();
		System.out.println(model.getSpots());

		// Grab single frame as Imglib image
		@SuppressWarnings("unchecked")
		Image<UnsignedByteType> img = (Image<UnsignedByteType>) TMUtils.getCroppedSingleFrameAsImage(imp, frame, segmentationChannel, settings); // will be cropped according to settings
		
		// Check to see if it is right
		ij.ImageJ.main(args);
		ImageJFunctions.copyToImagePlus(img, ImagePlus.GRAY8).show();
		
		// Segment it using individual segmenter
		segmenter.setTarget(img, img.getCalibration(), ss );
		if (!(segmenter.checkInput() && segmenter.process())) {
			System.err.println("Problem in segmentation: "+segmenter.getErrorMessage());
			return;
		}
		List<Spot> spots = segmenter.getResult();

		// Add found spots to existing model
		model.getSpots().get(frame).addAll(spots);
		
		// Filter on quality
		FeatureFilter filter = new FeatureFilter(Spot.QUALITY, 20f, true);
		model.addSpotFilter(filter);
		plugin.execSpotFiltering();
		
		// Display results
		HyperStackDisplayer displayer = new HyperStackDisplayer();
		displayer.setModel(model);
		displayer.render();
		displayer.refresh();
	}

}
