package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;

public abstract class CombinedPixelWeightener<I>
{
	final ArrayList<ViewDataBeads> views;
	final SPIMConfiguration conf;
	
	protected CombinedPixelWeightener( final ArrayList<ViewDataBeads> views )
	{
		this.views = views;
		this.conf = views.get( 0 ).getViewStructure().getSPIMConfiguration();
	}
	
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