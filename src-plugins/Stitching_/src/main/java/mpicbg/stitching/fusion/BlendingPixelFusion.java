package mpicbg.stitching.fusion;

import java.util.ArrayList;

public class BlendingPixelFusion implements PixelFusion
{
	public static double fractionBlended = 0.2;
	
	final int numDimensions;
	final int numImages;
	final int[][] dimensions;
	final float percentScaling;
	final float[] border;
	
	final ArrayList< ? extends ImageInterpolation< ? > > images;

	double valueSum, weightSum;
	
	/**
	 * Instantiates the per-pixel blending
	 * 
	 * @param images - all input images (the position in the list has to be the same as Id provided by addValue!)
	 */
	public BlendingPixelFusion( final ArrayList< ? extends ImageInterpolation< ? > > images )
	{
		this( images, (float)fractionBlended );
	}	
	/**
	 * Instantiates the per-pixel blending
	 * 
	 * @param images - all input images (the position in the list has to be the same as Id provided by addValue!)
	 * @param percentScaling - which percentage of the image should be blended ( e.g. 0,3 means 15% on the left and 15% on the right)
	 */
	private BlendingPixelFusion( final ArrayList< ? extends ImageInterpolation< ? > > images, final float fractionBlended )
	{
		this.images = images;
		this.percentScaling = fractionBlended;
		
		this.numDimensions = images.get( 0 ).getImage().getNumDimensions();
		this.numImages = images.size();
		this.dimensions = new int[ numImages ][ numDimensions ];
		
		for ( int i = 0; i < numImages; ++i )
			for ( int d = 0; d < numDimensions; ++d )
				dimensions[ i ][ d ] = images.get( i ).getImage().getDimension( d ) - 1; 

		this.border = new float[ numDimensions ];

		// reset
		clear();
	}
	
	@Override
	public void clear() { valueSum = weightSum = 0;	}

	@Override
	public void addValue( final float value, final int imageId, final float[] localPosition ) 
	{
		// we are always inside the image, so we do not want 0.0
		final double weight = Math.max( 0.00001, computeWeight( localPosition, dimensions[ imageId ], border, percentScaling ) );
		
		weightSum += weight;
		valueSum += value * weight;
	}

	@Override
	public float getValue()
	{ 
		if ( weightSum == 0 )
			return 0;
		else
			return (float)( valueSum / weightSum );
	}

	@Override
	public PixelFusion copy() { return new BlendingPixelFusion( images ); }

	/**
	 * From SPIM Registration
	 * 
	 * 
	 * @param location
	 * @param dimensions
	 * @param border
	 * @param dimensionScaling
	 * @param percentScaling
	 * @return
	 */
	final public static double computeWeight( final float[] location, final int[] dimensions, final float[] border, final float percentScaling )
	{		
		// compute multiplicative distance to the respective borders [0...1]
		double minDistance = 1;
		
		for ( int dim = 0; dim < location.length; ++dim )
		{
			// the position in the image
			final double localImgPos = location[ dim ];
			
			// the distance to the border that is closer
			double value = Math.max( 1, Math.min( localImgPos - border[ dim ] + 1, (dimensions[ dim ] - 1) - localImgPos - border[ dim ] + 1 ) );
						
			final float imgAreaBlend = Math.round( percentScaling * 0.5f * dimensions[ dim ] );
			
			if ( value < imgAreaBlend )
				value = value / imgAreaBlend;
			else
				value = 1;
			
			minDistance *= value;
		}
		
		if ( minDistance == 1 )
			return 1;
		else if ( minDistance <= 0 )
			return 0.0000001;
		else
			return ( Math.cos( (1 - minDistance) * Math.PI ) + 1 ) / 2;				
	}

}
