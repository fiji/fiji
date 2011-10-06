package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.spot.BlobDescriptiveStatistics;
import fiji.plugin.trackmate.gui.FilterGuiPanel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import ij.ImagePlus;
import ij.process.StackConverter;
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
			spot.putFeature(Spot.POSITION_T, 0);
			spot.putFeature(Spot.RADIUS, RADIUS);
			spot.putFeature(Spot.QUALITY, RADIUS);
			spots.add(spot);
		}
		
		System.out.println("Grabbing features...");
		BlobDescriptiveStatistics analyzer = new BlobDescriptiveStatistics();
		analyzer.setTarget(img, CALIBRATION);
		analyzer.process(spots);
		for (Spot s : spots) 
			System.out.println(s);

		// Launch renderer
		final SpotCollection allSpots = new SpotCollection();
		allSpots.put(0, spots);
		final TrackMate_ plugin = new TrackMate_();
		plugin.getModel().setSpots(allSpots, false);
		plugin.getModel().getSettings().imp = imp;
		final SpotDisplayer3D displayer = new SpotDisplayer3D();
		displayer.setModel(plugin.getModel());
		displayer.render();
		
		// Launch threshold GUI
		List<FeatureFilter> ff = new ArrayList<FeatureFilter>();
		final FilterGuiPanel gui = new FilterGuiPanel(
				BlobDescriptiveStatistics.FEATURES, 
				ff,
				BlobDescriptiveStatistics.FEATURE_NAMES,
				TMUtils.getSpotFeatureValues(allSpots.values(), BlobDescriptiveStatistics.FEATURES),
				"spots");

		// Set listeners
		gui.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				plugin.getModel().setSpotFilters(gui.getFeatureFilters());
				plugin.execSpotFiltering();
			}
		});
		gui.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e == gui.COLOR_FEATURE_CHANGED) {
					String feature = gui.getColorByFeature();
					displayer.setDisplaySettings(TrackMateModelView.KEY_SPOT_COLOR_FEATURE, feature);
					displayer.setDisplaySettings(TrackMateModelView.KEY_SPOT_RADIUS_RATIO, RAN.nextFloat());
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
		gui.addFilterPanel(BlobDescriptiveStatistics.MEAN_INTENSITY);		
		
	}
	
}
