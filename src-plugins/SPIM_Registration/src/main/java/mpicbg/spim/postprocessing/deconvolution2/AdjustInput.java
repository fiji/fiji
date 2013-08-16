package mpicbg.spim.postprocessing.deconvolution2;

import ij.IJ;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.interpolation.Interpolator;
import mpicbg.imglib.interpolation.linear.LinearInterpolatorFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.util.RealSum;

public class AdjustInput 
{	
	public static Random rnd = new Random( 14235235 );

	/**
	 * Norms an image so that the sum over all pixels is 1.
	 * 
	 * @param img - the {@link Image} to normalize
	 */
	final public static void normImage( final Image<FloatType> img )
	{
		final double sum = sumImage( img );	

		for ( final FloatType t : img )
			t.set( (float) ((double)t.get() / sum) );
	}
	
	/**
	 * @param img - the input {@link Image}
	 * @return - the sum of all pixels using {@link RealSum}
	 */
	final public static double sumImage( final Image<FloatType> img )
	{
		final RealSum sum = new RealSum();		

		for ( final FloatType t : img )
			sum.add( t.get() );

		return sum.getSum();
	}	

	public static double normAllImages( final ArrayList<LRFFT> data )
	{
		final Vector< Chunk > threadChunks = SimpleMultiThreading.divideIntoChunks( data.get( 0 ).getImage().getNumPixels(), Runtime.getRuntime().availableProcessors() );
		final int numThreads = threadChunks.size();
		
		IJ.log( new Date( System.currentTimeMillis() ) + ": numThreads = " + numThreads );
		
		final int[] minNumOverlap = new int[ numThreads ];
		final long[] avgNumOverlap = new long[ numThreads ];
		final int[] countAvgNumOverlap = new int[ numThreads ];
		
		final RealSum[] sum = new RealSum[ numThreads ];
		final long[] count = new long[ numThreads ];
		
		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
        for ( int ithread = 0; ithread < threads.length; ++ithread )
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();
        
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( myNumber );

                	final long start = myChunk.getStartPosition();
                	final long loopSize = myChunk.getLoopSize();

                	final RealSum mySum = new RealSum();
                	int myCount = 0;
                	int myMinNumOverlap = Integer.MAX_VALUE;
            		long myAvgNumOverlap = 0;
            		int myCountAvgNumOverlap = 0;
            		
            		final ArrayList<Cursor<FloatType>> cursorsImage = new ArrayList<Cursor<FloatType>>();
            		final ArrayList<Cursor<FloatType>> cursorsWeight = new ArrayList<Cursor<FloatType>>();
            		
            		for ( final LRFFT fft : data )
            		{
            			cursorsImage.add( fft.getImage().createCursor() );
            			if ( fft.getWeight() != null )
            				cursorsWeight.add( fft.getWeight().createCursor() );
            		}

        			for ( final Cursor<FloatType> c : cursorsImage )
        				c.fwd( start );
        			
        			for ( final Cursor<FloatType> c : cursorsWeight )
        				c.fwd( start );
            		
        			
            		for ( long l = 0; l < loopSize; ++l )
            		{
        				for ( final Cursor<FloatType> c : cursorsImage )
        					c.fwd();
        				
        				for ( final Cursor<FloatType> c : cursorsWeight )
        					c.fwd();
        				
        				// sum up individual intensities
        				double sumLocal = 0;
        				int countLocal = 0;
        				
        				for ( int i = 0; i < cursorsImage.size(); ++i )
        				{
        					if ( cursorsWeight.get( i ).getType().get() != 0 )
        					{
        						sumLocal += cursorsImage.get( i ).getType().get();
        						countLocal++;
        					}
        				}
        				
        				// at least two overlap to compute the average intensity there
        				if ( countLocal > 1 )
        				{
        					mySum.add( sumLocal );
        					myCount += countLocal;
        				}
        				
        				if ( countLocal > 0 )
        				{
        					myAvgNumOverlap += countLocal;
        					myCountAvgNumOverlap++;
        					
        					myMinNumOverlap = Math.min( countLocal, myMinNumOverlap );
        				}
            		}
            		
            		sum[ myNumber ] = mySum;
            		count[ myNumber ] = myCount;
            		minNumOverlap[ myNumber ] = myMinNumOverlap;
            		avgNumOverlap[ myNumber ] = myAvgNumOverlap;
            		countAvgNumOverlap[ myNumber ] = myCountAvgNumOverlap;
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );

        IJ.log( new Date( System.currentTimeMillis() ) + ": done normalizing." );
		
