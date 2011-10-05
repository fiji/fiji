package fiji.plugin.trackmate.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import javax.vecmath.Point3f;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.util.Util;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.segmentation.LogSegmenterSettings;
import fiji.plugin.trackmate.segmentation.PeakPickerSegmenter;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;

public class PeakPickerSegmenterTestDrive {

	public static void main(String[] args) {

		final int N_BLOBS = 20;
		final float RADIUS = 5; // µm
		final Random RAN = new Random();
		final float WIDTH = 100; // µm
		final float HEIGHT = 100; // µm
		final float DEPTH = 50; // µm
		final float[] CALIBRATION = new float[] {0.5f, 0.5f, 1}; 
		
		// Create 3D image
		Image<UnsignedByteType> img = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
		).createImage(new int[] {(int) (WIDTH/CALIBRATION[0]), (int) (HEIGHT/CALIBRATION[1]), (int) (DEPTH/CALIBRATION[2])}); 

		// Random blobs
		float[] radiuses = new float[N_BLOBS];
		ArrayList<float[]> centers = new ArrayList<float[]>(N_BLOBS);
		int[] intensities = new int[N_BLOBS]; 
		float x, y, z;
		for (int i = 0; i < N_BLOBS; i++) {
			radiuses[i] = (float) (RADIUS + RAN.nextGaussian());
			x = WIDTH * RAN.nextFloat();
			y = HEIGHT * RAN.nextFloat();
			z = DEPTH * RAN.nextFloat();
			centers.add(i, new float[] {x, y, z});
			intensities[i] = RAN.nextInt(100) + 100;
		}
		
		// Put the blobs in the image
		final SphereCursor<UnsignedByteType> cursor = new SphereCursor<UnsignedByteType>(img, centers.get(0), radiuses[0],	CALIBRATION);
		for (int i = 0; i < N_BLOBS; i++) {
			cursor.setSize(radiuses[i]);
			cursor.moveCenterToCoordinates(centers.get(i));
			while(cursor.hasNext()) 
				cursor.next().set(intensities[i]);		
		}
		cursor.close();

		// Instantiate segmenter
		LogSegmenterSettings settings = new LogSegmenterSettings();
		settings.expectedRadius = RADIUS;
		SpotSegmenter<UnsignedByteType> segmenter = new PeakPickerSegmenter<UnsignedByteType>();
		segmenter.setTarget(img, CALIBRATION, settings);
		
		// Segment
		long start = System.currentTimeMillis();
		if (!segmenter.checkInput() || !segmenter.process()) {
			System.out.println(segmenter.getErrorMessage());
			return;
		}
		Collection<Spot> spots = segmenter.getResult();
		long end = System.currentTimeMillis();
		
		// Display image
		ImageJFunctions.copyToImagePlus(img).show();
		
		// Display results
		int spot_found = spots.size();
		System.out.println("Segmentation took "+(end-start)+" ms.");
		System.out.println("Found "+spot_found+" blobs.\n");

		Point3f p1, p2;
		float dist, min_dist;
		int best_index = 0;
		float[] best_match;
		ArrayList<Spot> spot_list = new ArrayList<Spot>(spots);
		Spot best_spot = null;
		float[] coords = new float[3];

		while (!spot_list.isEmpty() && !centers.isEmpty()) {
			
			min_dist = Float.POSITIVE_INFINITY;
			for (Spot s : spot_list) {
				
				s.getPosition(coords);
				p1 = new Point3f(coords);

				for (int j = 0; j < centers.size(); j++) {
					p2 = new Point3f(centers.get(j));
					dist = p1.distance(p2);
					if (dist < min_dist) {
						min_dist = dist;
						best_index = j;
						best_spot = s;
					}
				}
			}
			
			spot_list.remove(best_spot);
			best_match = centers.remove(best_index);
			System.out.println("Blob coordinates: " + Util.printCoordinates(best_spot.getPosition(coords)));
			System.out.println(String.format("  Best matching center at distance %.1f with coords: " + Util.printCoordinates(best_match), min_dist));			
		}

		System.out.println();
		System.out.println("Unmatched real spots:");
		for (int i = 0; i < centers.size(); i++) 
			System.out.println("Center "+i+" at position: " + Util.printCoordinates(centers.get(i)));

		System.out.println();
		System.out.println("Unmatched found blobs:");
		for (int i = 0; i < spot_list.size(); i++) 
			System.out.println("Center "+i+" at position: " + Util.printCoordinates(spot_list.get(i).getPosition(null)));

		
	}

	
}
