package video2;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.process.ImageProcessor;

import ij.plugin.filter.PlugInFilter;


public class Insert_Stack implements PlugInFilter {

	private ImagePlus image;

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {

		int current = image.getCurrentSlice();
		String[] images = openImages();
		if(images.length == 0) {
			IJ.error("No other images open");
			return;
		}

		GenericDialog gd = new GenericDialog("Insert stack");
		gd.addChoice("Stack", images, images[0]);
		gd.addNumericField("Slice number", current, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int slice = (int)gd.getNextNumber();
		ImagePlus src = WindowManager.getImage(gd.getNextChoice());

		insertStack(image, slice, src);
	}

	private String[] openImages() {
		int[] ids = WindowManager.getIDList();
		String[] titles = new String[ids.length - 1];
		for(int i = 0, c = 0; i < ids.length; i++) {
			int id = ids[i];
			ImagePlus im = WindowManager.getImage(id);
			if(im != this.image)
				titles[c++] = im.getTitle();
		}
		return titles;
	}

	public static void insertStack(ImagePlus tgt, int slice, ImagePlus src) {

		int w = tgt.getWidth(), h = tgt.getHeight();

		ImageStack source = src.getStack();
		ImageStack target = tgt.getStack();

		if(source.getWidth() != w || source.getHeight() != h)
			throw new IllegalArgumentException(
				"Stack to insert has wrong dimensions");

		int d = src.getStackSize();
		for(int n = 0; n < d; n++) {
			ImageProcessor frame = source.getProcessor(n + 1);
			target.addSlice("", frame, slice + n);
		}

		// causes the change from image window to stack window
		tgt.setStack(null, target);
	}
}
