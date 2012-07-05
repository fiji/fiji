package process3d;

import java.awt.image.ColorModel;

import ij.plugin.filter.PlugInFilter;

import ij.gui.GenericDialog;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import ij.ImagePlus;
import ij.ImageStack;
import ij.IJ;

/**
 * This class implements the dilation filter.
 * The kernel size is fixed with a diameter of 3 pixels. This 
 * makes sense the operation gets rapidly computationally more
 * expensive with increasing diameter. Computational complexity 
 * is related to the third power of the diameter, so 2-fold 
 * diameter means 8-fold computation time.
 * For complexity reasons, the implementation uses a 6-neighbour-
 * hood and not a 27-neighborhood.
 */
public class Dilate_ implements PlugInFilter {

	private int w, h, d;
	private ImagePlus image;
	private byte[][] pixels_in;
	private byte[][] pixels_out;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Dilate");
		gd.addNumericField("Iso value", 255, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		dilate(image, (int)gd.getNextNumber(), false).show();
	}

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | DOES_8C | NO_CHANGES;
	}

	public ImagePlus dilate(ImagePlus image, int threshold, boolean newWin){

		// Determine dimensions of the image
		w = image.getWidth(); h = image.getHeight();
		d = image.getStackSize();

		pixels_in = new byte[d][];
		pixels_out = new byte[d][];
		for(int z = 0; z < d; z++) {
			pixels_in[z] = (byte[])image.getStack().getPixels(z+1);
			pixels_out[z] = new byte[w*h];
		}
		
		// iterate
		for(int z = 0; z < d; z++) {
			IJ.showProgress(z, d-1);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					if(get(x, y, z) == threshold ||
						get(x-1, y, z) == threshold ||
						get(x+1, y, z) == threshold ||
						get(x, y-1, z) == threshold ||
						get(x, y+1, z) == threshold ||
						get(x, y, z-1) == threshold ||
						get(x, y, z+1) == threshold)

						set(x, y, z, threshold);
					else
						set(x, y, z, get(x, y, z));
				}
			}
		}

		ColorModel cm = image.getStack().getColorModel();
		
		// create output image
		ImageStack stack = new ImageStack(w, h);
		for(int z = 0; z < d; z++) {
			stack.addSlice("", new ByteProcessor(
				w, h, pixels_out[z], cm));
		}
		if(!newWin) {
			image.setStack(null, stack);
			return image;
		}
		ImagePlus result = new ImagePlus(
					image.getTitle() + "_dilated", stack);
		result.setCalibration(image.getCalibration());
		return result;
	}

	public int get(int x, int y, int z) {
		x = x < 0 ? 0 : x; x = x >= w ? w-1 : x;
		y = y < 0 ? 0 : y; y = y >= h ? h-1 : y;
		z = z < 0 ? 0 : z; z = z >= d ? d-1 : z;
		return (int)(pixels_in[z][y*w + x] & 0xff);
	}

	public void set(int x, int y, int z, int v) {
		pixels_out[z][y*w + x] = (byte)v;
	}
}
