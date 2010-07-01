/** 
 * Author: Nick Perry
 * Description: 
 */

package fiji.plugin.nperry;

import java.util.ArrayList;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.*;
import ij.process.ImageProcessor;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionRealType;
import mpicbg.imglib.algorithm.roi.MedianFilter;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
//import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor3D;
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
		gd.addNumericField("Average blob diameter (pixels):", 0, 0);  // get the expected blob size (in pixels).
		gd.addChoice("Search type:", new String[] {"Maxima", "Minima"}, "Maxima");  // determines if we are searching for maxima, or minima.
		gd.showDialog();
		if (gd.wasCanceled()) return;

		// 3 - Retrieve parameters from dialogue:
		int diam = (int)gd.getNextNumber();
		String searchType = gd.getNextString();
		
		// 4 - Execute!
		Object[] result = exec(imp, diam, searchType);
		
		// Display (for testing)
		if (null != result) {
			ImagePlus scaled = (ImagePlus) result[1];
			scaled.show();
		}
	}
	
	/** Execute the plugin functionality: apply a median filter (for salt and pepper noise), a Gaussian blur, and then find maxima. */
	public Object[] exec(ImagePlus imp, int diam, String searchType) {
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
		final GaussianConvolutionRealType<T> conv = new GaussianConvolutionRealType<T>(img, new OutOfBoundsStrategyMirrorFactory<T>(), 10.0f); // Use sigma of 10.0f, probably need a better way to do this
		if (conv.checkInput() && conv.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
			img = conv.getResult(); 
		} else { 
	        System.out.println(conv.getErrorMessage()); 
	        return null;
		}
		
		// 3.5 - Apply a Laplace transform
		
		// 4 - Find maxima of newly convoluted image:
		findMaxima(img);
		
		// 5 - Return (for testing):
		ImagePlus newImg = ImageJFunctions.copyToImagePlus(img, imp.getType());  	// convert Image<T> to ImagePlus
		if (imp.isInvertedLut()) {													// if original image had inverted LUT, invert this new image's LUT also
			ImageProcessor newImgP = newImg.getProcessor();
			newImgP.invertLut();
		}
		return new Object[]{"new", newImg};
	}
	
	public void findMaxima(Image<T> img) {
		// 1 - Initialize local variables, cursors
		final LocalizableByDimCursor<T> curr = img.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>());  // adding a OutOfBounds strategy because the cursor can be on the border, and the neighborhood cursor will search its nonexistent neighbors beyond the limits of the image.
		/** Note: writing for 3D case only right now to test */
		final LocalNeighborhoodCursor3D<T> neighbors = new LocalNeighborhoodCursor3D<T>(curr);
		ArrayList< int[] > maxCoordinates = new ArrayList< int[] >();
		T currentValue;  // holds the value of the current pixel's intensity
		T neighborValue; // holds the value of the neighbor's intensity
		
		// 2 - Search all pixels for LOCAL maxima. A local maximum is a pixel that is the brightest in its 3D neighborhood (so the pixel is brighter or as bright as the 26 direct neighbors of it's cube-shaped neighborhood.
		while(curr.hasNext()) {
			curr.fwd();			 	// select the next pixel
			boolean isMax = true;  	// this pixel could be a max
			currentValue = curr.getType();
			neighbors.update();
			while(neighbors.hasNext()) {	//check this pixel's immediate neighbors
				neighbors.fwd();
				neighborValue = neighbors.getType();
				if (neighborValue.compareTo(currentValue) > 0) {  // if the neighbor's value is greater than our pixel's value, our pixel can no longer be a maximum so set isMax to false.
					isMax = false;
					break;
				}
			}
			if (isMax) {
				maxCoordinates.add(curr.getPosition());
			}
			// print out the list of maxima
			// to-do...
		}
		curr.close();
		neighbors.close();
	}
}
