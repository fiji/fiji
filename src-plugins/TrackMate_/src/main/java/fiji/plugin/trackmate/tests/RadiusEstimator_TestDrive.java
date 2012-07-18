package fiji.plugin.trackmate.tests;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.imglib2.cursor.special.SphereCursor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.jdom.JDOMException;

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
	
	public static <T extends NativeType<T> & RealType<T>> void main(String[] args) throws JDOMException, IOException {
		
		File testFile = new File("/Users/tinevez/Desktop/Data/Celegans-5pc_17timepoints-1.xml");
		TmXmlReader reader = new TmXmlReader(testFile, Logger.DEFAULT_LOGGER);
		reader.parse();
		
		ImagePlus imp = reader.getImage();
		imp.setDimensions(1, 41, 1); // otherwise it is 4D
		ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(imp);
		
		TrackMateModel<T> model = reader.getModel();
		model.getSettings().img = img;
		
		RadiusEstimator<T> es = new RadiusEstimator<T>();
		es.setTarget(img);
		
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
		RadiusToEstimatedAction<T> action = new RadiusToEstimatedAction<T>();
		action.execute(new TrackMate_<T>(model));
		
		// View
		HyperStackDisplayer<T> displayer = new HyperStackDisplayer<T>();
		displayer.setModel(model);
		displayer.render();
		displayer.refresh();
		
	}
	
	/**
	 * For testing purposes
	 */
	public static <T extends RealType<T> & NativeType<T>> void main2(String[] args) {
		
		final byte on = (byte) 255;
		SpotImp s1 = new SpotImp(new double[] {100, 100, 100});
		SpotImp s2 = new SpotImp(new double[] {100, 100, 200});
		SpotImp s3 = new SpotImp(new double[] {100, 100, 300});
		SpotImp[] spots = new SpotImp[] {s1, s2, s3};
		double[] radiuses = new double[]  {12, 20, 32};
		double[] calibration = new double[] {1, 1, 1};
		AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		
		// Create 3 spots image
		Img<UnsignedByteType> img = new ArrayImgFactory<UnsignedByteType>().create(new int[] {200, 200, 400}, new UnsignedByteType());
		ImgPlus<UnsignedByteType> testImage = new ImgPlus<UnsignedByteType>(img, "Test", axes, calibration); 
		
		
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
		RadiusEstimator<UnsignedByteType> es = new RadiusEstimator<UnsignedByteType>();
		es.setTarget(testImage);
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
