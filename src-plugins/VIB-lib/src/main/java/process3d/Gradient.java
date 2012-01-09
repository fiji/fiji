package process3d;

import ij.measure.Calibration;

import ij.plugin.filter.PlugInFilter;

import ij.gui.GenericDialog;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import ij.ImagePlus;
import ij.ImageStack;
import ij.IJ;

public class Gradient {

	public static ImagePlus calculateGrad(ImagePlus imp, boolean useCalib) {
		
		IJ.showStatus("Calculating gradient");

		Calibration c = imp.getCalibration();
		float dx = useCalib ? 2*(float)c.pixelWidth : 2;
		float dy = useCalib ? 2*(float)c.pixelHeight : 2;
		float dz = useCalib ? 2*(float)c.pixelDepth : 2;

		float[] H_x = new float[] {-1/dx, 0, 1/dx};
		ImagePlus g_x = Convolve3d.convolveX(imp, H_x);

		float[] H_y = new float[] {-1/dy, 0, 1/dy};
		ImagePlus g_y = Convolve3d.convolveY(imp, H_y);

		float[] H_z = new float[] {-1/dz, 0, 1/dz};
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
				x_[i]*x_[i] + y_[i]*y_[i] + z_[i]*z_[i]);
			}
		}
		ImagePlus ret = new ImagePlus("Gradient", grad);
		ret.setCalibration(c);
		return ret;
	}
}
