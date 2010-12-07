package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.spim.registration.ViewDataBeads;

public class BlendingSimple extends CombinedPixelWeightener<BlendingSimple>
{
	final int numDimensions;
	final int numViews;
	final float[] weights, border;
	final float[][] scaling;
	
	final int[][] imageSizes;
	
	float percentScaling = 0.3f;

	protected BlendingSimple( final ArrayList<ViewDataBeads> views ) 
	{
		super( views );
		
		numViews = views.size();
		numDimensions = views.get( 0 ).getNumDimensions();
		
		weights = new float[ numViews ];
		scaling = new float[ numViews ][ numDimensions ];
		border = new float[ numDimensions ];
		                 
		// cache image sizes
		imageSizes = new int[ numViews ][];
		
		for ( int i = 0; i < numViews; ++i )
		{
			imageSizes[ i ] = views.get( i ).getImageSize();
			
			for ( int d = 0; d < numDimensions; ++d )
				scaling[ i ][ d ] = 1;
			
			scaling[ i ][ 2 ] = (float)views.get( i ).getZStretching();
		}
		
		//setBorder( 15 );
	}
	
	public void setBorder( final float numPixels )
	{
		for ( int d = 0; d < border.length; ++d )
			border[ d ] = numPixels;
	}

	public void setBorder( final float[] numPixels )
	{
		for ( int d = 0; d < border.length; ++d )
			border[ d ] = numPixels[ d ];
	}
	
	public void setBlendingRange( final float ratio ) { this.percentScaling = ratio; }

	@Override
	public void close() {}

	@Override
	public float getWeight( final int view )  { return weights[ view ]; }

	@Override
	public void updateWeights( final int[][] locations, final boolean[] useView ) 
	{
		final float[][] tmp = new float[ locations.length ][ locations[ 0 ].length ];
		
		for ( int i = 0; i < locations.length; ++i )
			for ( int d = 0; d < locations[ 0 ].length; ++d )
				tmp[ i ][ d ] = locations[ i ][ d ];
		
		updateWeights( tmp, useView );
	}

	@Override
	public void updateWeights( final float[][] locations, boolean[] useView ) 
	{
		// check which location are inside its respective view
		int num = 0;
		for ( final boolean use : useView )
			if ( use )
				++num;
		
		// if there is only one or no view at this point we can save some work
		if ( num <= 1 )
		{
			for ( int i = 0; i < useView.length; ++i )
				if ( useView[i] )
					weights[i] = 1;
				else
					weights[i] = 0;
		}
		else
		{
			for ( int i = 0; i < useView.length; ++i )
				weights[ i ] = (float)computeWeight( locations[ i ], imageSizes[ i ], border, scaling[ i ], percentScaling );
		}
	}

	final public static double computeWeight( final int[] location, final int[] dimensions, final float[] border, final float[] dimensionScaling, final float percentScaling )
	{
		final float[] tmp = new float[ location.length ];
		
		for ( int d = 0; d < location.length; ++d )
			tmp[ d ] = location[ d ];

		return computeWeight( tmp, dimensions, border, dimensionScaling, percentScaling );
	}
	
	final public static double computeWeight( final float[] location, final int[] dimensions, final float[] border, final float[] dimensionScaling, final float percentScaling )
	{		
		// compute multiplicative distance to the respective borders [0...1]
		double minDistance = 1;
		
		for ( int dim = 0; dim < location.length; ++dim )
		{
			// the position in the image
			final double localImgPos = location[ dim ];
			
			// the distance to the border that is closer
			double value;
			if ( dimensionScaling != null && dimensionScaling[ dim ] != 0 )
			{
				value = Math.max( 0, Math.min( localImgPos - border[ dim ]/dimensionScaling[ dim ], (dimensions[ dim ] - 1) - localImgPos - border[ dim ]/dimensionScaling[ dim ] ) );
				value *= dimensionScaling[ dim ];
			}
			else
			{
				value = Math.max( 0, Math.min( localImgPos - border[ dim ], (dimensions[ dim ] - 1) - localImgPos - border[ dim ] ) );				
			}
						
			final float imgAreaBlend = Math.round( percentScaling * 0.5f * dimensions[ dim ] );
			
			if ( value < imgAreaBlend )
				value = value / imgAreaBlend;
			else
				value = 1;
			
			minDistance *= value;
		}
		
		if ( minDistance == 1 )
			return 1;
		else if ( minDistance == 0)
			return 0;
		else
			return ( Math.cos( (1 - minDistance) * Math.PI ) + 1 ) / 2;				
	}
}
