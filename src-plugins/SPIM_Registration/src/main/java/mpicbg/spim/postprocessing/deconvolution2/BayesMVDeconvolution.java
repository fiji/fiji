package mpicbg.spim.postprocessing.deconvolution2;

import ij.IJ;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;

public class BayesMVDeconvolution implements Deconvolver
{
	public static boolean debug = false;
	public static int debugInterval = 1;
	final static float minValue = 0.0001f;
	public static int speedUp = 1;

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
		
		this.avg = (float)AdjustInput.normAllImages( data, speedUp );
		
		IJ.log( "Average intensity in overlapping area: " + avg );        
		
		// init all views
		views.init( true );
		
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
		IJ.log( "iteration: " + iteration + " (" + new Date(System.currentTimeMillis()) + ")" );
		
		final int numViews = data.size();
		final Vector< Chunk > threadChunks = SimpleMultiThreading.divideIntoChunks( psi.getNumPixels(), Runtime.getRuntime().availableProcessors() );
		final int numThreads = threadChunks.size();
		
		final Image< FloatType > lastIteration;
		
		if ( collectStatistic )
			lastIteration = psi.clone();
		else
			lastIteration = null;

		//long time = System.currentTimeMillis();

		for ( int view = 0; view < numViews; ++view )
		{
			final LRFFT processingData = data.get( view );
						
			// convolve psi (current guess of the image) with the PSF of the current view
			final FourierConvolution<FloatType, FloatType> fftConvolution = processingData.getFFTConvolution1();
			fftConvolution.replaceImage( psi );
			fftConvolution.process();
			
			//System.out.println( view + " 1: " + fftConvolution.getProcessingTime() + " ms." );
			//System.out.println( view + " a: " + (time - System.currentTimeMillis()) + " ms." );
			final Image<FloatType> psiBlurred = fftConvolution.getResult();
			
			// size = 666, 363, 537
			
			// compute quotient img/psiBlurred
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
	                	
	            		computeQuotient( myChunk.getStartPosition(), myChunk.getLoopSize(), psiBlurred, processingData );                		
	                }
	            });
	        
	        SimpleMultiThreading.startAndJoin( threads );

			//System.out.println( view + " b: " + (time - System.currentTimeMillis()) + " ms." );

			// blur the residuals image with the kernel
			final FourierConvolution<FloatType, FloatType> invFFConvolution = processingData.getFFTConvolution2();
			invFFConvolution.replaceImage( psiBlurred );
			invFFConvolution.process();
			
			//System.out.println( view + " 2: " + invFFConvolution.getProcessingTime() + " ms." );
			//System.out.println( view + " c: " + (time - System.currentTimeMillis()) + " ms." );

			ai.set( 0 );
	        for ( int ithread = 0; ithread < threads.length; ++ithread )
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                	// Thread ID
	                	final int myNumber = ai.getAndIncrement();
	        
	                	// get chunk of pixels to process
	                	final Chunk myChunk = threadChunks.get( myNumber );
	                	
	            		computeFinalValues( myChunk.getStartPosition(), myChunk.getLoopSize(), psi, invFFConvolution, processingData, lambda );                		
	                }
	            });
	        
	        SimpleMultiThreading.startAndJoin( threads );

			// the result from the previous iteration
			//System.out.println( view + " d: " + (time - System.currentTimeMillis()) + " ms." );
		}
		
		if ( collectStatistic )
		{
			final AtomicInteger ai = new AtomicInteger(0);					
	        final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
	        
	        final double[][] sumMax = new double[ numThreads ][ 2 ];
	        
	        for ( int ithread = 0; ithread < threads.length; ++ithread )
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                	// Thread ID
	                	final int myNumber = ai.getAndIncrement();
	        
	                	// get chunk of pixels to process
	                	final Chunk myChunk = threadChunks.get( myNumber );
	                	
	                	collectStatistics( myChunk.getStartPosition(), myChunk.getLoopSize(), psi, lastIteration, sumMax[ myNumber ] );
	                }
	            });
	        
	        SimpleMultiThreading.startAndJoin( threads );
			
	        // accumulate the results from the individual threads
			double sumChange = 0;
			double maxChange = -1;
			
			for ( int i = 0; i < numThreads; ++i )
			{
				sumChange += sumMax[ i ][ 0 ];
				maxChange = Math.max( maxChange, sumMax[ i ][ 1 ] );
			}
			
			IJ.log("------------------------------------------------");
			IJ.log(" Iteration: " + iteration );
			IJ.log(" Change: " + sumChange );
			IJ.log(" Max Change per Pixel: " + maxChange );
			IJ.log("------------------------------------------------");
		}
		
		//System.out.println( "final: " + (time - System.currentTimeMillis()) + " ms." );
	}
	
	private static final void collectStatistics( final long start, final long loopSize, final Image< FloatType > psi, final Image< FloatType > lastIteration, final double[] sumMax )
	{
		double sumChange = 0;
		double maxChange = -1;
		
		final Cursor< FloatType > cursorPsi = psi.createCursor();
		final Cursor< FloatType > cursorLast = lastIteration.createCursor();
		
		cursorPsi.fwd( start );
		cursorLast.fwd( start );
		
		for ( long l = 0; l < loopSize; ++l )
		{
			final float last = cursorLast.next().get();
			final float next = cursorPsi.next().get();
			
			final float change = Math.abs( next - last );				
				
			sumChange += change;
			maxChange = Math.max( maxChange, change );
		}
		
		sumMax[ 0 ] = sumChange;
		sumMax[ 1 ] = maxChange;
	}
	
	private static final void computeQuotient( final long start, final long loopSize, final Image< FloatType > psiBlurred, final LRFFT processingData )
	{
		final Cursor<FloatType> cursorImg = processingData.getImage().createCursor();
		final Cursor<FloatType> cursorPsiBlurred = psiBlurred.createCursor();
		
		cursorImg.fwd( start );
		cursorPsiBlurred.fwd( start );
		
		for ( long l = 0; l < loopSize; ++l )
		{
			cursorImg.fwd();
			cursorPsiBlurred.fwd();
			
			final float imgValue = cursorImg.getType().get();
			final float psiBlurredValue = cursorPsiBlurred.getType().get();
			
			cursorPsiBlurred.getType().set( imgValue / psiBlurredValue );
		}
		
		cursorImg.close();
		cursorPsiBlurred.close();
		
	}
	
	private static final void computeFinalValues( final long start, final long loopSize, final Image< FloatType > psi, final FourierConvolution<FloatType, FloatType> invFFConvolution, final LRFFT processingData, final double lambda )
	{
		final Cursor< FloatType > cursorPsi = psi.createCursor();
		final Cursor< FloatType > cursorIntegral = invFFConvolution.getResult().createCursor();
		final Cursor< FloatType > cursorWeight = processingData.getWeight().createCursor();
		
		cursorPsi.fwd( start );
		cursorIntegral.fwd( start );
		cursorWeight.fwd( start );
		
		for ( long l = 0; l < loopSize; ++l )
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
}
