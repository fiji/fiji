package fiji.plugin.trackmate.tests;

import java.util.Iterator;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.providers.DetectorProvider;
import ij.ImagePlus;
import ij.gui.NewImage;

public class ConcurrentSpotTestDrive {

	public static void main(String[] args) {
		
		int nFrames = 20;
		
		TrackMateModel model = new TrackMateModel();
		
		// Create blank image
		ImagePlus imp = NewImage.createByteImage("Noise", 200, 200, nFrames, NewImage.FILL_BLACK);
		
		// Add noise to it
		for (int i = 0; i < imp.getStackSize(); i++) {
			imp.getStack().getProcessor(i+1).noise(50);
		}
		
		// Setup calibration
		imp.setDimensions(1, 1, nFrames);
		
		// Run track mate on it
		
		// Make settings
		Settings settings = new Settings();
		settings.setFrom(imp);
		DetectorProvider provider = new DetectorProvider(model);
		provider.select(LogDetectorFactory.DETECTOR_KEY);
		settings.detectorFactory = provider.getDetectorFactory();
		settings.detectorSettings = provider.getDefaultSettings();
		
		// Execute detection
		TrackMate trackmate = new TrackMate(model, settings);
		trackmate.execDetection();
		
		// Retrieve spots
		SpotCollection spots = trackmate.getModel().getSpots();
		
		// Parse spots and detect duplicate IDs
		int[] IDs = new int[Spot.IDcounter.get()];
		Iterator<Spot> it = spots.iterator(false);
		while(it.hasNext()) {
			Spot si = it.next();
			int id = si.ID();
			IDs[id]++;
		}
		
		boolean ok = true;
		for (int i = 0; i < IDs.length; i++) {
			if (IDs[i] > 1) {
				System.out.println("Found "+IDs[i]+" spots with the same ID = "+i);
				ok = false;
			}
		}
		if (ok) {
			System.out.println("No duplicate ID found.");
		}
			
	}

}
