package video2;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;

import ij.process.ImageProcessor;

import ij.plugin.filter.PlugInFilter;

public class Add_Empty_Frame implements PlugInFilter {

	private ImagePlus image;

	public static final int DEF_NUM_SLICES = 1;

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}
	
	public void run(ImageProcessor ip) {

		int current = image.getCurrentSlice();

		GenericDialog gd = new GenericDialog("Add Frame");
		gd.addNumericField("After slice", current, 0);
		gd.addNumericField("Number of slices", DEF_NUM_SLICES, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int slice = (int)gd.getNextNumber();
		int num = (int)gd.getNextNumber();

		addFrames(image, slice, num);
	}

	public static void addFrames(ImagePlus imp, int slice, int num) {
		ImageStack stack = imp.getStack();
		int w = imp.getWidth(), h = imp.getHeight();
		ImageProcessor frame = stack.getProcessor(slice).
							createProcessor(w, h);
		frame.setValue(0);
		frame.fill();

		for(int n = 0; n < num; n++)
			stack.addSlice("", frame, slice + n);

		// causes the change from image window to stack window
		imp.setStack(null, stack);
	}
}
