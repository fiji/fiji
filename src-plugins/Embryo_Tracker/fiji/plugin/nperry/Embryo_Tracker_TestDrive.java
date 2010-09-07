package fiji.plugin.nperry;

import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Install_J3D;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point3f;

import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

public class Embryo_Tracker_TestDrive {

	public static void main(String[] args) {

		System.out.println("Java3D version: "+Install_J3D.getJava3DVersion());
		
		final int N_BLOBS = 20;
		final float RADIUS = 10; // µm
		final Random RAN = new Random();
		final float WIDTH = 200; // µm
		final float HEIGHT = 200; // µm
		final float DEPTH = 50; // µm
		final float[] CALIBRATION = new float[] {0.5f, 0.5f, 1}; 
		
		// Create 3D image
		System.out.println("Creating image....");
		Image<UnsignedByteType> img = new ImageFactory<UnsignedByteType>(
				new UnsignedByteType(),
				new ArrayContainerFactory()
		).createImage(new int[] {(int) (WIDTH/CALIBRATION[0]), (int) (HEIGHT/CALIBRATION[1]), (int) (DEPTH/CALIBRATION[2])}); 

		// Random blobs
		float[] radiuses = new float[N_BLOBS];
		ArrayList<float[]> centers = new ArrayList<float[]>(N_BLOBS);
		int[] intensities = new int[N_BLOBS]; 
		for (int i = 0; i < N_BLOBS; i++) {
			radiuses[i] = (float) (RADIUS + RAN.nextGaussian());
			float x = WIDTH * RAN.nextFloat();
			float y = HEIGHT * RAN.nextFloat();
			float z = DEPTH * RAN.nextFloat();
			centers.add(i, new float[] {x, y, z});
			intensities[i] = RAN.nextInt(200);
		}
		
		// Put the blobs in the image
		final SphereCursor<UnsignedByteType> cursor = new SphereCursor<UnsignedByteType>(img, centers.get(0), radiuses[0],	CALIBRATION);
		for (int i = 0; i < N_BLOBS; i++) {
			cursor.setRadius(radiuses[i]);
			cursor.moveCenterToCoordinates(centers.get(i));
			while(cursor.hasNext()) 
				cursor.next().set(intensities[i]);		
		}
		cursor.close();
		
		// Cast the Image the ImagePlus and convert to 8-bit
		ImagePlus imp = ImageJFunctions.copyToImagePlus(img);
		if (imp.getType() != ImagePlus.GRAY8)
			new StackConverter(imp).convertToGray8();

		imp.getCalibration().pixelWidth 	= CALIBRATION[0];
		imp.getCalibration().pixelHeight	= CALIBRATION[1];
		imp.getCalibration().pixelDepth 	= CALIBRATION[2];
		imp.setTitle("3D blobs");
		
		ij.ImageJ.main(args);
		imp.show();
		
		Embryo_Tracker<UnsignedByteType> ebt = new Embryo_Tracker<UnsignedByteType>();
		System.out.println("Running the plugin...");
		long start = System.currentTimeMillis();
		ebt.run("diameter="+(2*(int)RADIUS));
		long end = System.currentTimeMillis();
		System.out.println("Done in "+(end-start)+" ms.");
		
		System.out.println("Checking results");
		ArrayList<Spot> spots = ebt.getSpots();

		Point3f p1, p2;
		float dist, min_dist;
		int best_index = 0;
		float[] coords, best_match;
		ArrayList<Spot> spot_list = new ArrayList<Spot>(spots);
		Spot best_spot = null;

		while (!spot_list.isEmpty()) {
			
			min_dist = Float.POSITIVE_INFINITY;
			for (Spot s : spot_list) {
				
				coords = s.getCoordinates();
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
			System.out.println("Blob coordinates: " + MathLib.printCoordinates(best_spot.getCoordinates()));
			System.out.println(String.format("  Best matching center at distance %.1f with coords: " + MathLib.printCoordinates(best_match), min_dist));			
		}
		System.out.println();
		System.out.println("Unmatched centers:");
		for (int i = 0; i < centers.size(); i++) {
			System.out.println("Center "+i+" at position: " + MathLib.printCoordinates(centers.get(i)));
		}
		
	}
}
