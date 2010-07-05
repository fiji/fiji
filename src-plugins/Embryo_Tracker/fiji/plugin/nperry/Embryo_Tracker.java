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
		findMaxima(img);
		
		// 5 - Return (for testing):
		ImagePlus newImg = ImageJFunctions.copyToImagePlus(img, imp.getType());  	// convert Image<T> to ImagePlus
		if (imp.isInvertedLut()) {													// if original image had inverted LUT, invert this new image's LUT also
			ImageProcessor newImgP = newImg.getProcessor();
			newImgP.invertLut();
		}
		return new Object[]{"new", newImg};
	}
	
	/**
	 * 
	 * @param img
	 */
	public void findMaxima(Image<T> img) {
		// 1 - Initialize local variables, cursors
		final LocalizableByDimCursor<T> curr = img.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>());  // this cursor is the main cursor which iterates over all the pixels in the image.  
		LocalNeighborhoodCursor<T> neighbors = null;					// this cursor is used to search the immediate neighbors of a pixel
		LocalizableByDimCursor<T> local = null;							// this cursor is used to search a connected "lake" of pixels, or pixels with the same value
		ArrayList< int[] > maxCoordinates = new ArrayList< int[] >();  	// holds the positions of the local maxima
		T currentValue = img.createType();  							// holds the value of the current pixel's intensity. We use createType() here because getType() gives us a pointer to the cursor's object, but since the neighborhood moves the parent cursor, when we check the neighbors, we actually change the object stored here, or the pixel we are trying to compare to. see fiji-devel list for further explanation.
		T neighborValue; 												// holds the value of the neighbor's intensity
		int width = img.getDimensions()[1];								// used for storing info in the visited and visitedLakeMember arrays correctly
		int numPixelsInXYPlane = img.getDimensions()[0] * width;		// used for storing info in the visited and visitedLakeMember arrays correctly
		boolean visited[] = new boolean[img.getNumPixels()];			// array stores whether or not this pixel has been visited.
		boolean visitedLakeMember[] = new boolean[img.getNumPixels()];	// any time a pixel is listed as 'visited' in the visited array, also listed as visited here. however, when searching a lake, we don't care if a pixel has been visited before, but rather if it's been visited in the current lake already. if it has, skip it when searching the lake. basically, this prevents the lake from being searched infinite times, while allowed previously visited pixels to be re-visited when searching lakes since they may determine whether the lake is a max or not.
		ConcurrentLinkedQueue< int[] > toSearch = new ConcurrentLinkedQueue< int[] >();	// holds the positions of pixels that belong to the current lake and need to have neighbors searched
		ConcurrentLinkedQueue< int[] > searched = new ConcurrentLinkedQueue< int[] >();	// holds the positions of pixels that belong to the current lake and have already had neighbors searched
		
		// 2 - Search all pixels for LOCAL maxima. A local maximum is a pixel that is the brightest in its immediate neighborhood (so the pixel is brighter or as bright as the 26 direct neighbors of it's cube-shaped neighborhood if 3D).
		
		// 2.1 - Iterate over all pixels in the image.
		while(curr.hasNext()) { 
			curr.fwd();
			//IJ.log("Outer loop: " + MathLib.printCoordinates(curr.getPosition()));
			if (visited[getIndexOfPosition(curr.getPosition(), width, numPixelsInXYPlane)]) {	// if we've already seen this pixel and decided what it is, skip it
				//IJ.log("====> Outer skipped.");
				continue;
			}
			boolean isMax = true;  				// this pixel could be a max
			toSearch.add(curr.getPosition());  	// add this initial pixel to the queue of pixels we need to search (currently the only thing in the queue)
			
			// 2.2 - Iterate through LL which contains the pixels of the "lake"
			/** ArrayList< int[] > connectedComponent = getConnectedComponent(curr.getPosition()); */
			//IJ.log("Connected Component:");
			while (!toSearch.isEmpty()) {		// conceptually, we are searching the "lake of equal maximum value" here
				int next[] = toSearch.poll();
				//IJ.log(MathLib.printCoordinates(next));
				if (visitedLakeMember[getIndexOfPosition(next, width, numPixelsInXYPlane)]) {	// prevents us from just searching the lake infinitely
					//IJ.log("====> Visited, skipped.");
					continue;
				} else {	// if we've never seen, add to both visited lists.
					visited[getIndexOfPosition(next, width, numPixelsInXYPlane)] = true;	
					visitedLakeMember[getIndexOfPosition(next, width, numPixelsInXYPlane)] = true;	
					searched.add(next);
				}
				local = img.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>());  // new cursor that will search this pixel
				neighbors = new LocalNeighborhoodCursor3D<T>(local);	// new cursor that will search the above pixel's immediate neighbors
				local.setPosition(next);
				currentValue.set(local.getType());  // store the value of this pixel in a variable
				neighbors.update();
				
				// 2.3 - Iterate through immediate neighbors
				while(neighbors.hasNext()) {
					neighbors.fwd();
					neighborValue = neighbors.getType();
					// Case 1: neighbor's value is strictly larger than ours, so ours cannot be a local maximum.
					if (neighborValue.compareTo(currentValue) > 0) {
						//IJ.log("LAKE KILLED by: " + MathLib.printCoordinates(local.getPosition()));
						isMax = false;
						while(!toSearch.isEmpty()) {	// clear the queues because we no longer need to search. they have been marked as visited and will remain false.			
							visited[getIndexOfPosition(toSearch.poll(), width, numPixelsInXYPlane)] = true;
							visitedLakeMember[getIndexOfPosition(next, width, numPixelsInXYPlane)] = true;	
						}
						searched.clear();	// clear the list of things we've searched so far, since they can't be maxes anymore
						break;
					}
					
					// Case 2: neighbor's value is strictly equal to ours, which means we could still be at a maximum, but the max value is a blob, not just a single point. We must check the area.
					else if (neighborValue.compareTo(currentValue) == 0 && isInner(local.getPosition())) {
						toSearch.add(local.getPosition());  // add to LL using the iterator's 'add'; if we used the LL's 'add' we would break the itr
					}
				}
				neighbors.reset();  // needed to get the outer cursor to work correctly;		
			}
			if (isMax) {	// if we get here, we've searched the entire lake, so find the average point and call that a max by adding to results list
				IJ.log("Lake not killed, so lake is a max!!");
				while (!searched.isEmpty()) {
					int max[] = searched.poll();
					IJ.log("Adding " + MathLib.printCoordinates(max) + " to max list.");
					maxCoordinates.add(max);	
				}
			} else {
				searched.clear();
			}
		}
		curr.close();
		neighbors.close();
		
		// 3 - Print out list of maxima (FOR TESTING):
		String img_dim = MathLib.printCoordinates(img.getDimensions());
		IJ.log("Image dimensions: " + img_dim);
		Iterator<int[]> itr = maxCoordinates.iterator();
		while (itr.hasNext()) {
			String pos_str = MathLib.printCoordinates(itr.next());
			IJ.log(pos_str);
		}
	}
	
	/**
	 * 
	 * @param start
	 * @return
	 */
	public ArrayList< int[] > getConnectedComponent(int start[], int width, int numPixelsInXYPlane) {
		// 0 - Initialize local variables, cursors
		LocalNeighborhoodCursor<T> neighbors = null;
		LocalizableByDimCursor<T> local = null;
		ArrayList< int[] > connectedComponent = new ArrayList< int[] >();				// list of indexes of pixels belonging to this connected component
		ConcurrentLinkedQueue< int[] > toSearch = new ConcurrentLinkedQueue< int[] >();	// holds the pixels that need to be searched
		boolean visited[] = new boolean[img.getNumPixels()];							// stores whether or not this pixel has been previously visited
		T currentValue = img.createType();  							
		T neighborValue; 
		
		// Beginning from the start[] parameter, search for immediate neighbors with the same intensity value, and subsequently search any neighbors found. Repeat until no new neighbors of equal intensity found.
		toSearch.add(start);
		while(!toSearch.isEmpty()) {  // 1. search the list of pixels known to be in connected component
			int curr[] = toSearch.poll();
			if (!visited[getIndexOfPosition(curr, width, numPixelsInXYPlane)]) {  // if we've never seen this pixel before: 
				visited[getIndexOfPosition(curr, width, numPixelsInXYPlane)] = true;  // mark it as visited so we don't re-visit it later in this search
				connectedComponent.add(curr);  // add it to the list of connected components.
				local = img.createLocalizableByDimCursor(new OutOfBoundsStrategyMirrorFactory<T>());  // new cursor that will search this pixel
				neighbors = new LocalNeighborhoodCursor3D<T>(local);	// new cursor that will search the above pixel's immediate neighbors
				local.setPosition(curr); // search the current pixel's neighbors for neighbors of equal intensity
				currentValue.set(local.getType());  // store the value of this pixel in a variable
				neighbors.update();
				while(neighbors.hasNext()) {  // 2. search the neighbors of our pixel for those with equal values. If any, they are 'connected' and therefore belong to the same connected component
					neighbors.fwd();
					neighborValue = neighbors.getType();
					if (neighborValue.compareTo(currentValue) == 0 && isInner(local.getPosition())) {
						toSearch.add(local.getPosition());  // add to LL using the iterator's 'add'; if we used the LL's 'add' we would break the itr
					}
				}
				neighbors.reset();  // needed to get the outer cursor to work correctly;		
			}
		}
		
		return connectedComponent;
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
