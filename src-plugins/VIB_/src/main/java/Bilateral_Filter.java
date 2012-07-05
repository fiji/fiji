import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.plugin.filter.PlugInFilter;

import vib.BilateralFilter;

/*

 This plugin implements the Bilateral Filter, described in

  C. Tomasi and R. Manduchi, "Bilateral Filtering for Gray and Color Images",
  Proceedings of the 1998 IEEE International Conference on Computer Vision,
  Bombay, India.

 Basically, it does a Gaussian blur taking into account the intensity domain
 in addition to the spatial domain (i.e. pixels are smoothed when they are
 close together _both_ spatially and by intensity.

*/
public class Bilateral_Filter implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Bilateral Parameters");
		gd.addNumericField("spatial radius", 3, 0);
		gd.addNumericField("range radius", 50, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		double spatialRadius = gd.getNextNumber();
		double rangeRadius = gd.getNextNumber();
		BilateralFilter.filter(
			image, spatialRadius, rangeRadius).show();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G;
	}
}

