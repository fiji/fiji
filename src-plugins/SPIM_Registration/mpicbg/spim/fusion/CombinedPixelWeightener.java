package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public abstract class CombinedPixelWeightener<I>
{
	final ViewStructure viewStructure;
	final ArrayList<ViewDataBeads> views;
	final SPIMConfiguration conf;
	
	protected CombinedPixelWeightener( final ViewStructure viewStructure )
	{
		this.viewStructure = viewStructure;
		this.views = viewStructure.getViews();
		this.conf = viewStructure.getSPIMConfiguration();
	}

	/**
	 * Updates the weights for all images, knowing where to grab all pixels from in each source image
	 * 
	 * @param locations - the locations of the source pixel in each source image 
	 */
	public abstract void updateWeights( final int[][] locations );
	
	/**
	 * Updates the weights for all images, knowing where to grab all pixels from in each source image
	 * and which of the images are hit
	 * 
	 * @param locations - the locations of the source pixel in each source image 
	 * @param use - if the particular view is hit or not
	 */
	public abstract void updateWeights( final int[][] locations, final boolean[] use );

	public abstract void updateWeights( final float[][] locations, final boolean[] use );

	/**
	 * Returns the weightening factor for one view
	 * 
	 * @param view - which source image
	 * @return a weightening factor between 0 and 1
	 */
	public abstract float getWeight( final int view );
	
	/**
	 * Closes the created images if applicable
	 */
	public abstract void close();
}