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

package mpicbg.imglib.algorithm.integral;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.array.ArrayLocalizableCursor;
import mpicbg.imglib.function.Converter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.label.FakeType;
import mpicbg.imglib.type.numeric.NumericType;

/** n-dimensional integral image that stores sums using type {@param <T>}.
 * Care must be taken that sums do not overflow the capacity of type {@param <T>}.
 *
 * The integral image will be one pixel larger in each dimension as for easy computation
 * of sums it has to contain "zeros" at the beginning of each dimension
 *
 * The {@link Converter} defines howto convert from Type {@param <R>} to {@param <T>}.
 *
 * Sums are done with the precision of {@param <T>} and then set to the integral image type,
 * which may crop the values according to the type's capabilities.
 *
 * @param <R> The type of the input image.
 * @param <T> The type of the integral image.
 *
 * @author Stephan Preibisch
 * @author Albert Cardona
 */
public class IntegralImage< R extends NumericType< R >, T extends NumericType< T > > implements OutputAlgorithm< T >
{
	protected final Image< R > img;
	protected final T type;
	protected Image< T > integral;
	protected final Converter< R, T > converter;

	public IntegralImage( final Image< R > img, final T type, final Converter< R, T > converter )
	{
		this.img = img;
		this.type = type;
		this.converter = converter;
	}

