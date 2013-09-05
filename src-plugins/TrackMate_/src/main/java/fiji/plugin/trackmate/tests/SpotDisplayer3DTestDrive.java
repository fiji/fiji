package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzer;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel.Category;
import fiji.plugin.trackmate.gui.panels.components.FilterGuiPanel;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Image3DUniverse;
import ij3d.Install_J3D;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class SpotDisplayer3DTestDrive {

	public static void main(String[] args) {

		System.out.println(Install_J3D.getJava3DVersion());
				
		final int N_BLOBS = 20;
		final double RADIUS = 5; // µm
		final Random RAN = new Random();
		final double WIDTH = 100; // µm
		final double HEIGHT = 100; // µm
		final double DEPTH = 50; // µm
		final double[] CALIBRATION = new double[] {0.5, 0.5, 1}; 
		final AxisType[] AXES = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		
		// Create 3D image
		System.out.println("Creating image....");
		Img<UnsignedByteType> source = new ArrayImgFactory<UnsignedByteType>()
				.create(new int[] {(int) (WIDTH/CALIBRATION[0]), (int) (HEIGHT/CALIBRATION[1]), (int) (DEPTH/CALIBRATION[2])},
						new UnsignedByteType());
		ImgPlus<UnsignedByteType> img = new ImgPlus<UnsignedByteType>(source, "test", AXES, CALIBRATION); 
		

		// Random blobs
		double[] radiuses = new double[N_BLOBS];
		ArrayList<double[]> centers = new ArrayList<double[]>(N_BLOBS);
		int[] intensities = new int[N_BLOBS]; 
		for (int i = 0; i < N_BLOBS; i++) {
			radiuses[i] = RADIUS + RAN.nextGaussian();
			double x = WIDTH * RAN.nextDouble();
			double y = HEIGHT * RAN.nextDouble();
			double z = DEPTH * RAN.nextDouble();
			centers.add(i, new double[] {x, y, z});
			intensities[i] = RAN.nextInt(200);
		}
		
		// Put the blobs in the image
		for (int i = 0; i < N_BLOBS; i++) {
			Spot tmpSpot = new Spot(centers.get(i));
			tmpSpot.putFeature(Spot.RADIUS, radiuses[i]);
			final SpotNeighborhood<UnsignedByteType> sphere = new SpotNeighborhood<UnsignedByteType>(tmpSpot, img);
			for (UnsignedByteType pixel : sphere) {
				pixel.set(intensities[i]);
			}
		}
		
		// Start ImageJ
		ij.ImageJ.main(args);
		
		// Cast the Img the ImagePlus and convert to 8-bit
		ImagePlus imp = ImageJFunctions.wrap(img, img.toString());
		if (imp.getType() != ImagePlus.GRAY8)
			new StackConverter(imp).convertToGray8();

		imp.getCalibration().pixelWidth 	= CALIBRATION[0];
		imp.getCalibration().pixelHeight	= CALIBRATION[1];
		imp.getCalibration().pixelDepth 	= CALIBRATION[2];
		imp.setTitle("3D blobs");

		// Create a Spot arrays
		List<Spot> spots = new ArrayList<Spot>(N_BLOBS);
		Spot spot;
		for (int i = 0; i < N_BLOBS; i++)  {
			spot = new Spot(centers.get(i), "Spot "+i);
			spot.putFeature(Spot.POSITION_T, Double.valueOf(0));
			spot.putFeature(Spot.RADIUS, RADIUS);
			spot.putFeature(Spot.QUALITY, RADIUS);
			spots.add(spot);
		}
		
		System.out.println("Grabbing features...");
		SpotIntensityAnalyzer<UnsignedByteType> analyzer = new SpotIntensityAnalyzer<UnsignedByteType>(img, spots.iterator());
		analyzer.process();
		for (Spot s : spots) 
			System.out.println(s);

		// Launch renderer
		final SpotCollection allSpots = new SpotCollection();
		allSpots.put(0, spots);
		final TrackMate trackmate = new TrackMate();
		trackmate.getModel().setSpots(allSpots, false);
		trackmate.getSettings().imp = imp;
		Image3DUniverse universe = new Image3DUniverse();
		universe.show();
		final SpotDisplayer3D displayer = new SpotDisplayer3D(trackmate.getModel(), new SelectionModel(trackmate.getModel()), universe);
		displayer.render();
		
		// Launch threshold GUI
		List<FeatureFilter> ff = new ArrayList<FeatureFilter>();
		final FilterGuiPanel gui = new FilterGuiPanel(trackmate.getModel(), Arrays.asList(new Category[] { Category.SPOTS } ) );
		gui.setFilters(ff);

		// Set listeners
		gui.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				trackmate.getSettings().setSpotFilters(gui.getFeatureFilters());
				trackmate.execSpotFiltering(false);
			}
		});
		gui.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e == gui.COLOR_FEATURE_CHANGED) {
					new Thread() {
						public void run() {
							String feature = gui.getColorFeature();
							@SuppressWarnings("unchecked")
							FeatureColorGenerator<Spot> spotColorGenerator = (FeatureColorGenerator<Spot>) displayer.getDisplaySettings(TrackMateModelView.KEY_SPOT_COLORING);
							spotColorGenerator.setFeature(feature);
							displayer.setDisplaySettings(TrackMateModelView.KEY_SPOT_COLORING, spotColorGenerator);
							displayer.setDisplaySettings(TrackMateModelView.KEY_SPOT_RADIUS_RATIO, RAN.nextFloat()+1);
							displayer.refresh();
						};
					}.start();
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
		gui.addFilterPanel(Spot.POSITION_Z);
		
	}
	
}
