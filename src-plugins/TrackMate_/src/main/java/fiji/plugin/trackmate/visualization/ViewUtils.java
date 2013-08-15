package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * A collection of static utilities related to {@link TrackMateModelView}s.
 * @author Jean-Yves Tinevez - 2013
 *
 */
public class ViewUtils {

	private static final double TARGET_X_IMAGE_SIZE = 512;
	private static final double TARGET_Z_IMAGE_SIZE = 128;

	private ViewUtils() { }
	
	public static final ImagePlus makeEmpytImagePlus(int width, int height, int nslices, int nframes, double[] calibration) {

		FinalInterval interval = new FinalInterval( new long[] { width, height, nslices, nframes } );
		
		ConstantRandomAccessible<UnsignedByteType> ra = new ConstantRandomAccessible<UnsignedByteType>(new UnsignedByteType(0), 4);
		IntervalView<UnsignedByteType> view = Views.interval(ra, interval);
		
		ImagePlus imp = ImageJFunctions.wrap(view, "blank");
		imp.getCalibration().pixelWidth = calibration[0];
		imp.getCalibration().pixelHeight = calibration[1];
		imp.getCalibration().pixelDepth = calibration[2];
		imp.setDimensions(1, nslices, nframes);
		
		return imp;
	}
	
	public static final ImagePlus makeEmpytImagePlus(Model model) {

		double maxX = 0;
		double maxY = 0;
		double maxZ = 0;
		int nframes = 0;

		for (Spot spot : model.getSpots().iterable(true)) {
			double r = spot.getFeature(Spot.RADIUS);
			double x = Math.ceil(r + spot.getFeature(Spot.POSITION_X));
			double y = Math.ceil(r + spot.getFeature(Spot.POSITION_Y) );
			double z = Math.ceil(spot.getFeature(Spot.POSITION_Z));
			int t = spot.getFeature(Spot.FRAME).intValue();
			
			if (x > maxX) {
				maxX = x;
			}
			if (y > maxY) {
				maxY = y;
			}
			if (z > maxZ) {
				maxZ = z;
			}
			if (t > nframes) {
				nframes = t;
			}
		}
		
		double calX = maxX / TARGET_X_IMAGE_SIZE;
		double calY = maxY / TARGET_X_IMAGE_SIZE;
		double calxy = Math.max(calX, calY);
		double calZ = maxZ / TARGET_Z_IMAGE_SIZE;
		
		int width = (int) Math.ceil(maxX / calxy);
		int height = (int) Math.ceil(maxY / calxy);
		int nslices;
		if (maxZ == 0) {
			nslices = 1;
		} else {
			nslices = (int) Math.ceil(maxZ / calZ);
		}
		double[] calibration = new double[] { calxy, calxy, calZ };
		
		ImagePlus imp = makeEmpytImagePlus(width, height, nslices, nframes+1, calibration);
		imp.getCalibration().setUnit(model.getSpaceUnits());
		imp.getCalibration().setTimeUnit(model.getTimeUnits());
		return imp;
	}
	
	
}
