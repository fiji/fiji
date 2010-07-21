package mpicbg.imglib.algorithm.extremafinder;

import ij.IJ;

import java.util.ArrayList;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.numeric.RealType;

/**
 *  * <p>This class finds the regional extrema of a 3 dimensional image. A regional maxima is defined as follows:</p>
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
 * @author Nick Perry
 *
 * @param <T>
 */
public class RegionalExtremaFinder3DOverTime<T extends RealType<T>> extends AbstractRegionalExtremaFinder<T>
{
	
	/*
	 * FIELDS 
	 */
	
	private long processingTime;											// stores the run time of process() once the method is invoked.
	private String errorMessage = "";										// stores any error messages.
	/* Bitmasks used in the findMaxima algorithm to perform quick checks */
	final static byte CC_MEMBER = (byte)1;	// pixel has been added to the lake, but not had neighbors inspected (explored, but not searched)
	final static byte PROCESSED = (byte)2;	// pixel has been added to the lake, and had neighbors inspected (explored, and searched)
	
	/*
	 * CONSTRUCTOR
	 */
	
	/**
	 * Constructor for the RegionalMaximaFinder2D class.
	 * <p>
	 * By default, the {@link OutOfBoundsStrategyFactory} is a constant value strategy, sets to 0,
	 * so as to avoid nasty mirroring of periodic maxima effects. Edge maxima will be discarded by
	 * default, and there will be no maxima interpolation.
	 * 
	 * @param image the image to find the maxima of
	 */
	public RegionalExtremaFinder3DOverTime( final Image<T> image)
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
	
	@Override
	public boolean process()
	{
		// 1 - Initialize local variables
		final long startTime = System.currentTimeMillis();
		
		// 2 - Iterate through t dimension
		
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
	 * Takes an ArrayList which represents a regional maximum, and computes the averaged coordinate.
	 * This average coordinate is returned, and represents the "center" of the regional maximum.
	 * Note that this is not guaranteed to be in the regional maximum itself (imagine a regional maximum
	 * that is a ring shape; the direct center is not part of the regional maximum itself, but is what
	 * would be computed by this function).
	 * 
	 * @param
	 * @return The coordinates of the "center pixel" of the regional maximum. 
	 */
	@Override
	public ArrayList< double[] > getRegionalExtremaCenters(ArrayList< ArrayList< int[] > > regionalMaxima) {
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
	final protected boolean isEdgeMax(final int[] coords) {
		return coords[0] == 0 || coords[0] == image.getDimension(0) - 1 || coords[1] == 0 || coords[1] == image.getDimension(1) - 1 || coords[2] == 0 || coords[2] == image.getDimension(2) - 1;
	}
	
	/**
	 * Given an ArrayList of int[] (coordinates), computes the averaged coordinates and returns them.
	 * 
	 * @param searched
	 * @return
	 */
	final static protected double[] findAveragePosition(final ArrayList < int[] > coords) {
		int count = 0;
		double avgX = 0, avgY = 0, avgZ = 0;
		while(!coords.isEmpty()) {
			int curr[] = coords.remove(0);
			avgX += curr[0];
			avgY += curr[1];
			avgZ += curr[2];
			count++;
		}
		return new double[] {avgX/count, avgY/count, avgZ/count};
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
