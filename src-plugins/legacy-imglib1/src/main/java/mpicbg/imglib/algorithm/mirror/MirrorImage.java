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

package mpicbg.imglib.algorithm.mirror;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.Algorithm;
import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.Type;

/**
 * Mirrors an n-dimensional image along an axis (one of the dimensions).
 * The calculation is performed in-place and multithreaded.
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class MirrorImage<T extends Type<T>> implements Algorithm, Benchmark, MultiThreaded
{
	String errorMessage = "";
	int numThreads;
	long processingTime = -1;

	final Image<T> image;
	final int dimension, numDimensions;
	
	/**
	 * @param image - The {@link Image} to mirror
	 * @param dimension - The axis to mirror (e.g. 0->x-Axis->horizontally, 1->y-axis->vertically)
	 */
	public MirrorImage( final Image<T> image, final int dimension )
	{
		this.image = image;
		this.dimension = dimension;
		this.numDimensions = image.getNumDimensions();
		
		setNumThreads();
	}
	
	@Override
	public boolean process()
	{
		final long startTime = System.currentTimeMillis();
		
		// divide the image into chunks
		final long imageSize = image.getNumPixels();
		final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );

		final int maxMirror = image.getDimension( dimension ) - 1;		
		final int sizeMirrorH = image.getDimension( dimension ) / 2;

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

                	final LocalizableCursor<T> cursorIn = image.createLocalizableCursor();
					final LocalizableByDimCursor<T> cursorOut = image.createLocalizableByDimCursor();
					final T temp = image.createType();
					final int[] position = new int[ numDimensions ];
					
					// set the cursorIn to right offset
					final long startPosition = myChunk.getStartPosition();
					final long loopSize = myChunk.getLoopSize();
					
					if ( startPosition > 0 )
						cursorIn.fwd( startPosition );
					
					// iterate over all pixels, if they are above the middle switch them with their counterpart
					// from the other half in the respective dimension
					for ( long i = 0; i < loopSize; ++i )
					{
						cursorIn.fwd();
						cursorIn.getPosition( position );
						
						if ( position[ dimension ] <= sizeMirrorH )
						{
							// set the localizable to the correct mirroring position
							position[ dimension ] = maxMirror - position[ dimension ];
							cursorOut.setPosition( position );
							
							// do a triangle switching
							final T in = cursorIn.getType();
							final T out = cursorOut.getType();
							
							temp.set( in );
							in.set( out );
							out.set( temp );
						}
					}
		
                }
            });
        
        SimpleMultiThreading.startAndJoin(threads);
		
		processingTime = System.currentTimeMillis() - startTime;
		return true;
	}
	
	@Override
	public boolean checkInput() 
	{
		if ( image == null )
		{
			errorMessage = "Input image is null.";
			return false;
		}
		else if ( dimension < 0 || dimension >= numDimensions )
		{
			errorMessage = "Dimension to mirror is invalid: " + dimension;
			return false;			
		}
		
		return true;
	}

	
	@Override
	public String getErrorMessage(){ return errorMessage; }

	@Override
	public long getProcessingTime() { return processingTime; }

	@Override
	public int getNumThreads() { return numThreads; }

	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) { this.numThreads = numThreads; }
}
