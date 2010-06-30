/** 
 * Author: Nick Perry
 * Description: 
 */

package fiji.plugin.nperry;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.*;
import ij.process.ImageProcessor;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionRealType;
import mpicbg.imglib.algorithm.roi.MedianFilter;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.algorithm.roi.StructuringElement;

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
	
	/** Execute the plugin functionality: apply a median filter (for salt and pepper noise), a Gaussian blur, and then find maxima. */
	public Object[] exec(ImagePlus imp) {
		// 0 - Check validity of parameters:
		if (null == imp) return null;
		
		// 1 - Set up for use with Imglib:
		img = ImagePlusAdapter.wrap(imp);
		
		// 2 - Apply a median filter, to get rid of salt and pepper noise which could be mistaken for maxima in the algorithm:
		StructuringElement strel;
		
		// 2.1 - Need to figure out the dimensionality of the image in order to create a StructuringElement of the correct dimensionality (StructuringElement needs to have same dimensionality as the image):
		if (img.getNumDimensions() == 3) {  // 3D case
			strel = new StructuringElement(new int[]{3, 3, 1}, "3D Square");  // unoptimized shape for 3D case. Note here that we manually are making this shape (not using a class method). This code is courtesy of Larry Lindsey
			Cursor<BitType> c = strel.createCursor();  // in this case, the shape is manually made, so we have to manually set it, too.
			while (c.hasNext()) 
			{ 
			    c.fwd(); 
			    c.getType().setOne(); 
			} 
			c.close(); 
		} else {  							// 2D case
			strel = StructuringElement.createCube(2, 3);  // unoptimized shape
		}
		
		// 2.2 - Apply the median filter:
		final MedianFilter<T> medFilt = new MedianFilter<T>(img, strel, new OutOfBoundsStrategyMirrorFactory<T>()); 
		// ***note: add back medFilt.checkInput() when it's fixed ***
		if (medFilt.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
			img = medFilt.getResult(); 
		} else { 
	        System.out.println(medFilt.getErrorMessage()); 
	        return null;
		}
		
		// 3 - Apply a Gaussian filter (code courtesy of Stephan Preibisch). Theoretically, this will make the center of blobs the brightest, and thus easier to find:
		final GaussianConvolutionRealType<T> conv = new GaussianConvolutionRealType<T>(img, new OutOfBoundsStrategyMirrorFactory<T>(), 2.0f); // Use sigma of 2.0f, probably need a better way to do this
		if (conv.checkInput() && conv.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
			img = conv.getResult(); 
		} else { 
	        System.out.println(conv.getErrorMessage()); 
	        return null;
		}
		
		// 4 - Find maxima of newly convoluted image:
		// LocalNeighborhoodCursor3D
		findMaxima(img);
		
		// Return (for testing):
		ImagePlus newImg = ImageJFunctions.copyToImagePlus(img, imp.getType());  	// convert Image<T> to ImagePlus
		if (imp.isInvertedLut()) {													// if original image had inverted LUT, invert this new image's LUT also
			ImageProcessor newImgP = newImg.getProcessor();
			newImgP.invertLut();
		}
		return new Object[]{"new", newImg};
	}
	
	public void findMaxima(Image<T> img) {
		final LocalizableByDimCursor<T> cur = img.createLocalizableByDimCursor();
		final boolean localMaxima[] = new boolean[img.getNumPixels()];
		final int[] max_position = img.createPositionArray();
		while(cur.hasNext()) {      // iterate over all of the pixels in the image in order to search for maxima.
			boolean isMax = true;   // potentially, the next pixel could be a maximum, so label it that way for now until we discover otherwise.
			
		}
	}
}
