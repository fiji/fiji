package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.DogSegmenter;
import fiji.plugin.trackmate.segmentation.DogSegmenterSettings;
import ij.ImagePlus;
import ij.gui.NewImage;

public class ConcurrentSpotTestDrive {

	/**
	 * @param args
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {
		
		int nFrames = 20;
		
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
		Settings settings = new Settings(imp);
		settings.segmenter = new DogSegmenter();
		DogSegmenterSettings dss = new DogSegmenterSettings();
		dss.expectedRadius = 2f;
		settings.segmenterSettings = dss;

		// Instantiate plugin
		TrackMate_ plugin = new TrackMate_(settings);
		
		// Execute segmentation
		plugin.execSegmentation();
		
		// Retrieve spots
		SpotCollection spots = plugin.getModel().getSpots();
		
		// Parse spots and detect duplicate IDs
		int[] IDs = new int[SpotImp.IDcounter.get()];
		for (Spot spot : spots) {
			SpotImp si = (SpotImp) spot;
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
