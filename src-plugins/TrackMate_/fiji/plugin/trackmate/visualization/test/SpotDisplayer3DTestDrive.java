package fiji.plugin.trackmate.visualization.test;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TMUtils;
import fiji.plugin.trackmate.features.FeatureFacade;
import fiji.plugin.trackmate.gui.ThresholdGuiPanel;
import fiji.plugin.trackmate.visualization.SpotDisplayer3D;
import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Image3DUniverse;
import ij3d.Install_J3D;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

public class SpotDisplayer3DTestDrive {

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
			cursor.setSize(radiuses[i]);
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
		List<Spot> spots = new ArrayList<Spot>(N_BLOBS);
		SpotImp spot;
		for (int i = 0; i < N_BLOBS; i++)  {
			spot = new SpotImp(centers.get(i), "Spot "+i);
			spot.setFrame(0);
			spots.add(spot);
		}
		
		System.out.println("Grabbing features...");
		new FeatureFacade<UnsignedByteType>(img, RADIUS, CALIBRATION).processFeature(Feature.MEAN_INTENSITY, spots);
		for (Spot s : spots) 
			System.out.println(s);

		// Launch renderer
		final Image3DUniverse universe = new Image3DUniverse();
		universe.addVoltex(imp);
		universe.show();

		final SpotCollection allSpots = new SpotCollection();
		allSpots.put(0, spots);
		final SpotDisplayer3D displayer = new SpotDisplayer3D(universe, RADIUS);
		displayer.render();
		displayer.setSpots(allSpots);
		
		// Launch threshold GUI
		final ThresholdGuiPanel gui = new ThresholdGuiPanel(TMUtils.getFeatureValues(allSpots.values()));

		// Set listeners
		gui.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				displayer.setSpotsToShow(allSpots.threshold(gui.getFeatureThresholds()));
				displayer.refresh();
			}
		});
		gui.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e == gui.COLOR_FEATURE_CHANGED) {
					Feature feature = gui.getColorByFeature();
					displayer.setColorByFeature(feature);
					displayer.setRadiusDisplayRatio(RAN.nextFloat());
					displayer.refresh();
				}
			}
		});
		
		// Display GUI
		JFrame frame = new JFrame();
		frame.getContentPane().add(gui);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		// Add a panel
		gui.addThresholdPanel(Feature.MEAN_INTENSITY);		
		
	}
	
}
