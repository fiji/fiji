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

package mpicbg.imglib.algorithm.projection;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.Benchmark;
import mpicbg.imglib.algorithm.MultiThreaded;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.Type;

/**
 * Computes a maximum projection along an arbitrary dimension, if the image in 1-dimensional it will return an Image of size 1 with the max value
 * 
 * @param <T>
 *
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class MaxProjection< T extends Comparable< T > & Type< T > > implements OutputAlgorithm< T >, Benchmark, MultiThreaded
{
	long processingTime;
	int numThreads;
	String errorMessage = "";

	final Image< T > image;
	final int projDim;
	
	Image< T > proj;
	
	public MaxProjection( final Image< T > image, final int dim )
	{
		this.image = image;
		this.projDim = dim;
		
		setNumThreads();
	}
	
	/**
	 * Get projection along the smallest dimension (which is usually the rotation axis)
	 * 
	 * @return - the averaged, projected PSF
	 */
	@Override
	public boolean process() 
	{
		final long startTime = System.currentTimeMillis();
		
		if ( image.getNumDimensions() == 1 )
		{
			// again a special 1d case - thanks Fernando for making me think about that all the time now
			// is a 0-dimensional Image<T> actually single variable?			
			final int[] projImageSize = new int[] { 1 };
			proj = image.getImageFactory().createImage( projImageSize );

			final LocalizableByDimCursor< T > inputIterator = image.createLocalizableByDimCursor();
			final LocalizableCursor< T > projIterator = proj.createLocalizableCursor();
			
			final T maxValue = image.createType();
			final T tmpValue = image.createType();
	
			// go to the one and only pixel
			projIterator.fwd();
			
			// init the input
			inputIterator.fwd();
			maxValue.set( inputIterator.getType() );
			
			while ( inputIterator.hasNext() )
			{
				inputIterator.fwd();

				tmpValue.set( inputIterator.getType() );
				
				if ( tmpValue.compareTo( maxValue ) > 0)
					maxValue.set( tmpValue );
			}

			projIterator.getType().set( maxValue );
		}
		else
		{
			final int[] dimensions = image.getDimensions();		
			final int[] projImageSize = new int[ dimensions.length - 1 ];
			
			int dim = 0;
			final int sizeProjection = dimensions[ projDim ];
			
			// the new dimensions
			for ( int d = 0; d < dimensions.length; ++d )
				if ( d != projDim )
					projImageSize[ dim++ ] = dimensions[ d ];
		
			proj = image.getImageFactory().createImage( projImageSize );
			
			long imageSize = proj.getNumPixels();

			final AtomicInteger ai = new AtomicInteger(0);					
	        final Thread[] threads = SimpleMultiThreading.newThreads( getNumThreads() );

	        final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
			
			for (int ithread = 0; ithread < threads.length; ++ithread)
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
	                	
	        			final LocalizableByDimCursor< T > inputIterator = image.createLocalizableByDimCursor();
	        			final LocalizableCursor< T > projIterator = proj.createLocalizableCursor();
	        			
	        			final int[] tmp = new int[ image.getNumDimensions() ];
	        			final T maxValue = image.createType();
	        			final T tmpValue = image.createType();

	        			projIterator.fwd( start );
	        	    	
	        	        // do as many pixels as wanted by this thread
	        	        for ( long j = 0; j < loopSize; ++j )
	        			{
	        				projIterator.fwd();
	        	
	        				int dim = 0;
	        				for ( int d = 0; d < dimensions.length; ++d )
	        					if ( d != projDim )
	        						tmp[ d ] = projIterator.getPosition( dim++ );
	        	
	        				tmp[ projDim ] = 0;			
	        				inputIterator.setPosition( tmp );
	        				maxValue.set( inputIterator.getType() );
	        	
	        				for ( int i = 1; i < sizeProjection; ++i )
	        				{
	        					inputIterator.fwd( projDim );
	        					
	        					tmpValue.set( inputIterator.getType() );
	        	
	        					if ( tmpValue.compareTo( maxValue ) > 0)
	        						maxValue.set( tmpValue );
	        				}
	        				
	        				projIterator.getType().set( maxValue );
	        			}	                	
	                }
	            });
	        
	        SimpleMultiThreading.startAndJoin( threads );			
		}
		
		proj.setName( "Max(" + this.projDim + ") " + image.getName() );

		processingTime = System.currentTimeMillis() - startTime;

		return true;
	}
	
	@Override
	public boolean checkInput() 
	{
		if ( errorMessage.length() > 0 )
		{
			return false;
		}
		else if ( image == null )
		{
			errorMessage = "MaxProjection: [Image<T> image] is null.";
			return false;
		}
		else if ( projDim < 0 || projDim >= image.getNumDimensions() )
		{
			errorMessage = "Invalid dimensionality for projection: " + projDim;
			return false;
		}	
		else
		{
			return true;
		}
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

	@Override
	public Image<T> getResult() { return proj; }
	
}
