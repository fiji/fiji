package video2;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;

import ij.process.ImageProcessor;

import ij.plugin.filter.PlugInFilter;

public class Delete_Frame implements PlugInFilter {

	private ImagePlus image;

	public static final int DEF_NUM_SLICES = 1;

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}
	
	public void run(ImageProcessor ip) {

		int current = image.getCurrentSlice();

		GenericDialog gd = new GenericDialog("Delete Frame");
		gd.addNumericField("Slice", current, 0);
		gd.addNumericField("Number of slices", DEF_NUM_SLICES, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int slice = (int)gd.getNextNumber();
		int num = (int)gd.getNextNumber();

		deleteFrames(image, slice, num);
	}

	public static void deleteFrames(ImagePlus imp, int slice, int num) {
		ImageStack stack = imp.getStack();
		for(int n = 0; n < num; n++)
			stack.deleteSlice(slice);

		/*
		 * causes the change from image window to stack window
		 * also:
		 * causes update of current slice. Previously, the
		 * current slice was not updated, so showed the wrong
		 * image. When the user then scrolled, the wrong image
		 * was stored to the corresponding file.
		 */
		imp.setStack(null, stack);
	}
}
