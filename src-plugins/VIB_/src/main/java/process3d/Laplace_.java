/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package process3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.IJ;

import ij.process.FloatProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import ij.plugin.filter.PlugInFilter;

public class Laplace_ implements PlugInFilter {

	private ImagePlus image;
	float tolerance = 5.0f;

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | DOES_16;
	}

	public void run(ImageProcessor ip) {
		ImagePlus laplace = calculateLaplace_(image);
		ImagePlus rebinned = Rebin_.rebin(laplace, 256);
		rebinned.show();
	}


	public static ImagePlus calculateLaplace_(ImagePlus imp) {
		
		IJ.showStatus("Calculating laplace");

		float[] H_x = new float[] {1.0f, -2.0f, 1.0f};
		ImagePlus g_x = Convolve3d.convolveX(imp, H_x);

		float[] H_y = new float[] {1.0f, -2.0f, 1.0f};
		ImagePlus g_y = Convolve3d.convolveY(imp, H_y);

		float[] H_z = new float[] {1.0f, -2.0f, 1.0f};
		ImagePlus g_z = Convolve3d.convolveZ(imp, H_z);

		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		ImageStack grad = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			FloatProcessor res = new FloatProcessor(w, h);
			grad.addSlice("", res);
			float[] values = (float[])res.getPixels();
			float[] x_ = (float[])g_x.getStack().
						getProcessor(z+1).getPixels();
			float[] y_ = (float[])g_y.getStack().
						getProcessor(z+1).getPixels();
			float[] z_ = (float[])g_z.getStack().
						getProcessor(z+1).getPixels();
			for(int i = 0; i < w*h; i++) {
				values[i] = (float)Math.sqrt(
						x_[i]*x_[i] + 
						y_[i]*y_[i] + 
						z_[i]*z_[i]);
			}
		}
		return new ImagePlus("Laplacian", grad);
	}
}
