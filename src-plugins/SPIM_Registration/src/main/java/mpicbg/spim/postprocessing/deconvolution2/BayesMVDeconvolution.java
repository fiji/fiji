package mpicbg.spim.postprocessing.deconvolution2;

import ij.IJ;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.type.numeric.real.FloatType;

public class BayesMVDeconvolution implements Deconvolver
{
	public static boolean debug = false;
	public static int debugInterval = 2;
	final static float minValue = 0.0001f;

	final int numViews, numDimensions;
    final float avg;
    final double lambda;
    
    boolean collectStatistics = true;
    
    // current iteration
    int i = 0;
    
	// the multi-view deconvolved image
	Image<FloatType> psi;
	
	// the input data
	final LRInput views;
	ArrayList<LRFFT> data;
	String name;
	
	public BayesMVDeconvolution( final LRInput views, final int numIterations, final double lambda, final String name )
	{
		this.name = name;
		this.data = views.getViews();
		this.views = views;
		this.numViews = data.size();
		this.numDimensions = data.get( 0 ).getImage().getNumDimensions();
		this.lambda = lambda;
		
		this.psi = data.get( 0 ).getImage().createNewImage( "psi (deconvolved image)" );
		
		this.avg = (float)AdjustInput.normAllImages( data );
		
		System.out.println( "Average intensity in overlapping area: " + avg );        
		
		ImageJFunctions.show( views.getViews().get( 0 ).getWeight() );
		
		//
		// the real data image psi is initialized with the average 
		//	
		for ( final FloatType f : psi )
			f.set( avg );
		
		// run the deconvolution
		while ( i < numIterations )
		{
			runIteration();
			
			if ( debug && i % debugInterval == 0 )
			{
				Image<FloatType> psiCopy = psi.clone();
				//ViewDataBeads.normalizeImage( psiCopy );
				psiCopy.setName( "Iteration " + i + " l=" + lambda );
				psiCopy.getDisplay().setMinMax( 0, 1 );
				ImageJFunctions.copyToImagePlus( psiCopy ).show();
				psiCopy.close();
				psiCopy = null;			}
		}
	}
	
	public LRInput getData() { return views; }
	public String getName() { return name; }
	public double getAvg() { return avg; }
	
	public Image<FloatType> getPsi() { return psi; }	
	public int getCurrentIteration() { return i; }
	
	public void runIteration() 
	{
		runIteration( psi, data, lambda, minValue, collectStatistics, i++ );
	}

	final private static void runIteration( final Image< FloatType> psi, final ArrayList< LRFFT > data, 
			final double lambda, final float minValue, final boolean collectStatistic, final int iteration )
	{
		final int numViews = data.size();

		Image< FloatType > lastIteration = null;
		
		if ( collectStatistic )
			lastIteration = psi.clone();

		for ( int view = 0; view < numViews; ++view )
		{
			final LRFFT processingData = data.get( view );
			
			// convolve psi (current guess of the image) with the PSF of the current view
			final FourierConvolution<FloatType, FloatType> fftConvolution = processingData.getFFTConvolution1();
			fftConvolution.replaceImage( psi );
			fftConvolution.process();
			
			final Image<FloatType> psiBlurred = fftConvolution.getResult();
			
			// compute quotient img/psiBlurred
			final Cursor<FloatType> cursorImg = processingData.getImage().createCursor();
			final Cursor<FloatType> cursorPsiBlurred = psiBlurred.createCursor();
			
			while ( cursorImg.hasNext() )
			{
				cursorImg.fwd();
				cursorPsiBlurred.fwd();
				
				final float imgValue = cursorImg.getType().get();
				final float psiBlurredValue = cursorPsiBlurred.getType().get();
				
				cursorPsiBlurred.getType().set( imgValue / psiBlurredValue );
			}
			
			cursorImg.close();
			cursorPsiBlurred.close();
			
			// blur the residuals image with the kernel
			final FourierConvolution<FloatType, FloatType> invFFConvolution = processingData.getFFTConvolution2();
			invFFConvolution.replaceImage( psiBlurred );
			invFFConvolution.process();
	
			// the result from the previous iteration
			final Cursor< FloatType > cursorPsi = psi.createCursor();
			final Cursor< FloatType > cursorIntegral = invFFConvolution.getResult().createCursor();
			final Cursor< FloatType > cursorWeight = processingData.getWeight().createCursor();
			
			while ( cursorPsi.hasNext() )
			{
				cursorPsi.fwd();
				cursorIntegral.fwd();
				cursorWeight.fwd();
				
				final float lastPsiValue = cursorPsi.getType().get();
				
				float value = lastPsiValue * cursorIntegral.getType().get();
	
				if ( value > 0 )
				{
					//
					// perform Tikhonov regularization if desired
					//		
					if ( lambda > 0 )
						value = ( (float)( (Math.sqrt( 1.0 + 2.0*lambda*value ) - 1.0) / lambda ) );
				}
				else
				{
					value = minValue;
				}
				//
				// get the final value and some statistics
				//
				float nextPsiValue;
				
				if ( Double.isNaN( value ) )
					nextPsiValue = (float)minValue;
				else
					nextPsiValue = (float)Math.max( minValue, value );
				
				// compute the difference between old and new
				float change = nextPsiValue - lastPsiValue;				
				
				// apply the apropriate amount
				change *= cursorWeight.getType().get();
				nextPsiValue = lastPsiValue + change;

				// store the new value
				cursorPsi.getType().set( (float)nextPsiValue );
			}
		}
		
		if ( collectStatistic )
		{
			double sumChange = 0;
			double maxChange = -1;
			
			final Cursor< FloatType > cursorPsi = psi.createCursor();
			final Cursor< FloatType > cursorLast = lastIteration.createCursor();
			
			while ( cursorLast.hasNext() )
			{
				final float last = cursorLast.next().get();
				final float next = cursorPsi.next().get();
				
				final float change = Math.abs( next - last );				
					
				sumChange += change;
				maxChange = Math.max( maxChange, change );
			}
			
			IJ.log("------------------------------------------------");
			IJ.log(" Iteration: " + iteration );
			IJ.log(" Change: " + sumChange );
			IJ.log(" Max Change per Pixel: " + maxChange );
			IJ.log("------------------------------------------------");
		}
	}
}
