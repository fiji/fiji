package fiji.plugin.spottracker;

import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Install_J3D;

import java.util.ArrayList;
import java.util.Random;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

public class Spot_Tracker_TestDrive {

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
		
		Spot_Tracker st = new Spot_Tracker();
		System.out.println("Running the plugin...");
		st.run(null); // launch the GUI;
	}
}
