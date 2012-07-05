package process3d;

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Find_Minima extends FindMinima implements PlugInFilter {

	public Find_Minima() {}

	public Find_Minima(ImagePlus image) {
		super(image);
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G;
	}

	public void run(ImageProcessor ip) {
		init(image);
		classify().show();
	}
}

