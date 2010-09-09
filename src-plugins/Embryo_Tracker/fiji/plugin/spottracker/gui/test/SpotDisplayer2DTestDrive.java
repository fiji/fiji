package fiji.plugin.spottracker.gui.test;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.features.FeatureFacade;
import fiji.plugin.spottracker.gui.SpotDisplayer2D;
import fiji.plugin.spottracker.gui.ThresholdGuiPanel;
import fiji.plugin.spottracker.segmentation.SpotSegmenter;
import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.HyperSphereIterator;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

public class SpotDisplayer2DTestDrive {

	public static void main(String[] args) {
		
		final int N_BLOBS = 50;
		final float RADIUS = 10; // µm
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
		final LocalizableByDimCursor<UnsignedByteType> centerCursor = img.createLocalizableByDimCursor();
		HyperSphereIterator<UnsignedByteType> cursor;
		float[] center;
		int intensity;
		float radius;
		for (int i = 0; i < N_BLOBS; i++) {
			center = centers.get(i);
			intensity = intensities[i];
			radius = radiuses[i];
			centerCursor.setPosition((int) (center[0]/CALIBRATION[0]), 0);
			centerCursor.setPosition((int) (center[1]/CALIBRATION[1]), 1);
			cursor = new HyperSphereIterator<UnsignedByteType>(img, centerCursor, (int) radius, new OutOfBoundsStrategyValueFactory<UnsignedByteType>());
			while(cursor.hasNext()) 
				cursor.next().set(intensity);		
			cursor.close();		
		}

		// Cast to ImagePlus
		ij.ImageJ.main(args);
		ImagePlus imp = ImageJFunctions.copyToImagePlus(img);
		imp.show();
		System.out.println("Creating image done.");
		
		SpotSegmenter<UnsignedByteType> segmenter = new SpotSegmenter<UnsignedByteType>(img, 2*RADIUS);
		Collection<Spot> spots;
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
		FeatureFacade<UnsignedByteType> featureCalculator = new FeatureFacade<UnsignedByteType>(img, segmenter.getFilteredImage(), 2*RADIUS, CALIBRATION);
		featureCalculator.processFeature(Feature.CONTRAST, spots);
		System.out.println("Features done.");
		
		final SpotDisplayer2D displayer = new SpotDisplayer2D(allSpots, imp, RADIUS);
		displayer.render();
		
		System.out.println("Starting threshold GUI...");
		final ThresholdGuiPanel gui = new ThresholdGuiPanel();
		gui.setSpots(allSpots.values());
		gui.addThresholdPanel(Feature.MEAN_INTENSITY);
		JFrame frame = new JFrame();
		frame.getContentPane().add(gui);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		System.out.println("Done.");
		
		gui.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				displayer.refresh(gui.getFeatures(), gui.getThresholds(), gui.getIsAbove());
			}
		});
		
		gui.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displayer.setColorByFeature(gui.getColorByFeature());
			}
		});
		
	}

}
