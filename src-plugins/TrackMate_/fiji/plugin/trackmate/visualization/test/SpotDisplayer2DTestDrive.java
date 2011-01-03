package fiji.plugin.trackmate.visualization.test;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Utils;
import fiji.plugin.trackmate.features.FeatureFacade;
import fiji.plugin.trackmate.gui.ThresholdGuiPanel;
import fiji.plugin.trackmate.segmentation.PeakPickerSegmenter;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.visualization.SpotDisplayer2D;
import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.DiscCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

public class SpotDisplayer2DTestDrive {

	public static void main(String[] args) {
		
		final int N_BLOBS = 50;
		final float RADIUS = 5; // µm
		final Random RAN = new Random();
		final float WIDTH = 200; // µm
		final float HEIGHT = 200; // µm
		final float[] CALIBRATION = new float[] {0.5f, 0.5f}; 
		
		// Create 2D image
		System.out.println("Creating image....");
		Image<UnsignedByteType> img = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
		).createImage(new int[] {(int) (WIDTH/CALIBRATION[0]), (int) (HEIGHT/CALIBRATION[1])}); 

		// Random blobs
		float[] radiuses = new float[N_BLOBS];
		ArrayList<float[]> centers = new ArrayList<float[]>(N_BLOBS);
		int[] intensities = new int[N_BLOBS]; 
		for (int i = 0; i < N_BLOBS; i++) {
			radiuses[i] = (float) (RADIUS + RAN.nextGaussian());
			float x = WIDTH * RAN.nextFloat();
			float y = HEIGHT * RAN.nextFloat();
			centers.add(i, new float[] {x, y});
			intensities[i] = RAN.nextInt(200);
		}
		
		// Put the blobs in the image
		DiscCursor<UnsignedByteType> cursor;
		float[] center;
		int intensity;
		float radius;
		for (int i = 0; i < N_BLOBS; i++) {
			center = centers.get(i);
			intensity = intensities[i];
			radius = radiuses[i];
			cursor = new DiscCursor<UnsignedByteType>(img, center, radius, CALIBRATION);
			while(cursor.hasNext()) 
				cursor.next().set(intensity);		
			cursor.close();		
		}

		// Cast to ImagePlus
		ij.ImageJ.main(args);
		ImagePlus imp = ImageJFunctions.copyToImagePlus(img);
		imp.show();
		System.out.println("Creating image done.");
		
		SegmenterSettings segSettings = new SegmenterSettings();
		segSettings.expectedRadius = RADIUS;
//		SpotSegmenter<UnsignedByteType> segmenter = new LogSegmenter<UnsignedByteType>();
		SpotSegmenter<UnsignedByteType> segmenter = new PeakPickerSegmenter<UnsignedByteType>(segSettings);
		segmenter.setCalibration(CALIBRATION);
		segmenter.setImage(img);
		List<Spot> spots;
		System.out.println("Segmenting...");
		if (segmenter.checkInput() && segmenter.process())
			spots = segmenter.getResult();
		else {
			System.out.println("Problem with segmentation:\n"+segmenter.getErrorMessage());
			return;
		}
		System.out.println("Segmentation done. Found "+spots.size()+" spots.");
		
		TreeMap<Integer, Collection<Spot>> allSpots = new TreeMap<Integer, Collection<Spot>>();
		allSpots.put(0, spots);
		
		System.out.println("Calculating features..");
		FeatureFacade<UnsignedByteType> featureCalculator = new FeatureFacade<UnsignedByteType>(img, RADIUS, CALIBRATION);
		featureCalculator.processFeature(Feature.MEAN_INTENSITY, spots);
		System.out.println("Features done.");

		imp.getCalibration().pixelWidth = CALIBRATION[0];
		imp.getCalibration().pixelHeight = CALIBRATION[1];
		final TreeMap<Integer, List<Spot>> allNodes = new TreeMap<Integer, List<Spot>>();
		allNodes.put(0, spots);
		
		// Prepare the settings object that will be passed to the displayer 
		Settings settings = new Settings();
		settings.segmenterSettings = segSettings;
		settings.imp = imp;
		settings.dx = CALIBRATION[0];
		settings.dy = CALIBRATION[1];
		
		final SpotDisplayer2D displayer = new SpotDisplayer2D(settings);
		displayer.setSpots(allNodes);
		displayer.render();
		
		System.out.println("Starting threshold GUI...");
		final ThresholdGuiPanel gui = new ThresholdGuiPanel(Utils.getFeatureValues(allSpots.values()));
		gui.addThresholdPanel(Feature.MEAN_INTENSITY);
		JFrame frame = new JFrame();
		frame.getContentPane().add(gui);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		System.out.println("Done.");
		
		gui.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				displayer.setSpotsToShow(Utils.thresholdSpots(allNodes, gui.getFeatureThresholds()));
				displayer.refresh();
			}
		});
		
		gui.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displayer.setColorByFeature(gui.getColorByFeature());
			}
		});
		
	}

}
