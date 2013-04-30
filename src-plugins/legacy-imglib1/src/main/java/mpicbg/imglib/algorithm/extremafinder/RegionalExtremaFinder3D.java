/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.extremafinder;

import java.util.ArrayList;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor3D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 * <p>This class finds the regional extrema of a 3 dimensional image. A regional maxima is defined as follows:</p>
 * 
 * <p>"A regional maximum M of a grayscale image I is a connected components of pixels such that every pixel in 
 * the neighborhood of M has a strictly lower value."</p>
 * 
 * <p>The definition of a regional minimum is simply the opposite; the neighboring pixels must be strictly brighter.</p>
 * 
 * <p>This class does not stipulate how much brighter a regional extreme must be from the neighboring pixels, but only
 * requires that it is brighter or dimmer. So a connected component of pixels that is 1 higher in intensity from its neighbors
 * is treated the same as a connected component that is 100 higher in intensity than it's neighbors.</p>
 * 
 * <p>The {@link #getRegionalExtrema()} method returns an ArrayList of ArrayLists, where each inner ArrayLists represents
 * a regional extreme and contains the coordinates of the pixels comprising that regional extreme.</p>
 * 
 * <p>Notably, this implementation does not allow for the identification of h-domes, as mentioned by Luc Vincent
 * in his paper, "Morphological Grayscale Reconstruction in Image Analysis: Applications and Efficient Algorithms."</p>
 * 
 * @param <T>
 * @author Nick Perry
 */
public class RegionalExtremaFinder3D<T extends RealType<T>> extends AbstractRegionalExtremaFinder<T>
{
	
	/*
	 * FIELDS 
	 */
	
	/** Causes the algorithm to find regional maxima by default (1 = maxima, -1 = minima). */
	protected int sign;				

	private long processingTime;											// stores the run time of process() once the method is invoked.
	private String errorMessage = "";										// stores any error messages.
	/* Bitmasks used in the findMaxima algorithm to perform quick checks */
	final static byte CC_MEMBER = (byte)1;	// pixel has been added to the lake, but not had neighbors inspected (explored, but not searched)
	final static byte PROCESSED = (byte)2;	// pixel has been added to the lake, and had neighbors inspected (explored, and searched)
	
	/*
	 * CONSTRUCTOR
	 */
	
	/**
	 * Constructor for the RegionalMaximaFinder3D class.
	 * <p>
	 * By default, the {@link OutOfBoundsStrategyFactory} is a constant value strategy, sets to 0,
	 * so as to avoid nasty mirroring of periodic maxima effects. Edge maxima will be discarded by
	 * default, and there will be no maxima interpolation.
	 * 
	 * @param image the image to find the maxima of
	 * @param findMaxima  if true, will return a <b>maxima</b> finder, and a <b>minima</b> finder otherwise
	 */
	public RegionalExtremaFinder3D( final Image<T> image, final boolean findMaxima)
	{
		this.image = image;
		this.processingTime = -1;
		if (findMaxima) {
			sign = 1;
		} else {
			sign = -1;
		}
	}
	
	/**
	 * Constructor for the RegionalMaximaFinder3D class, returning a <b>maxima</b> finder.
	 * <p>
	 * By default, the {@link OutOfBoundsStrategyFactory} is a constant value strategy, sets to 0,
	 * so as to avoid nasty mirroring of periodic maxima effects. Edge maxima will be discarded by
	 * default, and there will be no maxima interpolation.
	 * 
	 * @param image the image to find the maxima of
	 */
	public RegionalExtremaFinder3D( final Image<T> image)
	{
		this(image, true);
	}
	
	/**
	 * Returns the time necessary to execute the process() method, or in other words how long it takes to find the maxima of the input image.
	 */
	@Override
	public long getProcessingTime() { return processingTime; }
	
	/**
	 * Returns any error messages.
	 */
	@Override
	public String getErrorMessage() { return errorMessage; }
	
	/**
	 * Checks various fields to ensure they have been instantiated before executing the main functionality.
	 * 
	 * Make sure to call this method before calling process() to ensure everything is in order.
	 */
	@Override
	public boolean checkInput() 
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image == null )
		{
			errorMessage = "FindMaxima3D: [Image<T> img] is null.";
			return false;
		}
		else if ( outOfBoundsFactory == null )
		{
			errorMessage = "FindMaxima3D: [OutOfBoundsStrategyFactory<T>] is null.";
			return false;
		}
		else
			return true;
	}
	
	/**
	 * <p>Finds the regional extrema of the input image.</p>
	 * 
	 * <p><b>Algorithm:</b> Step 2.1 iterates through all of the pixels in the image with an outer cursor.
	 * For each pixel, if we have not processed the pixel previously, we then declare isExtrema to be true
	 * since the pixel could potentially be a regional extreme ('innocent until proven guilty' approach). We then
	 * add this pixel's coordinates to an ArrayList. The reason for the ArrayList is the following (example is for regional maxima): imagine
	 * a pixel which has a value of 255 (it's very bright, if 255 is white in an 8-bit image), and that
	 * 25 of his direct 3D neighbors are strictly less bright than him (<255), but that one of his neighbors
	 * is also 255. Therefore, our pixel is not a regional maximum, but this pixel and his bright neighbor
	 * COULD be. Remember, a regional max is a connected component of pixels where all neighboring pixels have strictly less intensity.
	 * The ArrayList is therefore used to hold the coordinates of the pixels comprising regional extrema.</p>
	 * 
	 * <p>So, step 2.2 is the iteration through the pixels in the ArrayList. If a neighboring pixel is found 
	 * to be strictly brighter (or lower), we mark isExtreme false, but continue iterating through the connected component because we know that the connected component cannot be
	 * a regional extreme, but we can save time by marking the pixels in this connected component as visited while we are here. If a neighboring 
	 * pixel is found to have the same intensity value, that pixel is added to the ArrayList and subsequently searched. 
	 * We stop this iteration once nothing more is added to the ArrayList (we've searched the entire connected component of our initial 
	 * pixel from step 2.1).</p>
	 * 
	 * <p>Step 2.3 is the actual iteration through the neighbors of our current pixel from step 2.2.</p>
	 * 
	 * <p>Once the connected component is completely searched, if isExtreme == true, then we never found
	 * a brighter (or dimmer) pixel, so the whole connected component is a regional extreme, and an ArrayList of the coordinates of the pixels
	 * making up the connected component is stored in another ArrayList.  If however isExtreme == false, we encountered a brighter (or dimmer) pixel on 
	 * the border of the connected component, so the connected components pixels are not regional extrema and are just ignored, but marked as
	 * processed so we don't visit again.</p>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean process()
	{
		// 1 - Initialize local variables, cursors
		final long startTime = System.currentTimeMillis();
		
		final LocalizableByDimCursor<T> curr = image.createLocalizableByDimCursor(outOfBoundsFactory);	// Used by step 2.1, the outer cursor
		final LocalizableByDimCursor<T> local = image.createLocalizableByDimCursor(outOfBoundsFactory);		// Used by step 2.2, the lake/connected component cursor
		final LocalNeighborhoodCursor3D<T> neighbors = new LocalNeighborhoodCursor3D<T>(local);				// Used by step 2.3, the neighbor searching cursor
		final ArrayList< int[] > toSearch = new ArrayList< int[] >();	// pixels known to be in the lake/connected component that we have yet to search the neighbors of
		final ArrayList< int[] > searched = new ArrayList< int[] >();	// pixels in the lake/connected component that have had their neighbors searched.
		T currentValue = image.createType();							// holds the pixel intensity of the outer pixel/lake
		T neighborValue;												// holds the pixel value of a neighbor, which is compared to currentValue
		final int width = image.getDimensions()[0];							// width of the image, used to map 3D coordinates to a 1D coordinate system for storing information about each pixel (visisted or not, etc)
		final int numPixelsInXYPlane = image.getDimensions()[1] * width;	// number of pixels in 1 stack, used to map 3D coordinates to a 1D coordinate system for storing information about each pixel (visisted or not, etc)
		final byte visitedAndProcessed[] = new byte[image.getNumPixels()];	// holds information on whether the pixel has been added to the lake/connected component, or whether pixel has had neighbors directly searched already.
		boolean isExtreme;													// stores whether our lake/connected component is a local maxima or not.
		int nextCoords[] = new int [3];									// declare coordinate arrays outside while loops to speed up. holds the coordinates of pixel in step 2.2
		int currCoords[] = new int[3];									// holds coordinates of pixel in step 2.1
		int neighborCoords[] = new int[3];								// holds coordinates of pixel in step 2.3

		// 2 - Search all pixels for LOCAL maxima.
		
		// 2.1 - Iterate over all pixels in the image.
		while(curr.hasNext()) {
			curr.fwd();
			curr.getPosition(currCoords);
			if ((visitedAndProcessed[getIndexOfPosition(currCoords, width, numPixelsInXYPlane)] & PROCESSED) != 0) {  // prevents revisiting pixels, increases speed
				continue;
			}
			isExtreme = true;
			currentValue.set(curr.getType());  // Store the intensity of this connected component.
			toSearch.add(currCoords);
			
			// 2.2 - Iterate through queue which contains the pixels of the connected component		
			while (!toSearch.isEmpty()) {
				nextCoords = toSearch.remove(0);
				
				if (null != threshold && sign * curr.getType().compareTo(threshold) < 0) // skip pixel with value lower than the threshold
					continue;
				
				if ((visitedAndProcessed[getIndexOfPosition(nextCoords, width, numPixelsInXYPlane)] & PROCESSED) != 0) {  // if visited, skip
					continue;
				} else {  // if not visited, mark as processed (has had neighbors searched) and add him to the searched ArrayList.
					visitedAndProcessed[getIndexOfPosition(nextCoords, width, numPixelsInXYPlane)] |= PROCESSED; 
					if ((allowEdgeMax) || (!allowEdgeMax && !isEdgeMax(nextCoords))) {
						searched.add(nextCoords.clone());
					}
				}
				local.setPosition(nextCoords);		// Set the location of the cursor to the pixel currently being searched in the connected component. This cursor is essentially needed only so we can use the neighborhood cursor.
				neighbors.update();					// Needed to get the neighborhood cursor to work properly.
				
				// 2.3 - Iterate through immediate neighbors, excluding out of bounds neighbors.
				while(neighbors.hasNext()) {
					neighbors.fwd();
					local.getPosition(neighborCoords);
					if (isWithinImageBounds(neighborCoords)) {
						if ((visitedAndProcessed[getIndexOfPosition(neighborCoords, width, numPixelsInXYPlane)] & CC_MEMBER) != 0) {  // We've already visited this neighbor before, and handled it accordingly, so skip it since it hasn't changed.
							continue;
						}	
						neighborValue = neighbors.getType();
						int compare = neighborValue.compareTo(currentValue);
						
						// Case 1: neighbor's value is strictly larger, so ours cannot be a regional maximum.
						if ((sign * compare) > 0) {
							isExtreme = false;
						}
						
						// Case 2: neighbor's value is strictly equal, which means this pixel belongs to the connected component.
						else if (compare == 0 && (visitedAndProcessed[getIndexOfPosition(nextCoords, width, numPixelsInXYPlane)] & PROCESSED) != 0) {  // Note: don't re-add a PROCESSED pixel to CC list; it's already been there.
							toSearch.add(neighborCoords.clone());
							visitedAndProcessed[getIndexOfPosition(neighborCoords, width, numPixelsInXYPlane)] |= CC_MEMBER;  // mark that this pixel has been added to the lake search list with VISITED (different than PROCESSED, which is used to say that a pixel has had his neighbor's searched.)
						}
						
						// Case 3: neighbor's value is strictly lower, so it can't be a regional max.  Don't bother considering it as a regional max.
						else {
							visitedAndProcessed[getIndexOfPosition(neighborCoords, width, numPixelsInXYPlane)] |= PROCESSED;
						}
					}
				}
				neighbors.reset();  // needed to get the outer cursor to work correctly;		
			}
			if (isExtreme && searched.size() > 0) {  // If isMax is still true, then our connected component is a regional maximum, so store the coordinates of the pixels making up the regional maximum.
				maxima.add((ArrayList<int[]>) searched.clone());
			}
			searched.clear();
		}
		curr.close();
		neighbors.close();
		
		processingTime= System.currentTimeMillis() - startTime;
		
		return true;
	}

	/**
	 * Returns the ArrayList containing the coordinates of the regional maxima found.
	 * 
	 * @return the Arraylist of ArrayLists, which is interpreted as the ArrayList of regional maxima (stored in an ArrayList)
	 */
	@Override
	public ArrayList< ArrayList< int[] > > getRegionalExtrema() { return maxima;	}
	
	
	/**
	 * Determines whether the input coordinates are on the edge of the image or not.
	 * 
	 * @param coords
	 * @return
	 */
	final protected boolean isEdgeMax(final int[] coords) {
		return coords[0] == 0 || coords[0] == image.getDimension(0) - 1 || coords[1] == 0 || coords[1] == image.getDimension(1) - 1 || coords[2] == 0 || coords[2] == image.getDimension(2) - 1;
	}
		
	/**
	 * Given a position array, returns whether or not the position is within the bounds of the image, or out of bounds.
	 * 
	 * @param pos
	 * @return
	 */
	final protected boolean isWithinImageBounds(final int[] pos) {
		return pos[0] > -1 && pos[0] < image.getDimension(0) && pos[1] > -1 && pos[1] < image.getDimension(1) && pos[2] > -1 && pos[2] < image.getDimension(2);
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
	final static protected int getIndexOfPosition(final int[] pos, final int width, final int numPixelsInXYPlane) {
		return pos[0] + width * pos[1] + numPixelsInXYPlane * pos[2];
	}
}
