package process3d;

import ij.ImageStack;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;

/**
 * Plugin which takes an ImagePlus and rebins the pixel values 
 * to the specified range.
 */
public class Rebin_ extends Rebin implements PlugInFilter {
	private ImagePlus image;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Rebin_");
		gd.addNumericField("min", 0.0f, 3);
		gd.addNumericField("max", 255f, 3);
		gd.addNumericField("nbins", 256, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		rebin(image, (float)(gd.getNextNumber()),
				(float)(gd.getNextNumber()),
				(int)(gd.getNextNumber())).show();
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_32;
	}
}
