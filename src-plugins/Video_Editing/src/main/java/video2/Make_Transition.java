package video2;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import ij.plugin.filter.PlugInFilter;


public class Make_Transition implements PlugInFilter {

	private ImagePlus image;

	public static final int FADE      = 0;
	public static final int H_STRIPES = 1;
	public static final int V_STRIPES = 2;

	public static final int DEF_NUM_SLICES = 20;
	public static final int DEF_TRANSITION = FADE;

	private static final Transition[] transitions = new Transition[] {
				new FadeTransition(),
				new HStripesTransition(),
				new VStripesTransition()};

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}
	
	public void run(ImageProcessor ip) {

		int current = image.getCurrentSlice();

		GenericDialog gd = new GenericDialog("Make Transition");
		gd.addNumericField("From slice", current, 0);
		gd.addNumericField("Number of slices", DEF_NUM_SLICES, 0);
		String[] tstrings = new String[] {
			"Fade transition", "Horizontal stripes", "Vertical stripes"};
		gd.addChoice("Transition type", tstrings, tstrings[DEF_TRANSITION]);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int from = (int)gd.getNextNumber();
		int num = (int)gd.getNextNumber();
		int type = gd.getNextChoiceIndex();

		makeTransition(image, from, num, type);
	}

	public static void makeTransition(
			ImagePlus imp, int from, int num, int type) {
		transitions[type].makeTransition(imp, from, num);
	}

	/**
	 * Transition interface.
	 */
	public interface Transition {
		public void makeTransition(ImagePlus imp, int from, int num);
	}

	/**
	 * Horizontal stripes
	 */
	private static class HStripesTransition implements Transition {
		public void makeTransition(ImagePlus imp, int from, int num) {
			ImageStack stack = imp.getStack();
			int w = imp.getWidth(), h = imp.getHeight();
			ImageProcessor fr = stack.getProcessor(from).duplicate();
			ImageProcessor to = stack.getProcessor(from + 1);
			int[] row = new int[w];
			for(int n = 0; n < num; n++) {
				for(int y = n; y < h; y += num) {
					to.getRow(0, y, row, w);
					fr.putRow(0, y, row, w);
				}
				stack.addSlice("", fr, from + n);
			}
		}
	}

	/**
	 * Vertical stripes
	 */
	private static class VStripesTransition implements Transition {
		public void makeTransition(ImagePlus imp, int from, int num) {
			ImageStack stack = imp.getStack();
			int w = imp.getWidth(), h = imp.getHeight();
			ImageProcessor fr = stack.getProcessor(from).duplicate();
			ImageProcessor to = stack.getProcessor(from + 1);
			int[] col = new int[h];
			for(int n = 0; n < num; n++) {
				for(int x = n; x < w; x += num) {
					to.getColumn(x, 0, col, h);
					fr.putColumn(x, 0, col, h);
				}
				stack.addSlice("", fr, from + n);
			}
		}
	}


	/**
	 * Fading transition
	 */
	private static class FadeTransition implements Transition {
		public void makeTransition(ImagePlus imp, int from, int num) {
			if(from > imp.getStackSize()) {
				IJ.error("Need a following slice to which to transit.");
				return;
			}
			num++; // so that really num slices are added
			ImageStack stack = imp.getStack();
			int[] before = (int[])(stack.getProcessor(from).
					convertToRGB().getPixels());
			int[] after = (int[])(stack.getProcessor(from+1).
					convertToRGB().getPixels());

			for(int z = 1; z < num; z++) {
				ColorProcessor bp = new ColorProcessor(
					stack.getWidth(), stack.getHeight());
				int[] pixels = (int[])bp.getPixels();
				double dp = z;
				double dn = num - z;

				for(int i = 0; i < pixels.length; i++) {
					pixels[i] = interpolate(
						before[i], dp, after[i], dn);
				}
				new ImagePlus("slice + " + z, bp).show();
				stack.addSlice("", bp, from + z - 1);
			}
		}

		public static int interpolate(int p, double dp, int n, double dn) {
			int rp = (p&0xff0000) >> 16;
			int rn = (n&0xff0000) >> 16;
			int gp = (p&0xff00) >> 8;
			int gn = (n&0xff00) >> 8;
			int bp = p&0xff;
			int bn = n&0xff;

			byte r_int = (byte) ((rn*dp + rp*dn) / (dn + dp));
			byte g_int = (byte) ((gn*dp + gp*dn) / (dn + dp));
			byte b_int = (byte) ((bn*dp + bp*dn) / (dn + dp));

			return ((r_int&0xff) << 16) +
				((g_int&0xff) << 8) +
				(b_int&0xff);
		}
	}
}
