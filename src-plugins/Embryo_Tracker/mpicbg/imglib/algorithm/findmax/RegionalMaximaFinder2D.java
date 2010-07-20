/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Nick Perry
 */
package mpicbg.imglib.algorithm.findmax;

import java.util.ArrayList;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.special.LocalNeighborhoodCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

public class RegionalMaximaFinder2D<T extends RealType<T>> extends AbstractRegionalMaximaFinder<T>
{

	/*
	 * FIELDS
	 */
	
	private long processingTime;											// stores the run time of process() once the method is invoked.
	private String errorMessage = "";										// stores any error messages.
	/* Bitmasks used in the findMaxima algorithm to perform quick checks */
	final static byte VISITED = (byte)1;	// pixel has been added to the lake, but not had neighbors inspected (explored, but not searched)
	final static byte PROCESSED = (byte)2;	// pixel has been added to the lake, and had neighbors inspected (explored, and searched)
	
	/**
	 * Constructor for the RegionalMaximaFinder2D class.
	 * <p>
	 * By default, the {@link OutOfBoundsStrategyFactory} is a constant value strategy, sets to 0,
	 * so as to avoid nasty mirroring of periodic maxima effects. Edge maxima will be discarded by
	 * default, and there will be no maxima interpolation.
	 * 
	 * @param image: the image to find the maxima of
	 */
	public RegionalMaximaFinder2D( final Image<T> image)
	{
		this.image = image;
		this.processingTime = -1;
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
	 * Finds the local maxima of the input image. 
	 * 
	 * Algorithm: Step 2.1 iterates through all of the pixels in the image with an outer cursor.
	 * For each pixel, if we have not processed the pixel previously, we then declare isMax to be true
	 * since the pixel could potentially be a max (innocent until proven guilty approach). We then
	 * add this pixel's coordinates to a LinkedList. The reason for the LinkedList is the following: image
	 * a pixel which has a value of 255 (it's very bright, if 255 is white in an 8-bit image). Image that
	 * 25 of his direct 3D neighbors are strictly less bright than him (<255), but that one of his neighbors
	 * is also 255. Therefore, our pixel is not a strict local maxima, but this pixel and his bright neighbor
	 * COULD be (all of the neighbor's neighboring pixels, besides our original one, are strictly less
	 * bright). So we have a local maxima made up of 2 pixels with intensity 255. This is still a local
	 * maxima, which is called in further comments a 'lake.' This is obviously a simple case, as lakes could be
	 * many more than 2 pixels. Some lakes could be 100 pixels, but still be a 'local' maxima in that they
	 * all have the same intensity value, but are brighter than anything around them. The LinkedList is used
	 * to handle the occurance of a lake. In the event a direct neighbor of a pixel is found to have the same
	 * intensity value, the neighboring pixel is added to the 'lake' represented by the LinkedList, and subsequently
	 * his neighbors will be searched.
	 * 
	 * So, step 2.2 is the iteration through the pixels in the lake. If a neighboring pixel is found to be brighter,
	 * we continue iterating through the lake (which can also be though of as a connected component, where connection
	 * is defined by having the same intensity as your connected neighbors) because we know that the lake cannot be
	 * a local maximum. If the neighboring pixel is found to have the same intensity value, that pixel is added to the lake
	 * and subsequently searched. We stop this iteration once nothing more is added to the lake (we've searched the entire
	 * connected component of our initial pixel from step 2.1)
	 * 
	 * Step 2.3 is the actual iteration through the neighbors of our current pixel from step 2.2. 
	 * 
	 * Once the lake/connected component is completely searched, if isMax == true, then we never found
	 * a brighter pixel, so the whole lake is a local max and the point used to represent the lake is calculated by taking
	 * the average of the coordinates of the pixels making up the lake (best approximation of the center of the lake). If
	 * however isMax == false, we encountered a brighter pixel on the border of the lake, so the lake's pixels
	 * are not local maxima and are just ignored, but marked as processed so we don't visit again.
	 */
	@Override
	public boolean process()
	{
		// 1 - Initialize local variables, cursors
		final long startTime = System.currentTimeMillis();
		
		final LocalizableByDimCursor<T> curr = image.createLocalizableByDimCursor(outOfBoundsFactory);	// Used by step 2.1, the outer cursor
		final LocalizableByDimCursor<T> local = image.createLocalizableByDimCursor(outOfBoundsFactory);		// Used by step 2.2, the lake/connected component cursor
		final LocalNeighborhoodCursor<T> neighbors = new LocalNeighborhoodCursor<T>(local);				// Used by step 2.3, the neighbor searching cursor
		T currentValue = image.createType();							// holds the pixel intensity of the outer pixel/lake
		T neighborValue;												// holds the pixel value of a neighbor, which is compared to currentValue
		final int width = image.getDimensions()[0];							// width of the image, used to map 3D coordinates to a 1D coordinate system for storing information about each pixel (visisted or not, etc)
		final byte visitedAndProcessed[] = new byte[image.getNumPixels()];	// holds information on whether the pixel has been added to the lake/connected component, or whether pixel has had neighbors directly searched already.
		boolean isMax;													// stores whether our lake/connected component is a local maxima or not.
		int nextCoords[] = new int [2];									// declare coordinate arrays outside while loops to speed up. holds the coordinates of pixel in step 2.2
		int currCoords[] = new int[2];									// holds coordinates of pixel in step 2.1
		int neighborCoords[] = new int[2];								// holds coordinates of pixel in step 2.3
		final byte VISITED = (byte)1;	// pixel has been added to the lake, but not had neighbors inspected (explored, but not searched)
		final byte PROCESSED = (byte)2;	// pixel has been added to the lake, and had neighbors inspected (explored, and searched)
		
		// 2 - Search all pixels for LOCAL maxima.
		
		// 2.1 - Iterate over all pixels in the image.
		while(curr.hasNext()) { 
			final ArrayList< int[] > toSearch = new ArrayList< int[] >();		// pixels known to be in the lake/connected component that we have yet to search the neighbors of
			final ArrayList< int[] > searched = new ArrayList< int[] >();		// pixels in the lake/connected component that have had their neighbors searched.
			curr.fwd();
			curr.getPosition(currCoords);
			if ((visitedAndProcessed[getIndexOfPosition(currCoords, width)] & PROCESSED) != 0) {  // prevents revisiting pixels, increases speed
				continue;
			}
			isMax = true;
			currentValue.set(curr.getType());  // Store the intensity of this lake/connected component.
			toSearch.add(currCoords);
			
			// 2.2 - Iterate through queue which contains the pixels of the "lake"		
			//while ((nextCoords = toSearch.poll()) != null) {
			while (!toSearch.isEmpty()) {
				nextCoords = toSearch.remove(0);
				if ((visitedAndProcessed[getIndexOfPosition(nextCoords, width)] & PROCESSED) != 0) {  // if visited, skip
					continue;
				} else {  // if not visited, mark as processed (has had neighbors searched) and add him to the searched LinkedList.
					visitedAndProcessed[getIndexOfPosition(nextCoords, width)] |= PROCESSED; 
					if ((allowEdgeMax) || (!allowEdgeMax && !isEdgeMax(nextCoords))) {
						searched.add(nextCoords.clone());
					}
				}
				local.setPosition(nextCoords);		// Set the location of the cursor to the pixel currently being searched in the lake. This cursor is essentially needed only so we can use the neighborhood cursor.
				neighbors.update();					// Needed to get the neighborhood cursor to work properly.
				
				// 2.3 - Iterate through immediate neighbors
				while(neighbors.hasNext()) {
					neighbors.fwd();
					neighborCoords = local.getPosition();
					neighborValue = neighbors.getType();
					
					// Case 1: neighbor's value is strictly larger than ours, so ours cannot be a local maximum.
					if (neighborValue.compareTo(currentValue) > 0) {
						isMax = false;
					}
					
					// Case 2: neighbor's value is strictly equal to ours, which means we could still be at a maximum, but the max value is a blob, not just a single point. We must check the area.
					else if (neighborValue.compareTo(currentValue) == 0 && isWithinImageBounds(neighborCoords)  && (visitedAndProcessed[getIndexOfPosition(neighborCoords, width)] & VISITED) == 0) {
						toSearch.add(neighborCoords);
						visitedAndProcessed[getIndexOfPosition(neighborCoords, width)] |= VISITED;  // mark that this pixel has been added to the lake search list with VISITED (different than PROCESSED, which is used to say that a pixel has had his neighbor's searched.)
					}
				}
				neighbors.reset();  // needed to get the outer cursor to work correctly;		
			}
			if (isMax) {  // If isMax is still true, then our lake/connected component is a local maximum, so find the averaged point in the center.
				if (searched.size() > 0) {
					//averagedMaxPos = findAveragePosition(searched);
					maxima.add(searched);
				}
			} else {  // otherwise, get rid of the lake we searched, we don't need coordinates since not a local maximum.
				searched.clear();
			}
		}
		curr.close();
		neighbors.close();
		
		processingTime= System.currentTimeMillis() - startTime;
		
		return true;
	}
	
	/**
	 * Returns the ArrayList containing the coordinates of the local maxima found.
	 * 
	 * @return
	 */
	public ArrayList< ArrayList< int[] > > getRegionalMaxima() { return maxima;	}
	
	@Override
	public ArrayList< double[] > getRegionalMaximaCenters(ArrayList< ArrayList< int[] > > regionalMaxima) {
		ArrayList< double[] > centeredRegionalMaxima = new ArrayList< double[] >();
		ArrayList< int[] > curr = null;
		while (!regionalMaxima.isEmpty()) {
			curr = regionalMaxima.remove(0);
			double averagedCoord[] = findAveragePosition(curr);
			centeredRegionalMaxima.add(averagedCoord);
		}
		return centeredRegionalMaxima;
	}
	
	/**
	 * Determines whether the input coordinates are on the edge of the image or not.
	 * 
	 * @param coords
	 * @return
	 */
	final protected boolean isEdgeMax(int coords[]) {
		return coords[0] == 0 || coords[0] == image.getDimension(0) - 1 || coords[1] == 0 || coords[1] == image.getDimension(1) - 1;
	}
	
	/**
	 * Given an ArrayList of int[] (coordinates), computes the averaged coordinates and returns them.
	 * 
	 * @param searched
	 * @return
	 */
	final static protected double[] findAveragePosition(ArrayList < int[] > searched) {
		int count = 0;
		double avgX = 0, avgY = 0;
		while(!searched.isEmpty()) {
			int curr[] = searched.remove(0);
			avgX += curr[0];
			avgY += curr[1];
			count++;
		}
		return new double[] {avgX/count, avgY/count};
	}
	
	/**
	 * Given a position array, returns whether or not the position is within the bounds of the image, or out of bounds.
	 * 
	 * @param pos
	 * @return
	 */
	final protected boolean isWithinImageBounds(int pos[]) {
		return pos[0] > -1 && pos[0] < image.getDimension(0) && pos[1] > -1 && pos[1] < image.getDimension(1);
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
	final static protected int getIndexOfPosition(int pos[], int width) {
		return pos[0] + width * pos[1];
	}

	@Override
	public void doInterpolate(boolean flag) {
		this.doInterpolate = flag;
	}
	
	@Override
	public void allowEdgeExtrema(boolean flag) {
		allowEdgeMax = flag;
	}

}

