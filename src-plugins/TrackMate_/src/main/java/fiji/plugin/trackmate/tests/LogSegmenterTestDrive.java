package fiji.plugin.trackmate.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import net.imglib2.cursor.special.SphereCursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.DownSampleLogSegmenter;
import fiji.plugin.trackmate.detection.LogSegmenterSettings;
import fiji.plugin.trackmate.detection.SpotSegmenter;

/**
 * Test class for {@link DownSampleLogSegmenter}
 * @author Jean-Yves Tinevez
 *
 */
public class LogSegmenterTestDrive {
	
	public static void main(String[] args) {

		final int N_BLOBS = 20;
		final double RADIUS = 5; // µm
		final Random RAN = new Random();
		final double WIDTH = 100; // µm
		final double HEIGHT = 100; // µm
		final double DEPTH = 50; // µm
		final double[] CALIBRATION = new double[] {0.5f, 0.5f, 1}; 
		final AxisType[] AXES = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		
		// Create 3D image
		Img<UnsignedByteType> source = new ArrayImgFactory<UnsignedByteType>()
				.create(new int[] {(int) (WIDTH/CALIBRATION[0]), (int) (HEIGHT/CALIBRATION[1]), (int) (DEPTH/CALIBRATION[2])}, 
						new UnsignedByteType());
		ImgPlus<UnsignedByteType> img = new ImgPlus<UnsignedByteType>(source, "Test", AXES, CALIBRATION);
		

		// Random blobs
		double[] radiuses = new double[N_BLOBS];
		ArrayList<double[]> centers = new ArrayList<double[]>(N_BLOBS);
		int[] intensities = new int[N_BLOBS]; 
		double x, y, z;
		for (int i = 0; i < N_BLOBS; i++) {
			radiuses[i] = RADIUS + RAN.nextGaussian();
			x = WIDTH * RAN.nextFloat();
			y = HEIGHT * RAN.nextFloat();
			z = DEPTH * RAN.nextFloat();
			centers.add(i, new double[] {x, y, z});
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

		// Instantiate segmenter
		LogSegmenterSettings settings = new LogSegmenterSettings();
		settings.expectedRadius = RADIUS;
		SpotSegmenter<UnsignedByteType> segmenter = new DownSampleLogSegmenter<UnsignedByteType>();
		segmenter.setTarget(img, settings);
		
		// Segment
		long start = System.currentTimeMillis();
		if (!segmenter.checkInput() || !segmenter.process()) {
			System.out.println(segmenter.getErrorMessage());
			return;
		}
		Collection<Spot> spots = segmenter.getResult();
		long end = System.currentTimeMillis();
		
		// Display image
		ImageJFunctions.show(img);
		
		// Display results
		int spot_found = spots.size();
		System.out.println("Segmentation took "+(end-start)+" ms.");
		System.out.println("Found "+spot_found+" blobs.\n");

		Point3d p1, p2;
		double dist, min_dist;
		int best_index = 0;
		double[] best_match;
		ArrayList<Spot> spot_list = new ArrayList<Spot>(spots);
		Spot best_spot = null;
		double[] coords = new double[3];

		while (!spot_list.isEmpty()) {
			
			min_dist = Float.POSITIVE_INFINITY;
			for (Spot s : spot_list) {
				
				s.getPosition(coords);
				p1 = new Point3d(coords);

				for (int j = 0; j < centers.size(); j++) {
					p2 = new Point3d(centers.get(j));
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
		System.out.println("Unmatched centers:");
		for (int i = 0; i < centers.size(); i++) 
			System.out.println("Center "+i+" at position: " + Util.printCoordinates(centers.get(i)));
		
		
	}

}
