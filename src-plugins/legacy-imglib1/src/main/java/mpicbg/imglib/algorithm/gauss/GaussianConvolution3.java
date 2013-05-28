/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package mpicbg.imglib.algorithm.gauss;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.math.ImageConverter;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.type.numeric.NumericType;
import mpicbg.imglib.util.Util;

/**
 * Computes a Gaussian Convolution on any {@link Type}(A) for which is defined how to convert it into a {@link NumericType}(B) on which the convolution is performed and back to the desired output {@link Type}(C). 
 * Of course A and C can be same. Note that the kernel precision is limited to double here.
 * 
 * @param <A> - The input {@link Type}
 * @param <B> - The {@link NumericType} used to compute the gaussian convolution
 * @param <C> - The output {@link Type}
 *
 * @author Stephan Preibisch
 */
public class GaussianConvolution3< A extends Type<A>, B extends NumericType<B>, C extends Type<C> > implements MultiThreaded, OutputAlgorithm<C>, Benchmark
{	
	Image<A> image;	
	final ImageFactory<B> factoryProcess;
	final ImageFactory<C> factoryOut;
	Image<C> convolved;
	
	Image<B> temp1, temp2;
	
	final Converter<A, B> converterIn; 
	final Converter<B, C> converterOut;
	
	final OutOfBoundsStrategyFactory<B> outOfBoundsFactory;
	int numDimensions;
	final double[] sigma;
    final double[][] kernel;

	long processingTime;
	int numThreads;
	String errorMessage = "";

	public GaussianConvolution3( final Image<A> image, final ImageFactory<B> factoryProcess, final ImageFactory<C> factoryOut, final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, 
									   final Converter<A, B> converterIn, final Converter<B, C> converterOut, final double[] sigma )
	{
		this.image = image;
		this.factoryProcess = factoryProcess;
		this.factoryOut = factoryOut;
		
		this.converterIn = converterIn;
		this.converterOut = converterOut;
		
		this.sigma = sigma;
		this.processingTime = -1;
		
		this.outOfBoundsFactory = outOfBoundsFactory;
		numDimensions = image.getNumDimensions();

		this.kernel = new double[ numDimensions ][];
		
		setNumThreads();
		computeKernel();
	}

	public GaussianConvolution3( final Image<A> image, final ImageFactory<B> factoryProcess, final ImageFactory<C> factoryOut, final OutOfBoundsStrategyFactory<B> outOfBoundsFactory, 
									   final Converter<A, B> converterIn, final Converter<B, C> converterOut, final double sigma )
	{
		this ( image, factoryProcess, factoryOut, outOfBoundsFactory, converterIn, converterOut, createArray( image, sigma ) );
	}
	
	protected static double[] createArray( final Image<?> image, final double sigma )
	{
		final double[] sigmas = new double[ image.getNumDimensions() ];
		
		for ( int d = 0; d < image.getNumDimensions(); ++d )
			sigmas[ d ] = sigma;
		
		return sigmas;
	}

	protected void computeKernel()
	{
		for ( int d = 0; d < numDimensions; ++d )
			this.kernel[ d ] = Util.createGaussianKernel1DDouble( sigma[ d ], true );		
	}
	
	public void setSigma( final double sigma ) { setSigma( createArray( image, sigma ) ); } 
	public void setSigma( final double sigma[] ) 
	{
		for ( int d = 0; d < numDimensions; ++d )
			this.sigma[ d ] = sigma[ d ];
		
		computeKernel();
	}
	public double[] getSigma() { return sigma.clone(); }
	
	public void setImage( final Image<A> image ) 
	{ 
		this.image = image;
		this.numDimensions = image.getNumDimensions();
	}
	public Image<A> getImage() { return image; }
		
