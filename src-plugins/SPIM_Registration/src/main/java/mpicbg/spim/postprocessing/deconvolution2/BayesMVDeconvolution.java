package mpicbg.spim.postprocessing.deconvolution2;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.postprocessing.deconvolution2.LRFFT.PSFTYPE;

public class BayesMVDeconvolution implements Deconvolver
{
	public static boolean debug = true;
	public static int debugInterval = 1;
	final static float minValue = 0.0001f;
	public static int speedUp = 1;

	final int numViews, numDimensions;
    final float avg;
    final double lambda;
    
    ImageStack stack;
    CompositeImage ci;
    
    boolean collectStatistics = true;
    
    // current iteration
    int i = 0;
    
	// the multi-view deconvolved image
	Image<FloatType> psi;
	
	// the input data
	final LRInput views;
	ArrayList<LRFFT> data;
	String name;
	
	public BayesMVDeconvolution( final LRInput views, final PSFTYPE iterationType, final int numIterations, final double lambda, final String name )
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
		views.init( iterationType );
		
		//
		// the real data image psi is initialized with the average 
		//	
		for ( final FloatType f : psi )
			f.set( avg );
		
		//this.stack = new ImageStack( this.psi.getDimension( 0 ), this.psi.getDimension( 1 ) );
		
		// run the deconvolution
		while ( i < numIterations )
		{
			runIteration();
			
			if ( debug && (i-1) % debugInterval == 0 )
			{
				psi.getDisplay().setMinMax( 0, 1 );
				final ImagePlus tmp = ImageJFunctions.copyToImagePlus( psi );
				
				if ( this.stack == null )
				{
					this.stack = tmp.getImageStack();
					for ( int i = 0; i < this.psi.getDimension( 2 ); ++i )
						this.stack.setSliceLabel( "Iteration 1", i + 1 );
					
					tmp.setTitle( "debug view" );
					this.ci = new CompositeImage( tmp, CompositeImage.COMPOSITE );
					this.ci.setDimensions( 1, this.psi.getDimension( 2 ), 1 );
					this.ci.show();
				}
				else if ( stack.getSize() == this.psi.getDimension( 2 ) )
				{
					IJ.log( "Stack size = " + this.stack.getSize() );
					final ImageStack t = tmp.getImageStack();
					for ( int i = 0; i < this.psi.getDimension( 2 ); ++i )
						this.stack.addSlice( "Iteration 2", t.getProcessor( i + 1 ) );
					IJ.log( "Stack size = " + this.stack.getSize() );
					this.ci.hide();
					IJ.log( "Stack size = " + this.stack.getSize() );
					
					this.ci = new CompositeImage( new ImagePlus( "debug view", this.stack ), CompositeImage.COMPOSITE );
					this.ci.setDimensions( 1, this.psi.getDimension( 2 ), 2 );
					this.ci.show();
				}
				else
				{
					final ImageStack t = tmp.getImageStack();
					for ( int i = 0; i < this.psi.getDimension( 2 ); ++i )
						this.stack.addSlice( "Iteration " + i, t.getProcessor( i + 1 ) );

					this.ci.setStack( this.stack, 1, this.psi.getDimension( 2 ), stack.getSize() / this.psi.getDimension( 2 ) );	
				}
				/*
				Image<FloatType> psiCopy = psi.clone();
				//ViewDataBeads.normalizeImage( psiCopy );
				psiCopy.setName( "Iteration " + i + " l=" + lambda );
				psiCopy.getDisplay().setMinMax( 0, 1 );
				ImageJFunctions.copyToImagePlus( psiCopy ).show();
				psiCopy.close();
				psiCopy = null;*/
			}
		}
		
		IJ.log( "DONE (" + new Date(System.currentTimeMillis()) + ")." );
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

		//int view = iteration % numViews;
		for ( int view = 0; view < numViews; ++view )
		{
			final LRFFT processingData = data.get( view );
						
			long time = System.currentTimeMillis();
			
			// convolve psi (current guess of the image) with the PSF of the current view
			final Image<FloatType> psiBlurred = processingData.convolve1( psi );
			
			//System.out.println( view + " 1: " + fftConvolution.getProcessingTime() + " ms." );
			System.out.println( view + " a: " + (time - System.currentTimeMillis()) + " ms." );
			
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

	        time = System.currentTimeMillis();
	        
			// blur the residuals image with the kernel
	        final Image< FloatType > integral = processingData.convolve2( psiBlurred );

			//System.out.println( view + " 2: " + invFFConvolution.getProcessingTime() + " ms." );
			System.out.println( view + " b: " + (time - System.currentTimeMillis()) + " ms." );

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
	                	
	            		computeFinalValues( myChunk.getStartPosition(), myChunk.getLoopSize(), psi, integral, processingData.getWeight(), lambda );                		
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
			
			IJ.log("iteration: " + iteration + " --- sum change: " + sumChange + " --- max change per pixel: " + maxChange );
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
	
	private static final void computeFinalValues( final long start, final long loopSize, final Image< FloatType > psi, final Image<FloatType> integral, final Image<FloatType> weight, final double lambda )
	{
		final Cursor< FloatType > cursorPsi = psi.createCursor();
		final Cursor< FloatType > cursorIntegral = integral.createCursor();
		final Cursor< FloatType > cursorWeight = weight.createCursor();
		
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
