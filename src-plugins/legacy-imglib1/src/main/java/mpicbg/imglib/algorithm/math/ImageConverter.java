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
import mpicbg.imglib.function.Converter;
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
public class ImageConverter< S extends Type<S>, T extends Type<T> > implements OutputAlgorithm<T>, MultiThreaded, Benchmark
{
	final Image<S> image; 
	final Image<T> output;
	final Converter<S,T> converter;

	long processingTime;
	int numThreads;
	String errorMessage = "";
	
	public ImageConverter( final Image<S> image, final Image<T> output, final Converter<S,T> converter )
	{
		this.image = image;
		this.output = output;
		this.converter = converter;
		
		setNumThreads();
	}
	
	public ImageConverter( final Image<S> image, final ImageFactory<T> factory, final Converter<S,T> converter )
	{
		this( image, createImageFromFactory( factory, image.getDimensions() ), converter );
	}
	
	@Override
	public Image<T> getResult() { return output; }

	@Override
	public boolean checkInput()
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image == null )
		{
			errorMessage = "ImageCalculator: [Image<S> image1] is null.";
			return false;
		}
		else if ( output == null )
		{
			errorMessage = "ImageCalculator: [Image<T> output] is null.";
			return false;
		}
		else if ( converter == null )
		{
			errorMessage = "ImageCalculator: [Converter<S,T>] is null.";
			return false;
		}
		else if ( !image.getContainer().compareStorageContainerDimensions( output.getContainer() ) )
		{
			errorMessage = "ImageCalculator: Images have different dimensions, not supported:" + 
				" Image: " + Util.printCoordinates( image.getDimensions() ) + 
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

		final long imageSize = image.getNumPixels();

		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads( getNumThreads() );

        final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
        
        final boolean isCompatible = image.getContainer().compareStorageContainerCompatibility( output.getContainer() ); 
	
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();
        
                	// get chunk of pixels to process
                	final Chunk myChunk = threadChunks.get( myNumber );
                	
					// check if all container types are comparable so that we can use simple iterators
					// we assume transivity here
					if (  isCompatible )
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
		final Cursor<S> cursorIn = image.createCursor();
		final Cursor<T> cursorOut = output.createCursor();
		
		// move to the starting position of the current thread
		cursorIn.fwd( startPos );
		cursorOut.fwd( startPos );
    	
        // do as many pixels as wanted by this thread
        for ( long j = 0; j < loopSize; ++j )
        {
			cursorIn.fwd();
			cursorOut.fwd();
			
			converter.convert( cursorIn.getType(), cursorOut.getType() );
		}
		
		cursorIn.close();
		cursorOut.close();		
	}
	
	protected void computeAdvanced( final long startPos, final long loopSize )
	{
		final LocalizableByDimCursor<S> cursorIn = image.createLocalizableByDimCursor();
		final LocalizableCursor<T> cursorOut = output.createLocalizableCursor();
		
		// move to the starting position of the current thread
		cursorOut.fwd( startPos );
    	
        // do as many pixels as wanted by this thread
        for ( long j = 0; j < loopSize; ++j )
        {
			cursorOut.fwd();
			cursorIn.setPosition( cursorOut );
			
			converter.convert( cursorIn.getType(), cursorOut.getType() );
		}
		
		cursorIn.close();
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
	
	protected static <T extends Type<T>> Image<T> createImageFromFactory( final ImageFactory<T> factory, final int[] size )
	{
		if ( factory == null || size == null )
			return null;
		else 
			return factory.createImage( size );			
	}
}