	@Override
	public long getProcessingTime() { return processingTime; }
	
	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }	
	
	/**
	 * The sigma the image was convolved with
	 * @return - double sigma
	 */
	public double[] getSigmas() { return sigma; }
	
	public int getKernelSize( final int dim ) { return kernel[ dim ].length; }
	
	@Override
	public Image<C> getResult() { return convolved;	}

	@Override
	public boolean checkInput() 
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image == null )
		{
			errorMessage = "GaussianConvolution: [Image<T> img] is null.";
			return false;
		}
		else if ( outOfBoundsFactory == null )
		{
			errorMessage = "GaussianConvolution: [OutOfBoundsStrategyFactory<T>] is null.";
			return false;
		}
		else
			return true;
	}

	@Override
	public String getErrorMessage() { return errorMessage; }
	
	protected Image<B> getInputImage()
	{		
		final ImageConverter<A, B> convIn = new ImageConverter<A, B>( image, factoryProcess, converterIn );
		
		if ( !convIn.checkInput() || !convIn.process() )
		{
			errorMessage = "Cannot convert from A->B: " + convIn.getErrorMessage();
			return null;
		}
		
		return convIn.getResult();
	}
	
	protected Image<B> getTempImage1( final int currentDim )
	{
		if ( currentDim == 0 )
			temp1 = getInputImage();
		
		return temp1;
	}

	protected Image<B> getTempImage2( final int currentDim )
	{
		if ( currentDim == 0 )
			temp2 = temp1.createNewImage();
		
		return temp2;
	}

	protected LocalizableByDimCursor<B> getInputIterator( final Image<B> temp1, final Image<B> temp2, final int currentDim )
	{
    	if ( currentDim % 2 == 0 )
    		return temp1.createLocalizableByDimCursor( outOfBoundsFactory );
    	else
    		return temp2.createLocalizableByDimCursor( outOfBoundsFactory );		
	}
	
	protected LocalizableCursor<B> getOutputIterator( final Image<B> temp1, final Image<B> temp2, final int currentDim )
	{
    	if ( currentDim % 2 == 0 )
    		return temp2.createLocalizableCursor();
    	else
    		return temp1.createLocalizableCursor();	                				
	}
	
	protected Image<C> getConvolvedImage()
	{
		// convert to output type/container        
        final Image<B> outputSource;
        
        if ( numDimensions % 2 == 0 )
        {
        	outputSource = temp1;
            
        	// close other temporary datastructure
            temp2.close();
        }
        else
        {
        	outputSource = temp2;

        	// close other temporary datastructure
        	if ( numDimensions > 1 )
        		temp1.close();
        }
        
		final ImageConverter<B, C> convOut = new ImageConverter<B, C>( outputSource, factoryOut, converterOut );
		
		if ( !convOut.checkInput() || !convOut.process() )
		{
			errorMessage = "Cannot convert from B->C: " + convOut.getErrorMessage();
			return null;
		}

        // close temporary datastructure 2
        if ( numDimensions % 2 == 0 )
        {
        	if ( numDimensions > 1 )
        		temp1.close();
        }
        else
        {
        	temp2.close();
        }
		
		return convOut.getResult();		
	}
	
	protected boolean processWithOptimizedMethod() { return false; }
	
	@Override
	public boolean process() 
	{		
		final long startTime = System.currentTimeMillis();

		if ( processWithOptimizedMethod() )
		{
    		processingTime = System.currentTimeMillis() - startTime;

			if ( errorMessage.length() == 0 )
				return true;
			else
				return false;
		}
		
		final long imageSize = image.getNumPixels();
		
		// divide the image into chunks
        final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );

        //
        // Folding loop
        //
        for ( int dim = 0; dim < numDimensions; dim++ )
        {
         	final int currentDim = dim;
         	
    		final Image<B> temp1 = getTempImage1( currentDim );
    		if ( temp1 == null ) return false;
    		
    		final Image<B> temp2 = getTempImage2( currentDim );
    		if ( temp2 == null ) return false;
        	
			final AtomicInteger ai = new AtomicInteger(0);					
	        final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
	        	        
	        for (int ithread = 0; ithread < threads.length; ++ithread)
	            threads[ithread] = new Thread(new Runnable()
	            {
	                public void run()
	                {
	                	// Thread ID
	                	final int myNumber = ai.getAndIncrement();

	                	// get chunk of pixels to process
	                	final Chunk myChunk = threadChunks.get( myNumber );

	                	final LocalizableByDimCursor<B> inputIterator = getInputIterator( temp1, temp2, currentDim );
	                	final LocalizableCursor<B> outputIterator = getOutputIterator( temp1, temp2, currentDim );
	                		                	
	                    // convolve the image in the current dimension using the given cursors
	                    convolveDim( inputIterator, outputIterator, currentDim, myChunk.getStartPosition(), myChunk.getLoopSize() );
		                
		                inputIterator.close();
		                outputIterator.close();		               
	                }
	            });
	        SimpleMultiThreading.startAndJoin(threads);
        }

        // get output image and close the temporary ones if appropriate
		convolved = getConvolvedImage();
		
        processingTime = System.currentTimeMillis() - startTime;
        
        return true;
	}
	
	protected void convolveDim( final LocalizableByDimCursor<B> inputIterator, final LocalizableCursor<B> outputIterator, final int currentDim, final long startPos, final long loopSize )
	{
		convolve( inputIterator, outputIterator, currentDim, kernel[ currentDim ], startPos, loopSize );
	}
	
	protected void convolve( final LocalizableByDimCursor<B> inputIterator, final LocalizableCursor<B> outputIterator, final int dim, final double[] kernel, final long startPos, final long loopSize )
	{		
    	// move to the starting position of the current thread
    	outputIterator.fwd( startPos );
   	 
        final int filterSize = kernel.length;
        final int filterSizeMinus1 = filterSize - 1;
        final int filterSizeHalf = filterSize / 2;
        final int filterSizeHalfMinus1 = filterSizeHalf - 1;
        final int numDimensions = inputIterator.getImage().getNumDimensions();
        
    	final int iteratorPosition = filterSizeHalf;
    	final double lastKernelEntry = kernel[ filterSizeMinus1 ]; 
    	
    	final int[] to = new int[ numDimensions ];
    	
    	final B sum = inputIterator.getType().createVariable();
    	final B tmp = inputIterator.getType().createVariable();
        
    	
        // do as many pixels as wanted by this thread
        for ( long j = 0; j < loopSize; ++j )
        {
        	outputIterator.fwd();			                			                	

        	// set the sum to zero
        	sum.setZero();
        	
        	//
        	// we move filtersize/2 of the convolved pixel in the input image
        	//
        	
        	// get the current positon in the output image
    		outputIterator.getPosition( to );
    		
    		// position in the input image is filtersize/2 to the left
    		to[ dim ] -= iteratorPosition;
    		
    		// set the input cursor to this very position
    		inputIterator.setPosition( to );

    		// iterate over the kernel length across the input image
        	for ( int f = -filterSizeHalf; f <= filterSizeHalfMinus1; ++f )
    		{
        		// get value from the input image
        		tmp.set( inputIterator.getType() );

         		// multiply the kernel
        		tmp.mul( kernel[ f + filterSizeHalf ] );
        		
        		// add up the sum
        		sum.add( tmp );
        		
        		// move the cursor forward for the next iteration
    			inputIterator.fwd( dim );
    		}

        	//
        	// for the last pixel we do not move forward
        	//
        	    		
    		// get value from the input image
    		tmp.set( inputIterator.getType() );
    		    		
    		// multiply the kernel
    		tmp.mul( lastKernelEntry );
    		
    		// add up the sum
    		sum.add( tmp );
    		    		
            outputIterator.getType().set( sum );			                		        	
        }
	}
}