		/*
		int minNumOverlap = data.size();
		long avgNumOverlap = 0;
		int countAvgNumOverlap = 0;
		
		final RealSum sum = new RealSum();
		// the number of overlapping pixels
		long count = 0;
		
		final ArrayList<Cursor<FloatType>> cursorsImage = new ArrayList<Cursor<FloatType>>();
		final ArrayList<Cursor<FloatType>> cursorsWeight = new ArrayList<Cursor<FloatType>>();
		
		for ( final LRFFT fft : data )
		{
			cursorsImage.add( fft.getImage().createCursor() );
			if ( fft.getWeight() != null )
				cursorsWeight.add( fft.getWeight().createCursor() );
		}
		
		final Cursor<FloatType> cursor = cursorsImage.get( 0 );

		// sum overlapping area individually
		while ( cursor.hasNext() )
		{
			for ( final Cursor<FloatType> c : cursorsImage )
				c.fwd();
			
			for ( final Cursor<FloatType> c : cursorsWeight )
				c.fwd();
			
			// sum up individual intensities
			double sumLocal = 0;
			int countLocal = 0;
			
			for ( int i = 0; i < cursorsImage.size(); ++i )
			{
				if ( cursorsWeight.get( i ).getType().get() != 0 )
				{
					sumLocal += cursorsImage.get( i ).getType().get();
					countLocal++;
				}
			}
			
			// at least two overlap
			if ( countLocal > 1 )
			{
				sum.add( sumLocal );
				count += countLocal;
			}
			
			if ( countLocal > 0 )
			{
				avgNumOverlap += countLocal;
				countAvgNumOverlap++;
				
				minNumOverlap = Math.min( countLocal, minNumOverlap );
			}
		}

		*/
        
        int minNumOverlapResult = minNumOverlap[ 0 ];
        long avgNumOverlapResult = avgNumOverlap[ 0 ];
		int countAvgNumOverlapResult = countAvgNumOverlap[ 0 ];
		
		RealSum sumResult = new RealSum();
		sumResult.add( sum[ 0 ].getSum() );
		long countResult = count[ 0 ];
		
        for ( int i = 1; i < numThreads; ++i )
        {
        	minNumOverlapResult = Math.min( minNumOverlapResult, minNumOverlap[ i ] );
        	avgNumOverlapResult += avgNumOverlap[ i ];
        	countAvgNumOverlapResult += countAvgNumOverlap[ i ];
        	countResult += count[ i ];
        	sumResult.add( sum[ i ].getSum() );
        }
        
		IJ.log( "Min number of overlapping views: " + minNumOverlapResult );
		IJ.log( "Average number of overlapping views: " + (avgNumOverlapResult/(double)countAvgNumOverlapResult) );

		if ( countResult == 0 )
			return 1;
		
		// compute the average sum
		final double avg = sumResult.getSum() / (double)countResult;
		
		// return the average intensity in the overlapping area
		return avg;
	}

	/**
	 * Adds additive gaussian noise: i = i + gauss(x, sigma)
	 * 
	 * @param amount - how many times sigma
	 * @return the signal-to-noise ratio (measured)
	 */
	public static double addGaussianNoise( final Image< FloatType > img, final Random rnd, final float sigma, boolean onlyPositive )
	{
		for ( final FloatType f : img )
		{
			float newValue = f.get() + (float)( rnd.nextGaussian() * sigma );
			
			if ( onlyPositive )
				newValue = Math.max( 0, newValue );
			
			f.set( newValue );
		}
		
		return 1;
	}

	/**
	 * Adds additive and multiplicative gaussian noise: i = i*gauss(x,sigma) + gauss(x, sigma)
	 * 
	 * @param amount - how many times sigma
	 * @return the signal-to-noise ratio (measured)
	 */
	public static double addGaussianNoiseAddMul( final Image< FloatType > img, final Random rnd, final float sigma, boolean onlyPositive )
	{
		for ( final FloatType f : img )
		{
			final float value = f.get();
			float newValue = value*(1+(float)( rnd.nextGaussian() * sigma/3 )) + (float)( Math.abs( rnd.nextGaussian() ) * sigma );
			
			if ( onlyPositive )
				newValue = Math.max( 0, newValue );
			
			f.set( newValue );
		}
		
		return 1;
	}

	public static void translate( final Image< FloatType > img, final float[] vector )
	{
		final Image< FloatType > tmp = img.clone();
		
		final LocalizableCursor< FloatType > cursor1 = img.createLocalizableCursor();		
		final Interpolator< FloatType > interpolator = tmp.createInterpolator( new LinearInterpolatorFactory<FloatType>( new OutOfBoundsStrategyMirrorFactory<FloatType>() ) );
		
		final int numDimensions = img.getNumDimensions();
		final float[] pos = new float[ numDimensions ];
		
		while ( cursor1.hasNext() )
		{
			cursor1.fwd();
			
			for( int d = 0; d < numDimensions; ++d )
				pos[ d ] = cursor1.getPosition( d ) - vector[ d ];
			
			interpolator.setPosition( pos );
			cursor1.getType().set( interpolator.getType() );
		}
		
		cursor1.close();
		interpolator.close();
	}
	
	/**
	 * Adjusts an image so that the minimal intensity is minValue and the average is average
	 * 
	 * @param image - the image to norm
	 * @param minValue - the minimal value
	 * @param targetAverage - the average that we want to have
	 */
	public static void adjustImage( final Image<FloatType> image, final float minValue, final float targetAverage )
	{
		// first norm the image to an average of (targetAverage - minValue)
		final double avg = sumImage( image )/(double)image.getNumPixels();
		final double correction = ( targetAverage - minValue ) / avg;

		// correct 
		for ( final FloatType t : image )
			t.set( (float)( t.get() * correction ) );
			
		// now add minValue to all pixels
		for ( final FloatType t : image )
			t.set( t.get() + minValue );
		
		//System.out.println( sumImage( image )/(double)image.getNumPixels() );
	}

}
