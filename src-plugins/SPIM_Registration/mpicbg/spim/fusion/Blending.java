package mpicbg.spim.fusion;

import mpicbg.spim.registration.ViewStructure;

public class Blending extends CombinedPixelWeightener<Blending>
{
	final boolean useView[];
	final int numViews;
	final float[] weights;
	final float[] minDistance;
	
	final int[][] imageSizes;
	
	protected Blending( final ViewStructure viewStructure )
	{
		super( viewStructure );
		
		numViews = viewStructure.getNumViews();
		useView = new boolean[numViews];
		weights = new float[numViews];
		minDistance = new float[numViews];
		
		// cache image sizes
		imageSizes = new int[numViews][];		
		for ( int i = 0; i < numViews; ++i )
			imageSizes[ i ] = views.get( i ).getImageSize();
	}

	@Override
	public void updateWeights( final int[][] loc )
	{
		// check which location are inside its respective view
		int num = 0;
		for (int view = 0; view < numViews; view++)
		{			
			if (loc[ view ][ 0 ] >= 0 && loc[ view ][ 1 ] >= 0 && loc[ view ][ 2 ] >= 0 && 
				loc[ view ][ 0 ] < imageSizes[ view ][ 0 ] && 
				loc[ view ][ 1 ] < imageSizes[ view ][ 1 ] && 
				loc[ view ][ 2 ] < imageSizes[ view ][ 2 ])
			{
				useView[view] = true;
				++num;
			}
		}	
		
		// compute the linear weights
		computeLinearWeights(num, loc, useView);
	}

	@Override
	public void updateWeights(final float[][] loc, final boolean[] useView)
	{
		// check which location are inside its respective view
		int num = 0;
		for (int view = 0; view < numViews; view++)
			if (useView[view])
				num++;
		
		// compute the linear weights
		computeLinearWeights(num, loc, useView);		
	}

	@Override
	public void updateWeights(final int[][] loc, final boolean[] useView)
	{
		// check which location are inside its respective view
		int num = 0;
		for (int view = 0; view < numViews; view++)
			if (useView[view])
				num++;
		
		// compute the linear weights
		computeLinearWeights(num, loc, useView);
	}
	
	@Override
	public float getWeight(final int view) { return weights[view]; }
	
	final private void computeLinearWeights(final int num, final int[][] loc, final boolean[] useView)
	{
		if (num <= 1)
		{
			for (int i = 0; i < useView.length; i++)
				if (useView[i])
					weights[i] = 1;
				else
					weights[i] = 0;
			return;
		}
		
		// compute the minimal distance to the border for each image
		float sumInverseWeights = 0;
		for (int i = 0; i < useView.length; i++)
		{
			if (useView[i])
			{
				minDistance[i] = 1;
				for (int dim = 0; dim < 3; dim++)
				{
					final int localImgPos = loc[i][dim];
					float value = Math.min(localImgPos, imageSizes[ i ][ dim ] - localImgPos - 1) + 1;
					
					final float imgHalf = imageSizes[ i ][ dim ]/2.0f;
					final float imgHalf10 = Math.round( 0.35f * imgHalf );
					
					if ( value < imgHalf10 )
						value = (value / imgHalf10);
					else
						value = 1;

					minDistance[i] *= value;
				}

				// the distance to the image, so always +1
				// minDistance[i]++;
				
				if ( minDistance[i] < 0 )
					minDistance[i] = 0;
				else if ( minDistance[i] > 1)
					minDistance[i] = 1;
				
				weights[i] = (float)Math.pow(minDistance[i], conf.alpha);				

				sumInverseWeights += weights[i];				
			}
		}
				
		if (sumInverseWeights == 0)
		{
			for (int i = 0; i < useView.length; i++)
				weights[i] = 0;			
		}
		else
		{
			// norm them so that the integral is 1
			for (int i = 0; i < useView.length; i++)
				if (useView[i])
					weights[i] /= sumInverseWeights;
				else
					weights[i] = 0;
		}
	}

	final private void computeLinearWeights(final int num, final float[][] loc, final boolean[] useView)
	{
		if (num <= 1)
		{
			for (int i = 0; i < useView.length; i++)
				if (useView[i])
					weights[i] = 1;
				else
					weights[i] = 0;
			return;
		}
		
		// compute the minimal distance to the border for each image
		float sumInverseWeights = 0;
		for (int i = 0; i < useView.length; i++)
		{
			if (useView[i])
			{
				minDistance[i] = 1;
				for (int dim = 0; dim < 3; dim++)
				{
					final float localImgPos = loc[i][dim];
					float value = Math.min(localImgPos, imageSizes[ i ][ dim ] - localImgPos - 1) + 1;
					
					final float imgHalf = imageSizes[ i ][ dim ]/2.0f;
					final float imgHalf10 = Math.round( 0.35f * imgHalf );
					
					if ( value < imgHalf10 )
						value = (value / imgHalf10);
					else
						value = 1;

					minDistance[i] *= value;
				}

				// the distance to the image, so always +1
				// minDistance[i]++;
				
				if ( minDistance[i] < 0 )
					minDistance[i] = 0;
				else if ( minDistance[i] > 1)
					minDistance[i] = 1;
				
				weights[i] = (float)Math.pow(minDistance[i], conf.alpha);				

				sumInverseWeights += weights[i];				
			}
		}
				
		if (sumInverseWeights == 0)
		{
			for (int i = 0; i < useView.length; i++)
				weights[i] = 0;			
		}
		else
		{
			// norm them so that the integral is 1
			for (int i = 0; i < useView.length; i++)
				if (useView[i])
					weights[i] /= sumInverseWeights;
				else
					weights[i] = 0;
		}
	}
	
	@Override
	public void close() 
	{
		//IOFunctions.println(new Date(System.currentTimeMillis()) + ": Finished Blending...");
	}
}
