package mpicbg.spim.registration.detection;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.integral.IntegralImageLong;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.LongType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class DetectionSegmentation 
{	
	public static <T extends RealType<T>> ArrayList< DifferenceOfGaussianPeak<T> > extractBeadsLaPlaceImgLib( 
			final Image<T> img,
			final float initialSigma,
			float minPeakValue,
			float minInitialPeakValue )
	{
		return extractBeadsLaPlaceImgLib(img, new OutOfBoundsStrategyMirrorFactory<T>(), 0.5f, initialSigma, minPeakValue, minInitialPeakValue, 4, true, false, ViewStructure.DEBUG_MAIN );
	}	

	public static <T extends RealType<T>> ArrayList< DifferenceOfGaussianPeak<T> > extractBeadsLaPlaceImgLib( 
 			final Image<T> img,
 			final OutOfBoundsStrategyFactory<T> oobsFactory,
 			final float imageSigma, 
 			final float initialSigma,
 			float minPeakValue,
 			float minInitialPeakValue,
 			final int stepsPerOctave,
 			final boolean findMax,
 			final boolean findMin,
 			final int debugLevel )
         	{
				final float k = (float)computeK( stepsPerOctave );
				final float sigma1 = initialSigma;
				final float sigma2 = initialSigma * k;
				
				return extractBeadsLaPlaceImgLib(img, oobsFactory, imageSigma, sigma1, sigma2, minPeakValue, minInitialPeakValue, findMax, findMin, debugLevel );
         	}
	
	public static <T extends RealType<T>> ArrayList< DifferenceOfGaussianPeak<T> > extractBeadsLaPlaceImgLib( 
			final Image<T> img,
			final OutOfBoundsStrategyFactory<T> oobsFactory,
			final float imageSigma, 
			final float sigma1,
			final float sigma2,
			float minPeakValue,
			float minInitialPeakValue,
			final boolean findMax,
			final boolean findMin,
			final int debugLevel )
	{
        //
        // Compute the Sigmas for the gaussian folding
        //
        final float[] sigmaXY = new float[]{ sigma1, sigma2 };
        final float[] sigmaDiffXY = computeSigmaDiff( sigmaXY, imageSigma );
        
		final float k = sigmaXY[ 1 ] / sigmaXY[ 0 ];
        final float K_MIN1_INV = computeKWeight(k);

        final double[][] sigmaDiff = new double[ 2 ][ 3 ];
        sigmaDiff[ 0 ][ 0 ] = sigmaDiffXY[ 0 ];
        sigmaDiff[ 0 ][ 1 ] = sigmaDiffXY[ 0 ];
        sigmaDiff[ 1 ][ 0 ] = sigmaDiffXY[ 1 ];
        sigmaDiff[ 1 ][ 1 ] = sigmaDiffXY[ 1 ];

        // sigmaZ is at least twice the image sigma
		if ( img.getNumDimensions() == 3 )
		{
			final float sigma1Z = Math.max( imageSigma * 2, sigma1 / img.getCalibration( 2 ) );
			final float sigma2Z = sigma1Z * k;
			final float[] sigmaZ = new float[]{ sigma1Z, sigma2Z };
			final float[] sigmaDiffZ = computeSigmaDiff( sigmaZ, imageSigma );
	        sigmaDiff[ 0 ][ 2 ] = sigmaDiffZ[ 0 ];
	        sigmaDiff[ 1 ][ 2 ] = sigmaDiffZ[ 1 ];
		}
        
        //System.out.println( sigmaXY[ 0 ] + ", " + sigmaXY[ 0 ] + ", " + sigmaZ[ 0 ] );
        //System.out.println( sigmaXY[ 1 ] + ", " + sigmaXY[ 1 ] + ", " + sigmaZ[ 1 ] );
        //System.out.println( sigmaDiff[ 0 ][ 0 ] + ", " + sigmaDiff[ 0 ][ 1 ] + ", " + sigmaDiff[ 0 ][ 2 ] );
        //System.out.println( sigmaDiff[ 1 ][ 0 ] + ", " + sigmaDiff[ 1 ][ 1 ] + ", " + sigmaDiff[ 1 ][ 2 ] );
        
		// compute difference of gaussian
		final DifferenceOfGaussianReal1<T> dog = new DifferenceOfGaussianReal1<T>( img, oobsFactory, sigmaDiff[0], sigmaDiff[1], minInitialPeakValue, K_MIN1_INV );
		dog.setKeepDoGImage( true );
		
		if ( !dog.checkInput() || !dog.process() )
		{
    		if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Cannot compute difference of gaussian for " + dog.getErrorMessage() );
			
			return new ArrayList< DifferenceOfGaussianPeak<T> >();
		}

		// remove all minima
        final ArrayList< DifferenceOfGaussianPeak<T> > peakList = dog.getPeaks();
        for ( int i = peakList.size() - 1; i >= 0; --i )
        {
        	if ( !findMin )
        	{
        		if ( peakList.get( i ).isMin() )
        			peakList.remove( i );
        	}
        	
        	if ( !findMax )
        	{
        		if ( peakList.get( i ).isMax() )
        			peakList.remove( i );        		
        	}
        }
		
        final SubpixelLocalization<T> spl = new SubpixelLocalization<T>( dog.getDoGImage(), dog.getPeaks() );
		spl.setAllowMaximaTolerance( true );
		spl.setMaxNumMoves( 10 );
		
		if ( !spl.checkInput() || !spl.process() )
		{
    		if ( debugLevel <= ViewStructure.DEBUG_ERRORONLY )
    			IOFunctions.println("(" + new Date(System.currentTimeMillis()) + "): Warning! Failed to compute subpixel localization " + spl.getErrorMessage() );
		}
		
		//dog.getDoGImage().getDisplay().setMinMax();
		//ImageJFunctions.copyToImagePlus( dog.getDoGImage() ).show();
		dog.getDoGImage().close();
			
        int peakTooLow = 0;
        int invalid = 0;
        int extrema = 0;

		// remove entries that are too low
        for ( int i = peakList.size() - 1; i >= 0; --i )
        {
        	final DifferenceOfGaussianPeak<T> maximum = peakList.get( i );
        	
        	if ( !maximum.isValid() )
        		++invalid;
        	
        	if ( findMax )
        	{
	        	if ( maximum.isMax() ) 
	        	{
	        		++extrema;
	        		if ( Math.abs( maximum.getValue().getRealDouble() ) < minPeakValue )
	        		{
	        			peakList.remove( i );
	        			++peakTooLow;
	        		}
	        	}
        	}
        	if ( findMin )
        	{
	        	if ( maximum.isMin() ) 
	        	{
	        		++extrema;
	        		if ( Math.abs( maximum.getValue().getRealDouble() ) < minPeakValue )
	        		{
	        			peakList.remove( i );
	        			++peakTooLow;
	        		}
	        	}        		
        	}
        }
        
		if ( debugLevel <= ViewStructure.DEBUG_ALL )
		{
	        IOFunctions.println( "number of peaks: " + dog.getPeaks().size() );        
	        IOFunctions.println( "invalid: " + invalid );
	        IOFunctions.println( "extrema: " + extrema );
	        IOFunctions.println( "peak to low: " + peakTooLow );
		}
		
		return peakList;
		
	}

	public static double computeK( final int stepsPerOctave ) { return Math.pow( 2f, 1f / stepsPerOctave ); }
	public static float computeKWeight( final float k ) { return 1.0f / (k - 1.0f); }
	public static float[] computeSigma( final float k, final float initialSigma )
	{
		final float[] sigma = new float[ 2 ];

		sigma[ 0 ] = initialSigma;
		sigma[ 1 ] = sigma[ 0 ] * k;

		return sigma;
	}
	public static float getDiffSigma( final float sigmaA, final float sigmaB ) { return (float) Math.sqrt( sigmaB * sigmaB - sigmaA * sigmaA ); }
	public static float[] computeSigmaDiff( final float[] sigma, final float imageSigma )
	{
		final float[] sigmaDiff = new float[ 2 ];

		sigmaDiff[ 0 ] = getDiffSigma( imageSigma, sigma[ 0 ] );
		sigmaDiff[ 1 ] = getDiffSigma( imageSigma, sigma[ 1 ] );

		return sigmaDiff;
	}

	public static ArrayList< DifferenceOfGaussianPeak< FloatType > > extractBeadsIntegralImage( 
			final ViewDataBeads view, final float minIntensity )
	{
		final Image< FloatType > img = view.getImage( false ); 
		IntegralImageLong< FloatType > intImg = new IntegralImageLong<FloatType>( img, new LongType(), new Converter< FloatType, LongType >()
		{
			@Override
			public void convert( final FloatType input, final LongType output ) { output.set( Util.round( input.get() ) ); } 
		} );
		intImg.process();
		final Image< LongType > integralImg = intImg.getResult();

		final FloatType min = new FloatType();
		final FloatType max = new FloatType();
		
		computeMinMax( img, min, max );
		
		//final Image< FloatType > dom = img.createNewImage();
		
		// in-place
		computeDifferencOfMean( integralImg, img, 3, 3, 3, 5, 5, 5, min.get(), max.get() );
		
		return null;
	}
	
	final private static void computeDifferencOfMean( final Image< LongType> integralImg, final Image< FloatType > domImg, final int sx1, final int sy1, final int sz1, final int sx2, final int sy2, final int sz2, final float min, final float max  )
	{
		final float sumPixels1 = sx1 * sy1 * sz1;
		final float sumPixels2 = sx2 * sy2 * sz2;
		
		final int sx1Half = sx1 / 2;
		final int sy1Half = sy1 / 2;
		final int sz1Half = sz1 / 2;

		final int sx2Half = sx2 / 2;
		final int sy2Half = sy2 / 2;
		final int sz2Half = sz2 / 2;
		
		final int sxHalfMax = Math.max( sx1Half, sx2Half );
		final int syHalfMax = Math.max( sy1Half, sy2Half );
		final int szHalfMax = Math.max( sz1Half, sz2Half );

		final int w = domImg.getDimension( 0 ) - ( Math.max( sx1, sx2 ) / 2 ) * 2;
		final int h = domImg.getDimension( 1 ) - ( Math.max( sy1, sy2 ) / 2 ) * 2;
		final int d = domImg.getDimension( 2 ) - ( Math.max( sz1, sz2 ) / 2 ) * 2;
				
		final long imageSize = domImg.getNumPixels();

		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads();

        final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, threads.length );
		
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();
        
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( myNumber );
                	final long loopSize = myChunk.getLoopSize();
                	
            		final LocalizableCursor< FloatType > cursor = domImg.createLocalizableCursor();
            		final LocalizableByDimCursor< LongType > randomAccess = integralImg.createLocalizableByDimCursor();

            		cursor.fwd( myChunk.getStartPosition() );
            		
            		// do as many pixels as wanted by this thread
                    for ( long j = 0; j < loopSize; ++j )
                    {                    	
            			final FloatType result = cursor.next();
            			
            			final int x = cursor.getPosition( 0 );
            			final int y = cursor.getPosition( 1 );
            			final int z = cursor.getPosition( 2 );
            			
            			final int xt = x - sxHalfMax;
            			final int yt = y - syHalfMax;
            			final int zt = z - szHalfMax;
            			
            			if ( xt >= 0 && yt >= 0 && zt >= 0 && xt < w && yt < h && zt < d )
            			{
            				final float s1 = computeSum( x - sx1Half, y - sy1Half, z - sz1Half, sx1, sy1, sz1, randomAccess ) / sumPixels1;
            				final float s2 = computeSum( x - sx2Half, y - sy2Half, z - sz2Half, sx2, sy2, sz2, randomAccess ) / sumPixels2;
            			
            				result.set( ( s2 - s1 ) / ( max - min ) );
            			}
                    }
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
 	}
	
	/**
	 * Compute the average in the area
	 * 
	 * @param fromX - start coordinate in x (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param fromY - start coordinate in y (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param fromZ - start coordinate in z (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param sX - number of pixels in x
	 * @param sY - number of pixels in y
	 * @param sZ - number of pixels in z
	 * @param randomAccess - randomAccess on the integral image
	 * @return
	 */
	final private static long computeSum( final int fromX, final int fromY, final int fromZ, final int vX, final int vY, final int vZ, final LocalizableByDimCursor< LongType > randomAccess )
	{
		randomAccess.setPosition( fromX, 0 );
		randomAccess.setPosition( fromY, 1 );
		randomAccess.setPosition( fromZ, 2 );
		
		long sum = -randomAccess.getType().get();
		
		randomAccess.move( vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( vY, 1 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( -vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( vZ, 2 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( vX, 0 );
		sum += randomAccess.getType().get();
		
		randomAccess.move( -vY, 1 );
		sum += -randomAccess.getType().get();
		
		randomAccess.move( -vX, 0 );
		sum += randomAccess.getType().get();
		
		return sum;
	}

	final static void computeMinMax( final Image< FloatType > img, final FloatType min, final FloatType max )
	{
		min.set( Float.MAX_VALUE );
		max.set( Float.MIN_VALUE );
		
		for ( final FloatType t : img )
		{
			final float value = t.get();
			
			if ( value > max.get() )
				max.set( value );
			
			if ( value < min.get() )
				min.set( value );
		}
	}

}
