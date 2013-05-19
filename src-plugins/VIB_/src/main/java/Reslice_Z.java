import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.ImageStack;
import ij.IJ;
import ij.ImagePlus;

import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Reslice_Z implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip) {
		double pd = image.getCalibration().pixelDepth;
		GenericDialog gd = new GenericDialog("Reslice_Z");
		gd.addNumericField("New pixel depth", pd, 3);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		pd = gd.getNextNumber();
		reslice(image, pd).show();
	}

	public static ImagePlus reslice(ImagePlus image, double pixelDepth) {

		int w = image.getWidth();
		int h = image.getHeight();

		Calibration cal = image.getCalibration();

		ImageStack stack = image.getStack();
		int numSlices = (int)Math.round(image.getStackSize() * cal.pixelDepth /
					pixelDepth);

		// Create a new Stack
		ImageStack newStack = new ImageStack(w, h);
		for(int z = 0; z < numSlices; z++) {
			double currentPos = z * pixelDepth;

			// getSliceBefore
			int ind_p = (int)Math.floor(currentPos / cal.pixelDepth);
			int ind_n = ind_p + 1;

			double d_p = currentPos - ind_p*cal.pixelDepth;
			double d_n = ind_n*cal.pixelDepth - currentPos;

			if(ind_n >= stack.getSize())
				ind_n = stack.getSize() - 1;

			ImageProcessor before = stack.getProcessor(ind_p + 1).duplicate();
			ImageProcessor after  = stack.getProcessor(ind_n + 1).duplicate();

			before.multiply(d_n / (d_n + d_p));
			after.multiply(d_p / (d_n + d_p));

			before.copyBits(after, 0, 0, Blitter.ADD);

			newStack.addSlice("", before);
		}
		ImagePlus result = new ImagePlus("Resliced", newStack);
		cal = cal.copy();
		cal.pixelDepth = pixelDepth;
		result.setCalibration(cal);
		return result;
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	}
}
