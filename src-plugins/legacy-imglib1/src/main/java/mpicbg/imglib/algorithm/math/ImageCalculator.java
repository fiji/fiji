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

package mpicbg.imglib.algorithm.math;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.function.Function;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.Type;
import mpicbg.imglib.util.Util;

/**
 * TODO
 *
 * @author Stephan Preibisch
 */
public class ImageCalculator<S extends Type<S>, T extends Type<T>, U extends Type<U>> implements OutputAlgorithm<U>, MultiThreaded, Benchmark
{
	final Image<S> image1; 
	final Image<T> image2; 
	final Image<U> output;
	final Function<S,T,U> function;

	long processingTime;
	int numThreads;
	String errorMessage = "";
	
	public ImageCalculator( final Image<S> image1, final Image<T> image2, final Image<U> output, final Function<S,T,U> function )
	{
		this.image1 = image1;
		this.image2 = image2;
		this.output = output;
		this.function = function;
		
		setNumThreads();
	}
	
	public ImageCalculator( final Image<S> image1, final Image<T> image2, final ImageFactory<U> factory, final Function<S,T,U> function )
	{
		this( image1, image2, createImageFromFactory( factory, image1.getDimensions() ), function );
	}
	
	@Override
	public Image<U> getResult() { return output; }

	@Override
	public boolean checkInput()
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image1 == null )
		{
			errorMessage = "ImageCalculator: [Image<S> image1] is null.";
			return false;
		}
		else if ( image2 == null )
		{
			errorMessage = "ImageCalculator: [Image<T> image2] is null.";
			return false;
		}
		else if ( output == null )
		{
			errorMessage = "ImageCalculator: [Image<U> output] is null.";
			return false;
		}
		else if ( function == null )
		{
			errorMessage = "ImageCalculator: [Function<S,T,U>] is null.";
			return false;
		}
		else if ( !image1.getContainer().compareStorageContainerDimensions( image2.getContainer() ) || 
				  !image1.getContainer().compareStorageContainerDimensions( output.getContainer() ) )
		{
			errorMessage = "ImageCalculator: Images have different dimensions, not supported:" + 
				" Image1: " + Util.printCoordinates( image1.getDimensions() ) + 
				" Image2: " + Util.printCoordinates( image2.getDimensions() ) +
				" Output: " + Util.printCoordinates( output.getDimensions() );
			return false;
		}
		else
			return true;
	}

	@Override
	public boolean process()
	{
		final long startTime = System.currentTimeMillis();
   
		final long imageSize = image1.getNumPixels();

		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads( getNumThreads() );

        final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
		
		// check if all container types are comparable so that we can use simple iterators
		// we assume transivity here
        final boolean isCompatible = image1.getContainer().compareStorageContainerCompatibility( image2.getContainer() ) &&
		 							 image1.getContainer().compareStorageContainerCompatibility( output.getContainer() );
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();
        
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( myNumber );
                	
                	if ( isCompatible )
                	{
            			// we can simply use iterators
            			computeSimple( myChunk.getStartPosition(), myChunk.getLoopSize() );                		
                	}
                	else
                	{
            			// we need a combination of Localizable and LocalizableByDim
            			computeAdvanced( myChunk.getStartPosition(), myChunk.getLoopSize() );                		
                	}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
		processingTime = System.currentTimeMillis() - startTime;
        
		return true;
	}
	
	protected void computeSimple( final long startPos, final long loopSize )
	{
		final Cursor<S> cursor1 = image1.createCursor();
		final Cursor<T> cursor2 = image2.createCursor();
		final Cursor<U> cursorOut = output.createCursor();
		
		// move to the starting position of the current thread
		cursor1.fwd( startPos );
		cursor2.fwd( startPos );
		cursorOut.fwd( startPos );
    	
        // do as many pixels as wanted by this thread
        for ( long j = 0; j < loopSize; ++j )
        {
			cursor1.fwd();
			cursor2.fwd();
			cursorOut.fwd();
			
			function.compute( cursor1.getType(), cursor2.getType(), cursorOut.getType() );
		}
		
		cursor1.close();
		cursor2.close();
		cursorOut.close();		
	}
	
	protected void computeAdvanced( final long startPos, final long loopSize )
	{
		System.out.println( startPos + " -> " + (startPos+loopSize) );
		final LocalizableByDimCursor<S> cursor1 = image1.createLocalizableByDimCursor();
		final LocalizableByDimCursor<T> cursor2 = image2.createLocalizableByDimCursor();
		final LocalizableCursor<U> cursorOut = output.createLocalizableCursor();
		
		// move to the starting position of the current thread
		cursorOut.fwd( startPos );
        
		// do as many pixels as wanted by this thread
        for ( long j = 0; j < loopSize; ++j )
		{
			cursorOut.fwd();
			cursor1.setPosition( cursorOut );
			cursor2.setPosition( cursorOut );
			
			function.compute( cursor1.getType(), cursor2.getType(), cursorOut.getType() );
		}
		
		cursor1.close();
		cursor2.close();
		cursorOut.close();			
		
	}

	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }

	@Override
	public int getNumThreads() { return numThreads; }	

	@Override
	public String getErrorMessage() { return errorMessage; }

	@Override
	public long getProcessingTime() { return processingTime; }
	
	protected static <U extends Type<U>> Image<U> createImageFromFactory( final ImageFactory<U> factory, final int[] size )
	{
		if ( factory == null || size == null )
			return null;
		else 
			return factory.createImage( size );			
	}
}
