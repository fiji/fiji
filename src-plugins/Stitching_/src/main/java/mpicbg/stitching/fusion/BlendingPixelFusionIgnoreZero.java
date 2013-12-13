package mpicbg.stitching.fusion;

import java.util.ArrayList;

public class BlendingPixelFusionIgnoreZero extends BlendingPixelFusion
{	
	/**
	 * Instantiates the per-pixel blending
	 * 
	 * @param images - all input images (the position in the list has to be the same as Id provided by addValue!)
	 */
	public BlendingPixelFusionIgnoreZero( final ArrayList< ? extends ImageInterpolation< ? > > images )
	{
		super( images );
	}	

	@Override
	public void addValue( final float value, final int imageId, final float[] localPosition ) 
	{
		if ( value != 0.0 )
		{
			// we are always inside the image, so we do not want 0.0
			final double weight = Math.max( 0.00001, computeWeight( localPosition, dimensions[ imageId ], border, percentScaling ) );
			
			weightSum += weight;
			valueSum += value * weight;
		}
	}

	@Override
	public PixelFusion copy() { return new BlendingPixelFusionIgnoreZero( images ); }
}
