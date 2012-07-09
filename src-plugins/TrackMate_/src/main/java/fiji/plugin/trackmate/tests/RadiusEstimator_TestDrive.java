package fiji.plugin.trackmate.tests;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom.JDOMException;

import net.imglib2.cursor.special.SphereCursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.action.RadiusToEstimatedAction;
import fiji.plugin.trackmate.features.spot.RadiusEstimator;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class RadiusEstimator_TestDrive {
	
	public static void main(String[] args) throws JDOMException, IOException {
		
		File testFile = new File("/Users/tinevez/Desktop/Data/Celegans-5pc_17timepoints-1.xml");
		TmXmlReader reader = new TmXmlReader(testFile, Logger.DEFAULT_LOGGER);
		reader.parse();
		
		ImagePlus imp = reader.getImage();
		imp.setDimensions(1, 41, 1); // otherwise it is 4D
		Img<? extends FloatType> img = ImageJFunctions.wrap(imp);
		
		TrackMateModel model = reader.getModel();
		model.getSettings().imp = imp;
		
		RadiusEstimator es = new RadiusEstimator();
		es.setTarget(img, new float[] { 0.2f, 0.2f, 1 } );
		
		SpotCollection allSpots = model.getSpots();
		model.setFilteredSpots(allSpots, false);
		List<Spot> spots = allSpots.get(0);
		
		long start, stop;
		for (int i = 0; i < spots.size(); i++) {
			Spot s = spots.get(i);
			double r = s.getFeature(Spot.RADIUS);
			start = System.currentTimeMillis();
			es.process(s);
			stop = System.currentTimeMillis();
			System.out.println(String.format("For spot %d, found diameter %.1f, start value was %.1f.", 
					i, s.getFeatures().get(RadiusEstimator.ESTIMATED_DIAMETER), 2*r));
			System.out.println("Computing time: "+(stop-start)+" ms.");
		}

		// Modify spot radius to estimated
		RadiusToEstimatedAction action = new RadiusToEstimatedAction();
		action.execute(new TrackMate_(model));
		
		// View
		HyperStackDisplayer displayer = new HyperStackDisplayer();
		displayer.setModel(model);
		displayer.render();
		displayer.refresh();
		
	}
	
	/**
	 * For testing purposes
	 */
	public static void main2(String[] args) {
		
		final byte on = (byte) 255;
		SpotImp s1 = new SpotImp(new float[] {100, 100, 100});
		SpotImp s2 = new SpotImp(new float[] {100, 100, 200});
		SpotImp s3 = new SpotImp(new float[] {100, 100, 300});
		SpotImp[] spots = new SpotImp[] {s1, s2, s3};
		float[] radiuses = new float[]  {12, 20, 32};
		float[] calibration = new float[] {1, 1, 1};
		
		// Create 3 spots image
		Img<UnsignedByteType> testImage = new ArrayImgFactory<UnsignedByteType>().create(new int[] {200, 200, 400}, new UnsignedByteType());

		SphereCursor<UnsignedByteType> cursor;
		int index = 0;
		for (SpotImp s : spots) {
			s.putFeature(Spot.RADIUS, radiuses[index]);
			cursor = new SphereCursor<UnsignedByteType>(
					testImage,
					s.getPosition(null),
					radiuses[index],
					calibration);
			while (cursor.hasNext())
				cursor.next().set(on);
			index++;			
		}
				
		ij.ImageJ.main(args);
		ij.ImagePlus imp = ImageJFunctions.wrap(testImage, testImage.toString());
		imp.show();
		
		// Apply the estimator
		RadiusEstimator es = new RadiusEstimator();
		es.setTarget(testImage, calibration);
//		es.nDiameters = 20;
		
		SpotImp s;
		double r;
		long start, stop;
		for (int i = 0; i < spots.length; i++) {
			s = spots[i];
			r = radiuses[i];
			start = System.currentTimeMillis();
			es.process(s);
			stop = System.currentTimeMillis();
			System.out.println(String.format("For spot %d, found diameter %.1f, real value was %.1f.", 
					i, s.getFeatures().get(RadiusEstimator.ESTIMATED_DIAMETER), 2*r));
			System.out.println("Computing time: "+(stop-start)+" ms.");
		}
	}
}
