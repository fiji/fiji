/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package process3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.measure.Calibration;

/**
 * Smoothes an ImagePlus, either uniformly or by Gaussian blur.
 * The user can specify if for Gaussian smoothing, the dimensions of
 * the pixels should be taken into account when calculating the
 * kernel. The user also specifies the radius / std dev.
 */
public class Smooth {

	public static ImagePlus smooth(ImagePlus image, boolean useGaussian,
					float sigma, boolean useCalibration) {

		int type = image.getType();

		Calibration calib = image.getCalibration();

		float pixelW = !useCalibration ? 1.0f
					: (float)Math.abs(calib.pixelWidth);
		float[] H_x = createKernel(sigma, pixelW, useGaussian);

		pixelW = !useCalibration ? 1.0f
					: (float)Math.abs(calib.pixelHeight);
		float[] H_y = createKernel(sigma, pixelW, useGaussian);

		pixelW = !useCalibration ? 1.0f
					: (float)Math.abs(calib.pixelDepth);
		float[] H_z = createKernel(sigma, pixelW, useGaussian);

		ImageStack stack = Convolve3d.
					convolve(image,H_x,H_y,H_z).getStack();

		if(image.getType() == ImagePlus.GRAY32) {
			ImagePlus result = new ImagePlus("Smoothed", stack);
			result.setCalibration(image.getCalibration());
			return result;
		}
		// convert the result to an image that matches the type of the
		// original (currently 8 bit or 16 bit)

		ImageStack stack2 = new ImageStack(
				stack.getWidth(), stack.getHeight());
		for(int z = 0; z < stack.getSize(); z++) {
			float[] f = (float[])stack.
					getProcessor(z+1).getPixels();
			if (type == ImagePlus.GRAY8) {
				byte[] b = new byte[f.length];
				for(int i = 0; i < b.length; i++) {
					b[i] = (byte)Math.round(f[i]);
				}
				stack2.addSlice("",b);
			} else if (type == ImagePlus.GRAY16)  {
				short[] s = new short[f.length];
				for(int i = 0; i < s.length; i++) {
					s[i] = (short)Math.round(f[i]);
				}
				stack2.addSlice("",s);
			}
		}
		ImagePlus result = new ImagePlus("Smoothed", stack2);
		result.setCalibration(image.getCalibration());
		return result;
	}

	public static float[] createKernel(float sigma, float pixelW,
				boolean useGaussian) {

		return useGaussian ? createGaussianKernel(sigma, pixelW)
				: createUniformKernel(sigma, pixelW);
	}

	public static float[] createUniformKernel(float radius, float pixelW) {
		radius /= pixelW;
		int diameter = (int)Math.ceil(2*radius+1);
		float[] H = new float[diameter];
		for(int i = 0; i < diameter; i++) {
			H[i] = 1f/diameter;
		}
		return H;
	}

	public static float[] createGaussianKernel(float sigma, float pixelW) {
		// the radius of a gaussian should at least be 2.5 sigma
		sigma /= pixelW;
		int diameter = (int)Math.ceil(5 * sigma);
		diameter = (diameter % 2 == 0) ? diameter + 1 : diameter;
		float[] kernel = new float[diameter];
		int radius = diameter/2;
		float sum = 0f;
		kernel[radius] = gauss(0, sigma);
		sum += kernel[radius];
		for(int x = 1; x <= radius; x++) {
			kernel[radius-x] = kernel[radius+x] = gauss(x, sigma);
			sum += 2*kernel[radius-x];
		}
		// normalize
		for(int i = 0; i < diameter; i++) {
			kernel[i] /= sum;
		}
		return kernel;
	}

	public static float gauss(float x, float sigma) {
		return (float)Math.exp(-x*x / (2*sigma*sigma));
	}
}
