package mpicbg.spim.registration.detection;

import java.util.ArrayList;
import java.util.Date;

import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.spim.io.IOFunctions;
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
}
