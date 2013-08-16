package fiji.plugin.trackmate.features.spot;

import static org.junit.Assert.assertEquals;
import ij.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;

public class RadiusEstimatorTest  <T extends NativeType<T> & RealType<T>>  {
	
	/** We want to retrieve real radius with a tolerance of:  */
	private static final double TOLERANCE = 0.05;


	/**
	 * We test that this estimator can retrieve the actual radius of perfect sphere
	 * with an accuracy at least 5%.
	 */
	@Test
	public void testEstimatorOnPerfectSpheres() {
		
		final byte on = (byte) 255;
		Spot s1 = new Spot(new double[] {100, 100, 100});
		Spot s2 = new Spot(new double[] {100, 100, 200});
		Spot s3 = new Spot(new double[] {100, 100, 300});
		Spot[] spots = new Spot[] {s1, s2, s3};
		double[] radiuses = new double[]  {12, 20, 32};
		double[] calibration = new double[] {1, 1, 1};
		AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		
		// Create 3 spots image
		Img<UnsignedByteType> img = new ArrayImgFactory<UnsignedByteType>().create(new int[] {200, 200, 400}, new UnsignedByteType());
		ImgPlus<UnsignedByteType> testImage = new ImgPlus<UnsignedByteType>(img, "Test", axes, calibration); 
		
		int index = 0;
		for (Spot s : spots) {
			s.putFeature(Spot.RADIUS, radiuses[index]);
			SpotNeighborhood<UnsignedByteType> sphere = new SpotNeighborhood<UnsignedByteType>(s, testImage);
			for(UnsignedByteType pixel : sphere) {
				pixel.set(on);
			}
			index++;			
		}
	
		// Apply the estimator
		SpotRadiusEstimator<UnsignedByteType> es = new SpotRadiusEstimator<UnsignedByteType>(testImage, null);
		
		Spot s;
		double r;
		for (int i = 0; i < spots.length; i++) {
			s = spots[i];
			r = radiuses[i];
			es.process(s);
			assertEquals(2*r, s.getFeatures().get(SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER), 2*r * TOLERANCE);
		}
		
	}
	
	
	public void exampleEstimation() {
		
		final byte on = (byte) 255;
		Spot s1 = new Spot(new double[] {100, 100, 100});
		Spot s2 = new Spot(new double[] {100, 100, 200});
		Spot s3 = new Spot(new double[] {100, 100, 300});
		Spot[] spots = new Spot[] {s1, s2, s3};
		double[] radiuses = new double[]  {12, 20, 32};
		double[] calibration = new double[] {1, 1, 1};
		AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		
		// Create 3 spots image
		Img<UnsignedByteType> img = new ArrayImgFactory<UnsignedByteType>().create(new int[] {200, 200, 400}, new UnsignedByteType());
		ImgPlus<UnsignedByteType> testImage = new ImgPlus<UnsignedByteType>(img, "Test", axes, calibration); 
		
		int index = 0;
		for (Spot s : spots) {
			s.putFeature(Spot.RADIUS, radiuses[index]);
			SpotNeighborhood<UnsignedByteType> sphere = new SpotNeighborhood<UnsignedByteType>(s, testImage);
			for(UnsignedByteType pixel : sphere) {
				pixel.set(on);
			}
			index++;			
		}
				
		ij.ImagePlus imp = ImageJFunctions.wrap(testImage, testImage.toString());
		imp.show();
		
		// Apply the estimator
		SpotRadiusEstimator<UnsignedByteType> es = new SpotRadiusEstimator<UnsignedByteType>(testImage, null);
		
		Spot s;
		double r;
		long start, stop;
		for (int i = 0; i < spots.length; i++) {
			s = spots[i];
			r = radiuses[i];
			start = System.currentTimeMillis();
			es.process(s);
			stop = System.currentTimeMillis();
			System.out.println(String.format("For spot %d, found diameter %.1f, real value was %.1f.", 
					i, s.getFeatures().get(SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER), 2*r));
			System.out.println("Computing time: "+(stop-start)+" ms.");
		}
	}
	
	
	public static  <T extends NativeType<T> & RealType<T>>  void main(String[] args) {
		ImageJ.main(args);
		new RadiusEstimatorTest<T>().exampleEstimation();
	}
}
