package marchingcubes;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.measure.Calibration;
import java.util.List;

import javax.vecmath.Point3f;

import ij3d.Volume;
import ij3d.ImgLibVolume;

import isosurface.Triangulator;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

import vib.NaiveResampler;

public class MCTriangulator implements Triangulator {

	public List getTriangles(ImagePlus image, int threshold, 
					boolean[] channels, int resamplingF) {

		if(resamplingF != 1)
			image = NaiveResampler.resample(image, resamplingF);
		// There is no need to zero pad any more. MCCube automatically
		// scans one pixel more in each direction, assuming a value
		// of zero outside the image.
		// zeroPad(image);
		// create Volume
		Volume volume = new Volume(image, channels);
		volume.setAverage(true);

		// get triangles
		List l = MCCube.getTriangles(volume, threshold);
		return l;
	}

	/**
	 * @param img The Image<? extends RealType> instance to use.
	 * @param threshold The cut-off (inclusive) of pixel values considered inside.
	 * @param origin The translation of the origin, in 3D.
	 */
	public<T extends RealType<T>> List<Point3f> getTriangles(Image<T> img, int threshold, float[] origin) throws Exception {
		return MCCube.getTriangles(new ImgLibVolume(img, origin), threshold);
	}

	static public void zeroPad(final ImagePlus imp) {
		ImageStack stack = imp.getStack();
		int w = stack.getWidth();
		int h = stack.getHeight();
		int d = stack.getSize();
		int type = imp.getType();
		// create new stack 
		ImageStack st = new ImageStack(w+2, h+2);

		// retrieve 1st processor
		ImageProcessor old = stack.getProcessor(1);
		
		// enlarge it and add it as a first slide.
		ImageProcessor ne = createProcessor(w+2, h+2, type);
		st.addSlice("", ne);

		// now do the same for all slices in the old stack
		for(int z = 0; z < d; z++) {
			old = stack.getProcessor(z+1);
			ne = createProcessor(w+2, h+2, type);
			ne.insert(old, 1, 1);
			st.addSlice(Integer.toString(z+1), ne);
		}

		// now add an empty new slice
		ne = createProcessor(w+2, h+2, type);
		st.addSlice(Integer.toString(d+1), ne);

		imp.setStack(null, st);

		// update the origin
		Calibration cal = imp.getCalibration();
		cal.xOrigin -= cal.pixelWidth;
		cal.yOrigin -= cal.pixelHeight;
		cal.zOrigin -= cal.pixelDepth;
		imp.setCalibration(cal);
	}

	private static final ImageProcessor createProcessor(
					int w, int h, int type) {
		if(type == ImagePlus.COLOR_RGB)
			return new ColorProcessor(w, h);
		return new ByteProcessor(w, h);
	}
}
