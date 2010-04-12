package process3d;

import ij.measure.Calibration;

import ij.plugin.filter.PlugInFilter;

import ij.gui.GenericDialog;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import ij.ImagePlus;
import ij.ImageStack;
import ij.IJ;

import process3d.Convolve_3d;

public class Gradient_ implements PlugInFilter {

	private ImagePlus image;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8G | DOES_16;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Gradient_");
		gd.addCheckbox("Use calibration", true);
		if(gd.wasCanceled())
			return;
		boolean useCalibration = gd.getNextBoolean();
		ImagePlus grad = calculateGrad(image, useCalibration);
		Rebin_.rebin(grad, 256).show();
	}
	
	public static ImagePlus calculateGrad(ImagePlus imp, boolean useCalib) {
		
		IJ.showStatus("Calculating gradient");

		Calibration c = imp.getCalibration();
		float dx = useCalib ? 2*(float)c.pixelWidth : 2;
		float dy = useCalib ? 2*(float)c.pixelHeight : 2;
		float dz = useCalib ? 2*(float)c.pixelDepth : 2;

		float[] H_x = new float[] {-1/dx, 0, 1/dx};
		ImagePlus g_x = Convolve_3d.convolveX(imp, H_x);

		float[] H_y = new float[] {-1/dy, 0, 1/dy};
		ImagePlus g_y = Convolve_3d.convolveY(imp, H_y);

		float[] H_z = new float[] {-1/dz, 0, 1/dz};
		ImagePlus g_z = Convolve_3d.convolveZ(imp, H_z);

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
				x_[i]*x_[i] + y_[i]*y_[i] + z_[i]*z_[i]);
			}
		}
		ImagePlus ret = new ImagePlus("Gradient", grad);
		ret.setCalibration(c);
		return ret;
	}
}
