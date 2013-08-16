/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package process3d;

import java.util.Arrays;

import ij.process.ImageProcessor;

import ij.ImagePlus;
import ij.IJ;

import ij.plugin.Duplicator;

/**
 * This class implements the minimum, maximum and median filter.
 * The kernel size is fixed with a diameter of 3 pixels. This 
 * makes sense since all three filters are computationally very
 * expensive. Computational complexity is related to the third power 
 * of the diameter, so 2-fold diameter means 8-fold computation time.
 */
public class MinMaxMedian {

	public static void main(String[] args) {
		ImagePlus imp = IJ.getImage();
		convolve(imp, MEDIAN).show();
	}

	/** Constant representing the minimum filter. */
	public static final int MINIMUM = 0;
	/** Constant representing the maximum filter. */
	public static final int MAXIMUM = 1;
	/** Constant representing the median filter. */
	public static final int MEDIAN  = 2;

	private static final int diameter = 3;

	/** 
	 * Main method which iterates through the stack and calls 
	 * convolvePoint() for each voxel.
	 */
	public static ImagePlus convolve(ImagePlus image, int method) {
		
		if(method < 0 || method >= 3) {
			IJ.error("Neither MINIMUM nor MAXIMUM nor MEDIAN chosen");
			return null;
		}

		ImagePlus result = new Duplicator().run(image);

		// Determine dimensions of the image
		int w = image.getWidth();
		int h = image.getHeight();
		int d = image.getStackSize();

		ImageProcessor[] in = new ImageProcessor[d];
		ImageProcessor[] out = new ImageProcessor[d];
		for(int z = 0; z < d; z++) {
			in[z]  = image.getStack().getProcessor(z + 1);
			out[z] = result.getStack().getProcessor(z + 1);
		}
		
		float[] values = new float[diameter * diameter * diameter];
		int r = diameter / 2;

		for(int z = 0; z < d; z++) {
			IJ.showProgress(0, d);
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					int idx = 0;
					for(int k = z - r; k <= z + r; k++) {
						if(k < 0 || k >= d)
							continue;

						for(int j = y - r; j <= y + r; j++) {
							if(j < 0 || j >= h)
								continue;

							for(int i = x - r; i <= x + r; i++) {
								if(i >= 0 && i < w)
									values[idx++] = in[k].getf(i, j);
							}
						}
					}
					Arrays.sort(values, 0, idx);
					float target = 0;
					switch(method) {
						case MINIMUM: target = values[0]; break;
						case MAXIMUM: target = values[idx - 1]; break;
						case MEDIAN:  target = values[idx / 2]; break;
					}
					out[z].setf(x, y, target);
				}
			}
		}
		
		// create output image
		String title = "";
		switch (method) {
			case MINIMUM: title = "Minimum"; break;
			case MAXIMUM: title = "Maximum"; break;
			case MEDIAN:  title = "Median";  break;
		}

		result.setTitle(title + " of " + image.getTitle());
		return result;
	}
}
