package fiji.plugin.nperry;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.*;
import ij.process.ImageProcessor;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionRealType;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;

public class Embryo_Tracker<T extends RealType<T>> implements PlugIn {
	/** Class/Instance variables */
	protected Image<T> img;								// Stores the image used by Imglib

	/** Ask for parameters and then execute. */
	public void run(String arg) {
		// 1 - Obtain the currently active image:
		ImagePlus imp = IJ.getImage();
		if (null == imp) return;
		
		// 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Track");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		// 3 - Execute!
		Object[] result = exec(imp);
		
		// Display (for testing)
		if (null != result) {
			ImagePlus scaled = (ImagePlus) result[1];
			scaled.show();
		}
	}
	
	/** Execute the plugin functionality: apply a Gaussian blur, and find maxima. */
	public Object[] exec(ImagePlus imp) {
		// 0 - Check validity of parameters:
		if (null == imp) return null;
		
		// 1 - Set up for use with Imglib:
		img = ImagePlusAdapter.wrap(imp);
		
		// 2 - Apply a Gaussian filter. Theoretically, this will make the center of blobs the brightest, and thus easier to find:
		// Note: Simple 2D case!!! use ComputeGaussFloatArray3D probably for 3D case...
		final GaussianConvolutionRealType<T> conv = new GaussianConvolutionRealType<T>(img, new OutOfBoundsStrategyMirrorFactory<T>(), 5.0f); // Use sigma of 5.0f, probably need a better way to do this
		final Image<T> gauss; 
		if (conv.checkInput() && conv.process()) { 
			gauss = conv.getResult(); 
		} else { 
	        System.out.println(conv.getErrorMessage()); 
	        return null;
		}
		
		// 3 - Find maxima of newly convoluted image:
		// to-do...
		
		// Return (for testing):
		ImagePlus newImg = ImageJFunctions.copyToImagePlus(gauss, imp.getType());  	// convert Image<T> to ImagePlus
		if (imp.isInvertedLut()) {													// if original image had inverted LUT, invert this new image's LUT also
			ImageProcessor newImgP = newImg.getProcessor();
			newImgP.invertLut();
		}
		return new Object[]{"new", newImg};
	}
}
