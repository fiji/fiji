package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.detection.LogDetectorSettings;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.HyperSliceImgPlus;

public class SingleFrame_TestDrive {
	
	public static void main(String[] args) {
		
		// Open image
		File file = new File("/Users/tinevez/Desktop/VIRUS/VIRUS snr 7 density mid-crop.tif");
		ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		
		// Set segmentation target
		int detectionChannel = 1;
		int frame = 0; // 0-based

		// Prepare detector instance
		LogDetector<UnsignedByteType> detector = new LogDetector<UnsignedByteType>();
		LogDetectorSettings<UnsignedByteType> lss = new LogDetectorSettings<UnsignedByteType>();
		lss.doSubPixelLocalization = false;
		lss.expectedRadius = 2f;
		
		// Build settings object
		Settings<UnsignedByteType> settings = new Settings<UnsignedByteType>(imp);
		settings.detectionChannel = detectionChannel;
		settings.tstart = frame;
		settings.tend = frame;
		settings.detector = detector;
		settings.detectorSettings = lss;
		
		// Feed this to the model & plugin
		TrackMateModel<UnsignedByteType> model = new TrackMateModel<UnsignedByteType>();
		model.setSettings(settings);
		model.getSpots().put(frame, new ArrayList<Spot>());
		TrackMate_<UnsignedByteType> plugin = new TrackMate_<UnsignedByteType>(model);
		
		// Segment using the plugin
//		plugin.execDetection();

		// Grab single frame as Imglib image
		ImgPlus<UnsignedByteType> img = ImagePlusAdapter.wrapImgPlus(imp);
		ImgPlus<UnsignedByteType> imgCT = HyperSliceImgPlus.fixTimeAxis(
				HyperSliceImgPlus.fixChannelAxis(img, detectionChannel-1), 
				frame);
		
		// Check to see if it is right
		ij.ImageJ.main(args);
//		 mpicbg.imglib.image.display.imagej.ImageJFunctions.copyToImagePlus(img, ImagePlus.GRAY8).show();
		
		// Segment it using individual detector
		detector.setTarget(imgCT, lss );
		if (!(detector.checkInput() && detector.process())) {
			System.err.println("Problem in detection: "+detector.getErrorMessage());
			return;
		}
		List<Spot> spots = detector.getResult();

		// Add found spots to existing model
		model.getSpots().get(frame).addAll(spots);
		
		// Filter on quality
		FeatureFilter filter = new FeatureFilter(Spot.QUALITY, 2d, true);
		settings.addSpotFilter(filter);
		plugin.execSpotFiltering();
		
		// Display results
		HyperStackDisplayer<UnsignedByteType> displayer = new HyperStackDisplayer<UnsignedByteType>();
		displayer.setModel(model);
		displayer.render();
		displayer.refresh();
	}

}
