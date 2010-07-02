/** 
 * Author: Nick Perry
 * Description: 
 */

package fiji.plugin.nperry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.*;
import ij.process.ImageProcessor;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionRealType;
import mpicbg.imglib.algorithm.math.MathLib;
import mpicbg.imglib.algorithm.roi.MedianFilter;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor3D;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor3D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
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
		int numDim = img.getNumDimensions();
		/*if (numDim == 3) {  // 3D case
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
		}*/
		
		// 3 - Apply a Gaussian filter (code courtesy of Stephan Preibisch). Theoretically, this will make the center of blobs the brightest, and thus easier to find:
		/*final GaussianConvolutionRealType<T> conv = new GaussianConvolutionRealType<T>(img, new OutOfBoundsStrategyMirrorFactory<T>(), 6.0f); // Use sigma of 10.0f, probably need a better way to do this
		if (conv.checkInput() && conv.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
			img = conv.getResult(); 
		} else { 
	        System.out.println(conv.getErrorMessage()); 
	        return null;
		}*/
		
		// 3.5 - Apply a Laplace transform?
		
		// 4 - Find maxima of newly convoluted image:
		findMaxima(img, numDim);
		
		// 5 - Return (for testing):
		ImagePlus newImg = ImageJFunctions.copyToImagePlus(img, imp.getType());  	// convert Image<T> to ImagePlus
		if (imp.isInvertedLut()) {													// if original image had inverted LUT, invert this new image's LUT also
			ImageProcessor newImgP = newImg.getProcessor();
			newImgP.invertLut();
		}
		return new Object[]{"new", newImg};
	}
	
	public void findMaxima(Image<T> img, int numDim) {
		// 1 - Initialize local variables, cursors
		final LocalizableByDimCursor<T> curr = img.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>());  // adding a OutOfBounds strategy because the cursor can be on the border, and the neighborhood cursor will search its nonexistent neighbors beyond the limits of the image.  
		LocalNeighborhoodCursor<T> neighbors = null;
		LocalizableByDimCursor<T> local = null;
		ArrayList< int[] > maxCoordinates = new ArrayList< int[] >();  // holds the positions of the local maxima
		T currentValue = img.createType();  // holds the value of the current pixel's intensity. We use createType() here because getType() gives us a pointer to the cursor's object, but since the neighborhood moves the parent cursor, when we check the neighbors, we actually change the object stored here, or the pixel we are trying to compare to. see fiji-devel list for further explanation.
		T neighborValue; // holds the value of the neighbor's intensity
		int width = img.getDimensions()[1];
		int numPixelsInXYPlane = img.getDimensions()[0] * width;
		boolean visited[] = new boolean[img.getNumPixels()];	// everything is initialized to false
		
		// 2 - Search all pixels for LOCAL maxima. A local maximum is a pixel that is the brightest in its immediate neighborhood (so the pixel is brighter or as bright as the 26 direct neighbors of it's cube-shaped neighborhood if 3D).
		int count = 0;
		//LinkedList< int[] > toSearch = new LinkedList< int[]>();	// this LL will hold all the coordinates we need to search for the given pixel. Essentially, if we have a pixel that is a max, but some neighbors share the same maximal value, we need to search the neighbors too until we can make sure this either is either a maximal area or not.
		ConcurrentLinkedQueue< int[] > toSearch = new ConcurrentLinkedQueue< int[] >();
		ConcurrentLinkedQueue< int[] > searched = new ConcurrentLinkedQueue< int[] >();
		// 2.1 - Iterate over all pixels in the image.
		while(curr.hasNext()) { 
			curr.fwd();			 									// select the next pixel
			IJ.log("Current pixel for outer loop: " + MathLib.printCoordinates(curr.getPosition()));
			if (visited[getIndexOfPosition(curr.getPosition(), width, numPixelsInXYPlane)]) {
				IJ.log(MathLib.printCoordinates(curr.getPosition()) + " has been visited, skipping.");
				continue;
			}
			boolean isMax = true;  									// this pixel could be a max
			toSearch.add(curr.getPosition());  						// add this initial pixel to the LL of pixels we need to search (currently the only thing in the LL)
			//ListIterator<int[]> itr = toSearch.listIterator(); 	 	// iterate our LL.
			// 2.2 - Iterate through LL which contains the pixels of the "lake"
			IJ.log("Starting lake search...");
			while (!toSearch.isEmpty()) {									// conceptually, we are searching the "lake of equal maximum value" here
				IJ.log("searching lake...");
				int next[] = toSearch.poll();
				searched.add(next);
				IJ.log("Top of LL: " + MathLib.printCoordinates(next));
				if (visited[getIndexOfPosition(next, width, numPixelsInXYPlane)]) {	// if we've seen
					IJ.log(MathLib.printCoordinates(next) + " has been visited, skipping.");
					continue;
				} else {															// if we've never seen
					visited[getIndexOfPosition(next, width, numPixelsInXYPlane)] = true;	
					IJ.log(MathLib.printCoordinates(next) + " has NOT been visited, adding to visited and processing neighbors.");
				}
				local = img.createLocalizableByDimCursor(new OutOfBoundsStrategyValueFactory<T>());  // new cursor that will search this pixel and it's "area"
				neighbors = new LocalNeighborhoodCursor3D<T>(local);
				local.setPosition(next);
				currentValue.set(local.getType());  				// store the value of this pixel in a variable
				neighbors.update();
				// 2.3 - Iterate through immediate neighbors
				IJ.log("Neighbor search starting");
				while(neighbors.hasNext()) {						//check this pixel's immediate neighbors
					neighbors.fwd();	// for now, allowed to check already seen neighbors, since they will simply be skipped after added to LL
					IJ.log("current neighbor: " + MathLib.printCoordinates(local.getPosition()));
					neighborValue = neighbors.getType();
					// Case 1: neighbor's value is strictly larger than ours, so ours cannot be a local maximum.
					if (neighborValue.compareTo(currentValue) > 0) {
						IJ.log("Neighbor is greater than top of LL: " + MathLib.printCoordinates(local.getPosition()));
						isMax = false;
						// Empty out the remainder of the LL so that we stop searching it. Use itr's remove() method to prevent the iterator from crashing.
						toSearch.clear();
						break;
					}
					
					// ----> need to deal with somehow preventing a black pixel from being isolated by visited pixels, making it a max.
					// Case 2: neighbor's value is strictly equal to ours, which means we could still be at a maximum, but the max value is a blob, not just a single point. We must check the area.
					else if (neighborValue.compareTo(currentValue) == 0 && isInner(local.getPosition()) && !visited[getIndexOfPosition(local.getPosition(), width, numPixelsInXYPlane)]) {
						toSearch.add(local.getPosition());  // add to LL using the iterator's 'add'; if we used the LL's 'add' we would break the itr
						IJ.log("Part of lake, being added to LL: " + MathLib.printCoordinates(local.getPosition()));
					}
				}
				neighbors.reset();  								// needed to get the outer cursor to work correctly;		
			}
			int count2 = 0;  //DEBUG
			if (isMax) {	// if we get here, we've searched the entire lake, so find the average point and call that a max by adding to results list
				while (!searched.isEmpty()) {
					count2 ++;  //DEBUG
					int pos[] = searched.poll();
					IJ.log("*** Max found: " + MathLib.printCoordinates(pos) + " ***");
					visited[getIndexOfPosition(pos, width, numPixelsInXYPlane)] = true;
					maxCoordinates.add(pos);	
				}
			} else {
				while (!searched.isEmpty()) {
					int pos[] = searched.poll();
					visited[getIndexOfPosition(pos, width, numPixelsInXYPlane)] = true;
				}
			}
			//IJ.log("added to max list: " + Integer.toString(count2)); //DEBUG
			toSearch.clear();										// clear our "lake" LL since we are moving onto the next pixel in the main loop, and no longer at this lake
			count++;
		}
		curr.close();
		neighbors.close();
		
		// 3 - Print out list of maxima (FOR TESTING):
		IJ.log("Count:" + count);
		String img_dim = MathLib.printCoordinates(img.getDimensions());
		IJ.log("Image dimensions: " + img_dim);
		Iterator<int[]> itr = maxCoordinates.iterator();
		while (itr.hasNext()) {
			String pos_str = MathLib.printCoordinates(itr.next());
			IJ.log(pos_str);
		}
	}
	
	/**
	 * Given a position array, returns whether or not the position is within the bounds of the image, or out of bounds.
	 * 
	 * @param pos
	 * @return
	 */
	public boolean isInner(int pos[]) {
		if (pos.length == 2) {
			return pos[0] > -1 && pos[0] < img.getDimension(0) && pos[1] > -1 && pos[1] < img.getDimension(1);
		} else {
			//IJ.log(MathLib.printCoordinates(pos));
			return pos[0] > -1 && pos[0] < img.getDimension(0) && pos[1] > -1 && pos[1] < img.getDimension(1) && pos[2] > -1 && pos[2] < img.getDimension(2);
		}
	}
	
	/**
	 * Given an array of a pixels position, the width of the image, and the number of pixels in a plane,
	 * returns the index of the position in a linear array as calculated by x + width * y + numPixInPlane * z.
	 *
	 * @param pos
	 * @param width
	 * @param numPixelsInXYPlane
	 * @return
	 */
	public int getIndexOfPosition(int pos[], int width, int numPixelsInXYPlane) {
		int z;
		if (pos.length == 2) {	// if 2D, assign z to 1 since the pos array has length 2
			z = 0;
		} else {				// otherwise, 3D
			z = pos[2];
		}
		return pos[0] + width * pos[1] + numPixelsInXYPlane * z;
	}
}
