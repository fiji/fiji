package process3d;

import ij.ImageStack;
import ij.ImagePlus;
import ij.process.ByteProcessor;

/**
 * Plugin which takes an ImagePlus and rebins the pixel values 
 * to the specified range.
 */
public class Rebin {

	public static ImagePlus rebin(ImagePlus imp, int nbins) {
		float[] minmax = new float[2];
		getMinAndMax(imp, minmax);
		return rebin(imp, minmax[0], minmax[1], nbins);
	}

	public static ImagePlus rebin(ImagePlus imp, 
					float min, float max, int nbins) {

		float delta = (max - min) / nbins;
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		ImageStack res = new ImageStack(w, h);

		for(int z = 0; z < d; z++) {
			float[] f = (float[])imp.getStack().getProcessor(z+1).
					getPixels();
			byte[] b = new byte[w*h];
			for(int i = 0; i < w*h; i++) {
				b[i] = (byte)((f[i] - min) / delta);
			}
			res.addSlice("", new ByteProcessor(w, h, b, null));
		}
		ImagePlus result = new ImagePlus("Rebinned", res);
		result.setCalibration(imp.getCalibration());
		return result;
	}

	public static void getMinAndMax(ImagePlus imp, float[] minmax) {
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;

		for(int z = 0; z < d; z++) {
			float[] f = (float[])imp.getStack().getProcessor(z+1).
					getPixels();
			for(int i = 0; i < w*h; i++) {
				min = f[i] < min ? f[i] : min;
				max = f[i] > max ? f[i] : max;
			}
		}
		minmax[0] = min;
		minmax[1] = max;
	}
}
