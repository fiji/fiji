package video2;

import java.awt.Rectangle;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.gui.Roi;
import ij.gui.GenericDialog;

import ij.process.Blitter;
import ij.process.ImageProcessor;

import ij.plugin.filter.PlugInFilter;

public class Move_Roi implements PlugInFilter {

	private ImagePlus image;

	public static final int DEF_PIX_PER_SLICE = 3;

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {

		int current = image.getCurrentSlice();

		GenericDialog gd = new GenericDialog("Delete Frame");
		gd.addNumericField("Slice", current, 0);
		gd.addNumericField("Pixels per slice", DEF_PIX_PER_SLICE, 0);
		gd.addNumericField("dx", 0, 0);
		gd.addNumericField("dy", 0, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int slice = (int)gd.getNextNumber();
		int speed = (int)gd.getNextNumber();
		int dx = (int)gd.getNextNumber();
		int dy = (int)gd.getNextNumber();

		moveRoi(image, image.getRoi(), dx, dy, slice, speed);
	}

	/**
	 * Copy the given roi from the specified slice of the given image
	 * and inserts it in consecutive frames, giving the effect of
	 * moving the selection.
	 * @param image The ImagePlus containing the frames
	 * @param roi   The selection to move.
	 * @param dx    The amount in x-direction to move.
	 * @param dy    The amount in y-direction to move.
	 * @param slice The starting slice.
	 * @param speed The amount of pixels to move per slice. A value lower
	 *              than 1 will move the roi within one frame.
	 */
	public static void moveRoi(ImagePlus image,
				Roi roi, int dx, int dy, int slice, int speed) {

		if(roi == null) {
			IJ.error("Roi required");
			return;
		}
		ImageStack stack = image.getStack();
		Rectangle r = roi.getBounds();
		int dt = Math.abs(dx) > Math.abs(dy) ? Math.abs(dx) : Math.abs(dy);
		ImageProcessor ip = stack.getProcessor(slice);
		ip.setRoi(roi);
		ImageProcessor copy = ip.crop();
		ImageProcessor ip2 = null;
		int inserted = 0;
		for(int i = 0; i < dt; i++) {
			int xt = r.x + Math.round((float)i * dx/dt);
			int yt = r.y + Math.round((float)i * dy/dt);
			ip2 = ip.duplicate();
			ip2.snapshot();
			roi.setLocation(xt, yt);
			ip2.setRoi(roi);
			ip2.copyBits(copy, xt, yt,
					Blitter.COPY_ZERO_TRANSPARENT);

			ip2.reset(ip2.getMask());
			if(speed < 1 || i % speed != 0)
				continue;
			stack.addSlice("", ip2, slice + inserted);
			inserted++;
		}
		// maybe the last one was not added (in case i % speed was 0)
		if(speed < 1 || dt % speed != 0)
			stack.addSlice("", ip2, slice + inserted);
	}
}