	@Override
	public boolean process()
	{
		final int numDimensions = img.getNumDimensions();
		final int integralSize[] = new int[ numDimensions ];

		// the size of the first dimension is changed
		for ( int d = 0; d < numDimensions; ++d )
			integralSize[ d ] = img.getDimension( d ) + 1;

		final ImageFactory< T > imgFactory = new ImageFactory< T >( type, new ArrayContainerFactory() );
		final Image< T > integral = imgFactory.createImage( integralSize );

		// not enough RAM or disc space
		if ( integral == null )
			return false;
		else
			this.integral = integral;

		if ( numDimensions > 1 )
		{
			/**
			 * Here we "misuse" a ArrayLocalizableCursor to iterate through all dimensions except the one we are computing the integral image in
			 */
			final int[] fakeSize = new int[ numDimensions - 1 ];

			// the size of dimension 0
			final int size = integralSize[ 0 ];

			for ( int d = 1; d < numDimensions; ++d )
				fakeSize[ d - 1 ] = integralSize[ d ];

			final long imageSize = getNumPixels( fakeSize );
			
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
	                	
	        			final ArrayLocalizableCursor< FakeType > cursorDim = ArrayLocalizableCursor.createLinearCursor( fakeSize );

	        			// location for the input location
	        			final int[] tmpIn = new int[ numDimensions ];

	        			// location for the integral location
	        			final int[] tmpOut = new int[ numDimensions ];

	        			final int[] tmp = new int[ numDimensions - 1 ];
	        			
	        			final LocalizableByDimCursor< R > cursorIn = img.createLocalizableByDimCursor();
	        			final LocalizableByDimCursor< T > cursorOut = integral.createLocalizableByDimCursor();

	        			final T tmpVar = integral.createType();
	        			final T sum = integral.createType();

	        			cursorDim.fwd( myChunk.getStartPosition() );
	        			
	        			// iterate over all dimensions except the one we are computing the integral in, which is dim=0 here
main:					for ( long j = 0; j < loopSize; ++j )
	        			{
	        				cursorDim.fwd();

	        				// get all dimensions except the one we are currently doing the integral on
	        				cursorDim.getPosition( tmp );

	        				tmpIn[ 0 ] = 0;
	        				tmpOut[ 0 ] = 1;

	        				for ( int d = 1; d < numDimensions; ++d )
	        				{
	        					tmpIn[ d ] = tmp[ d - 1 ] - 1;
	        					tmpOut[ d ] = tmp[ d - 1 ];

	        					// all entries of position 0 are 0
	        					if ( tmpOut[ d ] == 0 )
	        						continue main;
	        				}

	        				// set the cursor to the beginning of the correct line
	        				cursorIn.setPosition( tmpIn );

	        				// set the cursor in the integral image to the right position
	        				cursorOut.setPosition( tmpOut );
	        				
	        				// integrate over the line
	        				integrateLineDim0( converter, cursorIn, cursorOut, sum, tmpVar, size );
	        			}

	        			cursorIn.close();
	        			cursorOut.close();	                	
	                }
	            });
	        
	        SimpleMultiThreading.startAndJoin( threads );
		}
		else
		{
			final T tmpVar = integral.createType();
			final T sum = integral.createType();

			// the size of dimension 0
			final int size = integralSize[ 0 ];

			final LocalizableByDimCursor< R > cursorIn = img.createLocalizableByDimCursor();
			final LocalizableByDimCursor< T > cursorOut = integral.createLocalizableByDimCursor();

			cursorIn.setPosition( 0, 0 );
			cursorOut.setPosition( 1, 0 );

			// compute the first pixel
			converter.convert( cursorIn.getType(), sum );
			cursorOut.getType().set( sum );

			for ( int i = 2; i < size; ++i )
			{
				cursorIn.fwd( 0 );
				cursorOut.fwd( 0 );

				converter.convert( cursorIn.getType(), tmpVar );
				sum.add( tmpVar );
				cursorOut.getType().set( sum );
			}

			cursorIn.close();
			cursorOut.close();

			return true;
		}

		for ( int d = 1; d < numDimensions; ++d )
		{
			final int dim = d;
			
			final int[] fakeSize = new int[ numDimensions - 1 ];

			// the size of dimension d
			final int size = integralSize[ d ];

			// get all dimensions except the one we are currently doing the integral on
			int countDim = 0;
			for ( int e = 0; e < numDimensions; ++e )
				if ( e != d )
					fakeSize[ countDim++ ] = integralSize[ e ];

			final long imageSize = getNumPixels( fakeSize );
			
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

	        			/**
	        			 * Here we "misuse" a ArrayLocalizableCursor to iterate through all dimensions except the one we are computing the fft in
	        			 */
	        			final ArrayLocalizableCursor< FakeType > cursorDim = ArrayLocalizableCursor.createLinearCursor( fakeSize );
	                	
	        			// local instances
	        			final int[] tmp2 = new int[ numDimensions - 1 ];
	        			final int[] tmp = new int[ numDimensions ];

	        			final LocalizableByDimCursor< T > cursor = integral.createLocalizableByDimCursor();
	        			final T sum = integral.createType();

	        			cursorDim.fwd( myChunk.getStartPosition() );
	        			
	                    for ( long j = 0; j < loopSize; ++j )
	                    {
	        				cursorDim.fwd();
	        					        				
	        				// get all dimensions except the one we are currently doing the integral on
	        				cursorDim.getPosition( tmp2 );

	        				tmp[ dim ] = 1;
	        				int countDim = 0;
	        				for ( int e = 0; e < numDimensions; ++e )
	        					if ( e != dim )
	        						tmp[ e ] = tmp2[ countDim++ ];

	        				// update the cursor in the input image to the current dimension position
	        				cursor.setPosition( tmp );

	        				// sum up line
	        				integrateLine( dim, cursor, sum, size );
	        			}

	        			cursor.close();
	                }
	            });
	        
	        SimpleMultiThreading.startAndJoin( threads );
		}

		return true;
	}
	
	public static final long getNumPixels( final int[] size )
	{
		long num = size[ 0 ];
		
		for ( int d = 1; d < size.length; ++d )
			num *= size[ d ];
		
		return num;
	}
	
	protected void integrateLineDim0( final Converter< R, T > converter, final LocalizableByDimCursor< R > cursorIn, final LocalizableByDimCursor< T > cursorOut, final T sum, final T tmpVar, final int size )
	{
		// compute the first pixel
		converter.convert( cursorIn.getType(), sum );
		cursorOut.getType().set( sum );

		for ( int i = 2; i < size; ++i )
		{
			cursorIn.fwd( 0 );
			cursorOut.fwd( 0 );

			converter.convert( cursorIn.getType(), tmpVar );
			sum.add( tmpVar );
			cursorOut.getType().set( sum );
		}		
	}

	protected void integrateLine( final int d, final LocalizableByDimCursor< T > cursor, final T sum, final int size )
	{
		// init sum on first pixel that is not zero
		sum.set( cursor.getType() );

		for ( int i = 2; i < size; ++i )
		{
			cursor.fwd( d );

			sum.add( cursor.getType() );
			cursor.getType().set( sum );
		}
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public Image<T> getResult() {
		return integral;
	}
}
