package fiji.plugin.nperry.gui;

import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;
import ij3d.Install_J3D;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import customnode.CustomTriangleMesh;
import customnode.MeshMaker;
import fiji.plugin.nperry.Spot;

public class SpotDisplayer {
	
	private List<Spot> spots;
	private ImagePlus imp;
	private Content imageContent;

	
	public SpotDisplayer(List<Spot> spots, ImagePlus imp) {
		this.spots = spots;
		setImagePlus(imp);
	}
	
	public SpotDisplayer(List<Spot> spots) {
		this(spots, null);
	}
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	@SuppressWarnings("unchecked")
	public Image3DUniverse render(final float radius, final Color3f color) {
		Image3DUniverse univ = new Image3DUniverse();
		if (imp != null) {
			imageContent = univ.addVoltex(imp);
			imageContent.setLocked(true);
		}
		
		// Create the blobs
		float[] center;
		float x, y, z;
		
		ArrayList<Content> blobs = new ArrayList<Content>(spots.size());
		for (Spot spot : spots) {
			center = spot.getCoordinates();
			x = center[0];
			y = center[1];
			z = center[2];
			List<Point3f> mesh = MeshMaker.createSphere(x, y, z, radius);
			CustomTriangleMesh tmesh = new CustomTriangleMesh(mesh, color, 0);
			Content content = ContentCreator.createContent(tmesh, spot.getName());
			blobs.add(content);
		}
		univ.addContentLater(blobs);
		return univ;		
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	

	private void setImagePlus(final ImagePlus imp) {
		if (null == imp) { 
			this.imp = null;
			return;
		}
		ImagePlus copy = new Duplicator().run(imp, 1, imp.getNSlices());
		ContentCreator.convert(copy);
		this.imp = copy;
	}
	
	
	
	/*
	 * STATIC METHODS
	 */
		
		
	public static void main(String[] args) {

		System.out.println(Install_J3D.getJava3DVersion());
		
		
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
		for (int i = 0; i < N_BLOBS; i++) {
			radiuses[i] = (float) (RADIUS + RAN.nextGaussian());
			float x = WIDTH * RAN.nextFloat();
			float y = HEIGHT * RAN.nextFloat();
			float z = DEPTH * RAN.nextFloat();
			centers.add(i, new float[] {x, y, z});
			intensities[i] = RAN.nextInt(100) + 100;
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
		
		// Start ImageJ
		ij.ImageJ.main(args);
		
		// Cast the Image the ImagePlus and convert to 8-bit
		ImagePlus imp = ImageJFunctions.copyToImagePlus(img);
		if (imp.getType() != ImagePlus.GRAY8)
			new StackConverter(imp).convertToGray8();

		imp.getCalibration().pixelWidth 	= CALIBRATION[0];
		imp.getCalibration().pixelHeight	= CALIBRATION[1];
		imp.getCalibration().pixelDepth 	= CALIBRATION[2];
		imp.setTitle("3D blobs");

		// Create a Spot arrays
		List<Spot> blobs = new ArrayList<Spot>(N_BLOBS);
		for (int i = 0; i < N_BLOBS; i++) 
			blobs.add(new Spot(centers.get(i), "Spot "+i));
		
		// Launch renderer
		SpotDisplayer displayer = new SpotDisplayer(blobs, imp);
		displayer.render(5, new Color3f(Color.red)).show();
		
	}
}
